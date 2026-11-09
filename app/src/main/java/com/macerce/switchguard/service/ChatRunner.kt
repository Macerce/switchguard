package com.macerce.switchguard.service

import android.content.Context
import com.macerce.switchguard.R
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
import com.macerce.switchguard.core.Scene
import com.macerce.switchguard.data.EventLog
import com.macerce.switchguard.data.MetricsLog
import com.macerce.switchguard.data.RuntimeState
import com.macerce.switchguard.data.Store
import com.macerce.switchguard.ui.Actions
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Sohbet komutlarını yürütür ve yanıt metnini üretir; Telegram ve uygulama içi sohbet aynı yolu kullanır.
 * Ağ işi yapar: ana iş parçacığından çağırma.
 */
object ChatRunner {

    /** [ask]: yanıt bir soru (hangi kanal, emin misiniz); sesli kullanımda mikrofon yeniden açılır. */
    data class Reply(val text: String, val ask: Boolean = false)

    /**
     * Yanıt bekleyen soru. [all] true: "hepsini kapat/aç" onayı. [target] dolu: kanalı ya da adı netleşmemiş aç/kapat;
     * sıradaki mesaj komut değilse eksik parça sayılır ("iki", "salon ışık").
     */
    private class Pending(val at: Long, val on: Boolean, val all: Boolean, val target: String? = null, val needChannel: Boolean = false)

    private const val PENDING_MS = 90_000L
    private val pending = ConcurrentHashMap<String, Pending>()

    /** Süreli komutların tek kullanımlık döngüleri; bitince kendiliğinden silinir. */
    const val ONCE_PREFIX = "once:"

    /** Anlaşılmayan mesajda null. [control] false ise aç/kapat ve sahne reddedilir (Telegram'dan kontrol kapalı). */
    suspend fun answer(context: Context, text: String, by: String? = null, control: Boolean = true): String? =
        reply(context, text, by, control)?.text

    suspend fun reply(context: Context, text: String, by: String? = null, control: Boolean = true): Reply? {
        val key = by.orEmpty()
        var intent = ChatCommand.parse(text)
        if (intent == null) {
            // Sorunun yanıtı: "Hangi kanal?" → "iki"; "Hangisi?" → "salon ışık".
            val p = pending[key]?.takeIf { System.currentTimeMillis() - it.at < PENDING_MS && it.target != null } ?: return null
            if (DeviceKinds.words(text).isEmpty()) return null
            intent = ChatCommand.Intent.Switch(p.on, listOf(if (p.needChannel) p.target + " " + text.trim() else text.trim()))
        }
        val answer = run(context, intent, by, control)
        return Reply(answer, ask = pending.containsKey(key))
    }

