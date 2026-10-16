package com.macerce.switchguard.desktop.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.macerce.switchguard.core.DeviceLayout
import com.macerce.switchguard.core.DeviceSection
import com.macerce.switchguard.data.LiveState
import com.macerce.switchguard.desktop.AppGraph
import com.macerce.switchguard.desktop.i18n.Strings
import com.macerce.switchguard.desktop.ui.devices.AlarmBanner
import com.macerce.switchguard.desktop.ui.devices.BlindCard
import com.macerce.switchguard.desktop.ui.devices.CategoriesDialog
import com.macerce.switchguard.desktop.ui.devices.DeviceTile
import com.macerce.switchguard.desktop.ui.devices.DeviceUi
import com.macerce.switchguard.desktop.ui.devices.EmptyDevices
import com.macerce.switchguard.desktop.ui.devices.GuardCard
import com.macerce.switchguard.desktop.ui.devices.HintIconButton
import com.macerce.switchguard.desktop.ui.devices.LayoutEditBar
import com.macerce.switchguard.desktop.ui.devices.LayoutEditHint
import com.macerce.switchguard.desktop.ui.devices.LayoutSectionHeader
import com.macerce.switchguard.desktop.ui.devices.LocalDeviceUi
import com.macerce.switchguard.desktop.ui.devices.NoAccountCard
import com.macerce.switchguard.desktop.ui.devices.SessionLostCard
import com.macerce.switchguard.desktop.ui.devices.sectionTitle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState

/**
 * Cihazlar sekmesi (telefondaki DevicesScreen.kt, 2.12): üstte koruma kartı (ya da hesap yoksa Ayarlar'a / demoya
 * yönlendiren kart), oturum ve kör kalma uyarıları, altında kutucuk ızgarası. Izgara pencere genişledikçe sütun ekler;
 * küçük kutucuk bir hücre, büyük kutucuk (çok kanallıların varsayılanı) tam satırdır. Öncelikliler ve kategoriler
 * başlıklı bölümlerdir (`DeviceLayout.sections`). Kutucuğa tıklamak cihaz sayfasını açar (`AppState.openDevice`).
 *
 * Düzenleme modu (başlıktaki sürgü simgesi; telefondaki gibi): her kart tam satır ve altında araçlar (öncelik, sıra
 * okları, boyut, kategori). Masaüstüne özgü: bir kartı basılı tutup sürüklemek ya da düzenlemede tutamaktan sürüklemek sırayı değiştirir
 * ([DragOrder]; yalnızca aynı bölüm içinde, bırakınca bir kez kaydedilir). Düzen arayüze aittir (motor okumaz): doğrudan
 * `Store.deviceLayout`'a yazılır; anahtar ve izleme komutları motora gider.
 */
