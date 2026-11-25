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
    ZONE_ENTER, ZONE_EXIT,
    /** Döngü başladı / bitti ya da durduruldu; [Trigger.deviceId] = döngü kimliği ([Cycle]). */
    CYCLE_STARTED, CYCLE_FINISHED,
    /** Cihazın sensör değeri ([Trigger.sensor]) [Trigger.watts] eşiğinin üstüne çıktı / altına indi (ör. oda sıcaklığı). */
    SENSOR_ABOVE, SENSOR_BELOW,
    /** İkili sensör etkinleşti (kapı açıldı, hareket, su kaçağı) / durdu. */
    SENSOR_ACTIVE, SENSOR_CLEAR,
    /** Saat: her seçili günde [Trigger.minutes] (gece yarısından dakika, SS:DD) vaktinde; günler [Trigger.days]. Cihaz yok. */
    TIME;

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
    val usesCycle get() = this == CYCLE_STARTED || this == CYCLE_FINISHED
    val usesSensorValue get() = this == SENSOR_ABOVE || this == SENSOR_BELOW
    val usesSensorState get() = this == SENSOR_ACTIVE || this == SENSOR_CLEAR
    val usesSensor get() = usesSensorValue || usesSensorState
    val isClock get() = this == TIME
    /** Bir cihaza bağlı tetikleyici (gün doğumu, sıcaklık, bölge, döngü ve saat değil). */
    val usesDevice get() = !isSun && !usesTemp && !usesZone && !usesCycle && !isClock
}

data class Trigger(
    val kind: TriggerKind,
    val deviceId: String,
    val channel: Int = 0,
    val minutes: Int = 0,
    val watts: Double = 0.0,
    /** [TriggerKind.CHANNEL_POWER_LOW]: kanal açıldıktan sonra güce bakılmayan süre (kalkış). */
    val graceMinutes: Int = 0,
    /** Sensör tetikleyicilerinde hangi değer. */
    val sensor: SensorKind? = null,
    /** [TriggerKind.TIME]: hangi günler (bit 0 = pazartesi … bit 6 = pazar). */
    val days: Int = ActiveWindow.ALL_DAYS,
)

enum class ActionKind {
    TURN_ON, TURN_OFF, ALARM, NOTIFY,
    /** Sahneyi çalıştır: [AutoAction.deviceId] = sahne kimliği. */
    RUN_SCENE,
    /** Döngüyü başlat / durdur: [AutoAction.deviceId] = döngü kimliği. */
    START_CYCLE, STOP_CYCLE,
    /**
     * Şu anki aç/kapalı durumu hatırla ([AutoAction.deviceId] = tek cihaz ya da sahne kimliği: o sahnenin cihazları;
     * "tüm cihazlar" yoktur) / aynı kuralın hatırladığı duruma geri döndür ([StateSnapshot]).
     */
    SAVE_STATE, RESTORE_STATE,
    /** Açıksa kapat, kapalıysa aç. */
    TOGGLE,
    /** [AutoAction.seconds] saniye bekle. */
    DELAY,
    /** Koşul ([AutoAction.cond]) sağlanana kadar bekle; [AutoAction.seconds] > 0 ise en çok o kadar (sonra [AutoAction.stopOnTimeout]). */
    WAIT_UNTIL,
    /** Eğer [AutoAction.cond] sağlanıyorsa [AutoAction.then], değilse [AutoAction.otherwise]. */
    IF,
    /** [AutoAction.then] adımlarını [AutoAction.count] kez ya da (count = 0) [AutoAction.cond] sağlandıkça tekrarla. */
    REPEAT,
    /** Kuralı burada bitir. */
    STOP,
    /** Başka bir kuralın eylemlerini çalıştır ([AutoAction.deviceId] = kural kimliği; koşullarına bakılmaz). */
    RUN_AUTOMATION,
    /** Metni ntfy / webhook / Telegram'a gönder ([AutoAction.deviceId] = [SendTarget] adı, [AutoAction.text]). */
    SEND,
    /** Işık parlaklığı ([AutoAction.level] %) ve rengi ([AutoAction.color] 0xRRGGBB; -1 = değiştirme). */
    SET_LEVEL,
    /** Sayaca [AutoAction.count] ekle (eksi: çıkar) / sayacı [AutoAction.count] yap ([AutoAction.deviceId] = sayaç). */
    COUNTER_ADD, COUNTER_SET;

