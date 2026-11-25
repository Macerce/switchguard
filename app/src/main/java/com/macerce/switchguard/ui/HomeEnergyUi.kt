package com.macerce.switchguard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.macerce.switchguard.R
import com.macerce.switchguard.core.DeviceGroup
import com.macerce.switchguard.core.DeviceSnapshot
import com.macerce.switchguard.core.EnergyPeriod
import com.macerce.switchguard.core.EnergyReport
import com.macerce.switchguard.core.HomeEnergy
import com.macerce.switchguard.data.MetricsLog
import com.macerce.switchguard.data.Store
import com.macerce.switchguard.service.DailySummary
import com.macerce.switchguard.ui.theme.tileColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/**
 * Tüm evin enerji panosu (tam ekran): şu anki toplam güç, gün/ay/yıl toplamı ve maliyeti, dilim grafiği,
 * önceki dönemle karşılaştırma, cihazlara göre pay çubukları; ana sayaç varsa ölçülmeyen yükler.
 */
@Composable
fun HomeEnergyDialog(devices: List<DeviceSnapshot>, onOpenDevice: (String) -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().statusBarsPadding()) {
                AppTopBar(stringResource(R.string.home_energy), navigationIcon = { BackButton(onDismiss) }, small = true)
                Column(Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding().padding(bottom = 24.dp)) {
                    HomeEnergyBody(devices, onOpenDevice)
                }
            }
        }
    }
}

