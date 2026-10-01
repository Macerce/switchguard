package com.macerce.switchguard.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.macerce.switchguard.R
import com.macerce.switchguard.api.TuyaAuthException
import com.macerce.switchguard.api.TuyaClient
import com.macerce.switchguard.api.TuyaExpiredException
import com.macerce.switchguard.data.Store
import kotlinx.coroutines.launch
import java.io.IOException

/** Tuya Cloud projesinin bilgilerini alır, kaydetmeden önce bağlanıp dener. */
@Composable
fun TuyaDialog(store: Store, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    var accessId by remember { mutableStateOf(store.tuyaAccessId) }
    var secret by remember { mutableStateOf(store.tuyaSecret) }
    var region by remember { mutableStateOf(store.tuyaRegion) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(stringResource(R.string.tuya_dialog_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(stringResource(R.string.tuya_dialog_desc), style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    accessId, { accessId = it }, label = { Text("Access ID") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                )
                OutlinedTextField(
                    secret, { secret = it }, label = { Text("Access Secret") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                Text(stringResource(R.string.tuya_region), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 12.dp))
                TuyaClient.REGIONS.forEach { r ->
                    Row(
                        Modifier.fillMaxWidth().selectable(selected = r == region, onClick = { region = r }).padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = r == region, onClick = null)
                        Text(regionLabel(r), modifier = Modifier.padding(start = 8.dp))
                    }
                }
                if (busy) {
                    Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.padding(end = 12.dp))
                        Text(stringResource(R.string.tuya_checking))
                    }
                }
                message?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp))
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !busy && accessId.isNotBlank() && secret.isNotBlank(),
                onClick = {
                    busy = true
                    message = null
                    scope.launch {
                        Actions.connectTuya(context, accessId, secret, region)
                            .onSuccess { count ->
                                android.widget.Toast.makeText(
                                    context, resources.getString(R.string.tuya_connected_ok, count), android.widget.Toast.LENGTH_LONG,
                                ).show()
                                onDismiss()
                            }
                            .onFailure { e ->
                                message = when (e) {
                                    is TuyaAuthException -> resources.getString(R.string.tuya_auth_error)
                                    is TuyaExpiredException -> resources.getString(R.string.tuya_expired)
                                    is IOException -> resources.getString(R.string.state_no_internet)
                                    else -> e.message ?: e.javaClass.simpleName
                                }
                            }
                        busy = false
                    }
                },
            ) { Text(stringResource(R.string.action_connect)) }
        },
        dismissButton = {
            Row {
                if (store.hasTuya) {
                    TextButton(enabled = !busy, onClick = { Actions.disconnectTuya(context); onDismiss() }) {
                        Text(stringResource(R.string.tuya_disconnect))
                    }
                }
                TextButton(enabled = !busy, onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
            }
        },
    )
}

@Composable
fun regionLabel(region: String): String = stringResource(
    when (region) {
        "us" -> R.string.tuya_region_us
        "cn" -> R.string.tuya_region_cn
        "in" -> R.string.tuya_region_in
        else -> R.string.tuya_region_eu
    }
)
