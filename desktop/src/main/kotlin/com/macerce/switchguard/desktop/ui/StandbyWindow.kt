package com.macerce.switchguard.desktop.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
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
                // Ekran koruyucu modunda Windows ekranı zaten açık tutar; elle açılınca biz tutarız.
                if (!saver) DisplayWake.keepOn()
                delay(50_000)
            }
        }
        // Ekran koruyucu: .scr bu dosya durdukça bekler (30 sn'de bir tazelenir; uygulama çökerse eskir ve .scr biter).
        if (saver) {
            val marker = remember { graph.dataDir.resolve(com.macerce.switchguard.desktop.platform.ScreenSaver.MARKER) }
            LaunchedEffect(Unit) {
                while (true) {
                    runCatching { java.nio.file.Files.writeString(marker, System.currentTimeMillis().toString()) }
                    delay(30_000)
                }
            }
            androidx.compose.runtime.DisposableEffect(Unit) {
                onDispose { runCatching { java.nio.file.Files.deleteIfExists(marker) } }
            }
        }
        val focus = remember { FocusRequester() }
        LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
        Box(
            Modifier.fillMaxSize().focusRequester(focus).focusable().onPreviewKeyEvent {
                // Ekran koruyucuda her tuş kapatır; elle açılınca yalnız Esc.
                if (it.type == KeyEventType.KeyDown && (saver || it.key == Key.Escape)) { close(); true } else false
            }.then(if (saver) Modifier.closeOnMouse(close) else Modifier),
        ) {
            StandbyBoardScreen(
                graph, strings,
                onOpenApp = {
                    close()
                    graph.appState.showMainWindow()
                },
                onClose = close,
            )
        }
    }
}

// Ekran hep koyu: OLED'de siyah piksel yanmaz, gece gözü almaz (telefondakiyle aynı renkler).
private val Bg = Color(0xFF000000)
private val Ink = Color(0xFFE8EEED)
private val Dim = Color(0xFF8A9895)
private val Faint = Color(0xFF26302E)
private val Teal = Color(0xFF26A69A)
private val Amber = Color(0xFFFFB300)
private val Red = Color(0xFFEF5350)

/** Ekran koruyucu: fare biraz oynayınca ya da tıklanınca kapanır (pencere açılırkenki tek olay sayılmaz). */
private fun Modifier.closeOnMouse(close: () -> Unit): Modifier = pointerInput(Unit) {
    var origin: Offset? = null
    awaitPointerEventScope {
        while (true) {
            val e = awaitPointerEvent(PointerEventPass.Initial)
            if (e.type == PointerEventType.Press) { close(); return@awaitPointerEventScope }
            val p = e.changes.firstOrNull()?.position ?: continue
            val o = origin
            if (o == null) origin = p
            else if ((p - o).getDistance() > 12.dp.toPx()) { close(); return@awaitPointerEventScope }
        }
    }
}

