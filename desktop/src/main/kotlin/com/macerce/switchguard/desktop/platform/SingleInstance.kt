package com.macerce.switchguard.desktop.platform

import com.macerce.switchguard.desktop.Log
import com.sun.jna.Native
import com.sun.jna.win32.StdCallLibrary
import com.sun.jna.win32.W32APIOptions
import java.io.IOException
import java.io.InputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.channels.OverlappingFileLockException
import java.nio.charset.StandardCharsets.US_ASCII
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption.CREATE
import java.nio.file.StandardOpenOption.WRITE

/**
 * Aynı Windows kullanıcısı için tek uygulama örneği. [dir] (`%APPDATA%\SwitchGuard`) içindeki `instance.lock`
 * dosyasına `FileChannel.tryLock` ile kilit alınır; kilit süreç ölünce işletim sistemince bırakılır, bu yüzden
 * çöken bir örnek yeni açılışı engellemez. Kilidi alan ilk örnek 127.0.0.1'de rastgele bir porta dinler ve portu
 * `instance.port`'a yazar; ikinci örnek o porta `show\n` gönderip `false` döner ([acquire]), çağıran çıkar.
 * Aynı JVM'de ikinci bir [SingleInstance] da kilitli sayılır (`OverlappingFileLockException`).
 *
 * Kilit dosyasına hiçbir şey yazılmaz (Windows'ta kilitli bölgeye yazmak başka tutamaçlarda hata verir); port ayrı
 * bir dosyadadır. Windows dışında dosya kilidi yine çalışır, ama uygulama yalnızca Windows için üretilir.
 */
class SingleInstance(private val dir: Path) {
    private val lockFile = dir.resolve("instance.lock")
    private val portFile = dir.resolve("instance.port")

    private var channel: FileChannel? = null
    private var lock: FileLock? = null
    private var server: ServerSocket? = null

    /**
     * Kilidi almaya çalışır. Alırsa `true` döner ve [onShowRequest], bir sonraki örnek "göster" istediğinde çağrılır
     * (**dinleyici iş parçacığından**, arayüz iş parçacığından değil; çağıran gerekirse iş parçacığı değiştirir,
     * geri çağrının iş parçacığı güvenli olması beklenir). Kilit başkasındaysa sahibine "göster" isteği gönderir
     * ve `false` döner. Dizin ya da kilit dosyası hazırlanamazsa [IOException] atar: kilitsiz başlanmaz.
     */
    @Synchronized
    fun acquire(onShowRequest: () -> Unit): Boolean {
        if (lock != null) return true
        Files.createDirectories(dir)
        val ch = FileChannel.open(lockFile, CREATE, WRITE)
        val acquired = try {
            ch.tryLock()
        } catch (_: OverlappingFileLockException) {
            null // aynı JVM'de başka bir SingleInstance tutuyor
        } catch (e: IOException) {
            ch.close()
            throw e
        }
        if (acquired == null) {
            ch.close()
            signalOwner()
            return false
        }
        channel = ch
        lock = acquired
        listen(onShowRequest)
        return true
    }

    /** Kilidi, dinleyiciyi ve port dosyasını bırakır; kilit alınmadıysa ya da zaten bırakıldıysa bir şey yapmaz. */
    @Synchronized
    fun release() {
        val held = lock ?: return
        lock = null
        runCatching { server?.close() }
        server = null
        runCatching { Files.deleteIfExists(portFile) }
        runCatching { held.release() }
        runCatching { channel?.close() }
        channel = null
    }

    // ---------------------------------------------------------------- sahip tarafı

    /** Yerel dinleyiciyi kurar. Kurulamazsa kilit yine de tutulur: ikinci örnek çıkar ama pencereyi öne getiremez. */
    private fun listen(onShowRequest: () -> Unit) {
        try {
            // Eski (çöken bir örnekten kalan) port ikinci örneği yanlış yere yönlendirmesin.
            Files.deleteIfExists(portFile)
            val srv = ServerSocket(0, BACKLOG, LOOPBACK)
            server = srv
            Files.writeString(portFile, srv.localPort.toString(), US_ASCII)
            Thread({ serve(srv, onShowRequest) }, "single-instance").apply { isDaemon = true }.start()
        } catch (e: IOException) {
            Log.w(TAG, "could not start the show-request listener", e)
        }
    }

