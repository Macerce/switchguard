package com.macerce.switchguard.desktop.engine

import com.macerce.switchguard.api.WeatherClient
import com.macerce.switchguard.core.ChatCommand
import com.macerce.switchguard.core.ChatTemplate
import com.macerce.switchguard.core.Cycle
import com.macerce.switchguard.core.CycleEnd
import com.macerce.switchguard.core.CycleEngine
import com.macerce.switchguard.core.CyclePreset
import com.macerce.switchguard.core.CycleStep
import com.macerce.switchguard.core.CycleTarget
import com.macerce.switchguard.core.DeviceGroup
import com.macerce.switchguard.core.DeviceKinds
import com.macerce.switchguard.core.DeviceSnapshot
import com.macerce.switchguard.core.DurationText
import com.macerce.switchguard.core.EnergyWatch
import com.macerce.switchguard.core.Scene
import com.macerce.switchguard.core.SensorKind
import com.macerce.switchguard.core.StandbyBoard
import com.macerce.switchguard.core.StandbyBoard.UpcomingKind
import com.macerce.switchguard.data.EventLog
import com.macerce.switchguard.data.MetricsLog
import com.macerce.switchguard.data.RuntimeState
import com.macerce.switchguard.data.Store
import com.macerce.switchguard.desktop.i18n.Strings
import com.macerce.switchguard.desktop.ui.automations.formatSensor
import com.macerce.switchguard.desktop.ui.automations.labelKey
import com.macerce.switchguard.desktop.ui.automations.stateKey
import java.util.UUID
import kotlin.math.roundToInt

/**
 * Sohbet komutlarını yürütür ve yanıt metnini üretir (telefondaki `service/ChatRunner.kt` + `service/Briefing.kt`);
 * Telegram ve uygulama içi sohbet aynı yolu kullanır. Motor iş parçacığında çağrılır: aç/kapat, sahne, döngü ve
 * evde/dışarıda motorun kendi işlevleriyle yapılır ([switchNow], [sceneNow], [cycleNow], [awayNow]).
 *
 * Telefondan farklar: ağ işi burada yapılmaz (motor beklemesin); dış sıcaklığı ve günün havasını motor okuyup
 * [temperature] / [run]'a verir. Sensör soruları ([ChatCommand.Intent.Sensors]) yalnız masaüstünde.
 */
