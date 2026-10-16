package com.macerce.switchguard.desktop.ui.devices

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.macerce.switchguard.api.ApiException
import com.macerce.switchguard.core.Demo
import com.macerce.switchguard.core.DeviceSnapshot
import com.macerce.switchguard.core.DeviceTimers
import com.macerce.switchguard.core.RepeatTimer
import com.macerce.switchguard.core.TimerEntry
import com.macerce.switchguard.data.MetricsLog
import com.macerce.switchguard.desktop.engine.DailySummary
import com.macerce.switchguard.desktop.engine.MonitorEngine
import com.macerce.switchguard.desktop.i18n.Strings
import com.macerce.switchguard.desktop.platform.Links
import com.macerce.switchguard.desktop.ui.InfoRow
import com.macerce.switchguard.desktop.ui.LocalStrings
import com.macerce.switchguard.desktop.ui.RowDivider
import com.macerce.switchguard.desktop.ui.SectionCard
import com.macerce.switchguard.desktop.ui.SectionTitle
import com.macerce.switchguard.desktop.ui.str
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale
import java.util.UUID

// Telefondaki DeviceExtras.kt ve DemoUi.kt (2.12): enerji, eWeLink zamanlayıcıları, ham veri ve demo simülasyonu.
// Masaüstü farkları: saat seçimi fare/klavyeye uygun (oklarla ya da yazarak), gün adları seçili dilde, Toast yerine
// ekranın Snackbar'ı, pano AWT ile.

// ---------------------------------------------------------------- enerji

/** Cihaz sayfasının enerji verisi: son 7 günün (gün başı, kWh) listesi (en eskiden bugüne), bu ay, kayıt var mı. */
data class EnergyData(val days: List<Pair<Long, Double>>, val month: Double, val hasData: Boolean)

/** [now] anındaki enerji özeti; veritabanı okunduğundan IO iş parçacığında çağrılır. */
fun loadEnergy(metrics: MetricsLog, deviceId: String, now: Long, zone: ZoneId = ZoneId.systemDefault()): EnergyData {
    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    fun start(day: LocalDate) = day.atStartOfDay(zone).toInstant().toEpochMilli()
    val days = (6L downTo 0L).map { back ->
        val day = today.minusDays(back)
        start(day) to metrics.kwh(deviceId, start(day), minOf(start(day.plusDays(1)), now))
    }
    val month = metrics.kwh(deviceId, start(today.withDayOfMonth(1)), now)
    return EnergyData(days, month, metrics.hasPowerData(deviceId))
}

/**
 * Enerji ölçen cihazlarda: anlık değerler, bugün/bu ay tüketim ve son 7 günün grafiği. [requestLive] doluysa (eWeLink)
 * sayfa açıkken cihazdan aralıklarla canlı güç istenir; POW modelleri aksi halde seyrek gönderir.
 */
@Composable
fun EnergySection(device: DeviceSnapshot, metrics: MetricsLog, requestLive: (suspend () -> Unit)?) {
    val strings = LocalStrings.current
    val locale = strings.locale
    val metricsVersion by metrics.version.collectAsState()
    LaunchedEffect(device.id, requestLive != null) {
        if (requestLive == null) return@LaunchedEffect
        while (true) {
            requestLive()
            delay(100_000)
        }
    }
    val data by produceState<EnergyData?>(null, metricsVersion, device.id) {
        value = withContext(Dispatchers.IO) { runCatching { loadEnergy(metrics, device.id, System.currentTimeMillis()) }.getOrNull() }
    }

    SectionTitle(str("section_energy"))
    SectionCard {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Metric(str("energy_power"), device.power?.let { formatWatts(it, locale) } ?: "–", big = true)
            Metric(str("energy_voltage"), device.voltage?.let { String.format(locale, "%.0f V", it) } ?: "–")
            Metric(str("energy_current"), device.current?.let { String.format(locale, "%.2f A", it) } ?: "–")
        }
        if (device.channelPower.isNotEmpty()) {
            RowDivider(inset = 0.dp)
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                device.channelPower.toSortedMap().forEach { (ch, w) ->
                    Metric(str("channel_n", ch + 1), formatWatts(w, locale))
                }
            }
        }
        RowDivider(inset = 0.dp)
        val d = data
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Metric(str("energy_today"), d?.let { DailySummary.formatKwh(it.days.last().second, locale) } ?: "–")
            Metric(str("energy_month"), d?.let { DailySummary.formatKwh(it.month, locale) } ?: "–")
        }
        RowDivider(inset = 0.dp)
        when {
            d == null -> Unit
            !d.hasData -> Text(
                str("energy_no_data"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
            else -> WeekBars(d.days, strings)
        }
    }
    Text(
        str("energy_hint"),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
    )
}

