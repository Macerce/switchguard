package com.macerce.switchguard.desktop.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.macerce.switchguard.desktop.AppGraph
import com.macerce.switchguard.desktop.accounts.TuyaConnect
import com.macerce.switchguard.desktop.accounts.TuyaResult
import com.macerce.switchguard.desktop.i18n.Strings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Tuya veri merkezleri (telefondaki seçenekler). */
internal val TUYA_REGIONS = listOf("eu", "us", "cn", "in")

/**
 * Tuya bağlama penceresi: Access ID, Access Secret, veri merkezi; "Bağlan" arka planda dener ([TuyaConnect]).
 * Başarıda motor yeniden başlar ve bulunan cihaz sayısı gösterilir; hata metni pencerede kalır, alanlar korunur.
 * Altında "Nasıl kurulur?" anlatımı.
 */
@Composable
fun TuyaDialog(graph: AppGraph, strings: Strings, onDismiss: () -> Unit) {
    val store = graph.store
    var id by remember { mutableStateOf(store.tuyaAccessId) }
    var secret by remember { mutableStateOf("") }
    var region by remember { mutableStateOf(store.tuyaRegion) }
    var regionOpen by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf(false) }
    var connected by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(strings["tuya_dialog_title"]) },
        text = {
            Column(
                Modifier.widthIn(min = 480.dp).heightIn(max = 560.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(strings["tuya_dialog_desc"], style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(id, { id = it }, Modifier.fillMaxWidth(), label = { Text("Access ID") }, singleLine = true, enabled = !busy)
                OutlinedTextField(
                    secret, { secret = it }, Modifier.fillMaxWidth(),
                    label = { Text("Access Secret") }, singleLine = true, enabled = !busy,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                )
                Box {
                    OutlinedButton(onClick = { regionOpen = true }, enabled = !busy) {
                        Text("${strings["tuya_region"]}: ${strings["tuya_region_$region"]}")
                    }
                    DropdownMenu(regionOpen, { regionOpen = false }) {
                        for (r in TUYA_REGIONS) DropdownMenuItem(text = { Text(strings["tuya_region_$r"]) }, onClick = {
                            region = r
                            regionOpen = false
                        })
                    }
                }
                if (busy) {
                    Text(strings["tuya_checking"], style = MaterialTheme.typography.bodyMedium)
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
                message?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                }
                SetupSteps(strings["desktop_how_to_setup"], Setup.tuyaSteps(strings), Setup.tuyaActions(strings), inDialog = true)
            }
        },
        confirmButton = {
            if (connected) {
                Button(onClick = onDismiss) { Text(strings["action_done"]) }
            } else {
                Button(enabled = !busy && id.isNotBlank() && secret.isNotBlank(), onClick = {
                    busy = true
                    message = null
                    scope.launch {
                        val result = withContext(Dispatchers.IO) { TuyaConnect(store).connect(id, secret, region) }
                        busy = false
                        error = result !is TuyaResult.Connected
                        message = when (result) {
                            is TuyaResult.Connected -> if (result.devices > 0) {
                                strings.plural("tuya_connected_ok", result.devices, result.devices) + "\n\n" + strings["tuya_shared_hint"]
                            } else {
                                strings["tuya_connected_none"]
                            }
                            TuyaResult.AuthError -> strings["tuya_auth_error"]
                            TuyaResult.Expired -> strings["tuya_expired"]
                            TuyaResult.NoInternet -> strings["desktop_no_internet"]
                            is TuyaResult.Failed -> strings.format("desktop_error_x", result.message)
                        }
                        if (result is TuyaResult.Connected) {
                            connected = true
                            graph.engine.restart()
                        }
                    }
                }) { Text(strings["desktop_connect"]) }
            }
        },
        dismissButton = { if (!connected) TextButton(onClick = onDismiss, enabled = !busy) { Text(strings["action_cancel"]) } },
    )
}
