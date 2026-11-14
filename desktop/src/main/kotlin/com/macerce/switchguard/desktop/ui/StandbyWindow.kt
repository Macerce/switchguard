package com.macerce.switchguard.desktop.ui

import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.key
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.zIndex
import com.macerce.switchguard.core.StandbyGrid
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Sensors
import com.macerce.switchguard.desktop.ui.automations.stateKey
import com.macerce.switchguard.desktop.ui.automations.icon
import com.macerce.switchguard.core.SensorKind
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.draw.drawBehind
import com.macerce.switchguard.core.DeviceKinds
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Grain
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Thunderstorm
import androidx.compose.material.icons.rounded.Umbrella
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material.icons.rounded.WbCloudy
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import com.macerce.switchguard.api.WeatherClient
import com.macerce.switchguard.core.AlertAction
import com.macerce.switchguard.core.DeviceGroup
import com.macerce.switchguard.core.DurationText
import com.macerce.switchguard.core.StandbyBoard
import com.macerce.switchguard.core.StandbyBoard.UpcomingKind
import com.macerce.switchguard.data.ConnState
import com.macerce.switchguard.data.LoggedEvent
import com.macerce.switchguard.desktop.AppGraph
import com.macerce.switchguard.desktop.i18n.Strings
import com.macerce.switchguard.desktop.platform.DisplayWake
import com.macerce.switchguard.desktop.ui.history.icon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import androidx.compose.runtime.CompositionLocalProvider
import com.macerce.switchguard.desktop.ui.devices.DeviceUi
import com.macerce.switchguard.desktop.ui.devices.LocalDeviceUi
import com.macerce.switchguard.desktop.ui.devices.rememberSwitchControl
import com.macerce.switchguard.core.DeviceSnapshot

/**
 * Bekleme ekranı (telefon 2.25 `StandbyActivity`): çerçevesiz, ekranı kaplayan koyu pano — saat, hava, kritik durum,
 * anlık güç (yarım daire), son hareketler ve yakında çalışacak işler. [com.macerce.switchguard.desktop.AppState.standbyOpen]
 * doğruyken açıktır. Açıkken ekranın boşta kapanma sayacı her dakika sıfırlanır (bilgisayarın uyku ayarı değişmez).
 * Esc kapatır; tıklayınca "Uygulamayı aç" ve "Kapat" çıkar. Alarm penceresi her zaman üstte olduğundan bunun önüne geçer.
 */
@Composable
fun StandbyWindow(graph: AppGraph) {
    val open by graph.appState.standbyOpen.collectAsState()
    if (!open) return
    val saver by graph.appState.standbySaver.collectAsState()
    val strings = graph.strings()
    val close = {
        graph.appState.standbyOpen.value = false
        graph.appState.standbySaver.value = false
    }
    val state = rememberWindowState(placement = WindowPlacement.Fullscreen, position = WindowPosition(0.dp, 0.dp))
    Window(onCloseRequest = close, state = state, title = strings["board_menu"], undecorated = true, alwaysOnTop = saver) {
        LaunchedEffect(Unit) {
            window.toFront()
            while (true) {
                // Pano kalıcı: açık kaldıkça ekranın boşta kapanma sayacı sıfırlanır (ekran koruyucudan açılsa da).
                DisplayWake.keepOn()
                delay(50_000)
            }
        }
        val focus = remember { FocusRequester() }
        LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
        Box(
            Modifier.fillMaxSize().focusRequester(focus).focusable().onPreviewKeyEvent {
                // Pano kalıcıdır (ekran koruyucudan açılsa da): yalnız Esc ya da Kapat düğmesi kapatır.
                if (it.type == KeyEventType.KeyDown && it.key == Key.Escape) { close(); true } else false
            },
        ) {
            ProvideStrings(strings) { StandbyBoardScreen(
                graph, strings,
                onOpenApp = {
                    close()
                    graph.appState.showMainWindow()
                },
                onClose = close,
            ) }
        }
    }
}

// Telefon 2.31 TV panosuyla aynı görünüm: koyu zemin, yavaş süzülen ışıklar, cam kartlar, durumla değişen tek vurgu rengi.
private val Bg = Color(0xFF04070A)
private val Ink = Color(0xFFEAF2F2)
private val Dim = Color(0xFF8A9A9C)
private val Faint = Color.White.copy(alpha = 0.055f)
private val Edge = Color.White.copy(alpha = 0.08f)
private val Muted = Color(0xFF55646A)
private val Teal = Color(0xFF2EE6C0)
private val Deep = Color(0xFF1B6DFF)
private val Amber = Color(0xFFFFB648)
private val Red = Color(0xFFFF5A5F)
private val Idle = Color(0xFF7D8A90)
private val Sheet = Color(0xF2101619)

/** Cam kart: hafif beyaz zemin + ince kenar; [tint] verilirse o renkte yatay ışık (uyarı/alarm). */
private fun Modifier.glass(radius: Int = 22, tint: Color? = null): Modifier {
    val shape = RoundedCornerShape(radius.dp)
    val c = clip(shape)
    return if (tint != null) {
        c.background(Brush.horizontalGradient(listOf(tint.copy(alpha = 0.22f), tint.copy(alpha = 0.06f))), shape)
            .border(1.dp, tint.copy(alpha = 0.4f), shape)
    } else c.background(Faint, shape).border(1.dp, Edge, shape)
}

/**
 * Zemin: üç yumuşak ışık farklı hız ve yönlerde yavaşça süzülür (telefon TvHome `aurora`). Işıklar çok yavaş hareket
 * ettiğinden saniyede ~20 kare yeter: işlemci yorulmaz. Zaman yalnız çizimde okunur, ekran yeniden oluşturulmaz.
 */
