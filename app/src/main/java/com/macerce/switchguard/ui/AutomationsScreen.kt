package com.macerce.switchguard.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoMode
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.macerce.switchguard.R
import com.macerce.switchguard.core.ActionKind
import com.macerce.switchguard.core.AutoAction
import com.macerce.switchguard.core.Automation
import com.macerce.switchguard.core.DeviceSnapshot
import com.macerce.switchguard.core.Trigger
import com.macerce.switchguard.core.TriggerKind
import com.macerce.switchguard.data.Store
import java.util.UUID

private const val NEW = "new"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutomationsScreen(modifier: Modifier) {
    val context = LocalContext.current
    val store = remember { Store.get(context) }
    val version by store.version.collectAsStateWithLifecycle()
    val automations = remember(version) { store.automations }
    val devices = remember(version) { store.snapshots }
    val monitoring = remember(version) { store.monitoringEnabled }
    var editing by rememberSaveable { mutableStateOf<String?>(null) }

    editing?.let { id ->
        BackHandler { editing = null }
        AutomationEditor(
            initial = automations.firstOrNull { it.id == id },
            devices = devices,
            modifier = modifier,
            onSave = { store.saveAutomation(it); editing = null },
            onDelete = { store.deleteAutomation(id); editing = null },
            onBack = { editing = null },
        )
        return
    }

    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            TopAppBar(title = { Text(stringResource(R.string.tab_automations), fontWeight = FontWeight.SemiBold) })
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (!monitoring && automations.isNotEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.automations_need_monitoring),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 4.dp),
                        )
                    }
                }
                if (automations.isEmpty()) {
                    item { EmptyAutomations() }
                }
                items(automations, key = { it.id }) { a ->
                    AutomationCard(a, devices, onClick = { editing = a.id }, onToggle = { store.saveAutomation(a.copy(enabled = it)) })
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = { editing = NEW },
            icon = { Icon(Icons.Rounded.Add, null) },
            text = { Text(stringResource(R.string.automation_add)) },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        )
    }
}

