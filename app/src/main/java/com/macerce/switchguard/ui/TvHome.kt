package com.macerce.switchguard.ui

import android.text.format.DateFormat
import android.text.format.DateUtils
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoMode
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Nightlight
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Size
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.macerce.switchguard.R
import com.macerce.switchguard.api.WeatherClient
import com.macerce.switchguard.core.DeviceKinds
import com.macerce.switchguard.core.DeviceSnapshot
import com.macerce.switchguard.core.Readiness
import com.macerce.switchguard.core.SensorKind
import com.macerce.switchguard.data.ConnState
import com.macerce.switchguard.data.EventLog
import com.macerce.switchguard.data.LiveState
import com.macerce.switchguard.data.LoggedEvent
import com.macerce.switchguard.data.Store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Date
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.roundToInt

/** TV ana ekranının alt çubuğundan açılan yerler. */
enum class TvDest { ALERTS, AUTOMATIONS, HISTORY, SETTINGS }

/** TV ana ekranının renkleri: koyu zemin, cam kartlar, tek vurgu rengi (durumla değişir). */
private object Tv {
    val ink = Color(0xFF04070A)
    val glass = Color.White.copy(alpha = 0.055f)
    val glassFocus = Color.White.copy(alpha = 0.13f)
    val edge = Color.White.copy(alpha = 0.08f)
    val text = Color(0xFFEAF2F2)
    val muted = Color(0xFF8A9A9C)
    val faint = Color(0xFF55646A)
    val ok = Color(0xFF2EE6C0)
    val okDeep = Color(0xFF1B6DFF)
    val warn = Color(0xFFFFB648)
    val bad = Color(0xFFFF5A5F)
    val idle = Color(0xFF7D8A90)
}

/**
 * TV'nin ana ekranı: uzaktan okunan sade bir pano. Üstte büyük saat ve tek satır durum; ortada cihazlar kumandayla
 * gezilen bir sırada (OK aç/kapat, basılı tut ayrıntı); altta yalnız simgeli, odaklanınca adı açılan bir çubuk.
 * Zemindeki yavaş ışık durumu söyler: turkuaz her şey yolunda, amber dikkat, kırmızı alarm/sorun, gri izleme kapalı.
 */
