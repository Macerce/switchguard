package com.macerce.switchguard.ui

import android.app.KeyguardManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Grain
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Thunderstorm
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material.icons.rounded.Umbrella
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material.icons.rounded.WbCloudy
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.macerce.switchguard.R
import com.macerce.switchguard.api.WeatherClient
import com.macerce.switchguard.core.AlertAction
import com.macerce.switchguard.core.AppLanguage
import com.macerce.switchguard.core.DeviceGroup
import com.macerce.switchguard.core.DeviceSnapshot
import com.macerce.switchguard.core.DurationText
import com.macerce.switchguard.core.StandbyBoard
import com.macerce.switchguard.core.StandbyBoard.UpcomingKind
import com.macerce.switchguard.data.ConnState
import com.macerce.switchguard.data.EventLog
import com.macerce.switchguard.data.LiveState
import com.macerce.switchguard.data.LoggedEvent
import com.macerce.switchguard.data.MetricsLog
import com.macerce.switchguard.data.RuntimeState
import com.macerce.switchguard.data.Store
import com.macerce.switchguard.service.Briefing
import com.macerce.switchguard.service.ChatRunner
import com.macerce.switchguard.service.Speaker
import com.macerce.switchguard.ui.theme.SwitchGuardTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Bekleme ekranı: şarjdayken ya da masada dururken açık kalan, kilit ekranının üstünde de görünen tam ekran pano.
 * Saat, hava, kritik durum, bütün cihazlar (dokununca aç/kapat), anlık güç (yarım daire), son hareketler ve yakında
 * çalışacak işler; altta sesli komut, durumu sesli okuma ve günün özeti düğmeleri.
 * Aynı içerik Android ekran koruyucusu olarak da çalışır (bkz. [com.macerce.switchguard.service.StandbyDream]).
 */
class StandbyActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) = super.attachBaseContext(AppLanguage.wrap(newBase))

    /** Son sesli komut (an, söylenen); pano bunu görünce yürütür. */
    private var heard by mutableStateOf<Pair<Long, String>?>(null)
    private val voice = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val text = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull { it.isNotBlank() }
        if (result.resultCode == RESULT_OK && text != null) heard = System.currentTimeMillis() to text
    }

    private fun listen() {
        Speaker.stop()
        try {
            voice.launch(Speaker.listenIntent(this))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.chat_voice_unavailable, Toast.LENGTH_LONG).show()
        }
    }

    /** Cihaz kontrolü ve sesli komut kilit ekranından verilemez: kilitliyse önce kilit açtırılır. */
    private fun unlocked(action: () -> Unit) {
        val km = getSystemService(KeyguardManager::class.java)
        if (Build.VERSION.SDK_INT >= 26 && km?.isKeyguardLocked == true) {
            km.requestDismissKeyguard(this, object : KeyguardManager.KeyguardDismissCallback() {
                override fun onDismissSucceeded() = action()
            })
        } else action()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        setContent {
            SwitchGuardTheme {
                StandbyBoardScreen(
                    onOpenApp = { openApp() },
                    onClose = { finish() },
                    host = remember { StandbyHost(listen = { listen() }, unlocked = { unlocked(it) }) },
                    heard = heard,
                )
            }
        }
    }

    /** Kilit ekranındaysa önce kilidi açtırır, sonra uygulamayı açar. */
    private fun openApp() {
        val go = {
            startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            finish()
        }
        val km = getSystemService(KeyguardManager::class.java)
        if (Build.VERSION.SDK_INT >= 26 && km?.isKeyguardLocked == true) {
            km.requestDismissKeyguard(this, object : KeyguardManager.KeyguardDismissCallback() {
                override fun onDismissSucceeded() = go()
            })
        } else go()
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, StandbyActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}

// Ekran hep koyu: OLED'de siyah piksel yanmaz, gece gözü almaz.
private val Bg = Color(0xFF000000)
private val Ink = Color(0xFFE8EEED)
private val Dim = Color(0xFF8A9895)
private val Faint = Color(0xFF26302E)
private val Teal = Color(0xFF26A69A)
private val Amber = Color(0xFFFFB300)
private val Red = Color(0xFFEF5350)