class ChatRunner(
    private val store: Store,
    private val metrics: MetricsLog,
    private val runtime: RuntimeState,
    private val log: EventLog,
    private val strings: () -> Strings,
    private val now: () -> Long,
    /** Cihazı ya da grubu açar/kapatır; başarısızsa hata fırlatır. */
    private val switchNow: (DeviceSnapshot, Int, Boolean) -> Unit,
    private val sceneNow: (Scene, String?) -> Pair<Int, Int>,
    private val awayNow: (Boolean, String?) -> Unit,
    /** Döngüyü başlatır / durdurur; döngü yoksa false. */
    private val cycleNow: (String, Boolean) -> Boolean = { _, _ -> false },
) {

    /** [ask]: yanıt bir soru (hangi kanal, emin misiniz); sesli kullanımda dinleme yeniden açılır. */
    data class Reply(val text: String, val ask: Boolean = false)

    /**
     * Yanıt bekleyen soru. [all] true: "hepsini kapat/aç" onayı. [target] dolu: kanalı ya da adı netleşmemiş aç/kapat;
     * sıradaki mesaj komut değilse eksik parça sayılır ("iki", "salon ışık").
     */
    private class Pending(val at: Long, val on: Boolean, val all: Boolean, val target: String? = null, val needChannel: Boolean = false)

    private val pending = HashMap<String, Pending>()

    /** [by]'ın yanıt beklenen bir sorusu var mı (sesli kullanımda dinleme yeniden açılsın). */
    fun asking(by: String?): Boolean = pending[by.orEmpty()]?.let { now() - it.at < PENDING_MS } == true

    /**
     * Komut olarak anlaşılmayan mesaj bekleyen sorunun yanıtı olabilir: "Hangi kanal?" → "iki", "Hangisi?" →
     * "salon ışık". Öyleyse tamamlanmış aç/kapat komutu, değilse null.
     */
    fun followUp(text: String, by: String?): ChatCommand.Intent? {
        val p = pending[by.orEmpty()]?.takeIf { now() - it.at < PENDING_MS && it.target != null } ?: return null
        if (DeviceKinds.words(text).isEmpty()) return null
        return ChatCommand.Intent.Switch(p.on, listOf(if (p.needChannel) p.target + " " + text.trim() else text.trim()))
    }

    /**
     * [control] false ise aç/kapat, döngü ve sahne reddedilir (Telegram'dan kontrol kapalı). [weather]: brifing
     * için motorun az önce okuduğu günün havası. Sıcaklık için [temperature].
     */
    fun run(intent: ChatCommand.Intent, by: String?, control: Boolean, weather: WeatherClient.Today? = null): String {
        val s = strings()
        val key = by.orEmpty()
        val waiting = pending.remove(key)?.takeIf { now() - it.at < PENDING_MS }
        val refused = s["tg_control_off"]
        return when (intent) {
            is ChatCommand.Intent.Switch -> if (!control) refused else switch(intent, key)
            is ChatCommand.Intent.Batch -> if (!control) refused else intent.steps.joinToString("\n") { switch(it, null) }
            is ChatCommand.Intent.Timed -> if (!control) refused else timed(intent)
            is ChatCommand.Intent.SwitchAll -> {
                if (!control) return refused
                val count = allTargets(intent.on).size
                if (count == 0) return s[if (intent.on) "chat_all_already_on" else "chat_none_on"]
                pending[key] = Pending(now(), intent.on, all = true)
                s.format(if (intent.on) "chat_confirm_all_on" else "chat_confirm_all_off", count)
            }
            is ChatCommand.Intent.Confirm -> when {
                waiting == null || !waiting.all -> s["chat_nothing_pending"]
                !intent.yes -> s["chat_cancelled"]
                !control -> refused
                else -> switchAll(waiting.on)
            }
            is ChatCommand.Intent.RunCycle -> if (!control) refused else cycle(intent)
            is ChatCommand.Intent.Status -> status(intent.query)
            ChatCommand.Intent.WhatsOn -> onLine() ?: s["chat_none_on"]
            ChatCommand.Intent.Briefing -> briefing(weather)
            ChatCommand.Intent.Summary -> summary()
            is ChatCommand.Intent.Power -> power(intent.query)
            ChatCommand.Intent.Temperature -> temperature(null)
            is ChatCommand.Intent.Sensors -> sensors(intent.query, intent.kinds)
            is ChatCommand.Intent.SetPresence -> {
                awayNow(intent.away, by)
                s[if (intent.away) "tg_presence_away" else "tg_presence_home"]
            }
            is ChatCommand.Intent.RunScene -> {
                if (!control) return refused
                val scenes = store.scenes
                val scene = Scene.find(intent.query, scenes)
                    ?: return s.format("tg_scene_not_found", intent.query, scenes.joinToString(", ") { it.name }.ifEmpty { "—" })
                val (ok, failed) = sceneNow(scene, by)
                s.format("tg_scene_done", scene.name, ok, failed)
            }
            ChatCommand.Intent.Help -> s["chat_help"] + "\n" + s["desktop_chat_help_sensors"]
            ChatCommand.Intent.Ack -> s["chat_ack_app"]
        }
    }

    /**
     * Dış sıcaklık: [fresh] motorun az önce okuduğu değer; okunamadıysa son bilinen değer, o da yoksa hata. Konum
     * yoksa nereden ayarlanacağı söylenir. Oda sensörleri her durumda başta.
     */
    fun temperature(fresh: Double?): String {
        val s = strings()
        // Oda sensörleri (TH16, SNZB-02 ...): "Salon: 23,5 °C · 55 %".
        val indoor = store.snapshots.filter { it.online && SensorKind.TEMPERATURE in it.sensors }.joinToString("\n") { d ->
            "🌡 " + d.name + ": " + listOf(SensorKind.TEMPERATURE, SensorKind.HUMIDITY)
                .mapNotNull { k -> d.sensors[k]?.let { formatSensor(k, it) } }.joinToString(" · ")
        }
        fun withIndoor(outdoor: String) = if (indoor.isEmpty()) outdoor else "$indoor\n$outdoor"
        if (store.latitude == null || store.longitude == null) return indoor.ifEmpty { s["desktop_chat_temp_unknown"] }
        val t = fresh ?: runtime.outdoorTemp ?: return withIndoor(s["chat_temp_failed"])
        return withIndoor(s.format("chat_temp", String.format(s.locale, "%.1f", t)))
    }

    // ---------------------------------------------------------------- aç / kapat

    /** [askKey] doluysa ve tek hedef netleşmediyse soru olarak hatırlanır (yanıtı sıradaki mesajdır). */
    private fun switch(intent: ChatCommand.Intent.Switch, askKey: String?): String {
        val s = strings()
        val labels = s.eventStrings().labels
        val devices = store.snapshotsWithGroups
        val state = low(if (intent.on) labels.on else labels.off)
        if (intent.targets.all { it.isBlank() }) return s["chat_switch_usage"]
        val single = askKey != null && intent.targets.size == 1
        return intent.targets.joinToString("\n") { target ->
            when (val t = ChatCommand.resolve(target, devices)) {
                is ChatCommand.Target.One -> apply(t.device, t.channel, intent.on, label(t.device, t.channel), state)
                is ChatCommand.Target.Many -> t.devices.flatMap { d ->
                    val channels = if (DeviceGroup.isGroup(d.id)) listOf(0) else d.switches.keys.sorted()
                    channels.map { ch -> apply(d, ch, intent.on, label(d, ch), state) }
                }.joinToString("\n")
                is ChatCommand.Target.NeedChannel -> {
                    if (single) pending[askKey!!] = Pending(now(), intent.on, all = false, target = t.device.name, needChannel = true)
                    s.format("chat_need_channel", t.device.name, t.device.switches.size)
                }
                is ChatCommand.Target.Ambiguous -> {
                    if (single) pending[askKey!!] = Pending(now(), intent.on, all = false, target = target)
                    s.format("tg_ambiguous", t.names.joinToString(", "))
                }
                ChatCommand.Target.NotFound -> s.format("chat_not_found", target)
            }
        }
    }

    private fun label(d: DeviceSnapshot, ch: Int) = if (d.isMultiChannel) strings().eventStrings().labels.channel(d.name, ch + 1) else d.name

    private fun apply(device: DeviceSnapshot, channel: Int, on: Boolean, label: String, state: String): String {
        if (!device.online) return "⚠️ $label: ${low(strings().eventStrings().labels.offline)}"
        val result = runCatching { switchNow(device, channel, on) }
        return if (result.isSuccess) "✅ $label: $state" else "⚠️ $label: ${result.exceptionOrNull()?.message.orEmpty()}"
    }

    /** "Hepsini kapat"ın dokunacağı kanallar: çevrimiçi, durumu farklı olanlar (gruplar değil, üyeleri zaten listede). */
    private fun allTargets(on: Boolean): List<Pair<DeviceSnapshot, Int>> =
        store.snapshots.filter { it.online && !DeviceGroup.isGroup(it.id) }
            .flatMap { d -> d.switches.filterValues { it != on }.keys.sorted().map { d to it } }

    private fun switchAll(on: Boolean): String {
        val s = strings()
        val labels = s.eventStrings().labels
        val state = low(if (on) labels.on else labels.off)
        val targets = allTargets(on)
        if (targets.isEmpty()) return s[if (on) "chat_all_already_on" else "chat_none_on"]
        return targets.joinToString("\n") { (d, ch) -> apply(d, ch, on, label(d, ch), state) }
    }

    /**
     * Süreli komut tek kullanımlık döngü olarak kurulur: döngü motoru zamanı tutar, komut gidemezse yeniden dener,
     * Otomasyon sekmesinde geri sayımı görünür ve oradan durdurulabilir; bitince silinir (bkz. MonitorEngine.applyCycles).
     */
    private fun timed(intent: ChatCommand.Intent.Timed): String {
        val s = strings()
        val labels = s.eventStrings().labels
        val devices = store.snapshotsWithGroups
        val targets = mutableListOf<CycleTarget>()
        val names = mutableListOf<String>()
        for (target in intent.targets) {
            when (val t = ChatCommand.resolve(target, devices)) {
                is ChatCommand.Target.One -> {
                    targets += CycleTarget(t.device.id, t.channel)
                    names += label(t.device, t.channel)
                }
                is ChatCommand.Target.Many -> t.devices.forEach { d ->
                    val channels = if (DeviceGroup.isGroup(d.id)) listOf(0) else d.switches.keys.sorted()
                    channels.forEach { targets += CycleTarget(d.id, it) }
                    names += d.name
                }
                is ChatCommand.Target.NeedChannel -> return s.format("chat_need_channel", t.device.name, t.device.switches.size)
                is ChatCommand.Target.Ambiguous -> return s.format("tg_ambiguous", t.names.joinToString(", "))
                ChatCommand.Target.NotFound -> return s.format("chat_not_found", target)
            }
        }
        val (steps, repeat, end) = when {
            !intent.hold -> CycleEngine.Presets.countdown(targets, intent.seconds, intent.on)
            intent.on -> CycleEngine.Presets.onFor(targets, intent.seconds)
            else -> Triple(listOf(CycleStep(intent.seconds, targets.associate { it.key to false })), 1, CycleEnd.ON)
        }
        val name = names.joinToString(", ")
        val cycle = Cycle(
            id = ONCE_PREFIX + UUID.randomUUID(), name = name, targets = targets, steps = steps, repeat = repeat, end = end,
            preset = if (intent.hold) CyclePreset.ON_FOR else CyclePreset.COUNTDOWN,
        )
        store.saveCycle(cycle)
        cycleNow(cycle.id, true)
        val state = low(if (intent.on) labels.on else labels.off)
        val time = DurationText.format(intent.seconds * 1000L, s.durations())
        return "⏱ " + s.format(if (intent.hold) "chat_timer_for" else "chat_timer_later", name, state, time)
    }

    private fun cycle(intent: ChatCommand.Intent.RunCycle): String {
        val s = strings()
        val all = store.cycles.filterNot { it.id.startsWith(ONCE_PREFIX) }
        val query = DeviceKinds.words(intent.query)
        val running = store.cycleRuns.keys
        val found = all.filter { c ->
            val name = DeviceKinds.words(c.name)
            query.all { q -> name.any { n -> ChatCommand.similar(q, n) } }
        }.let { list ->
            // Ad söylenmediyse: durdururken çalışan döngü, başlatırken tek döngü.
            if (query.isEmpty() && !intent.start && list.size > 1) list.filter { it.id in running } else list
        }
        if (found.size != 1) {
            return s.format("chat_cycle_not_found", intent.query.ifEmpty { "—" }, all.joinToString(", ") { it.name }.ifEmpty { "—" })
        }
        cycleNow(found[0].id, intent.start)
        return s.format(if (intent.start) "chat_cycle_started" else "chat_cycle_stopped", found[0].name)
    }

    // ---------------------------------------------------------------- durum, güç, sensörler

    private fun status(query: String): String {
        val s = strings()
        val all = store.snapshotsWithGroups
        val list = if (query.isBlank()) all.filterNot { DeviceGroup.isGroup(it.id) } else when (val t = ChatCommand.resolve(query, all)) {
            is ChatCommand.Target.One -> listOf(t.device)
            is ChatCommand.Target.NeedChannel -> listOf(t.device)
            is ChatCommand.Target.Many -> t.devices
            is ChatCommand.Target.Ambiguous -> return s.format("tg_ambiguous", t.names.joinToString(", "))
            ChatCommand.Target.NotFound -> return s.format("chat_not_found", query)
        }
        if (list.isEmpty()) return s["chat_no_devices"]
        return list.joinToString("\n") { line(it) }
    }

    /** "🟢 Salon: açık · 120 W" / çok kanallıda "1: açık, 2: kapalı"; sensörlerde okumalar. */
    private fun line(d: DeviceSnapshot): String {
        val labels = strings().eventStrings().labels
        if (!d.online) return "⚪ ${d.name}: ${low(labels.offline)}"
        if (d.sensorOnly) return "${sensorIcon(d)} ${d.name}: ${readings(d, emptySet())}"
        val state = if (d.isMultiChannel) {
            d.switches.toSortedMap().entries.joinToString(", ") { (ch, on) -> "${ch + 1}: ${if (on) low(labels.on) else low(labels.off)}" }
        } else if (d.switches.values.any { it }) low(labels.on) else low(labels.off)
        val icon = if (d.switches.values.any { it }) "🟢" else "⚫"
        val watts = d.power?.let { " · ${watts(it)} W" }.orEmpty()
        val sensors = if (d.hasSensors) " · " + readings(d, emptySet()) else ""
        return "$icon ${d.name}: $state$watts$sensors"
    }

    /** "Kapı açık · Pil 80 %"; [kinds] boşsa hepsi. */
    private fun readings(d: DeviceSnapshot, kinds: Set<SensorKind>): String {
        val s = strings()
        return d.sensors.filterKeys { kinds.isEmpty() || it in kinds }.entries.joinToString(" · ") { (k, v) ->
            if (k.binary) s[k.stateKey(v >= 0.5)] else s[k.labelKey()] + " " + formatSensor(k, v)
        }
    }

    /** Uyarı gerektiren sensörde dikkat simgesi: su kaçağı, açık kapı, hareket. */
    private fun sensorIcon(d: DeviceSnapshot): String = when {
        d.sensorActive(SensorKind.WATER) == true -> "🚨"
        d.sensorActive(SensorKind.DOOR) == true -> "🚪"
        d.sensorActive(SensorKind.MOTION) == true -> "🏃"
        SensorKind.TEMPERATURE in d.sensors -> "🌡"
        else -> "📟"
    }

    /** Sensör okumaları: "salonun nemi", "kapılar açık mı", "sensörler". */
    private fun sensors(query: String, kinds: Set<SensorKind>): String {
        val s = strings()
        val all = store.snapshots.filter { it.hasSensors }
        val list = if (query.isBlank()) all else when (val t = ChatCommand.resolve(query, all)) {
            is ChatCommand.Target.One -> listOf(t.device)
            is ChatCommand.Target.NeedChannel -> listOf(t.device)
            is ChatCommand.Target.Many -> t.devices
            is ChatCommand.Target.Ambiguous -> return s.format("tg_ambiguous", t.names.joinToString(", "))
            ChatCommand.Target.NotFound -> return s.format("chat_not_found", query)
        }.filter { it.hasSensors }
        val shown = list.filter { d -> kinds.isEmpty() || d.sensors.keys.any { it in kinds } }
        if (shown.isEmpty()) return s["chat_sensors_none"]
        return shown.joinToString("\n") { d ->
            if (!d.online) "⚪ ${d.name}: ${low(s.eventStrings().labels.offline)}" else "${sensorIcon(d)} ${d.name}: ${readings(d, kinds)}"
        }
    }

    private fun power(query: String): String {
        val s = strings()
        val all = store.snapshots
        val list = if (query.isBlank()) all.filter { it.hasEnergy } else when (val t = ChatCommand.resolve(query, all)) {
            is ChatCommand.Target.One -> listOf(t.device)
            is ChatCommand.Target.NeedChannel -> listOf(t.device)
            is ChatCommand.Target.Many -> t.devices
            is ChatCommand.Target.Ambiguous -> return s.format("tg_ambiguous", t.names.joinToString(", "))
            ChatCommand.Target.NotFound -> return s.format("chat_not_found", query)
        }
        if (list.isEmpty()) return s["chat_power_none"]
        val now = now()
        val dayStart = EnergyWatch.dayStart(now)
        // Çevrimdışı cihazın son ölçümü anlık güç değildir: gösterilmez, toplama katılmaz.
        val live = list.filter { it.hasEnergy && it.online }
        val total = live.sumOf { it.power ?: 0.0 }
        val lines = list.map { d ->
            if (d.power == null && (d.online || !d.hasEnergy)) return@map s.format("chat_no_power", d.name)
            val typical = ChatCommand.typicalWatts(metrics.powerSamples(d.id, now - 7 * 86_400_000L, now))
            val today = metrics.kwh(d.id, dayStart, now)
            buildString {
                if (!d.online) append("⚪ ").append(s.format("chat_power_offline", d.name))
                else append("⚡ ").append(d.name).append(": ").append(watts(d.power ?: 0.0)).append(" W")
                append(" · ").append(s.format("chat_power_today", String.format(s.locale, "%.2f", today)))
                append('\n').append("   ").append(
                    typical?.let { s.format("chat_power_typical", watts(it)) } ?: s["chat_power_learning"]
                )
            }
        }
        val sum = if (live.size > 1) "\n" + s.format("chat_power_total", watts(total)) else ""
        return lines.joinToString("\n") + sum
    }

    // ---------------------------------------------------------------- brifing ve günün özeti (telefon Briefing.kt)

    /** "Açık (3): Salon, Pompa (1, 3), Klima." Açık cihaz yoksa null. */
    fun onLine(): String? {
        val s = strings()
        val on = store.snapshots.filter { !DeviceGroup.isGroup(it.id) && it.online && it.switches.values.any { v -> v } }
        if (on.isEmpty()) return null
        val names = on.joinToString(", ") { d ->
            if (d.isMultiChannel) d.name + " (" + d.switches.filterValues { it }.keys.sorted().joinToString(", ") { (it + 1).toString() } + ")" else d.name
        }
        return "${capital(s.eventStrings().labels.on)} (${on.size}): $names."
    }

    /**
     * Şu anki durumun sözlü özeti: hava, alarm, evde/dışarıda, açık cihazlar, çevrimdışılar, güç, sensörler, yakında
     * çalışacak işler. Satırlar nokta ile biter ki sesli okumada duraklansın.
     */
    fun briefing(weather: WeatherClient.Today?): String {
        val s = strings()
        val now = now()
        val lines = mutableListOf<String>()
        weather?.let { w ->
            val range = if (w.min != null && w.max != null) " (${w.min.roundToInt()}° – ${w.max.roundToInt()}°)" else ""
            val rain = w.rainChance?.takeIf { it > 0 }?.let { ", " + s.format("board_rain", it) }.orEmpty()
            lines += "${s[weatherKey(w.code)]}, ${w.temp.roundToInt()}°$range$rain."
        }
        store.pendingAlarm.lastOrNull()?.let { lines += s["board_alarm"] + ": $it." }
        lines += s[if (store.isAway(now)) "board_away" else "board_home"] + "."
        lines += onLine() ?: s["chat_none_on"]
        val snapshots = store.snapshots.filterNot { DeviceGroup.isGroup(it.id) }
        val monitored = store.monitoredIds()
        val offline = snapshots.filter { it.id in monitored && !it.online }
        if (offline.isNotEmpty()) lines += s.format("board_offline", offline.size) + ": " + offline.joinToString(", ") { it.name } + "."
        val energy = snapshots.filter { it.hasEnergy }
        if (energy.isNotEmpty()) {
            val from = DailySummary.startOfDay(now)
            val kwh = energy.sumOf { runCatching { metrics.kwh(it.id, from, now) }.getOrDefault(0.0) }
            val w = energy.filter { it.online }.sumOf { it.power ?: 0.0 }
            lines += "${watts(w)} W, " + s.format("board_today_kwh", String.format(s.locale, "%.1f", kwh)) + "."
        }
        // Sensörler: dikkat isteyenler (su, açık kapı, hareket) ve oda sıcaklıkları.
        val sensors = snapshots.filter { it.online && it.hasSensors }
        if (sensors.isNotEmpty()) {
            val alerts = sensors.flatMap { d ->
                listOf(SensorKind.WATER, SensorKind.DOOR, SensorKind.MOTION).filter { d.sensorActive(it) == true }.map { "${d.name}: ${s[it.stateKey(true)]}" }
            }
            val temps = sensors.filter { SensorKind.TEMPERATURE in it.sensors }.map { d -> "${d.name} ${formatSensor(SensorKind.TEMPERATURE, d.sensors.getValue(SensorKind.TEMPERATURE))}" }
            val parts = alerts + temps
            if (parts.isNotEmpty()) lines += s["board_sensors"] + ": " + parts.joinToString(", ") + "."
        }
        val upcoming = StandbyBoard.upcoming(
            store.automations, store.cycles, store.cycleRuns, store.chatTemplates.orEmpty(),
            runtime.templateRuns, store.latitude, store.longitude, now,
        ).take(5)
        if (upcoming.isEmpty()) lines += s["board_no_upcoming"] + "."
        else lines += s["board_upcoming"] + ": " + upcoming.joinToString("; ") { u ->
            val kind = s[when (u.kind) {
                UpcomingKind.SUNRISE -> "board_kind_sunrise"
                UpcomingKind.SUNSET -> "board_kind_sunset"
                UpcomingKind.CYCLE_START -> "board_kind_cycle_start"
                UpcomingKind.CYCLE_STEP -> "board_kind_cycle_step"
                UpcomingKind.CYCLE_END -> "board_kind_cycle_end"
                UpcomingKind.TEMPLATE -> "board_kind_template"
            }]
            "${s.time(u.time)} ${u.name} ($kind)"
        } + "."
        return lines.joinToString("\n")
    }

    /**
     * Günün ayrıntılı özeti: alarm ve kopma sayısı, toplam tüketim ve maliyet, dünle karşılaştırma, cihaz cihaz kaç
     * kez açıldığı / ne kadar açık kaldığı / ne harcadığı, en uzun açık kalan ve şu an açık olanlar.
     */
    fun summary(): String {
        val s = strings()
        val now = now()
        val doc = Reports.daily(store, metrics, log, s, now)
        val lines = mutableListOf<String>()
        if (doc.kpis.isNotEmpty()) lines += doc.kpis.joinToString(" · ") { "${it.label}: ${it.value}" } + "."
        val energy = store.snapshots.filter { it.hasEnergy && !DeviceGroup.isGroup(it.id) }
        if (energy.isNotEmpty()) {
            val day = 86_400_000L
            val from = DailySummary.startOfDay(now - day)
            val yesterday = energy.sumOf { runCatching { metrics.kwh(it.id, from, now - day) }.getOrDefault(0.0) }
            if (yesterday > 0.0005) lines += s.format("summary_yesterday", DailySummary.formatKwh(yesterday, s.locale))
        }
        val devices = DailySummary.build(store, metrics, s, now)
        if (devices == null) lines += s["summary_no_data"] else lines += devices.map { "$it." }
        doc.sections.flatMap { it.findings }.map { it.text }.filter { it != s["summary_no_data"] }.forEach { lines += it }
        onLine()?.let { lines += it }
        return s["summary_title"] + ".\n" + lines.joinToString("\n")
    }

    private fun capital(word: String): String {
        val locale = strings().locale
        return word.lowercase(locale).replaceFirstChar { it.titlecase(locale) }
    }

    /** Uyarı metinlerindeki "AÇIK/KAPALI" sohbette cümle içinde küçük yazılır ("Salon: açık"); Türkçe İ/ı korunur. */
    private fun low(word: String) = word.lowercase(strings().locale)

    private fun watts(w: Double) = if (w >= 100) w.toInt().toString() else String.format(strings().locale, "%.1f", w)

    companion object {
        /** Süreli komutların tek kullanımlık döngüleri; bitince kendiliğinden silinir. */
        const val ONCE_PREFIX = "once:"

        private const val PENDING_MS = 90_000L

        /** WMO hava kodu → kısa açıklama anahtarı. */
        fun weatherKey(code: Int): String = when (code) {
            0 -> "weather_clear"
            1, 2 -> "weather_partly"
            3 -> "weather_cloudy"
            45, 48 -> "weather_fog"
            51, 53, 55, 56, 57 -> "weather_drizzle"
            61, 63, 65, 66, 67 -> "weather_rain"
            71, 73, 75, 77, 85, 86 -> "weather_snow"
            80, 81, 82 -> "weather_showers"
            95, 96, 99 -> "weather_storm"
            else -> "weather_cloudy"
        }

        /**
         * Hazır şablonlar. Mesajlar komut olduğu için yalnız Türkçe ya da İngilizce yazılır (adlar uygulamanın dilinde).
         * Sonradan eklenen hazır şablonlar, listesini düzenlemiş kullanıcıya da bir kez sunulur (bkz. [templates]).
         */
        fun defaults(s: Strings): List<ChatTemplate> {
            val tr = s.language == "tr"
            fun t(id: String, name: String, trText: String, enText: String) = ChatTemplate(id, s[name], if (tr) trText else enText)
            return listOf(
                ChatTemplate("d-status", s["chat_tpl_status"], s["chat_tpl_status_text"]),
                t("d-brief", "chat_tpl_brief", "Durum özeti", "Briefing"),
                t("d-on", "chat_tpl_whats_on", "Neler açık?", "What is on?"),
                t("d-sensors", "board_sensors", "Sensörler", "Sensors"),
                t("d-lights-off", "chat_tpl_lights_off", "Tüm ışıkları kapat", "Turn off all lights"),
                t("d-lights-on", "chat_tpl_lights_on", "Tüm ışıkları aç", "Turn on all lights"),
                t("d-all-off", "chat_tpl_all_off", "Hepsini kapat", "Turn everything off"),
                ChatTemplate("d-power", s["chat_tpl_power"], s["chat_tpl_power_text"]),
                ChatTemplate("d-temp", s["chat_tpl_temp"], s["chat_tpl_temp_text"]),
                t("d-summary", "summary_title", "Günün özeti", "Daily summary"),
                ChatTemplate("d-home", s["chat_tpl_home"], s["chat_tpl_home_text"]),
                ChatTemplate("d-away", s["chat_tpl_away"], s["chat_tpl_away_text"]),
                ChatTemplate("d-help", s["chat_tpl_help"], s["chat_tpl_help_text"]),
            )
        }

        /** Kayıtlı şablonlar + kullanıcının henüz görmediği yeni hazır şablonlar; en çok kullanılan en başta. */
        fun templates(store: Store, s: Strings): List<ChatTemplate> {
            val defaults = defaults(s)
            val saved = store.chatTemplates
            val list = if (saved == null) defaults else {
                val seen = store.chatDefaultsSeen
                saved + defaults.filter { d -> d.id !in seen && saved.none { it.id == d.id } }
            }
            val uses = store.chatTemplateUses
            return list.sortedByDescending { uses[it.id] ?: 0 }
        }

        /** Şablon listesi kaydedilir; o ana kadarki hazır şablonlar "görüldü" sayılır (silinen geri gelmesin). */
        fun saveTemplates(store: Store, s: Strings, list: List<ChatTemplate>) {
            store.chatDefaultsSeen = store.chatDefaultsSeen + defaults(s).map { it.id }
            store.chatTemplates = list
        }

        fun countUse(store: Store, id: String) {
            val uses = store.chatTemplateUses
            store.chatTemplateUses = uses + (id to ((uses[id] ?: 0) + 1))
        }
    }
}
