package com.macerce.switchguard.desktop.ui.devices

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.macerce.switchguard.core.DeviceSnapshot
import com.macerce.switchguard.core.EnergyPeriod
import com.macerce.switchguard.core.EnergyReport
import com.macerce.switchguard.core.EnergySummary
import com.macerce.switchguard.core.EventType
import com.macerce.switchguard.core.Signal
import com.macerce.switchguard.core.Standby
import com.macerce.switchguard.core.Tariff
import com.macerce.switchguard.data.EventLog
import com.macerce.switchguard.data.MetricsLog
import com.macerce.switchguard.data.Store
import com.macerce.switchguard.desktop.Log
import com.macerce.switchguard.desktop.engine.DailySummary
import com.macerce.switchguard.desktop.i18n.Strings
import com.macerce.switchguard.desktop.report.EnergyExport
import com.macerce.switchguard.desktop.ui.Hue
import com.macerce.switchguard.desktop.ui.HueIcon
import com.macerce.switchguard.desktop.ui.LocalStrings
import com.macerce.switchguard.desktop.ui.SectionCard
import com.macerce.switchguard.desktop.ui.SectionTitle
import com.macerce.switchguard.desktop.ui.StatusColors
import com.macerce.switchguard.desktop.ui.energy.EnergyFormat
import com.macerce.switchguard.desktop.ui.report.ReportActions
import com.macerce.switchguard.desktop.ui.report.chooseSaveTarget
import com.macerce.switchguard.desktop.ui.str
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.util.Locale

