package com.macerce.switchguard.core

import org.json.JSONArray
import org.json.JSONObject

enum class TriggerKind {
    TURNED_ON, TURNED_OFF,
    /** Kanal [Trigger.minutes] dakikadır açık / kapalı. */
    STAYED_ON, STAYED_OFF,
    WENT_OFFLINE, CAME_ONLINE,
    /** Güç [Trigger.watts] değerinin üstüne çıktı / altına indi (yalnızca enerji ölçen cihazlar). */
    POWER_ABOVE, POWER_BELOW,
    /**
     * Kanal açıkken o kanalın gücü [Trigger.watts] altına düştü ve [Trigger.minutes] dakika öyle kaldı
     * (yalnızca kanal başına ölçen cihazlar, ör. SPM-4Relay). Açılıştan sonraki [Trigger.graceMinutes]
     * dakika sayılmaz; kanal kapalıyken hiç değerlendirilmez.
     */
    CHANNEL_POWER_LOW;

    val usesChannel get() = this in setOf(TURNED_ON, TURNED_OFF, STAYED_ON, STAYED_OFF, CHANNEL_POWER_LOW)
    /** Açık/kapalı kalma süresi tetikleyicileri. */
    val usesMinutes get() = this == STAYED_ON || this == STAYED_OFF
    val usesWatts get() = this == POWER_ABOVE || this == POWER_BELOW || this == CHANNEL_POWER_LOW
    /** Kanal başına güç ölçümü gerektirir. */
    val usesChannelPower get() = this == CHANNEL_POWER_LOW
}

data class Trigger(
    val kind: TriggerKind,
    val deviceId: String,
    val channel: Int = 0,
    val minutes: Int = 0,
    val watts: Double = 0.0,
    /** [TriggerKind.CHANNEL_POWER_LOW]: kanal açıldıktan sonra güce bakılmayan süre (kalkış). */
    val graceMinutes: Int = 0,
)

enum class ActionKind {
    TURN_ON, TURN_OFF, ALARM, NOTIFY;

    val usesDevice get() = this == TURN_ON || this == TURN_OFF
}

data class AutoAction(val kind: ActionKind, val deviceId: String = "", val channel: Int = 0)

data class Automation(
    val id: String,
    val name: String,
    val enabled: Boolean,
    val trigger: Trigger,
    val action: AutoAction,
    /** Tetiklendikten sonra eylemden önce beklenecek süre. */
    val delayMinutes: Int = 0,
) {
    companion object {
        fun listToJson(list: List<Automation>): String = JSONArray().apply {
            list.forEach { a ->
                put(
                    JSONObject()
                        .put("id", a.id).put("name", a.name).put("enabled", a.enabled).put("delay", a.delayMinutes)
                        .put("trigger", JSONObject()
                            .put("kind", a.trigger.kind.name).put("device", a.trigger.deviceId)
                            .put("channel", a.trigger.channel).put("minutes", a.trigger.minutes).put("watts", a.trigger.watts)
                            .put("grace", a.trigger.graceMinutes))
                        .put("action", JSONObject()
                            .put("kind", a.action.kind.name).put("device", a.action.deviceId).put("channel", a.action.channel))
                )
            }
        }.toString()

        /** Tanınmayan kayıtlar (ör. eski/yeni sürümden) atlanır. */
        fun listFromJson(json: String): List<Automation> {
            val arr = JSONArray(json)
            return (0 until arr.length()).mapNotNull { i ->
                runCatching {
                    val o = arr.getJSONObject(i)
                    val t = o.getJSONObject("trigger")
                    val a = o.getJSONObject("action")
                    Automation(
                        id = o.getString("id"),
                        name = o.optString("name"),
                        enabled = o.optBoolean("enabled", true),
                        delayMinutes = o.optInt("delay", 0),
                        trigger = Trigger(
                            TriggerKind.valueOf(t.getString("kind")), t.getString("device"),
                            t.optInt("channel"), t.optInt("minutes"), t.optDouble("watts", 0.0),
                            t.optInt("grace", 0),
                        ),
                        action = AutoAction(ActionKind.valueOf(a.getString("kind")), a.optString("device"), a.optInt("channel")),
                    )
                }.getOrNull()
            }
        }
    }
}

