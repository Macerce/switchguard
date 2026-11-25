package com.macerce.switchguard.ui

import com.macerce.switchguard.core.DeviceKind
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.material.icons.rounded.Close
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.zIndex
import com.macerce.switchguard.core.StandbyGrid
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
import androidx.compose.foundation.border
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import com.macerce.switchguard.core.DeviceKinds
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
import androidx.compose.foundation.gestures.scrollBy
import com.macerce.switchguard.core.HomeLauncher
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
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.NotificationsActive
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
import com.macerce.switchguard.core.ActionKind
import com.macerce.switchguard.core.Automation
import com.macerce.switchguard.core.SensorKind
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.Bolt
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
        // TV: yeni pano (kumandayla OK + onay ile aç/kapat); Geri bu ekranı kapatır, TV kaldığı ekrana döner.
        if (FormFactor.isTv(this)) {
            setContent { SwitchGuardTheme { TvStandby() } }
            return
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

// Home Assistant koyu tema: #111111 zemin, #1C1C1C kart, %12 ayırıcı kenar, 12 dp köşe; durum renkleri HA'nınkiler.
private val Bg = Color(0xFF111111)
private val Card = Color(0xFF1C1C1C)
private val Ink = Color(0xFFE1E1E1)
private val Dim = Color(0xFF9B9B9B)
private val Faint = Color(0xFF2C2C2C)
private val Edge = Color(0x1FE1E1E1)
private val Teal = Color(0xFF03A9F4)
private val Ok = Color(0xFF43A047)
private val Amber = Color(0xFFFFA600)
private val Red = Color(0xFFDB4437)
private val Inactive = Color(0xFF6F6F6F)
private val StateIcon = Color(0xFF44739E)
private val Sheet = Color(0xF5262626)
private val CardShape = RoundedCornerShape(12.dp)

/** Açık cihazın HA durum rengi: ışık/priz amber, su ve soğutma mavi, hava camgöbeği, ısıtma turuncu, kapı mor. */
private fun onColor(kind: DeviceKind): Color = when (kind) {
    DeviceKind.WATER, DeviceKind.COLD -> Color(0xFF2196F3)
    DeviceKind.AIR -> Color(0xFF00BCD4)
    DeviceKind.HEAT -> Color(0xFFFF9800)
    DeviceKind.DOOR -> Color(0xFF9C27B0)
    else -> Color(0xFFFFC107)
}

/** HA kartı (ha-card): düz kart zemini, ince ayırıcı kenar. */
private fun Modifier.glass(): Modifier = clip(CardShape).background(Card, CardShape).border(1.dp, Edge, CardShape)

private data class PowerView(val series: List<Double>, val todayKwh: Double)

/** Panonun etkinliğe bağlı işleri: konuşma tanıma penceresi ve kilit açtırma. */
class StandbyHost(val listen: () -> Unit, val unlocked: (() -> Unit) -> Unit)

/** Sütun (~40 dp; telefon dikeyde 8 sütun, cihaz kutusu yarım genişlik) ve HA bölüm ızgarasındaki gibi 56 dp satır, 8 dp aralık. */
private val CellTarget = 38.dp
private val RowH = 56.dp
private val Gap = 8.dp

private fun channelsOf(d: DeviceSnapshot): List<Int> =
    if (d.isMultiChannel) d.switches.keys.sorted() else listOf(d.switches.keys.minOrNull() ?: 0)

/**
 * Bekleme panosu: telefon ana ekranı gibi widget ızgarası (saat, hava, durum, güç, son hareketler, yakında, kurallar,
 * her cihaz kanalı ve her sensör ayrı widget). Basılı tutup sürükleyerek taşınır, köşe tutamağıyla boyutlanır, × ile
 * kaldırılır, "Ekle" ile geri gelir; düzen [Store.standbyGrid]'de saklanır.
 * [onOpenApp]/[onClose] null ise (ekran koruyucu) menü ve düzenleme yoktur; sistem dokunuşla kapatır.
 * [host] null ise (ekran koruyucu) cihazlar yalnız gösterilir, düğmeler çıkmaz. [heard] son sesli komut.
 */
@Composable
fun StandbyBoardScreen(onOpenApp: (() -> Unit)?, onClose: (() -> Unit)?, host: StandbyHost? = null, heard: Pair<Long, String>? = null) {
    // Home Assistant'ın yazı tipi Roboto; bazı telefonlarda (MIUI) sistem yazısı farklı olduğundan uygulamayla gelir.
    CompositionLocalProvider(androidx.compose.material3.LocalTextStyle provides androidx.compose.material3.LocalTextStyle.current.copy(fontFamily = HaFont)) {
        StandbyBoard(onOpenApp, onClose, host, heard)
    }
}

@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private val HaFont = androidx.compose.ui.text.font.FontFamily(
    listOf(400, 500, 600).map { w ->
        androidx.compose.ui.text.font.Font(
            R.font.roboto, FontWeight(w),
            variationSettings = androidx.compose.ui.text.font.FontVariation.Settings(androidx.compose.ui.text.font.FontVariation.weight(w)),
        )
    },
)

@Composable
private fun StandbyBoard(onOpenApp: (() -> Unit)?, onClose: (() -> Unit)?, host: StandbyHost?, heard: Pair<Long, String>?) {
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
    // Yalnız sensörler aç/kapa widget'ı değil, kendi sensör widget'ında değerleriyle gösterilir.
    val tiles = remember(version) { store.snapshotsWithGroups.filterNot { it.sensorOnly } }
    val sensors = remember(version) { snapshots.filter { it.hasSensors } }
    // Açık kurallar (otomasyon + koşullu uyarı); sensör hızlı uyarıları da dahil.
    val rules = remember(version) { store.automations.filter { it.enabled } }
    val ruleDevices = remember(version) { store.snapshotsWithGroups }
    val monitored = remember(version) { store.monitoredIds() }
    val alarmLines = remember(version) { store.pendingAlarm }
    val away = remember(version, minute) { store.isAway() }
    val lat = remember(version) { store.latitude }
    val lon = remember(version) { store.longitude }
    val dimAtNight = remember(version) { store.standbyDim }

    val recent by produceState(emptyList<LoggedEvent>(), eventsVersion) {
        value = withContext(Dispatchers.IO) { events.recent(limit = 40) }.filter { it.kind != "MONITOR_GAP" }.take(12)
    }
    val upcoming = remember(version, minute) {
        StandbyBoard.upcoming(
            store.automations, store.cycles, store.cycleRuns, store.chatTemplates.orEmpty(),
            RuntimeState.get(context).templateRuns, lat, lon, System.currentTimeMillis(),
        ).take(10)
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

    // Widget düzeni: kayıtlı sıra/boyut + kaldırılanlar. Yeni cihaz kendiliğinden sona eklenir.
    var grid by remember { mutableStateOf(StandbyGrid.parse(store.standbyGrid)) }
    var editing by remember { mutableStateOf(false) }
    var adding by remember { mutableStateOf(false) }
    // Düzenlemede "⋮" ile açılan kart menüsü (boyut + kaldır).
    var cardMenu by remember { mutableStateOf<String?>(null) }
    // Son yerleşim ("Ekle" mevcutları ekrandaki yerlerinde sabitler).
    val lastPlaced = remember { object { var value: List<StandbyGrid.Placed> = emptyList() } }
    val lastCols = remember { object { var value = 8 } }
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
    val canEdit = onOpenApp != null

    // Zemindeki ışık durumu söyler (TV'deki gibi): turkuaz yolunda, amber dikkat, kırmızı alarm/sorun, gri izleme kapalı.
    val offlineNames = snapshots.filter { it.id in monitored && !it.online }.map { it.name }
    val accent by animateColorAsState(
        when {
            alarmLines.isNotEmpty() || conn.state in setOf(ConnState.NO_INTERNET, ConnState.AUTH_ERROR, ConnState.ERROR) -> Red
            conn.state == ConnState.STOPPED -> Tv.idle
            offlineNames.isNotEmpty() || conn.state != ConnState.LIVE -> Amber
            else -> Ok
        },
        tween(900),
        label = "accent",
    )
    val onToggle: ((DeviceSnapshot, Int, Boolean) -> Unit)? = host?.let { h ->
        { d, ch, on ->
            if (!editing) h.unlocked {
                scope.launch {
                    val r = Actions.setSwitch(context, d, ch, on)
                    if (r.isFailure) panel = "⚠️ ${d.name}: ${r.exceptionOrNull()?.message.orEmpty()}"
                }
            }
        }
    }

    Box(
        Modifier.fillMaxSize().background(Bg)
            .then(
                if (canEdit) Modifier.clickable(remember { MutableInteractionSource() }, indication = null) {
                    if (editing) adding = false else if (panel != null) { panel = null; Speaker.stop() } else menu = !menu
                }
                else Modifier
            ),
    ) {
        BoxWithConstraints(
            Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 14.dp)
                .then(if (editing) Modifier else Modifier.offset(sx.dp, sy.dp).alpha(if (night) 0.55f else 1f)),
        ) {
            val cols = maxOf(8, ((maxWidth + Gap) / (CellTarget + Gap)).toInt())
            val cell = (maxWidth - Gap * (cols - 1)) / cols
            val placed = remember(shown, cols) { StandbyGrid.pack(shown, cols) }
            lastPlaced.value = placed
            lastCols.value = cols
            val scroll = rememberScrollState()
            val viewport = with(LocalDensity.current) { maxHeight.toPx() }
            val topPad = with(LocalDensity.current) { (if (editing) 64.dp else 0.dp).toPx() }
            Column(Modifier.fillMaxSize().verticalScroll(scroll)) {
                if (editing) Spacer(Modifier.height(64.dp))
                WidgetGrid(
                    placed, cols, cell, editing, canEdit, scroll, viewport, topPad,
                    onStartEdit = { menu = false; panel = null; Speaker.stop(); editing = true },
                    onPlace = { id, x, y, w, h -> StandbyGrid.place(placed, id, x, y, w, h, cols)?.let { save(it) } },
                    onMove = { id, x, y -> StandbyGrid.move(placed, id, x, y, cols)?.let { save(it) } },
                    onMenu = { id -> adding = false; cardMenu = id },
                ) { id, w, h ->
                    when (id) {
                        StandbyGrid.CLOCK -> ClockWidget(now, w, h)
                        StandbyGrid.WEATHER -> WeatherWidget(weather, lat != null, w, h)
                        StandbyGrid.STATUS -> StatusWidget(alarmLines, conn.state, offlineNames, monitored.size, away, w, h)
                        StandbyGrid.POWER -> PowerWidget(livePower, power, w, h)
                        StandbyGrid.RECENT -> Recent(recent.take(rowsFor(h)), Modifier.fillMaxSize())
                        StandbyGrid.UPCOMING -> Upcoming(upcoming.take(rowsFor(h)), now, Modifier.fillMaxSize())
                        StandbyGrid.RULES -> Rules(rules, ruleDevices, rowsFor(h), Modifier.fillMaxSize())
                        else -> {
                            StandbyGrid.parseDevice(id)?.let { (did, ch) ->
                                tiles.firstOrNull { it.id == did }?.let { d -> DeviceWidget(d, ch, accent, w, h, editing, onToggle) }
                            }
                            StandbyGrid.parseSensor(id)?.let { sid -> sensors.firstOrNull { it.id == sid }?.let { SensorWidget(it, w, h) } }
                        }
                    }
                }
                Spacer(Modifier.height(if (host != null || editing) 84.dp else 0.dp))
            }
        }
        if (editing) {
            val addable = available.filter { it in grid.removed }
            Row(
                Modifier.align(Alignment.TopCenter).padding(horizontal = 12.dp, vertical = 8.dp).widthIn(max = 720.dp).fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp)).background(Sheet)
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp)).padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.board_edit_hint), color = Dim, fontSize = 12.sp, lineHeight = 15.sp, maxLines = 3,
                    modifier = Modifier.padding(horizontal = 8.dp).weight(1f),
                )
                if (addable.isNotEmpty()) TextButton(onClick = { adding = !adding }) {
                    Icon(Icons.Rounded.Add, null, tint = Teal, modifier = Modifier.size(18.dp))
                    Text(stringResource(R.string.board_add), color = Teal)
                }
                TextButton(onClick = { grid = StandbyGrid.State(emptyList(), emptySet()); store.standbyGrid = null; adding = false }) {
                    Text(stringResource(R.string.board_reset), color = Ink)
                }
                TextButton(onClick = { editing = false; adding = false; cardMenu = null }) { Text(stringResource(R.string.action_done), color = Teal) }
            }
            AnimatedVisibility(adding && addable.isNotEmpty(), Modifier.align(Alignment.TopCenter).padding(top = 72.dp), enter = fadeIn(), exit = fadeOut()) {
                Column(
                    Modifier.padding(horizontal = 16.dp).widthIn(max = 420.dp).heightIn(max = 420.dp).clip(RoundedCornerShape(22.dp)).background(Sheet)
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(22.dp))
                        .verticalScroll(rememberScrollState()).padding(8.dp),
                ) {
                    for (id in addable) {
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                                .clickable { save(lastPlaced.value.let { StandbyGrid.freeze(it) } + StandbyGrid.item(id), grid.removed - id); if (addable.size == 1) adding = false }
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(Icons.Rounded.Add, stringResource(R.string.board_show), tint = Teal, modifier = Modifier.size(20.dp))
                            Text(widgetName(id, ruleDevices), color = Ink, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
        val menuItem = cardMenu?.let { id -> lastPlaced.value.firstOrNull { it.item.id == id } }
        if (editing && menuItem != null) {
            val id = menuItem.item.id
            val cols = lastCols.value
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f))
                    .clickable(remember { MutableInteractionSource() }, indication = null) { cardMenu = null },
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    Modifier.padding(24.dp).widthIn(max = 420.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Card)
                        .border(1.dp, Edge, RoundedCornerShape(16.dp))
                        .clickable(remember { MutableInteractionSource() }, indication = null) {}
                        .padding(16.dp),
                ) {
                    Text(widgetName(id, ruleDevices), color = Ink, fontSize = 20.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.board_size) + " · ${menuItem.w} × ${menuItem.h}", color = Dim, fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))
                    val (mw, mh) = StandbyGrid.minSize(id)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (y in 0 until 6) Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            for (x in 0 until cols) {
                                val inside = x < menuItem.w && y < menuItem.h
                                val allowed = x + 1 >= mw && y + 1 >= mh
                                Box(
                                    Modifier.weight(1f).height(28.dp).clip(RoundedCornerShape(6.dp))
                                        .background(if (inside) Teal.copy(alpha = 0.55f) else Color.White.copy(alpha = if (allowed) 0.08f else 0.03f))
                                        .clickable {
                                            val (w, h) = StandbyGrid.clampSize(id, x + 1, y + 1, cols)
                                            StandbyGrid.resize(lastPlaced.value, id, w, h, cols)?.let { save(it) }
                                        },
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = {
                            save(StandbyGrid.freeze(lastPlaced.value).filter { it.id != id }, grid.removed + id)
                            cardMenu = null
                        }) { Text(stringResource(R.string.board_hide), color = Red) }
                        TextButton(onClick = { cardMenu = null }) { Text(stringResource(R.string.action_done), color = Teal) }
                    }
                }
            }
        }
        if (host != null && !editing) {
            Row(
                Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp).offset(sx.dp, sy.dp).alpha(if (night) 0.55f else 1f)
                    .clip(CircleShape).background(Sheet).border(1.dp, Edge, CircleShape).padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
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
                    Modifier.widthIn(max = 560.dp).heightIn(max = 380.dp).clip(RoundedCornerShape(22.dp)).background(Sheet)
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(22.dp))
                        .clickable { panel = null; Speaker.stop() }
                        .verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 14.dp),
                ) {
                    Text(panel ?: stringResource(R.string.chat_typing), color = Ink, fontSize = 16.sp, lineHeight = 23.sp)
                }
            }
        }
        AnimatedVisibility(
            menu && panel == null && !working && !editing,
            Modifier.align(Alignment.BottomCenter).padding(bottom = if (host != null) 88.dp else 24.dp), enter = fadeIn(), exit = fadeOut(),
        ) {
            Row(
                Modifier.clip(RoundedCornerShape(28.dp)).background(Sheet).border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(28.dp)).padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                onOpenApp?.let { TextButton(onClick = it) { Text(stringResource(R.string.board_open_app), color = Teal) } }
                // SwitchGuard ana ekransa telefonun kendi ana ekranına geçiş (ana ekran tuşu yine buraya döner).
                if (canEdit && remember { HomeLauncher.isDefault(context) }) TextButton(onClick = { menu = false; HomeLauncher.returnToSystemHome(context) }) {
                    Text(stringResource(R.string.board_system_home), color = Ink)
                }
                if (canEdit) TextButton(onClick = { menu = false; panel = null; Speaker.stop(); editing = true }) {
                    Text(stringResource(R.string.board_edit), color = Ink)
                }
                onClose?.let { TextButton(onClick = it) { Text(stringResource(R.string.board_close), color = Ink) } }
            }
        }
    }
}

