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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import com.macerce.switchguard.core.DeviceRules
import androidx.compose.ui.platform.testTag
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.Icons
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.draw.scale

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
    val rules = remember(version) { store.rules }
    var optionsFor by remember { mutableStateOf<String?>(null) }
    val cardSize = remember(version) { store.cardSize }
    val compactIds = remember(version) { store.deviceLayout.compact }
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
                    // Motor üzerinden (anlık görüntülerle yarışmasın); hesap kalmadıysa izleme de durur.
                    Switch(checked = demoOn, onCheckedChange = { engine.setDemo(it, strings.demoNames()) }, modifier = Modifier.testTag("demo-switch"))
                }
                CardSizePicker(cardSize, strings) { store.cardSize = it }
            }

            warning?.let {
                Card(
                    Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                ) {
                    Text(it, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                }
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
                    columns = GridCells.Adaptive(minSize = CardStyle.gridMinWidth(cardSize)),
                    contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(if (cardSize == "large") 16.dp else 10.dp),
                    verticalArrangement = Arrangement.spacedBy(if (cardSize == "large") 16.dp else 10.dp),
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
                                style = CardStyle.of(cardSize, id in compactIds),
                                monitored = (rules[id] ?: DeviceRules()).monitored,
                                onOptions = { optionsFor = id },
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
                            )
                        }
                    }
                }
            }
        }
    }
    optionsFor?.let { id ->
        val device = byId[id]
        if (device == null) {
            optionsFor = null // cihaz listeden çıktı
        } else {
            DeviceOptionsDialog(
                device = device,
                initialRules = remember(id) { store.rulesFor(id) },
                compact = id in compactIds,
                strings = strings,
                onRules = { change -> engine.updateRules(id, change) },
                // Düzen arayüze ait (motor okumaz): doğrudan yazılır.
                onCompact = { on -> store.deviceLayout = store.deviceLayout.let { l -> if ((id in l.compact) != on) l.toggleCompact(id) else l } },
                onDemo = { op, ch -> engine.demo(op, id, ch) },
                onDismiss = { optionsFor = null },
            )
        }
    }
}

/**
 * Cihaz kartı. Görünüm [style]'a göre (genel kart boyutu ya da cihaza özel "küçük kart"): büyük ve orta kart ad, durum,
 * güç ve kanalları gösterir; küçük kart tek satırdır. Cihaza özel her şey (izleme, kurallar, küçük kart, demo
 * simülasyonu) ⋮ → "Seçenekler…" penceresindedir ([onOptions]); izlenmeyen cihazda kartta "İzlenmiyor" yazar.
 */
@Composable
private fun DeviceCard(
    device: DeviceSnapshot,
    strings: Strings,
    style: CardStyle,
    monitored: Boolean,
    onOptions: () -> Unit,
    onSwitch: (channel: Int, on: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (style == CardStyle.SMALL) {
        SmallDeviceCard(device, strings, monitored, onOptions, onSwitch, modifier)
        return
    }
    val medium = style == CardStyle.MEDIUM
    Card(
        modifier.fillMaxWidth(),
        shape = RoundedCornerShape(if (medium) 12.dp else 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            Modifier.padding(start = if (medium) 12.dp else 16.dp, end = 4.dp, top = if (medium) 6.dp else 8.dp, bottom = if (medium) 10.dp else 14.dp),
            verticalArrangement = Arrangement.spacedBy(if (medium) 4.dp else 8.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    device.name,
                    style = if (medium) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                StatusBadge(device.online, strings, medium)
                OptionsMenu(strings, onOptions)
            }
            Column(Modifier.padding(end = 12.dp), verticalArrangement = Arrangement.spacedBy(if (medium) 2.dp else 6.dp)) {
                if (!monitored) NotMonitored(strings)
                if (device.hasEnergy) {
                    Text(
                        "${strings["power"]}: ${watts(device.power ?: 0.0)}",
                        style = if (medium) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
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
                                style = if (medium) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
                            )
                            if (device.isMultiChannel) {
                                device.channelPowerOf(channel)?.let {
                                    Text(watts(it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        Switch(
                            checked = on, enabled = device.online, onCheckedChange = { onSwitch(channel, it) },
                            modifier = if (medium) Modifier.scale(0.85f) else Modifier,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Küçük kart (telefondaki küçük kart gibi): durum noktası, ad, tek kanallıda anahtar, çok kanallıda "açık/toplam"
 * kanal sayısı ve ⋮. Kanalları tek tek açıp kapatmak için büyük ya da orta kart kullanılır.
 */
@Composable
private fun SmallDeviceCard(
    device: DeviceSnapshot,
    strings: Strings,
    monitored: Boolean,
    onOptions: () -> Unit,
    onSwitch: (channel: Int, on: Boolean) -> Unit,
    modifier: Modifier,
) {
    Card(
        modifier.fillMaxWidth().testTag("card-small-${device.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(Modifier.fillMaxWidth().padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(if (device.online) StatusColors.online else StatusColors.offline))
            Column(Modifier.weight(1f)) {
                Text(device.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (!monitored) NotMonitored(strings)
            }
            if (device.isMultiChannel) {
                Text("${device.switches.values.count { it }}/${device.switches.size}", style = MaterialTheme.typography.bodyMedium)
            } else {
                device.switches.keys.firstOrNull()?.let { ch ->
                    Switch(
                        checked = device.switches[ch] == true, enabled = device.online, onCheckedChange = { onSwitch(ch, it) },
                        modifier = Modifier.scale(0.8f),
                    )
                }
            }
            OptionsMenu(strings, onOptions)
        }
    }
}

@Composable
private fun StatusBadge(online: Boolean, strings: Strings, small: Boolean) {
    Text(
        strings[if (online) "label_online" else "label_offline"],
        style = if (small) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        modifier = Modifier.clip(RoundedCornerShape(50)).background(if (online) StatusColors.online else StatusColors.offline)
            .padding(horizontal = if (small) 8.dp else 10.dp, vertical = if (small) 2.dp else 3.dp),
    )
}

@Composable
private fun NotMonitored(strings: Strings) {
    Text(strings["desktop_not_monitored"], style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
}

/** ⋮ menüsü: "Seçenekler…" (cihaza özel her şey bu pencerede). */
@Composable
private fun OptionsMenu(strings: Strings, onOptions: () -> Unit) {
    Box {
        var menu by remember { mutableStateOf(false) }
        IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, contentDescription = strings["desktop_options"]) }
        DropdownMenu(menu, { menu = false }) {
            DropdownMenuItem(text = { Text(strings["desktop_options"]) }, onClick = {
                menu = false
                onOptions()
            })
        }
    }
}

/** Üst çubukta genel kart boyutu: Büyük / Orta / Küçük. */
@Composable
private fun CardSizePicker(size: String, strings: Strings, onPick: (String) -> Unit) {
    val options = listOf("large" to strings["desktop_card_large"], "medium" to strings["desktop_card_medium"], "small" to strings["desktop_card_small"])
    Box {
        var open by remember { mutableStateOf(false) }
        OutlinedButton(onClick = { open = true }, modifier = Modifier.testTag("card-size")) {
            Text("${strings["desktop_card_size"]}: ${options.firstOrNull { it.first == size }?.second ?: options[0].second}")
        }
        DropdownMenu(open, { open = false }) {
            for ((value, label) in options) DropdownMenuItem(text = { Text(label) }, onClick = {
                open = false
                onPick(value)
            })
        }
    }
}

private fun watts(w: Double): String = String.format(Locale.ROOT, if (w < 100) "%.1f W" else "%.0f W", w)