    suspend fun run(context: Context, intent: ChatCommand.Intent, by: String?, control: Boolean): String = withContext(Dispatchers.IO) {
        val store = Store.get(context)
        val key = by.orEmpty()
        val waiting = pending.remove(key)?.takeIf { System.currentTimeMillis() - it.at < PENDING_MS }
        val refused = context.getString(R.string.tg_control_off)
        when (intent) {
            is ChatCommand.Intent.Switch -> if (!control) refused else switch(context, intent, key)
            is ChatCommand.Intent.Batch -> if (!control) refused else intent.steps.map { switch(context, it, null) }.joinToString("\n")
            is ChatCommand.Intent.Timed -> if (!control) refused else timed(context, intent)
            is ChatCommand.Intent.SwitchAll -> {
                if (!control) return@withContext refused
                val count = allTargets(store, intent.on).size
                if (count == 0) return@withContext context.getString(if (intent.on) R.string.chat_all_already_on else R.string.chat_none_on)
                pending[key] = Pending(System.currentTimeMillis(), intent.on, all = true)
                context.getString(if (intent.on) R.string.chat_confirm_all_on else R.string.chat_confirm_all_off, count)
            }
            is ChatCommand.Intent.Confirm -> when {
                waiting == null || !waiting.all -> context.getString(R.string.chat_nothing_pending)
                !intent.yes -> context.getString(R.string.chat_cancelled)
                !control -> refused
                else -> switchAll(context, waiting.on)
            }
            is ChatCommand.Intent.RunCycle -> if (!control) refused else cycle(context, intent)
            is ChatCommand.Intent.Status -> status(context, intent.query)
            ChatCommand.Intent.WhatsOn -> Briefing.onLine(context) ?: context.getString(R.string.chat_none_on)
            ChatCommand.Intent.Briefing -> Briefing.status(context)
            ChatCommand.Intent.Summary -> Briefing.summary(context)
            is ChatCommand.Intent.Power -> power(context, intent.query)
            ChatCommand.Intent.Temperature -> temperature(context)
            is ChatCommand.Intent.SetPresence -> {
                Actions.setAway(context, intent.away, by)
                context.getString(if (intent.away) R.string.tg_presence_away else R.string.tg_presence_home)
            }
            is ChatCommand.Intent.RunScene -> {
                if (!control) return@withContext refused
                val scenes = store.scenes
                val scene = Scene.find(intent.query, scenes)
                    ?: return@withContext context.getString(R.string.tg_scene_not_found, intent.query, scenes.joinToString(", ") { it.name }.ifEmpty { "—" })
                val (ok, failed) = Actions.runScene(context, scene, by)
                context.getString(R.string.tg_scene_done, scene.name, ok, failed)
            }
            ChatCommand.Intent.Help -> context.getString(R.string.chat_help)
            ChatCommand.Intent.Ack -> context.getString(R.string.chat_ack_app)
        }
    }

    /**
     * Dış sıcaklık: konum varsa anlık okunur (servis yalnızca sıcaklık kuralı varken okuyor, son değer eski olabilir);
     * okunamazsa son bilinen değer, o da yoksa hata. Konum yoksa nereden ayarlanacağı söylenir.
     */
    private fun temperature(context: Context): String {
        val store = Store.get(context)
        val runtime = RuntimeState.get(context)
        val lat = store.latitude
        val lon = store.longitude
        if (lat == null || lon == null) return context.getString(R.string.chat_temp_unknown)
        val fresh = runCatching { com.macerce.switchguard.api.WeatherClient.temperature(lat, lon) }.getOrNull()
        if (fresh != null) runtime.outdoorTemp = fresh
        val t = fresh ?: runtime.outdoorTemp ?: return context.getString(R.string.chat_temp_failed)
        return context.getString(R.string.chat_temp, String.format(Locale.getDefault(), "%.1f", t))
    }

    /** [askKey] doluysa ve tek hedef netleşmediyse soru olarak hatırlanır (yanıtı sıradaki mesajdır). */
    private suspend fun switch(context: Context, intent: ChatCommand.Intent.Switch, askKey: String?): String {
        val store = Store.get(context)
        val labels = EventLog.strings(context).labels
        val devices = store.snapshotsWithGroups
        val state = if (intent.on) low(context, labels.on) else low(context, labels.off)
        if (intent.targets.all { it.isBlank() }) return context.getString(R.string.chat_switch_usage)
        val single = askKey != null && intent.targets.size == 1
        // map (satır içi) kullanılır: askıya alınan aç/kapat joinToString'in lambdasında çağrılamaz.
        return intent.targets.map { target ->
            when (val t = ChatCommand.resolve(target, devices)) {
                is ChatCommand.Target.One -> {
                    val label = if (t.device.isMultiChannel) labels.channel(t.device.name, t.channel + 1) else t.device.name
                    apply(context, t.device, t.channel, intent.on, label, state)
                }
                is ChatCommand.Target.Many -> t.devices.flatMap { d ->
                    val channels = if (DeviceGroup.isGroup(d.id)) listOf(0) else d.switches.keys.sorted()
                    channels.map { ch ->
                        apply(context, d, ch, intent.on, if (d.isMultiChannel) labels.channel(d.name, ch + 1) else d.name, state)
                    }
                }.joinToString("\n")
                is ChatCommand.Target.NeedChannel -> {
                    if (single) pending[askKey!!] = Pending(System.currentTimeMillis(), intent.on, all = false, target = t.device.name, needChannel = true)
                    context.getString(R.string.chat_need_channel, t.device.name, t.device.switches.size)
                }
                is ChatCommand.Target.Ambiguous -> {
                    if (single) pending[askKey!!] = Pending(System.currentTimeMillis(), intent.on, all = false, target = target)
                    context.getString(R.string.tg_ambiguous, t.names.joinToString(", "))
                }
                ChatCommand.Target.NotFound -> context.getString(R.string.chat_not_found, target)
            }
        }.joinToString("\n")
    }