@Composable
private fun Metric(label: String, value: String, big: Boolean = false) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            style = if (big) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (big) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** Son 7 gün: gün başına yatay çubuk + değer (okunaklı, eksen gerektirmez). */
@Composable
private fun WeekBars(days: List<Pair<Long, Double>>, strings: Strings) {
    val max = days.maxOf { it.second }.takeIf { it > 0 } ?: 1.0
    // Telefondaki "EEE d" kalıbı: Türkçede gün önce ("25 Cum"), İngilizcede ad önce ("Fri 25").
    val dayFormat = remember(strings.locale) {
        DateTimeFormatter.ofPattern(if (strings.locale.language == "tr") "d EEE" else "EEE d", strings.locale)
    }
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(str("energy_last_7_days"), style = MaterialTheme.typography.labelLarge)
        days.forEach { (start, kwh) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    dayFormat.format(Instant.ofEpochMilli(start).atZone(ZoneId.systemDefault())),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.width(72.dp),
                )
                Box(Modifier.weight(1f).height(14.dp)) {
                    Box(
                        Modifier.fillMaxHeight()
                            .fillMaxWidth((kwh / max).toFloat().coerceIn(0.01f, 1f))
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp)),
                    )
                }
                Text(
                    DailySummary.formatKwh(kwh, strings.locale),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.width(84.dp).padding(start = 8.dp),
                )
            }
        }
    }
}

// ---------------------------------------------------------------- zamanlayıcılar

/** Cihazın eWeLink zamanlayıcıları: bilgisayar kapalıyken de cihazda çalışır. */
@Composable
fun TimersSection(device: DeviceSnapshot, actions: DeviceActions) {
    val strings = LocalStrings.current
    val ui = LocalDeviceUi.current
    val scope = rememberCoroutineScope()
    var entries by remember { mutableStateOf<List<TimerEntry>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<RepeatTimer?>(null) }

    LaunchedEffect(device.id) {
        actions.loadTimers(device.id)
            .onSuccess { entries = it; error = null }
            .onFailure {
                // Ham API mesajı yerine kısa açıklama: internet yok ya da eWeLink hata kodu.
                error = when (it) {
                    is ApiException -> "eWeLink ${it.code}"
                    else -> strings["state_no_internet"]
                }
            }
    }

    fun save(id: String, timer: RepeatTimer?) {
        busy = true
        scope.launch {
            actions.saveTimer(device, id, timer)
                .onSuccess { entries = it }
                .onFailure { ui.message(strings.format("control_failed", it.message ?: it.javaClass.simpleName)) }
            busy = false
        }
    }

    SectionTitle(str("section_timers"))
    SectionCard {
        val list = entries
        when {
            error != null -> Text(
                str("timers_load_failed", error ?: ""), Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error,
            )
            list == null -> Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
            }
            else -> {
                if (list.isEmpty()) {
                    Text(str("timers_empty"), Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                list.forEachIndexed { i, e ->
                    if (i > 0) RowDivider(inset = 0.dp)
                    val t = e.timer
                    if (t == null) {
                        InfoRow(Icons.Rounded.Schedule, str("timer_other"), str("timer_other_desc"))
                    } else {
                        val what = (if (device.isMultiChannel) str("channel_n", t.channel + 1) + " " else "") +
                            str(if (t.on) "auto_action_turn_on" else "auto_action_turn_off")
                        InfoRow(
                            Icons.Rounded.Schedule,
                            formatMinute(t.hour * 60 + t.minute, strings.locale) + " · " + what,
                            daysLabel(t.days, strings),
                            Modifier.clickable(enabled = !busy) { editing = t },
                        ) {
                            Switch(checked = t.enabled, enabled = !busy, onCheckedChange = { save(t.id, t.copy(enabled = it)) })
                        }
                    }
                }
                RowDivider(inset = 0.dp)
                val full = list.size >= DeviceTimers.MAX_TIMERS
                InfoRow(
                    Icons.Rounded.Add,
                    str("timer_add"),
                    if (full) str("timer_full", DeviceTimers.MAX_TIMERS) else str("timer_add_desc"),
                    Modifier.clickable(enabled = !busy && !full) {
                        editing = RepeatTimer(UUID.randomUUID().toString(), true, 7, 0, DeviceTimers.ALL_DAYS, 0, true)
                    }.testTag("timer-add"),
                )
            }
        }
    }

    editing?.let { t ->
        TimerDialog(
            device = device,
            initial = t,
            isNew = entries?.none { it.timer?.id == t.id } ?: true,
            onSave = { save(it.id, it); editing = null },
            onDelete = { save(t.id, null); editing = null },
            onDismiss = { editing = null },
        )
    }
}

/** "07:30" / "7:30 AM": günün dakikası, seçili dilin kısa saat biçimiyle. */
fun formatMinute(minute: Int, locale: Locale): String =
    DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale).format(LocalTime.of(minute / 60, minute % 60))