@Composable
private fun Modifier.aurora(accent: Color): Modifier {
    val time = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while (true) {
            withFrameNanos { time.floatValue = (it - start) / 1_000_000_000f }
            delay(50)
        }
    }
    val tint = rememberUpdatedState(accent)
    return this.drawBehind {
        val t = time.floatValue
        drawRect(Bg)
        val w = size.width
        val h = size.height
        fun glow(color: Color, alpha: Float, cx: Float, cy: Float, r: Float) {
            val c = Offset(cx, cy)
            val stops = listOf(1f, 0.72f, 0.42f, 0.18f, 0.05f, 0f).map { color.copy(alpha = alpha * it) }
            drawCircle(Brush.radialGradient(stops, center = c, radius = r), radius = r, center = c)
        }
        val breathe = 0.85f + 0.15f * sin(t * 0.7f)
        glow(tint.value, 0.26f * breathe, w * (0.22f + 0.16f * sin(t * 0.061f)), h * (0.18f + 0.14f * cos(t * 0.047f)), w * 0.62f)
        glow(Deep, 0.17f, w * (0.82f + 0.12f * sin(t * 0.039f + 1.7f)), h * (0.92f + 0.12f * cos(t * 0.053f + 0.6f)), w * 0.55f)
        glow(Color(0xFF7A5CFF), 0.09f, w * (0.55f + 0.28f * sin(t * 0.027f + 3.1f)), h * (0.55f + 0.22f * sin(t * 0.044f + 0.9f)), w * 0.45f)
    }
}

/** Durum noktası: canlıyken dışa doğru yayılan halka. */
@Composable
private fun PulseDot(color: Color, pulsing: Boolean) {
    val t = rememberInfiniteTransition(label = "pulse")
    val p by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2_200, easing = LinearEasing)), label = "p")
    Box(
        Modifier.size(12.dp).drawBehind {
            if (pulsing) drawCircle(color.copy(alpha = 0.45f * (1f - p)), radius = size.minDimension / 2 * (1f + 1.6f * p))
            drawCircle(color.copy(alpha = 0.35f), radius = size.minDimension)
            drawCircle(color)
        },
    )
}

private data class PowerView(val series: List<Double>, val todayKwh: Double)

/** Izgara hücresi (~44 dp) ve aralık: 1280 px pencerede ~28 sütun (telefon 2.31.3 ızgarasının masaüstü eşi). */
private val CellTarget = 44.dp
private val Gap = 6.dp

private fun channelsOf(d: DeviceSnapshot): List<Int> =
    if (d.isMultiChannel) d.switches.keys.sorted() else listOf(d.switches.keys.minOrNull() ?: 0)

/**
 * Bekleme panosu: telefon ana ekranı gibi widget ızgarası (saat, hava, durum, güç, son hareketler, yakında, kurallar,
 * her cihaz kanalı ve her sensör ayrı widget). Düzenlemede (tıkla → Düzenle ya da basılı tut) widget sürüklenerek
 * taşınır, köşe tutamağıyla boyutlanır, × ile kaldırılır, "Ekle" ile geri gelir; düzen `Store.standbyGrid`'de.
 */