    private suspend fun apply(context: Context, device: DeviceSnapshot, channel: Int, on: Boolean, label: String, state: String): String {
        if (!device.online) return "⚠️ $label: ${low(context, EventLog.strings(context).labels.offline)}"
        val result = Actions.setSwitch(context, device, channel, on)
        return if (result.isSuccess) "✅ $label: $state" else "⚠️ $label: ${result.exceptionOrNull()?.message.orEmpty()}"
    }

    /** "Hepsini kapat"ın dokunacağı kanallar: çevrimiçi, durumu farklı olanlar (gruplar değil, üyeleri zaten listede). */
    private fun allTargets(store: Store, on: Boolean): List<Pair<DeviceSnapshot, Int>> =
        store.snapshots.filter { it.online && !DeviceGroup.isGroup(it.id) }
            .flatMap { d -> d.switches.filterValues { it != on }.keys.sorted().map { d to it } }

    private suspend fun switchAll(context: Context, on: Boolean): String {
        val labels = EventLog.strings(context).labels
        val state = if (on) low(context, labels.on) else low(context, labels.off)
        val targets = allTargets(Store.get(context), on)
        if (targets.isEmpty()) return context.getString(if (on) R.string.chat_all_already_on else R.string.chat_none_on)
        return targets.map { (d, ch) -> apply(context, d, ch, on, if (d.isMultiChannel) labels.channel(d.name, ch + 1) else d.name, state) }.joinToString("\n")
    }