private data class PowerView(val series: List<Double>, val todayKwh: Double)

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
    val monitored = remember(version) { store.monitoredIds() }
    val away = remember(version, minute) { store.isAway() }
    val lat = remember(version) { store.latitude }
    val lon = remember(version) { store.longitude }
    val dimAtNight = remember(version) { store.standbyDim }
    val eventStrings = remember(strings) { strings.eventStrings() }

    val recent by produceState(emptyList<LoggedEvent>(), eventsVersion) {
        value = withContext(Dispatchers.IO) { runCatching { graph.eventLog.recent(limit = 30) }.getOrDefault(emptyList()) }
            .filter { it.kind != "MONITOR_GAP" }.take(5)
    }
    val upcoming = remember(version, minute) {
        StandbyBoard.upcoming(
            store.automations, store.cycles, store.cycleRuns, store.chatTemplates.orEmpty(),
            graph.runtime.templateRuns, lat, lon, System.currentTimeMillis(),
        ).take(4)
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

    Box(
        Modifier.fillMaxSize().background(Bg)
            .clickable(remember { MutableInteractionSource() }, indication = null) { menu = !menu }
            .testTag("standby"),
    ) {
        BoxWithConstraints(
            Modifier.fillMaxSize().padding(horizontal = 48.dp, vertical = 40.dp).offset(sx.dp, sy.dp).alpha(if (night) 0.55f else 1f),
        ) {
            val wide = maxWidth > maxHeight
            val compact = maxHeight < 520.dp
            val events = recent.take(if (compact) 3 else 5)
            val next = upcoming.take(if (compact) 2 else 4)
            val lines = if (compact) 1 else 2
            val statusPill: @Composable () -> Unit = {
                StatusPill(alarmLines, status.first, snapshots.filter { it.id in monitored && !it.online }.map { it.name }, monitored.size, away, strings)
            }
            if (wide) {
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(48.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1.1f), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        Clock(now, strings, big = !compact)
                        WeatherLine(weather, lat != null, strings)
                        statusPill()
                    }
                    power?.let { p ->
                        Box(Modifier.weight(0.8f), contentAlignment = Alignment.Center) {
                            PowerGauge(livePower, p.series, p.todayKwh, if (compact) 260.dp else 360.dp, strings)
                        }
                    }
                    Column(Modifier.weight(1.1f), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        Recent(events, lines, strings, eventStrings)
                        Upcoming(next, now, strings)
                    }
                }
            } else {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    Clock(now, strings, big = true)
                    WeatherLine(weather, lat != null, strings)
                    statusPill()
                    power?.let { p -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { PowerGauge(livePower, p.series, p.todayKwh, 320.dp, strings) } }
                    Recent(events, lines, strings, eventStrings)
                    Upcoming(next, now, strings)
                }
            }
        }
        AnimatedVisibility(menu, Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp), enter = fadeIn(), exit = fadeOut()) {
            Row(
                Modifier.clip(RoundedCornerShape(28.dp)).background(Color(0xFF1B2422)).padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                androidx.compose.material3.TextButton(onClick = onOpenApp, modifier = Modifier.testTag("standby-open-app")) {
                    Text(strings["board_open_app"], color = Teal)
                }
                androidx.compose.material3.TextButton(onClick = onClose, modifier = Modifier.testTag("standby-close")) {
                    Text(strings["board_close"] + "  (Esc)", color = Ink)
                }
            }
        }
    }
}

@Composable
private fun Clock(now: Long, strings: Strings, big: Boolean) {
    val locale = strings.locale
    val time = remember(now / 1000, locale) { DateFormat.getTimeInstance(DateFormat.SHORT, locale).format(Date(now)) }
    val date = remember(now / 60_000, locale) {
        java.text.SimpleDateFormat(if (locale.language == "tr") "d MMMM EEEE" else "EEEE, d MMMM", locale).format(Date(now))
    }
    Column {
        Text(time, color = Ink, fontSize = if (big) 120.sp else 92.sp, fontWeight = FontWeight.Light, lineHeight = if (big) 124.sp else 96.sp)
        Text(date, color = Dim, fontSize = 22.sp)
    }
}