private data class PowerView(val series: List<Double>, val todayKwh: Double)

/** Panonun etkinliğe bağlı işleri: konuşma tanıma penceresi ve kilit açtırma. */
class StandbyHost(val listen: () -> Unit, val unlocked: (() -> Unit) -> Unit)

/**
 * Bekleme panosu. [onOpenApp]/[onClose] null ise (ekran koruyucu) dokununca menü çıkmaz; sistem dokunuşla kapatır.
 * [host] null ise (ekran koruyucu) cihaz kutucukları yalnız gösterilir, düğmeler çıkmaz. [heard] son sesli komut.
 */
@Composable
fun StandbyBoardScreen(onOpenApp: (() -> Unit)?, onClose: (() -> Unit)?, host: StandbyHost? = null, heard: Pair<Long, String>? = null) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Alttaki pencere: sesli komutun yanıtı, durum özeti ya da günün özeti; dokununca kapanır, okuma kesilir.
    var panel by remember { mutableStateOf<String?>(null) }
    var working by remember { mutableStateOf(false) }
    val boardName = stringResource(R.string.board_menu)
    val notUnderstood = stringResource(R.string.chat_not_understood)
    DisposableEffect(Unit) { onDispose { Speaker.stop() } }
    fun show(text: String, ask: Boolean = false) {
        panel = text
        // Yanıt bir soruysa ("Hangi kanal?", "Emin misiniz?") okuma bitince mikrofon yeniden açılır.
        Speaker.say(context, text) { if (ask) host?.listen?.invoke() }
    }
    fun read(build: () -> String) {
        if (working) return
        working = true
        panel = null
        Speaker.stop()
        scope.launch {
            val text = withContext(Dispatchers.IO) { runCatching(build).getOrElse { "⚠️ " + it.message.orEmpty() } }
            working = false
            show(text)
        }
    }
    LaunchedEffect(heard) {
        val said = heard?.second ?: return@LaunchedEffect
        working = true
        panel = null
        val reply = runCatching { ChatRunner.reply(context, said, boardName) }.getOrElse { ChatRunner.Reply("⚠️ " + it.message.orEmpty()) }
        working = false
        show(reply?.text ?: notUnderstood, reply?.ask == true)
    }
    val store = remember { Store.get(context) }
    val events = remember { EventLog.get(context) }
    val metrics = remember { MetricsLog.get(context) }
    val version by store.version.collectAsStateWithLifecycle()
    val eventsVersion by events.version.collectAsStateWithLifecycle()
    val conn by LiveState.connection.collectAsStateWithLifecycle()

    // Saniyede bir tik; ağır işler dakikada bir.
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000 - now % 1000)
        }
    }
    val minute = now / 60_000

    val snapshots = remember(version) { store.snapshots }
    val tiles = remember(version) { store.snapshotsWithGroups }
    val monitored = remember(version) { store.monitoredIds() }
    val alarmLines = remember(version) { store.pendingAlarm }
    val away = remember(version, minute) { store.isAway() }
    val lat = remember(version) { store.latitude }
    val lon = remember(version) { store.longitude }
    val dimAtNight = remember(version) { store.standbyDim }

    val recent by produceState(emptyList<LoggedEvent>(), eventsVersion) {
        value = withContext(Dispatchers.IO) { events.recent(limit = 30) }.filter { it.kind != "MONITOR_GAP" }.take(5)
    }
    val upcoming = remember(version, minute) {
        StandbyBoard.upcoming(
            store.automations, store.cycles, store.cycleRuns, store.chatTemplates.orEmpty(),
            RuntimeState.get(context).templateRuns, lat, lon, System.currentTimeMillis(),
        ).take(4)
    }
    val energyIds = remember(snapshots) { snapshots.filter { it.hasEnergy && !DeviceGroup.isGroup(it.id) }.map { it.id } }
    val livePower = remember(snapshots) {
        snapshots.filter { it.online && !DeviceGroup.isGroup(it.id) }.sumOf { it.power ?: 0.0 }
    }
    val power by produceState<PowerView?>(null, energyIds, minute) {
        if (energyIds.isEmpty()) { value = null; return@produceState }
        value = withContext(Dispatchers.IO) {
            val t = System.currentTimeMillis()
            val from = t - 6 * 3_600_000L
            val series = StandbyBoard.totalPower(energyIds.map { metrics.powerSamples(it, from, t) }, from, t, 73)
            val midnight = Calendar.getInstance().apply {
                timeInMillis = t; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            PowerView(series, energyIds.sumOf { runCatching { metrics.kwh(it, midnight, t) }.getOrDefault(0.0) })
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
            .then(
                if (onOpenApp != null) Modifier.clickable(remember { MutableInteractionSource() }, indication = null) {
                    if (panel != null) { panel = null; Speaker.stop() } else menu = !menu
                }
                else Modifier
            ),
    ) {
        BoxWithConstraints(
            Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 24.dp).offset(sx.dp, sy.dp).alpha(if (night) 0.55f else 1f),
        ) {
            val wide = maxWidth > maxHeight
            // Yatay telefonda yükseklik az: daha az satır, tek satırlık olaylar.
            val compact = maxHeight < 520.dp
            val events = recent.take(if (compact) 3 else 5)
            val next = upcoming.take(if (compact) 2 else 4)
            val lines = if (compact) 1 else 2
            val status: @Composable () -> Unit = {
                StatusPill(alarmLines, conn.state, snapshots.filter { it.id in monitored && !it.online }.map { it.name }, monitored.size, away)
            }
            val devices: @Composable () -> Unit = {
                if (tiles.isNotEmpty()) DeviceTiles(
                    tiles,
                    onToggle = host?.let { h ->
                        { d, ch, on ->
                            h.unlocked {
                                scope.launch {
                                    val r = Actions.setSwitch(context, d, ch, on)
                                    if (r.isFailure) panel = "⚠️ ${d.name}: ${r.exceptionOrNull()?.message.orEmpty()}"
                                }
                            }
                        }
                    },
                )
            }
            val gauge: @Composable (Dp) -> Unit = { w ->
                val p = power
                if (p != null) PowerGauge(livePower, p.series, p.todayKwh, w)
            }
            if (wide) {
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(32.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1.1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Clock(now, big = !compact)
                        WeatherLine(weather, lat != null)
                        status()
                        devices()
                        if (host != null) Spacer(Modifier.height(64.dp))
                    }
                    if (power != null) {
                        Box(Modifier.weight(0.8f), contentAlignment = Alignment.Center) { gauge(if (compact) 220.dp else 280.dp) }
                    }
                    Column(Modifier.weight(1.1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Recent(events, lines)
                        Upcoming(next, now)
                    }
                }
            } else {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    Clock(now, big = true)
                    WeatherLine(weather, lat != null)
                    status()
                    devices()
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { gauge(260.dp) }
                    Recent(events, lines)
                    Upcoming(next, now)
                    if (host != null) Spacer(Modifier.height(64.dp))
                }
            }
        }
        if (host != null) {
            Row(
                Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp).offset(sx.dp, sy.dp).alpha(if (night) 0.55f else 1f),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                BoardAction(Icons.Rounded.Mic, stringResource(R.string.chat_voice)) { host.unlocked { host.listen() } }
                BoardAction(Icons.Rounded.RecordVoiceOver, stringResource(R.string.board_brief)) { read { Briefing.status(context) } }
                BoardAction(Icons.Rounded.Today, stringResource(R.string.summary_title)) { read { Briefing.summary(context) } }
            }
            AnimatedVisibility(
                panel != null || working,
                Modifier.align(Alignment.BottomCenter).padding(start = 20.dp, end = 20.dp, bottom = 88.dp),
                enter = fadeIn(), exit = fadeOut(),
            ) {
                Column(
                    Modifier.widthIn(max = 560.dp).heightIn(max = 380.dp).clip(RoundedCornerShape(20.dp)).background(Color(0xFF1B2422))
                        .clickable { panel = null; Speaker.stop() }
                        .verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 14.dp),
                ) {
                    Text(panel ?: stringResource(R.string.chat_typing), color = Ink, fontSize = 16.sp, lineHeight = 23.sp)
                }
            }
        }
        AnimatedVisibility(
            menu && panel == null && !working,
            Modifier.align(Alignment.BottomCenter).padding(bottom = if (host != null) 88.dp else 24.dp), enter = fadeIn(), exit = fadeOut(),
        ) {
            Row(
                Modifier.clip(RoundedCornerShape(28.dp)).background(Color(0xFF1B2422)).padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                onOpenApp?.let { TextButton(onClick = it) { Text(stringResource(R.string.board_open_app), color = Teal) } }
                onClose?.let { TextButton(onClick = it) { Text(stringResource(R.string.board_close), color = Ink) } }
            }
        }
    }
}