@Composable
internal fun StandbyBoardScreen(graph: AppGraph, strings: Strings, onOpenApp: () -> Unit, onClose: () -> Unit) {
    val store = graph.store
    val version by store.version.collectAsState()
    val eventsVersion by graph.eventLog.version.collectAsState()
    val status by graph.appState.status.collectAsState()
    val alarmLines by graph.appState.alarmLines.collectAsState()

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000 - now % 1000)
        }
    }
    val minute = now / 60_000

    val snapshots = remember(version) { store.snapshots }
    val tiles = remember(version) { store.deviceLayout.sorted(store.snapshotsWithGroups).filterNot { it.sensorOnly } }
    val sensors = remember(version) { snapshots.filter { it.hasSensors } }
    val rules = remember(version) { store.automations.filter { it.enabled } }
    val ruleDevices = remember(version) { store.snapshotsWithGroups }
    val ruleScenes = remember(version) { store.scenes }
    val ruleCycles = remember(version) { store.cycles }
    var controlError by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(controlError) { if (controlError != null) { delay(5000); controlError = null } }
    val deviceUi = remember(graph) {
        DeviceUi(
            send = { id, channel, on, onResult -> graph.engine.control(id, channel, on, onResult) },
            message = { controlError = it },
        )
    }
    val monitored = remember(version) { store.monitoredIds() }
    val away = remember(version, minute) { store.isAway() }
    val lat = remember(version) { store.latitude }
    val lon = remember(version) { store.longitude }
    val dimAtNight = remember(version) { store.standbyDim }
    val eventStrings = remember(strings) { strings.eventStrings() }

    val recent by produceState(emptyList<LoggedEvent>(), eventsVersion) {
        value = withContext(Dispatchers.IO) { runCatching { graph.eventLog.recent(limit = 40) }.getOrDefault(emptyList()) }
            .filter { it.kind != "MONITOR_GAP" }.take(12)
    }
    val upcoming = remember(version, minute) {
        StandbyBoard.upcoming(
            store.automations, store.cycles, store.cycleRuns, store.chatTemplates.orEmpty(),
            graph.runtime.templateRuns, lat, lon, System.currentTimeMillis(),
        ).take(10)
    }
    val energyIds = remember(snapshots) { snapshots.filter { it.hasEnergy && !DeviceGroup.isGroup(it.id) }.map { it.id } }
    val livePower = remember(snapshots) { snapshots.filter { it.online && !DeviceGroup.isGroup(it.id) }.sumOf { it.power ?: 0.0 } }
    val power by produceState<PowerView?>(null, energyIds, minute) {
        if (energyIds.isEmpty()) { value = null; return@produceState }
        value = withContext(Dispatchers.IO) {
            runCatching {
                val t = System.currentTimeMillis()
                val from = t - 6 * 3_600_000L
                val series = StandbyBoard.totalPower(energyIds.map { graph.metrics.powerSamples(it, from, t) }, from, t, 73)
                val midnight = Calendar.getInstance().apply {
                    timeInMillis = t; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                PowerView(series, energyIds.sumOf { graph.metrics.kwh(it, midnight, t) })
            }.getOrNull()
        }
    }
    val weather by produceState<WeatherClient.Today?>(null, lat, lon) {
        if (lat == null || lon == null) return@produceState
        while (true) {
            withContext(Dispatchers.IO) { runCatching { WeatherClient.today(lat, lon) }.getOrNull() }?.let { value = it }
            delay(if (value == null) 2 * 60_000L else 30 * 60_000L)
        }
    }

    val hour = remember(minute) { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val night = dimAtNight && (hour >= 23 || hour < 7)
    // Yanma (burn-in) olmasın: içerik her dakika birkaç dp kayar.
    val shifts = listOf(0 to 0, 6 to 4, -5 to 8, 4 to -6, -7 to -3, 2 to 7)
    val (sx, sy) = shifts[(minute % shifts.size).toInt()]

    var menu by remember { mutableStateOf(false) }
    LaunchedEffect(menu) { if (menu) { delay(6000); menu = false } }

    var grid by remember { mutableStateOf(StandbyGrid.parse(store.standbyGrid)) }
    var editing by remember { mutableStateOf(false) }
    var adding by remember { mutableStateOf(false) }
    // Son yerleşim ("Ekle" mevcutları ekrandaki yerlerinde sabitler).
    val lastPlaced = remember { object { var value: List<StandbyGrid.Placed> = emptyList() } }
    val available = remember(tiles, sensors, energyIds, rules.isNotEmpty()) {
        buildList {
            add(StandbyGrid.CLOCK); add(StandbyGrid.WEATHER); add(StandbyGrid.STATUS)
            for (d in tiles) for (ch in channelsOf(d)) add(StandbyGrid.device(d.id, ch))
            for (d in sensors) add(StandbyGrid.sensor(d.id))
            if (energyIds.isNotEmpty()) add(StandbyGrid.POWER)
            add(StandbyGrid.RECENT); add(StandbyGrid.UPCOMING)
            if (rules.isNotEmpty()) add(StandbyGrid.RULES)
        }
    }
    val shown = remember(grid, available) { StandbyGrid.visible(grid, available) }
    fun save(items: List<StandbyGrid.Item>, removed: Set<String> = grid.removed) {
        grid = StandbyGrid.commit(grid, items, removed)
        store.standbyGrid = StandbyGrid.format(grid)
    }

    // Zemindeki ışık durumu söyler: turkuaz yolunda, amber dikkat, kırmızı alarm/sorun, gri izleme kapalı.
    val conn = status.first
    val offlineNames = snapshots.filter { it.id in monitored && !it.online }.map { it.name }
    val accent by animateColorAsState(
        when {
            alarmLines.isNotEmpty() || conn in setOf(ConnState.NO_INTERNET, ConnState.AUTH_ERROR, ConnState.ERROR) -> Red
            conn == ConnState.STOPPED -> Idle
            offlineNames.isNotEmpty() || conn != ConnState.LIVE -> Amber
            else -> Teal
        },
        tween(900),
        label = "accent",
    )

    Box(
        Modifier.fillMaxSize().background(Bg)
            .clickable(remember { MutableInteractionSource() }, indication = null) { if (editing) adding = false else menu = !menu }
            .testTag("standby"),
    ) {
        Box(Modifier.fillMaxSize().alpha(if (night) 0.45f else 1f).aurora(accent))
        BoxWithConstraints(
            Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 24.dp)
                .then(if (editing) Modifier else Modifier.offset(sx.dp, sy.dp).alpha(if (night) 0.55f else 1f)),
        ) {
            val cols = maxOf(8, ((maxWidth + Gap) / (CellTarget + Gap)).toInt())
            val cell = (maxWidth - Gap * (cols - 1)) / cols
            val placed = remember(shown, cols) { StandbyGrid.pack(shown, cols) }
            lastPlaced.value = placed
            CompositionLocalProvider(LocalDeviceUi provides deviceUi, LocalStrings provides strings) {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    if (editing) Spacer(Modifier.height(64.dp))
                    WidgetGrid(
                        placed, cols, cell, editing, strings,
                        onStartEdit = { menu = false; editing = true },
                        onPlace = { id, x, y, w, h -> StandbyGrid.place(placed, id, x, y, w, h, cols)?.let { save(it) } },
                        onRemove = { id -> save(StandbyGrid.freeze(placed).filter { it.id != id }, grid.removed + id) },
                    ) { id, w, h ->
                        when (id) {
                            StandbyGrid.CLOCK -> ClockWidget(now, strings, w, h)
                            StandbyGrid.WEATHER -> WeatherWidget(weather, lat != null, strings, w, h)
                            StandbyGrid.STATUS -> StatusWidget(alarmLines, conn, offlineNames, monitored.size, away, accent, strings, h)
                            StandbyGrid.POWER -> PowerWidget(livePower, power, strings, w, h)
                            StandbyGrid.RECENT -> Recent(recent.take(rowsFor(h)), strings, eventStrings, Modifier.fillMaxSize())
                            StandbyGrid.UPCOMING -> Upcoming(upcoming.take(rowsFor(h)), now, strings, Modifier.fillMaxSize())
                            StandbyGrid.RULES -> Rules(rules, ruleDevices, ruleScenes, ruleCycles, rowsFor(h), strings, Modifier.fillMaxSize())
                            else -> {
                                StandbyGrid.parseDevice(id)?.let { (did, ch) ->
                                    tiles.firstOrNull { it.id == did }?.let { d -> DeviceWidget(d, ch, accent, w, h, editing, strings) }
                                }
                                StandbyGrid.parseSensor(id)?.let { sid -> sensors.firstOrNull { it.id == sid }?.let { SensorWidget(it, strings, h) } }
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
        controlError?.let { msg ->
            Text(
                msg, color = Ink, fontSize = 15.sp,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp)
                    .clip(RoundedCornerShape(20.dp)).background(Color(0xFF5A1F1D)).padding(horizontal = 18.dp, vertical = 8.dp),
            )
        }
        if (editing) {
            val addable = available.filter { it in grid.removed }
            Row(
                Modifier.align(Alignment.TopCenter).padding(horizontal = 16.dp, vertical = 10.dp).widthIn(max = 820.dp).fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp)).background(Sheet)
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp)).padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    strings["board_edit_hint"], color = Dim, fontSize = 13.sp, maxLines = 2,
                    modifier = Modifier.padding(horizontal = 10.dp).weight(1f),
                )
                if (addable.isNotEmpty()) androidx.compose.material3.TextButton(onClick = { adding = !adding }) {
                    Icon(Icons.Rounded.Add, null, tint = Teal, modifier = Modifier.size(18.dp))
                    Text(strings["board_add"], color = Teal)
                }
                androidx.compose.material3.TextButton(onClick = { grid = StandbyGrid.State(emptyList(), emptySet()); store.standbyGrid = null; adding = false }) {
                    Text(strings["board_reset"], color = Ink)
                }
                androidx.compose.material3.TextButton(onClick = { editing = false; adding = false }, modifier = Modifier.testTag("standby-edit-done")) {
                    Text(strings["action_done"], color = Teal)
                }
            }
            AnimatedVisibility(adding && addable.isNotEmpty(), Modifier.align(Alignment.TopCenter).padding(top = 76.dp), enter = fadeIn(), exit = fadeOut()) {
                Column(
                    Modifier.widthIn(max = 460.dp).heightIn(max = 480.dp).clip(RoundedCornerShape(22.dp)).background(Sheet)
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(22.dp))
                        .verticalScroll(rememberScrollState()).padding(8.dp),
                ) {
                    for (id in addable) {
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                                .clickable { save(lastPlaced.value.let { StandbyGrid.freeze(it) } + StandbyGrid.item(id), grid.removed - id); if (addable.size == 1) adding = false }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(Icons.Rounded.Add, strings["board_show"], tint = Teal, modifier = Modifier.size(20.dp))
                            Text(widgetName(id, ruleDevices, strings), color = Ink, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
        AnimatedVisibility(menu && !editing, Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp), enter = fadeIn(), exit = fadeOut()) {
            Row(
                Modifier.clip(RoundedCornerShape(28.dp)).background(Sheet).border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(28.dp)).padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                androidx.compose.material3.TextButton(onClick = onOpenApp, modifier = Modifier.testTag("standby-open-app")) {
                    Text(strings["board_open_app"], color = Teal)
                }
                androidx.compose.material3.TextButton(onClick = { menu = false; editing = true }, modifier = Modifier.testTag("standby-edit")) {
                    Text(strings["board_edit"], color = Ink)
                }
                androidx.compose.material3.TextButton(onClick = onClose, modifier = Modifier.testTag("standby-close")) {
                    Text(strings["board_close"] + "  (Esc)", color = Ink)
                }
            }
        }
    }
}

/** Liste widget'ında yüksekliğe sığan satır sayısı. */
private fun rowsFor(h: Dp): Int = maxOf(1, ((h - 46.dp) / 52.dp).toInt())

/**
 * Widget ızgarası. Düzenlemede widget fareyle doğrudan sürüklenir (kapalıyken basılı tutmak düzenlemeyi açar); diğer widget'lar yerinden oynamaz. Bırakılacak
 * yer çerçeveyle gösterilir (boşsa yeşil, doluysa kırmızı); dolu yere bırakılan widget eski yerine döner.
 * Düzenlemede sol üstte × (kaldır), sağ altta boyut tutamağı (yalnız boş alana doğru büyür).
 */
@Composable
private fun WidgetGrid(
    placed: List<StandbyGrid.Placed>,
    cols: Int,
    cell: Dp,
    editing: Boolean,
    strings: Strings,
    onStartEdit: () -> Unit,
    onPlace: (String, Int, Int, Int, Int) -> Unit,
    onRemove: (String) -> Unit,
    body: @Composable (String, Dp, Dp) -> Unit,
) {
    val density = LocalDensity.current
    val step = with(density) { (cell + Gap).toPx() }
    val gapPx = with(density) { Gap.toPx() }
    val latest = rememberUpdatedState(placed)
    val place = rememberUpdatedState(onPlace)
    val startEdit = rememberUpdatedState(onStartEdit)
    var dragId by remember { mutableStateOf<String?>(null) }
    var dragPos by remember { mutableStateOf(Offset.Zero) }
    // Boyutlanan widget'ın canlı (henüz hücreye oturmamış) boyutu.
    var sizing by remember { mutableStateOf<Triple<String, Float, Float>?>(null) }
    // Bırakılacak yer: (id, x, y, w, h).
    fun target(id: String): IntArray? {
        val me = latest.value.firstOrNull { it.item.id == id } ?: return null
        val s = sizing?.takeIf { it.first == id }
        return if (s != null) {
            val (w, h) = StandbyGrid.clampSize(id, ((s.second + gapPx) / step).roundToInt(), ((s.third + gapPx) / step).roundToInt(), cols)
            intArrayOf(me.x, me.y, w, h)
        } else if (dragId == id) {
            intArrayOf(
                (dragPos.x / step).roundToInt().coerceIn(0, maxOf(0, cols - me.w)),
                (dragPos.y / step).roundToInt().coerceAtLeast(0), me.w, me.h,
            )
        } else null
    }
    val active = dragId ?: sizing?.first
    val ghost = active?.let { id -> target(id)?.let { id to it } }
    val rows = maxOf(
        placed.maxOfOrNull { it.y + it.h } ?: 0,
        ghost?.second?.let { it[1] + it[3] } ?: 0,
    ) + if (editing) 3 else 0
    fun span(n: Int) = cell * n + Gap * (n - 1)
    Box(Modifier.fillMaxWidth().height(if (rows == 0) 0.dp else span(rows)).testTag("standby-controls")) {
        ghost?.let { (id, t) ->
            val ok = StandbyGrid.canPlace(latest.value, id, t[0], t[1], t[2], t[3], cols)
            Box(
                Modifier.offset { IntOffset((t[0] * step).roundToInt(), (t[1] * step).roundToInt()) }.size(span(t[2]), span(t[3]))
                    .clip(RoundedCornerShape(22.dp)).background((if (ok) Teal else Red).copy(alpha = 0.12f))
                    .border(2.dp, (if (ok) Teal else Red).copy(alpha = 0.8f), RoundedCornerShape(22.dp)),
            )
        }
        for (p in placed) key(p.item.id) {
            val id = p.item.id
            val dragging = dragId == id
            val live = sizing?.takeIf { it.first == id }
            val w = if (live != null) with(density) { live.second.toDp() } else span(p.w)
            val h = if (live != null) with(density) { live.third.toDp() } else span(p.h)
            Box(
                Modifier
                    .offset { if (dragging) IntOffset(dragPos.x.roundToInt(), dragPos.y.roundToInt()) else IntOffset((p.x * step).roundToInt(), (p.y * step).roundToInt()) }
                    .size(w, h)
                    .zIndex(if (dragging || live != null) 1f else 0f)
                    .graphicsLayer {
                        val s = if (dragging) 1.04f else 1f
                        scaleX = s; scaleY = s
                        alpha = if (dragging) 0.85f else 1f
                    }
                    .pointerInput(id, editing) {
                        val start: (Offset) -> Unit = {
                            startEdit.value()
                            val me = latest.value.firstOrNull { it.item.id == id }
                            if (me != null) { dragPos = Offset(me.x * step, me.y * step); dragId = id }
                        }
                        val drag: (androidx.compose.ui.input.pointer.PointerInputChange, Offset) -> Unit = { change, amount ->
                            change.consume()
                            if (dragId == id) dragPos += amount
                        }
                        val end: () -> Unit = {
                            target(id)?.let { t -> place.value(id, t[0], t[1], t[2], t[3]) }
                            dragId = null
                        }
                        if (editing) detectDragGestures(onDragStart = start, onDrag = drag, onDragEnd = end, onDragCancel = { dragId = null })
                        else detectDragGesturesAfterLongPress(onDragStart = start, onDrag = drag, onDragEnd = end, onDragCancel = { dragId = null })
                    }
                    .testTag("widget-$id"),
            ) {
                body(id, w, h)
                if (editing) {
                    Box(Modifier.matchParentSize().border(1.5.dp, Teal.copy(alpha = if (dragging) 0.9f else 0.5f), RoundedCornerShape(22.dp)))
                    EditKey(Icons.Rounded.Close, strings["board_hide"], Modifier.align(Alignment.TopStart)) { onRemove(id) }
                    // Boyut tutamağı: sürükledikçe canlı büyür, bırakınca en yakın hücre sayısına oturur (alan boşsa).
                    EditKey(
                        Icons.Rounded.OpenInFull, strings["board_edit"],
                        Modifier.align(Alignment.BottomEnd).testTag("resize-$id").pointerInput(id) {
                            detectDragGestures(
                                onDragStart = {
                                    val me = latest.value.firstOrNull { it.item.id == id }
                                    if (me != null) sizing = Triple(id, me.w * step - gapPx, me.h * step - gapPx)
                                },
                                onDrag = { change, amount ->
                                    change.consume()
                                    sizing = sizing?.let { (i, sw, sh) -> Triple(i, (sw + amount.x).coerceAtLeast(step), (sh + amount.y).coerceAtLeast(step)) }
                                },
                                onDragEnd = {
                                    target(id)?.let { t -> place.value(id, t[0], t[1], t[2], t[3]) }
                                    sizing = null
                                },
                                onDragCancel = { sizing = null },
                            )
                        },
                    ) {}
                }
            }
        }
    }
}

@Composable
private fun EditKey(icon: ImageVector, label: String?, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.padding(4.dp).size(28.dp).clip(CircleShape).background(Color(0xE6202A2E))
            .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, label, tint = Ink, modifier = Modifier.size(16.dp)) }
}

