package com.macerce.switchguard.ui

import android.text.format.DateUtils
import android.widget.Toast
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AllInclusive
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeviceHub
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.HourglassBottom
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LowPriority
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PowerOff
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.ViewTimeline
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.macerce.switchguard.R
import com.macerce.switchguard.core.Cycle
import com.macerce.switchguard.core.CycleEnd
import com.macerce.switchguard.core.CycleEngine
import com.macerce.switchguard.core.CyclePreset
import com.macerce.switchguard.core.CycleRun
import com.macerce.switchguard.core.CycleStart
import com.macerce.switchguard.core.CycleStep
import com.macerce.switchguard.core.CycleTarget
import com.macerce.switchguard.core.DeviceSnapshot
import com.macerce.switchguard.core.PresenceRule
import com.macerce.switchguard.service.MonitorService
import com.macerce.switchguard.ui.theme.raisedOnTile
import com.macerce.switchguard.ui.theme.tileColor
import kotlinx.coroutines.delay
import java.util.UUID

/** "1 sa 5 dk", "45 sn". */
@Composable
fun cycleDuration(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    val parts = buildList {
        if (h > 0) add("$h ${stringResource(R.string.cycle_unit_h)}")
        if (m > 0) add("$m ${stringResource(R.string.cycle_unit_m)}")
        if (s > 0 || isEmpty()) add("$s ${stringResource(R.string.cycle_unit_s)}")
    }
    return parts.joinToString(" ")
}

