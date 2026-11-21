package com.macerce.switchguard.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.macerce.switchguard.R
import com.macerce.switchguard.api.HookClient
import com.macerce.switchguard.core.AlertHooks
import com.macerce.switchguard.core.Heartbeat
import com.macerce.switchguard.data.Store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Canlılık adresi özeti: "hc-ping.com · 5 dk'da bir" ya da "Kapalı". */
@Composable
fun heartbeatSummary(store: Store): String {
    val url = store.heartbeatUrl
    if (!Heartbeat.isValidUrl(url)) return stringResource(R.string.state_off_title)
    val host = url.substringAfter("://").substringBefore('/')
    return host + " · " + pluralStringResource(R.plurals.minutes_n, store.heartbeatMin, store.heartbeatMin)
}

/** ntfy / webhook özeti: hangileri açık. */
@Composable
fun hooksSummary(store: Store): String {
    val on = listOfNotNull("ntfy".takeIf { Heartbeat.isValidUrl(store.ntfyUrl) }, "webhook".takeIf { Heartbeat.isValidUrl(store.webhookUrl) })
    return if (on.isEmpty()) stringResource(R.string.state_off_title) else on.joinToString(" · ")
}

/** "Dene" düğmesinin sonucu: gönderildi (HTTP kodu) ya da hata. */
@Composable
private fun TryResult(text: String?) {
    if (text != null) Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
}

/** Ayarlar › İzleme › Canlılık adresi: adres, aralık ve deneme. */
@Composable
fun HeartbeatDialog(store: Store, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    val res = LocalResources.current
    var url by remember { mutableStateOf(store.heartbeatUrl) }
    var minutes by remember { mutableStateOf(store.heartbeatMin) }
    var result by remember { mutableStateOf<String?>(null) }
    val valid = url.isBlank() || Heartbeat.isValidUrl(url)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.hb_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(stringResource(R.string.hb_desc), style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    url, { url = it.trim() },
                    label = { Text(stringResource(R.string.hb_url_label)) },
                    singleLine = true,
                    isError = !valid,
                    supportingText = if (!valid) ({ Text(stringResource(R.string.hook_invalid_url)) }) else null,
                    keyboardOptions = tvKeyboardOptions(KeyboardOptions(keyboardType = KeyboardType.Uri)),
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp).tvTextField(),
                )
                FlowRow(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Heartbeat.INTERVALS.forEach { m ->
                        ChoicePill(pluralStringResource(R.plurals.minutes_n, m, m), minutes == m, { minutes = m })
                    }
                }
                Text(stringResource(R.string.hb_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 10.dp))
                TextButton(
                    enabled = Heartbeat.isValidUrl(url),
                    modifier = Modifier.padding(top = 4.dp).tvFocusRing(),
                    onClick = {
                        scope.launch {
                            result = withContext(Dispatchers.IO) {
                                runCatching { HookClient.ping(url) }.fold(
                                    { res.getString(R.string.hook_sent, it) },
                                    { res.getString(R.string.hook_failed, it.message.orEmpty()) },
                                )
                            }
                        }
                    },
                ) { Text(stringResource(R.string.action_try)) }
                TryResult(result)
            }
        },
        confirmButton = {
            TextButton(enabled = valid, modifier = Modifier.tvFocusRing(), onClick = {
                store.heartbeatUrl = url
                store.heartbeatMin = minutes
                onDismiss()
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(modifier = Modifier.tvFocusRing(), onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

/** Uyarılar › ntfy ve webhook: iki adres ve deneme mesajı. */
@Composable
fun HooksDialog(store: Store, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    val res = LocalResources.current
    var ntfy by remember { mutableStateOf(store.ntfyUrl) }
    var hook by remember { mutableStateOf(store.webhookUrl) }
    var result by remember { mutableStateOf<String?>(null) }
    val ntfyOk = ntfy.isBlank() || Heartbeat.isValidUrl(ntfy)
    val hookOk = hook.isBlank() || Heartbeat.isValidUrl(hook)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.hooks_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(stringResource(R.string.hooks_desc), style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    ntfy, { ntfy = it.trim() },
                    label = { Text(stringResource(R.string.hooks_ntfy_label)) },
                    placeholder = { Text("https://ntfy.sh/…") },
                    singleLine = true, isError = !ntfyOk,
                    keyboardOptions = tvKeyboardOptions(KeyboardOptions(keyboardType = KeyboardType.Uri)),
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp).tvTextField(),
                )
                OutlinedTextField(
                    hook, { hook = it.trim() },
                    label = { Text(stringResource(R.string.hooks_webhook_label)) },
                    singleLine = true, isError = !hookOk,
                    supportingText = if (!ntfyOk || !hookOk) ({ Text(stringResource(R.string.hook_invalid_url)) }) else null,
                    keyboardOptions = tvKeyboardOptions(KeyboardOptions(keyboardType = KeyboardType.Uri)),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp).tvTextField(),
                )
                Text(stringResource(R.string.hooks_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 10.dp))
                TextButton(
                    enabled = (Heartbeat.isValidUrl(ntfy) || Heartbeat.isValidUrl(hook)) && ntfyOk && hookOk,
                    modifier = Modifier.padding(top = 4.dp).tvFocusRing(),
                    onClick = {
                        val text = res.getString(R.string.hooks_test_text)
                        val device = android.os.Build.MODEL
                        scope.launch {
                            result = withContext(Dispatchers.IO) {
                                val out = mutableListOf<String>()
                                if (Heartbeat.isValidUrl(ntfy)) out += "ntfy: " + runCatching { HookClient.ntfy(ntfy, text, AlertHooks.Event.TEST, device) }
                                    .fold({ res.getString(R.string.hook_sent, it) }, { res.getString(R.string.hook_failed, it.message.orEmpty()) })
                                if (Heartbeat.isValidUrl(hook)) out += "webhook: " + runCatching {
                                    HookClient.webhook(hook, AlertHooks.webhookJson(AlertHooks.Event.TEST, device, text, System.currentTimeMillis()))
                                }.fold({ res.getString(R.string.hook_sent, it) }, { res.getString(R.string.hook_failed, it.message.orEmpty()) })
                                out.joinToString("\n")
                            }
                        }
                    },
                ) { Text(stringResource(R.string.action_try)) }
                TryResult(result)
            }
        },
        confirmButton = {
            TextButton(enabled = ntfyOk && hookOk, modifier = Modifier.tvFocusRing(), onClick = {
                store.ntfyUrl = ntfy
                store.webhookUrl = hook
                onDismiss()
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(modifier = Modifier.tvFocusRing(), onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
