package com.ariabridge.lite

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import io.github.muntashirakon.adb.AbsAdbConnectionManager
import io.github.muntashirakon.adb.android.AdbMdns
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.InetAddress
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var manager: AbsAdbConnectionManager
    private lateinit var vault: AnomalyVault
    private lateinit var status: TextView
    private lateinit var codeInput: EditText
    private lateinit var output: TextView
    private lateinit var pairButton: Button
    private lateinit var scanButton: Button
    private lateinit var recoverButton: Button
    private lateinit var shareButton: Button
    private val executor = Executors.newSingleThreadExecutor()
    private var targets: List<RecoveryEngine.Target> = emptyList()
    private var lastRecoveryFile: File? = null
    private var pairingMdns: AdbMdns? = null
    private var multicastLock: WifiManager.MulticastLock? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vault = AnomalyVault(this)
        vault.record("app_launch", JSONObject().put("version", "0.1.2"))
        manager = AdbConnectionManager.getInstance(this)
        acquireMulticastLock()
        setContentView(buildUi())
    }

    private fun buildUi(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 24, 32, 24)
        }
        root.addView(TextView(this).apply {
            text = "Aria Bridge Lite"
            textSize = 28f
        })
        root.addView(TextView(this).apply {
            text = "Phone-only Chrome/Workday recovery bridge. It pairs with your own device over Android Wireless Debugging, then asks adbd to open Chrome's DevTools socket."
            textSize = 15f
            setPadding(0, 8, 0, 18)
        })
        status = TextView(this).apply { text = "Status: not paired"; textSize = 16f }
        root.addView(status)

        root.addView(Button(this).apply {
            text = "1  Open Developer Options"
            setOnClickListener { runCatching { startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)) } }
        })

        codeInput = EditText(this).apply {
            hint = "Six-digit Wireless Debugging pairing code"
            inputType = InputType.TYPE_CLASS_NUMBER
            maxLines = 1
        }
        root.addView(codeInput)

        pairButton = Button(this).apply {
            text = "2  Pair using code (auto-discovers port)"
            setOnClickListener { pairUsingCode() }
        }
        root.addView(pairButton)

        scanButton = Button(this).apply {
            text = "3  Connect + Scan Chrome Tabs"
            isEnabled = true
            setOnClickListener { connectAndScan() }
        }
        root.addView(scanButton)

        recoverButton = Button(this).apply {
            text = "4  Recover Workday State"
            isEnabled = false
            setOnClickListener { recoverWorkday() }
        }
        root.addView(recoverButton)

        shareButton = Button(this).apply {
            text = "Share Recovery JSON"
            isEnabled = false
            setOnClickListener { shareRecovery() }
        }
        root.addView(shareButton)

        root.addView(Button(this).apply {
            text = "Copy Output"
            setOnClickListener {
                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("Aria Bridge output", output.text))
                setStatus("Output copied")
            }
        })

        output = TextView(this).apply {
            text = "Output will appear here.\n"
            setTextIsSelectable(true)
            textSize = 13f
            setPadding(0, 18, 0, 80)
        }
        val scroll = ScrollView(this).apply { addView(output) }
        root.addView(scroll, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        return root
    }

    private fun pairUsingCode() {
        val code = codeInput.text.toString().trim()
        if (!code.matches(Regex("\\d{6}"))) {
            vault.record("pair_rejected_invalid_code_format")
            setStatus("Enter the six-digit code while the Android pairing dialog is still open")
            return
        }
        vault.record("pair_start")
        pairButton.isEnabled = false
        setStatus("Discovering the temporary ADB pairing service… keep the pairing dialog open")
        pairingMdns?.stop()
        pairingMdns = AdbMdns(this, AdbMdns.SERVICE_TYPE_TLS_PAIRING) { host: InetAddress?, port: Int, _: String? ->
            if (host != null && port > 0) {
                pairingMdns?.stop()
                vault.record("pair_service_discovered", JSONObject().put("host", host.hostAddress ?: "").put("port", port))
                executor.execute {
                    try {
                        setStatusUi("Pairing with ${host.hostAddress}:$port…")
                        manager.pair(host.hostAddress ?: "127.0.0.1", port, code)
                        vault.record("pair_success", JSONObject().put("host", host.hostAddress ?: "").put("port", port))
                        setStatusUi("Paired. Connecting to Wireless ADB…")
                        val connected = manager.connectTls(this, 12_000)
                        val isUp = connected || manager.isConnected
                        vault.record("adb_connect_after_pair", JSONObject().put("connected", isUp))
                        setStatusUi(if (isUp) "Connected to your phone over Wireless ADB ✅" else "Paired, but connection did not establish")
                        runOnUiThread { pairButton.isEnabled = true }
                    } catch (t: Throwable) {
                        vault.recordThrowable("pair_connect_error", t, JSONObject().put("host", host.hostAddress ?: "").put("port", port))
                        setStatusUi("Pair/connect error: ${t.message}")
                        appendUi("${t.javaClass.simpleName}: ${t.message}\n")
                        runOnUiThread { pairButton.isEnabled = true }
                    }
                }
            }
        }.also { it.start() }
    }

    private fun connectAndScan() {
        scanButton.isEnabled = false
        vault.record("scan_start", JSONObject().put("alreadyConnected", manager.isConnected))
        executor.execute {
            try {
                if (!manager.isConnected) {
                    setStatusUi("Finding Wireless ADB connection…")
                    manager.connectTls(this, 12_000)
                }
                check(manager.isConnected) { "Not connected. Pair first while the six-digit pairing dialog is open." }
                setStatusUi("Connected. Asking adbd for Chrome DevTools sockets…")
                val engine = RecoveryEngine(manager)
                targets = engine.listTargets()
                val lines = buildString {
                    append("Found ${targets.size} DevTools target(s)\n\n")
                    targets.forEachIndexed { i, t -> append("${i + 1}. ${t.title}\n${t.url}\nSocket: ${t.socket}\n\n") }
                }
                appendUi(lines)
                val hasWorkday = targets.any { it.url.contains("workday", true) || it.title.contains("Patient Service", true) }
                vault.record("scan_complete", JSONObject().put("targetCount", targets.size).put("workdayFound", hasWorkday).put("socketCount", targets.map { it.socket }.distinct().size))
                setStatusUi(if (hasWorkday) "Workday target found 🎯" else "Chrome scanned. No obvious Workday target yet.")
                runOnUiThread { recoverButton.isEnabled = hasWorkday }
            } catch (t: Throwable) {
                vault.recordThrowable("scan_error", t)
                setStatusUi("Scan failed: ${t.message}")
                appendUi("${t.javaClass.simpleName}: ${t.stackTraceToString()}\n")
            } finally {
                runOnUiThread { scanButton.isEnabled = true }
            }
        }
    }

    private fun recoverWorkday() {
        recoverButton.isEnabled = false
        vault.record("recovery_start", JSONObject().put("candidateCount", targets.count { it.url.contains("workday", true) || it.title.contains("Patient Service", true) }))
        executor.execute {
            try {
                val candidates = targets.filter { it.url.contains("workday", true) || it.title.contains("Patient Service", true) }
                check(candidates.isNotEmpty()) { "No Workday target is currently listed." }
                setStatusUi("Inspecting Workday page state through Chrome DevTools…")
                val engine = RecoveryEngine(manager)
                val recovered = JSONArray()
                candidates.forEach { target ->
                    runCatching { engine.recover(target) }
                        .onSuccess { recovered.put(it) }
                        .onFailure {
                            vault.recordThrowable("recovery_target_error", it, JSONObject().put("socket", target.socket).put("title", target.title.take(200)))
                            recovered.put(JSONObject().put("targetUrl", target.url).put("error", it.toString()))
                        }
                }
                val root = JSONObject().apply {
                    put("tool", "Aria Bridge Lite")
                    put("version", "0.1.2")
                    put("note", "Read-only capture of the currently live Workday Chrome target(s).")
                    put("targets", recovered)
                }
                val file = File(filesDir, "workday_recovery_${System.currentTimeMillis()}.json")
                file.writeText(root.toString(2))
                lastRecoveryFile = file
                vault.record("recovery_complete", JSONObject().put("recoveredTargets", recovered.length()).put("outputBytes", file.length()))
                appendUi("\n===== RECOVERY =====\n${root.toString(2)}\n")
                setStatusUi("Recovery capture complete ✅  ${file.name}")
                runOnUiThread { shareButton.isEnabled = true }
            } catch (t: Throwable) {
                vault.recordThrowable("recovery_error", t)
                setStatusUi("Recovery failed: ${t.message}")
                appendUi("${t.javaClass.simpleName}: ${t.stackTraceToString()}\n")
            } finally {
                runOnUiThread { recoverButton.isEnabled = true }
            }
        }
    }

    private fun shareRecovery() {
        val file = lastRecoveryFile ?: return
        val uri = FileProvider.getUriForFile(this, "$packageName.files", file)
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }, "Share Workday recovery"))
    }

    private fun acquireMulticastLock() {
        val wifi = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        multicastLock = wifi.createMulticastLock("aria_bridge_mdns").apply {
            setReferenceCounted(false)
            acquire()
        }
    }

    private fun setStatus(text: String) { status.text = "Status: $text" }
    private fun setStatusUi(text: String) = runOnUiThread { setStatus(text) }
    private fun appendUi(text: String) = runOnUiThread { output.append(text) }

    override fun onDestroy() {
        vault.record("app_destroy")
        pairingMdns?.stop()
        runCatching { multicastLock?.release() }
        executor.shutdownNow()
        super.onDestroy()
    }
}
