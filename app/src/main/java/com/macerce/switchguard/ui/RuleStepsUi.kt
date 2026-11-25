package com.macerce.switchguard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Numbers
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.ToggleOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.macerce.switchguard.R
import com.macerce.switchguard.core.ActionKind
import com.macerce.switchguard.core.AutoAction
import com.macerce.switchguard.core.Automation
import com.macerce.switchguard.core.CondSet
import com.macerce.switchguard.core.ConditionKind
import com.macerce.switchguard.core.ConditionMatch
import com.macerce.switchguard.core.Cycle
import com.macerce.switchguard.core.DeviceSnapshot
import com.macerce.switchguard.core.Helper
import com.macerce.switchguard.core.HelperKind
import com.macerce.switchguard.core.LightLevel
import com.macerce.switchguard.core.RunMode
import com.macerce.switchguard.core.Scene
import com.macerce.switchguard.core.ScriptRunner
import com.macerce.switchguard.core.SendTarget
import com.macerce.switchguard.core.StepTree
import com.macerce.switchguard.core.TextTemplate
import com.macerce.switchguard.data.Store
import com.macerce.switchguard.service.HelperControl
import com.macerce.switchguard.ui.theme.raisedOnTile
import com.macerce.switchguard.ui.theme.tileColor
import java.util.UUID

/** Adım düzenleyicisinin ortak verisi. */
internal data class StepCtx(
    /** Açılıp kapatılabilen hedefler (cihazlar, gruplar, yardımcı anahtar/zamanlayıcılar). */
    val targets: List<DeviceSnapshot>,
    /** Koşul cihazları (sensörler ve sayaçlar dahil). */
    val condTargets: List<DeviceSnapshot>,
    val scenes: List<Scene>,
    val cycles: List<Cycle>,
    val helpers: List<Helper>,
    /** Çalıştırılabilecek diğer kurallar. */
    val rules: List<Automation>,
    /** Parlaklığı/rengi ayarlanabilen cihazlar. */
    val lights: List<DeviceSnapshot>,
    val triggerCount: Int,
    val locationVersion: Int,
)

/** Adım türleri, düzenleyicide üç küme halinde. */
internal val STEP_DEVICE_KINDS = listOf(
    ActionKind.TURN_ON, ActionKind.TURN_OFF, ActionKind.TOGGLE, ActionKind.SET_LEVEL, ActionKind.RUN_SCENE,
    ActionKind.START_CYCLE, ActionKind.STOP_CYCLE, ActionKind.SAVE_STATE, ActionKind.RESTORE_STATE,
    ActionKind.COUNTER_ADD, ActionKind.COUNTER_SET,
)
internal val STEP_FLOW_KINDS = listOf(ActionKind.DELAY, ActionKind.WAIT_UNTIL, ActionKind.IF, ActionKind.REPEAT, ActionKind.RUN_AUTOMATION, ActionKind.STOP)
internal val STEP_MESSAGE_KINDS = listOf(ActionKind.NOTIFY, ActionKind.ALARM, ActionKind.SEND)

/** Bekleme süresi adımları (sn). */
private val SECOND_STEPS = listOf(5, 10, 15, 30, 45, 60, 90, 120, 180, 300, 600, 900, 1200, 1800, 2700, 3600, 5400, 7200, 10800, 14400, 21600, 43200, 86400)
/** "Şu olana kadar bekle" en uzun süre (sn); 0 = sınırsız. */
private val TIMEOUT_STEPS = listOf(0) + SECOND_STEPS

/** "30 sn", "5 dk", "1 sa 30 dk", "2 dk 30 sn". */
@Composable
internal fun secondsLabel(sec: Int): String = when {
    sec < 60 -> stringResource(R.string.dur_seconds_short, sec)
    sec % 60 == 0 -> holdLabel(sec / 60)
    else -> holdLabel(sec / 60) + " " + stringResource(R.string.dur_seconds_short, sec % 60)
}

/** Koşul kümesinin tek cümlelik metni. */
@Composable
internal fun condSetText(set: CondSet?, devices: List<DeviceSnapshot>): String =
    if (set == null || set.isEmpty) stringResource(R.string.cond_none_short) else conditionsText(set.items, set.match, devices, set.groupMatch)