/**
 * Gün / ay / yıl seçilerek geçmiş tüketim (telefon 2.14 `EnergyHistory`): dönemler arasında ileri-geri, çubuk grafik
 * (fareyle üzerine gelinen ya da tıklanan çubuğun değeri yazılır), toplam, maliyet ve en yüksek değerler; seçili dönem
 * PDF ya da CSV rapor olarak kaydedilir, kaydedilen dosya açılır. Cihazın kWh fiyatı buradan da değiştirilir.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun EnergyHistory(device: DeviceSnapshot, metrics: MetricsLog, store: Store, message: (String) -> Unit) {
    val strings = LocalStrings.current
    val locale = strings.locale
    val scope = rememberCoroutineScope()
    val metricsVersion by metrics.version.collectAsState()

    var period by remember { mutableStateOf(EnergyPeriod.DAY) }
    var anchor by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var selected by remember(period, anchor) { mutableStateOf<Int?>(null) }
    var hovered by remember(period, anchor) { mutableStateOf<Int?>(null) }
    var price by remember { mutableDoubleStateOf(store.priceFor(device.id)) }
    val tariff = remember(price, store.version.value) { store.tariffFor(device.id) }
    var editPrice by remember { mutableStateOf(false) }

    val now = System.currentTimeMillis()
    val (start, end) = EnergyReport.range(period, anchor)
    val isCurrent = now in start until end
    val first by produceState<Long?>(null, metricsVersion) { value = withContext(Dispatchers.IO) { runCatching { metrics.firstPowerTime(device.id) }.getOrNull() } }
    // Yalnızca içinde bulunulan dönem yeni ölçümle değişir; geçmiş dönemler bir kez hesaplanır.
    val summary by produceState<EnergySummary?>(null, period, start, if (isCurrent) metricsVersion else -1, tariff) {
        value = withContext(Dispatchers.IO) { runCatching { metrics.summary(device.id, period, start, tariff = tariff) }.getOrNull() }
    }

    fun export(format: EnergyExport.Format) {
        val target = chooseSaveTarget(EnergyExport.fileName(strings, device, period, start, format), strings["energy_report_title"], format.ext) ?: return
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    Files.newOutputStream(target).use { EnergyExport.write(strings, store, metrics, it, device, period, start, format) }
                }.onFailure { Log.w("EnergyHistory", "export failed", it) }.isSuccess
            }
            message(strings[if (ok) "energy_report_done" else "energy_report_failed"])
            if (ok) ReportActions.openFile(target)
        }
    }

    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SingleChoiceSegmentedButtonRow(Modifier.widthIn(min = 260.dp)) {
                val periods = EnergyPeriod.entries
                periods.forEachIndexed { i, p ->
                    SegmentedButton(
                        selected = period == p,
                        onClick = { period = p },
                        shape = SegmentedButtonDefaults.itemShape(i, periods.size),
                        modifier = Modifier.testTag("period-${p.name}"),
                    ) {
                        Text(strings[when (p) { EnergyPeriod.DAY -> "energy_period_day"; EnergyPeriod.MONTH -> "energy_period_month"; EnergyPeriod.YEAR -> "energy_period_year" }])
                    }
                }
            }
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { anchor = EnergyReport.shift(period, anchor, -1) }, enabled = first?.let { it < start } == true, modifier = Modifier.testTag("energy-prev")) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, str("energy_prev"))
                }
                Text(
                    EnergyFormat.periodLabel(period, start, locale), style = MaterialTheme.typography.titleSmall,
                    textAlign = TextAlign.Center, modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { anchor = EnergyReport.shift(period, anchor, 1) }, enabled = !isCurrent && start < now, modifier = Modifier.testTag("energy-next")) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, str("energy_next"))
                }
            }
        }

        val s = summary
        if (s == null) {
            Box(Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (s.coveredMs == 0L) {
            Text(str("energy_period_empty"), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(32.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Stat(str("energy_total"), DailySummary.formatKwh(s.kwh, locale), big = true)
                if (tariff.hasPrice) Stat(str("energy_cost"), EnergyFormat.money(s.cost, locale), big = true)
                s.avgWatts?.let { Stat(str("energy_avg_power"), formatWatts(it, locale)) }
                EnergyFormat.averageLabel(period)?.let { label -> s.avgPerActiveBucket?.let { Stat(strings[label], DailySummary.formatKwh(it, locale)) } }
                s.busiest?.let { b -> Stat(strings[EnergyFormat.busiestLabel(period)], EnergyFormat.sliceLabel(period, b, locale)) }
            }
            PeriodBars(s, hovered ?: selected, locale, onHover = { hovered = it }) { selected = if (selected == it) null else it }
            val sel = (hovered ?: selected)?.let { s.buckets.getOrNull(it) }
            val peakText = str("energy_peak")
            Text(
                if (sel != null) EnergyFormat.sliceLabel(period, sel, locale) + ": " + DailySummary.formatKwh(sel.kwh, locale) +
                    (sel.peakWatts?.let { " · $peakText " + formatWatts(it, locale) } ?: "") +
                    (if (tariff.hasPrice) " · " + EnergyFormat.money(sel.cost, locale) else "")
                else s.peak?.let { p ->
                    "$peakText: " + formatWatts(p.peakWatts!!, locale) + " (" +
                        EnergyFormat.peakTime(if (period == EnergyPeriod.DAY) period else EnergyPeriod.YEAR, p.peakAt!!, locale) + ")"
                }.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("energy-selection"),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            FilledTonalButton(onClick = { export(EnergyExport.Format.PDF) }, enabled = s != null, modifier = Modifier.testTag("energy-pdf")) {
                Icon(Icons.Rounded.Description, null, Modifier.size(18.dp))
                Text(str("energy_report_pdf"), Modifier.padding(start = 8.dp))
            }
            OutlinedButton(onClick = { export(EnergyExport.Format.CSV) }, enabled = s != null, modifier = Modifier.testTag("energy-csv")) {
                Icon(Icons.Rounded.TableChart, null, Modifier.size(18.dp))
                Text(str("energy_report_csv"), Modifier.padding(start = 8.dp))
            }
            TextButton(onClick = { editPrice = true }, modifier = Modifier.testTag("energy-price")) {
                Text(str("energy_set_price") + when {
                    tariff is Tariff.ThreeZone && tariff.hasPrice -> ": " + str("tariff_three_short")
                    price > 0 -> ": " + EnergyFormat.money(price, locale)
                    else -> ""
                })
            }
        }
    }

    if (editPrice) {
        PriceDialog(device, store, strings, onDismiss = { editPrice = false }) { price = store.priceFor(device.id) }
    }
}

@Composable
private fun Stat(label: String, value: String, big: Boolean = false) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            style = if (big) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (big) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun formatPrice(v: Double): String = java.math.BigDecimal.valueOf(v).stripTrailingZeros().toPlainString()

@Composable
private fun PriceDialog(device: DeviceSnapshot, store: Store, strings: Strings, onDismiss: () -> Unit, onSaved: () -> Unit) {
    val locale = strings.locale
    val own = store.devicePrices[device.id]
    var text by remember { mutableStateOf(own?.let(::formatPrice) ?: "") }
    var asDefault by remember { mutableStateOf(store.energyPrice <= 0) }
    val symbol = EnergyFormat.currency()?.getSymbol(locale) ?: ""
    val fallback = store.energyPrice
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings["energy_price_title"]) },
        text = {
            Column(Modifier.width(440.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(strings.format("energy_price_device_desc", device.name, symbol))
                OutlinedTextField(
                    value = text,
                    onValueChange = { v -> text = v.filter { it.isDigit() || it == ',' || it == '.' }.take(10) },
                    singleLine = true,
                    suffix = { Text("$symbol / kWh") },
                    placeholder = if (fallback > 0) ({ Text(formatPrice(fallback)) }) else null,
                    modifier = Modifier.fillMaxWidth().testTag("price-field"),
                )
                if (fallback > 0) {
                    Text(strings.format("energy_price_default_hint", EnergyFormat.money(fallback, locale)),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(Modifier.fillMaxWidth().clickable { asDefault = !asDefault }, verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = asDefault, onCheckedChange = null)
                    Text(strings["energy_price_as_default"], Modifier.padding(start = 8.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val value = text.replace(',', '.').toDoubleOrNull()
                store.setDevicePrice(device.id, value)
                if (asDefault && value != null) store.energyPrice = value
                onSaved()
                onDismiss()
            }, modifier = Modifier.testTag("price-save")) { Text(strings["action_save"]) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings["action_cancel"]) } },
    )
}

/** Dönemin dilimleri dikey çubuklarla; fareyle üzerine gelinen ya da seçilen çubuk vurgulanır. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun PeriodBars(summary: EnergySummary, selected: Int?, locale: Locale, onHover: (Int?) -> Unit, onSelect: (Int) -> Unit) {
    val buckets = summary.buckets
    val max = buckets.maxOf { it.kwh }.takeIf { it > 0 } ?: 1.0
    val every = when (summary.period) { EnergyPeriod.DAY -> 2; EnergyPeriod.MONTH -> 2; EnergyPeriod.YEAR -> 1 }
    val primary = MaterialTheme.colorScheme.primary
    val dim = primary.copy(alpha = 0.35f)
    val grid = MaterialTheme.colorScheme.outlineVariant
    Column {
        Text(
            if (buckets.any { it.kwh > 0 }) DailySummary.formatKwh(max, locale) else "",
            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box(Modifier.fillMaxWidth().height(180.dp)) {
            // Izgara: üst, orta, alt.
            Column(Modifier.matchParentSize(), verticalArrangement = Arrangement.SpaceBetween) {
                repeat(3) { Box(Modifier.fillMaxWidth().height(1.dp).background(grid.copy(alpha = 0.5f))) }
            }
            Row(Modifier.matchParentSize(), verticalAlignment = Alignment.Bottom) {
                buckets.forEachIndexed { i, b ->
                    Box(
                        Modifier.weight(1f).fillMaxHeight()
                            .onPointerEvent(PointerEventType.Enter) { onHover(i) }
                            .onPointerEvent(PointerEventType.Exit) { onHover(null) }
                            .clickable { onSelect(i) }
                            .testTag("bar-$i"),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        Box(
                            Modifier.fillMaxWidth(0.7f)
                                .fillMaxHeight((b.kwh / max).toFloat().coerceIn(if (b.kwh > 0) 0.02f else 0.005f, 1f))
                                .background(if (selected == null || selected == i) primary else dim, RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)),
                        )
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth()) {
            buckets.forEachIndexed { i, b ->
                Text(
                    if (i % every == 0) EnergyFormat.axisLabel(summary.period, b.start, locale) else "",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, softWrap = false, overflow = TextOverflow.Visible, textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

// ---------------------------------------------------------------- bağlantı ve bekleme tüketimi

/**
 * Bağlantı (telefon 2.15 `ConnectionCard`): Wi-Fi sinyali (çubuklar, dBm, ne anlama geldiği) ve son 7 günün kopmaları.
 * Sık kopmada olası neden ve öneri. Sinyal bildirmeyen ve hiç kopmamış cihazda gösterilmez.
 */
