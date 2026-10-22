package com.macerce.switchguard.desktop.ui

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.macerce.switchguard.core.Cloud
import com.macerce.switchguard.core.DeviceSnapshot
import com.macerce.switchguard.data.LoggedEvent
import com.macerce.switchguard.desktop.AppGraph
import com.macerce.switchguard.desktop.ui.devices.ChannelButtons
import com.macerce.switchguard.desktop.ui.devices.DemoSection
import com.macerce.switchguard.desktop.ui.devices.DeviceActions
import com.macerce.switchguard.desktop.ui.devices.DeviceGlyph
import com.macerce.switchguard.desktop.ui.devices.DeviceRulesSection
import com.macerce.switchguard.desktop.ui.devices.DeviceUi
import com.macerce.switchguard.desktop.ui.devices.EnergySection
import com.macerce.switchguard.desktop.ui.devices.LocalDeviceUi
import com.macerce.switchguard.desktop.ui.devices.RawDataRow
import com.macerce.switchguard.desktop.ui.devices.TimersSection
import com.macerce.switchguard.desktop.ui.devices.rememberDeviceRules
import com.macerce.switchguard.desktop.ui.devices.rememberSwitchControl
import com.macerce.switchguard.desktop.ui.devices.watts
import com.macerce.switchguard.desktop.ui.history.EventRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Cihaz sayfası (telefondaki DeviceDetailScreen.kt, 2.12): büyük güç düğmesi ya da kanal düğmeleri, demo simülasyonu,
 * enerji, eWeLink zamanlayıcıları, izleme anahtarı, olay başına kurallar, cihaz sesi ve kritik cihaz, son olaylar ve ham
 * veri. Kural değişiklikleri motora gider (`MonitorEngine.updateRules`); motor meşgulken de ekran beklemesin diye seçim
 * hemen gösterilir, motor yazınca ayardaki değerle birleşir. İçerik okunabilir genişlikte ortalanır; Esc geri döner.
 */