/** Yeni adım türlerinin metni; eski türlerde null ([actionText] kendisi yazar). */
@Composable
internal fun flowText(x: AutoAction, devices: List<DeviceSnapshot>): String? {
    val context = LocalContext.current
    fun deviceName(id: String, ch: Int) = devices.firstOrNull { it.id == id }
    return when (x.kind) {
        ActionKind.DELAY -> stringResource(R.string.step_delay_short, secondsLabel(x.seconds))
        ActionKind.WAIT_UNTIL -> stringResource(R.string.step_wait_short, condSetText(x.cond, devices)) +
            (if (x.seconds > 0) " · " + stringResource(R.string.step_timeout) + " " + secondsLabel(x.seconds) else "")
        ActionKind.IF -> stringResource(R.string.step_if_short, condSetText(x.cond, devices), x.then.size, x.otherwise.size)
        ActionKind.REPEAT -> if (x.count > 0) stringResource(R.string.step_repeat_short, x.count, x.then.size)
            else stringResource(R.string.step_repeat_while_short, condSetText(x.cond, devices), x.then.size)
        ActionKind.STOP -> stringResource(R.string.act_stop)
        ActionKind.RUN_AUTOMATION -> stringResource(R.string.step_run_rule_short,
            Store.get(context).automations.firstOrNull { it.id == x.deviceId }?.name ?: stringResource(R.string.device_missing))
        ActionKind.SEND -> sendTargetName(x.deviceId) + ": " + x.text.ifBlank { "{rule}" }
        ActionKind.SET_LEVEL -> {
            val d = deviceName(x.deviceId, x.channel)
            val what = listOfNotNull(x.level?.let { "$it%" }, x.color?.let { "●" }).joinToString(" ")
            (d?.let { channelLabel(it, x.channel, true) } ?: stringResource(R.string.device_missing)) + ": " + stringResource(R.string.act_set_level) +
                (if (what.isNotEmpty()) " $what" else "")
        }
        ActionKind.COUNTER_ADD -> (deviceName(x.deviceId, 0)?.name ?: stringResource(R.string.device_missing)) + " " + (if (x.count >= 0) "+" else "") + x.count
        ActionKind.COUNTER_SET -> (deviceName(x.deviceId, 0)?.name ?: stringResource(R.string.device_missing)) + " = " + x.count
        ActionKind.NOTIFY, ActionKind.ALARM -> if (x.text.isNotBlank()) stringResource(x.kind.title()) + ": " + x.text else null
        else -> null
    }
}

internal fun sendTargetName(id: String): String = when (runCatching { SendTarget.valueOf(id) }.getOrNull()) {
    SendTarget.NTFY -> "ntfy"
    SendTarget.WEBHOOK -> "Webhook"
    SendTarget.TELEGRAM -> "Telegram"
    null -> "?"
}

/** Adım kaydedilebilir mi (iç içe adımlar dahil). */
internal fun stepValid(x: AutoAction, c: StepCtx): Boolean {
    val condsOk = x.cond?.items.orEmpty().all { cond -> !cond.kind.usesDevice || c.condTargets.any { it.id == cond.deviceId } }
    val own = when {
        x.kind == ActionKind.SET_LEVEL -> c.lights.any { it.id == x.deviceId } && (x.level != null || x.color != null)
        x.kind.usesDevice -> c.targets.any { it.id == x.deviceId }
        x.kind == ActionKind.RUN_SCENE -> c.scenes.any { it.id == x.deviceId }
        x.kind.usesCycle -> c.cycles.any { it.id == x.deviceId }
        x.kind == ActionKind.SAVE_STATE -> snapshotTargetValid(x.deviceId, c.targets, c.scenes)
        x.kind.usesCounter -> c.helpers.any { it.deviceId == x.deviceId && it.kind == HelperKind.COUNTER }
        x.kind == ActionKind.RUN_AUTOMATION -> c.rules.any { it.id == x.deviceId }
        x.kind == ActionKind.SEND -> runCatching { SendTarget.valueOf(x.deviceId) }.isSuccess
        x.kind == ActionKind.WAIT_UNTIL || x.kind == ActionKind.IF -> x.cond?.isEmpty == false
        x.kind == ActionKind.REPEAT -> x.then.isNotEmpty() && (x.count > 0 || x.cond?.isEmpty == false)
        else -> true
    }
    return own && condsOk && (x.then + x.otherwise).all { stepValid(it, c) }
}