@Composable
private fun WeatherLine(w: WeatherClient.Today?, hasLocation: Boolean, strings: Strings) {
    if (w == null) {
        if (!hasLocation) Text(strings["desktop_board_no_location"], color = Dim, fontSize = 15.sp)
        return
    }
    val (icon, label) = weatherLook(w.code, w.isDay)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Icon(icon, null, tint = if (w.code == 0 && w.isDay) Amber else Ink, modifier = Modifier.size(44.dp))
        Text("${w.temp.roundToInt()}°", color = Ink, fontSize = 44.sp, fontWeight = FontWeight.Light)
        Column {
            Text(strings[label], color = Ink, fontSize = 17.sp)
            val parts = listOfNotNull(
                if (w.min != null && w.max != null) "↓${w.min.roundToInt()}°  ↑${w.max.roundToInt()}°" else null,
                w.rainChance?.takeIf { it > 0 }?.let { strings.format("board_rain", it) },
            )
            if (parts.isNotEmpty()) Text(parts.joinToString("  ·  "), color = Dim, fontSize = 15.sp)
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

private data class Look(val color: Color, val icon: ImageVector, val title: String, val detail: String?)

/** Kritik durum: alarm > bağlantı sorunu > çevrimdışı cihaz > her şey yolunda; altında evde/dışarıda. */
@Composable
private fun StatusPill(alarm: List<String>, conn: ConnState, offline: List<String>, watched: Int, away: Boolean, strings: Strings) {
    val look = when {
        alarm.isNotEmpty() -> Look(Red, Icons.Rounded.NotificationsActive, strings["board_alarm"], alarm.last())
        conn == ConnState.NO_INTERNET -> Look(Amber, Icons.Rounded.CloudOff, strings["board_no_internet"], null)
        conn == ConnState.AUTH_ERROR -> Look(Amber, Icons.Rounded.WarningAmber, strings["board_auth"], null)
        conn == ConnState.ERROR -> Look(Amber, Icons.Rounded.WarningAmber, strings["board_conn_error"], null)
        conn == ConnState.STOPPED -> Look(Amber, Icons.Rounded.WarningAmber, strings["board_stopped"], null)
        offline.isNotEmpty() -> Look(Amber, Icons.Rounded.CloudOff, strings.format("board_offline", offline.size), offline.joinToString(", "))
        else -> Look(Teal, Icons.Rounded.CheckCircle, strings["board_all_ok"], strings.format("board_watching", watched))
    }
    Row(
        Modifier.clip(RoundedCornerShape(20.dp)).background(look.color.copy(alpha = 0.14f)).padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(look.icon, null, tint = look.color, modifier = Modifier.size(30.dp))
        Column(Modifier.widthIn(max = 520.dp)) {
            Text(look.title, color = look.color, fontSize = 19.sp, fontWeight = FontWeight.Medium)
            if (look.detail != null) Text(look.detail, color = Dim, fontSize = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(Modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(if (away) Icons.AutoMirrored.Rounded.DirectionsWalk else Icons.Rounded.Home, null, tint = Dim, modifier = Modifier.size(16.dp))
                Text(strings[if (away) "board_away" else "board_home"], color = Dim, fontSize = 15.sp)
            }
        }
    }
}

/** Yarım daire güç göstergesi + son 6 saatin eğrisi. */
@Composable
private fun PowerGauge(watts: Double, series: List<Double>, todayKwh: Double, width: Dp, strings: Strings) {
    val peak = maxOf(series.maxOrNull() ?: 0.0, watts)
    val max = StandbyBoard.gaugeMax(peak)
    val frac = (watts / max).toFloat().coerceIn(0f, 1f)
    val arcColor = when {
        frac < 0.5f -> Teal
        frac < 0.8f -> Amber
        else -> Red
    }
    Column(Modifier.widthIn(max = width).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
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
                Text(boardWatts(watts), color = Ink, fontSize = 38.sp, fontWeight = FontWeight.Light)
                Text(strings.format("board_power_now", boardWatts(max)), color = Dim, fontSize = 13.sp)
            }
        }
        if (series.size >= 2 && series.any { it > 0 }) {
            Spacer(Modifier.height(12.dp))
            Canvas(Modifier.fillMaxWidth().height(48.dp)) {
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
                Text(strings["board_last_6h"], color = Dim, fontSize = 13.sp)
                Text(strings.format("board_today_kwh", String.format(strings.locale, "%.1f", todayKwh)), color = Dim, fontSize = 13.sp)
            }
        }
    }
}

private fun boardWatts(w: Double): String =
    if (w >= 10_000) String.format(Locale.getDefault(), "%.1f kW", w / 1000) else "${w.roundToInt()} W"

@Composable
private fun BoardTitle(text: String, strings: Strings) {
    Text(text.uppercase(strings.locale), color = Dim, fontSize = 13.sp, letterSpacing = 1.5.sp)
}

@Composable
private fun Recent(list: List<LoggedEvent>, lines: Int, strings: Strings, eventStrings: com.macerce.switchguard.core.EventStrings) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BoardTitle(strings["board_recent"], strings)
        if (list.isEmpty()) Text(strings["board_no_recent"], color = Dim, fontSize = 16.sp)
        for (e in list) {
            BoardRow(
                icon = e.icon(), tint = if (e.action == AlertAction.ALARM.name) Red else Dim,
                text = e.text(eventStrings), time = eventTime(e.time, strings), lines = lines,
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
private fun Upcoming(list: List<StandbyBoard.Upcoming>, now: Long, strings: Strings) {
    val durations = remember(strings) { strings.durations() }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
private fun BoardRow(icon: ImageVector, tint: Color, text: String, time: String, sub: String? = null, lines: Int = 1) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.size(38.dp).clip(CircleShape).background(tint.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(text, color = Ink, fontSize = 17.sp, maxLines = lines, overflow = TextOverflow.Ellipsis)
            if (sub != null) Text(sub, color = Dim, fontSize = 14.sp)
        }
        Text(time, color = Dim, fontSize = 16.sp)
    }
}