/**
 * Otomasyonları değerlendirir. Durumu bellekte tutar (servis ömrü boyunca):
 * kanalların ne zamandan beri o durumda olduğu, güç eşiklerinin kurulu olup olmadığı,
 * bekleyen gecikmeli eylemler. Tüm metotlar tek thread'den çağrılmalı.
 */
class AutomationEngine {

    private data class Pending(val automation: Automation, val dueAt: Long)

    /** "cihaz/kanal" → bu duruma geçtiği an. */
    private val since = mutableMapOf<String, Long>()
    /** Süre tetikleyicisi bu seri için tetiklendi mi (durum değişince sıfırlanır). */
    private val durationFired = mutableSetOf<String>()
    /** Güç tetikleyicisi yeniden tetiklenebilir mi (histerezis). */
    private val powerArmed = mutableMapOf<String, Boolean>()
    private val lastFiredAt = mutableMapOf<String, Long>()
    /** Kanal gücü eşiğin altına indiği an (otomasyon id → zaman). */
    private val lowSince = mutableMapOf<String, Long>()
    /** Bu düşüş için tetiklendi; güç toparlanınca ya da kanal kapanınca yeniden kurulur. */
    private val lowFired = mutableSetOf<String>()
    private val pending = mutableListOf<Pending>()

    /**
     * Yeni cihaz durumlarını işler; şimdi çalıştırılması gereken otomasyonları döndürür.
     * [previous] içinde olmayan cihazlar için anlık (değişim) tetikleyicileri çalışmaz.
     */
    fun onStates(
        previous: Map<String, DeviceSnapshot>,
        current: Map<String, DeviceSnapshot>,
        automations: List<Automation>,
        now: Long,
    ): List<Automation> {
        for ((id, d) in current) {
            for ((ch, on) in d.switches) {
                val key = key(id, ch)
                val was = previous[id]?.switches?.get(ch)
                if (key !in since || (was != null && was != on)) {
                    since[key] = now
                    durationFired.removeAll { it.startsWith("$key#") }
                }
            }
        }

        val fired = mutableListOf<Automation>()
        for (a in automations) {
            if (!a.enabled) continue
            val t = a.trigger
            val before = previous[t.deviceId]
            val after = current[t.deviceId] ?: continue
            val hit = when (t.kind) {
                TriggerKind.TURNED_ON, TriggerKind.TURNED_OFF -> {
                    val target = t.kind == TriggerKind.TURNED_ON
                    val was = before?.switches?.get(t.channel)
                    was != null && was != target && after.switches[t.channel] == target
                }
                TriggerKind.WENT_OFFLINE -> before != null && before.online && !after.online
                TriggerKind.CAME_ONLINE -> before != null && !before.online && after.online
                TriggerKind.POWER_ABOVE, TriggerKind.POWER_BELOW -> powerHit(a, after.power)
                TriggerKind.STAYED_ON, TriggerKind.STAYED_OFF, TriggerKind.CHANNEL_POWER_LOW -> false // tick'te bakılır
            }
            if (hit) fire(a, now, fired)
        }
        fired += durationHits(current, automations, now)
        fired += channelPowerHits(current, automations, now)
        return fired
    }

    /** Süre tetikleyicilerini ve vakti gelen gecikmeli eylemleri döndürür. */
    fun onTick(current: Map<String, DeviceSnapshot>, automations: List<Automation>, now: Long): List<Automation> {
        val out = (durationHits(current, automations, now) + channelPowerHits(current, automations, now)).toMutableList()
        val due = pending.filter { it.dueAt <= now }
        pending.removeAll(due)
        out += due.map { it.automation }
        return out
    }

