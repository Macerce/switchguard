package com.macerce.switchguard.desktop.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.macerce.switchguard.desktop.ui.str

/** "N saniye sonra çal" seçeneğinin süresi (telefondaki gibi). */
const val TEST_DELAY_SEC = 10

/**
 * Test alarmı (telefondaki `TestAlarmDialog`): sesli ya da sessiz (gece kimseyi uyandırmadan yalnızca alarm penceresi ve
 * bildirim), istenirse [TEST_DELAY_SEC] saniye sonra. Gecikmede başka bir pencereye geçilir ya da ekran kilitlenir ve
 * alarmın her şeyin üstünde açıldığı görülür. "Başlat" [onStart]'ı (sessiz mi, gecikme ms) çağırıp kapanır.
 */
@Composable
fun TestAlarmDialog(onStart: (silent: Boolean, delayMs: Long) -> Unit, onDismiss: () -> Unit) {
    var silent by remember { mutableStateOf(false) }
    var delayed by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(str("test_alarm")) },
        text = {
            Column(Modifier.widthIn(min = 420.dp, max = 520.dp).heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                listOf(
                    false to ("test_alarm_sound" to "desktop_test_alarm_sound_desc"),
                    true to ("test_alarm_silent" to "desktop_test_alarm_silent_desc"),
                ).forEach { (value, keys) ->
                    Row(
                        Modifier.fillMaxWidth()
                            .selectable(selected = silent == value, role = Role.RadioButton, onClick = { silent = value })
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = silent == value, onClick = null)
                        Column(Modifier.padding(start = 12.dp)) {
                            Text(str(keys.first), style = MaterialTheme.typography.bodyLarge)
                            Text(str(keys.second), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Row(
                    Modifier.fillMaxWidth()
                        .toggleable(value = delayed, role = Role.Switch, onValueChange = { delayed = it })
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(str("test_alarm_delay", TEST_DELAY_SEC), style = MaterialTheme.typography.bodyLarge)
                        Text(str("desktop_test_alarm_delay_desc"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = delayed, onCheckedChange = null, modifier = Modifier.padding(start = 12.dp))
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onStart(silent, if (delayed) TEST_DELAY_SEC * 1000L else 0L)
                onDismiss()
            }) { Text(str("test_alarm_start")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(str("action_cancel")) } },
    )
}
