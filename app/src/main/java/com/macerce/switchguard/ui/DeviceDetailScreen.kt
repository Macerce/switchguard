package com.macerce.switchguard.ui

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CloudQueue
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NotificationImportant
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PowerOff
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import com.macerce.switchguard.core.Cloud
import com.macerce.switchguard.core.Demo
import com.macerce.switchguard.core.Entitlement
import com.macerce.switchguard.core.EventType
import com.macerce.switchguard.data.EventLog
import com.macerce.switchguard.data.Store
import com.macerce.switchguard.core.DeviceSnapshot
import com.macerce.switchguard.ui.theme.Brand
import com.macerce.switchguard.ui.theme.LocalDarkTheme
import com.macerce.switchguard.ui.theme.StatusColors
import com.macerce.switchguard.ui.theme.tileColor
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor

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
    // Kural açık olsa bile ücretsiz sürümde başka cihaz izleniyorsa bu cihaz izlenmez.
    val monitoredIds = remember(version) { store.monitoredIds() }
    val monitored = deviceId in monitoredIds
    var dialog by remember { mutableStateOf<(@Composable () -> Unit)?>(null) }
    dialog?.invoke()
    val events = remember(logVersion) { log.recent(deviceId, limit = 15) }

    Column(Modifier.fillMaxSize()) {
        AppTopBar(device?.name ?: "", navigationIcon = { BackButton(onBack) }, small = true)
        if (device == null) {
            Text(stringResource(R.string.device_missing), Modifier.padding(24.dp))
            return
        }
        Column(Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding().padding(bottom = 24.dp)) {
            ControlHero(device, monitored)

            if (device.cloud == Cloud.DEMO) DemoSection(device)

            // Enerji ölçmeyen cihazlarda bu bölüm hiç görünmez.
            // SPM/DualR3 veriyi ancak istenince gönderdiğinden, veri gelmeden de gösterilir (açılınca istenir).
            if (device.hasEnergy || device.hasChannelPower) EnergySection(device)

            // Zamanlayıcılar eWeLink cihazlarının kendi özelliği.
            if (device.cloud == Cloud.EWELINK) TimersSection(device)

            SectionTitle(stringResource(R.string.section_monitoring))
            SectionCard {
                // Demo cihazları ücretsiz sürüm sınırına sayılmaz.
                val demo = Demo.isDemo(deviceId)
                val other = monitoredIds.firstOrNull { it != deviceId && !Demo.isDemo(it) }.takeUnless { demo }
                InfoRow(
                    Icons.Rounded.NotificationsActive,
                    stringResource(R.string.monitor_this_device),
                    if (!monitored && !store.isPro && other != null) stringResource(R.string.pro_limit_detail, Entitlement.FREE_DEVICES)
                    else stringResource(R.string.monitor_this_device_desc),
                ) {
                    Switch(checked = monitored, onCheckedChange = { on ->
                        when {
                            !on -> store.setRules(deviceId, rules.copy(monitored = false))
                            store.isPro || other == null -> {
                                if (!demo) store.freeDeviceId = deviceId
                                store.setRules(deviceId, rules.copy(monitored = true))
                            }
                            else -> {
                                val otherName = store.snapshots.firstOrNull { it.id == other }?.name.orEmpty()
                                dialog = {
                                    FreeSlotDialog(
                                        otherName = otherName,
                                        onSwitch = {
                                            store.freeDeviceId = deviceId
                                            store.setRules(deviceId, rules.copy(monitored = true))
                                            dialog = null
                                        },
                                        onUpgrade = { dialog = { ProDialog(onDismiss = { dialog = null }) } },
                                        onDismiss = { dialog = null },
                                    )
                                }
                            }
                        }
                    })
                }
            }

            if (monitored) {
                SectionTitle(stringResource(R.string.section_rules))
                SectionCard {
                    EventType.entries.forEachIndexed { i, type ->
                        if (i > 0) RowDivider()
                        RuleRow(type, rules.actionFor(type)) { store.setRules(deviceId, rules.with(type, it)) }
                    }
                }
                Text(
                    stringResource(R.string.rules_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )

                val pickSound = rememberSoundPicker { uri -> store.setRules(deviceId, rules.copy(soundUri = uri?.toString())) }
                val custom = rules.soundUri?.let(Uri::parse)
                val soundName = remember(rules.soundUri) {
                    if (custom == null) null else soundTitle(context, custom)
                } ?: stringResource(if (custom == null) R.string.device_sound_default else R.string.default_sound)
                SectionCard {
                    InfoRow(
                        Icons.Rounded.MusicNote,
                        stringResource(R.string.device_sound),
                        soundName,
                        Modifier.clickable { pickSound(custom) },
                    ) {
                        if (custom != null) {
                            IconButton(modifier = Modifier.tvFocusRing(), onClick = { store.setRules(deviceId, rules.copy(soundUri = null)) }) {
                                Icon(Icons.Rounded.Close, stringResource(R.string.device_sound_reset))
                            }
                        }
                    }
                    RowDivider()
                    InfoRow(
                        Icons.Rounded.NotificationImportant,
                        stringResource(R.string.device_critical),
                        stringResource(R.string.device_critical_desc),
                    ) {
                        Switch(checked = rules.critical, onCheckedChange = { store.setRules(deviceId, rules.copy(critical = it)) })
                    }
                }
            }

            SectionTitle(stringResource(R.string.section_recent_events))
            SectionCard {
                if (events.isEmpty()) {
                    Text(stringResource(R.string.no_events), Modifier.padding(20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    events.forEachIndexed { i, e ->
                        if (i > 0) RowDivider()
                        EventRow(e, showDevice = false)
                    }
                }
            }

            SectionTitle(stringResource(R.string.section_advanced))
            SectionCard { RawDataRow(deviceId) }
        }
    }
}

/** Ücretsiz sürümde izleme hakkı başka cihazdayken: hakkı bu cihaza taşı ya da Pro'ya geç. */
@Composable
private fun FreeSlotDialog(otherName: String, onSwitch: () -> Unit, onUpgrade: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pro_limit_title)) },
        text = { Text(stringResource(R.string.pro_limit_body, Entitlement.FREE_DEVICES, otherName)) },
        confirmButton = { TextButton(modifier = Modifier.tvFocusRing(), onClick = onUpgrade) { Text(stringResource(R.string.pro_upgrade)) } },
        dismissButton = { TextButton(modifier = Modifier.tvFocusRing(), onClick = onSwitch) { Text(stringResource(R.string.pro_limit_switch)) } },
    )
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