    /** Bir sonraki süre dolumu ya da gecikmeli eylem zamanı (tick zamanlaması için). */
    fun nextDueAt(current: Map<String, DeviceSnapshot>, automations: List<Automation>): Long? {
        val durations = automations.filter { it.enabled && it.trigger.kind.usesMinutes }.mapNotNull { a ->
            val t = a.trigger
            val on = current[t.deviceId]?.switches?.get(t.channel) ?: return@mapNotNull null
            if (on != (t.kind == TriggerKind.STAYED_ON)) return@mapNotNull null
            if (durationKey(a) in durationFired) return@mapNotNull null
            since[key(t.deviceId, t.channel)]?.plus(t.minutes * 60_000L)
        }
        val lowDue = automations.filter { it.enabled && it.trigger.kind.usesChannelPower }.mapNotNull { a ->
            val t = a.trigger
            if (current[t.deviceId]?.switches?.get(t.channel) != true || a.id in lowFired) return@mapNotNull null
            // Düşüş sürüyorsa süre dolumu; açılış payının bitişini servisin düzenli tick'i yakalar.
            lowSince[a.id]?.plus(t.minutes * 60_000L)
        }
        return (durations + lowDue + pending.map { it.dueAt }).minOrNull()
    }

    private fun durationHits(current: Map<String, DeviceSnapshot>, automations: List<Automation>, now: Long): List<Automation> {
        val fired = mutableListOf<Automation>()
        for (a in automations) {
            val t = a.trigger
            if (!a.enabled || !t.kind.usesMinutes) continue
            val on = current[t.deviceId]?.switches?.get(t.channel) ?: continue
            if (on != (t.kind == TriggerKind.STAYED_ON)) continue
            val start = since[key(t.deviceId, t.channel)] ?: continue
            val dk = durationKey(a)
            if (dk in durationFired || now - start < t.minutes * 60_000L) continue
            durationFired += dk
            fire(a, now, fired)
        }
        return fired
    }

    private fun channelPowerHits(current: Map<String, DeviceSnapshot>, automations: List<Automation>, now: Long): List<Automation> {
        val fired = mutableListOf<Automation>()
        for (a in automations) {
            val t = a.trigger
            if (!a.enabled || !t.kind.usesChannelPower) continue
            val d = current[t.deviceId]
            val power = d?.channelPowerOf(t.channel)
            if (d?.switches?.get(t.channel) != true) {
                // Kanal kapalı (ör. döngünün bekleme dönemi): güç doğal olarak düşer, sayılmaz.
                lowSince.remove(a.id); lowFired.remove(a.id)
                continue
            }
            val onAt = since[key(t.deviceId, t.channel)] ?: continue
            if (power == null || now - onAt < t.graceMinutes * 60_000L) {
                lowSince.remove(a.id)
                continue
            }
            if (power < t.watts) {
                val start = lowSince.getOrPut(a.id) { now }
                if (a.id !in lowFired && now - start >= t.minutes * 60_000L) {
                    lowFired += a.id
                    fire(a, now, fired)
                }
            } else {
                lowSince.remove(a.id)
                // Histerezis: eşiğin belirgin üstüne çıkınca yeniden kurulur.
                if (power >= t.watts + maxOf(t.watts * 0.1, 1.0)) lowFired.remove(a.id)
            }
        }
        return fired
    }

    /** Eşik aşılınca bir kez tetiklenir; değer eşiğin %10 gerisine dönünce yeniden kurulur. */
    private fun powerHit(a: Automation, power: Double?): Boolean {
        power ?: return false
        val t = a.trigger
        val margin = maxOf(t.watts * 0.1, 1.0)
        val (condition, rearm) = if (t.kind == TriggerKind.POWER_ABOVE) {
            (power > t.watts) to (power < t.watts - margin)
        } else {
            (power < t.watts) to (power > t.watts + margin)
        }
        val armed = powerArmed[a.id] ?: true
        return when {
            condition && armed -> { powerArmed[a.id] = false; true }
            rearm -> { powerArmed[a.id] = true; false }
            else -> false
        }
    }

    private fun fire(a: Automation, now: Long, out: MutableList<Automation>) {
        // Birbirini tetikleyen otomasyonların sonsuz döngüye girmesini engeller.
        val last = lastFiredAt[a.id]
        if (last != null && now - last < MIN_INTERVAL_MS) return
        lastFiredAt[a.id] = now
        if (a.delayMinutes > 0) pending += Pending(a, now + a.delayMinutes * 60_000L)
        else out += a
    }

    private fun key(deviceId: String, channel: Int) = "$deviceId/$channel"
    private fun durationKey(a: Automation) = "${key(a.trigger.deviceId, a.trigger.channel)}#${a.id}"

    companion object {
        const val MIN_INTERVAL_MS = 10_000L
    }
}
