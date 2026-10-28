package com.macerce.switchguard.service

import android.content.Context
import com.macerce.switchguard.R
import com.macerce.switchguard.core.ChatCommand
import com.macerce.switchguard.core.ChatTemplate
import com.macerce.switchguard.core.DeviceGroup
import com.macerce.switchguard.core.DeviceSnapshot
import com.macerce.switchguard.core.Scene
import com.macerce.switchguard.data.EventLog
import com.macerce.switchguard.data.MetricsLog
import com.macerce.switchguard.data.RuntimeState
import com.macerce.switchguard.data.Store
import com.macerce.switchguard.ui.Actions
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Sohbet komutlarını yürütür ve yanıt metnini üretir; Telegram ve uygulama içi sohbet aynı yolu kullanır.
 * Ağ işi yapar: ana iş parçacığından çağırma.
 */
object ChatRunner {

    /** Anlaşılmayan mesajda null. [control] false ise aç/kapat ve sahne reddedilir (Telegram'dan kontrol kapalı). */
    suspend fun answer(context: Context, text: String, by: String? = null, control: Boolean = true): String? {
        val intent = ChatCommand.parse(text) ?: return null
        return run(context, intent, by, control)
    }

    suspend fun run(context: Context, intent: ChatCommand.Intent, by: String?, control: Boolean): String = withContext(Dispatchers.IO) {
        val store = Store.get(context)
        when (intent) {
            is ChatCommand.Intent.Switch ->
                if (!control) context.getString(R.string.tg_control_off) else switch(context, intent)
            is ChatCommand.Intent.Status -> status(context, intent.query)
            is ChatCommand.Intent.Power -> power(context, intent.query)
            ChatCommand.Intent.Temperature -> temperature(context)
            is ChatCommand.Intent.SetPresence -> {
                Actions.setAway(context, intent.away, by)
                context.getString(if (intent.away) R.string.tg_presence_away else R.string.tg_presence_home)
            }
            is ChatCommand.Intent.RunScene -> {
                if (!control) return@withContext context.getString(R.string.tg_control_off)
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

    private suspend fun switch(context: Context, intent: ChatCommand.Intent.Switch): String {
        val store = Store.get(context)
        val labels = EventLog.strings(context).labels
        val devices = store.snapshotsWithGroups
        val state = if (intent.on) low(context, labels.on) else low(context, labels.off)
        if (intent.targets.all { it.isBlank() }) return context.getString(R.string.chat_switch_usage)
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
                is ChatCommand.Target.NeedChannel -> context.getString(R.string.chat_need_channel, t.device.name, t.device.switches.size)
                is ChatCommand.Target.Ambiguous -> context.getString(R.string.tg_ambiguous, t.names.joinToString(", "))
                ChatCommand.Target.NotFound -> context.getString(R.string.chat_not_found, target)
            }
        }.joinToString("\n")
    }

    private suspend fun apply(context: Context, device: DeviceSnapshot, channel: Int, on: Boolean, label: String, state: String): String {
        if (!device.online) return "⚠️ $label: ${low(context, EventLog.strings(context).labels.offline)}"
        val result = Actions.setSwitch(context, device, channel, on)
        return if (result.isSuccess) "✅ $label: $state" else "⚠️ $label: ${result.exceptionOrNull()?.message.orEmpty()}"
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
    private fun low(context: Context, word: String) = word.lowercase(context.resources.configuration.locales[0])

    private fun watts(w: Double) = if (w >= 100) w.toInt().toString() else String.format(Locale.getDefault(), "%.1f", w)

    /** Hiç şablon kaydedilmediyse gösterilen hazır şablonlar. */
    fun defaults(context: Context): List<ChatTemplate> = listOf(
        ChatTemplate("d-status", context.getString(R.string.chat_tpl_status), context.getString(R.string.chat_tpl_status_text)),
        ChatTemplate("d-power", context.getString(R.string.chat_tpl_power), context.getString(R.string.chat_tpl_power_text)),
        ChatTemplate("d-temp", context.getString(R.string.chat_tpl_temp), context.getString(R.string.chat_tpl_temp_text)),
        ChatTemplate("d-home", context.getString(R.string.chat_tpl_home), context.getString(R.string.chat_tpl_home_text)),
        ChatTemplate("d-away", context.getString(R.string.chat_tpl_away), context.getString(R.string.chat_tpl_away_text)),
        ChatTemplate("d-help", context.getString(R.string.chat_tpl_help), context.getString(R.string.chat_tpl_help_text)),
    )

    fun templates(context: Context): List<ChatTemplate> = Store.get(context).chatTemplates ?: defaults(context)
}
