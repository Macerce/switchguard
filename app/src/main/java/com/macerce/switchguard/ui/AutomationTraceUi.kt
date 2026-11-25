package com.macerce.switchguard.ui

import android.text.format.DateFormat
import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.LocalLaundryService
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PowerOff
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.macerce.switchguard.R
import com.macerce.switchguard.core.ActionKind
import com.macerce.switchguard.core.Automation
import com.macerce.switchguard.core.AutomationTemplate
import com.macerce.switchguard.core.AutomationTraces
import com.macerce.switchguard.core.DeviceSnapshot
import com.macerce.switchguard.core.StepResult
import com.macerce.switchguard.core.TraceEntry
import com.macerce.switchguard.core.TraceOutcome
import com.macerce.switchguard.data.TraceLog
import com.macerce.switchguard.ui.theme.Brand
import com.macerce.switchguard.ui.theme.LocalDarkTheme
import com.macerce.switchguard.ui.theme.tileColor

// ---------------------------------------------------------------- ortak alt sayfa

/** Telefonda alttan açılan sayfa, TV'de pencere (kumandayla alt sayfa kullanışsız). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AdaptiveSheet(title: String, onDismiss: () -> Unit, footer: (@Composable () -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    if (LocalIsTv.current) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(title) },
            text = { Column(Modifier.verticalScroll(rememberScrollState()), content = content) },
            confirmButton = { TextButton(onClick = onDismiss, modifier = Modifier.tvFocusRing()) { Text(stringResource(R.string.action_close)) } },
            dismissButton = footer,
        )
        return
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
            content()
            footer?.let { Spacer(Modifier.height(8.dp)); it() }
        }
    }
}

// ---------------------------------------------------------------- kural izi

private fun TraceOutcome.label(): Int = when (this) {
    TraceOutcome.RAN -> R.string.trace_ran
    TraceOutcome.DELAYED -> R.string.trace_delayed
    TraceOutcome.BLOCKED_WINDOW -> R.string.trace_blocked_window
    TraceOutcome.BLOCKED_CONDITION -> R.string.trace_blocked_condition
    TraceOutcome.BLOCKED_PRESENCE -> R.string.trace_blocked_presence
    TraceOutcome.THROTTLED -> R.string.trace_throttled
}

@Composable
private fun TraceOutcome.color(failed: Boolean): Color = when {
    this == TraceOutcome.RAN && failed -> Brand.alarm
    this == TraceOutcome.RAN -> Hue.GREEN.color()
    this == TraceOutcome.DELAYED -> Hue.BLUE.color()
    else -> Hue.AMBER.color()
}

@Composable
private fun Hue.color(): Color = if (LocalDarkTheme.current) dark else light

private fun TraceEntry.failed() = steps.any { it.result == StepResult.FAILED || it.result == StepResult.MISSING }

/** "14:02" (bugün) ya da "24 Kas 14:02". */
@Composable
private fun clock(at: Long): String {
    val context = LocalContext.current
    val time = DateFormat.getTimeFormat(context).format(java.util.Date(at))
    return if (DateUtils.isToday(at)) time
    else DateUtils.formatDateTime(context, at, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_MONTH or DateUtils.FORMAT_NO_YEAR) + " " + time
}

/** Kartın altındaki tek satır: "Son: 14:02 · Çalıştı". */
@Composable
internal fun LastRunLine(automationId: String) {
    val context = LocalContext.current
    remember { TraceLog.init(context) }
    val all by TraceLog.flow.collectAsState()
    val last = remember(all, automationId) { all.lastOrNull { it.automationId == automationId } } ?: return
    Spacer(Modifier.height(4.dp))
    Text(
        stringResource(R.string.trace_last, clock(last.lastAt), stringResource(last.outcome.label())),
        style = MaterialTheme.typography.labelMedium,
        color = last.outcome.color(last.failed()),
    )
}

