package com.macerce.switchguard.core

/**
 * Bekleme ekranındaki cihaz kartına dokununca ne olacağı: doğrudan aç/kapat, önce onay, ya da hiç (yalnız gösterir).
 * Duvardaki panoda derin dondurucu/pompa gibi kritik bir cihazın tek dokunuşla kapanmasını önlemek için kart başına seçilir.
 * Saklama metni: her satır "<widget id>\t<mod>"; varsayılan (TOGGLE) yazılmaz.
 */
object StandbyControl {
    enum class Mode { TOGGLE, CONFIRM, NONE }

    fun parse(text: String?): Map<String, Mode> =
        text.orEmpty().lineSequence().mapNotNull { line ->
            val i = line.lastIndexOf('\t')
            if (i <= 0) return@mapNotNull null
            val mode = runCatching { Mode.valueOf(line.substring(i + 1)) }.getOrNull() ?: return@mapNotNull null
            line.substring(0, i) to mode
        }.filter { it.second != Mode.TOGGLE }.toMap()

    fun format(modes: Map<String, Mode>): String? =
        modes.filter { it.value != Mode.TOGGLE && '\n' !in it.key && '\t' !in it.key }
            .entries.sortedBy { it.key }.joinToString("\n") { "${it.key}\t${it.value.name}" }.ifEmpty { null }

    fun with(modes: Map<String, Mode>, id: String, mode: Mode): Map<String, Mode> =
        if (mode == Mode.TOGGLE) modes - id else modes + (id to mode)

    /**
     * Pencere parlaklığı (0..1) ya da null (sistemin ayarı). Gece kısma açıkken 23:00–07:00 arası: dokunulmadıkça en
     * kısık, dokununca bir süre okunacak kadar kısık. Gündüz [idleDim] açıksa uzun süre dokunulmayınca kısılır.
     * Alarm varken ekran hep sistem parlaklığında kalır.
     */
    fun brightness(night: Boolean, idle: Boolean, idleDim: Boolean, alarm: Boolean): Float? = when {
        alarm -> null
        night -> if (idle) NIGHT_BRIGHTNESS else IDLE_BRIGHTNESS
        idle && idleDim -> IDLE_BRIGHTNESS
        else -> null
    }

    const val NIGHT_BRIGHTNESS = 0.01f
    const val IDLE_BRIGHTNESS = 0.08f
    /** Bu kadar dokunulmazsa ekran kısılır. */
    const val IDLE_AFTER_MS = 10 * 60_000L
    /** Gönderilen komutun durumu bu sürede gelmezse "bekliyor" görünümü bırakılır. */
    const val PENDING_MS = 10_000L
}