@Composable
fun DeviceDetailScreen(graph: AppGraph, deviceId: String, onBack: () -> Unit, actions: DeviceActions = remember(graph) { DeviceActions(graph.store) }) {
    val store = graph.store
    val strings = LocalStrings.current
    val version by store.version.collectAsState()
    val logVersion by graph.eventLog.version.collectAsState()

    val device = remember(version) { store.snapshots.firstOrNull { it.id == deviceId } }
    val rulesState = rememberDeviceRules(graph, deviceId)
    val rules = rulesState.rules
    val events by produceState(emptyList<LoggedEvent>(), logVersion, deviceId) {
        value = withContext(Dispatchers.IO) { runCatching { graph.eventLog.recent(deviceId, limit = 15) }.getOrDefault(emptyList()) }
    }

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val deviceUi = remember(graph) {
        DeviceUi(
            send = { id, channel, on, onResult -> graph.engine.control(id, channel, on, onResult) },
            message = { text -> scope.launch { snackbar.showSnackbar(text) } },
        )
    }

    CompositionLocalProvider(LocalDeviceUi provides deviceUi) {
        // Sayfa açılınca odak sayfada: Esc hemen geri döner (odak bir düğmedeyken de olay buraya kadar çıkar).
        val focus = remember { FocusRequester() }
        LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
        Box(
            Modifier.fillMaxSize()
                .onKeyEvent { e -> if (e.type == KeyEventType.KeyUp && e.key == Key.Escape) { onBack(); true } else false }
                .focusRequester(focus)
                .focusable(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(Modifier.readableWidth().fillMaxSize()) {
                PageHeader(device?.name ?: "", onBack = onBack)
                if (device == null) {
                    Text(str("device_missing"), Modifier.padding(24.dp))
                } else {
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        val scroll = rememberScrollState()
                        Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(bottom = 24.dp)) {
                            ControlHero(device, rules.monitored)
                            com.macerce.switchguard.desktop.ui.devices.ConnectionCard(device, graph.eventLog)

                            if (device.cloud == Cloud.DEMO) DemoSection(device) { op, ch -> graph.engine.demo(op, deviceId, ch) }

                            // Enerji ölçmeyen cihazlarda bu bölüm hiç görünmez. SPM/DualR3 veriyi ancak istenince
                            // gönderdiğinden, veri gelmeden de gösterilir (sayfa açıkken istenir).
                            if (device.hasEnergy || device.hasChannelPower) {
                                val live: (suspend () -> Unit)? =
                                    if (device.cloud == Cloud.EWELINK && store.isLoggedIn) ({ actions.requestLiveEnergy(device) }) else null
                                EnergySection(device, graph.metrics, store, live)
                            }
                            if (device.hasEnergy && device.cloud != Cloud.DEMO) {
                                com.macerce.switchguard.desktop.ui.devices.StandbyCard(device, graph.metrics, store)
                            }

                            // Zamanlayıcılar eWeLink cihazlarının kendi özelliği.
                            if (device.cloud == Cloud.EWELINK) TimersSection(device, actions)

                            DeviceRulesSection(graph, rulesState)

                            SectionTitle(str("section_recent_events"))
                            SectionCard {
                                if (events.isEmpty()) {
                                    Text(str("no_events"), Modifier.padding(20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                } else {
                                    events.forEachIndexed { i, e ->
                                        if (i > 0) RowDivider(70.dp)
                                        EventRow(e, showDevice = false)
                                    }
                                }
                            }

                            SectionTitle(str("section_advanced"))
                            SectionCard { RawDataRow(device, actions) }
                        }
                        VerticalScrollbar(
                            rememberScrollbarAdapter(scroll),
                            Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(vertical = 4.dp),
                        )
                    }
                }
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(16.dp))
        }
    }
}

/**
 * Sayfanın üstü: tek kanallı cihazda ortada büyük güç düğmesi (açıkken kart marka renginde), çok kanallıda büyük
 * kanal düğmeleri. Üstünde bağlantı, izleme ve (ölçülüyorsa) anlık güç.
 */
@Composable
private fun ControlHero(device: DeviceSnapshot, monitored: Boolean) {
    val dark = LocalDarkTheme.current
    val single = !device.isMultiChannel && device.switches.size == 1
    val anyOn = device.switches.values.any { it }
    val active = single && anyOn && device.online
    val background: Brush = if (active) Brand.active(dark) else SolidColor(tileColor())
    val content = if (active) Brand.onActive else MaterialTheme.colorScheme.onSurface
    val muted = if (active) Brand.onActive.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(28.dp)).background(background).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            DeviceGlyph(device, active, size = 40.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    str(if (device.online) "label_online_title" else "label_offline_title"),
                    style = MaterialTheme.typography.titleSmall,
                    color = if (device.online) content else StatusColors.offline,
                )
                Text(str(if (monitored) "monitored" else "not_monitored"), style = MaterialTheme.typography.bodySmall, color = muted)
            }
            if (device.hasEnergy && device.online) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(watts(device.power ?: 0.0), style = MaterialTheme.typography.headlineSmall, color = content)
                    Text(str("energy_power"), style = MaterialTheme.typography.labelMedium, color = muted)
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        if (single) {
            val (channel, on) = device.switches.entries.first()
            val control = rememberSwitchControl(device, channel, on)
            PowerButton(
                control.shown, control.busy, device.online, control.toggle, size = 112.dp, onBrand = active,
                modifier = Modifier.testTag("hero-power"),
            )
            Spacer(Modifier.height(12.dp))
            Text(str(if (control.shown) "state_on_title" else "state_off_title"), style = MaterialTheme.typography.headlineSmall, color = content)
            Text(str("desktop_click_to_toggle"), style = MaterialTheme.typography.bodySmall, color = muted)
            Spacer(Modifier.height(4.dp))
        } else {
            ChannelButtons(device, Modifier.fillMaxWidth(), height = 84.dp)
        }
    }
}