/** Geri sayım: "1:05:09", "4:07". */
private fun clock(ms: Long): String {
    val t = (ms.coerceAtLeast(0) + 999) / 1000
    val h = t / 3600
    val m = (t % 3600) / 60
    val s = t % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

private fun CyclePreset.icon() = when (this) {
    CyclePreset.COUNTDOWN -> Icons.Rounded.HourglassBottom
    CyclePreset.ON_FOR -> Icons.Rounded.Timer
    CyclePreset.BLINK -> Icons.Rounded.Repeat
    CyclePreset.CHASE -> Icons.Rounded.LowPriority
    CyclePreset.CUSTOM -> Icons.Rounded.ViewTimeline
}

private fun CyclePreset.title() = when (this) {
    CyclePreset.COUNTDOWN -> R.string.cycle_preset_countdown
    CyclePreset.ON_FOR -> R.string.cycle_preset_on_for
    CyclePreset.BLINK -> R.string.cycle_preset_blink
    CyclePreset.CHASE -> R.string.cycle_preset_chase
    CyclePreset.CUSTOM -> R.string.cycle_preset_custom
}

private fun CyclePreset.desc() = when (this) {
    CyclePreset.COUNTDOWN -> R.string.cycle_desc_countdown
    CyclePreset.ON_FOR -> R.string.cycle_desc_on_for
    CyclePreset.BLINK -> R.string.cycle_desc_blink
    CyclePreset.CHASE -> R.string.cycle_desc_chase
    CyclePreset.CUSTOM -> R.string.cycle_desc_custom
}

/** Otomasyon sekmesindeki döngü şeridi: oynat/durdur düğmesi, süren döngüde canlı geri sayım, sonda "Döngü ekle". */
@Composable
fun CyclesStrip(cycles: List<Cycle>, runs: Map<String, CycleRun>, onEdit: (String) -> Unit) {
    val context = LocalContext.current
    val resources = androidx.compose.ui.platform.LocalResources.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    if (runs.isNotEmpty()) {
        LaunchedEffect(runs) {
            while (true) {
                now = System.currentTimeMillis()
                delay(1000)
            }
        }
    }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(cycles, key = { it.id }) { c ->
            val run = runs[c.id]
            val shape = RoundedCornerShape(20.dp)
            val scheme = MaterialTheme.colorScheme
            Column(
                Modifier.width(184.dp).clip(shape)
                    .background(if (run != null) scheme.primaryContainer else tileColor())
                    .tvFocusRing(shape)
                    .clickable { onEdit(c.id) }
                    .padding(start = 14.dp, top = 12.dp, bottom = 10.dp, end = 4.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HueIcon(c.preset.icon(), if (run != null) Hue.GREEN else Hue.TEAL, size = 30.dp)
                    Spacer(Modifier.weight(1f))
                    IconButton(
                        modifier = Modifier.tvFocusRing(),
                        onClick = {
                            MonitorService.cycle(context, c.id, start = run == null)
                            Toast.makeText(
                                context,
                                resources.getString(if (run == null) R.string.cycle_started_toast else R.string.cycle_stopped_toast, c.name),
                                Toast.LENGTH_SHORT,
                            ).show()
                        },
                    ) {
                        Icon(
                            if (run == null) Icons.Rounded.PlayArrow else Icons.Rounded.Stop,
                            stringResource(if (run == null) R.string.cycle_start else R.string.cycle_stop),
                            tint = scheme.primary,
                        )
                    }
                }
                Text(c.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (run != null) {
                    Text(
                        stringResource(R.string.cycle_left, clock(run.stepEndsAt - now)),
                        style = MaterialTheme.typography.titleMedium, color = scheme.primary,
                    )
                    val round = if (c.repeat > 0) stringResource(R.string.cycle_round_of, run.round + 1, c.repeat)
                        else stringResource(R.string.cycle_round_n, run.round + 1)
                    Text(
                        if (c.steps.size > 1) stringResource(R.string.cycle_running, run.step + 1, c.steps.size, round) else round,
                        style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, maxLines = 1,
                    )
                } else {
                    Text(
                        cycleScheduleText(c, now),
                        style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        item {
            val shape = RoundedCornerShape(20.dp)
            Box(
                Modifier.width(120.dp).heightIn(min = 112.dp).clip(shape).background(tileColor()).tvFocusRing(shape)
                    .clickable { onEdit(NEW_AUTOMATION) },
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.Add, null, tint = MaterialTheme.colorScheme.primary)
                    Text(stringResource(R.string.cycle_add), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun cycleScheduleText(c: Cycle, now: Long): String {
    val context = LocalContext.current
    if (c.start == CycleStart.MANUAL) return stringResource(R.string.cycle_idle_manual)
    if (!c.enabled) return stringResource(R.string.cycle_disabled)
    val next = remember(c, now / 60_000) { CycleEngine.nextStart(c, now) } ?: return stringResource(R.string.cycle_idle_manual)
    val flags = DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_ABBREV_WEEKDAY
    val text = if (DateUtils.isToday(next)) DateUtils.formatDateTime(context, next, DateUtils.FORMAT_SHOW_TIME)
        else DateUtils.formatDateTime(context, next, flags)
    return stringResource(R.string.cycle_next_at, text)
}

/** Süre girişi: sayı + birim (sn / dk / sa). Değer saniyedir. */
@Composable
private fun DurationField(label: String, seconds: Int, onChange: (Int) -> Unit) {
    val units = listOf(1, 60, 3600)
    fun unitFor(sec: Int) = units.lastOrNull { sec >= it && sec % it == 0 } ?: 60
    var unit by remember { mutableIntStateOf(unitFor(seconds)) }
    var text by remember { mutableStateOf((seconds / unit).toString()) }
    // Değer dışarıdan değiştiyse (adım taşındı, kalıp değişti) alanı ona uydur; yazarken dokunma.
    LaunchedEffect(seconds) {
        if ((text.toIntOrNull() ?: -1) * unit != seconds) {
            unit = unitFor(seconds)
            text = (seconds / unit).toString()
        }
    }
    Column(Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value = text,
                onValueChange = { v ->
                    text = v.filter { it.isDigit() }.take(5)
                    text.toIntOrNull()?.let { onChange((it * unit).coerceAtMost(Cycle.MAX_STEP_SECONDS)) }
                },
                label = { Text(label) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = tvKeyboardOptions(KeyboardOptions(keyboardType = KeyboardType.Number)),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = raisedOnTile(), unfocusedContainerColor = raisedOnTile(),
                    focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                ),
                modifier = Modifier.weight(1f).tvTextField(),
            )
            Spacer(Modifier.width(8.dp))
            SingleChoiceSegmentedButtonRow(Modifier.width(168.dp)) {
                units.forEachIndexed { i, u ->
                    SegmentedButton(
                        selected = unit == u,
                        onClick = {
                            unit = u
                            text.toIntOrNull()?.let { onChange((it * u).coerceAtMost(Cycle.MAX_STEP_SECONDS)) }
                        },
                        shape = SegmentedButtonDefaults.itemShape(i, units.size),
                        icon = {},
                        modifier = Modifier.tvFocusRing(),
                    ) {
                        Text(stringResource(when (u) { 1 -> R.string.cycle_unit_s; 60 -> R.string.cycle_unit_m; else -> R.string.cycle_unit_h }), maxLines = 1)
                    }
                }
            }
        }
    }
}

private fun targetLabel(t: CycleTarget, devices: List<DeviceSnapshot>, resources: android.content.res.Resources): String {
    val d = devices.firstOrNull { it.id == t.deviceId } ?: return t.deviceId
    return if (d.isMultiChannel) resources.getString(R.string.label_channel, d.name, t.channel + 1) else d.name
}

/**
 * Döngü düzenleyici: kalıp (geri sayım, aç-sonra-kapat, aç/kapa, sırayla, özel adımlar), cihazlar, süreler,
 * tekrar, ne zaman başlayacağı (düğme, belirli saat + günler, saat aralığı), bitiş eylemi ve gelişmiş seçenekler.
 */
@Composable
fun CycleEditor(initial: Cycle?, devices: List<DeviceSnapshot>, onSave: (Cycle) -> Unit, onDelete: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val resources = androidx.compose.ui.platform.LocalResources.current
    val timePicker = rememberTimePicker()
    var name by rememberSaveable { mutableStateOf(initial?.name ?: "") }
    var preset by remember { mutableStateOf(initial?.preset ?: CyclePreset.COUNTDOWN) }
    val targets = remember { mutableStateListOf<CycleTarget>().apply { initial?.targets?.let(::addAll) } }
    val steps = remember { mutableStateListOf<CycleStep>().apply { initial?.steps?.let(::addAll) } }
    // Kalıpların süreleri (saniye).
    var mainSec by remember { mutableIntStateOf(initial?.steps?.firstOrNull()?.seconds ?: 30 * 60) }
    var offSec by remember { mutableIntStateOf(initial?.steps?.getOrNull(1)?.seconds ?: 10 * 60) }
    var countdownOn by remember { mutableStateOf(initial?.end == CycleEnd.ON) }
    var repeat by remember { mutableIntStateOf(initial?.repeat?.takeIf { it > 0 } ?: 3) }
    var forever by remember { mutableStateOf(initial?.repeat == 0) }
    var start by remember { mutableStateOf(initial?.start ?: CycleStart.MANUAL) }
    var startMinute by remember { mutableIntStateOf(initial?.startMinute ?: 7 * 60) }
    var endMinute by remember { mutableIntStateOf(initial?.endMinute ?: 22 * 60) }
    var days by remember { mutableIntStateOf(initial?.days ?: com.macerce.switchguard.core.ActiveWindow.ALL_DAYS) }
    var end by remember { mutableStateOf(initial?.end ?: CycleEnd.OFF) }
    var jitter by remember { mutableIntStateOf(initial?.jitterPct ?: 0) }
    var presence by remember { mutableStateOf(initial?.presence) }
    var stopOnManual by remember { mutableStateOf(initial?.stopOnManual ?: false) }
    var enabled by remember { mutableStateOf(initial?.enabled ?: true) }
    var confirmDelete by remember { mutableStateOf(false) }

    val repeats = preset == CyclePreset.BLINK || preset == CyclePreset.CHASE || preset == CyclePreset.CUSTOM
    val repeatValue = if (!repeats) 1 else if (forever) 0 else repeat
    val builtSteps: List<CycleStep> = when (preset) {
        CyclePreset.COUNTDOWN -> CycleEngine.Presets.countdown(targets, mainSec, countdownOn).first
        CyclePreset.ON_FOR -> CycleEngine.Presets.onFor(targets, mainSec).first
        CyclePreset.BLINK -> CycleEngine.Presets.blink(targets, mainSec, offSec, repeatValue).first
        CyclePreset.CHASE -> CycleEngine.Presets.chase(targets, mainSec, repeatValue).first
        CyclePreset.CUSTOM -> steps.map { s -> s.copy(states = s.states.filterKeys { k -> targets.any { it.key == k } }) }
    }
    val builtEnd = when (preset) {
        CyclePreset.COUNTDOWN -> if (countdownOn) CycleEnd.ON else CycleEnd.OFF
        else -> end
    }
    val draft = Cycle(
        id = initial?.id ?: UUID.randomUUID().toString(),
        name = name.trim(),
        enabled = enabled,
        targets = targets.toList(),
        steps = builtSteps,
        repeat = repeatValue,
        start = start,
        startMinute = startMinute,
        endMinute = endMinute,
        days = days,
        end = builtEnd,
        jitterPct = jitter,
        presence = if (start == CycleStart.MANUAL) null else presence,
        stopOnManual = stopOnManual,
        preset = preset,
    )

    Column(Modifier.fillMaxSize()) {
        AppTopBar(
            stringResource(if (initial == null) R.string.cycle_new else R.string.cycle_edit),
            navigationIcon = { BackButton(onBack) }, small = true,
            actions = {
                if (initial != null) {
                    IconButton(modifier = Modifier.tvFocusRing(), onClick = { confirmDelete = true }) {
                        Icon(Icons.Rounded.Delete, stringResource(R.string.action_delete))
                    }
                }
            },
        )
        Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
            TextField(
                value = name, onValueChange = { name = it.take(40) },
                label = { Text(stringResource(R.string.cycle_name)) },
                placeholder = { Text(stringResource(R.string.cycle_name_hint)) },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                keyboardOptions = tvKeyboardOptions(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = tileColor(), unfocusedContainerColor = tileColor(),
                    focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                ),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).tvTextField(),
            )

            // ------------------------------------------------ Kalıp
            StepCard(Icons.Rounded.Repeat, Hue.TEAL, stringResource(R.string.cycle_kind), stringResource(preset.desc())) {
                Spacer(Modifier.height(12.dp))
                OptionChips(CyclePreset.entries, preset, { it.icon() }, { stringResource(it.title()) }) { p ->
                    if (p == CyclePreset.CUSTOM && preset != CyclePreset.CUSTOM) {
                        // Şimdiki kalıbın adımlarıyla başla; ardından elle değiştirilir.
                        steps.clear()
                        steps.addAll(builtSteps.ifEmpty { listOf(CycleStep(mainSec, targets.associate { it.key to true })) })
                    }
                    preset = p
                }
            }

            // ------------------------------------------------ Cihazlar
            StepCard(Icons.Rounded.DeviceHub, Hue.BLUE, stringResource(R.string.cycle_targets), stringResource(R.string.cycle_targets_hint)) {
                Spacer(Modifier.height(8.dp))
                val rows = devices.flatMap { d -> d.switches.keys.sorted().map { CycleTarget(d.id, it) } }
                if (rows.isEmpty()) Text(stringResource(R.string.no_devices), Modifier.padding(8.dp))
                rows.forEach { t ->
                    val checked = t in targets
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).tvFocusRing(RoundedCornerShape(12.dp))
                            .selectable(selected = checked, role = Role.Checkbox, onClick = {
                                if (checked) targets.remove(t) else targets.add(t)
                            })
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = checked, onCheckedChange = null, modifier = Modifier.padding(8.dp))
                        Text(targetLabel(t, devices, resources), style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            // ------------------------------------------------ Süreler / adımlar
            StepCard(Icons.Rounded.Timer, Hue.AMBER, stringResource(if (preset == CyclePreset.CUSTOM) R.string.cycle_steps else R.string.cycle_duration),
                stringResource(R.string.cycle_round_length, cycleDuration(draft.roundSeconds))) {
                when (preset) {
                    CyclePreset.COUNTDOWN -> {
                        DurationField(stringResource(R.string.cycle_duration), mainSec) { mainSec = it }
                        FieldLabel(stringResource(R.string.cycle_after))
                        OptionGrid(listOf(false, true), countdownOn, { if (it) Icons.Rounded.PowerSettingsNew else Icons.Rounded.PowerOff },
                            { stringResource(if (it) R.string.cycle_end_on else R.string.cycle_end_off) }) { countdownOn = it }
                    }
                    CyclePreset.ON_FOR -> DurationField(stringResource(R.string.cycle_on_time), mainSec) { mainSec = it }
                    CyclePreset.BLINK -> {
                        DurationField(stringResource(R.string.cycle_on_time), mainSec) { mainSec = it }
                        DurationField(stringResource(R.string.cycle_off_time), offSec) { offSec = it }
                    }
                    CyclePreset.CHASE -> DurationField(stringResource(R.string.cycle_each_time), mainSec) { mainSec = it }
                    CyclePreset.CUSTOM -> CustomSteps(steps, targets, devices)
                }
                if (builtSteps.any { it.seconds < Cycle.MIN_STEP_SECONDS }) {
                    Text(stringResource(R.string.cycle_min_step), color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
                }
                if (repeats) {
                    FieldLabel(stringResource(R.string.cycle_repeat))
                    ToggleLine(stringResource(if (start == CycleStart.WINDOW) R.string.cycle_repeat_forever_window else R.string.cycle_repeat_forever), forever) { forever = it }
                    if (!forever) {
                        Stepper(stringResource(R.string.cycle_repeat), repeat, REPEAT_STEPS, format = { pluralStringResource(R.plurals.cycle_rounds, it, it) }) { repeat = it }
                    }
                }
            }

            // ------------------------------------------------ Ne zaman
            StepCard(Icons.Rounded.Schedule, Hue.INDIGO, stringResource(R.string.cycle_start_title), "") {
                Spacer(Modifier.height(12.dp))
                OptionChips(CycleStart.entries, start, {
                    when (it) { CycleStart.MANUAL -> Icons.Rounded.TouchApp; CycleStart.AT_TIME -> Icons.Rounded.Event; CycleStart.WINDOW -> Icons.Rounded.Schedule }
                }, {
                    stringResource(when (it) { CycleStart.MANUAL -> R.string.cycle_start_manual; CycleStart.AT_TIME -> R.string.cycle_start_at; CycleStart.WINDOW -> R.string.cycle_start_window })
                }) { start = it }
                if (start != CycleStart.MANUAL) {
                    val startTitle = stringResource(R.string.cycle_start_time)
                    val endTitle = stringResource(R.string.cycle_end_time)
                    Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TimeChip(startTitle, formatMinute(context, startMinute), Modifier.weight(1f)) { timePicker.pick(startTitle, startMinute) { startMinute = it } }
                        if (start == CycleStart.WINDOW) {
                            TimeChip(endTitle, formatMinute(context, endMinute), Modifier.weight(1f)) { timePicker.pick(endTitle, endMinute) { endMinute = it } }
                        }
                    }
                    DayChips(days) { days = it }
                    Text(
                        stringResource(if (start == CycleStart.WINDOW) R.string.cycle_window_note else R.string.cycle_at_note),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    ToggleLine(stringResource(R.string.cycle_enabled), enabled) { enabled = it }
                }
            }

            // ------------------------------------------------ Bitince
            if (preset != CyclePreset.COUNTDOWN) {
                StepCard(Icons.Rounded.Check, Hue.GREEN, stringResource(R.string.cycle_end_title), "") {
                    Spacer(Modifier.height(12.dp))
                    OptionGrid(CycleEnd.entries, end, {
                        when (it) {
                            CycleEnd.OFF -> Icons.Rounded.PowerOff; CycleEnd.ON -> Icons.Rounded.PowerSettingsNew
                            CycleEnd.KEEP -> Icons.Rounded.AllInclusive; CycleEnd.RESTORE -> Icons.Rounded.History
                        }
                    }, {
                        stringResource(when (it) {
                            CycleEnd.OFF -> R.string.cycle_end_off; CycleEnd.ON -> R.string.cycle_end_on
                            CycleEnd.KEEP -> R.string.cycle_end_keep; CycleEnd.RESTORE -> R.string.cycle_end_restore
                        })
                    }) { end = it }
                }
            }

            // ------------------------------------------------ Gelişmiş
            StepCard(Icons.Rounded.Tune, Hue.SLATE, stringResource(R.string.cycle_advanced), "") {
                Stepper(stringResource(R.string.cycle_jitter), jitter, JITTER_STEPS, format = {
                    if (it == 0) stringResource(R.string.cycle_jitter_off) else "±%$it"
                }) { jitter = it }
                Text(stringResource(R.string.cycle_jitter_desc), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp, start = 4.dp))
                if (start != CycleStart.MANUAL) {
                    FieldLabel(stringResource(R.string.cycle_presence))
                    val options = listOf<PresenceRule?>(null, PresenceRule.HOME, PresenceRule.AWAY)
                    OptionChips(options, presence, {
                        when (it) { null -> Icons.Rounded.AllInclusive; PresenceRule.HOME -> Icons.Rounded.Home; PresenceRule.AWAY -> Icons.AutoMirrored.Rounded.DirectionsWalk }
                    }, {
                        stringResource(when (it) { null -> R.string.cycle_presence_any; PresenceRule.HOME -> R.string.presence_home; PresenceRule.AWAY -> R.string.presence_away })
                    }) { presence = it }
                }
                ToggleLine(stringResource(R.string.cycle_stop_manual), stopOnManual) { stopOnManual = it }
                Text(stringResource(R.string.cycle_stop_manual_desc), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp, start = 4.dp))
            }

            Button(
                enabled = draft.valid,
                onClick = { onSave(draft) },
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp).height(56.dp).tvFocusRing(RoundedCornerShape(18.dp)),
            ) {
                Icon(Icons.Rounded.Check, null)
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.action_save))
            }
        }
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.cycle_delete_title), body = name, confirm = stringResource(R.string.action_delete),
            onConfirm = { confirmDelete = false; onDelete() }, onDismiss = { confirmDelete = false },
        )
    }
}

