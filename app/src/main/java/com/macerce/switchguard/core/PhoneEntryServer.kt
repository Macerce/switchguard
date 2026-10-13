package com.macerce.switchguard.core

import android.os.Handler
import android.os.Looper
import android.util.Log
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors

/**
 * [PhoneEntry] sayfasını ev ağında sunan küçük HTTP sunucusu. Yalnızca "/e/<token>" adresine cevap verir:
 * GET sayfayı, POST şifreli alanları alır. Doğru açılan ilk gönderimde [onReceived] ana iş parçacığında çağrılır
 * ve sunucu kapanır. Çok sayıda hatalı gönderimde de kapanır.
 */
class PhoneEntryServer(
    private val page: String,
    private val token: String,
    private val key: ByteArray,
    private val onReceived: (Map<String, String>) -> Unit,
) {
    private val main = Handler(Looper.getMainLooper())
    private val io = Executors.newSingleThreadExecutor()
    @Volatile private var server: ServerSocket? = null
    private var failures = 0

    /** Dinlemeye başlar ve portu döner; açılamazsa null. */
    fun start(): Int? {
        val srv = runCatching { ServerSocket(0) }.getOrElse { Log.w(TAG, "phone entry server", it); return null }
        server = srv
        io.execute {
            while (!srv.isClosed) {
                val s = runCatching { srv.accept() }.getOrNull() ?: break
                runCatching { handle(s) }.onFailure { Log.w(TAG, "phone entry request", it) }
                runCatching { s.close() }
            }
        }
        return srv.localPort
    }

    fun stop() {
        runCatching { server?.close() }
        io.shutdown()
    }

    private fun handle(s: Socket) {
        s.soTimeout = 15_000
        val input = BufferedInputStream(s.getInputStream())
        val head = readHead(input) ?: return
        val lines = head.split("\r\n")
        val (method, path) = lines.first().split(' ').let { (it.getOrNull(0) ?: "") to (it.getOrNull(1) ?: "") }
        val length = lines.drop(1).firstOrNull { it.startsWith("content-length:", ignoreCase = true) }
            ?.substringAfter(':')?.trim()?.toIntOrNull() ?: 0
        val out = s.getOutputStream()
        fun reply(status: String, type: String, body: String) {
            val bytes = body.toByteArray(Charsets.UTF_8)
            out.write(
                ("HTTP/1.1 $status\r\nContent-Type: $type\r\nContent-Length: ${bytes.size}\r\n" +
                    "Cache-Control: no-store\r\nReferrer-Policy: no-referrer\r\nConnection: close\r\n\r\n").toByteArray(Charsets.ISO_8859_1)
            )
            out.write(bytes)
            out.flush()
        }
        if (path != "/e/$token") return reply("404 Not Found", "text/plain", "")
        when (method) {
            "GET" -> reply("200 OK", "text/html; charset=utf-8", page)
            "POST" -> {
                if (length !in 1..MAX_BODY) return reply("413 Payload Too Large", "application/json", """{"ok":false}""")
                val body = String(readExactly(input, length) ?: return, Charsets.UTF_8)
                val values = PhoneEntry.open(key, body)
                if (values == null) {
                    reply("400 Bad Request", "application/json", """{"ok":false}""")
                    if (++failures >= MAX_FAILURES) runCatching { server?.close() }
                    return
                }
                reply("200 OK", "application/json", """{"ok":true}""")
                main.post { onReceived(values) }
                runCatching { server?.close() }
            }
            else -> reply("405 Method Not Allowed", "text/plain", "")
        }
    }

    /** İstek satırı ve başlıklar (boş satıra kadar); çok uzunsa null. */
    private fun readHead(input: InputStream): String? {
        val buf = ByteArrayOutputStream()
        var last4 = 0
        while (buf.size() < MAX_HEAD) {
            val b = input.read()
            if (b < 0) return null
            buf.write(b)
            last4 = (last4 shl 8) or b
            if (last4 == 0x0D0A0D0A) return buf.toString(Charsets.ISO_8859_1.name()).removeSuffix("\r\n\r\n")
        }
        return null
    }

    private fun readExactly(input: InputStream, n: Int): ByteArray? {
        val out = ByteArray(n)
        var read = 0
        while (read < n) {
            val r = input.read(out, read, n - read)
            if (r < 0) return null
            read += r
        }
        return out
    }

    companion object {
        private const val TAG = "SwitchGuard"
        private const val MAX_HEAD = 8 * 1024
        private const val MAX_BODY = 32 * 1024
        private const val MAX_FAILURES = 10
    }
}
