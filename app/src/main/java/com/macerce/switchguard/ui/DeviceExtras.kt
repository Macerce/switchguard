package com.macerce.switchguard.ui

import com.macerce.switchguard.core.Cloud
import android.content.ClipData
import android.content.ClipboardManager
import android.text.format.DateFormat as AndroidDateFormat
import android.widget.Toast
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
import com.macerce.switchguard.core.DeviceSnapshot
import com.macerce.switchguard.core.DeviceTimers
import com.macerce.switchguard.core.RepeatTimer
import com.macerce.switchguard.core.TimerEntry
import com.macerce.switchguard.data.MetricsLog
import com.macerce.switchguard.service.DailySummary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import java.util.UUID

fun formatWatts(w: Double): String =
    if (w >= 1000) String.format(Locale.getDefault(), "%.2f kW", w / 1000)
    else String.format(Locale.getDefault(), "%.0f W", w)

fun formatNumber(v: Double): String =
    if (v % 1.0 == 0.0) v.toLong().toString() else String.format(Locale.US, "%.1f", v)

// ---------------------------------------------------------------- enerji

/** Enerji ölçen cihazlarda: anlık değerler, bugün/bu ay tüketim ve son 7 günün grafiği. */
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
    val days = remember(metricsVersion, today) {
        (6 downTo 0).map { back ->
            val from = Calendar.getInstance().apply { timeInMillis = today; add(Calendar.DAY_OF_MONTH, -back) }.timeInMillis
            val to = Calendar.getInstance().apply { timeInMillis = from; add(Calendar.DAY_OF_MONTH, 1) }.timeInMillis
            from to metrics.kwh(device.id, from, minOf(to, now))
        }
    }
    val month = remember(metricsVersion, today) {
        val from = Calendar.getInstance().apply { timeInMillis = today; set(Calendar.DAY_OF_MONTH, 1) }.timeInMillis
        metrics.kwh(device.id, from, now)
    }
    val hasData = remember(metricsVersion) { metrics.hasPowerData(device.id) }

    SectionTitle(stringResource(R.string.section_energy))
    SectionCard {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Metric(stringResource(R.string.energy_power), device.power?.let(::formatWatts) ?: "–", big = true)
            Metric(stringResource(R.string.energy_voltage), device.voltage?.let { String.format(Locale.getDefault(), "%.0f V", it) } ?: "–")
            Metric(stringResource(R.string.energy_current), device.current?.let { String.format(Locale.getDefault(), "%.2f A", it) } ?: "–")
        }
        if (device.channelPower.isNotEmpty()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.surface)
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                device.channelPower.toSortedMap().forEach { (ch, w) ->
                    Metric(stringResource(R.string.channel_n, ch + 1), formatWatts(w))
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.surface)
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Metric(stringResource(R.string.energy_today), DailySummary.formatKwh(days.last().second))
            Metric(stringResource(R.string.energy_month), DailySummary.formatKwh(month))
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.surface)
        if (!hasData) {
            Text(
                stringResource(R.string.energy_no_data),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
        } else {
            WeekBars(days)
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

/** Son 7 gün: gün başına yatay çubuk + değer (okunaklı, eksen gerektirmez). */
@Composable
private fun WeekBars(days: List<Pair<Long, Double>>) {
    val max = days.maxOf { it.second }.takeIf { it > 0 } ?: 1.0
    val dayFormat = remember { AndroidDateFormat.getBestDateTimePattern(Locale.getDefault(), "EEE d") }
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.energy_last_7_days), style = MaterialTheme.typography.labelLarge)
        days.forEach { (start, kwh) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    AndroidDateFormat.format(dayFormat, start).toString(),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.width(64.dp),
                )
                Box(Modifier.weight(1f).height(14.dp)) {
                    Box(
                        Modifier.fillMaxHeight()
                            .fillMaxWidth((kwh / max).toFloat().coerceIn(0.01f, 1f))
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                    )
                }
                Text(
                    DailySummary.formatKwh(kwh),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.width(76.dp).padding(start = 8.dp),
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
                    if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.surface)
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
                            Modifier.clickable(enabled = !busy) { editing = t },
                        ) {
                            Switch(checked = t.enabled, enabled = !busy, onCheckedChange = { save(t.id, t.copy(enabled = it)) })
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.surface)
                val full = list.size >= DeviceTimers.MAX_TIMERS
                InfoRow(
                    Icons.Rounded.Add,
                    stringResource(R.string.timer_add),
                    if (full) stringResource(R.string.timer_full, DeviceTimers.MAX_TIMERS) else stringResource(R.string.timer_add_desc),
                    Modifier.clickable(enabled = !busy && !full) {
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
    var t by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (isNew) R.string.timer_add else R.string.timer_edit)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                InfoRow(
                    Icons.Rounded.Schedule, stringResource(R.string.timer_time), formatMinute(context, t.hour * 60 + t.minute),
                    Modifier.clickable { pickTime(context, t.hour * 60 + t.minute) { m -> t = t.copy(hour = m / 60, minute = m % 60) } },
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
            TextButton(enabled = t.days.isNotEmpty(), onClick = { onSave(t) }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            Row {
                if (!isNew) TextButton(onClick = onDelete) { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
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
