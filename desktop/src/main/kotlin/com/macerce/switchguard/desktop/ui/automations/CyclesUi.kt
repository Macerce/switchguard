package com.macerce.switchguard.desktop.ui.automations

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.HourglassBottom
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.macerce.switchguard.core.ActiveWindow
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
import com.macerce.switchguard.desktop.i18n.Strings
import com.macerce.switchguard.desktop.ui.Button
import com.macerce.switchguard.desktop.ui.Hue
import com.macerce.switchguard.desktop.ui.HueIcon
import com.macerce.switchguard.desktop.ui.OutlinedButton
import com.macerce.switchguard.desktop.ui.PageHeader
import com.macerce.switchguard.desktop.ui.TextButton
import com.macerce.switchguard.desktop.ui.raisedOnTile
import com.macerce.switchguard.desktop.ui.readableWidth
import com.macerce.switchguard.desktop.ui.settings.TimeDialog
import com.macerce.switchguard.desktop.ui.settings.formatMinute
import com.macerce.switchguard.desktop.ui.tileColor
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import java.util.UUID

/** Yeni döngü düzenleyicisinin işareti. */
internal const val NEW_CYCLE = "new-cycle"

/** "1 sa 5 dk", "45 sn". */
internal fun cycleDuration(seconds: Long, strings: Strings): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    val parts = buildList {
        if (h > 0) add("$h ${strings["cycle_unit_h"]}")
        if (m > 0) add("$m ${strings["cycle_unit_m"]}")
        if (s > 0 || isEmpty()) add("$s ${strings["cycle_unit_s"]}")
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

private fun CyclePreset.titleKey() = when (this) {
    CyclePreset.COUNTDOWN -> "cycle_preset_countdown"
    CyclePreset.ON_FOR -> "cycle_preset_on_for"
    CyclePreset.BLINK -> "cycle_preset_blink"
    CyclePreset.CHASE -> "cycle_preset_chase"
    CyclePreset.CUSTOM -> "cycle_preset_custom"
}

private fun CyclePreset.descKey() = when (this) {
    CyclePreset.COUNTDOWN -> "cycle_desc_countdown"
    CyclePreset.ON_FOR -> "cycle_desc_on_for"
    CyclePreset.BLINK -> "cycle_desc_blink"
    CyclePreset.CHASE -> "cycle_desc_chase"
    CyclePreset.CUSTOM -> "cycle_desc_custom"
}

/**
 * Otomasyon sekmesindeki döngü şeridi (telefondaki `CyclesStrip`): oynat/durdur düğmesi, süren döngüde canlı geri
 * sayım, sonda "Döngü ekle". Masaüstünde satıra sığmayan kart alt satıra iner.
 */
@Composable
fun CyclesStrip(
    cycles: List<Cycle>, runs: Map<String, CycleRun>, strings: Strings,
    onToggle: (Cycle, start: Boolean) -> Unit, onEdit: (String) -> Unit,
) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(runs.isNotEmpty()) {
        while (true) {
            now = System.currentTimeMillis()
            delay(if (runs.isNotEmpty()) 1000 else 30_000)
        }
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        cycles.forEach { c ->
            val run = runs[c.id]
            val shape = RoundedCornerShape(6.dp)
            val scheme = MaterialTheme.colorScheme
            Column(
                Modifier.width(200.dp).heightIn(min = 112.dp).clip(shape)
                    .background(if (run != null) scheme.primaryContainer else tileColor())
                    .clickable { onEdit(c.id) }
                    .padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 4.dp)
                    .testTag("cycle-${c.id}"),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HueIcon(c.preset.icon(), if (run != null) Hue.GREEN else Hue.TEAL, size = 28.dp)
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { onToggle(c, run == null) }, modifier = Modifier.testTag("cycle-toggle-${c.id}")) {
                        Icon(
                            if (run == null) Icons.Rounded.PlayArrow else Icons.Rounded.Stop,
                            strings[if (run == null) "cycle_start" else "cycle_stop"],
                            tint = scheme.primary,
                        )
                    }
                }
                Text(c.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (run != null) {
                    Text(strings.format("cycle_left", clock(run.stepEndsAt - now)), style = MaterialTheme.typography.titleMedium, color = scheme.primary)
                    val round = if (c.repeat > 0) strings.format("cycle_round_of", run.round + 1, c.repeat)
                        else strings.format("cycle_round_n", run.round + 1)
                    Text(
                        if (c.steps.size > 1) strings.format("cycle_running", run.step + 1, c.steps.size, round) else round,
                        style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, maxLines = 1,
                    )
                } else {
                    Text(
                        cycleScheduleText(c, now, strings),
                        style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        val shape = RoundedCornerShape(6.dp)
        Box(
            Modifier.width(140.dp).heightIn(min = 112.dp).clip(shape).background(tileColor())
                .clickable { onEdit(NEW_CYCLE) }.testTag("add-cycle"),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.Add, null, tint = MaterialTheme.colorScheme.primary)
                Text(strings["cycle_add"], style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

private fun cycleScheduleText(c: Cycle, now: Long, strings: Strings): String {
    if (c.start == CycleStart.MANUAL) return strings["cycle_idle_manual"]
    if (!c.enabled) return strings["cycle_disabled"]
    val next = CycleEngine.nextStart(c, now) ?: return strings["cycle_idle_manual"]
    val time = DateFormat.getTimeInstance(DateFormat.SHORT, strings.locale).format(Date(next))
    val today = Calendar.getInstance().apply { timeInMillis = now }
    val day = Calendar.getInstance().apply { timeInMillis = next }
    val sameDay = today.get(Calendar.YEAR) == day.get(Calendar.YEAR) && today.get(Calendar.DAY_OF_YEAR) == day.get(Calendar.DAY_OF_YEAR)
    val text = if (sameDay) time else strings.array("day_short")[ActiveWindow.dayIndex(day)] + " " + time
    return strings.format("cycle_next_at", text)
}

/** Süre girişi: sayı + birim (sn / dk / sa). Değer saniyedir. */
@Composable
private fun DurationField(label: String, seconds: Int, strings: Strings, tag: String, onChange: (Int) -> Unit) {
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
    Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        TextField(
            value = text,
            onValueChange = { v ->
                text = v.filter { it.isDigit() }.take(5)
                text.toIntOrNull()?.let { onChange((it * unit).coerceAtMost(Cycle.MAX_STEP_SECONDS)) }
            },
            label = { Text(label) },
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = raisedOnTile(), unfocusedContainerColor = raisedOnTile(),
                focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
            ),
            modifier = Modifier.weight(1f).testTag(tag),
        )
        Spacer(Modifier.width(8.dp))
        val scheme = MaterialTheme.colorScheme
        Row(Modifier.clip(RoundedCornerShape(6.dp)).background(raisedOnTile())) {
            units.forEach { u ->
                val on = unit == u
                Box(
                    Modifier.width(52.dp).height(40.dp)
                        .background(if (on) scheme.primary else Color.Transparent)
                        .selectable(selected = on, role = Role.RadioButton, onClick = {
                            unit = u
                            text.toIntOrNull()?.let { onChange((it * u).coerceAtMost(Cycle.MAX_STEP_SECONDS)) }
                        })
                        .testTag("$tag-unit-$u"),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        strings[when (u) { 1 -> "cycle_unit_s"; 60 -> "cycle_unit_m"; else -> "cycle_unit_h" }],
                        color = if (on) scheme.onPrimary else scheme.onSurface, style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

private fun targetLabel(t: CycleTarget, devices: List<DeviceSnapshot>, strings: Strings): String {
    val d = devices.firstOrNull { it.id == t.deviceId } ?: return t.deviceId
    return if (d.isMultiChannel) strings.format("label_channel", d.name, t.channel + 1) else d.name
}

/**
 * Döngü düzenleyici (telefondaki `CycleEditor`): kalıp (geri sayım, aç-sonra-kapat, aç/kapa, sırayla, özel adımlar),
 * cihazlar, süreler, tekrar, ne zaman başlayacağı (düğme, belirli saat + günler, saat aralığı), bitiş eylemi ve
 * gelişmiş seçenekler. Esc geri döner.
 */
@Composable
fun CycleEditor(
    initial: Cycle?, devices: List<DeviceSnapshot>, strings: Strings, modifier: Modifier,
    onSave: (Cycle) -> Unit, onDelete: () -> Unit, onBack: () -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var preset by remember { mutableStateOf(initial?.preset ?: CyclePreset.COUNTDOWN) }
    val targets = remember { mutableStateListOf<CycleTarget>().apply { initial?.targets?.let(::addAll) } }
    val steps = remember { mutableStateListOf<CycleStep>().apply { initial?.steps?.let(::addAll) } }
    var mainSec by remember { mutableIntStateOf(initial?.steps?.firstOrNull()?.seconds ?: 30 * 60) }
    var offSec by remember { mutableIntStateOf(initial?.steps?.getOrNull(1)?.seconds ?: 10 * 60) }
    var countdownOn by remember { mutableStateOf(initial?.end == CycleEnd.ON) }
    var repeat by remember { mutableIntStateOf(initial?.repeat?.takeIf { it > 0 } ?: 3) }
    var forever by remember { mutableStateOf(initial?.repeat == 0) }
    var start by remember { mutableStateOf(initial?.start ?: CycleStart.MANUAL) }
    var startMinute by remember { mutableIntStateOf(initial?.startMinute ?: 7 * 60) }
    var endMinute by remember { mutableIntStateOf(initial?.endMinute ?: 22 * 60) }
    var days by remember { mutableIntStateOf(initial?.days ?: ActiveWindow.ALL_DAYS) }
    var end by remember { mutableStateOf(initial?.end ?: CycleEnd.OFF) }
    var jitter by remember { mutableIntStateOf(initial?.jitterPct ?: 0) }
    var presence by remember { mutableStateOf(initial?.presence) }
    var stopOnManual by remember { mutableStateOf(initial?.stopOnManual ?: false) }
    var enabled by remember { mutableStateOf(initial?.enabled ?: true) }
    var confirmDelete by remember { mutableStateOf(false) }
    var pickTime by remember { mutableStateOf<Pair<String, Int>?>(null) }

    val repeats = preset == CyclePreset.BLINK || preset == CyclePreset.CHASE || preset == CyclePreset.CUSTOM
    val repeatValue = if (!repeats) 1 else if (forever) 0 else repeat
    val builtSteps: List<CycleStep> = when (preset) {
        CyclePreset.COUNTDOWN -> CycleEngine.Presets.countdown(targets, mainSec, countdownOn).first
        CyclePreset.ON_FOR -> CycleEngine.Presets.onFor(targets, mainSec).first
        CyclePreset.BLINK -> CycleEngine.Presets.blink(targets, mainSec, offSec, repeatValue).first
        CyclePreset.CHASE -> CycleEngine.Presets.chase(targets, mainSec, repeatValue).first
        CyclePreset.CUSTOM -> steps.map { s -> s.copy(states = s.states.filterKeys { k -> targets.any { it.key == k } }) }
    }
    val builtEnd = if (preset == CyclePreset.COUNTDOWN) (if (countdownOn) CycleEnd.ON else CycleEnd.OFF) else end
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

    Box(
        modifier.fillMaxSize().onPreviewKeyEvent {
            if (it.type == KeyEventType.KeyDown && it.key == Key.Escape) { onBack(); true } else false
        },
    ) {
        Column(Modifier.align(Alignment.TopCenter).readableWidth().fillMaxSize()) {
            PageHeader(
                strings[if (initial == null) "cycle_new" else "cycle_edit"],
                onBack = onBack,
                actions = {
                    if (initial != null) {
                        IconButton(onClick = { confirmDelete = true }, modifier = Modifier.testTag("delete-cycle")) {
                            Icon(Icons.Rounded.Delete, strings["action_delete"])
                        }
                    }
                },
            )
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
                TextField(
                    value = name, onValueChange = { name = it.take(40) },
                    label = { Text(strings["cycle_name"]) },
                    placeholder = { Text(strings["cycle_name_hint"]) },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = tileColor(), unfocusedContainerColor = tileColor(),
                        focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                    ),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).testTag("cycle-name"),
                )

                // ------------------------------------------------ Kalıp
                StepCard(Icons.Rounded.Repeat, Hue.TEAL, strings["cycle_kind"], strings[preset.descKey()]) {
                    Spacer(Modifier.height(12.dp))
                    OptionGrid(
                        CyclePreset.entries, preset, wideColumns = 5,
                        icon = { it.icon() }, label = { strings[it.titleKey()] }, tag = { "preset-${it.name}" },
                    ) { p ->
                        if (p == CyclePreset.CUSTOM && preset != CyclePreset.CUSTOM) {
                            // Şimdiki kalıbın adımlarıyla başla; ardından elle değiştirilir.
                            steps.clear()
                            steps.addAll(builtSteps.ifEmpty { listOf(CycleStep(mainSec, targets.associate { it.key to true })) })
                        }
                        preset = p
                    }
                }

                // ------------------------------------------------ Cihazlar
                StepCard(Icons.Rounded.DeviceHub, Hue.BLUE, strings["cycle_targets"], strings["cycle_targets_hint"]) {
                    Spacer(Modifier.height(8.dp))
                    val rows = devices.flatMap { d -> d.switches.keys.sorted().map { CycleTarget(d.id, it) } }
                    if (rows.isEmpty()) Text(strings["no_devices"], Modifier.padding(8.dp))
                    rows.forEach { t ->
                        val checked = t in targets
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp))
                                .selectable(selected = checked, role = Role.Checkbox, onClick = {
                                    if (checked) targets.remove(t) else targets.add(t)
                                })
                                .testTag("cycle-target-${t.key}"),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(checked = checked, onCheckedChange = null, modifier = Modifier.padding(8.dp))
                            Text(targetLabel(t, devices, strings), style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }

                // ------------------------------------------------ Süreler / adımlar
                StepCard(
                    Icons.Rounded.Timer, Hue.AMBER, strings[if (preset == CyclePreset.CUSTOM) "cycle_steps" else "cycle_duration"],
                    strings.format("cycle_round_length", cycleDuration(draft.roundSeconds, strings)),
                ) {
                    when (preset) {
                        CyclePreset.COUNTDOWN -> {
                            DurationField(strings["cycle_duration"], mainSec, strings, "main") { mainSec = it }
                            FieldLabel(strings["cycle_after"])
                            OptionGrid(
                                listOf(false, true), countdownOn, wideColumns = 2,
                                icon = { if (it) Icons.Rounded.PowerSettingsNew else Icons.Rounded.PowerOff },
                                label = { strings[if (it) "cycle_end_on" else "cycle_end_off"] }, tag = { "countdown-$it" },
                            ) { countdownOn = it }
                        }
                        CyclePreset.ON_FOR -> DurationField(strings["cycle_on_time"], mainSec, strings, "main") { mainSec = it }
                        CyclePreset.BLINK -> {
                            DurationField(strings["cycle_on_time"], mainSec, strings, "main") { mainSec = it }
                            DurationField(strings["cycle_off_time"], offSec, strings, "off") { offSec = it }
                        }
                        CyclePreset.CHASE -> DurationField(strings["cycle_each_time"], mainSec, strings, "main") { mainSec = it }
                        CyclePreset.CUSTOM -> CustomSteps(steps, targets, devices, strings)
                    }
                    if (builtSteps.any { it.seconds < Cycle.MIN_STEP_SECONDS }) {
                        Text(strings["cycle_min_step"], color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
                    }
                    if (repeats) {
                        FieldLabel(strings["cycle_repeat"])
                        ToggleLine(strings[if (start == CycleStart.WINDOW) "cycle_repeat_forever_window" else "cycle_repeat_forever"], forever, "forever") { forever = it }
                        if (!forever) {
                            Stepper(strings["cycle_repeat"], repeat, REPEAT_STEPS, strings, "repeat", format = { strings.plural("cycle_rounds", it, it) }) { repeat = it }
                        }
                    }
                }

                // ------------------------------------------------ Ne zaman
                StepCard(Icons.Rounded.Schedule, Hue.INDIGO, strings["cycle_start_title"], "") {
                    Spacer(Modifier.height(12.dp))
                    OptionGrid(
                        CycleStart.entries, start, wideColumns = 3,
                        icon = { when (it) { CycleStart.MANUAL -> Icons.Rounded.TouchApp; CycleStart.AT_TIME -> Icons.Rounded.Event; CycleStart.WINDOW -> Icons.Rounded.Schedule } },
                        label = { strings[when (it) { CycleStart.MANUAL -> "cycle_start_manual"; CycleStart.AT_TIME -> "cycle_start_at"; CycleStart.WINDOW -> "cycle_start_window" }] },
                        tag = { "start-${it.name}" },
                    ) { start = it }
                    if (start != CycleStart.MANUAL) {
                        Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TimeChip(strings["cycle_start_time"], formatMinute(strings, startMinute), Modifier.weight(1f)) { pickTime = "start" to startMinute }
                            if (start == CycleStart.WINDOW) {
                                TimeChip(strings["cycle_end_time"], formatMinute(strings, endMinute), Modifier.weight(1f)) { pickTime = "end" to endMinute }
                            }
                        }
                        DayChips(days, strings) { days = it }
                        Text(
                            strings[if (start == CycleStart.WINDOW) "cycle_window_note" else "desktop_cycle_at_note"],
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        ToggleLine(strings["cycle_enabled"], enabled, "cycle-enabled") { enabled = it }
                    }
                }

                // ------------------------------------------------ Bitince
                if (preset != CyclePreset.COUNTDOWN) {
                    StepCard(Icons.Rounded.Check, Hue.GREEN, strings["cycle_end_title"], "") {
                        Spacer(Modifier.height(12.dp))
                        OptionGrid(
                            CycleEnd.entries, end, wideColumns = 4,
                            icon = {
                                when (it) {
                                    CycleEnd.OFF -> Icons.Rounded.PowerOff; CycleEnd.ON -> Icons.Rounded.PowerSettingsNew
                                    CycleEnd.KEEP -> Icons.Rounded.AllInclusive; CycleEnd.RESTORE -> Icons.Rounded.History
                                }
                            },
                            label = {
                                strings[when (it) {
                                    CycleEnd.OFF -> "cycle_end_off"; CycleEnd.ON -> "cycle_end_on"
                                    CycleEnd.KEEP -> "cycle_end_keep"; CycleEnd.RESTORE -> "cycle_end_restore"
                                }]
                            },
                            tag = { "end-${it.name}" },
                        ) { end = it }
                    }
                }

                // ------------------------------------------------ Gelişmiş
                StepCard(Icons.Rounded.Tune, Hue.SLATE, strings["cycle_advanced"], "") {
                    Stepper(strings["cycle_jitter"], jitter, JITTER_STEPS, strings, "jitter", format = {
                        if (it == 0) strings["cycle_jitter_off"] else "±%$it"
                    }) { jitter = it }
                    Text(strings["cycle_jitter_desc"], style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp, start = 4.dp))
                    if (start != CycleStart.MANUAL) {
                        FieldLabel(strings["cycle_presence"])
                        OptionGrid(
                            listOf<PresenceRule?>(null, PresenceRule.HOME, PresenceRule.AWAY), presence, wideColumns = 3,
                            icon = { when (it) { null -> Icons.Rounded.AllInclusive; PresenceRule.HOME -> Icons.Rounded.Home; PresenceRule.AWAY -> Icons.AutoMirrored.Rounded.DirectionsWalk } },
                            label = { strings[when (it) { null -> "cycle_presence_any"; PresenceRule.HOME -> "presence_home"; PresenceRule.AWAY -> "presence_away" }] },
                            tag = { "cycle-presence-${it?.name ?: "any"}" },
                        ) { presence = it }
                    }
                    ToggleLine(strings["cycle_stop_manual"], stopOnManual, "stop-manual") { stopOnManual = it }
                    Text(strings["cycle_stop_manual_desc"], style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp, start = 4.dp))
                }

                Button(
                    enabled = draft.valid,
                    onClick = { onSave(draft) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).height(36.dp).testTag("save-cycle"),
                ) {
                    Icon(Icons.Rounded.Check, null)
                    Spacer(Modifier.width(8.dp))
                    Text(strings["action_save"], style = MaterialTheme.typography.titleSmall)
                }
            }
        }
    }

    pickTime?.let { (which, initialMinute) ->
        TimeDialog(
            strings[if (which == "start") "cycle_start_time" else "cycle_end_time"], initialMinute, strings.locale,
            onPick = { m ->
                if (which == "start") startMinute = m else endMinute = m
                pickTime = null
            },
            onDismiss = { pickTime = null },
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(strings["cycle_delete_title"]) },
            text = { Text(name) },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete() }, modifier = Modifier.testTag("confirm-delete")) {
                    Text(strings["action_delete"])
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(strings["action_cancel"]) } },
        )
    }
}