/** Alttaki yuvarlak düğme (sesli komut, durumu oku, günün özeti). */
@Composable
private fun BoardAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(52.dp).clip(CircleShape).background(Color(0xFF1B2422)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, label, tint = Ink, modifier = Modifier.size(26.dp)) }
}

/**
 * Bütün cihazlar (gruplar dahil) küçük kutucuklar halinde; çok kanallıda her kanal ayrı kutucuk.
 * [onToggle] null ise (ekran koruyucu) yalnız durum gösterilir.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DeviceTiles(devices: List<DeviceSnapshot>, onToggle: ((DeviceSnapshot, Int, Boolean) -> Unit)?) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BoardTitle(stringResource(R.string.tab_devices))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for (d in devices) {
                val channels = if (d.isMultiChannel) d.switches.keys.sorted() else listOf(d.switches.keys.minOrNull() ?: 0)
                for (ch in channels) {
                    val on = d.online && d.switches[ch] == true
                    val shape = RoundedCornerShape(14.dp)
                    Row(
                        Modifier.clip(shape).background(if (on) Teal.copy(alpha = 0.24f) else Faint)
                            .then(if (onToggle != null && d.online) Modifier.clickable { onToggle(d, ch, !on) } else Modifier)
                            .alpha(if (d.online) 1f else 0.5f)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            if (d.online) Icons.Rounded.PowerSettingsNew else Icons.Rounded.CloudOff, null,
                            tint = if (on) Teal else Dim, modifier = Modifier.size(18.dp),
                        )
                        Text(
                            d.name,
                            color = if (on) Ink else Dim, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 150.dp),
                        )
                        // Kanal numarası ayrı yazılır ki uzun adda kesilmesin.
                        if (channels.size > 1) Text((ch + 1).toString(), color = if (on) Teal else Dim, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
private fun Clock(now: Long, big: Boolean) {
    val locale = LocalConfiguration.current.locales[0]
    val h24 = is24h()
    val time = remember(now / 1000, locale, h24) {
        android.text.format.DateFormat.format(
            android.text.format.DateFormat.getBestDateTimePattern(locale, if (h24) "Hm" else "hm"), now,
        ).toString()
    }
    val date = remember(now / 60_000, locale) {
        android.text.format.DateFormat.format(android.text.format.DateFormat.getBestDateTimePattern(locale, "EEEEdMMMM"), now).toString()
    }
    Column {
        Text(time, color = Ink, fontSize = if (big) 104.sp else 80.sp, fontWeight = FontWeight.Light, lineHeight = if (big) 108.sp else 84.sp)
        Text(date, color = Dim, fontSize = 20.sp)
    }
}

@Composable
private fun is24h(): Boolean = android.text.format.DateFormat.is24HourFormat(LocalContext.current)

@Composable
private fun WeatherLine(w: WeatherClient.Today?, hasLocation: Boolean) {
    if (w == null) {
        if (!hasLocation) Text(stringResource(R.string.board_no_location), color = Dim, fontSize = 14.sp)
        return
    }
    val (icon, label) = weatherLook(w.code, w.isDay)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, null, tint = if (w.code == 0 && w.isDay) Amber else Ink, modifier = Modifier.size(40.dp))
        Text("${w.temp.roundToInt()}°", color = Ink, fontSize = 40.sp, fontWeight = FontWeight.Light)
        Column {
            Text(stringResource(label), color = Ink, fontSize = 16.sp)
            val parts = listOfNotNull(
                if (w.min != null && w.max != null) "↓${w.min.roundToInt()}°  ↑${w.max.roundToInt()}°" else null,
                w.rainChance?.takeIf { it > 0 }?.let { stringResource(R.string.board_rain, it) },
            )
            if (parts.isNotEmpty()) Text(parts.joinToString("  ·  "), color = Dim, fontSize = 14.sp)
        }
    }
}

/** WMO hava kodu → simge + kısa açıklama. */
private fun weatherLook(code: Int, day: Boolean): Pair<ImageVector, Int> = when (code) {
    0 -> (if (day) Icons.Rounded.WbSunny else Icons.Rounded.NightsStay) to R.string.weather_clear
    1, 2 -> (if (day) Icons.Rounded.WbCloudy else Icons.Rounded.NightsStay) to R.string.weather_partly
    3 -> Icons.Rounded.Cloud to R.string.weather_cloudy
    45, 48 -> Icons.Rounded.Cloud to R.string.weather_fog
    51, 53, 55, 56, 57 -> Icons.Rounded.Grain to R.string.weather_drizzle
    61, 63, 65, 66, 67 -> Icons.Rounded.Umbrella to R.string.weather_rain
    71, 73, 75, 77, 85, 86 -> Icons.Rounded.AcUnit to R.string.weather_snow
    80, 81, 82 -> Icons.Rounded.Umbrella to R.string.weather_showers
    95, 96, 99 -> Icons.Rounded.Thunderstorm to R.string.weather_storm
    else -> Icons.Rounded.Cloud to R.string.weather_cloudy
}

