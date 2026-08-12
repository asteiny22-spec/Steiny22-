package com.ariabridge.lite

import android.content.Context
import android.os.Build
import android.os.Process
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import org.json.JSONObject
import java.io.File
import java.security.KeyStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class AnomalyVault(private val context: Context) {
    private val alias = "aria_anomaly_vault_v1"
    private val vaultFile = File(context.filesDir, ".aria_anomaly_vault")
    private val lock = Any()

    fun record(type: String, details: JSONObject = JSONObject()) {
        runCatching {
            val event = JSONObject().apply {
                put("ts", isoNow())
                put("type", type)
                put("pid", Process.myPid())
                put("thread", Thread.currentThread().name)
                put("details", details)
            }
            val encrypted = encrypt(event.toString().toByteArray(Charsets.UTF_8))
            synchronized(lock) {
                vaultFile.appendText(android.util.Base64.encodeToString(encrypted, android.util.Base64.NO_WRAP) + "\n")
            }
        }
    }

    fun recordThrowable(type: String, t: Throwable, extra: JSONObject = JSONObject()) {
        val details = JSONObject().apply {
            put("class", t.javaClass.name)
            put("message", t.message ?: "")
            put("stack", t.stackTraceToString().take(12000))
            put("extra", extra)
        }
        record(type, details)
    }

    fun metadata(): JSONObject = JSONObject().apply {
        put("path", vaultFile.absolutePath)
        put("exists", vaultFile.exists())
        put("bytes", if (vaultFile.exists()) vaultFile.length() else 0L)
        put("device", "${Build.MANUFACTURER} ${Build.MODEL}")
        put("android", Build.VERSION.RELEASE)
        put("sdk", Build.VERSION.SDK_INT)
    }

    private fun getOrCreateKey(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(alias, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    private fun encrypt(plain: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val iv = cipher.iv
        val body = cipher.doFinal(plain)
        return ByteArray(1 + iv.size + body.size).also { out ->
            out[0] = iv.size.toByte()
            System.arraycopy(iv, 0, out, 1, iv.size)
            System.arraycopy(body, 0, out, 1 + iv.size, body.size)
        }
    }

    fun decryptAllForExplicitExport(): String {
        if (!vaultFile.exists()) return ""
        val key = getOrCreateKey()
        return synchronized(lock) {
            vaultFile.readLines().filter { it.isNotBlank() }.joinToString("\n") { line ->
                runCatching {
                    val packed = android.util.Base64.decode(line, android.util.Base64.NO_WRAP)
                    val ivLen = packed[0].toInt() and 0xff
                    val iv = packed.copyOfRange(1, 1 + ivLen)
                    val body = packed.copyOfRange(1 + ivLen, packed.size)
                    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                    cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
                    String(cipher.doFinal(body), Charsets.UTF_8)
                }.getOrElse { JSONObject().put("type", "vault_decode_error").put("message", it.message ?: "").toString() }
            }
        }
    }

    private fun isoNow(): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        fmt.timeZone = TimeZone.getTimeZone("UTC")
        return fmt.format(Date())
    }
}
