package com.macerce.switchguard.desktop.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.macerce.switchguard.core.Entitlement
import com.macerce.switchguard.core.License
import com.macerce.switchguard.data.Store
import com.macerce.switchguard.desktop.ui.str

/**
 * Lisans penceresi: lisans yoksa anahtar girilir (geçerliyse kaydedilir, Pro hemen açılır); varsa sahibi ve tarihi
 * gösterilir, anahtar kaldırılabilir.
 */
@Composable
fun LicenseDialog(store: Store, onDismiss: () -> Unit) {
    val current = store.license
    var key by remember { mutableStateOf("") }
    var invalid by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(str(if (current != null) "license_title_active" else "license_title")) },
        text = {
            Column(Modifier.widthIn(min = 420.dp, max = 520.dp)) {
                if (current != null) {
                    Text(str("license_owner", current.owner, current.issued), style = MaterialTheme.typography.bodyMedium)
                } else {
                    Text(str("license_body", Entitlement.FREE_DEVICES), style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        key, { key = it; invalid = false },
                        label = { Text(str("license_key")) },
                        isError = invalid,
                        supportingText = if (invalid) ({ Text(str("license_invalid")) }) else null,
                        minLines = 2,
                        modifier = Modifier.testTag("license-key"),
                    )
                }
            }
        },
        confirmButton = {
            if (current != null) {
                TextButton(onClick = onDismiss) { Text(str("action_close")) }
            } else {
                TextButton(enabled = key.isNotBlank(), modifier = Modifier.testTag("license-save"), onClick = {
                    val clean = key.filterNot(Char::isWhitespace)
                    if (License.parse(clean) == null) invalid = true else { store.licenseKey = clean; onDismiss() }
                }) { Text(str("license_activate")) }
            }
        },
        dismissButton = {
            if (current != null) {
                TextButton(onClick = { store.licenseKey = null; onDismiss() }) { Text(str("license_remove")) }
            } else {
                TextButton(onClick = onDismiss) { Text(str("action_cancel")) }
            }
        },
    )
}