/** Kritik durum: alarm > bağlantı sorunu > çevrimdışı cihaz > her şey yolunda. */
@Composable
private fun StatusPill(alarm: List<String>, conn: ConnState, offline: List<String>, watched: Int, away: Boolean) {
    val (color, icon, title, detail) = when {
        alarm.isNotEmpty() -> Quad(Red, Icons.Rounded.NotificationsActive, stringResource(R.string.board_alarm), alarm.last())
        conn == ConnState.NO_INTERNET -> Quad(Amber, Icons.Rounded.CloudOff, stringResource(R.string.board_no_internet), null)
        conn == ConnState.AUTH_ERROR -> Quad(Amber, Icons.Rounded.WarningAmber, stringResource(R.string.board_auth), null)
        conn == ConnState.ERROR -> Quad(Amber, Icons.Rounded.WarningAmber, stringResource(R.string.board_conn_error), null)
        conn == ConnState.STOPPED -> Quad(Amber, Icons.Rounded.WarningAmber, stringResource(R.string.board_stopped), null)
        offline.isNotEmpty() -> Quad(Amber, Icons.Rounded.CloudOff, stringResource(R.string.board_offline, offline.size), offline.joinToString(", "))
        else -> Quad(Teal, Icons.Rounded.CheckCircle, stringResource(R.string.board_all_ok), stringResource(R.string.board_watching, watched))
    }
    Row(
        Modifier.clip(RoundedCornerShape(20.dp)).background(color.copy(alpha = 0.14f)).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(28.dp))
        Column(Modifier.weight(1f, fill = false)) {
            Text(title, color = color, fontSize = 18.sp, fontWeight = FontWeight.Medium)
            if (detail != null) Text(detail, color = Dim, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Row(Modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(if (away) Icons.AutoMirrored.Rounded.DirectionsWalk else Icons.Rounded.Home, null, tint = Dim, modifier = Modifier.size(16.dp))
                Text(stringResource(if (away) R.string.board_away else R.string.board_home), color = Dim, fontSize = 14.sp)
            }
        }
    }
}