/** Liste widget'ında yüksekliğe sığan satır sayısı. */
private fun rowsFor(h: Dp): Int = maxOf(1, ((h - 56.dp) / 48.dp).toInt())

/**
 * Widget ızgarası. Basılı tutunca düzenleme açılır ve widget sürüklenir; diğer widget'lar yerinden oynamaz. Bırakılacak
 * yer çerçeveyle gösterilir (boşsa yeşil, doluysa kırmızı); dolu yere bırakılan widget eski yerine döner.
 * Düzenlemede sol üstte × (kaldır), sağ altta boyut tutamağı (yalnız boş alana doğru büyür).
 */
@Composable
private fun WidgetGrid(
    placed: List<StandbyGrid.Placed>,
    cols: Int,
    cell: Dp,
    editing: Boolean,
    canEdit: Boolean,
    scroll: androidx.compose.foundation.ScrollState,
    viewport: Float,
    topPad: Float,
    onStartEdit: () -> Unit,
    onPlace: (String, Int, Int, Int, Int) -> Unit,
    onMove: (String, Int, Int) -> Unit,
    onMenu: (String) -> Unit,
    body: @Composable (String, Dp, Dp) -> Unit,
) {
    val density = LocalDensity.current
    val stepX = with(density) { (cell + Gap).toPx() }
    val stepY = with(density) { (RowH + Gap).toPx() }
    val gapPx = with(density) { Gap.toPx() }
    val latest = rememberUpdatedState(placed)
    val place = rememberUpdatedState(onPlace)
    val move = rememberUpdatedState(onMove)
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
            val (w, h) = StandbyGrid.clampSize(id, ((s.second + gapPx) / stepX).roundToInt(), ((s.third + gapPx) / stepY).roundToInt(), cols)
            intArrayOf(me.x, me.y, w, h)
        } else if (dragId == id) {
            intArrayOf(
                (dragPos.x / stepX).roundToInt().coerceIn(0, maxOf(0, cols - me.w)),
                (dragPos.y / stepY).roundToInt().coerceAtLeast(0), me.w, me.h,
            )
        } else null
    }
    // Sürüklenen widget ekranın üst/alt kenarına gelince sayfa kendiliğinden kayar (uzak bir yere taşınabilsin).
    LaunchedEffect(dragId) {
        val id = dragId ?: return@LaunchedEffect
        val edge = with(density) { 72.dp.toPx() }
        val speed = with(density) { 12.dp.toPx() }
        while (dragId == id) {
            val me = latest.value.firstOrNull { it.item.id == id }
            val hPx = (me?.h ?: 1) * stepY - gapPx
            val top = dragPos.y + topPad - scroll.value
            val want = when {
                top < edge -> -speed
                top + hPx > viewport - edge -> speed
                else -> 0f
            }
            if (want != 0f) {
                val used = scroll.scrollBy(want)
                if (used != 0f) dragPos = Offset(dragPos.x, (dragPos.y + used).coerceAtLeast(0f))
            }
            delay(16)
        }
    }
    val active = dragId ?: sizing?.first
    val ghost = active?.let { id -> target(id)?.let { id to it } }
    val rows = maxOf(
        placed.maxOfOrNull { it.y + it.h } ?: 0,
        ghost?.second?.let { it[1] + it[3] } ?: 0,
    ) + if (editing) 2 else 0
    fun span(n: Int) = cell * n + Gap * (n - 1)
    fun spanY(n: Int) = RowH * n + Gap * (n - 1)
    Box(Modifier.fillMaxWidth().height(if (rows == 0) 0.dp else spanY(rows))) {
        ghost?.let { (id, t) ->
            // Taşırken dolu yer de olur (yer değiştirir); boyutlarken yalnız boş alan.
            val ok = if (dragId == id) true else StandbyGrid.canPlace(latest.value, id, t[0], t[1], t[2], t[3], cols)
            Box(
                Modifier.offset { IntOffset((t[0] * stepX).roundToInt(), (t[1] * stepY).roundToInt()) }.size(span(t[2]), spanY(t[3]))
                    .clip(CardShape).background((if (ok) Teal else Red).copy(alpha = 0.10f))
                    .border(1.5.dp, (if (ok) Teal else Red).copy(alpha = 0.6f), CardShape),
            )
        }
        for (p in placed) key(p.item.id) {
            val id = p.item.id
            val dragging = dragId == id
            val live = sizing?.takeIf { it.first == id }
            val w = if (live != null) with(density) { live.second.toDp() } else span(p.w)
            val h = if (live != null) with(density) { live.third.toDp() } else spanY(p.h)
            Box(
                Modifier
                    .offset { if (dragging) IntOffset(dragPos.x.roundToInt(), dragPos.y.roundToInt()) else IntOffset((p.x * stepX).roundToInt(), (p.y * stepY).roundToInt()) }
                    .size(w, h)
                    .zIndex(if (dragging || live != null) 1f else 0f)
                    .graphicsLayer {
                        val s = if (dragging) 1.04f else 1f
                        scaleX = s; scaleY = s
                        alpha = if (dragging) 0.85f else 1f
                    }
                    .then(
                        if (!canEdit) Modifier
                        else Modifier.pointerInput(id) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    startEdit.value()
                                    val me = latest.value.firstOrNull { it.item.id == id }
                                    if (me != null) { dragPos = Offset(me.x * stepX, me.y * stepY); dragId = id }
                                },
                                onDrag = { change, amount ->
                                    change.consume()
                                    if (dragId == id) dragPos += amount
                                },
                                onDragEnd = {
                                    target(id)?.let { t -> move.value(id, t[0], t[1]) }
                                    dragId = null
                                },
                                onDragCancel = { dragId = null },
                            )
                        },
                    ),
            ) {
                body(id, w, h)
                if (editing) {
                    Box(Modifier.matchParentSize().border(1.dp, Teal.copy(alpha = if (dragging) 0.7f else 0.35f), CardShape))
                    EditKey(Icons.Rounded.MoreVert, stringResource(R.string.board_edit), Modifier.align(Alignment.TopEnd)) { onMenu(id) }
                }
            }
        }
    }
}

