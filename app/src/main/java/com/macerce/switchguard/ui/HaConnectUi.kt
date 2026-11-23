package com.macerce.switchguard.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.macerce.switchguard.R
import com.macerce.switchguard.api.HaLan
import com.macerce.switchguard.core.HaProtocol
import com.macerce.switchguard.data.Store
import com.macerce.switchguard.ui.theme.StatusColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Home Assistant'a bağlanma: yerel adres (ör. http://192.168.1.5:8123), isteğe bağlı dış adres (Nabu Casa,
 * Tailscale, Cloudflare Tunnel, DuckDNS) ve uzun ömürlü erişim anahtarı. "Bağlantıyı dene" iki adresi ayrı ayrı
 * sınar; en az biri bağlanırsa kaydedilir ve varlıklar listelenir. Anahtar yalnızca bu telefonda şifreli tutulur.
 */
@Composable
fun HaConnectDialog(prefillLocal: String?, onDismiss: () -> Unit, onConnected: (String) -> Unit) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val store = remember { Store.get(context) }
    val saved = remember { store.haServer }
    val scope = rememberCoroutineScope()
    var local by remember { mutableStateOf(saved?.local ?: prefillLocal.orEmpty()) }
    var remote by remember { mutableStateOf(saved?.remote.orEmpty()) }
    var token by remember { mutableStateOf(saved?.token.orEmpty()) }
    var checking by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<String?>(null) }
    var failed by remember { mutableStateOf(false) }
    var help by remember { mutableStateOf(false) }

    fun describe(code: Int): String = resources.getString(
        when (code) {
            200 -> R.string.ha_test_ok
            401, 403 -> R.string.ha_test_token
            0 -> R.string.ha_test_unreachable
            else -> R.string.ha_test_other
        }
    )

    AlertDialog(
        onDismissRequest = { if (!checking) onDismiss() },
        icon = { Icon(Icons.Rounded.Home, null) },
        title = { Text(stringResource(R.string.ha_connect_title)) },
        text = {
            Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = local, onValueChange = { local = it.trim().take(200); result = null }, singleLine = true,
                    label = { Text(stringResource(R.string.ha_local_url)) }, placeholder = { Text("http://192.168.1.5:8123") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri), modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = remote, onValueChange = { remote = it.trim().take(200); result = null }, singleLine = true,
                    label = { Text(stringResource(R.string.ha_remote_url)) }, placeholder = { Text("https://xxxx.ui.nabu.casa") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri), modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = token, onValueChange = { token = it.trim(); result = null }, singleLine = true,
                    label = { Text(stringResource(R.string.ha_token)) }, visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth(),
                )
                Text(stringResource(R.string.ha_token_hint), style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = { help = !help }, modifier = Modifier.tvFocusRing()) { Text(stringResource(R.string.ha_remote_help_title)) }
                if (help) Text(stringResource(R.string.ha_remote_help_body), style = MaterialTheme.typography.bodySmall)
                if (checking) LinearProgressIndicator(Modifier.fillMaxWidth())
                result?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium,
                        color = if (failed) StatusColors.warning else MaterialTheme.colorScheme.primary)
                }
            }
        },
        dismissButton = { TextButton(enabled = !checking, modifier = Modifier.tvFocusRing(), onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
        confirmButton = {
            TextButton(
                enabled = !checking && token.isNotBlank() && (local.isNotBlank() || remote.isNotBlank()), modifier = Modifier.tvFocusRing(),
                onClick = {
                    checking = true
                    result = null
                    scope.launch {
                        val (lc, rc) = withContext(Dispatchers.IO) {
                            (if (local.isBlank()) -1 else HaLan.test(local, token)) to (if (remote.isBlank()) -1 else HaLan.test(remote, token))
                        }
                        val lines = listOfNotNull(
                            lc.takeIf { it >= 0 }?.let { resources.getString(R.string.ha_test_local, describe(it)) },
                            rc.takeIf { it >= 0 }?.let { resources.getString(R.string.ha_test_remote, describe(it)) },
                        )
                        if (lc == 200 || rc == 200) {
                            val s = HaProtocol.Server(local, remote, token)
                            store.haServer = s
                            HaLan.server = s
                            HaLan.forgetRoute()
                            val n = withContext(Dispatchers.IO) { HaLan.loadEntities() }
                            checking = false
                            onConnected((lines + resources.getString(R.string.ha_entities_found, n.coerceAtLeast(0))).joinToString("\n"))
                        } else {
                            checking = false
                            failed = true
                            result = lines.joinToString("\n")
                        }
                    }
                },
            ) { Text(stringResource(if (checking) R.string.lan_ip_checking else R.string.ha_connect_test)) }
        },
    )
}