    private fun serve(srv: ServerSocket, onShowRequest: () -> Unit) {
        while (!srv.isClosed) {
            try {
                srv.accept().use { socket ->
                    socket.soTimeout = READ_TIMEOUT_MS
                    if (readCommand(socket.getInputStream()) == SHOW) {
                        Log.i(TAG, "another instance asked to show the window")
                        try {
                            onShowRequest()
                        } catch (t: Throwable) {
                            Log.w(TAG, "show request handler failed", t)
                        }
                    }
                }
            } catch (_: IOException) {
                // kapatma (accept kesilir), zaman aşımı ya da kopan bağlantı: döngü isClosed ile biter
            }
        }
    }

    /** İlk satırı okur; en fazla [MAX_COMMAND] bayt, uzun/sonsuz yazan istemci dinleyiciyi tutamaz. */
    private fun readCommand(input: InputStream): String {
        val buffer = ByteArray(MAX_COMMAND)
        var n = 0
        while (n < buffer.size) {
            val read = input.read(buffer, n, buffer.size - n)
            if (read < 0) break
            n += read
            if (buffer[n - 1] == '\n'.code.toByte()) break
        }
        return String(buffer, 0, n, US_ASCII).trim()
    }

    // ---------------------------------------------------------------- ikinci örnek tarafı

    /**
     * Sahibe "göster" gönderir. Sahip kilidi almış ama dinleyiciyi henüz kurmamış olabilir (yeni açılıyor):
     * kısa aralıklarla birkaç kez denenir.
     */
    private fun signalOwner() {
        repeat(ATTEMPTS) { attempt ->
            if (sendShow()) return
            if (attempt < ATTEMPTS - 1) Thread.sleep(RETRY_DELAY_MS)
        }
        Log.w(TAG, "the running instance did not answer the show request")
    }

    private fun sendShow(): Boolean = try {
        allowForeground()
        val port = Files.readString(portFile, US_ASCII).trim().toInt()
        Socket().use { socket ->
            socket.connect(InetSocketAddress(LOOPBACK, port), CONNECT_TIMEOUT_MS)
            socket.getOutputStream().apply {
                write("$SHOW\n".toByteArray(US_ASCII))
                flush()
            }
        }
        true
    } catch (_: IOException) {
        false // dosya yok ya da yarım, port kapalı
    } catch (_: NumberFormatException) {
        false
    }

    /**
     * Kullanıcı ikinci örneği kendisi başlattı: Windows'un ön plan kilidi o sürece öne geçme hakkı verir, ilk örnek
     * ise arka plandadır ve `toFront` yalnızca görev çubuğu düğmesini yanıp söndürür. Hak, sahibe devredilir
     * (`ASFW_ANY`). Hakkı olmayan bir süreçte (ör. betikten başlatılmış) çağrı sessizce başarısız olur.
     */
    private fun allowForeground() {
        if (!IS_WINDOWS) return
        try {
            User32Ext.INSTANCE.AllowSetForegroundWindow(ASFW_ANY)
        } catch (t: Throwable) {
            Log.w(TAG, "AllowSetForegroundWindow failed", t)
        }
    }

    private interface User32Ext : StdCallLibrary {
        fun AllowSetForegroundWindow(processId: Int): Boolean

        companion object {
            val INSTANCE: User32Ext = Native.load("user32", User32Ext::class.java, W32APIOptions.DEFAULT_OPTIONS)
        }
    }

    private companion object {
        const val TAG = "SingleInstance"
        const val ASFW_ANY = -1
        val IS_WINDOWS = System.getProperty("os.name").startsWith("Windows")
        const val SHOW = "show"
        const val BACKLOG = 8
        const val MAX_COMMAND = 16
        const val READ_TIMEOUT_MS = 2000
        const val CONNECT_TIMEOUT_MS = 1000
        const val ATTEMPTS = 6
        const val RETRY_DELAY_MS = 300L

        /** `localhost` çözümlemesine (IPv6 `::1`) bağlı kalmamak için sayısal 127.0.0.1. */
        val LOOPBACK: InetAddress = InetAddress.getByAddress(byteArrayOf(127, 0, 0, 1))
    }
}
