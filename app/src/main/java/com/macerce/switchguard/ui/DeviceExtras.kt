package com.macerce.switchguard.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.text.format.DateFormat as AndroidDateFormat
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.macerce.switchguard.core.EnergyPeriod
import com.macerce.switchguard.core.EnergyReport
import com.macerce.switchguard.core.EnergySummary
import com.macerce.switchguard.data.Store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.macerce.switchguard.R
import com.macerce.switchguard.api.ApiException
import com.macerce.switchguard.core.Cloud
import com.macerce.switchguard.core.DeviceSnapshot
import com.macerce.switchguard.core.DeviceTimers
import com.macerce.switchguard.core.RepeatTimer
import com.macerce.switchguard.core.TimerEntry
import com.macerce.switchguard.data.MetricsLog
import com.macerce.switchguard.service.DailySummary
import java.util.Calendar
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun formatWatts(w: Double): String =
    if (w >= 1000) String.format(Locale.getDefault(), "%.2f kW", w / 1000)
    else String.format(Locale.getDefault(), "%.0f W", w)

fun formatNumber(v: Double): String =
    if (v % 1.0 == 0.0) v.toLong().toString() else String.format(Locale.US, "%.1f", v)

// ---------------------------------------------------------------- enerji

/** Enerji ölçen cihazlarda: anlık değerler, bugün/bu ay tüketim ve gün/ay/yıl geçmişi + rapor. */
@Composable
fun EnergySection(device: DeviceSnapshot) {
    val context = LocalContext.current
    val metrics = remember { MetricsLog.get(context) }
    val metricsVersion by metrics.version.collectAsStateWithLifecycle()

    // Ekran açıkken cihazdan canlı güç iste; POW modelleri aksi halde seyrek gönderir.
    LaunchedEffect(device.id) {
        // Tuya prizleri gücü kendiliğinden bildirir; istek eWeLink'e özgü.
        if (device.cloud != Cloud.EWELINK) return@LaunchedEffect
        while (true) {
            Actions.requestLiveEnergy(context, device)
            delay(100_000)
        }
    }

    val now = System.currentTimeMillis()
    val today = DailySummary.startOfDay(now)
    val todayKwh = remember(metricsVersion, today) { metrics.kwh(device.id, today, now) }
    val month = remember(metricsVersion, today) {
        val from = Calendar.getInstance().apply { timeInMillis = today; set(Calendar.DAY_OF_MONTH, 1) }.timeInMillis
        metrics.kwh(device.id, from, now)
    }
    val hasData = remember(metricsVersion) { metrics.hasPowerData(device.id) }

    SectionTitle(stringResource(R.string.section_energy))
    SectionCard {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            // Çevrimdışıyken son ölçüm anlık değer gibi gösterilmez.
            val live = device.online
            Metric(stringResource(R.string.energy_power), device.power?.takeIf { live }?.let(::formatWatts) ?: "–", big = true)
            Metric(stringResource(R.string.energy_voltage), device.voltage?.takeIf { live }?.let { String.format(Locale.getDefault(), "%.0f V", it) } ?: "–")
            Metric(stringResource(R.string.energy_current), device.current?.takeIf { live }?.let { String.format(Locale.getDefault(), "%.2f A", it) } ?: "–")
        }
        if (device.online && device.channelPower.isNotEmpty()) {
            RowDivider(inset = 0.dp)
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                device.channelPower.toSortedMap().forEach { (ch, w) ->
                    Metric(stringResource(R.string.channel_n, ch + 1), formatWatts(w))
                }
            }
        }
        RowDivider(inset = 0.dp)
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Metric(stringResource(R.string.energy_today), DailySummary.formatKwh(todayKwh))
            Metric(stringResource(R.string.energy_month), DailySummary.formatKwh(month))
        }
        RowDivider(inset = 0.dp)
        if (!hasData) {
            Text(
                stringResource(R.string.energy_no_data),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
        } else {
            EnergyHistory(device, metricsVersion)
        }
    }
    Text(
        stringResource(R.string.energy_hint),
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

/**
 * Gün / ay / yıl seçilerek geçmiş tüketim: dönemler arasında ileri-geri, çubuk grafik (dokunulan çubuğun
 * değeri yazılır), toplam, maliyet ve en yüksek değerler; seçili dönem PDF ya da CSV rapor olarak kaydedilir.
 */
@Composable
private fun EnergyHistory(device: DeviceSnapshot, metricsVersion: Int) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val locale = resources.configuration.locales[0]
    val metrics = remember { MetricsLog.get(context) }
    val store = remember { Store.get(context) }
    val scope = rememberCoroutineScope()

    var period by rememberSaveable { mutableStateOf(EnergyPeriod.DAY) }
    var anchor by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }
    var selected by remember(period, anchor) { mutableStateOf<Int?>(null) }
    var price by remember { mutableDoubleStateOf(store.priceFor(device.id)) }
    // Fiyat değişince maliyet yeniden hesaplansın.
    val tariff = remember(price) { store.tariffFor(device.id) }
    var editPrice by remember { mutableStateOf(false) }
    var chooseReport by remember { mutableStateOf(false) }

    val now = System.currentTimeMillis()
    val (start, end) = EnergyReport.range(period, anchor)
    val isCurrent = now in start until end
    val first = remember(metricsVersion) { metrics.firstPowerTime(device.id) }
    // Yalnızca içinde bulunulan dönem yeni ölçümle değişir; geçmiş dönemler bir kez hesaplanır.
    val summary by produceState<EnergySummary?>(null, period, start, if (isCurrent) metricsVersion else -1, tariff) {
        value = withContext(Dispatchers.IO) { metrics.summary(device.id, period, start, tariff = tariff) }
    }

    @Composable
    fun saver(format: EnergyExport.Format) =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(format.mime)) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            scope.launch {
                val ok = runCatching { EnergyExport.write(context, uri, device, period, start, format) }.isSuccess
                Toast.makeText(
                    context, resources.getString(if (ok) R.string.energy_report_done else R.string.energy_report_failed),
                    Toast.LENGTH_LONG,
                ).show()
            }
        }
    val pdfSaver = saver(EnergyExport.Format.PDF)
    val csvSaver = saver(EnergyExport.Format.CSV)

    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            val periods = EnergyPeriod.entries
            periods.forEachIndexed { i, p ->
                SegmentedButton(
                    selected = period == p,
                    onClick = { period = p },
                    shape = SegmentedButtonDefaults.itemShape(i, periods.size),
                ) {
                    Text(stringResource(when (p) {
                        EnergyPeriod.DAY -> R.string.energy_period_day
                        EnergyPeriod.MONTH -> R.string.energy_period_month
                        EnergyPeriod.YEAR -> R.string.energy_period_year
                    }))
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { anchor = EnergyReport.shift(period, anchor, -1) },
                enabled = first != null && first < start,
                modifier = Modifier.tvFocusRing(),
            ) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, stringResource(R.string.energy_prev)) }
            Text(
                EnergyFormat.periodLabel(period, start, locale),
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = { anchor = EnergyReport.shift(period, anchor, 1) },
                enabled = !isCurrent && start < now,
                modifier = Modifier.tvFocusRing(),
            ) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, stringResource(R.string.energy_next)) }
        }

        val s = summary
        if (s == null) {
            Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (s.coveredMs == 0L) {
            Text(
                stringResource(R.string.energy_period_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Metric(stringResource(R.string.energy_total), DailySummary.formatKwh(s.kwh), big = true)
                if (tariff.hasPrice) Metric(stringResource(R.string.energy_cost), EnergyFormat.money(s.cost, locale))
            }
            PeriodBars(s, selected, locale) { selected = if (selected == it) null else it }
            val sel = selected?.let { s.buckets.getOrNull(it) }
            val peakText = stringResource(R.string.energy_peak)
            Text(
                if (sel != null) EnergyFormat.sliceLabel(period, sel, locale) + ": " + DailySummary.formatKwh(sel.kwh) +
                    (sel.peakWatts?.let { " · $peakText " + formatWatts(it) } ?: "") +
                    (if (tariff.hasPrice) " · " + EnergyFormat.money(sel.cost, locale) else "")
                else s.peak?.let { p ->
                    "$peakText: " + formatWatts(p.peakWatts!!) + " (" +
                        EnergyFormat.peakTime(if (period == EnergyPeriod.DAY) period else EnergyPeriod.YEAR, p.peakAt!!, locale) + ")"
                }.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                s.avgWatts?.let { Metric(stringResource(R.string.energy_avg_power), formatWatts(it)) }
                EnergyFormat.averageLabel(period)?.let { label ->
                    s.avgPerActiveBucket?.let { Metric(stringResource(label), DailySummary.formatKwh(it)) }
                }
                s.busiest?.let { b ->
                    Metric(stringResource(EnergyFormat.busiestLabel(period)), EnergyFormat.sliceLabel(period, b, locale))
                }
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // TV'de dosya kaydetme ekranı (DocumentsUI) yok.
            if (!LocalIsTv.current) {
                FilledTonalButton(onClick = { chooseReport = true }, enabled = s != null) {
                    Icon(Icons.Rounded.Description, null, Modifier.size(18.dp))
                    Text(stringResource(R.string.energy_report), Modifier.padding(start = 8.dp))
                }
            }
            TextButton(onClick = { editPrice = true }, modifier = Modifier.tvFocusRing()) {
                Text(stringResource(R.string.energy_set_price) + when {
                    tariff is com.macerce.switchguard.core.Tariff.ThreeZone && tariff.hasPrice -> ": " + stringResource(R.string.tariff_three_short)
                    price > 0 -> ": " + EnergyFormat.money(price, locale)
                    else -> ""
                })
            }
        }
    }

    if (chooseReport) {
        AlertDialog(
            onDismissRequest = { chooseReport = false },
            title = { Text(stringResource(R.string.energy_report_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.energy_report_desc, EnergyFormat.periodLabel(period, start, locale)))
                    TextButton(onClick = {
                        chooseReport = false
                        pdfSaver.launch(EnergyExport.fileName(context, device, period, start, EnergyExport.Format.PDF))
                    }) { Text(stringResource(R.string.energy_report_pdf)) }
                    TextButton(onClick = {
                        chooseReport = false
                        csvSaver.launch(EnergyExport.fileName(context, device, period, start, EnergyExport.Format.CSV))
                    }) { Text(stringResource(R.string.energy_report_csv)) }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { chooseReport = false }) { Text(stringResource(android.R.string.cancel)) } },
        )
    }
    if (editPrice) {
        val own = store.devicePrices[device.id]
        var text by remember { mutableStateOf(own?.let(::formatPrice) ?: "") }
        var asDefault by remember { mutableStateOf(store.energyPrice <= 0) }
        val symbol = EnergyFormat.currency()?.getSymbol(locale) ?: ""
        val fallback = store.energyPrice
        AlertDialog(
            onDismissRequest = { editPrice = false },
            title = { Text(stringResource(R.string.energy_price_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.energy_price_device_desc, device.name, symbol))
                    OutlinedTextField(
                        value = text,
                        onValueChange = { v -> text = v.filter { it.isDigit() || it == ',' || it == '.' }.take(10) },
                        singleLine = true,
                        suffix = { Text("$symbol / kWh") },
                        placeholder = if (fallback > 0) ({ Text(formatPrice(fallback)) }) else null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    )
                    if (fallback > 0) {
                        Text(
                            stringResource(R.string.energy_price_default_hint, EnergyFormat.money(fallback, locale)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Row(
                        Modifier.fillMaxWidth().clickable { asDefault = !asDefault }.tvFocusRing(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = asDefault, onCheckedChange = null)
                        Text(stringResource(R.string.energy_price_as_default), Modifier.padding(start = 8.dp))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val value = text.replace(',', '.').toDoubleOrNull()
                    store.setDevicePrice(device.id, value)
                    if (asDefault && value != null) store.energyPrice = value
                    price = store.priceFor(device.id)
                    editPrice = false
                }) { Text(stringResource(android.R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { editPrice = false }) { Text(stringResource(android.R.string.cancel)) } },
        )
    }
}

private fun formatPrice(v: Double): String = java.math.BigDecimal.valueOf(v).stripTrailingZeros().toPlainString()

/** Dönemin dilimleri dikey çubuklarla; dokunulan çubuk vurgulanır. */
@Composable
private fun PeriodBars(summary: EnergySummary, selected: Int?, locale: Locale, onSelect: (Int) -> Unit) {
    val buckets = summary.buckets
    val max = buckets.maxOf { it.kwh }.takeIf { it > 0 } ?: 1.0
    val every = when (summary.period) { EnergyPeriod.DAY -> 3; EnergyPeriod.MONTH -> 5; EnergyPeriod.YEAR -> 1 }
    val primary = MaterialTheme.colorScheme.primary
    val dim = primary.copy(alpha = 0.35f)
    Column {
        Text(
            if (buckets.any { it.kwh > 0 }) DailySummary.formatKwh(max) else "",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(Modifier.fillMaxWidth().height(120.dp), verticalAlignment = Alignment.Bottom) {
            buckets.forEachIndexed { i, b ->
                Box(
                    Modifier.weight(1f).fillMaxHeight().clickable { onSelect(i) },
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Box(
                        Modifier.fillMaxWidth(0.7f)
                            .fillMaxHeight((b.kwh / max).toFloat().coerceIn(if (b.kwh > 0) 0.02f else 0.005f, 1f))
                            .background(
                                if (selected == null || selected == i) primary else dim,
                                RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp),
                            )
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth()) {
            buckets.forEachIndexed { i, b ->
                Text(
                    if (i % every == 0) EnergyFormat.axisLabel(summary.period, b.start, locale) else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Visible,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

// ---------------------------------------------------------------- zamanlayıcılar

/** Cihazın eWeLink zamanlayıcıları: telefon kapalıyken de cihazda çalışır. */
@Composable
fun TimersSection(device: DeviceSnapshot) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    var entries by remember { mutableStateOf<List<TimerEntry>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<RepeatTimer?>(null) }

    LaunchedEffect(device.id) {
        Actions.loadTimers(context, device.id)
            .onSuccess { entries = it; error = null }
            .onFailure {
                // Ham API mesajı yerine kısa açıklama: internet yok ya da eWeLink hata kodu.
                error = when (it) {
                    is ApiException -> "eWeLink ${it.code}"
                    else -> resources.getString(R.string.state_no_internet)
                }
            }
    }

    fun save(id: String, timer: RepeatTimer?) {
        busy = true
        scope.launch {
            Actions.saveTimer(context, device, id, timer)
                .onSuccess { entries = it }
                .onFailure {
                    Toast.makeText(context, resources.getString(R.string.control_failed, it.message ?: ""), Toast.LENGTH_LONG).show()
                }
            busy = false
        }
    }

    SectionTitle(stringResource(R.string.section_timers))
    SectionCard {
        val list = entries
        when {
            error != null -> Text(stringResource(R.string.timers_load_failed, error ?: ""), Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            list == null -> Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
            }
            else -> {
                if (list.isEmpty()) {
                    Text(stringResource(R.string.timers_empty), Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                list.forEachIndexed { i, e ->
                    if (i > 0) RowDivider(inset = 0.dp)
                    val t = e.timer
                    if (t == null) {
                        InfoRow(Icons.Rounded.Schedule, stringResource(R.string.timer_other), stringResource(R.string.timer_other_desc))
                    } else {
                        InfoRow(
                            Icons.Rounded.Schedule,
                            formatMinute(context, t.hour * 60 + t.minute) + " · " +
                                (if (device.isMultiChannel) stringResource(R.string.channel_n, t.channel + 1) + " " else "") +
                                stringResource(if (t.on) R.string.auto_action_turn_on else R.string.auto_action_turn_off),
                            daysLabel(t.days),
                            Modifier.tvFocusRing(RoundedCornerShape(12.dp), inside = true).clickable(enabled = !busy) { editing = t },
                        ) {
                            Switch(checked = t.enabled, enabled = !busy, onCheckedChange = { save(t.id, t.copy(enabled = it)) })
                        }
                    }
                }
                RowDivider(inset = 0.dp)
                val full = list.size >= DeviceTimers.MAX_TIMERS
                InfoRow(
                    Icons.Rounded.Add,
                    stringResource(R.string.timer_add),
                    if (full) stringResource(R.string.timer_full, DeviceTimers.MAX_TIMERS) else stringResource(R.string.timer_add_desc),
                    Modifier.tvFocusRing(RoundedCornerShape(12.dp), inside = true).clickable(enabled = !busy && !full) {
                        editing = RepeatTimer(UUID.randomUUID().toString(), true, 7, 0, DeviceTimers.ALL_DAYS, 0, true)
                    },
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

/** 0=Pazar düzenindeki günleri yerel kısa adlarla, Pazartesi'den başlayarak yazar. */
@Composable
private fun daysLabel(days: Set<Int>): String {
    if (days.size == 7) return stringResource(R.string.days_every)
    if (days == setOf(1, 2, 3, 4, 5)) return stringResource(R.string.days_weekdays)
    if (days == setOf(0, 6)) return stringResource(R.string.days_weekend)
    return WEEK_ORDER.filter { it in days }.joinToString(", ") { dayName(it) }
}

private val WEEK_ORDER = listOf(1, 2, 3, 4, 5, 6, 0)

private fun dayName(cronDay: Int): String {
    val cal = Calendar.getInstance().apply { set(Calendar.DAY_OF_WEEK, cronDay + 1) } // Calendar: Pazar=1
    return AndroidDateFormat.format("EEE", cal).toString()
}

@Composable
private fun TimerDialog(
    device: DeviceSnapshot,
    initial: RepeatTimer,
    isNew: Boolean,
    onSave: (RepeatTimer) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val time = rememberTimePicker()
    var t by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (isNew) R.string.timer_add else R.string.timer_edit)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val timeTitle = stringResource(R.string.timer_time)
                InfoRow(
                    Icons.Rounded.Schedule, timeTitle, formatMinute(context, t.hour * 60 + t.minute),
                    Modifier.clickable { time.pick(timeTitle, t.hour * 60 + t.minute) { m -> t = t.copy(hour = m / 60, minute = m % 60) } },
                )
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    listOf(true, false).forEachIndexed { i, on ->
                        SegmentedButton(
                            selected = t.on == on,
                            onClick = { t = t.copy(on = on) },
                            shape = SegmentedButtonDefaults.itemShape(i, 2),
                        ) { Text(stringResource(if (on) R.string.auto_action_turn_on else R.string.auto_action_turn_off)) }
                    }
                }
                if (device.isMultiChannel) {
                    Picker(stringResource(R.string.automation_channel), device.switches.keys.sorted(), t.channel,
                        { stringResource(R.string.channel_n, it + 1) }) { t = t.copy(channel = it) }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    WEEK_ORDER.forEach { d ->
                        FilterChip(
                            selected = d in t.days,
                            onClick = { t = t.copy(days = if (d in t.days) t.days - d else t.days + d) },
                            label = { Text(dayName(d).take(2)) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(modifier = Modifier.tvFocusRing(), enabled = t.days.isNotEmpty(), onClick = { onSave(t) }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            Row {
                if (!isNew) TextButton(modifier = Modifier.tvFocusRing(), onClick = onDelete) { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) }
                TextButton(modifier = Modifier.tvFocusRing(), onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
            }
        },
    )
}

// ---------------------------------------------------------------- ham veri

/** Cihazın ham verisini panoya kopyalar (desteklenmeyen bir modeli bildirmek için). */
@Composable
fun RawDataRow(deviceId: String) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    InfoRow(
        Icons.Rounded.Code, stringResource(R.string.raw_data), stringResource(R.string.raw_data_desc),
        Modifier.clickable {
            scope.launch {
                Actions.rawParams(context, deviceId)
                    .onSuccess {
                        context.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("device", it))
                        Toast.makeText(context, R.string.raw_data_copied, Toast.LENGTH_SHORT).show()
                    }
                    .onFailure {
                        Toast.makeText(context, resources.getString(R.string.control_failed, it.message ?: ""), Toast.LENGTH_LONG).show()
                    }
            }
        },
    )
}