private data class Quad(val color: Color, val icon: ImageVector, val title: String, val detail: String?)

/** Yarım daire güç göstergesi + son 6 saatin eğrisi. */
@Composable
private fun PowerGauge(watts: Double, series: List<Double>, todayKwh: Double, width: Dp) {
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
                if (frac > 0f) {
                    drawArc(arcColor, 180f, 180f * frac, false, topLeft, arc, style = Stroke(stroke, cap = StrokeCap.Round))
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(boardWatts(watts), color = Ink, fontSize = 34.sp, fontWeight = FontWeight.Light)
                Text(stringResource(R.string.board_power_now, boardWatts(max)), color = Dim, fontSize = 12.sp)
            }
        }
        if (series.size >= 2 && series.any { it > 0 }) {
            Spacer(Modifier.height(10.dp))
            Canvas(Modifier.fillMaxWidth().height(40.dp)) {
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
                Text(stringResource(R.string.board_last_6h), color = Dim, fontSize = 12.sp)
                Text(stringResource(R.string.board_today_kwh, String.format(LocalConfiguration.current.locales[0], "%.1f", todayKwh)), color = Dim, fontSize = 12.sp)
            }
        }
    }
}

private fun boardWatts(w: Double): String =
    if (w >= 10_000) String.format(Locale.getDefault(), "%.1f kW", w / 1000) else "${w.roundToInt()} W"