@Composable
private fun HomeEnergyBody(devices: List<DeviceSnapshot>, onOpenDevice: (String) -> Unit) {
    val context = LocalContext.current
    val locale = LocalResources.current.configuration.locales[0]
    val metrics = remember { MetricsLog.get(context) }
    val store = remember { Store.get(context) }
    val version by metrics.version.collectAsStateWithLifecycle()

    var period by rememberSaveable { mutableStateOf(EnergyPeriod.DAY) }
    var anchor by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }
    var selected by remember(period, anchor) { mutableStateOf<Int?>(null) }

    val real = devices.filter { !DeviceGroup.isGroup(it.id) }
    val ids = real.map { it.id }
    val meters = real.filter(HomeEnergy::isMeter).map { it.id }.toSet()
    val now = System.currentTimeMillis()
    val (start, end) = EnergyReport.range(period, anchor)
    val isCurrent = now in start until end

    data class Loaded(val home: HomeEnergy, val previousKwh: Double, val first: Long?)
    val loaded by produceState<Loaded?>(null, period, start, ids, if (isCurrent) version else -1) {
        value = withContext(Dispatchers.IO) {
            val measured = ids.filter { metrics.hasPowerData(it) }
            fun at(from: Long) = measured.associateWith { metrics.summary(it, period, from, tariff = store.tariffFor(it)) }
            val home = HomeEnergy.build(period, start, end, at(start), meters)
            val prevStart = EnergyReport.shift(period, start, -1)
            val prev = HomeEnergy.build(period, prevStart, start, at(prevStart), meters)
            Loaded(home, prev.total.kwh, measured.mapNotNull { metrics.firstPowerTime(it) }.minOrNull())
        }
    }

    // Şu an: sayaç varsa onun gücü, yoksa ölçen cihazların toplamı.
    val livePower = real.filter { it.online && it.power != null }.let { on ->
        val m = on.filter { it.id in meters }
        (m.ifEmpty { on }).sumOf { it.power ?: 0.0 }
    }
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatTile(stringResource(R.string.home_energy_now), formatWatts(livePower), Modifier.weight(1f))
        StatTile(
            stringResource(R.string.home_energy_devices_count),
            (loaded?.home?.shares?.size ?: 0).toString(),
            Modifier.weight(1f),
        )
    }
    Spacer(Modifier.height(8.dp))

    SectionCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                val periods = EnergyPeriod.entries
                periods.forEachIndexed { i, p ->
                    SegmentedButton(selected = period == p, onClick = { period = p }, shape = SegmentedButtonDefaults.itemShape(i, periods.size)) {
                        Text(stringResource(when (p) {
                            EnergyPeriod.DAY -> R.string.energy_period_day
                            EnergyPeriod.MONTH -> R.string.energy_period_month
                            EnergyPeriod.YEAR -> R.string.energy_period_year
                        }))
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                val first = loaded?.first
                IconButton(
                    onClick = { anchor = EnergyReport.shift(period, anchor, -1) },
                    enabled = first != null && first < start, modifier = Modifier.tvFocusRing(),
                ) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, stringResource(R.string.energy_prev)) }
                Text(
                    EnergyFormat.periodLabel(period, start, locale), style = MaterialTheme.typography.titleSmall,
                    textAlign = TextAlign.Center, modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = { anchor = EnergyReport.shift(period, anchor, 1) },
                    enabled = !isCurrent && start < now, modifier = Modifier.tvFocusRing(),
                ) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, stringResource(R.string.energy_next)) }
            }
            val l = loaded
            when {
                l == null -> Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                l.home.total.coveredMs == 0L -> Text(
                    stringResource(if (l.first == null) R.string.home_energy_empty else R.string.energy_period_empty),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                else -> {
                    val s = l.home.total
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Metric(stringResource(R.string.energy_total), DailySummary.formatKwh(s.kwh), big = true)
                        if (s.cost > 0) Metric(stringResource(R.string.energy_cost), EnergyFormat.money(s.cost, locale))
                    }
                    if (l.previousKwh > 0) {
                        val diff = ((s.kwh - l.previousKwh) / l.previousKwh * 100).roundToInt()
                        Text(
                            stringResource(R.string.home_energy_previous, DailySummary.formatKwh(l.previousKwh)) +
                                if (!isCurrent) " · " + (if (diff > 0) "+" else "") + "$diff%" else "",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    PeriodBars(s, selected, locale) { selected = if (selected == it) null else it }
                    val sel = selected?.let { s.buckets.getOrNull(it) }
                    if (sel != null) {
                        Text(
                            EnergyFormat.sliceLabel(period, sel, locale) + ": " + DailySummary.formatKwh(sel.kwh) +
                                (if (sel.cost > 0) " · " + EnergyFormat.money(sel.cost, locale) else ""),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    s.busiest?.let { b ->
                        Metric(stringResource(EnergyFormat.busiestLabel(period)), EnergyFormat.sliceLabel(period, b, locale))
                    }
                }
            }
        }
    }

    val l = loaded ?: return
    if (l.home.shares.isEmpty() && l.home.meter == null) return
    SectionTitle(stringResource(R.string.home_energy_by_device))
    SectionCard {
        Column(Modifier.padding(vertical = 8.dp)) {
            val names = real.associate { it.id to it.name }
            val baseKwh = l.home.total.kwh
            l.home.shares.forEach { sh ->
                ShareRow(names[sh.id] ?: sh.id, sh.kwh, if (baseKwh > 0) sh.kwh / baseKwh else 0.0, sh.cost, locale) { onOpenDevice(sh.id) }
            }
            l.home.untrackedKwh?.takeIf { it > 0.0005 }?.let { u ->
                ShareRow(stringResource(R.string.home_energy_untracked), u, if (baseKwh > 0) u / baseKwh else 0.0, 0.0, locale, muted = true, onClick = null)
            }
        }
    }
    if (l.home.meter != null) {
        Text(
            stringResource(R.string.home_energy_meter_hint),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
    }
}

/** Cihaz payı satırı: ad, kWh ve yüzde; altında oranlı çubuk. */
@Composable
private fun ShareRow(
    name: String, kwh: Double, share: Double, cost: Double, locale: java.util.Locale,
    muted: Boolean = false, onClick: (() -> Unit)?,
) {
    val scheme = MaterialTheme.colorScheme
    val pct = (share * 100).roundToInt()
    Column(
        Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick).tvFocusRing() else Modifier)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis,
                color = if (muted) scheme.onSurfaceVariant else scheme.onSurface, modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                DailySummary.formatKwh(kwh) + (if (cost > 0) " · " + EnergyFormat.money(cost, locale) else "") + " · $pct%",
                style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                color = if (muted) scheme.onSurfaceVariant else scheme.onSurface,
            )
        }
        Box(Modifier.padding(top = 6.dp).fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(tileColor())) {
            Box(
                Modifier.fillMaxWidth(share.toFloat().coerceIn(0.01f, 1f)).height(6.dp).clip(RoundedCornerShape(3.dp))
                    .background(if (muted) scheme.outline else scheme.primary),
            )
        }
    }
}