/** Pazartesi'den başlayan hafta, cron düzeninde (0 = Pazar). */
private val WEEK_ORDER = listOf(1, 2, 3, 4, 5, 6, 0)

private fun dayName(cronDay: Int, locale: Locale): String =
    (if (cronDay == 0) DayOfWeek.SUNDAY else DayOfWeek.of(cronDay)).getDisplayName(TextStyle.SHORT, locale)

/** 0=Pazar düzenindeki günleri seçili dilin kısa adlarıyla, Pazartesi'den başlayarak yazar. */
fun daysLabel(days: Set<Int>, strings: Strings): String = when {
    days.size == 7 -> strings["days_every"]
    days == setOf(1, 2, 3, 4, 5) -> strings["days_weekdays"]
    days == setOf(0, 6) -> strings["days_weekend"]
    else -> WEEK_ORDER.filter { it in days }.joinToString(", ") { dayName(it, strings.locale) }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun TimerDialog(
    device: DeviceSnapshot,
    initial: RepeatTimer,
    isNew: Boolean,
    onSave: (RepeatTimer) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val locale = LocalStrings.current.locale
    var t by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(str(if (isNew) "timer_add" else "timer_edit")) },
        text = {
            Column(Modifier.widthIn(min = 400.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(str("timer_time"), style = MaterialTheme.typography.titleSmall, modifier = Modifier.width(96.dp))
                    TimeStepper(t.hour * 60 + t.minute) { m -> t = t.copy(hour = m / 60, minute = m % 60) }
                }
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    listOf(true, false).forEachIndexed { i, on ->
                        SegmentedButton(
                            selected = t.on == on,
                            onClick = { t = t.copy(on = on) },
                            shape = SegmentedButtonDefaults.itemShape(i, 2),
                        ) { Text(str(if (on) "auto_action_turn_on" else "auto_action_turn_off")) }
                    }
                }
                if (device.isMultiChannel) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(str("automation_channel"), style = MaterialTheme.typography.titleSmall)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            device.switches.keys.sorted().forEach { ch ->
                                FilterChip(
                                    selected = t.channel == ch,
                                    onClick = { t = t.copy(channel = ch) },
                                    label = { Text(str("channel_n", ch + 1)) },
                                )
                            }
                        }
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    WEEK_ORDER.forEach { d ->
                        FilterChip(
                            selected = d in t.days,
                            onClick = { t = t.copy(days = if (d in t.days) t.days - d else t.days + d) },
                            label = { Text(dayName(d, locale)) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = t.days.isNotEmpty(), onClick = { onSave(t) }) { Text(str("action_save")) }
        },
        dismissButton = {
            Row {
                if (!isNew) TextButton(onClick = onDelete) { Text(str("action_delete"), color = MaterialTheme.colorScheme.error) }
                TextButton(onClick = onDismiss) { Text(str("action_cancel")) }
            }
        },
    )
}

/**
 * Masaüstü saat seçimi (telefondaki saat kadranı yerine): saat ve dakika ayrı alanlar; üstteki/alttaki oklarla,
 * klavyenin yukarı/aşağı tuşlarıyla ya da yazarak değişir. Değer günün dakikasıdır.
 */
@Composable
fun TimeStepper(minuteOfDay: Int, modifier: Modifier = Modifier, onChange: (Int) -> Unit) {
    val hour = minuteOfDay / 60
    val minute = minuteOfDay % 60
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        NumberSpinner(hour, 24, str("desktop_hour"), "hour") { onChange(it * 60 + minute) }
        Text(":", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(horizontal = 8.dp))
        NumberSpinner(minute, 60, str("desktop_minute"), "minute") { onChange(hour * 60 + it) }
    }
}

