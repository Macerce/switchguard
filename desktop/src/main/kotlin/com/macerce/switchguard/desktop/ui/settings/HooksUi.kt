package com.macerce.switchguard.desktop.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.macerce.switchguard.api.HookClient
import com.macerce.switchguard.core.AlertHooks
import com.macerce.switchguard.core.Heartbeat
import com.macerce.switchguard.core.TelegramAuth
import com.macerce.switchguard.data.Store
import com.macerce.switchguard.desktop.i18n.Strings
import com.macerce.switchguard.desktop.ui.TextButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/*
 * Telefon 2.35–2.37 karşılıkları (ui/HooksUi.kt, ui/TelegramGroupControl.kt): canlılık adresi (Ayarlar → İzleme),
 * ntfy / webhook (Ayarlar → Alarm), Telegram gruplarında kim aç/kapa yapabilir (Akıllı uyarılar → Telegram).
 */

/** Bilgisayarın adı: ntfy / webhook mesajında gönderen cihaz. */
private fun computerName(): String =
    System.getenv("COMPUTERNAME")?.takeIf { it.isNotBlank() }
        ?: runCatching { java.net.InetAddress.getLocalHost().hostName }.getOrNull()?.takeIf { it.isNotBlank() }
        ?: "PC"

/** Canlılık adresi özeti: "hc-ping.com · 5 dk" ya da "Kapalı". */
fun heartbeatSummary(store: Store, strings: Strings): String {
    val url = store.heartbeatUrl
    if (!Heartbeat.isValidUrl(url)) return strings["state_off_title"]
    val host = url.substringAfter("://").substringBefore('/')
    return host + " · " + strings.plural("minutes_n", store.heartbeatMin, store.heartbeatMin)
}

/** ntfy / webhook özeti: hangileri açık. */
fun hooksSummary(store: Store, strings: Strings): String {
    val on = listOfNotNull("ntfy".takeIf { Heartbeat.isValidUrl(store.ntfyUrl) }, "webhook".takeIf { Heartbeat.isValidUrl(store.webhookUrl) })
    return if (on.isEmpty()) strings["state_off_title"] else on.joinToString(" · ")
}

/** Gruplarda kontrol özeti: kaç kişi seçili (hiçbiri ise "kimse"). */
fun groupControlSummary(store: Store, strings: Strings): String {
    val n = store.telegramRecipients.filter { TelegramAuth.isGroup(it.id) && it.control == true }.sumOf { it.controllers.size }
    return if (n == 0) strings["tg_group_control_nobody"] else strings.format("tg_group_control_count", n)
}

private fun tryText(strings: Strings, r: Result<Int>): String =
    r.fold({ strings.format("hook_sent", it) }, { strings.format("hook_failed", it.message.orEmpty()) })