@Composable
private fun EmptyAutomations() {
    Column(Modifier.fillMaxWidth().padding(vertical = 32.dp, horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Rounded.AutoMode, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.automations_empty), style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.automations_examples),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AutomationCard(a: Automation, devices: List<DeviceSnapshot>, onClick: () -> Unit, onToggle: (Boolean) -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(20.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(a.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(
                    describe(a, devices),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(12.dp))
            Switch(checked = a.enabled, onCheckedChange = onToggle)
        }
    }
}

/** "Rögar · Kanal 1 açılınca → Lamba aç (5 dk sonra)" */
@Composable
private fun describe(a: Automation, devices: List<DeviceSnapshot>): String {
    val t = a.trigger
    val src = devices.firstOrNull { it.id == t.deviceId }
    val srcName = src?.let { channelLabel(it, t.channel, t.kind.usesChannel) } ?: stringResource(R.string.device_missing)
    val trigger = when (t.kind) {
        TriggerKind.STAYED_ON, TriggerKind.STAYED_OFF -> stringResource(t.kind.title()) + " " + stringResource(R.string.minutes_n, t.minutes)
        TriggerKind.POWER_ABOVE, TriggerKind.POWER_BELOW -> stringResource(t.kind.title()) + " " + formatWatts(t.watts)
        else -> stringResource(t.kind.title())
    }
    val act = a.action
    val action = if (act.kind.usesDevice) {
        val dst = devices.firstOrNull { it.id == act.deviceId }
        val dstName = dst?.let { channelLabel(it, act.channel, true) } ?: stringResource(R.string.device_missing)
        "$dstName: " + stringResource(act.kind.title())
    } else stringResource(act.kind.title())
    val delay = if (a.delayMinutes > 0) " " + stringResource(R.string.automation_after, a.delayMinutes) else ""
    return "$srcName · $trigger → $action$delay"
}

@Composable
private fun channelLabel(d: DeviceSnapshot, channel: Int, withChannel: Boolean): String =
    if (withChannel && d.isMultiChannel) "${d.name} · " + stringResource(R.string.channel_n, channel + 1) else d.name

fun TriggerKind.title(): Int = when (this) {
    TriggerKind.TURNED_ON -> R.string.trigger_turned_on
    TriggerKind.TURNED_OFF -> R.string.trigger_turned_off
    TriggerKind.STAYED_ON -> R.string.trigger_stayed_on
    TriggerKind.STAYED_OFF -> R.string.trigger_stayed_off
    TriggerKind.WENT_OFFLINE -> R.string.trigger_went_offline
    TriggerKind.CAME_ONLINE -> R.string.trigger_came_online
    TriggerKind.POWER_ABOVE -> R.string.trigger_power_above
    TriggerKind.POWER_BELOW -> R.string.trigger_power_below
}

fun ActionKind.title(): Int = when (this) {
    ActionKind.TURN_ON -> R.string.auto_action_turn_on
    ActionKind.TURN_OFF -> R.string.auto_action_turn_off
    ActionKind.ALARM -> R.string.auto_action_alarm
    ActionKind.NOTIFY -> R.string.auto_action_notify
}

// ---------------------------------------------------------------- düzenleyici

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AutomationEditor(
    initial: Automation?,
    devices: List<DeviceSnapshot>,
    modifier: Modifier,
    onSave: (Automation) -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
) {
    val first = devices.firstOrNull()
    var name by rememberSaveable { mutableStateOf(initial?.name ?: "") }
    var triggerDevice by rememberSaveable { mutableStateOf(initial?.trigger?.deviceId ?: first?.id.orEmpty()) }
    var triggerKind by rememberSaveable { mutableStateOf(initial?.trigger?.kind ?: TriggerKind.TURNED_ON) }
    var triggerChannel by rememberSaveable { mutableStateOf(initial?.trigger?.channel ?: 0) }
    var minutes by rememberSaveable { mutableStateOf(initial?.trigger?.minutes?.takeIf { it > 0 }?.toString() ?: "30") }
    var watts by rememberSaveable { mutableStateOf(initial?.trigger?.watts?.takeIf { it > 0 }?.let(::formatNumber) ?: "1000") }
    var actionKind by rememberSaveable { mutableStateOf(initial?.action?.kind ?: ActionKind.NOTIFY) }
    var actionDevice by rememberSaveable { mutableStateOf(initial?.action?.deviceId?.ifEmpty { null } ?: first?.id.orEmpty()) }
    var actionChannel by rememberSaveable { mutableStateOf(initial?.action?.channel ?: 0) }
    var delay by rememberSaveable { mutableStateOf(initial?.delayMinutes?.toString() ?: "0") }
    var confirmDelete by remember { mutableStateOf(false) }

    val src = devices.firstOrNull { it.id == triggerDevice }
    val dst = devices.firstOrNull { it.id == actionDevice }
    // Güç tetikleyicileri yalnızca enerji ölçen cihazlarda sunulur.
    val kinds = TriggerKind.entries.filter { !it.usesWatts || src?.hasEnergy == true }
    if (triggerKind !in kinds) triggerKind = TriggerKind.TURNED_ON

    val minutesValue = minutes.toIntOrNull()
    val wattsValue = watts.replace(',', '.').toDoubleOrNull()
    val delayValue = delay.toIntOrNull()
    val valid = name.isNotBlank() && src != null &&
        (!triggerKind.usesMinutes || (minutesValue != null && minutesValue > 0)) &&
        (!triggerKind.usesWatts || (wattsValue != null && wattsValue > 0)) &&
        (!actionKind.usesDevice || dst != null) &&
        delayValue != null && delayValue >= 0

    Column(modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(if (initial == null) R.string.automation_new else R.string.automation_edit)) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_back)) } },
            actions = {
                if (initial != null) {
                    IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Rounded.Delete, stringResource(R.string.action_delete)) }
                }
            },
        )
        if (devices.isEmpty()) {
            Text(stringResource(R.string.no_devices), Modifier.padding(24.dp))
            return
        }
        Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text(stringResource(R.string.automation_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )

            SectionTitle(stringResource(R.string.automation_when))
            SectionCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Picker(stringResource(R.string.automation_device), devices, src, { it.name }) {
                        triggerDevice = it.id; triggerChannel = 0
                    }
                    Picker(stringResource(R.string.automation_event), kinds, triggerKind, { stringResource(it.title()) }) { triggerKind = it }
                    if (triggerKind.usesChannel && src?.isMultiChannel == true) {
                        ChannelPicker(src, triggerChannel) { triggerChannel = it }
                    }
                    if (triggerKind.usesMinutes) {
                        NumberField(minutes, stringResource(R.string.automation_minutes)) { minutes = it }
                    }
                    if (triggerKind.usesWatts) {
                        NumberField(watts, stringResource(R.string.automation_watts), decimal = true) { watts = it }
                        src?.power?.let {
                            Text(
                                stringResource(R.string.automation_current_power, formatWatts(it)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            SectionTitle(stringResource(R.string.automation_then))
            SectionCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Picker(stringResource(R.string.automation_action), ActionKind.entries, actionKind, { stringResource(it.title()) }) { actionKind = it }
                    if (actionKind.usesDevice) {
                        Picker(stringResource(R.string.automation_device), devices, dst, { it.name }) {
                            actionDevice = it.id; actionChannel = 0
                        }
                        if (dst?.isMultiChannel == true) ChannelPicker(dst, actionChannel) { actionChannel = it }
                    }
                    NumberField(delay, stringResource(R.string.automation_delay)) { delay = it }
                }
            }
            Text(
                stringResource(R.string.automation_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )

            Button(
                enabled = valid,
                onClick = {
                    onSave(
                        Automation(
                            id = initial?.id ?: UUID.randomUUID().toString(),
                            name = name.trim(),
                            enabled = initial?.enabled ?: true,
                            trigger = Trigger(
                                triggerKind, triggerDevice,
                                channel = if (triggerKind.usesChannel) triggerChannel else 0,
                                minutes = if (triggerKind.usesMinutes) minutesValue ?: 0 else 0,
                                watts = if (triggerKind.usesWatts) wattsValue ?: 0.0 else 0.0,
                            ),
                            action = if (actionKind.usesDevice) AutoAction(actionKind, actionDevice, actionChannel) else AutoAction(actionKind),
                            delayMinutes = delayValue ?: 0,
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).height(52.dp),
            ) { Text(stringResource(R.string.action_save)) }
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.automation_delete_title),
            body = name,
            confirm = stringResource(R.string.action_delete),
            onConfirm = { confirmDelete = false; onDelete() },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun ChannelPicker(device: DeviceSnapshot, selected: Int, onPick: (Int) -> Unit) {
    val channels = device.switches.keys.sorted()
    Picker(stringResource(R.string.automation_channel), channels, selected, { stringResource(R.string.channel_n, it + 1) }, onPick)
}

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

/** Açılır seçim kutusu. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> Picker(label: String, options: List<T>, selected: T?, text: @Composable (T) -> String, onPick: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected?.let { text(it) } ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { o ->
                DropdownMenuItem(text = { Text(text(o)) }, onClick = { onPick(o); expanded = false })
            }
        }
    }
}
