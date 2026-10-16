package com.macerce.switchguard.desktop.ui.automations

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.macerce.switchguard.core.ActionKind
import com.macerce.switchguard.core.Automation
import com.macerce.switchguard.core.DeviceSnapshot
import com.macerce.switchguard.core.TriggerKind
import com.macerce.switchguard.desktop.AppGraph
import com.macerce.switchguard.desktop.i18n.Strings
import java.util.Locale
import java.util.UUID

/** Yeni otomasyon düzenleyicisinin işareti. */
private const val NEW = "new"

/**
 * Uyarılar (telefondaki "Otomasyon" ekranının masaüstü karşılığı; kullanıcı burada "uyarı" der): liste, aç/kapa, düzenleyici. Ekleme, değiştirme
 * ve silme motor üzerinden yapılır (`MonitorEngine.saveAutomation` / `deleteAutomation`); değerlendirme zaten motorda.
 */
@Composable
fun AutomationsScreen(graph: AppGraph, strings: Strings, modifier: Modifier = Modifier) {
    val store = graph.store
    val version by store.version.collectAsState()
    val automations = remember(version) { store.automations }
    val devices = remember(version) { store.snapshots }
    val monitoring = remember(version) { store.monitoringEnabled }
    var editing by remember { mutableStateOf<String?>(null) }

    editing?.let { id ->
        AutomationEditor(
            initial = automations.firstOrNull { it.id == id },
            devices = devices,
            strings = strings,
            modifier = modifier,
            onSave = {
                graph.engine.saveAutomation(it)
                editing = null
            },
            onDelete = {
                graph.engine.deleteAutomation(id)
                editing = null
            },
            onBack = { editing = null },
        )
        return
    }

    Box(modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Text(strings["desktop_alerts_tab"], style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) }
            if (!monitoring && automations.isNotEmpty()) {
                item { Text(strings["desktop_alerts_need_monitoring"], style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error) }
            }
            if (automations.isEmpty()) {
                item {
                    Column(Modifier.padding(vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(strings["desktop_alerts_empty"], style = MaterialTheme.typography.bodyLarge)
                        Text(strings["automations_examples"], style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            items(automations, key = { it.id }) { a ->
                AutomationCard(
                    a, devices, strings,
                    onClick = { editing = a.id },
                    onToggle = { on -> graph.engine.saveAutomation(a.copy(enabled = on)) },
                )
            }
            item { Text(strings["desktop_automation_hint"], style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        ExtendedFloatingActionButton(
            onClick = { editing = NEW },
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text(strings["automation_add"]) },
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp).testTag("add-automation"),
        )
    }
}

@Composable
private fun AutomationCard(a: Automation, devices: List<DeviceSnapshot>, strings: Strings, onClick: () -> Unit, onToggle: (Boolean) -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth().widthIn(max = 900.dp).clickable(onClick = onClick), shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(a.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(describe(a, devices, strings), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(12.dp))
            Switch(checked = a.enabled, onCheckedChange = onToggle, modifier = Modifier.testTag("enabled-${a.id}"))
        }
    }
}

/** "Rögar · Kanal 1 · açılınca → Lamba: Aç (5 dk sonra)" (telefondaki `describe`). */
internal fun describe(a: Automation, devices: List<DeviceSnapshot>, strings: Strings): String {
    val t = a.trigger
    val srcName = devices.firstOrNull { it.id == t.deviceId }?.let { channelLabel(it, t.channel, t.kind.usesChannel, strings) } ?: strings["device_missing"]
    val trigger = when (t.kind) {
        TriggerKind.STAYED_ON, TriggerKind.STAYED_OFF -> strings[t.kind.titleKey()] + " " + strings.plural("minutes_n", t.minutes, t.minutes)
        TriggerKind.POWER_ABOVE, TriggerKind.POWER_BELOW -> strings[t.kind.titleKey()] + " " + formatWatts(t.watts)
        TriggerKind.CHANNEL_POWER_LOW -> strings[t.kind.titleKey()] + " " + formatWatts(t.watts) + " " +
            strings.format("automation_low_for", t.minutes, t.graceMinutes)
        else -> strings[t.kind.titleKey()]
    }
    val act = a.action
    val action = if (act.kind.usesDevice) {
        val dstName = devices.firstOrNull { it.id == act.deviceId }?.let { channelLabel(it, act.channel, true, strings) } ?: strings["device_missing"]
        "$dstName: " + strings[act.kind.titleKey()]
    } else {
        strings[act.kind.titleKey()]
    }
    val delay = if (a.delayMinutes > 0) " " + strings.format("automation_after", a.delayMinutes) else ""
    return "$srcName · $trigger → $action$delay"
}

private fun channelLabel(d: DeviceSnapshot, channel: Int, withChannel: Boolean, strings: Strings): String =
    if (withChannel && d.isMultiChannel) "${d.name} · " + strings.format("channel_n", channel + 1) else d.name

internal fun TriggerKind.titleKey(): String = when (this) {
    TriggerKind.TURNED_ON -> "trigger_turned_on"
    TriggerKind.TURNED_OFF -> "trigger_turned_off"
    TriggerKind.STAYED_ON -> "trigger_stayed_on"
    TriggerKind.STAYED_OFF -> "trigger_stayed_off"
    TriggerKind.WENT_OFFLINE -> "trigger_went_offline"
    TriggerKind.CAME_ONLINE -> "trigger_came_online"
    TriggerKind.POWER_ABOVE -> "trigger_power_above"
    TriggerKind.POWER_BELOW -> "trigger_power_below"
    TriggerKind.CHANNEL_POWER_LOW -> "trigger_channel_power_low"
}

internal fun ActionKind.titleKey(): String = when (this) {
    ActionKind.TURN_ON -> "auto_action_turn_on"
    ActionKind.TURN_OFF -> "auto_action_turn_off"
    ActionKind.ALARM -> "auto_action_alarm"
    ActionKind.NOTIFY -> "auto_action_notify"
}

private fun formatWatts(w: Double): String =
    if (w >= 1000) String.format(Locale.getDefault(), "%.2f kW", w / 1000) else String.format(Locale.getDefault(), "%.0f W", w)

/** Telefondaki düzenleyici: ad, ne zaman (cihaz, olay, kanal, süre/eşik), ne yapılsın (işlem, cihaz, kanal), gecikme. */
@Composable
private fun AutomationEditor(
    initial: Automation?,
    devices: List<DeviceSnapshot>,
    strings: Strings,
    modifier: Modifier,
    onSave: (Automation) -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
) {
    var form by remember { mutableStateOf(AutomationForm.from(initial, devices)) }
    var confirmDelete by remember { mutableStateOf(false) }
    val f = form.normalized(devices)
    val src = devices.firstOrNull { it.id == f.triggerDevice }
    val dst = devices.firstOrNull { it.id == f.actionDevice }

    Column(modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings["action_back"]) }
            Text(
                strings[if (initial == null) "desktop_alert_new" else "desktop_alert_edit"],
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            if (initial != null) TextButton(onClick = { confirmDelete = true }) { Text(strings["action_delete"]) }
        }
        if (devices.isEmpty()) {
            Text(strings["no_devices"], Modifier.padding(24.dp))
            return@Column
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).widthIn(max = 720.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(f.name, { form = f.copy(name = it) }, Modifier.fillMaxWidth(), label = { Text(strings["automation_name"]) }, singleLine = true)

            Text(strings["automation_when"], style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Picker(strings["automation_device"], devices, src, { it.name }, "pick-trigger-device") {
                form = f.copy(triggerDevice = it.id, triggerChannel = 0)
            }
            Picker(strings["automation_event"], AutomationForm.kindsFor(src), f.triggerKind, { titleCase(strings[it.titleKey()], strings.language) }, "pick-event") {
                form = f.copy(triggerKind = it)
            }
            if (f.triggerKind.usesChannel && src != null && src.isMultiChannel) {
                Picker(strings["automation_channel"], src.switches.keys.sorted(), f.triggerChannel, { strings.format("channel_n", it + 1) }, "pick-trigger-channel") {
                    form = f.copy(triggerChannel = it)
                }
            }
            if (f.triggerKind.usesMinutes) NumberField(f.minutes, strings["automation_minutes"]) { form = f.copy(minutes = it) }
            if (f.triggerKind.usesWatts) {
                NumberField(f.watts, strings["automation_watts"], decimal = true) { form = f.copy(watts = it) }
                src?.power?.let { Text(strings.format("automation_current_power", formatWatts(it)), style = MaterialTheme.typography.bodySmall) }
            }
            if (f.triggerKind.usesChannelPower) {
                NumberField(f.lowMinutes, strings["automation_low_minutes"]) { form = f.copy(lowMinutes = it) }
                NumberField(f.grace, strings["automation_grace"]) { form = f.copy(grace = it) }
            }

            Text(strings["automation_then"], style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Picker(strings["automation_action"], ActionKind.entries, f.actionKind, { strings[it.titleKey()] }, "pick-action") {
                form = f.copy(actionKind = it)
            }
            if (f.actionKind.usesDevice) {
                Picker(strings["automation_device"], devices, dst, { it.name }, "pick-action-device") {
                    form = f.copy(actionDevice = it.id, actionChannel = 0)
                }
                if (dst != null && dst.isMultiChannel) {
                    Picker(strings["automation_channel"], dst.switches.keys.sorted(), f.actionChannel, { strings.format("channel_n", it + 1) }, "pick-action-channel") {
                        form = f.copy(actionChannel = it)
                    }
                }
            }
            NumberField(f.delay, strings["automation_delay"]) { form = f.copy(delay = it) }
            Text(strings["desktop_automation_hint"], style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Button(
                enabled = f.isValid(devices),
                onClick = { onSave(f.build(initial?.id ?: UUID.randomUUID().toString(), initial?.enabled ?: true)) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).height(48.dp),
            ) { Text(strings["action_save"]) }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            text = { Text(strings["desktop_alert_delete_title"]) },
            confirmButton = {
                Button(onClick = {
                    confirmDelete = false
                    onDelete()
                }, modifier = Modifier.testTag("confirm-delete")) { Text(strings["action_delete"]) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(strings["action_cancel"]) } },
        )
    }
}

/** Yalnızca rakam (ve ondalıksa nokta/virgül) kabul eden alan. */
@Composable
private fun NumberField(value: String, label: String, decimal: Boolean = false, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { v -> onChange(v.filter { it.isDigit() || (decimal && (it == '.' || it == ',')) }) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Açılır seçim: "Etiket: seçili" düğmesi, tıklanınca seçenek menüsü. */
@Composable
private fun <T> Picker(label: String, options: List<T>, selected: T?, text: (T) -> String, tag: String, onPick: (T) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth().testTag(tag)) {
            Text("$label: " + (selected?.let(text) ?: "—"), Modifier.weight(1f))
        }
        DropdownMenu(open, { open = false }) {
            options.forEach { o ->
                DropdownMenuItem(text = { Text(text(o)) }, onClick = {
                    open = false
                    onPick(o)
                })
            }
        }
    }
}
