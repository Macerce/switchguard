package com.macerce.switchguard.desktop

import java.io.IOException
import java.io.PrintWriter
import java.io.StringWriter
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.nio.file.StandardOpenOption.APPEND
import java.nio.file.StandardOpenOption.CREATE
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Küçük dosya günlüğü (UTF-8): `<dir>\logs\switchguard.log`, 5 MB dolunca `.1`, `.2` olarak döner; en fazla 3 dosya.
 * [init] çağrılmadan önce (ya da dosyaya yazılamazsa) satırlar yalnızca stderr'e gider.
 * Çağıranlar gizli bilgi (token, anahtar, sır) geçirmemekle yükümlüdür; buraya yalnızca ileti ve istisna yazılır.
 */
object Log {
    private const val MAX_BYTES = 5L * 1024 * 1024
    private const val FILE_NAME = "switchguard.log"

    private val lock = Any()
    private val stamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
    private var file: Path? = null

    fun init(dir: Path) {
        synchronized(lock) {
            file = try {
                val logs = dir.resolve("logs")
                Files.createDirectories(logs)
                logs.resolve(FILE_NAME)
            } catch (e: IOException) {
                System.err.println("Log init failed: $e")
                null
            }
        }
    }

    fun i(tag: String, msg: String) = write("I", tag, msg, null)

    fun w(tag: String, msg: String, t: Throwable? = null) = write("W", tag, msg, t)

    private fun write(level: String, tag: String, msg: String, t: Throwable?) {
        val line = buildString {
            append(stamp.format(LocalDateTime.now())).append(' ').append(level).append('/').append(tag)
            append(": ").append(msg).append('\n')
            if (t != null) append(StringWriter().also { t.printStackTrace(PrintWriter(it)) })
        }
        synchronized(lock) {
            val f = file
            if (f == null) {
                System.err.print(line)
                return
            }
            try {
                val bytes = line.toByteArray(UTF_8)
                rotateIfNeeded(f, bytes.size)
                Files.write(f, bytes, CREATE, APPEND)
            } catch (e: IOException) {
                System.err.print(line)
            }
        }
    }

    private fun rotateIfNeeded(f: Path, incoming: Int) {
        if (!Files.exists(f) || Files.size(f) + incoming <= MAX_BYTES) return
        val one = f.resolveSibling("$FILE_NAME.1")
        val two = f.resolveSibling("$FILE_NAME.2")
        if (Files.exists(one)) Files.move(one, two, REPLACE_EXISTING)
        Files.move(f, one, REPLACE_EXISTING)
    }
}
