package com.macerce.switchguard.core

import org.json.JSONObject

/** Olay metinlerinde kullanılan, dile göre değişen kalıplar. */
class EventStrings(
    val labels: ChangeLabels,
    val shortDrop: (name: String) -> String,
    val test: String,
    val automationFired: (automation: String) -> String,
    val deviceMissing: String,
    val automationFailed: (text: String, error: String) -> String,
    val powerStale: (name: String) -> String,
)

/**
 * Bir geçmiş kaydının dilden bağımsız tarifi. Kayıtta JSON olarak saklanır ve
 * Geçmiş her gösterildiğinde seçili dille yeniden yazılır.
 * Kanal numaraları 0'dan başlar (cihazdaki "outlet" gibi).
 */
sealed class EventArgs {
    data class Switch(val channel: Int, val multi: Boolean, val from: Boolean, val to: Boolean) : EventArgs()
    data class Connection(val from: Boolean, val to: Boolean) : EventArgs()
    data object ShortDrop : EventArgs()
    data object Test : EventArgs()
    data object PowerStale : EventArgs()
    data class AutoMissing(val automation: String) : EventArgs()
    data class AutoSwitch(
        val automation: String, val channel: Int, val multi: Boolean, val on: Boolean,
        /** Cihaza gönderilemediyse hata mesajı (sunucudan geldiği gibi, çevrilmez). */
        val error: String? = null,
    ) : EventArgs()
    data class AutoAlert(val automation: String, val source: String?) : EventArgs()

    /** [deviceName]: kayıttaki cihaz adı. */
    fun render(deviceName: String, s: EventStrings): String = when (this) {
        is Switch -> Change.Switch("", deviceName, channel, multi, from, to).describe(s.labels)
        is Connection -> Change.Connection("", deviceName, from, to).describe(s.labels)
        ShortDrop -> s.shortDrop(deviceName)
        Test -> s.test
        PowerStale -> s.powerStale(deviceName)
        is AutoMissing -> "${s.automationFired(automation)}: ${s.deviceMissing}"
        is AutoSwitch -> {
            val label = if (multi) s.labels.channel(deviceName, channel + 1) else deviceName
            val text = "${s.automationFired(automation)}: $label → ${if (on) s.labels.on else s.labels.off}"
            if (error != null) s.automationFailed(text, error) else text
        }
        is AutoAlert -> s.automationFired(automation).let { if (source != null) "$it ($source)" else it }
    }

    fun toJson(): String = JSONObject().apply {
        when (val a = this@EventArgs) {
            is Switch -> { put("t", "switch"); put("ch", a.channel); put("multi", a.multi); put("from", a.from); put("to", a.to) }
            is Connection -> { put("t", "conn"); put("from", a.from); put("to", a.to) }
            ShortDrop -> put("t", "short_drop")
            Test -> put("t", "test")
            PowerStale -> put("t", "power_stale")
            is AutoMissing -> { put("t", "auto_missing"); put("auto", a.automation) }
            is AutoSwitch -> {
                put("t", "auto_switch"); put("auto", a.automation); put("ch", a.channel); put("multi", a.multi); put("on", a.on)
                a.error?.let { put("err", it) }
            }
            is AutoAlert -> { put("t", "auto_alert"); put("auto", a.automation); a.source?.let { put("src", it) } }
        }
    }.toString()

    companion object {
        fun parse(json: String?): EventArgs? {
            if (json.isNullOrEmpty()) return null
            return runCatching {
                val o = JSONObject(json)
                when (o.getString("t")) {
                    "switch" -> Switch(o.getInt("ch"), o.getBoolean("multi"), o.getBoolean("from"), o.getBoolean("to"))
                    "conn" -> Connection(o.getBoolean("from"), o.getBoolean("to"))
                    "short_drop" -> ShortDrop
                    "test" -> Test
                    "power_stale" -> PowerStale
                    "auto_missing" -> AutoMissing(o.getString("auto"))
                    "auto_switch" -> AutoSwitch(
                        o.getString("auto"), o.getInt("ch"), o.getBoolean("multi"), o.getBoolean("on"),
                        if (o.has("err")) o.getString("err") else null,
                    )
                    "auto_alert" -> AutoAlert(o.getString("auto"), if (o.has("src")) o.getString("src") else null)
                    else -> null
                }
            }.getOrNull()
        }

        /**
         * Tarifi olmayan eski bir kaydın metnini, olası dillerin kalıplarıyla yeniden üretip
         * karşılaştırarak tarifini bulur. Tanınmazsa null (metin olduğu gibi gösterilir).
         */
        fun fromLegacy(kind: String, deviceName: String, text: String, languages: List<EventStrings>): EventArgs? {
            val candidates = buildList {
                for (from in listOf(false, true)) for (to in listOf(false, true)) {
                    add(Connection(from, to))
                    for (multi in listOf(false, true)) for (ch in 0 until if (multi) MAX_CHANNELS else 1) add(Switch(ch, multi, from, to))
                }
                add(ShortDrop); add(Test); add(PowerStale)
            }
            for (s in languages) {
                candidates.firstOrNull { it.render(deviceName, s) == text }?.let { return it }
                if (kind == "AUTOMATION") automationFromLegacy(deviceName, text, s)?.let { return it }
            }
            return null
        }

        private fun automationFromLegacy(deviceName: String, text: String, s: EventStrings): EventArgs? {
            // "Otomasyon “ad”" kalıbının ad öncesi ve sonrası; ad her şeyi içerebilir, bu yüzden tüm bitişler denenir.
            val marker = "\u0000"
            val (prefix, suffix) = s.automationFired(marker).split(marker).let { it[0] to it.getOrElse(1) { "" } }
            if (!text.startsWith(prefix)) return null
            var end = text.indexOf(suffix, prefix.length)
            while (end >= 0) {
                val name = text.substring(prefix.length, end)
                val tries = buildList {
                    add(AutoAlert(name, null)); add(AutoAlert(name, deviceName)); add(AutoMissing(name))
                    for (on in listOf(false, true)) {
                        add(AutoSwitch(name, 0, false, on))
                        for (ch in 0 until MAX_CHANNELS) add(AutoSwitch(name, ch, true, on))
                    }
                }
                tries.firstOrNull { it.render(deviceName, s) == text }?.let { return it }
                // Başarısız anahtarlama: "<metin> — başarısız: <hata>"
                val (fPrefix, fSep) = s.automationFailed(marker, marker).split(marker).let { it[0] to it[1] }
                if (fPrefix.isEmpty()) {
                    var sep = text.indexOf(fSep)
                    while (sep >= 0) {
                        val inner = text.substring(0, sep)
                        val err = text.substring(sep + fSep.length)
                        tries.filterIsInstance<AutoSwitch>().firstOrNull { it.render(deviceName, s) == inner }
                            ?.let { return it.copy(error = err) }
                        sep = text.indexOf(fSep, sep + 1)
                    }
                }
                end = if (suffix.isEmpty()) -1 else text.indexOf(suffix, end + 1)
            }
            return null
        }

        private const val MAX_CHANNELS = 8
    }
}

fun Change.toArgs(): EventArgs = when (this) {
    is Change.Switch -> EventArgs.Switch(channel, multiChannel, from, to)
    is Change.Connection -> EventArgs.Connection(from, to)
}