/** Yeni tür seçilince adımın varsayılanları (önceki hedef uyuyorsa korunur). */
internal fun stepDefaults(x: AutoAction, k: ActionKind, c: StepCtx, ruleName: String): AutoAction {
    val keepDevice = x.deviceId.takeIf { id -> c.targets.any { it.id == id } }
    val base = AutoAction(k, waitMinutes = x.waitMinutes)
    return when {
        k == ActionKind.SET_LEVEL -> base.copy(deviceId = x.deviceId.takeIf { id -> c.lights.any { it.id == id } } ?: c.lights.firstOrNull()?.id.orEmpty(), level = 80)
        k.usesDevice -> base.copy(deviceId = keepDevice ?: c.targets.firstOrNull()?.id.orEmpty(), channel = if (keepDevice != null) x.channel else 0)
        k == ActionKind.RUN_SCENE -> base.copy(deviceId = c.scenes.firstOrNull()?.id.orEmpty())
        k == ActionKind.SAVE_STATE -> base.copy(deviceId = x.deviceId.takeIf { snapshotTargetValid(it, c.targets, c.scenes) } ?: snapshotDefault(c.targets))
        k.usesCycle -> base.copy(deviceId = c.cycles.firstOrNull()?.id.orEmpty())
        k.usesCounter -> base.copy(deviceId = c.helpers.firstOrNull { it.kind == HelperKind.COUNTER }?.deviceId.orEmpty(), count = 1)
        k == ActionKind.DELAY -> base.copy(seconds = 60)
        k == ActionKind.WAIT_UNTIL -> base.copy(cond = x.cond ?: CondSet(), seconds = 600)
        k == ActionKind.IF -> base.copy(cond = x.cond ?: CondSet(), then = x.then, otherwise = x.otherwise)
        k == ActionKind.REPEAT -> base.copy(count = 3, cond = x.cond, then = x.then)
        k == ActionKind.RUN_AUTOMATION -> base.copy(deviceId = c.rules.firstOrNull()?.id.orEmpty())
        k == ActionKind.SEND -> base.copy(deviceId = SendTarget.NTFY.name, text = x.text.ifBlank { ruleName.ifBlank { "{rule}" } })
        k == ActionKind.NOTIFY || k == ActionKind.ALARM -> base.copy(text = x.text)
        else -> base
    }
}

/**
 * Bir adımın düzenleyicisi. Blok adımlarında (eğer, tekrarla) iç adımlar satır olarak listelenir; dokununca o adım
 * açılır ([onOpen]); "Adım ekle" ilgili dala ekler ([onAdd]).
 */