@Composable
private fun EditKey(icon: ImageVector, label: String?, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.padding(4.dp).size(30.dp).clip(CircleShape).background(Color(0xF02A2D35))
            .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, label, tint = Ink, modifier = Modifier.size(18.dp)) }
}

@Composable
private fun widgetName(id: String, devices: List<DeviceSnapshot>): String = when (id) {
    StandbyGrid.CLOCK -> stringResource(R.string.board_sec_clock)
    StandbyGrid.WEATHER -> stringResource(R.string.board_sec_weather)
    StandbyGrid.STATUS -> stringResource(R.string.board_sec_status)
    StandbyGrid.POWER -> stringResource(R.string.board_sec_power)
    StandbyGrid.RECENT -> stringResource(R.string.board_recent)
    StandbyGrid.UPCOMING -> stringResource(R.string.board_upcoming)
    StandbyGrid.RULES -> stringResource(R.string.automations_rules_title)
    else -> StandbyGrid.parseDevice(id)?.let { (did, ch) ->
        devices.firstOrNull { it.id == did }?.let { if (it.isMultiChannel) "${it.name} · ${ch + 1}" else it.name }
    } ?: StandbyGrid.parseSensor(id)?.let { sid -> devices.firstOrNull { it.id == sid }?.name } ?: id
}