    val usesDevice get() = this == TURN_ON || this == TURN_OFF || this == TOGGLE || this == SET_LEVEL
    val usesSnapshot get() = this == SAVE_STATE || this == RESTORE_STATE
    val usesCycle get() = this == START_CYCLE || this == STOP_CYCLE
    val usesCounter get() = this == COUNTER_ADD || this == COUNTER_SET
    /** İçinde başka adımlar olan blok. */
    val isBlock get() = this == IF || this == REPEAT
    /** Akış adımı (bekle, eğer, tekrarla, dur): kendisi bir cihaza dokunmaz. */
    val isFlow get() = isBlock || this == DELAY || this == WAIT_UNTIL || this == STOP
    /** Bildirim/alarm/gönderim metni alır. */
    val usesText get() = this == NOTIFY || this == ALARM || this == SEND
}

/** [ActionKind.SEND] hedefi. */
enum class SendTarget { NTFY, WEBHOOK, TELEGRAM }

/** Kural yeniden tetiklenince, önceki çalışması (beklemeli adımları) sürüyorsa ne olur. */
enum class RunMode {
    /** Öncekini iptal edip baştan başla (2.55 öncesi davranış). */
    RESTART,
    /** Yeni tetiklenişi yok say. */
    SINGLE,
    /** Sıraya al; öncekiler bitince çalışır ([Automation.maxRuns] kadar). */
    QUEUED,
    /** Aynı anda çalıştır ([Automation.maxRuns] kadar). */
    PARALLEL,
}

/** Kural yalnızca evdeyken ya da dışarıdayken geçerli. */
enum class PresenceRule { HOME, AWAY }

/**
 * Kuralın bir adımı. [waitMinutes]: yalnız üst düzey ek eylemlerde, kendinden önceki eylemden sonra beklenecek süre
 * (2.55 öncesi biçim; yeni kurallarda [ActionKind.DELAY] adımı).
 */
