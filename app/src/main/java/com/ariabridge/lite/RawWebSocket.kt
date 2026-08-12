package com.ariabridge.lite

import io.github.muntashirakon.adb.AdbStream
import java.io.ByteArrayOutputStream
import java.io.EOFException
import java.io.InputStream
import java.io.OutputStream
import java.net.URI
import java.security.SecureRandom
import android.util.Base64

class RawWebSocket(private val stream: AdbStream, private val wsUrl: String) : AutoCloseable {
    private val input: InputStream = stream.openInputStream()
    private val output: OutputStream = stream.openOutputStream()

    fun handshake() {
        val uri = URI(wsUrl)
        val path = buildString {
            append(if (uri.rawPath.isNullOrEmpty()) "/" else uri.rawPath)
            if (!uri.rawQuery.isNullOrEmpty()) append('?').append(uri.rawQuery)
        }
        val keyBytes = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val key = Base64.encodeToString(keyBytes, Base64.NO_WRAP)
        val request = "GET $path HTTP/1.1\r\n" +
            "Host: localhost\r\n" +
            "Upgrade: websocket\r\n" +
            "Connection: Upgrade\r\n" +
            "Sec-WebSocket-Key: $key\r\n" +
            "Sec-WebSocket-Version: 13\r\n\r\n"
        output.write(request.toByteArray(Charsets.UTF_8))
        output.flush()
        val headers = readHttpHeaders(input)
        require(headers.startsWith("HTTP/1.1 101") || headers.startsWith("HTTP/1.0 101")) {
            "WebSocket upgrade failed: ${headers.lineSequence().firstOrNull()}"
        }
    }

    fun sendText(text: String) {
        val payload = text.toByteArray(Charsets.UTF_8)
        val mask = ByteArray(4).also { SecureRandom().nextBytes(it) }
        val frame = ByteArrayOutputStream()
        frame.write(0x81)
        when {
            payload.size < 126 -> frame.write(0x80 or payload.size)
            payload.size <= 0xFFFF -> {
                frame.write(0x80 or 126)
                frame.write((payload.size ushr 8) and 0xFF)
                frame.write(payload.size and 0xFF)
            }
            else -> {
                frame.write(0x80 or 127)
                val len = payload.size.toLong()
                for (shift in 56 downTo 0 step 8) frame.write(((len ushr shift) and 0xFF).toInt())
            }
        }
        frame.write(mask)
        payload.forEachIndexed { index, b -> frame.write(b.toInt() xor mask[index % 4].toInt()) }
        output.write(frame.toByteArray())
        output.flush()
    }

    fun readText(): String {
        val assembled = ByteArrayOutputStream()
        var collectingText = false
        while (true) {
            val first = input.read()
            if (first < 0) throw EOFException("WebSocket closed")
            val second = input.read()
            if (second < 0) throw EOFException("WebSocket closed")
            val fin = (first and 0x80) != 0
            val opcode = first and 0x0F
            val masked = (second and 0x80) != 0
            var length = (second and 0x7F).toLong()
            if (length == 126L) length = ((readByte(input) shl 8) or readByte(input)).toLong()
            else if (length == 127L) {
                length = 0
                repeat(8) { length = (length shl 8) or readByte(input).toLong() }
            }
            require(length <= 8_000_000) { "Frame too large: $length" }
            val mask = if (masked) ByteArray(4) { readByte(input).toByte() } else null
            val payload = ByteArray(length.toInt())
            readFully(input, payload)
            if (mask != null) payload.indices.forEach { payload[it] = (payload[it].toInt() xor mask[it % 4].toInt()).toByte() }

            when (opcode) {
                0x1 -> {
                    assembled.reset()
                    assembled.write(payload)
                    collectingText = true
                    if (fin) return assembled.toString(Charsets.UTF_8.name())
                }
                0x0 -> if (collectingText) {
                    assembled.write(payload)
                    if (fin) return assembled.toString(Charsets.UTF_8.name())
                }
                0x8 -> throw EOFException("WebSocket closed by peer")
                0x9 -> sendControl(0xA, payload)
            }
        }
    }

    private fun sendControl(opcode: Int, payload: ByteArray) {
        val mask = ByteArray(4).also { SecureRandom().nextBytes(it) }
        val frame = ByteArrayOutputStream()
        frame.write(0x80 or opcode)
        frame.write(0x80 or payload.size)
        frame.write(mask)
        payload.forEachIndexed { index, b -> frame.write(b.toInt() xor mask[index % 4].toInt()) }
        output.write(frame.toByteArray())
        output.flush()
    }

    override fun close() {
        try { stream.close() } catch (_: Throwable) {}
    }

    companion object {
        fun readHttpHeaders(input: InputStream): String {
            val out = ByteArrayOutputStream()
            var state = 0
            while (out.size() < 65536) {
                val b = input.read()
                if (b < 0) break
                out.write(b)
                state = when {
                    state == 0 && b == '\r'.code -> 1
                    state == 1 && b == '\n'.code -> 2
                    state == 2 && b == '\r'.code -> 3
                    state == 3 && b == '\n'.code -> 4
                    else -> 0
                }
                if (state == 4) break
            }
            return out.toString(Charsets.UTF_8.name())
        }

        private fun readByte(input: InputStream): Int {
            val b = input.read()
            if (b < 0) throw EOFException()
            return b
        }

        private fun readFully(input: InputStream, buffer: ByteArray) {
            var offset = 0
            while (offset < buffer.size) {
                val n = input.read(buffer, offset, buffer.size - offset)
                if (n < 0) throw EOFException()
                offset += n
            }
        }
    }
}
