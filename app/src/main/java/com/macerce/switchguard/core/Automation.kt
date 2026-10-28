package com.macerce.switchguard.core

import java.util.Calendar
import org.json.JSONArray
import org.json.JSONObject

enum class TriggerKind {
    TURNED_ON, TURNED_OFF,
    /** Kanal [Trigger.minutes] dakikadır açık / kapalı. */
    STAYED_ON, STAYED_OFF,
    WENT_OFFLINE, CAME_ONLINE,
    /**
     * Güç [Trigger.watts] değerinin üstüne çıktı / altına indi (yalnızca enerji ölçen cihazlar). [Trigger.minutes] > 0 ise
     * ancak o kadar dakika kesintisiz öyle kalınca (ör. kompresör 6 saattir durmadı, dondurucu 4 saattir hiç çalışmadı).
     */
    POWER_ABOVE, POWER_BELOW,
    /**
     * Kanal açıkken o kanalın gücü [Trigger.watts] altına düştü ve [Trigger.minutes] dakika öyle kaldı
     * (yalnızca kanal başına ölçen cihazlar, ör. SPM-4Relay). Açılıştan sonraki [Trigger.graceMinutes]
     * dakika sayılmaz; kanal kapalıyken hiç değerlendirilmez.
     */
    CHANNEL_POWER_LOW,
    /** Gün doğumu / batımı; [Trigger.minutes] = kayma (eksi: önce). Konum gerekir, cihaz yok. */
    SUNRISE, SUNSET,
    /** Dış sıcaklık [Trigger.watts] °C'nin üstüne çıktı / altına indi (internetten, yarım saatte bir). Konum gerekir. */
    TEMP_ABOVE, TEMP_BELOW,
    /** Telefon haritada seçilen bölgeye girdi / bölgeden çıktı; [Trigger.deviceId] = bölge kimliği ([Zone]). */
    ZONE_ENTER, ZONE_EXIT;

    val usesChannel get() = this in setOf(TURNED_ON, TURNED_OFF, STAYED_ON, STAYED_OFF, CHANNEL_POWER_LOW)
    /** Açık/kapalı kalma süresi tetikleyicileri. */
    val usesMinutes get() = this == STAYED_ON || this == STAYED_OFF
    val usesWatts get() = this == POWER_ABOVE || this == POWER_BELOW || this == CHANNEL_POWER_LOW
    /** Kanal başına güç ölçümü gerektirir. */
    val usesChannelPower get() = this == CHANNEL_POWER_LOW
    /** İsteğe bağlı "en az şu kadar sürerse" süresi alır. */
    val allowsHold get() = this == POWER_ABOVE || this == POWER_BELOW
    val isSun get() = this == SUNRISE || this == SUNSET
    val usesTemp get() = this == TEMP_ABOVE || this == TEMP_BELOW
    val usesZone get() = this == ZONE_ENTER || this == ZONE_EXIT
    /** Bir cihaza bağlı tetikleyici (gün doğumu, sıcaklık ve bölge değil). */
    val usesDevice get() = !isSun && !usesTemp && !usesZone
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
    TURN_ON, TURN_OFF, ALARM, NOTIFY,
    /** Sahneyi çalıştır: [AutoAction.deviceId] = sahne kimliği. */
    RUN_SCENE;

    val usesDevice get() = this == TURN_ON || this == TURN_OFF
}

/** Kural yalnızca evdeyken ya da dışarıdayken geçerli. */
enum class PresenceRule { HOME, AWAY }

data class AutoAction(val kind: ActionKind, val deviceId: String = "", val channel: Int = 0)

data class Automation(
    val id: String,
    val name: String,
    val enabled: Boolean,
    val trigger: Trigger,
    val action: AutoAction,
    /** Tetiklendikten sonra eylemden önce beklenecek süre. */
    val delayMinutes: Int = 0,
    /** Yalnızca bu saatlerde/günlerde; null = her zaman. */
    val window: ActiveWindow? = null,
    /** Ve şu koşulda; null = koşul yok. */
    val condition: Condition? = null,
    /** Yalnızca evdeyken / dışarıdayken; null = her zaman. */
    val presence: PresenceRule? = null,
) {
    /** Tetiklenen kural şimdi uygulanabilir mi (saat aralığı, ek koşul, evde/dışarıda). */
    fun allowed(devices: Map<String, DeviceSnapshot>, now: Long, away: Boolean? = null): Boolean =
        (window?.contains(now) ?: true) && (condition?.holds(devices) ?: true) &&
            (presence == null || away == null || (presence == PresenceRule.AWAY) == away)

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
                        .apply {
                            a.window?.let { put("window", JSONObject().put("start", it.startMinute).put("end", it.endMinute).put("days", it.days)) }
                            a.condition?.let { put("cond", JSONObject().put("kind", it.kind.name).put("device", it.deviceId).put("channel", it.channel)) }
                            a.presence?.let { put("presence", it.name) }
                        }
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
                        window = o.optJSONObject("window")?.let { w ->
                            ActiveWindow(w.optInt("start"), w.optInt("end"), w.optInt("days", ActiveWindow.ALL_DAYS))
                        },
                        condition = o.optJSONObject("cond")?.let { c ->
                            Condition(ConditionKind.valueOf(c.getString("kind")), c.getString("device"), c.optInt("channel"))
                        },
                        presence = o.optString("presence").takeIf { it.isNotEmpty() }?.let { PresenceRule.valueOf(it) },
                    )
                }.getOrNull()
            }
        }
    }
}

