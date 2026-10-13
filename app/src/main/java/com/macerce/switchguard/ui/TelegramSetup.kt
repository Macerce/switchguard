package com.macerce.switchguard.ui

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.macerce.switchguard.R
import com.macerce.switchguard.api.TelegramClient
import com.macerce.switchguard.api.TelegramException
import com.macerce.switchguard.core.Escalation
import com.macerce.switchguard.core.PhoneEntry
import com.macerce.switchguard.data.Store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * "İkinci kişiye haber ver": kullanıcının kendi Telegram botu, haber verilecek kişiler ve bekleme süresi.
 * Adımlar: bot anahtarını gir ve dene → kişiler bota Başlat'a dokunur → "Kişileri bul" → süre → deneme mesajı → kaydet.
 */
@Composable
fun TelegramDialog(store: Store, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    var token by remember { mutableStateOf(store.telegramToken) }
    var bot by remember { mutableStateOf(store.telegramBot.ifEmpty { null }) }
    var found by remember { mutableStateOf(store.telegramRecipients) }
    var selected by remember { mutableStateOf(store.telegramRecipients.map { it.id }.toSet()) }
    var delay by remember { mutableStateOf(store.escalateMin.takeIf { it > 0 } ?: DEFAULT_DELAY) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf(false) }
    // Sonuç, işi başlatan düğmenin hemen altında görünür (pencere kaydırılabilir; en altta gözden kaçar).
    var messageAt by remember { mutableStateOf(Section.BOT) }

    fun describe(e: Throwable): String = when {
        e is TelegramException && e.code == 401 -> resources.getString(R.string.tg_err_token)
        e is TelegramException && e.code == 403 -> resources.getString(R.string.tg_err_blocked)
        e is TelegramException && e.code == 400 -> resources.getString(R.string.tg_err_chat)
        e is TelegramException -> e.message.orEmpty()
        else -> resources.getString(R.string.state_no_internet)
    }

    /** Ağ işini arka planda yapar; sonucu [at] bölümünün altında gösterir. */
    fun launchTask(at: Section, work: suspend () -> String) {
        busy = true
        message = null
        messageAt = at
        scope.launch {
            runCatching { work() }
                .onSuccess { message = it; error = false }
                .onFailure { message = describe(it); error = true }
            busy = false
        }
    }

    val verify = {
        launchTask(Section.BOT) {
            val name = withContext(Dispatchers.IO) { TelegramClient.botUsername(token) }
            bot = name
            resources.getString(R.string.tg_bot_ok, name)
        }
    }

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(stringResource(R.string.tg_title)) },
        text = {
            // Sabit azami yükseklik: pencere içi kaydırma + odak görünür tutma döngüsünü (titreme) önler.
            Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState())) {
                Text(stringResource(R.string.tg_desc), style = MaterialTheme.typography.bodyMedium)

                Step(stringResource(R.string.tg_step_bot))
                Text(stringResource(R.string.tg_step_bot_desc), style = MaterialTheme.typography.bodySmall)
                if (!LocalIsTv.current) {
                    TextButton(onClick = { Actions.openUrl(context, "https://t.me/BotFather") }, modifier = Modifier.tvFocusRing()) {
                        Text(stringResource(R.string.tg_open_botfather))
                    }
                }
                PhoneEntryButton(
                    stringResource(R.string.tg_title),
                    listOf(PhoneEntry.Field("token", stringResource(R.string.tg_token), secret = true)),
                    onReceived = { v -> v["token"]?.takeIf { it.isNotEmpty() }?.let { token = it; bot = null; verify() } },
                    modifier = Modifier.padding(top = 8.dp),
                )
                OutlinedTextField(
                    token, { token = it; bot = null },
                    label = { Text(stringResource(R.string.tg_token)) }, singleLine = true, keyboardOptions = tvKeyboardOptions(),
                    visualTransformation = PasswordVisualTransformation(),
                    isError = token.isNotBlank() && !Escalation.isTokenFormat(token),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp).tvTextField(),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = verify, enabled = !busy && Escalation.isTokenFormat(token),
                        modifier = Modifier.padding(top = 8.dp).tvFocusRing(),
                    ) { Text(stringResource(R.string.tg_verify)) }
                    bot?.let {
                        Icon(Icons.Rounded.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 12.dp, top = 8.dp).size(20.dp))
                        Text("@$it", modifier = Modifier.padding(start = 6.dp, top = 8.dp), fontWeight = FontWeight.SemiBold)
                    }
                }
                Status(busy, message, error, here = messageAt == Section.BOT)

                Step(stringResource(R.string.tg_step_people))
                Text(
                    if (bot != null) stringResource(R.string.tg_step_people_desc, "t.me/$bot") else stringResource(R.string.tg_step_people_wait),
                    style = MaterialTheme.typography.bodySmall,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                    if (bot != null && !LocalIsTv.current) {
                        OutlinedButton(onClick = {
                            val share = Intent(Intent.ACTION_SEND).setType("text/plain")
                                .putExtra(Intent.EXTRA_TEXT, resources.getString(R.string.tg_share_text, "https://t.me/$bot"))
                            runCatching { context.startActivity(Intent.createChooser(share, null)) }
                        }) { Text(stringResource(R.string.tg_share)) }
                    }
                    OutlinedButton(
                        enabled = !busy && bot != null,
                        modifier = Modifier.tvFocusRing(),
                        onClick = {
                            launchTask(Section.PEOPLE) {
                                val chats = withContext(Dispatchers.IO) { TelegramClient.findChats(token) }
                                // Önceden seçilmiş ama artık listede görünmeyenler (24 saatten eski) korunur.
                                val merged = (chats + found.filter { old -> chats.none { it.id == old.id } }).distinctBy { it.id }
                                found = merged
                                // Tek kişi bulunduysa seçili gelsin.
                                if (selected.isEmpty() && merged.size == 1) selected = setOf(merged.first().id)
                                if (chats.isEmpty()) resources.getString(R.string.tg_none_found)
                                else resources.getQuantityString(R.plurals.tg_found, chats.size, chats.size)
                            }
                        },
                    ) { Text(stringResource(R.string.tg_find)) }
                }
                Status(busy, message, error, here = messageAt == Section.PEOPLE)
                found.forEach { r ->
                    Row(
                        Modifier.fillMaxWidth().tvFocusRing(RoundedCornerShape(12.dp), inside = true)
                            .toggleable(value = r.id in selected, onValueChange = { on -> selected = if (on) selected + r.id else selected - r.id })
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = r.id in selected, onCheckedChange = null)
                        Text(r.name, modifier = Modifier.padding(start = 8.dp))
                        if (r.id < 0) {
                            Text(
                                stringResource(R.string.tg_group), style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    }
                }

                Step(stringResource(R.string.tg_step_when))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Escalation.DELAYS.forEach { m ->
                        FilterChip(
                            selected = delay == m, onClick = { delay = m },
                            label = { Text(stringResource(R.string.dur_minutes, m)) },
                            modifier = Modifier.tvFocusRing(RoundedCornerShape(8.dp)),
                        )
                    }
                }
                Text(stringResource(R.string.tg_when_desc, delay), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))

                OutlinedButton(
                    enabled = !busy && bot != null && selected.isNotEmpty(),
                    modifier = Modifier.padding(top = 12.dp).tvFocusRing(),
                    onClick = {
                        launchTask(Section.TEST) {
                            val text = resources.getString(R.string.tg_test_message, delay)
                            withContext(Dispatchers.IO) { found.filter { it.id in selected }.forEach { TelegramClient.send(token, it.id, text) } }
                            resources.getString(R.string.tg_test_sent)
                        }
                    },
                ) { Text(stringResource(R.string.tg_test)) }
                Status(busy, message, error, here = messageAt == Section.TEST)
            }
        },
        confirmButton = {
            TextButton(
                enabled = !busy && bot != null && selected.isNotEmpty(),
                modifier = Modifier.tvFocusRing(),
                onClick = {
                    store.telegramToken = token
                    store.telegramBot = bot.orEmpty()
                    store.telegramRecipients = found.filter { it.id in selected }
                    store.escalateMin = delay
                    onDismiss()
                },
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            Row {
                if (store.escalateMin > 0) {
                    TextButton(enabled = !busy, modifier = Modifier.tvFocusRing(), onClick = { store.disableEscalation(); onDismiss() }) {
                        Text(stringResource(R.string.tg_turn_off))
                    }
                }
                TextButton(enabled = !busy, modifier = Modifier.tvFocusRing(), onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
            }
        },
    )
}

private enum class Section { BOT, PEOPLE, TEST }

/** Bir bölümün altındaki "deneniyor…" ya da sonuç satırı; [here] değilse hiçbir şey göstermez. */
@Composable
private fun Status(busy: Boolean, message: String?, error: Boolean, here: Boolean) {
    if (!here) return
    if (busy) {
        Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            Text(stringResource(R.string.tuya_checking), modifier = Modifier.padding(start = 10.dp), style = MaterialTheme.typography.bodySmall)
        }
    }
    message?.let {
        Text(
            it, style = MaterialTheme.typography.bodySmall,
            color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun Step(title: String) =
    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))

private const val DEFAULT_DELAY = 5

/** Ayarlar satırındaki özet: "5 dk kapatılmazsa · 2 kişi" ya da "Kapalı". */
@Composable
fun escalationSummary(store: Store): String {
    val n = store.telegramRecipients.size
    return if (!store.escalationReady) stringResource(R.string.blind_alert_off)
    else androidx.compose.ui.res.pluralStringResource(R.plurals.tg_summary, n, store.escalateMin, n)
}
