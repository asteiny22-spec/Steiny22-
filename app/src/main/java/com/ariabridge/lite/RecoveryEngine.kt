package com.ariabridge.lite

import io.github.muntashirakon.adb.AbsAdbConnectionManager
import io.github.muntashirakon.adb.LocalServices
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream

class RecoveryEngine(private val manager: AbsAdbConnectionManager) {
    data class Target(val socket: String, val id: String, val title: String, val url: String, val wsUrl: String)

    fun discoverChromeSockets(): List<String> {
        val stream = manager.openStream("shell:cat /proc/net/unix | grep chrome_devtools_remote")
        val text = stream.openInputStream().use { readAllText(it, 500_000) }
        try { stream.close() } catch (_: Throwable) {}
        return Regex("@?(chrome_devtools_remote(?:_[0-9]+)?)")
            .findAll(text)
            .map { it.groupValues[1] }
            .distinct()
            .toList()
    }

    fun listTargets(): List<Target> {
        val all = mutableListOf<Target>()
        for (socket in discoverChromeSockets().ifEmpty { listOf("chrome_devtools_remote") }) {
            val raw = httpGet(socket, "/json/list")
            val body = raw.substringAfter("\r\n\r\n", raw)
            val array = runCatching { JSONArray(body) }.getOrNull() ?: continue
            for (i in 0 until array.length()) {
                val o = array.optJSONObject(i) ?: continue
                val ws = o.optString("webSocketDebuggerUrl")
                if (ws.isBlank()) continue
                all += Target(socket, o.optString("id"), o.optString("title"), o.optString("url"), ws)
            }
        }
        return all.distinctBy { it.id to it.socket }
    }

    fun recover(target: Target): JSONObject {
        val stream = manager.openStream(LocalServices.LOCAL_UNIX_SOCKET_ABSTRACT, target.socket)
        RawWebSocket(stream, target.wsUrl).use { ws ->
            ws.handshake()
            val request = JSONObject().apply {
                put("id", 9001)
                put("method", "Runtime.evaluate")
                put("params", JSONObject().apply {
                    put("expression", RECOVERY_EXPRESSION)
                    put("returnByValue", true)
                    put("awaitPromise", true)
                    put("userGesture", false)
                })
            }
            ws.sendText(request.toString())
            repeat(200) {
                val message = JSONObject(ws.readText())
                if (message.optInt("id") == 9001) {
                    val value = message.optJSONObject("result")?.optJSONObject("result")?.opt("value")
                    return when (value) {
                        is JSONObject -> value
                        else -> JSONObject().put("rawResult", value ?: JSONObject.NULL)
                    }.apply {
                        put("targetTitle", target.title)
                        put("targetUrl", target.url)
                        put("devtoolsSocket", target.socket)
                    }
                }
            }
            error("No Runtime.evaluate response received")
        }
    }

    private fun httpGet(socket: String, path: String): String {
        val stream = manager.openStream(LocalServices.LOCAL_UNIX_SOCKET_ABSTRACT, socket)
        val out = stream.openOutputStream()
        val input = stream.openInputStream()
        val request = "GET $path HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n"
        out.write(request.toByteArray())
        out.flush()
        val result = readAllText(input, 4_000_000)
        try { stream.close() } catch (_: Throwable) {}
        return result
    }

    private fun readAllText(input: InputStream, max: Int): String {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (out.size() < max) {
            val n = try { input.read(buffer) } catch (e: java.io.IOException) {
                if (out.size() > 0) break else throw e
            }
            if (n <= 0) break
            out.write(buffer, 0, minOf(n, max - out.size()))
        }
        return out.toString(Charsets.UTF_8.name())
    }

    companion object {
        private val RECOVERY_EXPRESSION = """
(() => {
  const clean = s => (s == null ? '' : String(s)).replace(/\\s+/g,' ').trim();
  const uniq = a => [...new Set(a.filter(Boolean))];
  const describe = el => {
    const candidates = [
      el.getAttribute && el.getAttribute('aria-label'),
      el.getAttribute && el.getAttribute('data-automation-label'),
      el.value,
      el.closest && el.closest('label') && el.closest('label').innerText,
      el.closest && el.closest('[role=option]') && el.closest('[role=option]').innerText,
      el.parentElement && el.parentElement.innerText,
      el.innerText,
      el.textContent
    ];
    return clean(candidates.find(x => clean(x)) || '');
  };
  const selected = uniq([
    ...document.querySelectorAll('input[type=checkbox]:checked, input[type=radio]:checked, [role=checkbox][aria-checked=true], [role=option][aria-selected=true], [aria-selected=true]')
  ].map(describe));
  const skillEls = [...document.querySelectorAll('*')].filter(el => {
    const attrs = [...(el.attributes || [])].map(a => `${'$'}{a.name}=${'$'}{a.value}`).join(' ');
    const txt = clean(el.innerText || el.textContent || '');
    return /skill/i.test(attrs) || (/skill/i.test(txt) && txt.length < 5000);
  });
  const skillText = uniq(skillEls.map(describe)).slice(0, 10000);
  const automation = uniq([...document.querySelectorAll('[data-automation-id]')]
      .filter(el => /skill|multi|prompt|select/i.test(el.getAttribute('data-automation-id') || ''))
      .map(el => `${'$'}{el.getAttribute('data-automation-id')}: ${'$'}{clean(el.innerText || el.textContent || '')}`))
      .slice(0, 10000);
  const ls = {}; for (let i=0;i<localStorage.length;i++){ const k=localStorage.key(i); try{ ls[k]=localStorage.getItem(k); }catch(e){} }
  const ss = {}; for (let i=0;i<sessionStorage.length;i++){ const k=sessionStorage.key(i); try{ ss[k]=sessionStorage.getItem(k); }catch(e){} }
  const scripts = [...document.scripts].map(s => s.src).filter(Boolean).slice(0,500);
  return {
    capturedAt: new Date().toISOString(),
    title: document.title,
    url: location.href,
    selected,
    skillText,
    automation,
    localStorage: ls,
    sessionStorage: ss,
    scripts,
    counts: { selected: selected.length, skillText: skillText.length, automation: automation.length }
  };
})()
""".trimIndent()
    }
}