/**
 * Otomasyonları değerlendirir. Durumu bellekte tutar: kanalların ne zamandan beri o durumda olduğu,
 * güç eşiklerinin kurulu olup olmadığı, bekleyen gecikmeli eylemler. Servis yeniden başladığında
 * süreler baştan başlamasın ve gecikmeli eylemler kaybolmasın diye durum [saveState] ile diske yazılır,
 * [restore] ile geri yüklenir. Tüm metotlar tek thread'den çağrılmalı.
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
    /** Süreli güç tetikleyicisinin koşulunun sağlanmaya başladığı an (otomasyon id → zaman). */
    private val holdSince = mutableMapOf<String, Long>()
    /** Bu seri için tetiklendi; koşul belirgin biçimde bozulunca yeniden kurulur. */
    private val holdFired = mutableSetOf<String>()
    /** Gün doğumu/batımı tetikleyicisinin son çalıştığı gün (otomasyon id → gün başlangıcı). */
    private val sunFiredDay = mutableMapOf<String, Long>()
    /** Sıcaklık tetikleyicisi yeniden tetiklenebilir mi (1 °C histerezis). */
    private val tempArmed = mutableMapOf<String, Boolean>()
    private val pending = mutableListOf<Pending>()
    /** Son verilen ortam (evde/dışarıda); tetiklenen kuralların koşulları buna göre. */
    private var env = Environment()

    /**
     * Yeni cihaz durumlarını işler; şimdi çalıştırılması gereken otomasyonları döndürür.
     * [previous] içinde olmayan cihazlar için anlık (değişim) tetikleyicileri çalışmaz.
     */
    fun onStates(
        previous: Map<String, DeviceSnapshot>,
        current: Map<String, DeviceSnapshot>,
        automations: List<Automation>,
        now: Long,
        env: Environment = Environment(),
    ): List<Automation> {
        this.env = env
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
                TriggerKind.POWER_ABOVE, TriggerKind.POWER_BELOW -> t.minutes <= 0 && powerHit(a, after.power)
                TriggerKind.STAYED_ON, TriggerKind.STAYED_OFF, TriggerKind.CHANNEL_POWER_LOW,
                TriggerKind.SUNRISE, TriggerKind.SUNSET, TriggerKind.TEMP_ABOVE, TriggerKind.TEMP_BELOW -> false // tick'te bakılır
                TriggerKind.ZONE_ENTER, TriggerKind.ZONE_EXIT -> false // [onZone]
            }
            if (hit) fire(a, now, fired, current)
        }
        fired += durationHits(current, automations, now)
        fired += channelPowerHits(current, automations, now)
        fired += powerHoldHits(current, automations, now)
        return fired
    }

    /** Süre tetikleyicilerini ve vakti gelen gecikmeli eylemleri döndürür. */
    fun onTick(current: Map<String, DeviceSnapshot>, automations: List<Automation>, now: Long, env: Environment = Environment()): List<Automation> {
        this.env = env
        val out = (durationHits(current, automations, now) + channelPowerHits(current, automations, now) +
            powerHoldHits(current, automations, now) + sunHits(current, automations, now, env) +
            tempHits(current, automations, now, env)).toMutableList()
        val due = pending.filter { it.dueAt <= now }
        pending.removeAll(due)
        // Bekleme sırasında kapatılan ya da silinen otomasyonun eylemi yapılmaz; düzenlendiyse güncel hali kullanılır.
        val current = automations.associateBy { it.id }
        out += due.mapNotNull { p -> current[p.automation.id]?.takeIf { it.enabled } }
        return out
    }

    /** Bir sonraki süre dolumu ya da gecikmeli eylem zamanı (tick zamanlaması için). */
    fun nextDueAt(
        current: Map<String, DeviceSnapshot>,
        automations: List<Automation>,
        env: Environment = Environment(),
        now: Long = System.currentTimeMillis(),
    ): Long? {
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
        val holdDue = automations.filter { it.enabled && it.trigger.kind.allowsHold && it.trigger.minutes > 0 && it.id !in holdFired }
            .mapNotNull { a -> holdSince[a.id]?.plus(a.trigger.minutes * 60_000L) }
        val sunDue = automations.filter { it.enabled && it.trigger.kind.isSun }.mapNotNull { sunTime(it, now, env) }.filter { it > now }
        return (durations + lowDue + holdDue + sunDue + pending.map { it.dueAt }).minOrNull()
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
            // Saat aralığı dışında ya da koşul sağlanmıyorsa harcanmaz: aralık açılınca/koşul sağlanınca tetiklenir.
            if (!a.allowed(current, now, env.away)) continue
            durationFired += dk
            fire(a, now, fired, current)
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
                if (a.id !in lowFired && now - start >= t.minutes * 60_000L && a.allowed(current, now, env.away)) {
                    lowFired += a.id
                    fire(a, now, fired, current)
                }
            } else {
                lowSince.remove(a.id)
                // Histerezis: eşiğin belirgin üstüne çıkınca yeniden kurulur.
                if (power >= t.watts + maxOf(t.watts * 0.1, 1.0)) lowFired.remove(a.id)
            }
        }
        return fired
    }

    /**
     * Süreli güç tetikleyicileri: koşul [Trigger.minutes] boyunca kesintisiz sağlanınca bir kez. Cihaz çevrimdışıyken
     * gücü bilinmez, süre baştan başlar.
     */
    private fun powerHoldHits(current: Map<String, DeviceSnapshot>, automations: List<Automation>, now: Long): List<Automation> {
        val fired = mutableListOf<Automation>()
        for (a in automations) {
            val t = a.trigger
            if (!a.enabled || !t.kind.allowsHold || t.minutes <= 0) continue
            val d = current[t.deviceId]
            val power = d?.power
            if (d == null || !d.online || power == null) {
                holdSince.remove(a.id)
                continue
            }
            val above = t.kind == TriggerKind.POWER_ABOVE
            if (if (above) power > t.watts else power < t.watts) {
                val start = holdSince.getOrPut(a.id) { now }
                if (a.id !in holdFired && now - start >= t.minutes * 60_000L && a.allowed(current, now, env.away)) {
                    holdFired += a.id
                    fire(a, now, fired, current)
                }
            } else {
                holdSince.remove(a.id)
                val margin = maxOf(t.watts * 0.1, 1.0)
                if (if (above) power < t.watts - margin else power > t.watts + margin) holdFired.remove(a.id)
            }
        }
        return fired
    }

    /** Bugünün gün doğumu/batımı + kayma; konum yoksa ya da kutup gününde null. */
    private fun sunTime(a: Automation, now: Long, env: Environment): Long? {
        val lat = env.latitude ?: return null
        val lon = env.longitude ?: return null
        return Sun.time(now, lat, lon, a.trigger.kind == TriggerKind.SUNRISE)?.plus(a.trigger.minutes * 60_000L)
    }

    /**
     * Gün doğumu/batımı: vakti gelince günde bir kez. Vakit [SUN_LATE_MS]'den fazla geçtikten sonra fark edildiyse
     * (telefon kapalıydı) o günkü atlanır: "gün doğunca kapat" öğlen çalışmasın.
     */
    private fun sunHits(current: Map<String, DeviceSnapshot>, automations: List<Automation>, now: Long, env: Environment): List<Automation> {
        val fired = mutableListOf<Automation>()
        val today = Calendar.getInstance().run {
            timeInMillis = now; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0); timeInMillis
        }
        for (a in automations) {
            if (!a.enabled || !a.trigger.kind.isSun || sunFiredDay[a.id] == today) continue
            val at = sunTime(a, now, env) ?: continue
            if (now < at) continue
            sunFiredDay[a.id] = today
            if (now - at <= SUN_LATE_MS) fire(a, now, fired, current)
        }
        return fired
    }

    /** Dış sıcaklık eşiği: bir kez tetiklenir, 1 °C geri dönünce yeniden kurulur. */
    private fun tempHits(current: Map<String, DeviceSnapshot>, automations: List<Automation>, now: Long, env: Environment): List<Automation> {
        val temp = env.outdoorTemp ?: return emptyList()
        val fired = mutableListOf<Automation>()
        for (a in automations) {
            val t = a.trigger
            if (!a.enabled || !t.kind.usesTemp) continue
            val above = t.kind == TriggerKind.TEMP_ABOVE
            val condition = if (above) temp > t.watts else temp < t.watts
            val rearm = if (above) temp < t.watts - 1 else temp > t.watts + 1
            val armed = tempArmed[a.id] ?: true
            if (condition && armed && a.allowed(current, now, env.away)) {
                tempArmed[a.id] = false
                fire(a, now, fired, current)
            } else if (rearm) tempArmed[a.id] = true
        }
        return fired
    }

    /**
     * Telefon [zoneId] bölgesine girdi ([entered]) ya da çıktı: o bölgenin uyan kuralları. Aynı geçiş art arda
     * bildirilirse (konum servisi tekrarlayabilir) [fire]'ın 10 sn sınırı ikinciyi yutar.
     */
    fun onZone(
        zoneId: String,
        entered: Boolean,
        current: Map<String, DeviceSnapshot>,
        automations: List<Automation>,
        now: Long,
        env: Environment = Environment(),
    ): List<Automation> {
        this.env = env
        val kind = if (entered) TriggerKind.ZONE_ENTER else TriggerKind.ZONE_EXIT
        val fired = mutableListOf<Automation>()
        automations.filter { it.enabled && it.trigger.kind == kind && it.trigger.deviceId == zoneId }
            .forEach { fire(it, now, fired, current) }
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

    private fun fire(a: Automation, now: Long, out: MutableList<Automation>, current: Map<String, DeviceSnapshot>) {
        // Saat aralığı ve "ve" koşulu.
        if (!a.allowed(current, now, env.away)) return
        // Birbirini tetikleyen otomasyonların sonsuz döngüye girmesini engeller.
        val last = lastFiredAt[a.id]
        if (last != null && now - last < MIN_INTERVAL_MS) return
        lastFiredAt[a.id] = now
        if (a.delayMinutes > 0) pending += Pending(a, now + a.delayMinutes * 60_000L)
        else out += a
    }

    private fun key(deviceId: String, channel: Int) = "$deviceId/$channel"
    private fun durationKey(a: Automation) = "${key(a.trigger.deviceId, a.trigger.channel)}#${a.id}"

    /** Kalıcı durum (zamanlar duvar saatiyle, telefon yeniden başlasa da geçerli). */
    fun saveState(): String = JSONObject()
        .put("since", JSONObject(since.toMap()))
        .put("durFired", JSONArray(durationFired.toList()))
        .put("armed", JSONObject(powerArmed.toMap()))
        .put("last", JSONObject(lastFiredAt.toMap()))
        .put("lowSince", JSONObject(lowSince.toMap()))
        .put("lowFired", JSONArray(lowFired.toList()))
        .put("holdSince", JSONObject(holdSince.toMap()))
        .put("holdFired", JSONArray(holdFired.toList()))
        .put("sunDay", JSONObject(sunFiredDay.toMap()))
        .put("tempArmed", JSONObject(tempArmed.toMap()))
        .put("pending", JSONArray().apply { pending.forEach { put(JSONObject().put("id", it.automation.id).put("due", it.dueAt)) } })
        .toString()

    /**
     * [saveState] çıktısını geri yükler. Bekleyen eylemler [automations]'daki güncel tanımla eşleştirilir;
     * bu arada silinen otomasyonunki atılır. Bozuk kayıt yok sayılır.
     */
    fun restore(json: String?, automations: List<Automation>) {
        val o = runCatching { JSONObject(json ?: return) }.getOrNull() ?: return
        fun longs(name: String) = o.optJSONObject(name)?.let { m -> m.keys().asSequence().associateWith { m.getLong(it) } }.orEmpty()
        fun strings(name: String) = o.optJSONArray(name)?.let { a -> (0 until a.length()).map { a.getString(it) } }.orEmpty()
        runCatching {
            since.putAll(longs("since"))
            durationFired += strings("durFired")
            o.optJSONObject("armed")?.let { m -> m.keys().forEach { powerArmed[it] = m.getBoolean(it) } }
            lastFiredAt.putAll(longs("last"))
            lowSince.putAll(longs("lowSince"))
            lowFired += strings("lowFired")
            holdSince.putAll(longs("holdSince"))
            holdFired += strings("holdFired")
            sunFiredDay.putAll(longs("sunDay"))
            o.optJSONObject("tempArmed")?.let { m -> m.keys().forEach { tempArmed[it] = m.getBoolean(it) } }
            val byId = automations.associateBy { it.id }
            val arr = o.optJSONArray("pending") ?: JSONArray()
            for (i in 0 until arr.length()) {
                val p = arr.getJSONObject(i)
                byId[p.getString("id")]?.let { pending += Pending(it, p.getLong("due")) }
            }
        }
    }

    companion object {
        const val MIN_INTERVAL_MS = 10_000L
        /** Gün doğumu/batımı bundan geç fark edilirse (telefon kapalıydı) o günkü çalıştırılmaz. */
        const val SUN_LATE_MS = 2 * 3_600_000L
    }
}