/** Kuralın çalışma geçmişi: her tetiklenişte ne oldu, hangi koşul tuttu/tutmadı, eylemlerin sonucu. */
@Composable
internal fun TraceSheet(a: Automation, devices: List<DeviceSnapshot>, onDismiss: () -> Unit) {
    val context = LocalContext.current
    remember { TraceLog.init(context) }
    val all by TraceLog.flow.collectAsState()
    val list = remember(all, a.id) { AutomationTraces.of(all, a.id) }
    AdaptiveSheet(
        stringResource(R.string.trace_title),
        onDismiss,
        footer = if (list.isEmpty()) null else ({
            TextButton(onClick = { TraceLog.clear(context, a.id) }, modifier = Modifier.tvFocusRing()) { Text(stringResource(R.string.trace_clear)) }
        }),
    ) {
        Text(describe(a, devices).first, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        if (list.isEmpty()) {
            Text(stringResource(R.string.trace_empty), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(vertical = 16.dp))
            return@AdaptiveSheet
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            list.forEach { e -> TraceCard(a, e, devices) }
        }
    }
}

@Composable
private fun TraceCard(a: Automation, e: TraceEntry, devices: List<DeviceSnapshot>) {
    val color = e.outcome.color(e.failed())
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(tileColor()).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                when {
                    e.outcome == TraceOutcome.RAN && e.failed() -> Icons.Rounded.Cancel
                    e.outcome == TraceOutcome.RAN -> Icons.Rounded.CheckCircle
                    e.outcome == TraceOutcome.DELAYED -> Icons.Rounded.HourglassTop
                    else -> Icons.Rounded.Block
                },
                null, tint = color, modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(stringResource(e.outcome.label()), style = MaterialTheme.typography.titleSmall, color = color, modifier = Modifier.weight(1f))
            Text(clock(e.at), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (e.count > 1) {
            Text(stringResource(R.string.trace_repeated, e.count, clock(e.lastAt)), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        e.value?.let { v ->
            val t = a.trigger
            val unit = when {
                t.kind.usesWatts -> " W"
                t.kind.usesTemp -> " °C"
                t.kind.usesSensorValue -> t.sensor?.unit?.let { " $it" }.orEmpty()
                else -> ""
            }
            Text(stringResource(R.string.trace_value, formatNumber(Math.round(v * 10) / 10.0) + unit), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        // Koşullar: her biri ✓ / ✗ (kural sonradan düzenlendiyse sayı tutmayabilir; o zaman gösterilmez).
        if (e.conds.isNotEmpty() && e.conds.size == a.conditions.size) {
            Text(stringResource(R.string.trace_conditions), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 4.dp))
            a.conditions.zip(e.conds).forEach { (c, held) ->
                ResultRow(held, conditionText(c, devices))
            }
        }
        if (e.steps.isNotEmpty()) {
            Text(stringResource(R.string.trace_actions), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 4.dp))
            e.steps.sortedBy { it.step }.forEach { s ->
                val act = a.allActions.getOrNull(s.step)
                val what = act?.let { actionText(it, devices) } ?: "#${s.step + 1}"
                val result = stringResource(
                    when (s.result) {
                        StepResult.DONE -> R.string.trace_step_done
                        StepResult.FAILED -> R.string.trace_step_failed
                        StepResult.MISSING -> R.string.trace_step_missing
                        StepResult.SILENCED -> R.string.trace_step_silenced
                    }
                )
                val extra = when {
                    s.error.isEmpty() -> ""
                    act?.kind == ActionKind.SAVE_STATE -> " · " + stringResource(R.string.trace_saved_n, s.error)
                    else -> " · " + s.error
                }
                ResultRow(s.result == StepResult.DONE, "$what — $result$extra")
            }
        }
    }
}

@Composable
private fun ResultRow(ok: Boolean, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Text(if (ok) "✓" else "✗", color = if (ok) Hue.GREEN.color() else Brand.alarm, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

// ---------------------------------------------------------------- şablonlar

fun AutomationTemplate.title(): Int = when (this) {
    AutomationTemplate.FORGOTTEN_OFF -> R.string.tpl_forgotten_off
    AutomationTemplate.ON_TOO_LONG_NOTIFY -> R.string.tpl_on_too_long
    AutomationTemplate.OFFLINE_ALARM -> R.string.tpl_offline_alarm
    AutomationTemplate.MACHINE_DONE -> R.string.tpl_machine_done
    AutomationTemplate.APPLIANCE_STOPPED -> R.string.tpl_appliance_stopped
    AutomationTemplate.HIGH_POWER -> R.string.tpl_high_power
    AutomationTemplate.PAUSE_AND_RESTORE -> R.string.tpl_pause_restore
    AutomationTemplate.SUNSET_ON -> R.string.tpl_sunset_on
    AutomationTemplate.NIGHT_OFF -> R.string.tpl_night_off
    AutomationTemplate.MORNING_ON -> R.string.tpl_morning_on
}

private fun AutomationTemplate.desc(): Int = when (this) {
    AutomationTemplate.FORGOTTEN_OFF -> R.string.tpl_forgotten_off_desc
    AutomationTemplate.ON_TOO_LONG_NOTIFY -> R.string.tpl_on_too_long_desc
    AutomationTemplate.OFFLINE_ALARM -> R.string.tpl_offline_alarm_desc
    AutomationTemplate.MACHINE_DONE -> R.string.tpl_machine_done_desc
    AutomationTemplate.APPLIANCE_STOPPED -> R.string.tpl_appliance_stopped_desc
    AutomationTemplate.HIGH_POWER -> R.string.tpl_high_power_desc
    AutomationTemplate.PAUSE_AND_RESTORE -> R.string.tpl_pause_restore_desc
    AutomationTemplate.SUNSET_ON -> R.string.tpl_sunset_on_desc
    AutomationTemplate.NIGHT_OFF -> R.string.tpl_night_off_desc
    AutomationTemplate.MORNING_ON -> R.string.tpl_morning_on_desc
}

private fun AutomationTemplate.icon(): Pair<ImageVector, Hue> = when (this) {
    AutomationTemplate.FORGOTTEN_OFF -> Icons.Rounded.PowerOff to Hue.SLATE
    AutomationTemplate.ON_TOO_LONG_NOTIFY -> Icons.Rounded.NotificationsActive to Hue.BLUE
    AutomationTemplate.OFFLINE_ALARM -> Icons.Rounded.WifiOff to Hue.RED
    AutomationTemplate.MACHINE_DONE -> Icons.Rounded.LocalLaundryService to Hue.TEAL
    AutomationTemplate.APPLIANCE_STOPPED -> Icons.Rounded.AcUnit to Hue.RED
    AutomationTemplate.HIGH_POWER -> Icons.Rounded.Bolt to Hue.ORANGE
    AutomationTemplate.PAUSE_AND_RESTORE -> Icons.Rounded.Restore to Hue.AMBER
    AutomationTemplate.SUNSET_ON -> Icons.Rounded.WbTwilight to Hue.PURPLE
    AutomationTemplate.NIGHT_OFF -> Icons.Rounded.Bedtime to Hue.INDIGO
    AutomationTemplate.MORNING_ON -> Icons.Rounded.WbSunny to Hue.AMBER
}

/** "Ekle": boş kural ya da hazır şablon. [onPick] null = boş kural. Uygun cihazı olmayan şablon gösterilmez. */
@Composable
internal fun TemplatePicker(devices: List<DeviceSnapshot>, onDismiss: () -> Unit, onPick: (AutomationTemplate?) -> Unit) {
    val available = remember(devices) { AutomationTemplate.entries.filter { it.pickDevice(devices) != null } }
    AdaptiveSheet(stringResource(R.string.template_title), onDismiss) {
        TemplateRow(Icons.Rounded.Add, Hue.SLATE, stringResource(R.string.template_blank), stringResource(R.string.template_blank_desc)) { onPick(null) }
        if (available.isNotEmpty()) {
            Text(stringResource(R.string.template_section), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
        }
        available.forEach { t ->
            val (icon, hue) = t.icon()
            TemplateRow(icon, hue, stringResource(t.title()), stringResource(t.desc())) { onPick(t) }
        }
    }
}

@Composable
private fun TemplateRow(icon: ImageVector, hue: Hue, title: String, desc: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp).tvFocusRing(shape).clip(shape).background(tileColor()).clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HueIcon(icon, hue, size = 36.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