/** Özel adımlar: her adımın süresi ve her hedef için Dokunma / Aç / Kapat; taşı, sil, ekle. */
@Composable
private fun CustomSteps(steps: MutableList<CycleStep>, targets: List<CycleTarget>, devices: List<DeviceSnapshot>) {
    val resources = androidx.compose.ui.platform.LocalResources.current
    steps.forEachIndexed { i, step ->
        androidx.compose.runtime.key(i, steps.size) {
            Column(
                Modifier.fillMaxWidth().padding(top = 12.dp).clip(RoundedCornerShape(16.dp)).background(raisedOnTile()).padding(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.cycle_step_n, i + 1), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    IconButton(enabled = i > 0, modifier = Modifier.tvFocusRing(), onClick = {
                        val s = steps.removeAt(i); steps.add(i - 1, s)
                    }) { Icon(Icons.Rounded.ArrowUpward, stringResource(R.string.cycle_move_up)) }
                    IconButton(enabled = i < steps.size - 1, modifier = Modifier.tvFocusRing(), onClick = {
                        val s = steps.removeAt(i); steps.add(i + 1, s)
                    }) { Icon(Icons.Rounded.ArrowDownward, stringResource(R.string.cycle_move_down)) }
                    IconButton(enabled = steps.size > 1, modifier = Modifier.tvFocusRing(), onClick = { steps.removeAt(i) }) {
                        Icon(Icons.Rounded.Delete, stringResource(R.string.cycle_step_delete))
                    }
                }
                DurationField(stringResource(R.string.cycle_duration), step.seconds) { sec -> steps[i] = steps[i].copy(seconds = sec) }
                targets.forEach { t ->
                    Text(targetLabel(t, devices, resources), style = MaterialTheme.typography.bodyMedium, maxLines = 1,
                        overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 10.dp, bottom = 4.dp))
                    val options = listOf<Boolean?>(null, true, false)
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        options.forEachIndexed { idx, o ->
                            SegmentedButton(
                                selected = step.states[t.key] == o,
                                onClick = {
                                    val cur = steps[i]
                                    steps[i] = cur.copy(states = if (o == null) cur.states - t.key else cur.states + (t.key to o))
                                },
                                shape = SegmentedButtonDefaults.itemShape(idx, options.size),
                                modifier = Modifier.tvFocusRing(),
                            ) {
                                Text(stringResource(when (o) { null -> R.string.scene_skip; true -> R.string.auto_action_turn_on; false -> R.string.auto_action_turn_off }), maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
    if (steps.size < Cycle.MAX_STEPS) {
        OutlinedButton(
            onClick = { steps.add(CycleStep(steps.lastOrNull()?.seconds ?: 60)) },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp).tvFocusRing(RoundedCornerShape(16.dp)),
        ) {
            Icon(Icons.Rounded.Add, null)
            Spacer(Modifier.size(8.dp))
            Text(stringResource(R.string.cycle_add_step))
        }
    }
}

private val REPEAT_STEPS = listOf(1, 2, 3, 4, 5, 6, 8, 10, 12, 15, 20, 24, 30, 40, 50, 60, 75, 100)
private val JITTER_STEPS = listOf(0, 10, 20, 30, 40, 50)
