package com.macerce.switchguard.core

/**
 * Yerel hata kaydı biçimi. Uygulama çökünce yığın telefonda bir dosyaya yazılır; hiçbir yere kendiliğinden
 * gönderilmez. Kullanıcı isterse "Hata raporu gönder" ile e-postaya eklenir (gizlilik: veri toplamıyoruz).
 */
object CrashReport {
    data class Meta(val appVersion: String, val versionCode: Int, val android: String, val sdk: Int, val device: String)

    const val KEEP = 5

    fun header(meta: Meta) =
        "SwitchGuard ${meta.appVersion} (${meta.versionCode})\nAndroid ${meta.android} (API ${meta.sdk})\n${meta.device}\n"

    fun format(meta: Meta, time: String, thread: String, stack: String): String =
        header(meta) + "Time: $time\nThread: $thread\n\n" + stack.trimEnd() + "\n"

    /** Dosya adları zamana göre sıralanabilir ("crash-<ms>.txt"); en yeni [keep] tanesi dışındakiler silinir. */
    fun toDelete(names: List<String>, keep: Int = KEEP): List<String> = names.sorted().dropLast(keep)

    /** E-posta metni: ortam, tanılama satırları ve en yeniden eskiye hata kayıtları; [maxChars]'ta kesilir. */
    fun body(meta: Meta, diagnostics: List<String>, reports: List<String>, maxChars: Int): String {
        val text = buildString {
            append(header(meta)).append('\n')
            diagnostics.forEach { append(it).append('\n') }
            reports.asReversed().forEachIndexed { i, r ->
                append("\n----- ").append(i + 1).append(" -----\n").append(r)
            }
        }
        return if (text.length <= maxChars) text else text.take(maxChars - 1) + "…"
    }
}