/** Özel adımlar: her adımın süresi ve her hedef için Dokunma / Aç / Kapat; taşı, sil, ekle. */
@Composable
private fun CustomSteps(steps: MutableList<CycleStep>, targets: List<CycleTarget>, devices: List<DeviceSnapshot>, strings: Strings) {
    val scheme = MaterialTheme.colorScheme
    steps.forEachIndexed { i, step ->
        key(i, steps.size) {
            Column(Modifier.fillMaxWidth().padding(top = 12.dp).clip(RoundedCornerShape(8.dp)).background(raisedOnTile()).padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(strings.format("cycle_step_n", i + 1), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    IconButton(enabled = i > 0, onClick = { val s = steps.removeAt(i); steps.add(i - 1, s) }) {
                        Icon(Icons.Rounded.ArrowUpward, strings["cycle_move_up"])
                    }
                    IconButton(enabled = i < steps.size - 1, onClick = { val s = steps.removeAt(i); steps.add(i + 1, s) }) {
                        Icon(Icons.Rounded.ArrowDownward, strings["cycle_move_down"])
                    }
                    IconButton(enabled = steps.size > 1, onClick = { steps.removeAt(i) }) {
                        Icon(Icons.Rounded.Delete, strings["cycle_step_delete"])
                    }
                }
                DurationField(strings["cycle_duration"], step.seconds, strings, "step-$i") { sec -> steps[i] = steps[i].copy(seconds = sec) }
                targets.forEach { t ->
                    Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(targetLabel(t, devices, strings), style = MaterialTheme.typography.bodyMedium, maxLines = 1,
                            overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        Row(Modifier.clip(RoundedCornerShape(6.dp)).background(tileColor())) {
                            listOf<Boolean?>(null, true, false).forEach { o ->
                                val on = step.states[t.key] == o
                                Box(
                                    Modifier.width(84.dp).height(34.dp)
                                        .background(if (on) scheme.primary else Color.Transparent)
                                        .selectable(selected = on, role = Role.RadioButton, onClick = {
                                            val cur = steps[i]
                                            steps[i] = cur.copy(states = if (o == null) cur.states - t.key else cur.states + (t.key to o))
                                        }),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        strings[when (o) { null -> "scene_skip"; true -> "auto_action_turn_on"; false -> "auto_action_turn_off" }],
                                        color = if (on) scheme.onPrimary else scheme.onSurface, style = MaterialTheme.typography.labelLarge, maxLines = 1,
                                    )
                                }
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
            modifier = Modifier.padding(top = 12.dp).testTag("add-step"),
        ) {
            Icon(Icons.Rounded.Add, null)
            Spacer(Modifier.size(8.dp))
            Text(strings["cycle_add_step"])
        }
    }
}

private val REPEAT_STEPS = listOf(1, 2, 3, 4, 5, 6, 8, 10, 12, 15, 20, 24, 30, 40, 50, 60, 75, 100)
private val JITTER_STEPS = listOf(0, 10, 20, 30, 40, 50)
