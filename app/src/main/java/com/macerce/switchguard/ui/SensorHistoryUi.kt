package com.macerce.switchguard.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.macerce.switchguard.R
import com.macerce.switchguard.core.DeviceSnapshot
import com.macerce.switchguard.core.SensorHistory
import com.macerce.switchguard.core.SensorKind
import com.macerce.switchguard.core.SensorRange
import com.macerce.switchguard.core.SensorStats
import com.macerce.switchguard.data.MetricsLog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Sayısal sensörü olan cihazlarda: sıcaklık/nem/ışık/pil geçmişi (24 saat / 7 gün / 30 gün), en düşük/yüksek/ortalama. */
@Composable
fun SensorHistorySection(device: DeviceSnapshot) {
    val kinds = device.sensors.keys.filter { SensorHistory.recordable(it) }.sortedBy { it.ordinal }
    if (kinds.isEmpty()) return
    val context = LocalContext.current
    val locale = LocalResources.current.configuration.locales[0]
    val metrics = remember { MetricsLog.get(context) }
    val version by metrics.version.collectAsStateWithLifecycle()
    var kind by rememberSaveable(device.id) { mutableStateOf(kinds.first()) }
    if (kind !in kinds) kind = kinds.first()
    var range by rememberSaveable { mutableStateOf(SensorRange.DAY) }
    var selected by remember(kind, range) { mutableStateOf<Int?>(null) }
    // (yüklendi mi, özet): özet null = bu aralıkta kayıt yok.
    val result by produceState<Pair<Boolean, SensorStats?>>(false to null, device.id, kind, range, version) {
        value = true to withContext(Dispatchers.IO) { metrics.sensorStats(device.id, kind, range) }
    }

    SectionTitle(stringResource(R.string.sensor_history))
    SectionCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (kinds.size > 1) {
                OptionChips(kinds, kind, { it.icon() }, { stringResource(it.label()) }) { kind = it }
            }
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SensorRange.entries.forEachIndexed { i, r ->
                    SegmentedButton(
                        selected = range == r,
                        onClick = { range = r },
                        shape = SegmentedButtonDefaults.itemShape(i, SensorRange.entries.size),
                    ) {
                        Text(stringResource(when (r) {
                            SensorRange.DAY -> R.string.sensor_range_day
                            SensorRange.WEEK -> R.string.sensor_range_week
                            SensorRange.MONTH -> R.string.sensor_range_month
                        }))
                    }
                }
            }
            val s = result.second
            when {
                !result.first -> Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                s == null -> Text(
                    stringResource(R.string.sensor_history_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                else -> {
                    val timeFormat = remember(range, locale) { SimpleDateFormat(if (range == SensorRange.DAY) "HH:mm" else "d MMM HH:mm", locale) }
                    SensorChart(s, kind, range, locale, selected) { selected = if (selected == it) null else it }
                    val sel = selected?.let { s.points.getOrNull(it) }
                    if (sel != null) {
                        Text(
                            timeFormat.format(Date(sel.start)) + ": " + formatSensor(kind, round1(sel.avg)) +
                                " (" + formatSensor(kind, round1(sel.min)) + " – " + formatSensor(kind, round1(sel.max)) + ")",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatCell(stringResource(R.string.sensor_hist_min), formatSensor(kind, round1(s.min.value)), timeFormat.format(Date(s.min.time)))
                        StatCell(stringResource(R.string.sensor_hist_max), formatSensor(kind, round1(s.max.value)), timeFormat.format(Date(s.max.time)))
                        StatCell(stringResource(R.string.sensor_hist_avg), formatSensor(kind, round1(s.avg)), null)
                    }
                }
            }
        }
    }
}

private fun round1(v: Double) = Math.round(v * 10) / 10.0

@Composable
private fun StatCell(label: String, value: String, sub: String?) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        if (sub != null) Text(sub, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Ortalama çizgisi + en düşük–en yüksek bandı; dokunulan dilim işaretlenir. */
@Composable
private fun SensorChart(s: SensorStats, kind: SensorKind, range: SensorRange, locale: Locale, selected: Int?, onSelect: (Int) -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    val band = primary.copy(alpha = 0.18f)
    val grid = MaterialTheme.colorScheme.outlineVariant
    val n = s.points.size
    // Düz çizgi görünmesin: değer aralığı en az türün bir adımı kadar.
    val lo0 = s.min.value
    val hi0 = s.max.value
    val pad = maxOf((hi0 - lo0) * 0.1, if (kind == SensorKind.TEMPERATURE) 0.5 else 1.0)
    val lo = lo0 - pad
    val hi = hi0 + pad
    val axis = remember(range, locale) { SimpleDateFormat(if (range == SensorRange.DAY) "HH:mm" else "d MMM", locale) }
    Column {
        Text(formatSensor(kind, round1(hi0)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Canvas(
            Modifier.fillMaxWidth().height(140.dp).pointerInput(n) {
                detectTapGestures { o -> onSelect((o.x / size.width * n).toInt().coerceIn(0, n - 1)) }
            },
        ) {
            val w = size.width
            val h = size.height
            fun x(i: Int) = (i + 0.5f) / n * w
            fun y(v: Double) = (h - ((v - lo) / (hi - lo)) * h).toFloat()
            drawLine(grid, Offset(0f, y(hi0)), Offset(w, y(hi0)), strokeWidth = 1f)
            drawLine(grid, Offset(0f, y(lo0)), Offset(w, y(lo0)), strokeWidth = 1f)
            // Ölçüm olmayan dilimlerde çizgi kesilir.
            var i = 0
            while (i < n) {
                if (s.points[i] == null) { i++; continue }
                var j = i
                while (j + 1 < n && s.points[j + 1] != null) j++
                val run = (i..j).map { it to s.points[it]!! }
                val area = Path().apply {
                    run.forEachIndexed { k, (idx, p) -> if (k == 0) moveTo(x(idx), y(p.max)) else lineTo(x(idx), y(p.max)) }
                    run.asReversed().forEach { (idx, p) -> lineTo(x(idx), y(p.min)) }
                    close()
                }
                drawPath(area, band)
                if (run.size == 1) {
                    drawCircle(primary, 3.dp.toPx(), Offset(x(i), y(run[0].second.avg)))
                } else {
                    val line = Path().apply {
                        run.forEachIndexed { k, (idx, p) -> if (k == 0) moveTo(x(idx), y(p.avg)) else lineTo(x(idx), y(p.avg)) }
                    }
                    drawPath(line, primary, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
                i = j + 1
            }
            selected?.let { sel ->
                drawLine(primary.copy(alpha = 0.5f), Offset(x(sel), 0f), Offset(x(sel), h), strokeWidth = 1.dp.toPx())
                s.points.getOrNull(sel)?.let { drawCircle(primary, 4.dp.toPx(), Offset(x(sel), y(it.avg))) }
            }
        }
        Text(formatSensor(kind, round1(lo0)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            val mid = s.from + (s.to - s.from) / 2
            listOf(s.from, mid, s.to).forEach {
                Text(axis.format(Date(it)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