@Composable
fun ConnectionCard(device: DeviceSnapshot, log: EventLog) {
    val strings = LocalStrings.current
    val logVersion by log.version.collectAsState()
    val drops by produceState(0, logVersion, device.id) {
        value = withContext(Dispatchers.IO) {
            runCatching { log.count(device.id, EventType.WENT_OFFLINE.name, System.currentTimeMillis() - 7 * 86_400_000L) }.getOrDefault(0)
        }
    }
    val rssi = device.rssi
    if (rssi == null && drops == 0) return
    SectionTitle(str("connection_title"))
    SectionCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                HueIcon(Icons.Rounded.Wifi, Hue.BLUE, size = 32.dp)
                Column(Modifier.padding(start = 12.dp).weight(1f)) {
                    if (rssi != null) {
                        val q = Signal.quality(rssi)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SignalBars(q.bars)
                            Text(strings.format("signal_value", strings[q.labelKey()], rssi), style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(start = 8.dp))
                        }
                    } else {
                        Text(str("signal_unknown"), style = MaterialTheme.typography.titleSmall)
                    }
                    Text(strings.plural("drops_week", drops, drops), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            val advice = when (Signal.dropCause(drops, rssi)) {
                Signal.Cause.WEAK_SIGNAL -> "drops_cause_signal"
                Signal.Cause.POWER_OR_ROUTER -> "drops_cause_power"
                Signal.Cause.UNKNOWN -> "drops_cause_unknown"
                null -> if (rssi != null && Signal.quality(rssi) == Signal.Quality.WEAK) "signal_weak_hint" else null
            }
            if (advice != null) Text(strings[advice], style = MaterialTheme.typography.bodyMedium, color = StatusColors.warning)
            Text(str("signal_explain"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Dört çubuklu sinyal göstergesi. */
@Composable
fun SignalBars(bars: Int) {
    val on = MaterialTheme.colorScheme.primary
    val off = MaterialTheme.colorScheme.outlineVariant
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        (1..4).forEach { i -> Box(Modifier.width(4.dp).height((4 + i * 3).dp).background(if (i <= bars) on else off, RoundedCornerShape(1.dp))) }
    }
}

fun Signal.Quality.labelKey(): String = when (this) {
    Signal.Quality.EXCELLENT -> "signal_excellent"
    Signal.Quality.GOOD -> "signal_good"
    Signal.Quality.FAIR -> "signal_fair"
    Signal.Quality.WEAK -> "signal_weak"
}

/**
 * Bekleme (temel) tüketimi (telefon 2.15 `StandbyCard`): son 7 günün en düşük saatlik ortalaması, aylık karşılığı ve
 * maliyeti. Yeterli ölçüm yoksa ya da önemsizse gösterilmez.
 */
@Composable
fun StandbyCard(device: DeviceSnapshot, metrics: MetricsLog, store: Store) {
    val strings = LocalStrings.current
    val locale = strings.locale
    val result by produceState<Standby.Result?>(null, device.id) {
        value = withContext(Dispatchers.IO) { runCatching { metrics.standby(device.id) }.getOrNull() }
    }
    val r = result ?: return
    val tariff = remember { store.tariffFor(device.id) }
    val cost = remember(r) { if (tariff.hasPrice) Standby.monthlyCost(r.watts, tariff, System.currentTimeMillis()) else 0.0 }
    SectionTitle(str("standby_title"))
    SectionCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                HueIcon(Icons.Rounded.PowerSettingsNew, Hue.AMBER, size = 32.dp)
                Column(Modifier.padding(start = 12.dp)) {
                    Text(formatWatts(r.watts, locale), style = MaterialTheme.typography.titleMedium)
                    Text(
                        strings.format("standby_month", DailySummary.formatKwh(r.monthlyKwh, locale)) +
                            (if (cost > 0) " · ≈ " + EnergyFormat.money(cost, locale) else ""),
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(str("standby_explain"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