@Composable
private fun NumberSpinner(value: Int, count: Int, label: String, tag: String, onChange: (Int) -> Unit) {
    fun fmt(v: Int) = String.format(Locale.ROOT, "%02d", v)
    var text by remember { mutableStateOf(fmt(value)) }
    // Oklarla değişince alan güncellenir; yazarken ("1" → "12") yazılan metin korunur.
    LaunchedEffect(value) { if (text.toIntOrNull() != value) text = fmt(value) }
    fun step(delta: Int) = onChange(Math.floorMod(value + delta, count))
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = { step(+1) }, modifier = Modifier.size(32.dp).testTag("$tag-up")) {
            Icon(Icons.Rounded.KeyboardArrowUp, "$label +")
        }
        OutlinedTextField(
            value = text,
            onValueChange = { new ->
                val digits = new.filter(Char::isDigit).takeLast(2)
                val parsed = digits.toIntOrNull()
                // "75" dakika olamaz: son rakamdan yeniden başlanır.
                text = if (parsed != null && parsed >= count) digits.takeLast(1) else digits
                text.toIntOrNull()?.let(onChange)
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.titleLarge.copy(textAlign = TextAlign.Center),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .width(80.dp)
                .testTag("$tag-field")
                .onFocusChanged { if (!it.isFocused) text = fmt(value) }
                .onPreviewKeyEvent { e ->
                    if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (e.key) {
                        Key.DirectionUp -> { step(+1); true }
                        Key.DirectionDown -> { step(-1); true }
                        else -> false
                    }
                },
        )
        IconButton(onClick = { step(-1) }, modifier = Modifier.size(32.dp).testTag("$tag-down")) {
            Icon(Icons.Rounded.KeyboardArrowDown, "$label −")
        }
    }
}

// ---------------------------------------------------------------- ham veri

/** Cihazın ham verisini panoya kopyalar (desteklenmeyen bir modeli bildirmek için). */
@Composable
fun RawDataRow(device: DeviceSnapshot, actions: DeviceActions) {
    val strings = LocalStrings.current
    val ui = LocalDeviceUi.current
    val scope = rememberCoroutineScope()
    InfoRow(
        Icons.Rounded.Code, str("raw_data"), str("raw_data_desc"),
        Modifier.clickable {
            scope.launch {
                actions.rawParams(device)
                    .mapCatching { Links.copyToClipboard(it) }
                    .onSuccess { ui.message(strings["raw_data_copied"]) }
                    .onFailure { ui.message(strings.format("control_failed", it.message ?: it.javaClass.simpleName)) }
            }
        }.testTag("raw-data"),
    )
}

// ---------------------------------------------------------------- demo

/**
 * Demo cihazının sayfasındaki simülasyon düğmeleri: cihazın kendisinde olmuş gibi bir olay canlandırır, böylece
 * kurallar ve alarm gerçek bir cihazdaki gibi denenebilir ([onDemo] motora gider; izleme kapalıysa açar).
 */
@Composable
fun DemoSection(device: DeviceSnapshot, onDemo: (op: String, channel: Int) -> Unit) {
    SectionTitle(str("section_demo"))
    SectionCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(str("demo_hint"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            device.switches.keys.sorted().forEach { ch ->
                DemoButton(if (device.isMultiChannel) str("demo_toggle_channel", ch + 1) else str("demo_toggle")) {
                    onDemo(MonitorEngine.DEMO_TOGGLE, ch)
                }
            }
            DemoButton(str(if (device.online) "demo_disconnect" else "demo_reconnect")) { onDemo(MonitorEngine.DEMO_ONLINE, 0) }
            // Güç düşüşü yalnızca açık ve güç ölçen kanallarda anlamlı.
            device.switches.keys.sorted()
                .filter { device.switches[it] == true && device.channelPowerOf(it) != null }
                .forEach { ch ->
                    val stalled = device.channelPowerOf(ch) == Demo.STALLED_WATTS
                    val label = when {
                        device.isMultiChannel && stalled -> str("demo_power_restore", ch + 1)
                        device.isMultiChannel -> str("demo_power_drop", ch + 1)
                        stalled -> str("demo_power_restore_single")
                        else -> str("demo_power_drop_single")
                    }
                    DemoButton(label) { onDemo(MonitorEngine.DEMO_POWER, ch) }
                }
        }
    }
}

@Composable
private fun DemoButton(text: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) { Text(text) }
}
