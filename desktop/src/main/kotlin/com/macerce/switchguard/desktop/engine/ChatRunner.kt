package com.macerce.switchguard.desktop.engine

import com.macerce.switchguard.core.ChatCommand
import com.macerce.switchguard.core.ChatTemplate
import com.macerce.switchguard.core.DeviceGroup
import com.macerce.switchguard.core.DeviceSnapshot
import com.macerce.switchguard.core.EnergyWatch
import com.macerce.switchguard.core.Scene
import com.macerce.switchguard.data.MetricsLog
import com.macerce.switchguard.data.RuntimeState
import com.macerce.switchguard.data.Store
import com.macerce.switchguard.desktop.i18n.Strings

/**
 * Sohbet komutlarını yürütür ve yanıt metnini üretir (telefondaki `service/ChatRunner.kt`); Telegram ve uygulama içi
 * sohbet aynı yolu kullanır. Motor iş parçacığında çağrılır: aç/kapat, sahne ve evde/dışarıda motorun kendi
 * işlevleriyle yapılır ([switchNow], [sceneNow], [awayNow]).
 *
 * Telefondan fark: dış sıcaklık burada okunmaz (ağ işi motoru bekletmesin); motor okuyup [temperature]'a verir.
 */
class ChatRunner(
    private val store: Store,
    private val metrics: MetricsLog,
    private val runtime: RuntimeState,
    private val strings: () -> Strings,
    private val now: () -> Long,
    /** Cihazı ya da grubu açar/kapatır; başarısızsa hata fırlatır. */
    private val switchNow: (DeviceSnapshot, Int, Boolean) -> Unit,
    private val sceneNow: (Scene, String?) -> Pair<Int, Int>,
    private val awayNow: (Boolean, String?) -> Unit,
) {

    /** [control] false ise aç/kapat ve sahne reddedilir (Telegram'dan kontrol kapalı). Sıcaklık için [temperature]. */
    fun run(intent: ChatCommand.Intent, by: String?, control: Boolean): String {
        val s = strings()
        return when (intent) {
            is ChatCommand.Intent.Switch -> if (!control) s["tg_control_off"] else switch(intent)
            is ChatCommand.Intent.Status -> status(intent.query)
            is ChatCommand.Intent.Power -> power(intent.query)
            ChatCommand.Intent.Temperature -> temperature(null)
            is ChatCommand.Intent.SetPresence -> {
                awayNow(intent.away, by)
                s[if (intent.away) "tg_presence_away" else "tg_presence_home"]
            }
            is ChatCommand.Intent.RunScene -> {
                if (!control) return s["tg_control_off"]
                val scenes = store.scenes
                val scene = Scene.find(intent.query, scenes)
                    ?: return s.format("tg_scene_not_found", intent.query, scenes.joinToString(", ") { it.name }.ifEmpty { "—" })
                val (ok, failed) = sceneNow(scene, by)
                s.format("tg_scene_done", scene.name, ok, failed)
            }
            ChatCommand.Intent.Help -> s["chat_help"]
            ChatCommand.Intent.Ack -> s["chat_ack_app"]
        }
    }

    /**
     * Dış sıcaklık: [fresh] motorun az önce okuduğu değer; okunamadıysa son bilinen değer, o da yoksa hata. Konum
     * yoksa nereden ayarlanacağı söylenir.
     */
    fun temperature(fresh: Double?): String {
        val s = strings()
        if (store.latitude == null || store.longitude == null) return s["desktop_chat_temp_unknown"]
        val t = fresh ?: runtime.outdoorTemp ?: return s["chat_temp_failed"]
        return s.format("chat_temp", String.format(s.locale, "%.1f", t))
    }

    private fun switch(intent: ChatCommand.Intent.Switch): String {
        val s = strings()
        val labels = s.eventStrings().labels
        val devices = store.snapshotsWithGroups
        val state = low(if (intent.on) labels.on else labels.off)
        if (intent.targets.all { it.isBlank() }) return s["chat_switch_usage"]
        return intent.targets.joinToString("\n") { target ->
            when (val t = ChatCommand.resolve(target, devices)) {
                is ChatCommand.Target.One -> {
                    val label = if (t.device.isMultiChannel) labels.channel(t.device.name, t.channel + 1) else t.device.name
                    apply(t.device, t.channel, intent.on, label, state)
                }
                is ChatCommand.Target.Many -> t.devices.flatMap { d ->
                    val channels = if (DeviceGroup.isGroup(d.id)) listOf(0) else d.switches.keys.sorted()
                    channels.map { ch -> apply(d, ch, intent.on, if (d.isMultiChannel) labels.channel(d.name, ch + 1) else d.name, state) }
                }.joinToString("\n")
                is ChatCommand.Target.NeedChannel -> s.format("chat_need_channel", t.device.name, t.device.switches.size)
                is ChatCommand.Target.Ambiguous -> s.format("tg_ambiguous", t.names.joinToString(", "))
                ChatCommand.Target.NotFound -> s.format("chat_not_found", target)
            }
        }
    }

    private fun apply(device: DeviceSnapshot, channel: Int, on: Boolean, label: String, state: String): String {
        if (!device.online) return "⚠️ $label: ${low(strings().eventStrings().labels.offline)}"
        val result = runCatching { switchNow(device, channel, on) }
        return if (result.isSuccess) "✅ $label: $state" else "⚠️ $label: ${result.exceptionOrNull()?.message.orEmpty()}"
    }

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

    /** "🟢 Salon: açık · 120 W" / çok kanallıda "1: açık, 2: kapalı". */
    private fun line(d: DeviceSnapshot): String {
        val labels = strings().eventStrings().labels
        if (!d.online) return "⚪ ${d.name}: ${low(labels.offline)}"
        val state = if (d.isMultiChannel) {
            d.switches.toSortedMap().entries.joinToString(", ") { (ch, on) -> "${ch + 1}: ${if (on) low(labels.on) else low(labels.off)}" }
        } else if (d.switches.values.any { it }) low(labels.on) else low(labels.off)
        val icon = if (d.switches.values.any { it }) "🟢" else "⚫"
        val watts = d.power?.let { " · ${watts(it)} W" }.orEmpty()
        return "$icon ${d.name}: $state$watts"
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

    /** Uyarı metinlerindeki "AÇIK/KAPALI" sohbette cümle içinde küçük yazılır ("Salon: açık"); Türkçe İ/ı korunur. */
    private fun low(word: String) = word.lowercase(strings().locale)

    private fun watts(w: Double) = if (w >= 100) w.toInt().toString() else String.format(strings().locale, "%.1f", w)

    companion object {
        /** Hiç şablon kaydedilmediyse gösterilen hazır şablonlar. */
        fun defaults(s: Strings): List<ChatTemplate> = listOf(
            ChatTemplate("d-status", s["chat_tpl_status"], s["chat_tpl_status_text"]),
            ChatTemplate("d-power", s["chat_tpl_power"], s["chat_tpl_power_text"]),
            ChatTemplate("d-temp", s["chat_tpl_temp"], s["chat_tpl_temp_text"]),
            ChatTemplate("d-home", s["chat_tpl_home"], s["chat_tpl_home_text"]),
            ChatTemplate("d-away", s["chat_tpl_away"], s["chat_tpl_away_text"]),
            ChatTemplate("d-help", s["chat_tpl_help"], s["chat_tpl_help_text"]),
        )

        fun templates(store: Store, s: Strings): List<ChatTemplate> = store.chatTemplates ?: defaults(s)
    }
}