private fun widgetName(id: String, devices: List<DeviceSnapshot>, strings: Strings): String = when (id) {
    StandbyGrid.CLOCK -> strings["board_sec_clock"]
    StandbyGrid.WEATHER -> strings["board_sec_weather"]
    StandbyGrid.STATUS -> strings["board_sec_status"]
    StandbyGrid.POWER -> strings["board_sec_power"]
    StandbyGrid.RECENT -> strings["board_recent"]
    StandbyGrid.UPCOMING -> strings["board_upcoming"]
    StandbyGrid.RULES -> strings["automations_rules_title"]
    else -> StandbyGrid.parseDevice(id)?.let { (did, ch) ->
        devices.firstOrNull { it.id == did }?.let { if (it.isMultiChannel) "${it.name} · ${ch + 1}" else it.name }
    } ?: StandbyGrid.parseSensor(id)?.let { sid -> devices.firstOrNull { it.id == sid }?.name } ?: id
}

/** dp cinsinden boyutu yazı boyutuna çevirir. */
@Composable
private fun dpSp(v: Float): TextUnit = with(LocalDensity.current) { v.dp.toSp() }

/**
 * Cihaz widget'ı (telefon TV panosundaki kartın eşi): açıkken altından vurgu renginde ışık yükselir, simge dolu daire
 * olur; fare üstüne gelince kenar aydınlanır. Küçükken simge + ad, yatayda simge yanında durum, büyükken TV kartı.
 */
