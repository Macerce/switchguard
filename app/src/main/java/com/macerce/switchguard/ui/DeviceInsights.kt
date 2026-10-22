package com.macerce.switchguard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.macerce.switchguard.R
import com.macerce.switchguard.core.DeviceSnapshot
import com.macerce.switchguard.core.EventType
import com.macerce.switchguard.core.Signal
import com.macerce.switchguard.core.Standby
import com.macerce.switchguard.data.EventLog
import com.macerce.switchguard.data.MetricsLog
import com.macerce.switchguard.data.Store
import com.macerce.switchguard.service.DailySummary
import com.macerce.switchguard.ui.theme.StatusColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Bağlantı: Wi-Fi sinyali (çubuklar, dBm, ne anlama geldiği) ve son 7 günün kopmaları. Sık kopmada olası neden ve öneri.
 * Sinyal bildirmeyen ve hiç kopmamış cihazda gösterilmez.
 */
@Composable
fun ConnectionCard(device: DeviceSnapshot) {
    val context = LocalContext.current
    val log = remember { EventLog.get(context) }
    val logVersion by log.version.collectAsStateWithLifecycle()
    val drops = remember(logVersion, device.id) {
        log.count(device.id, EventType.WENT_OFFLINE.name, System.currentTimeMillis() - 7 * 86_400_000L)
    }
    val rssi = device.rssi
    if (rssi == null && drops == 0) return
    SectionTitle(stringResource(R.string.connection_title))
    SectionCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                HueIcon(Icons.Rounded.Wifi, Hue.BLUE, size = 32.dp)
                Column(Modifier.padding(start = 12.dp).weight(1f)) {
                    if (rssi != null) {
                        val q = Signal.quality(rssi)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SignalBars(q.bars)
                            Text(
                                stringResource(R.string.signal_value, stringResource(q.label()), rssi),
                                style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    } else {
                        Text(stringResource(R.string.signal_unknown), style = MaterialTheme.typography.titleSmall)
                    }
                    Text(
                        pluralStringResource(R.plurals.drops_week, drops, drops),
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            val advice = when (Signal.dropCause(drops, rssi)) {
                Signal.Cause.WEAK_SIGNAL -> R.string.drops_cause_signal
                Signal.Cause.POWER_OR_ROUTER -> R.string.drops_cause_power
                Signal.Cause.UNKNOWN -> R.string.drops_cause_unknown
                null -> if (rssi != null && Signal.quality(rssi) == Signal.Quality.WEAK) R.string.signal_weak_hint else null
            }
            if (advice != null) Text(stringResource(advice), style = MaterialTheme.typography.bodyMedium, color = StatusColors.warning)
            Text(stringResource(R.string.signal_explain), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Dört çubuklu sinyal göstergesi. */
@Composable
fun SignalBars(bars: Int) {
    val on = MaterialTheme.colorScheme.primary
    val off = MaterialTheme.colorScheme.outlineVariant
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        (1..4).forEach { i ->
            Box(Modifier.width(4.dp).height((4 + i * 3).dp).background(if (i <= bars) on else off, RoundedCornerShape(1.dp)))
        }
    }
}

fun Signal.Quality.label(): Int = when (this) {
    Signal.Quality.EXCELLENT -> R.string.signal_excellent
    Signal.Quality.GOOD -> R.string.signal_good
    Signal.Quality.FAIR -> R.string.signal_fair
    Signal.Quality.WEAK -> R.string.signal_weak
}

/**
 * Bekleme (temel) tüketimi: son 7 günün en düşük saatlik ortalaması, aylık karşılığı ve maliyeti; ne anlama geldiği.
 * Yeterli ölçüm yoksa ya da önemsizse gösterilmez.
 */
@Composable
fun StandbyCard(device: DeviceSnapshot) {
    val context = LocalContext.current
    val locale = LocalResources.current.configuration.locales[0]
    val metrics = remember { MetricsLog.get(context) }
    val result by produceState<Standby.Result?>(null, device.id) {
        value = withContext(Dispatchers.IO) { runCatching { metrics.standby(device.id) }.getOrNull() }
    }
    val r = result ?: return
    val tariff = remember { Store.get(context).tariffFor(device.id) }
    val cost = remember(r) { if (tariff.hasPrice) Standby.monthlyCost(r.watts, tariff, System.currentTimeMillis()) else 0.0 }
    SectionTitle(stringResource(R.string.standby_title))
    SectionCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                HueIcon(Icons.Rounded.PowerSettingsNew, Hue.AMBER, size = 32.dp)
                Column(Modifier.padding(start = 12.dp)) {
                    Text(formatWatts(r.watts), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.standby_month, DailySummary.formatKwh(r.monthlyKwh)) +
                            (if (cost > 0) " · ≈ " + EnergyFormat.money(cost, locale) else ""),
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(stringResource(R.string.standby_explain), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