@Composable
private fun TryResult(text: String?) {
    if (text != null) {
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun UrlField(value: String, label: String, error: Boolean, tag: String, placeholder: String? = null, onChange: (String) -> Unit) {
    OutlinedTextField(
        value, { onChange(it.trim()) },
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        singleLine = true,
        isError = error,
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp).testTag(tag),
    )
}

/** Ayarlar › İzleme › Canlılık adresi: adres, aralık ve deneme. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HeartbeatDialog(store: Store, strings: Strings, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var url by remember { mutableStateOf(store.heartbeatUrl) }
    var minutes by remember { mutableStateOf(store.heartbeatMin) }
    var result by remember { mutableStateOf<String?>(null) }
    val valid = url.isBlank() || Heartbeat.isValidUrl(url)
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.widthIn(max = 560.dp),
        title = { Text(strings["hb_title"]) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(strings["hb_desc"], style = MaterialTheme.typography.bodyMedium)
                UrlField(url, strings["hb_url_label"], !valid, "hb-url") { url = it }
                if (!valid) Text(strings["hook_invalid_url"], style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                FlowRow(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Heartbeat.INTERVALS.forEach { m ->
                        FilterChip(selected = minutes == m, onClick = { minutes = m }, label = { Text(strings.plural("minutes_n", m, m)) })
                    }
                }
                Text(strings["hb_hint"], style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 10.dp))
                TextButton(
                    enabled = Heartbeat.isValidUrl(url),
                    modifier = Modifier.padding(top = 4.dp).testTag("hb-try"),
                    onClick = {
                        val u = url
                        scope.launch { result = tryText(strings, withContext(Dispatchers.IO) { runCatching { HookClient.ping(u) } }) }
                    },
                ) { Text(strings["action_try"]) }
                TryResult(result)
            }
        },
        confirmButton = {
            TextButton(enabled = valid, modifier = Modifier.testTag("hb-save"), onClick = {
                store.heartbeatUrl = url
                store.heartbeatMin = minutes
                onDismiss()
            }) { Text(strings["action_save"]) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings["action_cancel"]) } },
    )
}

/** Ayarlar › Alarm › ntfy ve webhook: iki adres ve deneme mesajı. */
@Composable
fun HooksDialog(store: Store, strings: Strings, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var ntfy by remember { mutableStateOf(store.ntfyUrl) }
    var hook by remember { mutableStateOf(store.webhookUrl) }
    var result by remember { mutableStateOf<String?>(null) }
    val ntfyOk = ntfy.isBlank() || Heartbeat.isValidUrl(ntfy)
    val hookOk = hook.isBlank() || Heartbeat.isValidUrl(hook)
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.widthIn(max = 560.dp),
        title = { Text(strings["hooks_title"]) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(strings["hooks_desc"], style = MaterialTheme.typography.bodyMedium)
                UrlField(ntfy, strings["hooks_ntfy_label"], !ntfyOk, "ntfy-url", placeholder = "https://ntfy.sh/…") { ntfy = it }
                UrlField(hook, strings["hooks_webhook_label"], !hookOk, "webhook-url") { hook = it }
                if (!ntfyOk || !hookOk) Text(strings["hook_invalid_url"], style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                Text(strings["hooks_hint"], style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 10.dp))
                TextButton(
                    enabled = (Heartbeat.isValidUrl(ntfy) || Heartbeat.isValidUrl(hook)) && ntfyOk && hookOk,
                    modifier = Modifier.padding(top = 4.dp).testTag("hooks-try"),
                    onClick = {
                        val text = strings["hooks_test_text"]
                        val device = computerName()
                        val n = ntfy
                        val h = hook
                        scope.launch {
                            result = withContext(Dispatchers.IO) {
                                val out = mutableListOf<String>()
                                if (Heartbeat.isValidUrl(n)) out += "ntfy: " + tryText(strings, runCatching { HookClient.ntfy(n, text, AlertHooks.Event.TEST, device) })
                                if (Heartbeat.isValidUrl(h)) out += "webhook: " + tryText(strings, runCatching {
                                    HookClient.webhook(h, AlertHooks.webhookJson(AlertHooks.Event.TEST, device, text, System.currentTimeMillis()))
                                })
                                out.joinToString("\n")
                            }
                        }
                    },
                ) { Text(strings["action_try"]) }
                TryResult(result)
            }
        },
        confirmButton = {
            TextButton(enabled = ntfyOk && hookOk, modifier = Modifier.testTag("hooks-save"), onClick = {
                store.ntfyUrl = ntfy
                store.webhookUrl = hook
                onDismiss()
            }) { Text(strings["action_save"]) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings["action_cancel"]) } },
    )
}

/**
 * Telegram gruplarında kim aç/kapa yapabilir. Grupta kontrol varsayılan olarak kapalı; açılan grupta yalnızca
 * işaretlenen kişiler (gruba yazmış olanlar listelenir) komut verebilir. Bkz. [TelegramAuth].
 */
@Composable
fun GroupControlDialog(store: Store, strings: Strings, onDismiss: () -> Unit) {
    val groups = remember { store.telegramRecipients.filter { TelegramAuth.isGroup(it.id) } }
    val senders = remember { store.telegramSenders }
    var edited by remember { mutableStateOf(groups.associateBy { it.id }) }
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.widthIn(max = 560.dp),
        title = { Text(strings["tg_group_control_title"]) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                edited.values.forEach { g ->
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(g.name.ifEmpty { g.id.toString() }, style = MaterialTheme.typography.titleSmall)
                            Text(strings["tg_group_control_on"], style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = g.control == true,
                            onCheckedChange = { on -> edited = edited + (g.id to g.copy(control = on)) },
                            modifier = Modifier.testTag("group-control-${g.id}"),
                        )
                    }
                    if (g.control == true) {
                        val people = senders[g.id].orEmpty()
                        // Seçilmiş ama listede olmayanlar (eski yazanlar) da gösterilsin; kimlikle.
                        val extra = g.controllers.filter { id -> people.none { it.first == id } }.map { it to "#$it" }
                        (people + extra).forEach { (id, name) ->
                            val checked = id in g.controllers
                            Row(
                                Modifier.fillMaxWidth().toggleable(checked, role = Role.Checkbox) { on ->
                                    val next = if (on) g.controllers + id else g.controllers - id
                                    edited = edited + (g.id to g.copy(controllers = next))
                                }.padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(checked = checked, onCheckedChange = null)
                                Text(name.ifEmpty { "#$id" }, style = MaterialTheme.typography.bodyLarge, fontWeight = if (checked) FontWeight.Medium else null)
                            }
                        }
                        Text(
                            strings["tg_group_control_hint"],
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        },
        confirmButton = {
            TextButton(modifier = Modifier.testTag("group-control-save"), onClick = {
                store.telegramRecipients = store.telegramRecipients.map { r -> edited[r.id] ?: r }
                onDismiss()
            }) { Text(strings["action_save"]) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings["action_cancel"]) } },
    )
}