@Composable
internal fun StepEditor(
    path: StepTree.Path,
    x: AutoAction,
    c: StepCtx,
    ruleName: String,
    onChange: (AutoAction) -> Unit,
    onDelete: () -> Unit,
    onOpen: (StepTree.Path) -> Unit,
    onAdd: (Char) -> Unit,
    onMove: (Int) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(top = 12.dp)) {
        path.parent()?.let { parent ->
            TextButton(modifier = Modifier.tvFocusRing(), onClick = { onOpen(parent) }) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.step_back))
            }
        }
        val nestable = path.depth < StepTree.MAX_DEPTH - 1
        FieldLabel(stringResource(R.string.step_group_device))
        OptionGrid(STEP_DEVICE_KINDS, x.kind, { it.icon() }, { stringResource(it.editorTitle()) }, hue = { it.hue() }) { onChange(stepDefaults(x, it, c, ruleName)) }
        FieldLabel(stringResource(R.string.step_group_flow))
        OptionGrid(STEP_FLOW_KINDS.filter { nestable || !it.isBlock }, x.kind, { it.icon() }, { stringResource(it.editorTitle()) }, hue = { it.hue() }) {
            onChange(stepDefaults(x, it, c, ruleName))
        }
        FieldLabel(stringResource(R.string.step_group_message))
        OptionGrid(STEP_MESSAGE_KINDS, x.kind, { it.icon() }, { stringResource(it.editorTitle()) }, hue = { it.hue() }) { onChange(stepDefaults(x, it, c, ruleName)) }

        when {
            x.kind == ActionKind.SET_LEVEL -> LevelFields(x, c, onChange)
            x.kind.usesDevice -> {
                FieldLabel(stringResource(R.string.automation_device))
                DeviceChooser(c.targets, x.deviceId) { onChange(x.copy(deviceId = it.id, channel = 0)) }
                c.targets.firstOrNull { it.id == x.deviceId }?.takeIf { it.isMultiChannel }?.let { d ->
                    FieldLabel(stringResource(R.string.automation_channel))
                    ChannelChooser(d, x.channel) { onChange(x.copy(channel = it)) }
                }
            }
            x.kind == ActionKind.RUN_SCENE -> {
                FieldLabel(stringResource(R.string.scenes_title))
                if (c.scenes.isEmpty()) Hint(stringResource(R.string.scene_none_yet))
                else OptionGrid(c.scenes.map { it.id }, x.deviceId, { Icons.Rounded.PlayArrow }, { id -> c.scenes.first { it.id == id }.name }) { onChange(x.copy(deviceId = it)) }
            }
            x.kind.usesCycle -> CyclePick(c.cycles, x.deviceId) { onChange(x.copy(deviceId = it)) }
            x.kind == ActionKind.SAVE_STATE -> SnapshotScope(c.targets, c.scenes, x.deviceId) { onChange(x.copy(deviceId = it)) }
            x.kind == ActionKind.RESTORE_STATE -> Hint(stringResource(R.string.snap_restore_hint))
            x.kind.usesCounter -> CounterFields(x, c, onChange)
            x.kind == ActionKind.DELAY ->
                Stepper(stringResource(R.string.step_delay_len), x.seconds, SECOND_STEPS, format = { secondsLabel(it) }) { onChange(x.copy(seconds = it)) }
            x.kind == ActionKind.WAIT_UNTIL -> {
                StepConditions(path, x, c, onChange)
                Stepper(stringResource(R.string.step_timeout), x.seconds, TIMEOUT_STEPS, zeroLabel = stringResource(R.string.step_timeout_none),
                    format = { secondsLabel(it) }) { onChange(x.copy(seconds = it)) }
                if (x.seconds > 0) ToggleLine(stringResource(R.string.step_stop_on_timeout), x.stopOnTimeout) { onChange(x.copy(stopOnTimeout = it)) }
            }
            x.kind == ActionKind.IF -> {
                StepConditions(path, x, c, onChange)
                ChildSteps(stringResource(R.string.step_then), x.then, path, 't', c, onOpen) { onAdd('t') }
                ChildSteps(stringResource(R.string.step_else), x.otherwise, path, 'e', c, onOpen) { onAdd('e') }
            }
            x.kind == ActionKind.REPEAT -> {
                Stepper(stringResource(R.string.step_repeat_times), x.count, (0..ScriptRunner.MAX_REPEAT).toList(),
                    zeroLabel = stringResource(R.string.step_repeat_while)) { onChange(x.copy(count = it)) }
                if (x.count == 0) StepConditions(path, x, c, onChange)
                ChildSteps(stringResource(R.string.step_body), x.then, path, 't', c, onOpen) { onAdd('t') }
            }
            x.kind == ActionKind.RUN_AUTOMATION -> {
                FieldLabel(stringResource(R.string.step_rule_pick))
                if (c.rules.isEmpty()) Hint(stringResource(R.string.step_rule_none))
                else OptionGrid(c.rules.map { it.id }, x.deviceId, { Icons.Rounded.PlayArrow }, { id -> c.rules.first { it.id == id }.name }) { onChange(x.copy(deviceId = it)) }
            }
            x.kind == ActionKind.STOP -> Hint(stringResource(R.string.step_stop_hint))
            x.kind == ActionKind.SEND -> SendFields(x, onChange)
        }
        if (x.kind == ActionKind.NOTIFY || x.kind == ActionKind.ALARM) MessageField(x.text) { onChange(x.copy(text = it)) }
        // Eski biçimin "önceki eylemden sonra bekle"si yalnız üst düzeyde; iç adımlarda "Bekle" adımı kullanılır.
        if (path.depth == 0 && x.waitMinutes > 0) {
            Stepper(stringResource(R.string.rule_action_wait), x.waitMinutes, DELAY_STEPS, zeroLabel = stringResource(R.string.automation_now)) { onChange(x.copy(waitMinutes = it)) }
        }
        Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(modifier = Modifier.tvFocusRing(), onClick = { onMove(-1) }) { Icon(Icons.Rounded.KeyboardArrowUp, stringResource(R.string.step_move_up)) }
            IconButton(modifier = Modifier.tvFocusRing(), onClick = { onMove(1) }) { Icon(Icons.Rounded.KeyboardArrowDown, stringResource(R.string.step_move_down)) }
            Spacer(Modifier.weight(1f))
            TextButton(modifier = Modifier.tvFocusRing(), onClick = onDelete) {
                Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
}

