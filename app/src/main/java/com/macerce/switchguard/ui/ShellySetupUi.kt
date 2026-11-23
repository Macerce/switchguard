package com.macerce.switchguard.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.macerce.switchguard.R
import com.macerce.switchguard.api.ShellySetup
import com.macerce.switchguard.core.ShellyProtocol
import com.macerce.switchguard.ui.theme.StatusColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Fabrika ayarlı Shelly'yi ev Wi-Fi'sine tanıtma penceresi ([ShellySetup]). Wi-Fi adı telefonun bağlı olduğu ağla
 * dolu gelir (Shelly'nin kendi ağıysa boş). Android 10 altında elle kurulum anlatılır.
 */
@Composable
fun ShellySetupDialog(currentSsid: String?, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    var ssid by remember { mutableStateOf(currentSsid?.takeUnless(ShellyProtocol::isSetupSsid).orEmpty()) }
    var password by remember { mutableStateOf("") }
    var stage by remember { mutableStateOf(0) }
    var message by remember { mutableStateOf<String?>(null) }
    var done by remember { mutableStateOf(false) }
    val busy = stage != 0
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        icon = { Icon(Icons.Rounded.Wifi, null) },
        title = { Text(stringResource(R.string.lan_shelly_setup_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!ShellySetup.supported) {
                    Text(stringResource(R.string.lan_shelly_setup_old_android), style = MaterialTheme.typography.bodyMedium)
                    return@Column
                }
                Text(stringResource(R.string.lan_shelly_setup_body), style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = ssid, onValueChange = { ssid = it.take(32); message = null }, singleLine = true, enabled = !busy && !done,
                    label = { Text(stringResource(R.string.lan_shelly_setup_ssid)) }, modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = password, onValueChange = { password = it.take(64); message = null }, singleLine = true, enabled = !busy && !done,
                    label = { Text(stringResource(R.string.lan_shelly_setup_pass)) }, visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth(),
                )
                if (busy) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text(
                        stringResource(if (stage == 1) R.string.lan_shelly_setup_stage1 else R.string.lan_shelly_setup_stage2),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                message?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = if (done) MaterialTheme.colorScheme.primary else StatusColors.warning)
                }
            }
        },
        dismissButton = if (done || !ShellySetup.supported) null else {
            { TextButton(enabled = !busy, modifier = Modifier.tvFocusRing(), onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
        },
        confirmButton = {
            if (done || !ShellySetup.supported) {
                TextButton(modifier = Modifier.tvFocusRing(), onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
            } else TextButton(
                enabled = !busy && ssid.isNotBlank(), modifier = Modifier.tvFocusRing(),
                onClick = {
                    stage = 1
                    message = null
                    val net = ssid.trim()
                    scope.launch {
                        val r = withContext(Dispatchers.IO) {
                            ShellySetup.provision(context, net, password) { s -> scope.launch { stage = s } }
                        }
                        stage = 0
                        done = r == ShellySetup.Result.OK
                        message = when (r) {
                            ShellySetup.Result.OK -> resources.getString(R.string.lan_shelly_setup_ok, net)
                            ShellySetup.Result.NOT_SHELLY -> resources.getString(R.string.lan_shelly_setup_not_shelly)
                            ShellySetup.Result.REJECTED -> resources.getString(R.string.lan_shelly_setup_rejected)
                            ShellySetup.Result.UNSUPPORTED -> resources.getString(R.string.lan_shelly_setup_old_android)
                            ShellySetup.Result.NO_DEVICE_NETWORK -> resources.getString(R.string.lan_shelly_setup_no_network)
                        }
                    }
                },
            ) { Text(stringResource(R.string.lan_shelly_setup_start)) }
        },
    )
}
