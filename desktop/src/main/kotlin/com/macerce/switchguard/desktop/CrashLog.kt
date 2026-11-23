package com.macerce.switchguard.desktop

import java.io.PrintWriter
import java.io.StringWriter
import java.nio.file.Files
import java.nio.file.Path
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Yakalanmamış hatalar günlüğe yazılır. Uygulama `javaw` ile, konsolsuz çalışır: stderr'e yazılan yığın izi
 * kaybolur. [install] her iş parçacığının (arayüz/AWT iş parçacığı dahil) yakalanmamış hatasını [Log]'a yönlendirir;
 * [Log.init]'ten hemen sonra çağrılır.
 *
 * Telefon 2.33 (`data/CrashLog` + `ui/CrashPrompt`): hata ayrıca `crashes/` klasörüne tek dosya olarak yazılır (en
 * çok [KEEP]); bir sonraki açılışta görülmemiş en yeni hata için bir kez "rapor gönderilsin mi" diye sorulur.
 */
object CrashLog {
    private const val TAG = "Crash"
    private const val KEEP = 5
    private const val MAX_CHARS = 1500
    @Volatile private var dir: Path? = null

    fun install(dataDir: Path? = null) {
        dir = dataDir?.resolve("crashes")
        Thread.setDefaultUncaughtExceptionHandler { thread, t ->
            Log.w(TAG, "uncaught exception in thread '${thread.name}'", t)
            record(thread.name, t)
        }
    }

    /** Hatayı rapor dosyasına yazar (yazılamazsa sessizce geçer: hata işleyicisi asla fırlatmamalı). */
    fun record(where: String, error: Throwable) {
        val d = dir ?: return
        runCatching {
            Files.createDirectories(d)
            val stack = StringWriter().also { error.printStackTrace(PrintWriter(it)) }.toString()
            val now = Date()
            val name = "crash-" + SimpleDateFormat("yyyyMMdd-HHmmss-SSS", Locale.ROOT).format(now) + ".txt"
            val text = buildString {
                append("SwitchGuard ").append(System.getProperty("jpackage.app-version") ?: "dev").append('\n')
                append(System.getProperty("os.name")).append(' ').append(System.getProperty("os.version"))
                    .append(" · Java ").append(System.getProperty("java.version")).append('\n')
                append(SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).format(now)).append(" · ").append(where).append('\n')
                append(stack.take(MAX_CHARS * 4))
            }
            Files.writeString(d.resolve(name), text)
            Files.list(d).use { s -> s.filter { it.fileName.toString().startsWith("crash-") }.sorted().toList() }
                .dropLast(KEEP).forEach { runCatching { Files.delete(it) } }
        }
    }

    /** Sorulmamış en yeni hata raporu (dosya adı ve metni); yoksa null. */
    fun pending(): Pair<String, String>? = runCatching {
        val d = dir ?: return null
        if (!Files.isDirectory(d)) return null
        val latest = Files.list(d).use { s -> s.map { it.fileName.toString() }.filter { it.startsWith("crash-") }.sorted().toList() }.lastOrNull()
            ?: return null
        val seen = d.resolve("seen").takeIf { Files.exists(it) }?.let { Files.readString(it).trim() }
        if (latest == seen) null else latest to Files.readString(d.resolve(latest))
    }.getOrNull()

    fun markSeen(name: String) {
        val d = dir ?: return
        runCatching { Files.writeString(d.resolve("seen"), name) }
    }

    /** E-posta gövdesine sığacak kısa rapor (mailto adresleri uzun gövdeyi kesebilir). */
    fun short(report: String): String = if (report.length <= MAX_CHARS) report else report.take(MAX_CHARS) + "\n…"
}