/** Blok içindeki adımlar: her biri satır (dokununca açılır) ve sonda "Adım ekle". */
@Composable
private fun ChildSteps(title: String, list: List<AutoAction>, path: StepTree.Path, branch: Char, c: StepCtx, onOpen: (StepTree.Path) -> Unit, onAdd: () -> Unit) {
    FieldLabel(title)
    val devices = c.condTargets
    list.forEachIndexed { i, child ->
        ChoiceSummary(actionText(child, devices), child.kind.icon(), MaterialTheme.colorScheme.primary) { onOpen(path.child(branch, i)) }
    }
    TextButton(modifier = Modifier.padding(top = 4.dp).tvFocusRing(), onClick = onAdd) {
        Icon(Icons.Rounded.Add, null)
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.step_add))
    }
}

/** Adımın koşulları (eğer / şu olana kadar / sağlandıkça); yazılan eşik kaybolmasın diye taslak ayrı tutulur. */
@Composable
private fun StepConditions(path: StepTree.Path, x: AutoAction, c: StepCtx, onChange: (AutoAction) -> Unit) {
    val set = x.cond ?: CondSet()
    var drafts by rememberSaveable(path.toString(), stateSaver = CondDraft.ListSaver) { mutableStateOf(set.items.map(CondDraft::of)) }
    FieldLabel(stringResource(R.string.step_conditions))
    CondListEditor(drafts, set.match, set.groupMatch, c.condTargets, c.locationVersion, c.triggerCount) { d, m, g ->
        drafts = d
        onChange(x.copy(cond = CondSet(d.map { it.toCondition() }, m, g)))
    }
}

/**
 * Koşul listesi: hepsi/herhangi biri, her koşul (değil, grup) ve grupların kendi hepsi/herhangi biri kuralı.
 * Kuralın "ek koşullar"ı ve adım koşulları aynı düzenleyiciyi kullanır.
 */
@Composable
internal fun CondListEditor(
    drafts: List<CondDraft>,
    match: ConditionMatch,
    groupMatch: Map<Int, ConditionMatch>,
    targets: List<DeviceSnapshot>,
    locationVersion: Int,
    triggerCount: Int,
    onChange: (List<CondDraft>, ConditionMatch, Map<Int, ConditionMatch>) -> Unit,
) {
    fun newCond() = CondDraft(ConditionKind.IS_ON, targets.firstOrNull { !it.sensorOnly }?.id ?: targets.firstOrNull()?.id.orEmpty())
    if (drafts.size > 1) {
        FieldLabel(stringResource(R.string.cond_match))
        OptionGrid(ConditionMatch.entries, match, { if (it == ConditionMatch.ALL) Icons.Rounded.DoneAll else Icons.Rounded.Done },
            { stringResource(if (it == ConditionMatch.ALL) R.string.cond_match_all else R.string.cond_match_any) }) { onChange(drafts, it, groupMatch) }
    }
    drafts.forEachIndexed { i, d ->
        key(i) {
            ConditionBlock(i, d, targets, locationVersion,
                onChange = { n -> onChange(drafts.toMutableList().also { it[i] = n }, match, groupMatch) },
                onRemove = { onChange(drafts.filterIndexed { j, _ -> j != i }, match, groupMatch) },
                triggerCount = triggerCount,
                groups = drafts.size > 2)
        }
    }
    // Kullanılan grupların kendi bağlacı.
    drafts.map { it.group }.filter { it > 0 }.distinct().sorted().forEach { g ->
        FieldLabel(stringResource(R.string.cond_group_match, g))
        OptionChips(ConditionMatch.entries, groupMatch[g] ?: ConditionMatch.ALL, { if (it == ConditionMatch.ALL) Icons.Rounded.DoneAll else Icons.Rounded.Done },
            { stringResource(if (it == ConditionMatch.ALL) R.string.cond_match_all else R.string.cond_match_any) }) { onChange(drafts, match, groupMatch + (g to it)) }
    }
    TextButton(modifier = Modifier.padding(top = 4.dp).tvFocusRing(), onClick = { onChange(drafts + newCond(), match, groupMatch) }) {
        Icon(Icons.Rounded.Add, null)
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.cond_add))
    }
    if (drafts.any { it.kind.usesTemp || it.kind.isSun }) {
        val context = LocalContext.current
        val store = remember { Store.get(context) }
        var v by remember { mutableStateOf(0) }
        key(locationVersion, v) { LocationSetup(store, onChanged = { v++ }, modifier = Modifier.padding(top = 12.dp)) }
    }
}

