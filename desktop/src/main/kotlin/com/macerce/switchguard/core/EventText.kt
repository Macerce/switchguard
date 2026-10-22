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
    /** İzlemenin durduğu aralık (ms, duvar saati). */
    val monitorGap: (from: Long, to: Long) -> String,
    /** İzleme [since]'den beri çalışmıyor. */
    val monitorBlind: (since: Long) -> String,
    /** İzleme yeniden çalışıyor; [from]–[to] arası kördü. */
    val monitorRestored: (from: Long, to: Long) -> String,
    /** Alarm kapatıldı: kaç olay vardı, ilk olaydan kapatılana kadar geçen süre. */
    val alarmDismissed: (count: Int, responseMs: Long) -> String,
    /** Hatırlatma: [label] hâlâ [state]; ilk alarmdan bu yana [durationMs]. */
    val reminder: (label: String, state: String, durationMs: Long) -> String,
    /** Alarm [minutes] dakikadır kapatılmadı: [recipients] kişiye haber verildi ya da ([error]) verilemedi. */
    val escalation: (recipients: Int, minutes: Int, error: String?) -> String,
    /** Elektrik kesilmiş olabilir: izlenen [devices] cihazın hepsi koptu. */
    val outage: (devices: Int) -> String = { "" },
    /** Elektrik geldi: kesinti [from]–[to]. */
    val outageEnded: (from: Long, to: Long) -> String = { _, _ -> "" },
    /** [name] dün ortalamadan belirgin fazla ([high]) ya da az enerji harcadı. */
    val energyAnomaly: (name: String, high: Boolean, kwh: Double, average: Double) -> String = { _, _, _, _ -> "" },
    /** Bu ayın tahmini maliyeti bütçeyi aştı. */
    val budget: (cost: Double, budget: Double) -> String = { _, _ -> "" },
    /** [who] alarmı Telegram'dan üstlendi; [responseMs] = alarmdan bu yana. */
    val telegramAck: (who: String, responseMs: Long) -> String = { _, _ -> "" },
    /** [who] Telegram'dan [label]'ı açtı/kapattı. */
    val telegramSwitch: (who: String, label: String, on: Boolean, error: String?) -> String = { _, _, _, _ -> "" },
    /** Bekçi: [name] [silentMs]'dir sinyal vermiyor ([lost]) ya da yeniden veriyor. */
    val watchdog: (name: String, lost: Boolean, silentMs: Long) -> String = { _, _, _ -> "" },
    /** Bu cihazın şarjı kesildi ([lost]) ya da geri geldi ([ms] süren kesintiden sonra). */
    val charger: (lost: Boolean, ms: Long) -> String = { _, _ -> "" },
    /** Sahne çalıştırıldı: [ok] cihaz değişti, [failed] değişmedi; [by] kim/ne çalıştırdı. */
    val scene: (name: String, ok: Int, failed: Int, by: String?) -> String = { _, _, _, _ -> "" },
    /** Evde/dışarıda değişti ([by] kim) ya da tatil bitti. */
    val presence: (away: Boolean, by: String?, vacationEnded: Boolean) -> String = { _, _, _ -> "" },
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
    /** İzleme [from]–[to] arasında çalışmadı (servis kapatıldı, telefon kapalıydı). */
    data class MonitorGap(val from: Long, val to: Long) : EventArgs()
    /** İzleme [since]'den beri kör (internet/bulut/oturum sorunu). */
    data class MonitorBlind(val since: Long) : EventArgs()
    data class MonitorRestored(val from: Long, val to: Long) : EventArgs()
    /** Alarm kapatıldı; [responseMs] = ilk olaydan kapatılana kadar geçen süre (tepki süresi). */
    data class AlarmDismissed(val count: Int, val responseMs: Long) : EventArgs()
    /** Alarm kapatıldı ama durum sürüyor: [connection] ise bağlantı, değilse [channel] kanalı hâlâ [to]. */
    data class Reminder(val channel: Int, val multi: Boolean, val connection: Boolean, val to: Boolean, val durationMs: Long) : EventArgs() {
        companion object {
            fun of(r: AlarmReminders.Reminder) = when (val c = r.change) {
                is Change.Switch -> Reminder(c.channel, c.multiChannel, connection = false, to = c.to, durationMs = r.durationMs)
                is Change.Connection -> Reminder(0, multi = false, connection = true, to = c.to, durationMs = r.durationMs)
            }
        }
    }

    /** İkinci kişiye haber verme (Telegram). [error] doluysa gönderilemedi. */
    data class Escalation(val recipients: Int, val minutes: Int, val error: String? = null) : EventArgs()

    data class Outage(val devices: Int) : EventArgs()
    data class OutageEnded(val from: Long, val to: Long) : EventArgs()
    /** Dünkü tüketim [kwh], önceki günlerin ortalaması [average]. */
    data class EnergyAnomaly(val high: Boolean, val kwh: Double, val average: Double) : EventArgs()
    data class Budget(val cost: Double, val budget: Double) : EventArgs()
    data class TelegramAck(val who: String, val responseMs: Long) : EventArgs()
    data class TelegramSwitch(val who: String, val channel: Int, val multi: Boolean, val on: Boolean, val error: String? = null) : EventArgs()
    /** [name]: izlenen diğer cihaz (telefon, TV). */
    data class Watchdog(val name: String, val lost: Boolean, val silentMs: Long) : EventArgs()
    data class Charger(val lost: Boolean, val ms: Long) : EventArgs()
    data class SceneRun(val name: String, val ok: Int, val failed: Int, val by: String? = null) : EventArgs()
    data class Presence(val away: Boolean, val by: String? = null, val vacationEnded: Boolean = false) : EventArgs()

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
        is MonitorGap -> s.monitorGap(from, to)
        is MonitorBlind -> s.monitorBlind(since)
        is MonitorRestored -> s.monitorRestored(from, to)
        is AlarmDismissed -> s.alarmDismissed(count, responseMs)
        is Reminder -> {
            val label = if (!connection && multi) s.labels.channel(deviceName, channel + 1) else deviceName
            val state = when {
                connection -> if (to) s.labels.online else s.labels.offline
                else -> if (to) s.labels.on else s.labels.off
            }
            s.reminder(label, state, durationMs)
        }
        is Escalation -> s.escalation(recipients, minutes, error)
        is Outage -> s.outage(devices)
        is OutageEnded -> s.outageEnded(from, to)
        is EnergyAnomaly -> s.energyAnomaly(deviceName, high, kwh, average)
        is Budget -> s.budget(cost, budget)
        is TelegramAck -> s.telegramAck(who, responseMs)
        is TelegramSwitch -> s.telegramSwitch(who, if (multi) s.labels.channel(deviceName, channel + 1) else deviceName, on, error)
        is Watchdog -> s.watchdog(name, lost, silentMs)
        is Charger -> s.charger(lost, ms)
        is SceneRun -> s.scene(name, ok, failed, by)
        is Presence -> s.presence(away, by, vacationEnded)
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
            is MonitorGap -> { put("t", "gap"); put("from", a.from); put("to", a.to) }
            is MonitorBlind -> { put("t", "blind"); put("since", a.since) }
            is MonitorRestored -> { put("t", "restored"); put("from", a.from); put("to", a.to) }
            is AlarmDismissed -> { put("t", "dismissed"); put("n", a.count); put("ms", a.responseMs) }
            is Reminder -> {
                put("t", "reminder"); put("ch", a.channel); put("multi", a.multi); put("conn", a.connection)
                put("to", a.to); put("ms", a.durationMs)
            }
            is Escalation -> { put("t", "escalation"); put("n", a.recipients); put("min", a.minutes); a.error?.let { put("err", it) } }
            is Outage -> { put("t", "outage"); put("n", a.devices) }
            is OutageEnded -> { put("t", "outage_end"); put("from", a.from); put("to", a.to) }
            is EnergyAnomaly -> { put("t", "energy"); put("high", a.high); put("kwh", a.kwh); put("avg", a.average) }
            is Budget -> { put("t", "budget"); put("cost", a.cost); put("budget", a.budget) }
            is TelegramAck -> { put("t", "tg_ack"); put("who", a.who); put("ms", a.responseMs) }
            is TelegramSwitch -> {
                put("t", "tg_switch"); put("who", a.who); put("ch", a.channel); put("multi", a.multi); put("on", a.on)
                a.error?.let { put("err", it) }
            }
            is Watchdog -> { put("t", "watchdog"); put("name", a.name); put("lost", a.lost); put("ms", a.silentMs) }
            is Charger -> { put("t", "charger"); put("lost", a.lost); put("ms", a.ms) }
            is SceneRun -> { put("t", "scene"); put("name", a.name); put("ok", a.ok); put("fail", a.failed); a.by?.let { put("by", it) } }
            is Presence -> { put("t", "presence"); put("away", a.away); a.by?.let { put("by", it) }; put("vacEnd", a.vacationEnded) }
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
                    "gap" -> MonitorGap(o.getLong("from"), o.getLong("to"))
                    "blind" -> MonitorBlind(o.getLong("since"))
                    "restored" -> MonitorRestored(o.getLong("from"), o.getLong("to"))
                    "dismissed" -> AlarmDismissed(o.getInt("n"), o.getLong("ms"))
                    "reminder" -> Reminder(o.getInt("ch"), o.getBoolean("multi"), o.getBoolean("conn"), o.getBoolean("to"), o.getLong("ms"))
                    "escalation" -> Escalation(o.getInt("n"), o.getInt("min"), if (o.has("err")) o.getString("err") else null)
                    "outage" -> Outage(o.getInt("n"))
                    "outage_end" -> OutageEnded(o.getLong("from"), o.getLong("to"))
                    "energy" -> EnergyAnomaly(o.getBoolean("high"), o.getDouble("kwh"), o.getDouble("avg"))
                    "budget" -> Budget(o.getDouble("cost"), o.getDouble("budget"))
                    "tg_ack" -> TelegramAck(o.getString("who"), o.getLong("ms"))
                    "tg_switch" -> TelegramSwitch(
                        o.getString("who"), o.getInt("ch"), o.getBoolean("multi"), o.getBoolean("on"),
                        if (o.has("err")) o.getString("err") else null,
                    )
                    "watchdog" -> Watchdog(o.getString("name"), o.getBoolean("lost"), o.getLong("ms"))
                    "charger" -> Charger(o.getBoolean("lost"), o.getLong("ms"))
                    "scene" -> SceneRun(o.getString("name"), o.getInt("ok"), o.getInt("fail"), if (o.has("by")) o.getString("by") else null)
                    "presence" -> Presence(o.getBoolean("away"), if (o.has("by")) o.getString("by") else null, o.optBoolean("vacEnd"))
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
