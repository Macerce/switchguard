package com.macerce.switchguard.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.ui.unit.dp
import com.macerce.switchguard.R
import com.macerce.switchguard.api.CallMeBotClient
import com.macerce.switchguard.core.CallMode
import com.macerce.switchguard.core.Escalation
import com.macerce.switchguard.core.Recipient
import com.macerce.switchguard.data.Store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** "Ahmet: Telegram · Ayşe: telefon" ya da "Kapalı". */
@Composable
fun callsSummary(s: Store): String {
    val list = s.telegramRecipients.filter { it.calls }
    if (list.isEmpty()) return stringResource(R.string.blind_alert_off)
    val tg = stringResource(R.string.calls_telegram)
    val phone = stringResource(R.string.calls_phone)
    return list.joinToString(" · ") { "${it.name}: ${if (it.call == CallMode.TELEGRAM) tg else phone}" }
}

/**
 * Kritik alarmda arama: her kişi için yok / Telegram araması (CallMeBot) / telefon araması. Kişi, kendi kademe süresi
 * dolunca mesajla birlikte aranır. Gruplar aranamaz.
 */
@Composable
fun CallsDialog(store: Store, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val people = store.telegramRecipients.filter { it.id > 0 }
    val modes = remember { mutableStateMapOf<Long, CallMode>().apply { people.forEach { put(it.id, it.call) } } }
    val targets = remember { mutableStateMapOf<Long, String>().apply { people.forEach { put(it.id, it.callTo) } } }
    val hasPhone = remember { context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY) }
    var message by remember { mutableStateOf<String?>(null) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) message = resources.getString(R.string.call_err_permission)
    }
    fun valid(id: Long) = (modes[id] ?: CallMode.NONE).let { m -> m == CallMode.NONE || Escalation.isCallTarget(m, targets[id].orEmpty()) }
    fun test(r: Recipient) {
        val mode = modes[r.id] ?: return
        val to = targets[r.id].orEmpty().trim()
        when (mode) {
            CallMode.TELEGRAM -> {
                message = resources.getString(R.string.calls_testing)
                scope.launch {
                    val turkish = resources.configuration.locales[0].language == "tr"
                    message = runCatching { withContext(Dispatchers.IO) { CallMeBotClient.call(to, resources.getString(R.string.call_test_text), turkish) } }
                        .fold({ resources.getString(R.string.calls_test_sent) }, { e -> resources.getString(R.string.call_failed, r.name, e.message.orEmpty()) })
                }
            }
            // Deneme aramasında telefonun kendi arama ekranı açılır (kullanıcı gerekirse hemen kapatır).
            CallMode.PHONE -> runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", to.replace(" ", ""), null))) }
            CallMode.NONE -> Unit
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.calls_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.calls_desc))
                if (people.isEmpty()) Text(stringResource(R.string.calls_no_people), color = MaterialTheme.colorScheme.onSurfaceVariant)
                people.forEach { r ->
                    val mode = modes[r.id] ?: CallMode.NONE
                    Text(r.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOfNotNull(
                            CallMode.NONE to R.string.calls_none,
                            CallMode.TELEGRAM to R.string.calls_telegram,
                            (CallMode.PHONE to R.string.calls_phone).takeIf { hasPhone || mode == CallMode.PHONE },
                        ).forEach { (m, label) ->
                            FilterChip(
                                selected = mode == m,
                                onClick = {
                                    modes[r.id] = m
                                    if (m == CallMode.PHONE &&
                                        context.checkSelfPermission(Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED
                                    ) permission.launch(Manifest.permission.CALL_PHONE)
                                },
                                label = { Text(stringResource(label)) },
                                modifier = Modifier.tvFocusRing(RoundedCornerShape(8.dp)),
                            )
                        }
                    }
                    if (mode != CallMode.NONE) {
                        val ok = valid(r.id)
                        OutlinedTextField(
                            value = targets[r.id].orEmpty(),
                            onValueChange = { targets[r.id] = it.take(40) },
                            label = { Text(stringResource(if (mode == CallMode.TELEGRAM) R.string.calls_tg_target else R.string.calls_phone_target)) },
                            singleLine = true,
                            isError = !ok && targets[r.id].orEmpty().isNotBlank(),
                            keyboardOptions = KeyboardOptions(keyboardType = if (mode == CallMode.PHONE) KeyboardType.Phone else KeyboardType.Text),
                            modifier = Modifier.fillMaxWidth().tvTextField(),
                        )
                        if (mode == CallMode.TELEGRAM) {
                            Text(stringResource(R.string.calls_tg_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(enabled = ok && targets[r.id].orEmpty().isNotBlank(), modifier = Modifier.tvFocusRing(), onClick = { test(r) }) {
                            Text(stringResource(R.string.calls_test))
                        }
                    }
                }
                message?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
            }
        },
        confirmButton = {
            TextButton(
                enabled = people.all { valid(it.id) },
                modifier = Modifier.tvFocusRing(),
                onClick = {
                    store.telegramRecipients = store.telegramRecipients.map { r ->
                        val mode = modes[r.id] ?: return@map r
                        r.copy(call = mode, callTo = if (mode == CallMode.NONE) "" else targets[r.id].orEmpty().trim())
                    }
                    onDismiss()
                },
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(modifier = Modifier.tvFocusRing(), onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