/** Bildirim/alarm/gönderim metni ve yer tutucu düğmeleri. */
@Composable
private fun MessageField(text: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = text, onValueChange = onChange,
        label = { Text(stringResource(R.string.step_text_hint)) },
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp).tvTextField(),
        keyboardOptions = tvKeyboardOptions(),
    )
    Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.step_text_tokens), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(start = 8.dp)) {
            items(TextTemplate.TOKENS) { t ->
                Box(
                    Modifier.clip(RoundedCornerShape(10.dp)).background(raisedOnTile()).tvFocusRing(RoundedCornerShape(10.dp))
                        .clickable { onChange(if (text.isEmpty() || text.endsWith(" ")) text + t else "$text $t") }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) { Text(t, style = MaterialTheme.typography.labelMedium) }
            }
        }
    }
}

@Composable
private fun SendFields(x: AutoAction, onChange: (AutoAction) -> Unit) {
    val context = LocalContext.current
    val store = remember { Store.get(context) }
    FieldLabel(stringResource(R.string.step_send_target))
    val target = runCatching { SendTarget.valueOf(x.deviceId) }.getOrDefault(SendTarget.NTFY)
    OptionChips(SendTarget.entries, target, { ActionKind.SEND.icon() }, { sendTargetName(it.name) }) { onChange(x.copy(deviceId = it.name)) }
    val ready = when (target) {
        SendTarget.NTFY -> com.macerce.switchguard.core.Heartbeat.isValidUrl(store.ntfyUrl)
        SendTarget.WEBHOOK -> com.macerce.switchguard.core.Heartbeat.isValidUrl(store.webhookUrl)
        SendTarget.TELEGRAM -> store.telegramToken.isNotEmpty() && store.telegramRecipients.isNotEmpty()
    }
    if (!ready) Text(stringResource(R.string.step_send_not_set), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error,
        modifier = Modifier.padding(top = 6.dp))
    MessageField(x.text) { onChange(x.copy(text = it)) }
}

@Composable
private fun CounterFields(x: AutoAction, c: StepCtx, onChange: (AutoAction) -> Unit) {
    val counters = c.helpers.filter { it.kind == HelperKind.COUNTER }
    FieldLabel(stringResource(R.string.step_counter_pick))
    if (counters.isEmpty()) return Hint(stringResource(R.string.step_counter_none))
    OptionGrid(counters.map { it.deviceId }, x.deviceId, { Icons.Rounded.Numbers }, { id -> counters.first { it.deviceId == id }.name }) { onChange(x.copy(deviceId = it)) }
    var text by rememberSaveable(x.kind) { mutableStateOf(x.count.toString()) }
    NumberField(text, stringResource(if (x.kind == ActionKind.COUNTER_ADD) R.string.step_counter_add_value else R.string.step_counter_set_value), "",
        allowNegative = true) { t ->
        text = t.filter { it.isDigit() || it == '-' }
        text.toIntOrNull()?.let { onChange(x.copy(count = it)) }
    }
}