@Composable
fun TvHome(
    /** Ayrıntı sayfası; null = bekleme ekranı (ayrıntı açılmaz). */
    onOpenDevice: ((String) -> Unit)?,
    onOpen: (TvDest) -> Unit,
    onOpenChat: () -> Unit,
    unseenHistory: Int,
    restoreFocusTo: String?,
    onFocusRestored: () -> Unit,
    /** Bekleme ekranı / ekran koruyucu: alt çubuk yok, Geri tuşu ekranı kapatır. */
    standby: Boolean = false,
) {
    val context = LocalContext.current
    val store = remember { Store.get(context) }
    val version by store.version.collectAsStateWithLifecycle()
    val connection by LiveState.connection.collectAsStateWithLifecycle()
    val devices = remember(version) { store.snapshots }
    val shown = remember(version) { store.deviceLayout.sorted(store.snapshotsWithGroups) }
    val alarm = remember(version) { store.pendingAlarm }
    val monitoring = remember(version) { store.monitoringEnabled }
    val issues = remember(version) { Readiness.issues(readinessInput(context, store)).filter { monitoring || it in Readiness.SHOWN_WHEN_STOPPED } }

    val state = if (monitoring) connection.state else ConnState.STOPPED
    val problem = state in setOf(ConnState.NO_INTERNET, ConnState.AUTH_ERROR, ConnState.ERROR)
    val accent by animateColorAsState(
        when {
            alarm.isNotEmpty() || problem -> Tv.bad
            state == ConnState.STOPPED -> Tv.idle
            issues.isNotEmpty() || state != ConnState.LIVE -> Tv.warn
            else -> Tv.ok
        },
        tween(900),
        label = "accent",
    )

    // Son olay: alttaki tek satırlık akış.
    val log = remember { EventLog.get(context) }
    val logVersion by log.version.collectAsStateWithLifecycle()
    var lastEvent by remember { mutableStateOf<LoggedEvent?>(null) }
    LaunchedEffect(logVersion) { lastEvent = withContext(Dispatchers.IO) { log.recent(limit = 1).firstOrNull() } }

    // OK doğrudan anahtarlamaz: yanlışlıkla basılan tuş dondurucu/pompa gibi kritik cihazı kapatmasın diye önce sorulur.
    // Onay ayrı pencere değil ekranın içinde bir katman: ekran koruyucuda (DreamService) ayrı pencere açılamaz.
    var asking by remember { mutableStateOf<Pair<DeviceSnapshot, SwitchControl>?>(null) }
    // Onay kapanınca odak sorulan karta döner.
    var refocus by remember { mutableStateOf<String?>(null) }
    Box(Modifier.fillMaxSize().aurora(accent)) {
        Column(Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
            TopBand(state, connection.detail, monitoring, devices, accent)
            Spacer(Modifier.weight(1f))
            if (alarm.isNotEmpty()) {
                AlarmStrip(alarm) { Actions.dismissAlarm(context) }
                Spacer(Modifier.height(20.dp))
            }
            DeviceRow(
                shown, accent, alarm.isEmpty(), onOpenDevice,
                restoreFocusTo = refocus ?: restoreFocusTo,
                onFocusRestored = { if (refocus != null) refocus = null else onFocusRestored() },
                onAsk = { d, c -> asking = d to c },
                hint = stringResource(if (standby) R.string.tv_standby_hint else R.string.tv_home_hint),
                enabled = asking == null,
            )
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                LastEventLine(lastEvent, Modifier.weight(1f))
                Spacer(Modifier.width(16.dp))
                if (!standby) Dock(monitoring, unseenHistory, issues.isNotEmpty(), accent, onOpen, onOpenChat)
            }
        }
        asking?.let { (d, c) ->
            val close = { refocus = d.id; asking = null }
            ConfirmSwitch(d, turnOn = !c.shown, onConfirm = { if (!c.busy) c.toggle(!c.shown); close() }, onDismiss = close)
        }
    }
}

/**
 * Zemin: üç yumuşak ışık, farklı hız ve yönlerde yavaşça süzülür (Lissajous yolları, tekrar etmeyen desen).
 * Zaman kare saatinden okunur ve yalnız çizim aşamasında kullanılır: ekran yeniden oluşturulmaz, akıcı kalır.
 * Geçişler çok duraklı ve aynı rengin saydamına iner: koyu halka ve bantlanma olmaz. Kenar boşluğunun dışına da taşar.
 */
@Composable
private fun Modifier.aurora(accent: Color): Modifier {
    val time = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while (true) withFrameNanos { time.floatValue = (it - start) / 1_000_000_000f }
    }
    val tint = rememberUpdatedState(accent)
    return this.drawBehind {
        val t = time.floatValue
        val bleed = 80.dp.toPx()
        drawRect(Tv.ink, topLeft = Offset(-bleed, -bleed), size = Size(size.width + 2 * bleed, size.height + 2 * bleed))
        val w = size.width
        val h = size.height
        fun glow(color: Color, alpha: Float, cx: Float, cy: Float, r: Float) {
            val c = Offset(cx, cy)
            val stops = listOf(1f, 0.72f, 0.42f, 0.18f, 0.05f, 0f).map { color.copy(alpha = alpha * it) }
            drawCircle(Brush.radialGradient(stops, center = c, radius = r), radius = r, center = c)
        }
        val acc = tint.value
        // Nefes: 9 sn'lik çok hafif parlaklık dalgası.
        val breathe = 0.85f + 0.15f * sin(t * 0.7f)
        glow(acc, 0.26f * breathe, w * (0.22f + 0.16f * sin(t * 0.061f)), h * (0.18f + 0.14f * cos(t * 0.047f)), w * 0.62f)
        glow(Tv.okDeep, 0.17f, w * (0.82f + 0.12f * sin(t * 0.039f + 1.7f)), h * (0.92f + 0.12f * cos(t * 0.053f + 0.6f)), w * 0.55f)
        glow(Color(0xFF7A5CFF), 0.09f, w * (0.55f + 0.28f * sin(t * 0.027f + 3.1f)), h * (0.55f + 0.22f * sin(t * 0.044f + 0.9f)), w * 0.45f)
    }
}

