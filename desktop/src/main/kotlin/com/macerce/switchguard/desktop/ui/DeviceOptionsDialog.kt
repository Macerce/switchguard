package com.macerce.switchguard.desktop.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.macerce.switchguard.core.AlertAction
import com.macerce.switchguard.core.Cloud
import com.macerce.switchguard.core.Demo
import com.macerce.switchguard.core.DeviceRules
import com.macerce.switchguard.core.DeviceSnapshot
import com.macerce.switchguard.core.EventType
import com.macerce.switchguard.desktop.engine.MonitorEngine
import com.macerce.switchguard.desktop.i18n.Strings
import com.macerce.switchguard.desktop.ui.settings.SwitchRow

/** Kural satırlarındaki olaylar ve telefondaki etiketleri (DeviceDetailScreen ile aynı anahtarlar). */
private val EVENTS = listOf(
    EventType.TURNED_OFF to "event_turned_off",
    EventType.TURNED_ON to "event_turned_on",
    EventType.WENT_OFFLINE to "event_went_offline",
    EventType.CAME_ONLINE to "event_came_online",
)

private val ACTIONS = listOf(
    AlertAction.ALARM to "action_alarm",
    AlertAction.NOTIFY to "action_notify",
    AlertAction.IGNORE to "action_ignore",
)

/**
 * Kartın ⋮ → "Seçenekler…" penceresi: cihaza özel her şey burada (kartın kendisi sade kalır).
 * - İzleme ve kurallar: izlensin mi, olay başına ALARM / BİLDİRİM / YOK. Her seçim [onRules]'a **değişiklik olarak**
 *   gider (çağıran motora gönderir: `MonitorEngine.updateRules`; motor o anki kurala uygular).
 * - Görünüm: "Küçük kart" (telefondaki gibi; `DeviceLayout.compact`).
 * - Demo cihazında simülasyon: dışarıdan aç/kapa, bağlantı kopması, güç düşüşü ([onDemo]).
 * [device] canlıdır (çağıran her değişiklikte yeniden verir): simülasyon düğmelerinin metni durumu izler.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DeviceOptionsDialog(
    device: DeviceSnapshot,
    initialRules: DeviceRules,
    compact: Boolean,
    strings: Strings,
    onRules: ((DeviceRules) -> DeviceRules) -> Unit,
    onCompact: (Boolean) -> Unit,
    onDemo: (op: String, channel: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var rules by remember { mutableStateOf(initialRules) }
    fun set(change: (DeviceRules) -> DeviceRules) {
        rules = change(rules)
        onRules(change)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.format("desktop_options_title_x", device.name)) },
        text = {
            Column(
                Modifier.widthIn(min = 480.dp).heightIn(max = 600.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // --- İzleme ve kurallar ---
                Text(strings["desktop_section_rules"], style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                SwitchRow(strings["monitor_this_device"], strings["monitor_this_device_desc"], rules.monitored) { on ->
                    set { r -> r.copy(monitored = on) }
                }
                for ((type, label) in EVENTS) {
                    Text(strings[label], style = MaterialTheme.typography.bodyLarge)
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        ACTIONS.forEachIndexed { i, (action, actionLabel) ->
                            SegmentedButton(
                                selected = rules.actionFor(type) == action,
                                onClick = { set { r -> r.with(type, action) } },
                                shape = SegmentedButtonDefaults.itemShape(i, ACTIONS.size),
                                enabled = rules.monitored,
                                modifier = Modifier.testTag("rule-${type.name}-${action.name}"),
                            ) { Text(strings[actionLabel]) }
                        }
                    }
                }
                Text(strings["rules_hint"], style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                // --- Görünüm ---
                HorizontalDivider()
                Text(strings["desktop_section_view"], style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                SwitchRow(strings["desktop_compact_card"], strings["desktop_compact_card_desc"], compact, onChange = onCompact)

                // --- Demo simülasyonu (telefondaki DemoSection ile aynı üç işlem) ---
                if (device.cloud == Cloud.DEMO) {
                    HorizontalDivider()
                    Text(strings["desktop_section_simulation"], style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    Text(strings["demo_hint"], style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        device.switches.keys.sorted().forEach { ch ->
                            DemoButton(if (device.isMultiChannel) strings.format("demo_toggle_channel", ch + 1) else strings["demo_toggle"]) {
                                onDemo(MonitorEngine.DEMO_TOGGLE, ch)
                            }
                        }
                        DemoButton(strings[if (device.online) "demo_disconnect" else "demo_reconnect"]) { onDemo(MonitorEngine.DEMO_ONLINE, 0) }
                        // Güç düşüşü yalnızca açık ve güç ölçen kanallarda anlamlı.
                        device.switches.keys.sorted()
                            .filter { device.switches[it] == true && device.channelPowerOf(it) != null }
                            .forEach { ch ->
                                val stalled = device.channelPowerOf(ch) == Demo.STALLED_WATTS
                                val label = when {
                                    device.isMultiChannel && stalled -> strings.format("demo_power_restore", ch + 1)
                                    device.isMultiChannel -> strings.format("demo_power_drop", ch + 1)
                                    stalled -> strings["demo_power_restore_single"]
                                    else -> strings["demo_power_drop_single"]
                                }
                                DemoButton(label) { onDemo(MonitorEngine.DEMO_POWER, ch) }
                            }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text(strings["action_done"]) } },
    )
}

@Composable
private fun DemoButton(text: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
        Text(text, style = MaterialTheme.typography.labelMedium)
    }
}