@Composable
private fun LevelFields(x: AutoAction, c: StepCtx, onChange: (AutoAction) -> Unit) {
    FieldLabel(stringResource(R.string.automation_device))
    if (c.lights.isEmpty()) return Hint(stringResource(R.string.step_level_none))
    DeviceChooser(c.lights, x.deviceId) { onChange(x.copy(deviceId = it.id, channel = 0)) }
    ToggleLine(stringResource(R.string.step_level_brightness), x.level != null) { onChange(x.copy(level = if (it) 80 else null)) }
    x.level?.let { lv ->
        Stepper(stringResource(R.string.step_level_brightness), lv, listOf(1, 5, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100), format = { "$it%" }) { onChange(x.copy(level = it)) }
    }
    ToggleLine(stringResource(R.string.step_level_color), x.color != null) { onChange(x.copy(color = if (it) LightLevel.PRESETS.first() else null)) }
    x.color?.let { selected ->
        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LightLevel.PRESETS.forEach { rgb ->
                val on = rgb == selected
                Box(
                    Modifier.size(34.dp).clip(CircleShape).background(Color(0xFF000000 or rgb.toLong()))
                        .border(if (on) 3.dp else 1.dp, if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, CircleShape)
                        .tvFocusRing(CircleShape)
                        .clickable { onChange(x.copy(color = rgb)) },
                )
            }
        }
    }
}

/** Kural yeniden tetiklenince: baştan başla / yok say / sıraya al / aynı anda (+ en çok kaç). */
@Composable
internal fun RunModeBlock(mode: RunMode, maxRuns: Int, onChange: (RunMode, Int) -> Unit) {
    FieldLabel(stringResource(R.string.run_mode_title))
    OptionChips(RunMode.entries, mode, { it.icon() }, { stringResource(it.title()) }) { onChange(it, maxRuns) }
    if (mode == RunMode.QUEUED || mode == RunMode.PARALLEL) {
        Stepper(stringResource(R.string.run_mode_max), maxRuns, (2..10).toList(), format = { stringResource(R.string.run_mode_n, it) }) { onChange(mode, it) }
    }
}

private fun RunMode.title(): Int = when (this) {
    RunMode.RESTART -> R.string.run_mode_restart
    RunMode.SINGLE -> R.string.run_mode_single
    RunMode.QUEUED -> R.string.run_mode_queued
    RunMode.PARALLEL -> R.string.run_mode_parallel
}

private fun RunMode.icon() = when (this) {
    RunMode.RESTART -> Icons.Rounded.PlayArrow
    RunMode.SINGLE -> Icons.Rounded.Stop
    RunMode.QUEUED -> Icons.Rounded.KeyboardArrowDown
    RunMode.PARALLEL -> Icons.Rounded.DoneAll
}

// ---------------------------------------------------------------- yardımcılar

internal fun HelperKind.icon() = when (this) {
    HelperKind.TOGGLE -> Icons.Rounded.ToggleOn
    HelperKind.COUNTER -> Icons.Rounded.Numbers
    HelperKind.TIMER -> Icons.Rounded.Timer
}

internal fun HelperKind.title(): Int = when (this) {
    HelperKind.TOGGLE -> R.string.helper_kind_toggle
    HelperKind.COUNTER -> R.string.helper_kind_counter
    HelperKind.TIMER -> R.string.helper_kind_timer
}