@Composable
private fun BoardTitle(text: String) {
    Text(text.uppercase(LocalConfiguration.current.locales[0]), color = Dim, fontSize = 12.sp, letterSpacing = 1.5.sp)
}

@Composable
private fun Recent(list: List<LoggedEvent>, lines: Int) {
    val context = LocalContext.current
    val strings = remember(context) { EventLog.strings(context) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        BoardTitle(stringResource(R.string.board_recent))
        if (list.isEmpty()) Text(stringResource(R.string.board_no_recent), color = Dim, fontSize = 15.sp)
        for (e in list) {
            val alarm = e.action == AlertAction.ALARM.name
            BoardRow(
                icon = e.icon(), tint = if (alarm) Red else Dim,
                text = e.text(strings),
                time = eventTime(e.time),
                lines = lines,
            )
        }
    }
}

/** Bugünse yalnız saat, dünse "dün 23:42", daha eskiyse kısa tarih. */
@Composable
private fun eventTime(t: Long): String {
    val clock = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(t))
    return when {
        android.text.format.DateUtils.isToday(t) -> clock
        android.text.format.DateUtils.isToday(t + android.text.format.DateUtils.DAY_IN_MILLIS) ->
            stringResource(R.string.yesterday).lowercase(LocalConfiguration.current.locales[0]) + " " + clock
        else -> DateFormat.getDateInstance(DateFormat.SHORT).format(Date(t))
    }
}

@Composable
private fun Upcoming(list: List<StandbyBoard.Upcoming>, now: Long) {
    val context = LocalContext.current
    val durations = remember(context) { EventLog.durations(context) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        BoardTitle(stringResource(R.string.board_upcoming))
        if (list.isEmpty()) Text(stringResource(R.string.board_no_upcoming), color = Dim, fontSize = 15.sp)
        for (u in list) {
            val (icon, kind) = when (u.kind) {
                UpcomingKind.SUNRISE -> Icons.Rounded.WbTwilight to R.string.board_kind_sunrise
                UpcomingKind.SUNSET -> Icons.Rounded.WbTwilight to R.string.board_kind_sunset
                UpcomingKind.CYCLE_START -> Icons.Rounded.Autorenew to R.string.board_kind_cycle_start
                UpcomingKind.CYCLE_STEP -> Icons.Rounded.Autorenew to R.string.board_kind_cycle_step
                UpcomingKind.CYCLE_END -> Icons.Rounded.Autorenew to R.string.board_kind_cycle_end
                UpcomingKind.TEMPLATE -> Icons.Rounded.Forum to R.string.board_kind_template
            }
            // Dakikaya yuvarlanır: saniye saniye değişen yazı gözü yormasın.
            val left = ((u.time - now + 59_999) / 60_000) * 60_000
            BoardRow(
                icon = icon, tint = Teal,
                text = "${u.name} · ${stringResource(kind)}",
                time = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(u.time)),
                sub = stringResource(R.string.board_in, DurationText.format(left, durations)),
            )
        }
    }
}

@Composable
private fun BoardRow(icon: ImageVector, tint: Color, text: String, time: String, sub: String? = null, lines: Int = 1) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(34.dp).clip(CircleShape).background(tint.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(text, color = Ink, fontSize = 16.sp, maxLines = lines, overflow = TextOverflow.Ellipsis)
            if (sub != null) Text(sub, color = Dim, fontSize = 13.sp)
        }
        Text(time, color = Dim, fontSize = 15.sp)
    }
}