    /**
     * Süreli komut tek kullanımlık döngü olarak kurulur: döngü motoru zamanı tutar, komut gidemezse yeniden dener,
     * Otomasyon sekmesinde geri sayımı görünür ve oradan durdurulabilir; bitince silinir.
     */
    private fun timed(context: Context, intent: ChatCommand.Intent.Timed): String {
        val store = Store.get(context)
        val labels = EventLog.strings(context).labels
        val devices = store.snapshotsWithGroups
        val targets = mutableListOf<CycleTarget>()
        val names = mutableListOf<String>()
        for (target in intent.targets) {
            when (val t = ChatCommand.resolve(target, devices)) {
                is ChatCommand.Target.One -> {
                    targets += CycleTarget(t.device.id, t.channel)
                    names += if (t.device.isMultiChannel) labels.channel(t.device.name, t.channel + 1) else t.device.name
                }
                is ChatCommand.Target.Many -> t.devices.forEach { d ->
                    val channels = if (DeviceGroup.isGroup(d.id)) listOf(0) else d.switches.keys.sorted()
                    channels.forEach { targets += CycleTarget(d.id, it) }
                    names += d.name
                }
                is ChatCommand.Target.NeedChannel -> return context.getString(R.string.chat_need_channel, t.device.name, t.device.switches.size)
                is ChatCommand.Target.Ambiguous -> return context.getString(R.string.tg_ambiguous, t.names.joinToString(", "))
                ChatCommand.Target.NotFound -> return context.getString(R.string.chat_not_found, target)
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
        MonitorService.cycle(context, cycle.id, start = true)
        val state = if (intent.on) low(context, labels.on) else low(context, labels.off)
        val time = DurationText.format(intent.seconds * 1000L, EventLog.durations(context))
        return "⏱ " + context.getString(if (intent.hold) R.string.chat_timer_for else R.string.chat_timer_later, name, state, time)
    }

    private fun cycle(context: Context, intent: ChatCommand.Intent.RunCycle): String {
        val store = Store.get(context)
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
            return context.getString(R.string.chat_cycle_not_found, intent.query.ifEmpty { "—" }, all.joinToString(", ") { it.name }.ifEmpty { "—" })
        }
        MonitorService.cycle(context, found[0].id, intent.start)
        return context.getString(if (intent.start) R.string.chat_cycle_started else R.string.chat_cycle_stopped, found[0].name)
    }

    private fun status(context: Context, query: String): String {
        val store = Store.get(context)
        val all = store.snapshotsWithGroups
        val list = if (query.isBlank()) all.filterNot { DeviceGroup.isGroup(it.id) } else when (val t = ChatCommand.resolve(query, all)) {
            is ChatCommand.Target.One -> listOf(t.device)
            is ChatCommand.Target.NeedChannel -> listOf(t.device)
            is ChatCommand.Target.Many -> t.devices
            is ChatCommand.Target.Ambiguous -> return context.getString(R.string.tg_ambiguous, t.names.joinToString(", "))
            ChatCommand.Target.NotFound -> return context.getString(R.string.chat_not_found, query)
        }
        if (list.isEmpty()) return context.getString(R.string.chat_no_devices)
        return list.joinToString("\n") { line(context, it) }
    }

    /** "💡 Salon: açık · 120 W" / çok kanallıda "K1 açık, K2 kapalı". */
    private fun line(context: Context, d: DeviceSnapshot): String {
        val labels = EventLog.strings(context).labels
        if (!d.online) return "⚪ ${d.name}: ${low(context, labels.offline)}"
        val state = if (d.isMultiChannel) {
            d.switches.toSortedMap().entries.joinToString(", ") { (ch, on) -> "${ch + 1}: ${if (on) low(context, labels.on) else low(context, labels.off)}" }
        } else if (d.switches.values.any { it }) low(context, labels.on) else low(context, labels.off)
        val icon = if (d.switches.values.any { it }) "🟢" else "⚫"
        val watts = d.power?.let { " · ${watts(it)} W" }.orEmpty()
        return "$icon ${d.name}: $state$watts"
    }

    private fun power(context: Context, query: String): String {
        val store = Store.get(context)
        val all = store.snapshots
        val list = if (query.isBlank()) all.filter { it.hasEnergy } else when (val t = ChatCommand.resolve(query, all)) {
            is ChatCommand.Target.One -> listOf(t.device)
            is ChatCommand.Target.NeedChannel -> listOf(t.device)
            is ChatCommand.Target.Many -> t.devices
            is ChatCommand.Target.Ambiguous -> return context.getString(R.string.tg_ambiguous, t.names.joinToString(", "))
            ChatCommand.Target.NotFound -> return context.getString(R.string.chat_not_found, query)
        }
        if (list.isEmpty()) return context.getString(R.string.chat_power_none)
        val metrics = MetricsLog.get(context)
        val now = System.currentTimeMillis()
        val dayStart = com.macerce.switchguard.core.EnergyWatch.dayStart(now)
        // Çevrimdışı cihazın son ölçümü anlık güç değildir: gösterilmez, toplama katılmaz.
        val live = list.filter { it.hasEnergy && it.online }
        val total = live.sumOf { it.power ?: 0.0 }
        val lines = list.map { d ->
            if (d.power == null && (d.online || !d.hasEnergy)) return@map context.getString(R.string.chat_no_power, d.name)
            val typical = ChatCommand.typicalWatts(metrics.powerSamples(d.id, now - 7 * 86_400_000L, now))
            val today = metrics.kwh(d.id, dayStart, now)
            buildString {
                if (!d.online) append("⚪ ").append(context.getString(R.string.chat_power_offline, d.name))
                else append("⚡ ").append(d.name).append(": ").append(watts(d.power ?: 0.0)).append(" W")
                append(" · ").append(context.getString(R.string.chat_power_today, String.format(Locale.getDefault(), "%.2f", today)))
                append('\n').append("   ").append(
                    typical?.let { context.getString(R.string.chat_power_typical, watts(it)) } ?: context.getString(R.string.chat_power_learning)
                )
            }
        }
        val sum = if (live.size > 1) "\n" + context.getString(R.string.chat_power_total, watts(total)) else ""
        return lines.joinToString("\n") + sum
    }

    /** Uyarı metinlerindeki "AÇIK/KAPALI" sohbette cümle içinde küçük yazılır ("Salon: açık"); Türkçe İ/ı korunur. */
    internal fun low(context: Context, word: String) = word.lowercase(context.resources.configuration.locales[0])

    internal fun watts(w: Double) = if (w >= 100) w.toInt().toString() else String.format(Locale.getDefault(), "%.1f", w)

    /**
     * Hazır şablonlar. Mesajlar komut olduğu için yalnız Türkçe ya da İngilizce yazılır (adlar uygulamanın dilinde).
     * Sonradan eklenen hazır şablonlar, listesini düzenlemiş kullanıcıya da bir kez sunulur (bkz. [templates]).
     */
    fun defaults(context: Context): List<ChatTemplate> {
        val tr = context.resources.configuration.locales[0].language == "tr"
        fun t(id: String, name: Int, trText: String, enText: String) = ChatTemplate(id, context.getString(name), if (tr) trText else enText)
        return listOf(
            ChatTemplate("d-status", context.getString(R.string.chat_tpl_status), context.getString(R.string.chat_tpl_status_text)),
            t("d-brief", R.string.chat_tpl_brief, "Durum özeti", "Briefing"),
            t("d-on", R.string.chat_tpl_whats_on, "Neler açık?", "What is on?"),
            t("d-lights-off", R.string.chat_tpl_lights_off, "Tüm ışıkları kapat", "Turn off all lights"),
            t("d-lights-on", R.string.chat_tpl_lights_on, "Tüm ışıkları aç", "Turn on all lights"),
            t("d-all-off", R.string.chat_tpl_all_off, "Hepsini kapat", "Turn everything off"),
            ChatTemplate("d-power", context.getString(R.string.chat_tpl_power), context.getString(R.string.chat_tpl_power_text)),
            ChatTemplate("d-temp", context.getString(R.string.chat_tpl_temp), context.getString(R.string.chat_tpl_temp_text)),
            t("d-summary", R.string.summary_title, "Günün özeti", "Daily summary"),
            ChatTemplate("d-home", context.getString(R.string.chat_tpl_home), context.getString(R.string.chat_tpl_home_text)),
            ChatTemplate("d-away", context.getString(R.string.chat_tpl_away), context.getString(R.string.chat_tpl_away_text)),
            ChatTemplate("d-help", context.getString(R.string.chat_tpl_help), context.getString(R.string.chat_tpl_help_text)),
        )
    }

    /** Kayıtlı şablonlar + kullanıcının henüz görmediği yeni hazır şablonlar; en çok kullanılan en başta. */
    fun templates(context: Context): List<ChatTemplate> {
        val store = Store.get(context)
        val defaults = defaults(context)
        val saved = store.chatTemplates
        val list = if (saved == null) defaults else {
            val seen = store.chatDefaultsSeen
            saved + defaults.filter { d -> d.id !in seen && saved.none { it.id == d.id } }
        }
        val uses = store.chatTemplateUses
        return list.sortedByDescending { uses[it.id] ?: 0 }
    }

    /** Şablon listesi kaydedilir; o ana kadarki hazır şablonlar "görüldü" sayılır (silinen geri gelmesin). */
    fun saveTemplates(context: Context, list: List<ChatTemplate>) {
        val store = Store.get(context)
        store.chatDefaultsSeen = store.chatDefaultsSeen + defaults(context).map { it.id }
        store.chatTemplates = list
    }

    fun countUse(context: Context, id: String) {
        val store = Store.get(context)
        store.chatTemplateUses = store.chatTemplateUses + (id to ((store.chatTemplateUses[id] ?: 0) + 1))
    }
}
