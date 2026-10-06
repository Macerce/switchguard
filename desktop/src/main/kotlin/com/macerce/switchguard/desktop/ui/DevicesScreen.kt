package com.macerce.switchguard.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.macerce.switchguard.core.Cloud
import com.macerce.switchguard.core.Demo
import com.macerce.switchguard.core.DeviceSnapshot
import com.macerce.switchguard.desktop.AppGraph
import com.macerce.switchguard.desktop.engine.MonitorEngine
import com.macerce.switchguard.desktop.i18n.Strings
import kotlinx.coroutines.launch
import java.util.Locale
import androidx.compose.runtime.LaunchedEffect
import sh.calvin.reorderable.rememberReorderableLazyGridState
import sh.calvin.reorderable.ReorderableItem
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.animation.core.animateFloatAsState

/**
 * Cihazlar ekranı (ilk aşama): üstte durum metni, izlemeyi başlat/durdur, test alarmı ve demo aç/kapa; altında
 * [com.macerce.switchguard.data.Store.version] her değiştiğinde yenilenen cihaz kartları ızgarası.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DevicesScreen(graph: AppGraph, strings: Strings, modifier: Modifier = Modifier) {
    val store = graph.store
    val engine = graph.engine
    val version by store.version.collectAsState()
    val devices = remember(version) { store.snapshots }
    // Gösterilen sıra: kullanıcının düzeni (DeviceLayout.order); sürüklerken DragOrder'ın yerel sırası.
    val sortedIds = remember(version) { store.deviceLayout.sorted(store.snapshots).map { it.id } }
    val byId = remember(version) { store.snapshots.associateBy { it.id } }
    val order = remember { DragOrder() }
    LaunchedEffect(sortedIds) { order.show(sortedIds) }
    val monitoring = remember(version) { store.monitoringEnabled }
    val demoOn = remember(version) { store.demoMode }
    val canMonitor = remember(version) { store.hasAnyAccount }
    val status by graph.appState.status.collectAsState()
    val warning by graph.appState.warning.collectAsState()

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(modifier = modifier, snackbarHost = { SnackbarHost(snackbar) }, containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // --- Üst çubuk ---
            FlowRow(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(14.dp).clip(CircleShape).background(StatusColors.of(status.first)))
                    Text(status.second.ifEmpty { strings["state_stopped"] }, style = MaterialTheme.typography.titleMedium)
                }
                if (monitoring) {
                    OutlinedButton(onClick = { engine.stop() }) { Text(strings["monitoring_stop"]) }
                } else {
                    Button(onClick = { engine.start() }, enabled = canMonitor) { Text(strings["monitoring_start"]) }
                }
                OutlinedButton(onClick = { engine.testAlarm() }) { Text(strings["test_alarm"]) }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(strings["demo_devices"], style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = demoOn, onCheckedChange = { on ->
                        if (on) {
                            store.enableDemo(strings.demoNames())
                        } else {
                            store.disableDemo()
                            // Telefondaki Actions.disableDemo ile aynı: izlenecek başka kaynak kalmadıysa izleme de durur.
                            if (store.monitoringEnabled && !store.hasAnyAccount) engine.stop()
                        }
                    })
                }
            }

            warning?.let {
                Card(
                    Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                ) {
                    Text(it, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }

            if (demoOn) {
                Text(
                    strings["demo_hint"],
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 8.dp),
                )
            }

            // --- Cihaz kartları ---
            if (devices.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        strings["devices_empty"],
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(32.dp),
                    )
                }
            } else {
                val shown = order.displayed(sortedIds)
                val gridState = rememberLazyGridState()
                val reorder = rememberReorderableLazyGridState(gridState) { from, to ->
                    order.move(from.key as String, to.key as String)
                }
                if (shown.size > 1) {
                    Text(
                        strings["desktop_drag_hint"],
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp),
                    )
                }
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Adaptive(minSize = 340.dp),
                    contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(shown, key = { it }) { id ->
                        val device = byId[id] ?: return@items
                        ReorderableItem(reorder, key = id) { isDragging ->
                            // Kalkan kart: hafif büyür ve gölgelenir; yalnızca çizim katmanı değişir (her karede bileşim kurulmaz).
                            val lift by animateFloatAsState(if (isDragging) 1f else 0f)
                            DeviceCard(
                                modifier = Modifier
                                    .graphicsLayer {
                                        val scale = 1f + 0.03f * lift
                                        scaleX = scale
                                        scaleY = scale
                                        shadowElevation = 16.dp.toPx() * lift
                                        shape = RoundedCornerShape(16.dp)
                                    }
                                    .longPressDraggableHandle(
                                        onDragStarted = { order.begin(shown) },
                                        // Yeni sıra bırakınca bir kez kaydedilir.
                                        onDragStopped = { order.end()?.let { ids -> store.deviceLayout = store.deviceLayout.copy(order = ids) } },
                                    ),
                                device = device,
                                strings = strings,
                                onSwitch = { channel, on ->
                                    // Sonuç motor iş parçacığında gelir ve hiç gelmeyebilir (izleme durursa): beklenmez.
                                    engine.control(device.id, channel, on) { error ->
                                        if (error != null) {
                                            scope.launch {
                                                snackbar.showSnackbar(
                                                    strings.format("control_failed", error.message ?: error.javaClass.simpleName),
                                                )
                                            }
                                        }
                                    }
                                },
                                onDemo = { op, channel -> engine.demo(op, device.id, channel) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DeviceCard(
    device: DeviceSnapshot,
    strings: Strings,
    onSwitch: (channel: Int, on: Boolean) -> Unit,
    onDemo: (op: String, channel: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    device.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                val color = if (device.online) StatusColors.online else StatusColors.offline
                Text(
                    strings[if (device.online) "label_online" else "label_offline"],
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(color).padding(horizontal = 10.dp, vertical = 3.dp),
                )
            }

            if (device.hasEnergy) {
                Text(
                    "${strings["power"]}: ${watts(device.power ?: 0.0)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Kanallar: ad (çok kanallıda), varsa kanal gücü ve aç/kapa anahtarı.
            device.switches.keys.sorted().forEach { channel ->
                val on = device.switches[channel] == true
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (device.isMultiChannel) strings.format("channel_n", channel + 1) else strings[if (on) "label_on" else "label_off"],
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        if (device.isMultiChannel) {
                            device.channelPowerOf(channel)?.let {
                                Text(watts(it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    Switch(checked = on, enabled = device.online, onCheckedChange = { onSwitch(channel, it) })
                }
            }

            if (device.cloud == Cloud.DEMO) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Telefondaki DemoSection ile aynı üç simülasyon: dışarıdan aç/kapa, bağlantı kopması, güç düşüşü.
                    device.switches.keys.sorted().forEach { ch ->
                        DemoButton(
                            if (device.isMultiChannel) strings.format("demo_toggle_channel", ch + 1) else strings["demo_toggle"],
                        ) { onDemo(MonitorEngine.DEMO_TOGGLE, ch) }
                    }
                    DemoButton(strings[if (device.online) "demo_disconnect" else "demo_reconnect"]) {
                        onDemo(MonitorEngine.DEMO_ONLINE, 0)
                    }
                    // Güç düşüşü yalnızca açık ve güç ölçen kanallarda anlamlı.
                    device.switches.keys.sorted()
                        .filter { device.switches[it] == true && device.channelPowerOf(it) != null }
                        .forEach { ch ->
                            val stalled = device.channelPowerOf(ch) == Demo.STALLED_WATTS
                            val label = when {
                                device.isMultiChannel && stalled -> strings.format("demo_power_restore", ch + 1)
                                device.isMultiChannel -> strings.format("demo_power_drop", ch + 1)
                                stalled -> strings["demo_power_restore_single"]
                                else -> strings["demo_power_drop_single"]
                            }
                            DemoButton(label) { onDemo(MonitorEngine.DEMO_POWER, ch) }
                        }
                }
            }
        }
    }
}

@Composable
private fun DemoButton(text: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
        Text(text, style = MaterialTheme.typography.labelMedium)
    }
}

private fun watts(w: Double): String = String.format(Locale.ROOT, if (w < 100) "%.1f W" else "%.0f W", w)
