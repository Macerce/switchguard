package com.macerce.switchguard.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CloudQueue
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PowerOff
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.macerce.switchguard.R
import com.macerce.switchguard.core.AlertAction
import com.macerce.switchguard.core.EventType
import com.macerce.switchguard.data.EventLog
import com.macerce.switchguard.data.Store

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceDetailScreen(deviceId: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val store = remember { Store.get(context) }
    val log = remember { EventLog.get(context) }
    val version by store.version.collectAsStateWithLifecycle()
    val logVersion by log.version.collectAsStateWithLifecycle()

    val device = remember(version) { store.snapshots.firstOrNull { it.id == deviceId } }
    val rules = remember(version) { store.rulesFor(deviceId) }
    val events = remember(logVersion) { log.recent(deviceId, limit = 15) }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(device?.name ?: "", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_back)) }
            },
        )
        if (device == null) {
            Text(stringResource(R.string.device_missing), Modifier.padding(24.dp))
            return
        }
        Column(Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding().padding(bottom = 24.dp)) {
            SectionTitle(stringResource(R.string.section_control))
            SectionCard {
                Column(Modifier.padding(vertical = 8.dp)) {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) { OnlineDot(device.online) }
                    device.switches.toSortedMap().forEach { (ch, on) -> ChannelRow(device, ch, on) }
                }
            }

            // Enerji ölçmeyen cihazlarda bu bölüm hiç görünmez.
            if (device.hasEnergy) EnergySection(device)

            TimersSection(device)

            SectionTitle(stringResource(R.string.section_monitoring))
            SectionCard {
                InfoRow(
                    Icons.Rounded.NotificationsActive,
                    stringResource(R.string.monitor_this_device),
                    stringResource(R.string.monitor_this_device_desc),
                ) {
                    Switch(checked = rules.monitored, onCheckedChange = { store.setRules(deviceId, rules.copy(monitored = it)) })
                }
            }

            if (rules.monitored) {
                SectionTitle(stringResource(R.string.section_rules))
                SectionCard {
                    EventType.entries.forEachIndexed { i, type ->
                        if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.surface)
                        RuleRow(type, rules.actionFor(type)) { store.setRules(deviceId, rules.with(type, it)) }
                    }
                }
                Text(
                    stringResource(R.string.rules_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )
            }

            SectionTitle(stringResource(R.string.section_recent_events))
            SectionCard {
                if (events.isEmpty()) {
                    Text(stringResource(R.string.no_events), Modifier.padding(20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    events.forEachIndexed { i, e ->
                        if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.surface)
                        EventRow(e, showDevice = false)
                    }
                }
            }

            SectionTitle(stringResource(R.string.section_advanced))
            SectionCard { RawDataRow(deviceId) }
        }
    }
}

private fun EventType.icon(): ImageVector = when (this) {
    EventType.TURNED_OFF -> Icons.Rounded.PowerOff
    EventType.TURNED_ON -> Icons.Rounded.PowerSettingsNew
    EventType.WENT_OFFLINE -> Icons.Rounded.CloudOff
    EventType.CAME_ONLINE -> Icons.Rounded.CloudQueue
}

fun EventType.title(): Int = when (this) {
    EventType.TURNED_OFF -> R.string.event_turned_off
    EventType.TURNED_ON -> R.string.event_turned_on
    EventType.WENT_OFFLINE -> R.string.event_went_offline
    EventType.CAME_ONLINE -> R.string.event_came_online
}

fun AlertAction.title(): Int = when (this) {
    AlertAction.ALARM -> R.string.action_alarm
    AlertAction.NOTIFY -> R.string.action_notify
    AlertAction.IGNORE -> R.string.action_ignore
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RuleRow(type: EventType, current: AlertAction, onChange: (AlertAction) -> Unit) {
    Column(Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(type.icon(), null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(12.dp))
            Text(stringResource(type.title()), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(10.dp))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            AlertAction.entries.forEachIndexed { i, action ->
                SegmentedButton(
                    selected = current == action,
                    onClick = { onChange(action) },
                    shape = SegmentedButtonDefaults.itemShape(i, AlertAction.entries.size),
                ) {
                    Text(stringResource(action.title()), fontWeight = if (current == action) FontWeight.SemiBold else null)
                }
            }
        }
    }
}