@Composable
private fun DeviceWidget(d: DeviceSnapshot, ch: Int, accent: Color, w: Dp, h: Dp, editing: Boolean, strings: Strings) {
    val control = rememberSwitchControl(d, ch, d.switches[ch] == true)
    val online = d.online
    val on = control.shown && online
    val lit = if (online) accent else Muted
    val kind = remember(d.name, d.isMultiChannel, d.hasEnergy, d.cloud, d.sensorOnly) { DeviceKinds.of(d) }
    val name = if (d.isMultiChannel) "${d.name} · ${ch + 1}" else d.name
    val state = when {
        !online -> strings["label_offline_title"]
        control.busy -> "…"
        control.shown -> strings["state_on_title"]
        else -> strings["state_off_title"]
    }
    val stateColor = when { !online -> Red.copy(alpha = 0.85f); on -> lit; else -> Ink }
    val hover = remember { MutableInteractionSource() }
    val hovered by hover.collectIsHoveredAsState()
    val shape = RoundedCornerShape(22.dp)
    val small = minOf(w, h)
    val circle = (small * 0.42f).coerceIn(26.dp, 64.dp)
    val iconCircle: @Composable () -> Unit = {
        Box(
            Modifier.size(circle)
                .drawBehind { if (on) drawCircle(lit.copy(alpha = 0.22f), radius = size.minDimension * 0.66f) }
                .background(if (on) lit else Color.White.copy(alpha = 0.07f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(if (online) kind.icon() else Icons.Rounded.CloudOff, null, Modifier.size(circle * 0.55f), tint = if (on) Bg else if (online) Ink else Muted)
        }
    }
    Box(
        Modifier.fillMaxSize().clip(shape)
            .drawBehind {
                if (on) drawRect(Brush.verticalGradient(listOf(Color.Transparent, lit.copy(alpha = 0.24f)), startY = size.height * 0.3f))
            }
            .background(if (hovered && !editing) Color.White.copy(alpha = 0.10f) else Faint, shape)
            .border(
                if (hovered && !editing) 1.5.dp else 1.dp,
                when { hovered && !editing -> Color.White.copy(alpha = 0.6f); on -> lit.copy(alpha = 0.45f); else -> Edge },
                shape,
            )
            .hoverable(hover)
            .clickable(enabled = online && !control.busy && !editing) { control.toggle(!control.shown) }
            .padding(if (small < 90.dp) 8.dp else 14.dp),
    ) {
        when {
            h >= 140.dp && w >= 120.dp -> Column(Modifier.fillMaxSize()) {
                iconCircle()
                Spacer(Modifier.weight(1f))
                Text(state, color = stateColor, fontSize = dpSp((h.value * 0.14f).coerceIn(18f, 40f)), fontWeight = FontWeight.Light, maxLines = 1)
                Text(name, color = Ink.copy(alpha = 0.92f), fontSize = dpSp((h.value * 0.085f).coerceIn(13f, 22f)), maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            w >= h * 1.4f && w >= 120.dp -> Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                iconCircle()
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(state, color = stateColor, fontSize = dpSp((h.value * 0.22f).coerceIn(14f, 30f)), fontWeight = FontWeight.Light, maxLines = 1)
                    Text(name, color = Ink.copy(alpha = 0.92f), fontSize = dpSp((h.value * 0.15f).coerceIn(11f, 20f)), maxLines = if (h >= 110.dp) 2 else 1, overflow = TextOverflow.Ellipsis)
                }
            }
            else -> Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceEvenly) {
                iconCircle()
                Text(
                    name, color = if (on) lit else Ink.copy(alpha = 0.92f), fontSize = dpSp((small.value * 0.13f).coerceIn(10f, 16f)),
                    maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** Saat widget'ı: zemini yok (ana ekran saati gibi); tarih yalnız yükseklik yetince. */
@Composable
private fun ClockWidget(now: Long, strings: Strings, w: Dp, h: Dp) {
    val locale = strings.locale
    val time = remember(now / 1000, locale) { DateFormat.getTimeInstance(DateFormat.SHORT, locale).format(Date(now)) }
    val date = remember(now / 60_000, locale) {
        java.text.SimpleDateFormat(if (locale.language == "tr") "d MMMM EEEE" else "EEEE, d MMMM", locale).format(Date(now))
    }
    val showDate = h >= 110.dp
    val size = minOf(h.value * (if (showDate) 0.6f else 0.78f), w.value * (if (time.length <= 5) 0.27f else 0.2f))
    Column(Modifier.fillMaxSize().padding(horizontal = 4.dp), verticalArrangement = Arrangement.Center) {
        Text(time, color = Ink, fontSize = dpSp(size), fontWeight = FontWeight.Thin, letterSpacing = (-2).sp, lineHeight = dpSp(size * 1.04f), maxLines = 1, softWrap = false)
        if (showDate) Text(
            date.uppercase(locale), color = Dim, fontSize = dpSp((h.value * 0.07f).coerceIn(11f, 22f)), fontWeight = FontWeight.Medium,
            letterSpacing = 3.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 6.dp),
        )
    }
}

@Composable
private fun WeatherWidget(w: WeatherClient.Today?, hasLocation: Boolean, strings: Strings, width: Dp, h: Dp) {
    Row(Modifier.fillMaxSize().padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        if (w == null) {
            if (!hasLocation) Text(strings["desktop_board_no_location"], color = Dim, fontSize = 14.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
            return@Row
        }
        val (icon, label) = weatherLook(w.code, w.isDay)
        Icon(icon, null, tint = if (w.code == 0 && w.isDay) Amber else Ink, modifier = Modifier.size((h * 0.45f).coerceIn(24.dp, 110.dp)))
        Text("${w.temp.roundToInt()}°", color = Ink, fontSize = dpSp((h.value * 0.5f).coerceIn(20f, 110f)), fontWeight = FontWeight.Thin, maxLines = 1)
        if (width >= 170.dp) Column {
            Text(strings[label], color = Ink, fontSize = dpSp((h.value * 0.18f).coerceIn(13f, 28f)), maxLines = 1, overflow = TextOverflow.Ellipsis)
            val parts = listOfNotNull(
                if (w.min != null && w.max != null) "↓${w.min.roundToInt()}°  ↑${w.max.roundToInt()}°" else null,
                w.rainChance?.takeIf { it > 0 }?.let { strings.format("board_rain", it) },
            )
            if (parts.isNotEmpty()) Text(parts.joinToString("  ·  "), color = Dim, fontSize = dpSp((h.value * 0.14f).coerceIn(11f, 22f)), maxLines = 2)
        }
    }
}

/** WMO hava kodu → simge + kısa açıklama anahtarı. */
private fun weatherLook(code: Int, day: Boolean): Pair<ImageVector, String> = when (code) {
    0 -> (if (day) Icons.Rounded.WbSunny else Icons.Rounded.NightsStay) to "weather_clear"
    1, 2 -> (if (day) Icons.Rounded.WbCloudy else Icons.Rounded.NightsStay) to "weather_partly"
    3 -> Icons.Rounded.Cloud to "weather_cloudy"
    45, 48 -> Icons.Rounded.Cloud to "weather_fog"
    51, 53, 55, 56, 57 -> Icons.Rounded.Grain to "weather_drizzle"
    61, 63, 65, 66, 67 -> Icons.Rounded.Umbrella to "weather_rain"
    71, 73, 75, 77, 85, 86 -> Icons.Rounded.AcUnit to "weather_snow"
    80, 81, 82 -> Icons.Rounded.Umbrella to "weather_showers"
    95, 96, 99 -> Icons.Rounded.Thunderstorm to "weather_storm"
    else -> Icons.Rounded.Cloud to "weather_cloudy"
}

/** Kritik durum: alarm > bağlantı sorunu > çevrimdışı cihaz > her şey yolunda; altında evde/dışarıda. */
@Composable
private fun StatusWidget(alarm: List<String>, conn: ConnState, offline: List<String>, watched: Int, away: Boolean, accent: Color, strings: Strings, h: Dp) {
    val (color, title, detail) = when {
        alarm.isNotEmpty() -> Triple(Red, strings["board_alarm"], alarm.last())
        conn == ConnState.NO_INTERNET -> Triple(Amber, strings["board_no_internet"], null)
        conn == ConnState.AUTH_ERROR -> Triple(Amber, strings["board_auth"], null)
        conn == ConnState.ERROR -> Triple(Amber, strings["board_conn_error"], null)
        conn == ConnState.STOPPED -> Triple(Amber, strings["board_stopped"], null)
        offline.isNotEmpty() -> Triple(Amber, strings.format("board_offline", offline.size), offline.joinToString(", "))
        else -> Triple(Teal, strings["board_all_ok"], strings.format("board_watching", watched))
    }
    val ok = color == Teal
    Row(
        Modifier.fillMaxSize().glass(22, tint = if (ok) null else color).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(Modifier.size(26.dp), contentAlignment = Alignment.Center) { PulseDot(if (ok) accent else color, pulsing = ok || alarm.isNotEmpty()) }
        Column(Modifier.weight(1f)) {
            Text(title, color = Ink, fontSize = dpSp((h.value * 0.2f).coerceIn(15f, 30f)), fontWeight = FontWeight.Light, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val sub = listOfNotNull(detail, strings[if (away) "board_away" else "board_home"]).joinToString(" · ")
            Text(sub, color = Dim, fontSize = dpSp((h.value * 0.13f).coerceIn(12f, 20f)), maxLines = maxOf(1, ((h - 50.dp) / 18.dp).toInt()), overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun PowerWidget(watts: Double, p: PowerView?, strings: Strings, w: Dp, h: Dp) {
    Box(Modifier.fillMaxSize().glass(22).padding(12.dp), contentAlignment = Alignment.Center) {
        val iw = w - 24.dp
        val ih = h - 24.dp
        val withChart = minOf(iw, (ih - 76.dp) * 2)
        val chart = p != null && withChart >= 120.dp
        val width = if (chart) withChart else minOf(iw, (ih - 4.dp) * 2)
        PowerGauge(watts, p?.series.orEmpty(), p?.todayKwh ?: 0.0, width, chart, strings)
    }
}

/** Yarım daire güç göstergesi + (isteğe bağlı) son 6 saatin eğrisi. */
@Composable
private fun PowerGauge(watts: Double, series: List<Double>, todayKwh: Double, width: Dp, chart: Boolean, strings: Strings) {
    val peak = maxOf(series.maxOrNull() ?: 0.0, watts)
    val max = StandbyBoard.gaugeMax(peak)
    val frac = (watts / max).toFloat().coerceIn(0f, 1f)
    val arcColor = when {
        frac < 0.5f -> Teal
        frac < 0.8f -> Amber
        else -> Red
    }
    Column(Modifier.width(width), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth().aspectRatio(2f), contentAlignment = Alignment.BottomCenter) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = size.width * 0.075f
                val d = size.width - stroke
                val topLeft = Offset(stroke / 2, stroke / 2)
                val arc = Size(d, d)
                drawArc(Faint, 180f, 180f, false, topLeft, arc, style = Stroke(stroke, cap = StrokeCap.Round))
                if (frac > 0f) drawArc(arcColor, 180f, 180f * frac, false, topLeft, arc, style = Stroke(stroke, cap = StrokeCap.Round))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(boardWatts(watts), color = Ink, fontSize = dpSp((width.value * 0.15f).coerceIn(16f, 56f)), fontWeight = FontWeight.Thin, maxLines = 1)
                Text(strings.format("board_power_now", boardWatts(max)), color = Dim, fontSize = dpSp((width.value * 0.05f).coerceIn(10f, 15f)), maxLines = 1)
            }
        }
        if (chart && series.size >= 2 && series.any { it > 0 }) {
            Spacer(Modifier.height(12.dp))
            Canvas(Modifier.fillMaxWidth().height(44.dp)) {
                val top = series.max().coerceAtLeast(1.0)
                val stepX = size.width / (series.size - 1)
                val line = Path()
                series.forEachIndexed { i, v ->
                    val x = i * stepX
                    val y = size.height - (v / top).toFloat() * size.height
                    if (i == 0) line.moveTo(x, y) else line.lineTo(x, y)
                }
                val fill = Path().apply { addPath(line); lineTo(size.width, size.height); lineTo(0f, size.height); close() }
                drawPath(fill, Brush.verticalGradient(listOf(Teal.copy(alpha = 0.35f), Color.Transparent)))
                drawPath(line, Teal, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(strings["board_last_6h"], color = Dim, fontSize = 13.sp, maxLines = 1)
                Text(strings.format("board_today_kwh", String.format(strings.locale, "%.1f", todayKwh)), color = Dim, fontSize = 13.sp, maxLines = 1)
            }
        }
    }
}

private fun boardWatts(w: Double): String =
    if (w >= 10_000) String.format(Locale.getDefault(), "%.1f kW", w / 1000) else "${w.roundToInt()} W"

/** Oda sensörü widget'ı: büyük sıcaklık/nem; kapı açık ya da su kaçağı kırmızı, hareket turuncu; pil %20 altındaysa. */
@Composable
private fun SensorWidget(d: DeviceSnapshot, strings: Strings, h: Dp) {
    val s = d.sensors
    val alert = d.online && (d.sensorActive(SensorKind.WATER) == true || d.sensorActive(SensorKind.DOOR) == true)
    val busy = d.online && d.sensorActive(SensorKind.MOTION) == true
    val accent = when { alert -> Color(0xFFFF6B6B); busy -> Amber; else -> Teal }
    val big = dpSp((h.value * 0.32f).coerceIn(18f, 60f))
    val mid = dpSp((h.value * 0.18f).coerceIn(13f, 30f))
    Column(
        Modifier.fillMaxSize().glass(20, tint = if (alert) accent else null)
            .alpha(if (d.online) 1f else 0.5f).padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val icon = s.keys.firstOrNull { it != SensorKind.BATTERY }?.icon() ?: Icons.Rounded.Sensors
            Icon(if (d.online) icon else Icons.Rounded.CloudOff, null, tint = if (d.online) accent else Dim, modifier = Modifier.size(16.dp))
            Text(d.name, color = Dim, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            s[SensorKind.TEMPERATURE]?.let { Text(sensorNumber(it) + "°", color = Ink, fontSize = big, fontWeight = FontWeight.Light, maxLines = 1) }
            s[SensorKind.HUMIDITY]?.let { Text(Math.round(it).toString() + " %", color = Dim, fontSize = mid, maxLines = 1) }
            for (k in listOf(SensorKind.DOOR, SensorKind.MOTION, SensorKind.WATER)) {
                val v = s[k] ?: continue
                Text(strings[k.stateKey(v >= 0.5)], color = if (v >= 0.5) accent else Ink, fontSize = mid, fontWeight = FontWeight.Medium, maxLines = 1)
            }
            s[SensorKind.BATTERY]?.takeIf { it < 20 }?.let { Text("🔋 " + Math.round(it) + " %", color = Amber, fontSize = 14.sp, maxLines = 1) }
        }
    }
}

private fun sensorNumber(v: Double): String = if (v == Math.floor(v)) v.toLong().toString() else v.toString()

/** Açık kurallar: ad ve "Eğer → O zaman" özeti; uyarılar zil, otomasyonlar şimşek simgesiyle. */
@Composable
private fun Rules(
    list: List<com.macerce.switchguard.core.Automation>, devices: List<DeviceSnapshot>, scenes: List<com.macerce.switchguard.core.Scene>,
    cycles: List<com.macerce.switchguard.core.Cycle>, max: Int, strings: Strings, modifier: Modifier,
) {
    Column(modifier.glass(22).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        BoardTitle(strings["automations_rules_title"] + " · " + list.size, strings)
        for (a in list.take(max)) {
            val (cond, act) = com.macerce.switchguard.desktop.ui.automations.describe(a, devices, strings, scenes, cycles)
            val kind = a.action.kind
            val alert = kind == com.macerce.switchguard.core.ActionKind.ALARM || kind == com.macerce.switchguard.core.ActionKind.NOTIFY
            BoardRow(
                if (alert) Icons.Rounded.NotificationsActive else Icons.Rounded.Bolt,
                if (kind == com.macerce.switchguard.core.ActionKind.ALARM) Color(0xFFFF6B6B) else if (alert) Amber else Teal,
                a.name.ifBlank { cond }, "", sub = "$cond → $act",
            )
        }
    }
}

@Composable
private fun BoardTitle(text: String, strings: Strings) {
    Text(text.uppercase(strings.locale), color = Dim, fontSize = 13.sp, letterSpacing = 1.5.sp, maxLines = 1)
}

@Composable
private fun Recent(list: List<LoggedEvent>, strings: Strings, eventStrings: com.macerce.switchguard.core.EventStrings, modifier: Modifier) {
    Column(modifier.glass(22).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BoardTitle(strings["board_recent"], strings)
        if (list.isEmpty()) Text(strings["board_no_recent"], color = Dim, fontSize = 16.sp)
        for (e in list) {
            BoardRow(
                icon = e.icon(), tint = if (e.action == AlertAction.ALARM.name) Red else Dim,
                text = e.text(eventStrings), time = eventTime(e.time, strings),
            )
        }
    }
}

/** Bugünse yalnız saat, dünse "dün 23:42", daha eskiyse kısa tarih. */
private fun eventTime(t: Long, strings: Strings): String {
    val clock = DateFormat.getTimeInstance(DateFormat.SHORT, strings.locale).format(Date(t))
    val day = { ms: Long -> Calendar.getInstance().apply { timeInMillis = ms }.let { it.get(Calendar.YEAR) * 1000 + it.get(Calendar.DAY_OF_YEAR) } }
    val today = day(System.currentTimeMillis())
    val yesterday = day(System.currentTimeMillis() - 86_400_000L)
    return when (day(t)) {
        today -> clock
        yesterday -> strings["yesterday"].lowercase(strings.locale) + " " + clock
        else -> DateFormat.getDateInstance(DateFormat.SHORT, strings.locale).format(Date(t))
    }
}

@Composable
private fun Upcoming(list: List<StandbyBoard.Upcoming>, now: Long, strings: Strings, modifier: Modifier) {
    val durations = remember(strings) { strings.durations() }
    Column(modifier.glass(22).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BoardTitle(strings["board_upcoming"], strings)
        if (list.isEmpty()) Text(strings["board_no_upcoming"], color = Dim, fontSize = 16.sp)
        for (u in list) {
            val (icon, kind) = when (u.kind) {
                UpcomingKind.SUNRISE -> Icons.Rounded.WbTwilight to "board_kind_sunrise"
                UpcomingKind.SUNSET -> Icons.Rounded.WbTwilight to "board_kind_sunset"
                UpcomingKind.CYCLE_START -> Icons.Rounded.Autorenew to "board_kind_cycle_start"
                UpcomingKind.CYCLE_STEP -> Icons.Rounded.Autorenew to "board_kind_cycle_step"
                UpcomingKind.CYCLE_END -> Icons.Rounded.Autorenew to "board_kind_cycle_end"
                UpcomingKind.TEMPLATE -> Icons.Rounded.Forum to "board_kind_template"
            }
            // Dakikaya yuvarlanır: saniye saniye değişen yazı gözü yormasın.
            val left = ((u.time - now + 59_999) / 60_000) * 60_000
            BoardRow(
                icon = icon, tint = Teal,
                text = "${u.name} · ${strings[kind]}",
                time = DateFormat.getTimeInstance(DateFormat.SHORT, strings.locale).format(Date(u.time)),
                sub = strings.format("board_in", DurationText.format(left, durations)),
            )
        }
    }
}

@Composable
private fun BoardRow(icon: ImageVector, tint: Color, text: String, time: String, sub: String? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(tint.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(19.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(text, color = Ink, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (sub != null) Text(sub, color = Dim, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(time, color = Dim, fontSize = 15.sp, maxLines = 1)
    }
}
