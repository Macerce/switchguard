package com.macerce.switchguard.desktop.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import com.macerce.switchguard.core.DeviceRules
import com.macerce.switchguard.core.EventType
import com.macerce.switchguard.desktop.i18n.Strings
import com.macerce.switchguard.desktop.ui.settings.SwitchRow

/** Kural penceresindeki olaylar ve telefondaki etiketleri (DeviceDetailScreen ile aynı anahtarlar). */
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
 * Bir cihazın kuralları: izlensin mi ve olay başına ALARM / BİLDİRİM / YOK. Her seçim hemen [onChange]'e **değişiklik
 * olarak** gider (çağıran motora gönderir: `MonitorEngine.updateRules`; motor onu o anki kurala uygular, pencere açıldıktan
 * sonra başka yerden yapılan değişiklik ezilmez); pencere seçimi hemen gösterir.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RulesDialog(deviceName: String, initial: DeviceRules, strings: Strings, onChange: ((DeviceRules) -> DeviceRules) -> Unit, onDismiss: () -> Unit) {
    var rules by remember { mutableStateOf(initial) }
    fun set(change: (DeviceRules) -> DeviceRules) {
        rules = change(rules)
        onChange(change)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.format("desktop_rules_title_x", deviceName)) },
        text = {
            Column(Modifier.widthIn(min = 460.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text(strings["action_done"]) } },
    )
}