/** dp cinsinden boyutu yazı boyutuna çevirir (yazı ölçeği widget'ı taşırmasın diye yok sayılır). */
@Composable
private fun dpSp(v: Float): TextUnit = with(LocalDensity.current) { v.dp.toSp() }

/** Alttaki yuvarlak düğme (sesli komut, durumu oku, günün özeti). */
@Composable
private fun BoardAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(48.dp).clip(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, label, tint = Ink.copy(alpha = 0.75f), modifier = Modifier.size(22.dp)) }
}

/**
 * Home Assistant "tile" kartı: 36 dp yuvarlak simge (renginin %20'si zeminde), 14 sp kalın ad, altında 12 sp durum.
 * Geniş/kısa kutuda yatay, uzun kutuda dikey (ortalı) düzen. [color] null ise etkin değil (gri).
 */
@Composable
private fun HaTile(icon: ImageVector, color: Color?, name: String, state: String, w: Dp, h: Dp, onClick: (() -> Unit)? = null) {
    val tint by animateColorAsState(color ?: Inactive, tween(250), label = "tileIcon")
    val iconBox: @Composable () -> Unit = {
        Box(Modifier.size(36.dp).background(tint.copy(alpha = 0.2f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(24.dp))
        }
    }
    val vertical = h >= 112.dp && w < h * 1.5f
    val align = if (vertical) TextAlign.Center else TextAlign.Start
    val texts: @Composable (Modifier) -> Unit = { m ->
        Column(m) {
            Text(name, color = Ink, fontSize = dpSp(14f), fontWeight = FontWeight.Medium, lineHeight = dpSp(20f), letterSpacing = 0.1.sp,
                maxLines = if (vertical) 2 else 1, overflow = TextOverflow.Ellipsis, textAlign = align, modifier = Modifier.fillMaxWidth())
            Text(state, color = Dim, fontSize = dpSp(12f), lineHeight = dpSp(16f), letterSpacing = 0.4.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = align, modifier = Modifier.fillMaxWidth())
        }
    }
    Box(
        Modifier.fillMaxSize().glass().then(onClick?.let { Modifier.clickable(onClick = it) } ?: Modifier).padding(10.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (vertical) Column(horizontalAlignment = Alignment.CenterHorizontally) {
            iconBox()
            Spacer(Modifier.height(10.dp))
            texts(Modifier)
        } else Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            iconBox()
            Spacer(Modifier.width(10.dp))
            texts(Modifier.weight(1f))
        }
    }
}

/** Cihaz kanalı: HA anahtar kutusu; dokununca açılır/kapanır, açıkken türünün renginde, watt durumun yanında. */
@Composable
private fun DeviceWidget(d: DeviceSnapshot, ch: Int, @Suppress("UNUSED_PARAMETER") accent: Color, w: Dp, h: Dp, editing: Boolean, onToggle: ((DeviceSnapshot, Int, Boolean) -> Unit)?) {
    val on = d.online && d.switches[ch] == true
    val kind = remember(d.name, d.isMultiChannel, d.hasEnergy, d.cloud, d.sensorOnly) { DeviceKinds.of(d) }
    val name = if (d.isMultiChannel) "${d.name} · ${ch + 1}" else d.name
    val watts = d.power?.takeIf { on && it >= 1 }?.let { " · " + boardWatts(it) }.orEmpty()
    val state = stringResource(if (!d.online) R.string.label_offline_title else if (on) R.string.state_on_title else R.string.state_off_title) + watts
    HaTile(
        if (d.online) kind.icon() else Icons.Rounded.CloudOff, if (on) onColor(kind) else null, name, state, w, h,
        onClick = if (onToggle != null && d.online && !editing) ({ onToggle(d, ch, !on) }) else null,
    )
}

/** Oda sensörü: HA sensör kutusu; durumda sıcaklık/nem, kapı/hareket/su; düşük pil (%20 altı) sonda. */
@Composable
private fun SensorWidget(d: DeviceSnapshot, w: Dp, h: Dp) {
    val res = LocalContext.current.resources
    val s = d.sensors
    val alert = d.online && (d.sensorActive(SensorKind.WATER) == true || d.sensorActive(SensorKind.DOOR) == true)
    val busy = d.online && d.sensorActive(SensorKind.MOTION) == true
    val parts = buildList {
        s[SensorKind.TEMPERATURE]?.let { add(formatNumber(it) + " °C") }
        s[SensorKind.HUMIDITY]?.let { add(formatNumber(it.roundToInt().toDouble()) + " %") }
        for (k in listOf(SensorKind.DOOR, SensorKind.MOTION, SensorKind.WATER)) s[k]?.let { add(res.getString(k.stateLabel(it >= 0.5))) }
        s[SensorKind.BATTERY]?.takeIf { it < 20 }?.let { add("🔋 " + it.roundToInt() + " %") }
    }
    val state = if (!d.online) stringResource(R.string.label_offline_title) else parts.joinToString(" · ")
    val icon = s.keys.firstOrNull { it != SensorKind.BATTERY }?.icon() ?: Icons.Rounded.Sensors
    HaTile(if (d.online) icon else Icons.Rounded.CloudOff, if (!d.online) null else if (alert) Red else if (busy) Amber else Teal, d.name, state, w, h)
}

/** HA kart başlığı (card-header): 24 sp, normal kalınlık. */
@Composable
private fun CardHeader(text: String) {
    Text(
        text, color = Ink, fontSize = dpSp(24f), lineHeight = dpSp(32f), letterSpacing = (-0.3).sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
    )
}

/** HA varlık kartı (entities): başlık ve 40 dp satırlar. */
@Composable
private fun EntitiesCard(title: String, modifier: Modifier, rows: @Composable () -> Unit) {
    Column(modifier.glass()) {
        CardHeader(title)
        Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { rows() }
    }
}

/** Açık kurallar: ad ve "Eğer → O zaman" özeti; uyarılar zil, otomasyonlar şimşek simgesiyle. */
@Composable
private fun Rules(list: List<Automation>, devices: List<DeviceSnapshot>, max: Int, modifier: Modifier) {
    EntitiesCard(stringResource(R.string.automations_rules_title) + " · " + list.size, modifier) {
        for (a in list.take(max)) {
            val (cond, act) = describe(a, devices)
            val alert = a.action.kind == ActionKind.ALARM || a.action.kind == ActionKind.NOTIFY
            BoardRow(
                if (alert) Icons.Rounded.NotificationsActive else Icons.Rounded.Bolt,
                if (a.action.kind == ActionKind.ALARM) Red else if (alert) Amber else StateIcon,
                a.name.ifBlank { cond }, "", sub = "$cond → $act", lines = 1,
            )
        }
    }
}

/** Saat kartı (HA clock kartı): ortada büyük saat, yer varsa altında tarih. */
@Composable
private fun ClockWidget(now: Long, w: Dp, h: Dp) {
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
    val inner = h - 24.dp
    val showDate = inner >= 80.dp
    val size = minOf(inner.value * (if (showDate) 0.6f else 0.8f), w.value * (if (h24) 0.25f else 0.19f))
    Column(
        Modifier.fillMaxSize().glass().padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
    ) {
        Text(time, color = Ink, fontSize = dpSp(size), lineHeight = dpSp(size * 1.1f), maxLines = 1, softWrap = false)
        if (showDate) Text(
            date.replaceFirstChar { it.titlecase(locale) }, color = Dim, fontSize = dpSp((inner.value * 0.14f).coerceIn(14f, 22f)),
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun is24h(): Boolean = android.text.format.DateFormat.is24HourFormat(LocalContext.current)

/** Hava kartı (HA weather-forecast): solda simge, durum ve en düşük/en yüksek; sağda büyük sıcaklık. */
@Composable
private fun WeatherWidget(w: WeatherClient.Today?, hasLocation: Boolean, width: Dp, h: Dp) {
    Row(
        Modifier.fillMaxSize().glass().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (w == null) {
            if (!hasLocation) Text(stringResource(R.string.board_no_location), color = Dim, fontSize = 14.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
            return@Row
        }
        val (icon, label) = weatherLook(w.code, w.isDay)
        val sunny = w.code <= 2 && w.isDay
        Icon(icon, null, tint = if (sunny) Color(0xFFFFC107) else Color(0xFFB0BEC5), modifier = Modifier.size(((h - 16.dp) * 0.8f).coerceIn(32.dp, 96.dp)))
        if (width >= 200.dp) Column(Modifier.weight(1f)) {
            Text(stringResource(label), color = Ink, fontSize = dpSp(16f), lineHeight = dpSp(22f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            val parts = listOfNotNull(
                if (w.min != null && w.max != null) "${w.max.roundToInt()}° / ${w.min.roundToInt()}°" else null,
                w.rainChance?.takeIf { it > 0 }?.let { stringResource(R.string.board_rain, it) },
            )
            if (parts.isNotEmpty()) Text(parts.joinToString(" · "), color = Dim, fontSize = dpSp(14f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        } else Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.Top) {
            val t = (h.value * 0.3f).coerceIn(28f, 64f)
            Text("${w.temp.roundToInt()}", color = Ink, fontSize = dpSp(t), lineHeight = dpSp(t * 1.1f), maxLines = 1)
            Text("°C", color = Dim, fontSize = dpSp(t * 0.5f), maxLines = 1, modifier = Modifier.padding(top = (t * 0.12f).dp))
        }
    }
}

/** WMO hava kodu → simge + kısa açıklama. */
internal fun weatherLook(code: Int, day: Boolean): Pair<ImageVector, Int> = when (code) {
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

/** Kritik durum (HA tile): alarm > bağlantı sorunu > çevrimdışı cihaz > her şey yolunda; durumda ayrıntı ve evde/dışarıda. */
@Composable
private fun StatusWidget(alarm: List<String>, conn: ConnState, offline: List<String>, watched: Int, away: Boolean, w: Dp, h: Dp) {
    val (color, title, detail) = when {
        alarm.isNotEmpty() -> Triple(Red, stringResource(R.string.board_alarm), alarm.last())
        conn == ConnState.NO_INTERNET -> Triple(Amber, stringResource(R.string.board_no_internet), null)
        conn == ConnState.AUTH_ERROR -> Triple(Amber, stringResource(R.string.board_auth), null)
        conn == ConnState.ERROR -> Triple(Amber, stringResource(R.string.board_conn_error), null)
        conn == ConnState.STOPPED -> Triple(Amber, stringResource(R.string.board_stopped), null)
        offline.isNotEmpty() -> Triple(Amber, stringResource(R.string.board_offline, offline.size), offline.joinToString(", "))
        else -> Triple(Ok, stringResource(R.string.board_all_ok), stringResource(R.string.board_watching, watched))
    }
    val sub = listOfNotNull(detail, stringResource(if (away) R.string.board_away else R.string.board_home)).joinToString(" · ")
    HaTile(if (color == Ok) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline, color, title, sub, w, h)
}

/** Güç kartı (HA gauge): yarım daire gösterge; yükseklik yetince son 6 saatin eğrisi. */
@Composable
private fun PowerWidget(watts: Double, p: PowerView?, w: Dp, h: Dp) {
    Box(Modifier.fillMaxSize().glass().padding(16.dp), contentAlignment = Alignment.Center) {
        val iw = w - 32.dp
        val ih = h - 32.dp
        val withChart = minOf(iw, (ih - 72.dp) * 2)
        val chart = p != null && withChart >= 120.dp
        val width = if (chart) withChart else minOf(iw, (ih - 4.dp) * 2)
        PowerGauge(watts, p?.series.orEmpty(), p?.todayKwh ?: 0.0, width, chart)
    }
}

/** Yarım daire güç göstergesi + (isteğe bağlı) son 6 saatin eğrisi. */
@Composable
private fun PowerGauge(watts: Double, series: List<Double>, todayKwh: Double, width: Dp, chart: Boolean) {
    val peak = maxOf(series.maxOrNull() ?: 0.0, watts)
    val max = StandbyBoard.gaugeMax(peak)
    val frac = (watts / max).toFloat().coerceIn(0f, 1f)
    val arcColor = when {
        frac < 0.5f -> Ok
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
                if (frac > 0f) {
                    drawArc(arcColor, 180f, 180f * frac, false, topLeft, arc, style = Stroke(stroke, cap = StrokeCap.Round))
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(boardWatts(watts), color = Ink, fontSize = dpSp((width.value * 0.12f).coerceIn(16f, 40f)), maxLines = 1)
                Text(stringResource(R.string.board_power_now, boardWatts(max)), color = Dim, fontSize = dpSp((width.value * 0.05f).coerceIn(10f, 15f)), maxLines = 1)
            }
        }
        if (chart && series.size >= 2 && series.any { it > 0 }) {
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
                drawPath(fill, Brush.verticalGradient(listOf(Teal.copy(alpha = 0.22f), Color.Transparent)))
                drawPath(line, Teal, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.board_last_6h), color = Dim, fontSize = 12.sp, maxLines = 1)
                Text(stringResource(R.string.board_today_kwh, String.format(LocalConfiguration.current.locales[0], "%.1f", todayKwh)), color = Dim, fontSize = 12.sp, maxLines = 1)
            }
        }
    }
}

private fun boardWatts(w: Double): String =
    if (w >= 10_000) String.format(Locale.getDefault(), "%.1f kW", w / 1000) else "${w.roundToInt()} W"

@Composable
private fun Recent(list: List<LoggedEvent>, modifier: Modifier) {
    val context = LocalContext.current
    val strings = remember(context) { EventLog.strings(context) }
    EntitiesCard(stringResource(R.string.board_recent), modifier) {
        if (list.isEmpty()) Text(stringResource(R.string.board_no_recent), color = Dim, fontSize = 14.sp)
        for (e in list) {
            val alarm = e.action == AlertAction.ALARM.name
            BoardRow(icon = e.icon(), tint = if (alarm) Red else StateIcon, text = e.text(strings), time = eventTime(e.time), lines = 2)
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
private fun Upcoming(list: List<StandbyBoard.Upcoming>, now: Long, modifier: Modifier) {
    val context = LocalContext.current
    val durations = remember(context) { EventLog.durations(context) }
    EntitiesCard(stringResource(R.string.board_upcoming), modifier) {
        if (list.isEmpty()) Text(stringResource(R.string.board_no_upcoming), color = Dim, fontSize = 14.sp)
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
                icon = icon, tint = StateIcon,
                text = "${u.name} · ${stringResource(kind)}",
                time = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(u.time)),
                sub = stringResource(R.string.board_in, DurationText.format(left, durations)),
            )
        }
    }
}

/** HA varlık satırı: 40 dp simge alanı (zeminsiz), 16 dp boşluk, ad; sağda zaman. */
@Composable
private fun BoardRow(icon: ImageVector, tint: Color, text: String, time: String, sub: String? = null, lines: Int = 1) {
    Row(Modifier.heightIn(min = 40.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) { Icon(icon, null, tint = tint, modifier = Modifier.size(24.dp)) }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(text, color = Ink, fontSize = dpSp(14f), lineHeight = dpSp(20f), maxLines = lines, overflow = TextOverflow.Ellipsis)
            if (sub != null) Text(sub, color = Dim, fontSize = dpSp(12f), lineHeight = dpSp(16f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (time.isNotEmpty()) Text(time, color = Dim, fontSize = dpSp(14f), maxLines = 1, modifier = Modifier.padding(start = 8.dp))
    }
}