data class AutoAction(
    val kind: ActionKind,
    val deviceId: String = "",
    val channel: Int = 0,
    val waitMinutes: Int = 0,
    /** [ActionKind.DELAY] süresi; [ActionKind.WAIT_UNTIL] en uzun bekleme (0 = sınırsız). */
    val seconds: Int = 0,
    /** [ActionKind.IF] / [ActionKind.WAIT_UNTIL] / [ActionKind.REPEAT] (count = 0 iken "sağlandıkça") koşulu. */
    val cond: CondSet? = null,
    /** [ActionKind.IF] "ise" dalı; [ActionKind.REPEAT] gövdesi. */
    val then: List<AutoAction> = emptyList(),
    /** [ActionKind.IF] "değilse" dalı. */
    val otherwise: List<AutoAction> = emptyList(),
    /** [ActionKind.REPEAT] tekrar sayısı; sayaç eylemlerinde değer. */
    val count: Int = 0,
    /** Bildirim/alarm/gönderim metni; boş = varsayılan metin. Yer tutucular: [TextTemplate]. */
    val text: String = "",
    /** [ActionKind.SET_LEVEL] parlaklık %1–100; null = değiştirme. */
    val level: Int? = null,
    /** [ActionKind.SET_LEVEL] renk 0xRRGGBB; null = değiştirme. */
    val color: Int? = null,
    /** [ActionKind.WAIT_UNTIL] süre dolunca kuralı bitir (false: sonraki adımla devam). */
    val stopOnTimeout: Boolean = false,
) {
    /** Bu adım ve içindeki bütün adımlar (önce kendisi; "ise", sonra "değilse"). */
    fun flatten(): List<AutoAction> = listOf(this) + then.flatMap { it.flatten() } + otherwise.flatMap { it.flatten() }

    companion object {
        fun toJson(x: AutoAction): JSONObject = JSONObject().put("kind", x.kind.name).put("device", x.deviceId).put("channel", x.channel).apply {
            if (x.waitMinutes > 0) put("wait", x.waitMinutes)
            if (x.seconds > 0) put("sec", x.seconds)
            x.cond?.let { put("cond", it.toJson()) }
            if (x.then.isNotEmpty()) put("then", listToJson(x.then))
            if (x.otherwise.isNotEmpty()) put("else", listToJson(x.otherwise))
            if (x.count != 0) put("count", x.count)
            if (x.text.isNotEmpty()) put("text", x.text)
            x.level?.let { put("level", it) }
            x.color?.let { put("color", it) }
            if (x.stopOnTimeout) put("stopTo", true)
        }

        fun listToJson(list: List<AutoAction>): JSONArray = JSONArray().apply { list.forEach { put(toJson(it)) } }

        /** Tanınmayan tür (yeni sürümden) hata verir: yarım anlaşılmış adım çalıştırılmaz. */
        fun fromJson(x: JSONObject): AutoAction = AutoAction(
            ActionKind.valueOf(x.getString("kind")), x.optString("device"), x.optInt("channel"), x.optInt("wait"),
            seconds = x.optInt("sec", 0),
            cond = CondSet.fromJson(x.optJSONObject("cond")),
            then = listFromJson(x.optJSONArray("then")),
            otherwise = listFromJson(x.optJSONArray("else")),
            count = x.optInt("count", 0),
            text = x.optString("text"),
            level = if (x.has("level")) x.getInt("level") else null,
            color = if (x.has("color")) x.getInt("color") else null,
            stopOnTimeout = x.optBoolean("stopTo", false),
        )

        fun listFromJson(arr: JSONArray?): List<AutoAction> = arr?.let { a -> (0 until a.length()).map { fromJson(a.getJSONObject(it)) } }.orEmpty()
    }
}

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
    /** Ve şu koşullarda; boş = koşul yok. */
    val conditions: List<Condition> = emptyList(),
    /** Koşulların hepsi mi, herhangi biri mi sağlanmalı. */
    val conditionMatch: ConditionMatch = ConditionMatch.ALL,
    /** Yalnızca evdeyken / dışarıdayken; null = her zaman. */
    val presence: PresenceRule? = null,
    /**
     * İlk eylemden sonra sırayla yapılacak ek eylemler (her biri kendi [AutoAction.waitMinutes] beklemesiyle; içlerinde
     * eğer/tekrarla blokları olabilir). JSON'da ayrı alandadır ("more"): bunu bilmeyen eski sürümler yalnız ilk eylemi
     * çalıştırır.
     */
    val extraActions: List<AutoAction> = emptyList(),
    /**
     * Döngü koruması: kural [AutomationEngine.LOOP_WINDOW_MS] içinde bu kadar kez çalıştıysa bir sonraki tetiklenişte
     * çalışmaz; kuralın açıp kapattığı cihazlar kapatılıp kapalı bırakılır ([LoopGuard]), alarm çalar ve kural kapatılır.
     * 0 = kapalı.
     */
    val loopGuard: Int = 0,
    /** Ek tetikleyiciler: herhangi biri olunca da çalışır ("veya"). JSON'da "triggers2". */
    val extraTriggers: List<Trigger> = emptyList(),
    /** Koşul gruplarının kendi hepsi/herhangi biri kuralı ([Condition.group]). */
    val groupMatch: Map<Int, ConditionMatch> = emptyMap(),
    /** Önceki çalışması sürerken yeniden tetiklenirse. */
    val mode: RunMode = RunMode.RESTART,
    /** [RunMode.QUEUED] / [RunMode.PARALLEL] sınırı. */
    val maxRuns: Int = 3,
    /** Motorun tetikleyici başına kopyasında hangi tetikleyici (kaydedilmez; bkz. [units]). */
    val triggerIndex: Int = 0,
) {
    /** İlk eylem ve ek eylemler, sırayla. */
    val allActions: List<AutoAction> get() = listOf(action) + extraActions

    /** Bütün tetikleyiciler (ilki + ekler). */
    val allTriggers: List<Trigger> get() = listOf(trigger) + extraTriggers

    /** Bütün adımlar düzleştirilmiş (iç içe bloklar dahil); kural izindeki adım numaraları buna göredir. */
    val flatActions: List<AutoAction> get() = allActions.flatMap { it.flatten() }

    val condSet: CondSet get() = CondSet(conditions, conditionMatch, groupMatch)

    /**
     * Motor için tetikleyici başına bir kopya: ilki kendi kimliğiyle, ekler "kimlik~n" ile. Böylece her tetikleyicinin
     * kendi durumu (eşik, süre) ayrı tutulur; çalışan kural [baseId] ile bulunur.
     */
    fun units(): List<Automation> =
        if (extraTriggers.isEmpty()) listOf(this)
        else allTriggers.mapIndexed { i, t -> copy(id = if (i == 0) id else "$id$TRIGGER_SEP$i", trigger = t, extraTriggers = emptyList(), triggerIndex = i) }

    /** Tetiklenen kural şimdi uygulanabilir mi (saat aralığı, ek koşullar, evde/dışarıda). */
    fun allowed(devices: Map<String, DeviceSnapshot>, now: Long, away: Boolean? = null, outdoorTemp: Double? = null): Boolean =
        verdict(devices, now, away, outdoorTemp).ok

    /** [allowed]'ın ayrıntısı (kural izi için): hangi sınama tuttu, hangisi tutmadı. */
    fun verdict(devices: Map<String, DeviceSnapshot>, now: Long, away: Boolean? = null, outdoorTemp: Double? = null): Verdict =
        verdict(devices, CondCtx(now = now, outdoorTemp = outdoorTemp, away = away, triggerIndex = triggerIndex))

    fun verdict(devices: Map<String, DeviceSnapshot>, ctx: CondCtx): Verdict {
        val set = condSet
        val each = set.each(devices, ctx)
        return Verdict(
            windowOk = window?.contains(ctx.now) ?: true,
            conds = each,
            condsOk = set.combine(each),
            presenceOk = presence == null || ctx.away == null || (presence == PresenceRule.AWAY) == ctx.away,
        )
    }

    companion object {
        /** Ek tetikleyici kopyalarının kimlik ayırıcısı ("kural~1"). */
        const val TRIGGER_SEP = '~'

        /** Motor kopyasının (tetikleyici "~n", kategori "@cihaz") ait olduğu kuralın kimliği. */
        fun baseId(id: String): String = id.substringBefore(TRIGGER_SEP).substringBefore('@')

        /** Tetikleyici eki atılmış kimlik (kategori kopyası ayrı kalır): döngü koruması kural başına sayar. */
        fun ruleKey(id: String): String = if (TRIGGER_SEP !in id) id else id.substringBefore(TRIGGER_SEP) +
            id.substringAfter(TRIGGER_SEP).dropWhile { it.isDigit() }

        /**
         * Eski sürümlerin (2.54 ve öncesi, bilgisayar 0.18) okuyamayacağı işaret: kuralın "action" alanına yazılır, gerçek
         * eylem "action2"de durur. Eski okuyucu bu türü tanımaz ve kuralı hiç yüklemez; yanlış çalıştırmaktan iyidir.
         */
        const val NEWER_MARKER = "REQUIRES_SWITCHGUARD_2_55"

        /** 2.55 öncesinde var olan türler. */
        private val LEGACY_ACTIONS = setOf(
            ActionKind.TURN_ON, ActionKind.TURN_OFF, ActionKind.ALARM, ActionKind.NOTIFY, ActionKind.RUN_SCENE,
            ActionKind.START_CYCLE, ActionKind.STOP_CYCLE, ActionKind.SAVE_STATE, ActionKind.RESTORE_STATE,
        )
        private val LEGACY_CONDITIONS = setOf(
            ConditionKind.IS_ON, ConditionKind.IS_OFF, ConditionKind.IS_ONLINE, ConditionKind.IS_OFFLINE,
            ConditionKind.POWER_ABOVE, ConditionKind.POWER_BELOW, ConditionKind.SENSOR_ABOVE, ConditionKind.SENSOR_BELOW,
            ConditionKind.SENSOR_ACTIVE, ConditionKind.SENSOR_CLEAR, ConditionKind.TEMP_ABOVE, ConditionKind.TEMP_BELOW,
        )

        /**
         * Eski bir sürüm bu kuralı farklı (ör. ters) çalıştırır mı: "değilse" ya da gruplu koşul (eski okuyucu bunları
         * sessizce atar), yeni koşul türü, ek tetikleyici, eski sürümün atlayacağı adım (bekle, eğer, dur… ya da içi dolu
         * blok) ya da tekrar tetiklenince başka çalışma kipi. Öyleyse eski sürüm kuralı hiç yüklemesin.
         */
        fun needsNewReader(a: Automation): Boolean =
            a.conditions.any { it.negate || it.group > 0 || it.kind !in LEGACY_CONDITIONS } ||
                a.groupMatch.isNotEmpty() ||
                a.extraTriggers.isNotEmpty() ||
                a.mode != RunMode.RESTART ||
                a.allActions.any { x ->
                    x.kind !in LEGACY_ACTIONS || x.cond != null || x.then.isNotEmpty() || x.otherwise.isNotEmpty() || x.seconds > 0
                }

        fun triggerToJson(t: Trigger): JSONObject = JSONObject()
            .put("kind", t.kind.name).put("device", t.deviceId)
            .put("channel", t.channel).put("minutes", t.minutes).put("watts", t.watts)
            .put("grace", t.graceMinutes)
            .apply {
                t.sensor?.let { put("sensor", it.name) }
                if (t.days != ActiveWindow.ALL_DAYS) put("days", t.days)
            }

        fun triggerFromJson(t: JSONObject) = Trigger(
            TriggerKind.valueOf(t.getString("kind")), t.getString("device"),
            t.optInt("channel"), t.optInt("minutes"), t.optDouble("watts", 0.0),
            t.optInt("grace", 0),
            t.optString("sensor").takeIf { it.isNotEmpty() }?.let { SensorKind.valueOf(it) },
            t.optInt("days", ActiveWindow.ALL_DAYS),
        )

        fun listToJson(list: List<Automation>): String = JSONArray().apply {
            list.forEach { a ->
                put(
                    JSONObject()
                        .put("id", a.id).put("name", a.name).put("enabled", a.enabled).put("delay", a.delayMinutes)
                        .put("trigger", triggerToJson(a.trigger))
                        .apply {
                            if (needsNewReader(a)) {
                                put("action", JSONObject().put("kind", NEWER_MARKER).put("device", "").put("channel", 0))
                                put("action2", AutoAction.toJson(a.action))
                            } else put("action", AutoAction.toJson(a.action))
                            if (a.extraActions.isNotEmpty()) put("more", AutoAction.listToJson(a.extraActions))
                            if (a.extraTriggers.isNotEmpty()) put("triggers2", JSONArray().apply { a.extraTriggers.forEach { put(triggerToJson(it)) } })
                            a.window?.let { put("window", JSONObject().put("start", it.startMinute).put("end", it.endMinute).put("days", it.days)) }
                            if (a.conditions.isNotEmpty()) {
                                put("conds", CondSet.condsToJson(a.conditions))
                                if (a.conditionMatch != ConditionMatch.ALL) put("condMatch", a.conditionMatch.name)
                                if (a.groupMatch.isNotEmpty()) put("groupMatch", CondSet.groupMatchToJson(a.groupMatch))
                            }
                            a.presence?.let { put("presence", it.name) }
                            if (a.loopGuard > 0) put("guard", a.loopGuard)
                            if (a.mode != RunMode.RESTART) put("mode", a.mode.name).put("maxRuns", a.maxRuns)
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
                    Automation(
                        id = o.getString("id"),
                        name = o.optString("name"),
                        enabled = o.optBoolean("enabled", true),
                        delayMinutes = o.optInt("delay", 0),
                        trigger = triggerFromJson(o.getJSONObject("trigger")),
                        // Bilinmeyen ek eylem atlanır (2.35 davranışı); içindeki bloklar kesin okunur.
                        extraActions = o.optJSONArray("more")?.let { arr ->
                            (0 until arr.length()).mapNotNull { j -> runCatching { AutoAction.fromJson(arr.getJSONObject(j)) }.getOrNull() }
                        }.orEmpty(),
                        action = AutoAction.fromJson(o.optJSONObject("action2") ?: o.getJSONObject("action")),
                        window = o.optJSONObject("window")?.let { w ->
                            ActiveWindow(w.optInt("start"), w.optInt("end"), w.optInt("days", ActiveWindow.ALL_DAYS))
                        },
                        // "cond" = 2.30.5 öncesi tek koşul. Bilinmeyen koşul kuralı düşürür (koşulsuz çalışmasın).
                        conditions = (o.optJSONArray("conds")?.let { arr -> (0 until arr.length()).map { arr.getJSONObject(it) } }
                            ?: listOfNotNull(o.optJSONObject("cond"))).map { CondSet.condFromJson(it) },
                        conditionMatch = o.optString("condMatch").takeIf { it.isNotEmpty() }?.let { ConditionMatch.valueOf(it) } ?: ConditionMatch.ALL,
                        groupMatch = CondSet.groupMatchFromJson(o.optJSONObject("groupMatch")),
                        presence = o.optString("presence").takeIf { it.isNotEmpty() }?.let { PresenceRule.valueOf(it) },
                        loopGuard = o.optInt("guard", 0),
                        extraTriggers = o.optJSONArray("triggers2")?.let { arr ->
                            (0 until arr.length()).map { j -> triggerFromJson(arr.getJSONObject(j)) }
                        }.orEmpty(),
                        mode = o.optString("mode").takeIf { it.isNotEmpty() }?.let { RunMode.valueOf(it) } ?: RunMode.RESTART,
                        maxRuns = o.optInt("maxRuns", 3).coerceIn(1, 10),
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
    /** Saat tetikleyicisinin son çalıştığı gün ("kimlik@dakika" → gün başlangıcı; 0 = bugün vakti henüz gelmedi). */
    private val clockFiredDay = mutableMapOf<String, Long>()
    private val pending = mutableListOf<Pending>()
    /** Son verilen ortam (evde/dışarıda); tetiklenen kuralların koşulları buna göre. */
    private var env = Environment()

    /** Kural izi: her tetiklenişin sonucu (çalıştı, gecikti, engellendi). Tek thread'den çağrılır. */
    var onTrace: ((TraceEntry) -> Unit)? = null

    /** Döngü koruması devreye girdi: kural çalıştırılmadı; servis cihazı kapatır, alarm verir, kuralı kapatır. */
    var onLockout: ((Automation) -> Unit)? = null

    /**
     * Kural şu an yeni çalışma kabul etmiyor mu (tek kipte önceki çalışması sürüyor; sıra/aynı anda sınırı dolu).
     * Öyleyse tetikleniş izde "zaten çalışıyordu" olur ve döngü korumasına sayılmaz.
     */
    var busy: ((Automation) -> Boolean)? = null

    /** Kuralın (bütün kopyalarının) bekleyen gecikmeli eylemlerini iptal eder. */
    fun cancelPending(baseId: String) {
        pending.removeAll { Automation.baseId(it.automation.id) == baseId }
    }

    /** Döngü koruması için kuralın son çalışma anları (pencere içindekiler). */
    private val recentRuns = mutableMapOf<String, List<Long>>()

    private fun trace(a: Automation, now: Long, outcome: TraceOutcome, conds: List<Boolean>, current: Map<String, DeviceSnapshot>) {
        val cb = onTrace ?: return
        val t = a.trigger
        val d = current[t.deviceId]
        val value = when {
            t.kind.usesChannelPower -> d?.channelPowerOf(t.channel)
            t.kind.usesWatts -> d?.power
            t.kind.usesSensorValue -> t.sensor?.let { d?.sensors?.get(it) }
            t.kind.usesTemp -> env.outdoorTemp
            else -> null
        }
        cb(TraceEntry(Automation.baseId(a.id), now, outcome, conds, value, env.away, trigger = a.triggerIndex))
    }

    /** [Automation.allowed]; tutmazsa izine "engellendi" yazar (art arda aynıları tek satırda sayılır). */
    /** Koşulların bağlamı: ortam, kanalların ne zamandan beri o durumda olduğu, hava karanlık mı, hangi tetikleyici. */
    fun condCtx(now: Long, triggerIndex: Int? = null): CondCtx = CondCtx(
        now = now, outdoorTemp = env.outdoorTemp, away = env.away,
        sinceOf = { id, ch -> since[key(id, ch)] }, dark = isDark(now, env), triggerIndex = triggerIndex,
    )

    private fun allowedTraced(a: Automation, current: Map<String, DeviceSnapshot>, now: Long): Boolean {
        val v = a.verdict(current, condCtx(now, a.triggerIndex))
        if (!v.ok) trace(a, now, v.outcome, v.conds, current)
        return v.ok
    }

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
                TriggerKind.SUNRISE, TriggerKind.SUNSET, TriggerKind.TEMP_ABOVE, TriggerKind.TEMP_BELOW, TriggerKind.TIME -> false // tick'te bakılır
                TriggerKind.ZONE_ENTER, TriggerKind.ZONE_EXIT -> false // [onZone]
                TriggerKind.CYCLE_STARTED, TriggerKind.CYCLE_FINISHED -> false // [onCycle]
                TriggerKind.SENSOR_ACTIVE, TriggerKind.SENSOR_CLEAR -> {
                    val s = t.sensor
                    val was = s?.let { before?.sensorActive(it) }
                    val now2 = s?.let { after.sensorActive(it) }
                    was != null && now2 != null && was != now2 && now2 == (t.kind == TriggerKind.SENSOR_ACTIVE)
                }
                TriggerKind.SENSOR_ABOVE, TriggerKind.SENSOR_BELOW -> sensorHit(a, after)
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
        val devicesNow = current
        val out = (durationHits(current, automations, now) + channelPowerHits(current, automations, now) +
            powerHoldHits(current, automations, now) + sunHits(current, automations, now, env) +
            tempHits(current, automations, now, env) + clockHits(current, automations, now)).toMutableList()
        val due = pending.filter { it.dueAt <= now }
        pending.removeAll(due)
        // Bekleme sırasında kapatılan ya da silinen otomasyonun eylemi yapılmaz; düzenlendiyse güncel hali kullanılır.
        val current = automations.associateBy { it.id }
        due.mapNotNull { p -> current[p.automation.id]?.takeIf { it.enabled } }.forEach { a ->
            out += a
            trace(a, now, TraceOutcome.RAN, emptyList(), devicesNow)
        }
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
        val clockDue = automations.filter { it.enabled && it.trigger.kind.isClock }
            .mapNotNull { ClockTrigger.nextAt(it.trigger.minutes, it.trigger.days, now) }
        return (durations + lowDue + holdDue + sunDue + clockDue + pending.map { it.dueAt }).minOrNull()
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
            if (!allowedTraced(a, current, now)) continue
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
                if (a.id !in lowFired && now - start >= t.minutes * 60_000L && allowedTraced(a, current, now)) {
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
                if (a.id !in holdFired && now - start >= t.minutes * 60_000L && allowedTraced(a, current, now)) {
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

    /**
     * Saat: seçili günlerde vakti gelince günde bir kez. Vakit [SUN_LATE_MS]'den fazla geçtikten sonra fark edildiyse
     * (telefon kapalıydı) o günkü atlanır. Yeni kurulan (ya da saati değiştirilen) kural, kurulduğu andan önceki
     * vakitte çalışmaz: ilk kez görüldüğünde bugünün vakti geçmişse bugün atlanır.
     */
    private fun clockHits(current: Map<String, DeviceSnapshot>, automations: List<Automation>, now: Long): List<Automation> {
        val fired = mutableListOf<Automation>()
        val today = ClockTrigger.dayStart(now)
        for (a in automations) {
            if (!a.enabled || !a.trigger.kind.isClock) continue
            val key = a.id + "@" + a.trigger.minutes
            val at = ClockTrigger.todayAt(a.trigger.minutes, a.trigger.days, now)
            if (key !in clockFiredDay) clockFiredDay[key] = if (at != null && now >= at) today else 0L
            if (at == null || now < at || clockFiredDay[key] == today) continue
            clockFiredDay[key] = today
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
            if (condition && armed && allowedTraced(a, current, now)) {
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

    /** Döngü başladı ([started]) ya da bitti/durdu: o döngünün kuralları. */
    fun onCycle(
        cycleId: String,
        started: Boolean,
        current: Map<String, DeviceSnapshot>,
        automations: List<Automation>,
        now: Long,
        env: Environment = Environment(),
    ): List<Automation> {
        this.env = env
        val kind = if (started) TriggerKind.CYCLE_STARTED else TriggerKind.CYCLE_FINISHED
        val fired = mutableListOf<Automation>()
        automations.filter { it.enabled && it.trigger.kind == kind && it.trigger.deviceId == cycleId }
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

    /**
     * Sensör eşiği: bir kez tetiklenir, değer türün histerezisi kadar ([SensorKind.hysteresis]; ör. 0,5 °C) geri dönünce
     * yeniden kurulur. Çevrimdışı cihazın eski değeri sayılmaz.
     */
    private fun sensorHit(a: Automation, d: DeviceSnapshot): Boolean {
        val t = a.trigger
        val kind = t.sensor ?: return false
        if (!d.online) return false
        val v = d.sensors[kind] ?: return false
        val above = t.kind == TriggerKind.SENSOR_ABOVE
        val condition = if (above) v > t.watts else v < t.watts
        val rearm = if (above) v <= t.watts - kind.hysteresis else v >= t.watts + kind.hysteresis
        val armed = powerArmed[a.id] ?: true
        return when {
            condition && armed -> { powerArmed[a.id] = false; true }
            rearm -> { powerArmed[a.id] = true; false }
            else -> false
        }
    }

    private fun fire(a: Automation, now: Long, out: MutableList<Automation>, current: Map<String, DeviceSnapshot>) {
        // Saat aralığı ve "ve" koşulu.
        val v = a.verdict(current, condCtx(now, a.triggerIndex))
        if (!v.ok) return trace(a, now, v.outcome, v.conds, current)
        // Birbirini tetikleyen otomasyonların sonsuz döngüye girmesini engeller.
        val last = lastFiredAt[a.id]
        if (last != null && now - last < MIN_INTERVAL_MS) return trace(a, now, TraceOutcome.THROTTLED, v.conds, current)
        lastFiredAt[a.id] = now
        if (busy?.invoke(a) == true) return trace(a, now, TraceOutcome.BUSY, v.conds, current)
        if (a.loopGuard > 0) {
            // Kural başına sayılır (ek tetikleyiciler birlikte); kategori kuralında cihaz başına.
            val rk = Automation.ruleKey(a.id)
            val runs = recentRuns[rk].orEmpty().filter { now - it < LOOP_WINDOW_MS }
            if (runs.size >= a.loopGuard) {
                // Çok sık çalışıyor: bekleyen eylemleri (ör. "geri getir") de iptal et; servis kuralın cihazlarını kapalı bırakır.
                recentRuns.remove(rk)
                pending.removeAll { Automation.ruleKey(it.automation.id) == rk }
                trace(a, now, TraceOutcome.LOCKED_OUT, v.conds, current)
                onLockout?.invoke(a)
                return
            }
            recentRuns[rk] = runs + now
        }
        if (a.delayMinutes > 0) {
            pending += Pending(a, now + a.delayMinutes * 60_000L)
            trace(a, now, TraceOutcome.DELAYED, v.conds, current)
        } else {
            out += a
            trace(a, now, TraceOutcome.RAN, v.conds, current)
        }
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
        .put("clockDay", JSONObject(clockFiredDay.toMap()))
        .put("runs", JSONObject().apply { recentRuns.forEach { (k, v) -> put(k, JSONArray(v)) } })
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
            clockFiredDay.putAll(longs("clockDay"))
            o.optJSONObject("runs")?.let { m -> m.keys().forEach { k -> m.getJSONArray(k).let { a -> recentRuns[k] = (0 until a.length()).map { a.getLong(it) } } } }
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
        /** Döngü korumasının saydığı pencere. */
        const val LOOP_WINDOW_MS = 3_600_000L
        /** Yeni kuralda döngü koruması açılınca varsayılan sınır. */
        const val LOOP_GUARD_DEFAULT = 3
        /** Gün doğumu/batımı bundan geç fark edilirse (telefon kapalıydı) o günkü çalıştırılmaz. */
        const val SUN_LATE_MS = 2 * 3_600_000L
    }
}