/** Yardımcılar şeridi: her kartta hızlı denetim (anahtar, başlat/durdur, −/+), kalemle düzenleme; sonda "Yeni". */
@Composable
internal fun HelpersStrip(helpers: List<Helper>, onEdit: (String) -> Unit) {
    val context = LocalContext.current
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(helpers, key = { it.id }) { h ->
            val shape = RoundedCornerShape(20.dp)
            Column(
                Modifier.width(160.dp).clip(shape).background(tileColor()).tvFocusRing(shape)
                    .padding(start = 14.dp, top = 12.dp, bottom = 8.dp, end = 4.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HueIcon(h.kind.icon(), if (h.kind == HelperKind.COUNTER) Hue.ORANGE else Hue.INDIGO, size = 30.dp)
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { onEdit(h.id) }, modifier = Modifier.tvFocusRing()) {
                        Icon(Icons.Rounded.Edit, stringResource(R.string.helper_edit), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Text(h.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                when (h.kind) {
                    HelperKind.TOGGLE -> Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(if (h.on) R.string.helper_on else R.string.helper_off), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                        Switch(checked = h.on, onCheckedChange = { HelperControl.switch(context, h.deviceId, it) })
                    }
                    HelperKind.TIMER -> Row(verticalAlignment = Alignment.CenterVertically) {
                        val left = ((h.endsAt - System.currentTimeMillis()) / 60_000L + 1).toInt().coerceAtLeast(1)
                        Text(if (h.on) stringResource(R.string.helper_timer_left, holdLabel(left)) else stringResource(R.string.helper_timer_idle),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                        IconButton(modifier = Modifier.tvFocusRing(), onClick = { HelperControl.switch(context, h.deviceId, !h.on) }) {
                            Icon(if (h.on) Icons.Rounded.Stop else Icons.Rounded.PlayArrow, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    HelperKind.COUNTER -> Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(modifier = Modifier.tvFocusRing(), onClick = { HelperControl.count(context, h.deviceId, add = -1) }) { Icon(Icons.Rounded.Remove, null) }
                        Text(h.value.toString(), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        IconButton(modifier = Modifier.tvFocusRing(), onClick = { HelperControl.count(context, h.deviceId, add = 1) }) { Icon(Icons.Rounded.Add, null) }
                    }
                }
            }
        }
        item {
            val shape = RoundedCornerShape(20.dp)
            Box(
                Modifier.width(120.dp).heightIn(min = 104.dp).clip(shape).background(tileColor()).tvFocusRing(shape)
                    .clickable { onEdit(NEW_AUTOMATION) },
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.Add, null, tint = MaterialTheme.colorScheme.primary)
                    Text(stringResource(R.string.helper_add), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

/** Yardımcı ekleme/düzenleme penceresi: ad, tür (yalnız yenisinde), zamanlayıcı süresi, sayaçta sıfırla. */
@Composable
internal fun HelperDialog(initial: Helper?, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var name by rememberSaveable { mutableStateOf(initial?.name.orEmpty()) }
    var kind by rememberSaveable { mutableStateOf(initial?.kind ?: HelperKind.TOGGLE) }
    var minutes by rememberSaveable { mutableStateOf(initial?.minutes ?: 10) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (initial == null) R.string.helper_add else R.string.helper_edit)) },
        text = {
            Column {
                Text(stringResource(R.string.helpers_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = name, onValueChange = { name = it }, singleLine = true,
                    label = { Text(stringResource(R.string.helper_name)) },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp).tvTextField(),
                    keyboardOptions = tvKeyboardOptions(),
                )
                if (initial == null) {
                    FieldLabel(stringResource(R.string.helper_kind))
                    OptionChips(HelperKind.entries, kind, { it.icon() }, { stringResource(it.title()) }) { kind = it }
                }
                if (kind == HelperKind.TIMER) {
                    Stepper(stringResource(R.string.helper_timer_minutes), minutes, listOf(1, 2, 3, 5, 10, 15, 20, 30, 45, 60, 90, 120, 180, 240, 360, 480, 720, 1440),
                        format = { holdLabel(it) }) { minutes = it }
                }
                if (initial != null) {
                    if (initial.kind == HelperKind.COUNTER) {
                        TextButton(modifier = Modifier.tvFocusRing(), onClick = { HelperControl.count(context, initial.deviceId, set = 0) }) {
                            Text(stringResource(R.string.helper_counter_reset))
                        }
                    }
                    TextButton(modifier = Modifier.tvFocusRing(), onClick = { HelperControl.delete(context, initial.id); onDismiss() }) {
                        Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), modifier = Modifier.tvFocusRing(), onClick = {
                val h = initial?.copy(name = name.trim(), minutes = minutes)
                    ?: Helper(UUID.randomUUID().toString().take(8), name.trim(), kind, minutes = minutes)
                HelperControl.save(context, h)
                onDismiss()
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(modifier = Modifier.tvFocusRing(), onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

/** Düzenleyici için yerel ışıklar: parlaklığı/rengi ayarlanabilen yerel cihazlar. */
internal fun lightTargets(store: Store, devices: List<DeviceSnapshot>): List<DeviceSnapshot> {
    val locals = store.localDevices.associateBy { it.id }
    return devices.filter { d -> locals[d.id]?.let { l -> (0 until l.channels.coerceAtLeast(1)).any { LightLevel.supports(l, it) } } == true }
}