@Composable
fun DevicesScreen(
    graph: AppGraph,
    strings: Strings,
    modifier: Modifier = Modifier,
    /** Kaydırma yeri sekmenin dışında tutulur (AppNav): cihaz sayfasından dönünce liste aynı yerde kalır. */
    gridState: LazyGridState = rememberLazyGridState(),
) {
    val store = graph.store
    val engine = graph.engine
    val version by store.version.collectAsState()
    val connection by LiveState.connection.collectAsState()
    val alarm by graph.appState.alarmLines.collectAsState()
    val warning by graph.appState.warning.collectAsState()
    val blind by graph.appState.blind.collectAsState()

    val devices = remember(version) { store.snapshots }
    val monitoring = remember(version) { store.monitoringEnabled }
    val hasAccount = remember(version) { store.hasAnyAccount }
    val sessionLost = remember(version) { store.sessionLost && !store.isLoggedIn }
    val monitoredIds = remember(version) { store.monitoredIds() }
    val layout = remember(version) { store.deviceLayout }
    val saveLayout: (DeviceLayout) -> Unit = { store.deviceLayout = it }
    var editMode by remember { mutableStateOf(false) }
    var showCategories by remember { mutableStateOf(false) }
    if (showCategories) CategoriesDialog(layout, saveLayout) { showCategories = false }

    // Sürükle-bırak: sürüklerken bölümler bellekteki sıradan kurulur, bırakınca sıra bir kez yazılır.
    val sortedIds = remember(version) { layout.sorted(devices).map { it.id } }
    val order = remember { DragOrder() }
    LaunchedEffect(sortedIds) { order.show(sortedIds) }
    val shownLayout = if (order.dragging) layout.copy(order = order.ids) else layout
    val sections = remember(shownLayout, devices) { shownLayout.sections(devices) }
    val sectionOf = remember(sections) { sections.flatMap { s -> s.devices.map { it.id to s.key } }.toMap() }
    val currentSectionOf by rememberUpdatedState(sectionOf)

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val deviceUi = remember(engine) {
        DeviceUi(
            send = { id, channel, on, onResult -> engine.control(id, channel, on, onResult) },
            message = { text -> scope.launch { snackbar.showSnackbar(text) } },
        )
    }
    var refreshing by remember { mutableStateOf(false) }

    CompositionLocalProvider(LocalDeviceUi provides deviceUi) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.readableWidth(1200.dp).fillMaxSize()) {
                PageHeader(
                    str(if (editMode) "action_edit_layout" else "app_name"),
                    actions = {
                        if (editMode) {
                            TextButton(onClick = { editMode = false }, modifier = Modifier.testTag("edit-done")) { Text(str("action_done")) }
                        } else {
                            if (devices.isNotEmpty()) {
                                HintIconButton(Icons.Rounded.Tune, str("action_edit_layout"), { editMode = true }, Modifier.testTag("edit-layout"))
                            }
                            // Telefondaki çekip yenileme: izleme açıksa motor hemen senkronlar.
                            HintIconButton(
                                Icons.Rounded.Refresh,
                                str("action_refresh"),
                                onClick = {
                                    engine.syncNow()
                                    scope.launch {
                                        refreshing = true
                                        delay(1200)
                                        refreshing = false
                                    }
                                },
                                enabled = monitoring && hasAccount && !refreshing,
                                content = if (refreshing) ({ CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) }) else null,
                            )
                        }
                    },
                )
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    val reorder = rememberReorderableLazyGridState(gridState) { from, to ->
                        val a = from.key as? String
                        val b = to.key as? String
                        // Bölümler arası taşıma yok: öncelik ve kategori kendi araçlarıyla değişir.
                        if (a != null && b != null && currentSectionOf[a] != null && currentSectionOf[a] == currentSectionOf[b]) {
                            order.move(a, b)
                        }
                    }
                    val full: (LazyGridItemSpanScope.() -> GridItemSpan) = { GridItemSpan(maxLineSpan) }
                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Adaptive(minSize = 220.dp),
                        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize().testTag("devices-grid"),
                    ) {
                        if (alarm.isNotEmpty()) {
                            item(key = "alarm", span = full) { AlarmBanner(alarm) { graph.dismissAlarm() } }
                        }
                        if (!editMode) {
                            item(key = "guard", span = full) {
                                if (hasAccount) {
                                    GuardCard(
                                        connection.state, connection.detail, connection.lastSync, monitoring, devices,
                                        onStart = { engine.start() }, onStop = { engine.stop() },
                                    )
                                } else {
                                    NoAccountCard(
                                        onAccounts = { graph.appState.openSettings("ACCOUNTS") },
                                        // Motor üzerinden (anlık görüntülerle yarışmasın).
                                        onDemo = { engine.setDemo(true, strings.demoNames()) },
                                    )
                                }
                            }
                            val sessionText = warning ?: if (sessionLost) strings["session_lost_body"] else null
                            if (sessionText != null) {
                                item(key = "session", span = full) {
                                    SessionLostCard(sessionText) { graph.appState.openSettings("ACCOUNTS") }
                                }
                            }
                            blind?.let { text -> item(key = "blind", span = full) { BlindCard(text) } }
                        }
                        if (hasAccount && devices.isEmpty()) {
                            item(key = "empty", span = full) { EmptyDevices(showTuyaHint = store.hasTuya) }
                        }
                        if (editMode) item(key = "edit-hint", span = full) { LayoutEditHint { showCategories = true } }
                        for (section in sections) {
                            // Düzenleme dışında boş kategori başlığı gösterilmez.
                            if (section.devices.isEmpty() && !editMode) continue
                            val headed = section.kind != DeviceSection.Kind.ALL
                            val collapsed = headed && section.key in layout.collapsed && !editMode
                            if (headed) {
                                item(key = "h-" + section.key, span = full) {
                                    LayoutSectionHeader(sectionTitle(section), section.devices.size, collapsed) {
                                        saveLayout(layout.toggleCollapsed(section.key))
                                    }
                                }
                            }
                            if (section.devices.isEmpty()) {
                                item(key = "e-" + section.key, span = full) {
                                    Text(
                                        str("category_no_devices"),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 4.dp),
                                    )
                                }
                            }
                            if (!collapsed) {
                                itemsIndexed(
                                    section.devices,
                                    key = { _, d -> d.id },
                                    // Düzenlemede her kart tam satır: düzen araçları sığsın.
                                    span = { _, d -> if (editMode || layout.isLarge(d)) GridItemSpan(maxLineSpan) else GridItemSpan(1) },
                                ) { i, device ->
                                    ReorderableItem(reorder, key = device.id) { isDragging ->
                                        // Kalkan kart: hafif büyür ve gölgelenir; yalnızca çizim katmanı değişir.
                                        val lift by animateFloatAsState(if (isDragging) 1f else 0f)
                                        val drag = Modifier.draggableHandle(
                                            onDragStarted = { order.begin(sortedIds) },
                                            // Yeni sıra bırakınca bir kez kaydedilir.
                                            onDragStopped = { order.end()?.let { ids -> store.deviceLayout = store.deviceLayout.copy(order = ids) } },
                                        )
                                        DeviceTile(
                                            device,
                                            large = editMode || layout.isLarge(device),
                                            monitored = device.id in monitoredIds,
                                            priority = device.id in layout.priority,
                                            onClick = { if (!editMode) graph.appState.openDevice.value = device.id },
                                            editBar = if (editMode) {
                                                {
                                                    LayoutEditBar(
                                                        device, layout, devices,
                                                        canUp = i > 0, canDown = i < section.devices.lastIndex,
                                                        dragHandle = drag, onChange = saveLayout,
                                                    )
                                                }
                                            } else null,
                                            modifier = Modifier
                                                .graphicsLayer {
                                                    val scale = 1f + 0.02f * lift
                                                    scaleX = scale
                                                    scaleY = scale
                                                    shadowElevation = 16.dp.toPx() * lift
                                                    shape = RoundedCornerShape(24.dp)
                                                    clip = false
                                                }
                                                .longPressDraggableHandle(
                                                    onDragStarted = { order.begin(sortedIds) },
                                                    onDragStopped = { order.end()?.let { ids -> store.deviceLayout = store.deviceLayout.copy(order = ids) } },
                                                ),
                                        )
                                    }
                                }
                            }
                        }
                    }
                    VerticalScrollbar(
                        rememberScrollbarAdapter(gridState),
                        Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(vertical = 4.dp),
                    )
                }
            }
            SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(16.dp))
        }
    }
}