/**
 * Detayın üstü: tek kanallı cihazda ortada büyük güç düğmesi (açıkken kart marka renginde), çok kanallıda
 * büyük kanal düğmeleri. Altında bağlantı, izleme ve (ölçülüyorsa) anlık güç.
 */
@Composable
private fun ControlHero(device: DeviceSnapshot, monitored: Boolean) {
    val dark = LocalDarkTheme.current
    val single = !device.isMultiChannel && device.switches.size == 1
    val anyOn = device.switches.values.any { it }
    val active = single && anyOn && device.online
    val background: Brush = if (active) Brand.active(dark) else SolidColor(tileColor())
    val content = if (active) Brand.onActive else MaterialTheme.colorScheme.onSurface
    val muted = if (active) Brand.onActive.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(28.dp)).background(background).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            DeviceGlyph(device, active, size = 40)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(if (device.online) R.string.label_online_title else R.string.label_offline_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = if (device.online) content else StatusColors.offline,
                )
                Text(
                    stringResource(if (monitored) R.string.monitored else R.string.not_monitored),
                    style = MaterialTheme.typography.bodySmall,
                    color = muted,
                )
            }
            if (device.hasEnergy && device.online) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(formatWatts(device.power ?: 0.0), style = MaterialTheme.typography.headlineSmall, color = content)
                    Text(stringResource(R.string.energy_power), style = MaterialTheme.typography.labelMedium, color = muted)
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        if (single) {
            val (channel, on) = device.switches.entries.first()
            val control = rememberSwitchControl(device, channel, on)
            PowerButton(control.shown, control.busy, device.online, control.toggle, size = 112.dp, onBrand = active)
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(if (control.shown) R.string.state_on_title else R.string.state_off_title),
                style = MaterialTheme.typography.headlineSmall,
                color = content,
            )
            Text(stringResource(R.string.tap_to_toggle), style = MaterialTheme.typography.bodySmall, color = muted)
            Spacer(Modifier.height(4.dp))
        } else {
            ChannelButtons(device, Modifier.fillMaxWidth(), height = 84)
        }
    }
}