@Composable
private fun TopBand(state: ConnState, detail: String, monitoring: Boolean, devices: List<DeviceSnapshot>, accent: Color) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    // Saat dakika başında güncellenir.
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            delay(60_000 - System.currentTimeMillis() % 60_000 + 50)
            value = System.currentTimeMillis()
        }
    }
    val time = remember(now) { DateFormat.getTimeFormat(context).format(Date(now)) }
    val date = remember(now, locale) {
        android.icu.text.SimpleDateFormat(DateFormat.getBestDateTimePattern(locale, "EEEEdMMMM"), locale).format(Date(now))
    }
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.Top) {
        Column {
            Text(time, fontSize = 76.sp, fontWeight = FontWeight.Thin, color = Tv.text, letterSpacing = (-2).sp, lineHeight = 80.sp)
            Text(
                date.uppercase(locale),
                fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Tv.muted, letterSpacing = 3.sp,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
        Spacer(Modifier.width(44.dp))
        TvWeather(Modifier.padding(top = 22.dp).weight(1f))
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(top = 18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PulseDot(accent, pulsing = monitoring && state == ConnState.LIVE)
                Spacer(Modifier.width(12.dp))
                Text(stringResource(connTitle(state)), fontSize = 20.sp, fontWeight = FontWeight.Light, color = Tv.text)
            }
            Spacer(Modifier.height(6.dp))
            val on = devices.count { d -> d.online && d.switches.values.any { it } }
            val offline = devices.count { !it.online }
            val power = devices.filter { it.online && it.hasEnergy }.sumOf { it.power ?: 0.0 }
            val parts = buildList {
                add("${devices.size} ${stringResource(R.string.stat_devices).lowercase(locale)}")
                add("$on ${stringResource(R.string.stat_on).lowercase(locale)}")
                if (offline > 0) add("$offline ${stringResource(R.string.stat_offline).lowercase(locale)}")
                if (devices.any { it.hasEnergy }) add(formatWatts(power))
            }
            Text(
                if (!monitoring) stringResource(R.string.monitoring_off_hint) else if (detail.isNotEmpty()) detail else parts.joinToString("   ·   "),
                fontSize = 13.sp, color = Tv.muted, letterSpacing = 0.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

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

@Composable
private fun AlarmStrip(lines: List<String>, onDismiss: () -> Unit) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    val shape = RoundedCornerShape(22.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .background(Brush.horizontalGradient(listOf(Tv.bad.copy(alpha = 0.32f), Tv.bad.copy(alpha = 0.08f))), shape)
            .border(1.dp, Tv.bad.copy(alpha = 0.5f), shape)
            .padding(horizontal = 22.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PulseDot(Tv.bad, pulsing = true)
        Spacer(Modifier.width(16.dp))
        Text(
            lines.joinToString("  ·  "), fontSize = 18.sp, color = Tv.text, maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(16.dp))
        Box(
            Modifier
                .focusRequester(focus)
                .tvFocusRing(CircleShape, color = Color.White)
                .clip(CircleShape)
                .background(Tv.bad)
                .clickable(onClick = onDismiss)
                .padding(horizontal = 20.dp, vertical = 10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.VolumeOff, null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.action_dismiss_alarm), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun DeviceRow(
    devices: List<DeviceSnapshot>,
    accent: Color,
    takeFocus: Boolean,
    onOpenDevice: ((String) -> Unit)?,
    restoreFocusTo: String?,
    onFocusRestored: () -> Unit,
    onAsk: (DeviceSnapshot, SwitchControl) -> Unit,
    hint: String,
    enabled: Boolean,
) {
    if (devices.isEmpty()) {
        val ctx = LocalContext.current
        val loggedIn = remember { Store.get(ctx).hasAnyAccount }
        Text(
            stringResource(if (loggedIn) R.string.no_devices else R.string.login_needed_title),
            color = Tv.muted, fontSize = 18.sp, modifier = Modifier.padding(8.dp),
        )
        return
    }
    val requesters = remember(devices.map { it.id }) { devices.associate { it.id to FocusRequester() } }
    // Açılışta ilk kart; detaydan dönüşte açılan kart odak alır (kart yerleşene kadar kısa süre denenir).
    LaunchedEffect(restoreFocusTo, takeFocus) {
        val target = restoreFocusTo ?: if (takeFocus) devices.first().id else return@LaunchedEffect
        for (attempt in 1..20) {
            val r = runCatching { requesters[target]?.requestFocus() }.getOrNull()
            if (r != null && r != false) break
            delay(50)
        }
        if (restoreFocusTo != null) onFocusRestored()
    }
    Column {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        ) {
            itemsIndexed(devices, key = { _, d -> d.id }) { _, d ->
                DeviceOrb(
                    d, accent, requesters.getValue(d.id), enabled,
                    onOpen = onOpenDevice?.let { open -> { open(d.id) } },
                    onAsk = { c -> onAsk(d, c) },
                )
            }
        }
        Text(
            hint,
            fontSize = 12.sp, color = Tv.faint, letterSpacing = 1.sp,
            modifier = Modifier.padding(start = 10.dp, top = 2.dp),
        )
    }
}

/**
 * Cihaz kartı: koyu cam, açıkken altta vurgu renginde ışık ve simgede hale. Odaklanınca büyür, kenarı aydınlanır.
 * Tek kanallı cihazda OK aç/kapat; çok kanallı, sensör ya da çevrimdışı cihazda OK ayrıntıyı açar. Basılı tutmak
 * her zaman ayrıntıyı açar.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DeviceOrb(device: DeviceSnapshot, accent: Color, focus: FocusRequester, enabled: Boolean, onOpen: (() -> Unit)?, onAsk: (SwitchControl) -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val lift by animateFloatAsState(if (focused) 1f else 0f, tween(220), label = "lift")
    val single = device.online && !device.sensorOnly && !device.isMultiChannel && device.switches.size == 1
    val control = if (single) {
        val (channel, on) = device.switches.entries.first()
        rememberSwitchControl(device, channel, on)
    } else null
    val on = device.online && (control?.shown ?: device.switches.values.any { it })
    val glow by animateFloatAsState(if (on) 1f else 0f, tween(500), label = "glow")
    val kind = remember(device.name, device.isMultiChannel, device.hasEnergy, device.cloud, device.sensorOnly, SensorKind.DOOR in device.sensors) { DeviceKinds.of(device) }
    val shape = RoundedCornerShape(26.dp)
    val lit = if (device.online) accent else Tv.faint

    Column(
        Modifier
            .width(176.dp)
            .height(196.dp)
            .graphicsLayer {
                val s = 1f + 0.08f * lift
                scaleX = s
                scaleY = s
            }
            .clip(shape)
            .drawBehind {
                // Açık cihazın altından yükselen ışık; odakta dış hale.
                if (glow > 0f) {
                    drawRect(
                        Brush.verticalGradient(listOf(Color.Transparent, lit.copy(alpha = 0.22f * glow)), startY = size.height * 0.35f),
                    )
                }
            }
            .background(if (focused) Tv.glassFocus else Tv.glass, shape)
            .border(
                if (focused) 1.5.dp else 1.dp,
                when {
                    focused -> Color.White.copy(alpha = 0.85f)
                    on -> lit.copy(alpha = 0.45f)
                    else -> Tv.edge
                },
                shape,
            )
            .focusRequester(focus)
            .onFocusChanged { focused = it.isFocused }
            .combinedClickable(
                enabled = enabled,
                role = if (control != null) Role.Switch else Role.Button,
                onClick = {
                    if (control != null) { if (!control.busy) onAsk(control) } else onOpen?.invoke()
                },
                onLongClick = onOpen,
            )
            .padding(18.dp),
    ) {
        Box(
            Modifier
                .size(54.dp)
                .drawBehind {
                    if (glow > 0f) drawCircle(lit.copy(alpha = 0.22f * glow), radius = size.minDimension * 0.62f)
                }
                .background(if (on) lit else Color.White.copy(alpha = 0.07f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (device.online) kind.icon() else Icons.Rounded.CloudOff,
                contentDescription = null,
                tint = if (on) Tv.ink else if (device.online) Tv.text else Tv.faint,
                modifier = Modifier.size(28.dp),
            )
            if (control?.busy == true) CircularProgressIndicator(Modifier.size(54.dp), color = Color.White, strokeWidth = 2.dp)
        }
        Spacer(Modifier.weight(1f))
        // Sensör: büyük değer; diğerleri: durum kelimesi.
        val sensor = sensorSummary(device)
        val big = when {
            !device.online -> stringResource(R.string.label_offline_title)
            device.sensorOnly && sensor.isNotEmpty() -> sensor.substringBefore(" · ")
            device.isMultiChannel -> "${device.switches.values.count { it }}/${device.switches.size}"
            on -> stringResource(R.string.state_on_title)
            else -> stringResource(R.string.state_off_title)
        }
        Text(
            big,
            fontSize = 26.sp,
            fontWeight = FontWeight.Light,
            color = when {
                !device.online -> Tv.bad.copy(alpha = 0.85f)
                on -> lit
                else -> Tv.text
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(2.dp))
        Text(device.name, fontSize = 15.sp, color = Tv.text.copy(alpha = 0.92f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        val extra = buildList {
            if (device.online && device.hasEnergy) add(formatWatts(device.power ?: 0.0))
            if (device.online && !device.sensorOnly && sensor.isNotEmpty()) add(sensor)
            if (device.online && device.sensorOnly && " · " in sensor) add(sensor.substringAfter(" · "))
        }.joinToString(" · ")
        Text(extra.ifEmpty { " " }, fontSize = 12.sp, color = Tv.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun LastEventLine(event: LoggedEvent?, modifier: Modifier) {
    if (event == null) {
        Spacer(modifier)
        return
    }
    val now by produceState(System.currentTimeMillis()) { while (true) { delay(30_000); value = System.currentTimeMillis() } }
    Row(modifier.padding(start = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(event.icon(), null, tint = Tv.muted, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            event.text(eventStrings()),
            fontSize = 13.sp, color = Tv.muted, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            if (now - event.time < 60_000) DateFormat.getTimeFormat(LocalContext.current).format(Date(event.time))
            else DateUtils.getRelativeTimeSpanString(event.time, maxOf(now, event.time), DateUtils.MINUTE_IN_MILLIS).toString(),
            fontSize = 12.sp, color = Tv.faint, maxLines = 1,
        )
    }
}

private class DockItem(val icon: ImageVector, val label: Int, val badge: Int = 0, val dot: Boolean = false, val onClick: () -> Unit)

/** Alt çubuk: yalnız simgeler; odaklanan öğe yumuşakça genişleyip adını gösterir. */
@Composable
private fun Dock(
    monitoring: Boolean,
    unseen: Int,
    attention: Boolean,
    accent: Color,
    onOpen: (TvDest) -> Unit,
    onOpenChat: () -> Unit,
) {
    val context = LocalContext.current
    val items = listOf(
        DockItem(Icons.Rounded.NotificationsActive, R.string.tab_alerts) { onOpen(TvDest.ALERTS) },
        DockItem(Icons.Rounded.AutoMode, R.string.tab_automations) { onOpen(TvDest.AUTOMATIONS) },
        DockItem(Icons.Rounded.History, R.string.tab_history, badge = unseen) { onOpen(TvDest.HISTORY) },
        DockItem(Icons.Rounded.Mic, R.string.chat_voice, onClick = onOpenChat),
        DockItem(Icons.Rounded.Nightlight, R.string.board_menu) { StandbyActivity.start(context) },
        if (monitoring) DockItem(Icons.Rounded.Stop, R.string.action_stop_monitoring) { Actions.stopMonitoring(context) }
        else DockItem(Icons.Rounded.PlayArrow, R.string.action_start_monitoring, dot = true) { Actions.startMonitoring(context) },
        // Dikkat gereken izin/ayar sorunu: Ayarlar'da nokta.
        DockItem(Icons.Rounded.Settings, R.string.tab_settings, dot = attention) { onOpen(TvDest.SETTINGS) },
    )
    Row(
        Modifier
            .background(Color.White.copy(alpha = 0.05f), CircleShape)
            .border(1.dp, Tv.edge, CircleShape)
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEach { DockButton(it, accent) }
    }
}

@Composable
private fun DockButton(item: DockItem, accent: Color) {
    var focused by remember { mutableStateOf(false) }
    val bg by animateColorAsState(if (focused) Tv.text else Color.Transparent, tween(180), label = "dockBg")
    val fg = if (focused) Tv.ink else Tv.text.copy(alpha = 0.8f)
    Row(
        Modifier
            .height(44.dp)
            .clip(CircleShape)
            .background(bg)
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = item.onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            Icon(item.icon, contentDescription = stringResource(item.label), tint = fg, modifier = Modifier.size(22.dp))
            if (item.badge > 0 || item.dot) {
                Box(
                    Modifier.align(Alignment.TopEnd).padding(start = 0.dp).size(8.dp)
                        .background(if (item.badge > 0) Tv.bad else accent, CircleShape),
                )
            }
        }
        AnimatedVisibility(focused, enter = fadeIn() + expandHorizontally(), exit = fadeOut() + shrinkHorizontally()) {
            Text(
                stringResource(item.label) + if (item.badge > 0) "  ${if (item.badge > 99) "99+" else item.badge}" else "",
                color = fg, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1,
                modifier = Modifier.padding(start = 10.dp, end = 4.dp),
            )
        }
    }
}

/** Dış hava (Open-Meteo, konum Ayarlar'dan): simge, büyük sıcaklık, durum ve günün en düşük/en yüksek değeri. */
@Composable
private fun TvWeather(modifier: Modifier) {
    val context = LocalContext.current
    val store = remember { Store.get(context) }
    val version by store.version.collectAsStateWithLifecycle()
    val lat = remember(version) { store.latitude }
    val lon = remember(version) { store.longitude }
    val w by produceState<WeatherClient.Today?>(null, lat, lon) {
        if (lat == null || lon == null) return@produceState
        while (true) {
            withContext(Dispatchers.IO) { runCatching { WeatherClient.today(lat, lon) }.getOrNull() }?.let { value = it }
            delay(if (value == null) 2 * 60_000L else 30 * 60_000L)
        }
    }
    val today = w
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        // Saatten ince bir çizgiyle ayrılır.
        Box(Modifier.width(1.dp).height(64.dp).background(Tv.edge))
        Spacer(Modifier.width(28.dp))
        if (today == null) {
            if (lat == null) Text(stringResource(R.string.board_no_location), fontSize = 13.sp, color = Tv.faint, maxLines = 2)
            return@Row
        }
        val (icon, label) = weatherLook(today.code, today.isDay)
        Icon(icon, null, tint = if (today.code == 0 && today.isDay) Tv.warn else Tv.text.copy(alpha = 0.9f), modifier = Modifier.size(40.dp))
        Spacer(Modifier.width(14.dp))
        Text("${today.temp.roundToInt()}°", fontSize = 46.sp, fontWeight = FontWeight.Thin, color = Tv.text)
        Spacer(Modifier.width(14.dp))
        Column {
            Text(stringResource(label), fontSize = 15.sp, color = Tv.text.copy(alpha = 0.9f), maxLines = 1)
            val parts = listOfNotNull(
                if (today.min != null && today.max != null) "↓${today.min.roundToInt()}°  ↑${today.max.roundToInt()}°" else null,
                today.rainChance?.takeIf { it > 0 }?.let { stringResource(R.string.board_rain, it) },
            )
            if (parts.isNotEmpty()) Text(parts.joinToString("  ·  "), fontSize = 13.sp, color = Tv.muted, maxLines = 1)
        }
    }
}

/**
 * Aç/kapat onayı: ekranın üstünde koyu katman + ortada panel. Odak "Vazgeç"te başlar: art arda iki kez OK'a basmak
 * cihazı değiştirmez; onaylamak için sağa geçilir. Yön tuşları panelden çıkamaz; Geri vazgeçer.
 */
@Composable
private fun ConfirmSwitch(device: DeviceSnapshot, turnOn: Boolean, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val cancelFocus = remember { FocusRequester() }
    val okFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        for (attempt in 1..20) {
            val r = runCatching { cancelFocus.requestFocus() }.getOrNull()
            if (r != null && r != false) break
            delay(30)
        }
    }
    val kind = remember(device.name, device.isMultiChannel, device.hasEnergy, device.cloud, device.sensorOnly) { DeviceKinds.of(device) }
    val color = if (turnOn) Tv.ok else Tv.warn
    Box(
        Modifier
            .fillMaxSize()
            // Karartma TV kenar payının dışına da taşar: ekranın tamamı kararır.
            .drawBehind {
                val bleed = 80.dp.toPx()
                drawRect(Color.Black.copy(alpha = 0.6f), topLeft = Offset(-bleed, -bleed), size = Size(size.width + 2 * bleed, size.height + 2 * bleed))
            }
            .onPreviewKeyEvent { e ->
                val down = e.type == KeyEventType.KeyDown
                when (e.key) {
                    Key.DirectionLeft -> { if (down) runCatching { cancelFocus.requestFocus() }; true }
                    Key.DirectionRight -> { if (down) runCatching { okFocus.requestFocus() }; true }
                    Key.DirectionUp, Key.DirectionDown -> true
                    Key.Back, Key.Escape -> { if (e.type == KeyEventType.KeyUp) onDismiss(); true }
                    else -> false
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        val shape = RoundedCornerShape(30.dp)
        Column(
            Modifier
                .width(440.dp)
                .clip(shape)
                .background(Color(0xF2101619))
                .border(1.dp, Color.White.copy(alpha = 0.12f), shape)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.size(64.dp).background(color.copy(alpha = 0.16f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(kind.icon(), null, tint = color, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.height(18.dp))
            Text(device.name, fontSize = 22.sp, color = Tv.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(if (turnOn) R.string.tv_confirm_on else R.string.tv_confirm_off),
                fontSize = 16.sp, color = Tv.muted,
            )
            Spacer(Modifier.height(28.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                DialogButton(stringResource(R.string.action_cancel), Tv.text, Color.White.copy(alpha = 0.08f), Modifier.focusRequester(cancelFocus), onDismiss)
                DialogButton(
                    stringResource(if (turnOn) R.string.auto_action_turn_on else R.string.auto_action_turn_off),
                    Tv.ink, color, Modifier.focusRequester(okFocus), onConfirm,
                )
            }
        }
    }
}

@Composable
private fun DialogButton(text: String, fg: Color, bg: Color, modifier: Modifier, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.08f else 1f, tween(160), label = "btn")
    Box(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .onFocusChanged { focused = it.isFocused }
            .clip(CircleShape)
            .background(bg)
            .border(if (focused) 2.dp else 0.dp, if (focused) Color.White else Color.Transparent, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 30.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = fg, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}

/**
 * TV bekleme ekranı (uygulamadaki "Bekleme ekranı" ve Android ekran koruyucusu): aynı pano, alt çubuksuz.
 * Cihazlar OK + onayla açılıp kapanır; Geri ekranı kapatır ve TV kaldığı ekrana döner. TV kenar payı burada verilir.
 */
@Composable
fun TvStandby() {
    CompositionLocalProvider(LocalIsTv provides true) {
        Box(Modifier.fillMaxSize().background(Tv.ink)) {
            Box(Modifier.fillMaxSize().padding(FormFactor.TV_OVERSCAN_H, FormFactor.TV_OVERSCAN_V)) {
                TvHome(
                    onOpenDevice = null, onOpen = {}, onOpenChat = {}, unseenHistory = 0,
                    restoreFocusTo = null, onFocusRestored = {}, standby = true,
                )
            }
        }
    }
}
