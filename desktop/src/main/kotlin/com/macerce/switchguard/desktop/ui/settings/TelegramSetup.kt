package com.macerce.switchguard.desktop.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.macerce.switchguard.api.TelegramClient
import com.macerce.switchguard.api.TelegramException
import com.macerce.switchguard.core.Escalation
import com.macerce.switchguard.core.Recipient
import com.macerce.switchguard.data.Store
import com.macerce.switchguard.desktop.i18n.Strings
import com.macerce.switchguard.desktop.platform.Links
import com.macerce.switchguard.desktop.ui.LocalStrings
import com.macerce.switchguard.desktop.ui.SectionCard
import com.macerce.switchguard.desktop.ui.SectionTitle
import com.macerce.switchguard.desktop.ui.str
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Telegram Bot API çağrıları; testlerde sahtesi verilir (gerçek Telegram'a istek gitmesin). Hepsi engelleyicidir. */
interface TelegramApi {
    fun botUsername(token: String): String
    fun findChats(token: String): List<Recipient>
    fun send(token: String, chatId: Long, text: String)
}

/** Gerçek [TelegramClient]. */
object TelegramClientApi : TelegramApi {
    override fun botUsername(token: String) = TelegramClient.botUsername(token)
    override fun findChats(token: String) = TelegramClient.findChats(token)
    override fun send(token: String, chatId: Long, text: String) {
        TelegramClient.send(token, chatId, text)
    }
}

private const val DEFAULT_DELAY = 5

private enum class Section { BOT, PEOPLE, TEST }

/**
 * Ayarlar → İkinci kişiye haber ver (telefondaki `TelegramDialog`; masaüstünde pencere yerine tam sayfa): kullanıcının
 * kendi Telegram botu, haber verilecek kişiler ve bekleme süresi. Adımlar: bot anahtarını gir ve dene → kişiler bota
 * Başlat'a basar → "Kişileri bul" → süre → deneme mesajı → kaydet. Ağ işleri arka planda ([Dispatchers.IO]); sonuç,
 * işi başlatan düğmenin hemen altında görünür. Değişiklikler "Kaydet"e basılınca yazılır, geri oku vazgeçer.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TelegramPage(store: Store, onDone: () -> Unit, api: TelegramApi = TelegramClientApi) {
    val strings = LocalStrings.current
    val scope = rememberCoroutineScope()
    var token by remember { mutableStateOf(store.telegramToken) }
    var showToken by remember { mutableStateOf(false) }
    var bot by remember { mutableStateOf(store.telegramBot.ifEmpty { null }) }
    var found by remember { mutableStateOf(store.telegramRecipients) }
    var selected by remember { mutableStateOf(store.telegramRecipients.map { it.id }.toSet()) }
    var delay by remember { mutableStateOf(store.escalateMin.takeIf { it > 0 } ?: DEFAULT_DELAY) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf(false) }
    var messageAt by remember { mutableStateOf(Section.BOT) }
    val enabledNow = remember { store.escalateMin > 0 }

    fun describe(e: Throwable): String = when {
        e is TelegramException && e.code == 401 -> strings["tg_err_token"]
        e is TelegramException && e.code == 403 -> strings["tg_err_blocked"]
        e is TelegramException && e.code == 400 -> strings["tg_err_chat"]
        e is TelegramException -> e.message.orEmpty()
        else -> strings["desktop_no_internet"]
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

    fun note(at: Section, text: String) {
        messageAt = at
        message = text
        error = false
    }

    val verify = {
        launchTask(Section.BOT) {
            val name = withContext(Dispatchers.IO) { api.botUsername(token) }
            bot = name
            strings.format("tg_bot_ok", name)
        }
    }

    PageIntro(str("desktop_tg_desc"))

    // --- 1. Bot ---
    SectionTitle(str("tg_step_bot"))
    StepCard {
        Text(str("tg_step_bot_desc"), style = MaterialTheme.typography.bodyMedium)
        TextButton(onClick = { if (!Links.open(BOTFATHER_URL)) note(Section.BOT, strings["desktop_link_copied"]) }) {
            Icon(Icons.AutoMirrored.Rounded.OpenInNew, null, Modifier.size(18.dp))
            Text(str("tg_open_botfather"), Modifier.padding(start = 8.dp))
        }
        OutlinedTextField(
            token, { token = it; bot = null },
            label = { Text(str("tg_token")) }, singleLine = true, enabled = !busy,
            visualTransformation = if (showToken) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            isError = token.isNotBlank() && !Escalation.isTokenFormat(token),
            trailingIcon = {
                IconButton(onClick = { showToken = !showToken }) {
                    Icon(if (showToken) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, str("tg_token"))
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = verify, enabled = !busy && Escalation.isTokenFormat(token)) { Text(str("tg_verify")) }
            bot?.let {
                Icon(Icons.Rounded.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 12.dp).size(20.dp))
                Text("@$it", modifier = Modifier.padding(start = 6.dp), fontWeight = FontWeight.SemiBold)
            }
        }
        Status(busy, message, error, here = messageAt == Section.BOT)
    }

    // --- 2. Kişiler ---
    SectionTitle(str("tg_step_people"))
    StepCard {
        Text(
            bot?.let { str("desktop_tg_step_people_desc", "t.me/$it") } ?: str("tg_step_people_wait"),
            style = MaterialTheme.typography.bodyMedium,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            bot?.let { name ->
                // Telefondaki "Bağlantıyı paylaş" yerine: bağlantıyı tarayıcıda aç ya da davet metnini kopyala.
                OutlinedButton(onClick = { if (!Links.open("https://t.me/$name")) note(Section.PEOPLE, strings["desktop_link_copied"]) }) {
                    Icon(Icons.AutoMirrored.Rounded.OpenInNew, null, Modifier.size(18.dp))
                    Text(str("desktop_tg_open_link"), Modifier.padding(start = 8.dp))
                }
                OutlinedButton(onClick = {
                    runCatching { Links.copyToClipboard(strings.format("tg_share_text", "https://t.me/$name")) }
                    note(Section.PEOPLE, strings["desktop_tg_link_copied"])
                }) {
                    Icon(Icons.Rounded.ContentCopy, null, Modifier.size(18.dp))
                    Text(str("desktop_tg_copy_link"), Modifier.padding(start = 8.dp))
                }
            }
            Button(
                enabled = !busy && bot != null,
                onClick = {
                    launchTask(Section.PEOPLE) {
                        val chats = withContext(Dispatchers.IO) { api.findChats(token) }
                        // Önceden seçilmiş ama artık listede görünmeyenler (24 saatten eski) korunur.
                        val merged = (chats + found.filter { old -> chats.none { it.id == old.id } }).distinctBy { it.id }
                        found = merged
                        // Tek kişi bulunduysa seçili gelsin.
                        if (selected.isEmpty() && merged.size == 1) selected = setOf(merged.first().id)
                        if (chats.isEmpty()) strings["tg_none_found"] else strings.plural("tg_found", chats.size, chats.size)
                    }
                },
            ) { Text(str("tg_find")) }
        }
        Status(busy, message, error, here = messageAt == Section.PEOPLE)
        if (found.isNotEmpty()) {
            Column {
                found.forEach { r ->
                    Row(
                        Modifier.fillMaxWidth()
                            .toggleable(value = r.id in selected, role = Role.Checkbox, onValueChange = { on -> selected = if (on) selected + r.id else selected - r.id })
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = r.id in selected, onCheckedChange = null)
                        Text(r.name, modifier = Modifier.padding(start = 8.dp), style = MaterialTheme.typography.bodyLarge)
                        if (r.id < 0) {
                            Text(
                                str("tg_group"), style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    }
                }
            }
        }
    }

    // --- 3. Ne zaman ---
    SectionTitle(str("tg_step_when"))
    StepCard {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Escalation.DELAYS.forEach { m ->
                FilterChip(selected = delay == m, onClick = { delay = m }, label = { Text(str("dur_minutes", m)) })
            }
        }
        Text(str("tg_when_desc", delay), style = MaterialTheme.typography.bodyMedium)
        OutlinedButton(
            enabled = !busy && bot != null && selected.isNotEmpty(),
            onClick = {
                launchTask(Section.TEST) {
                    val text = strings.format("tg_test_message", delay)
                    withContext(Dispatchers.IO) { found.filter { it.id in selected }.forEach { api.send(token, it.id, text) } }
                    strings["tg_test_sent"]
                }
            },
        ) { Text(str("tg_test")) }
        Status(busy, message, error, here = messageAt == Section.TEST)
    }

    // --- Kaydet / kapat ---
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (enabledNow) {
            TextButton(
                enabled = !busy,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                onClick = { store.disableEscalation(); onDone() },
            ) { Text(str("tg_turn_off")) }
        }
        TextButton(enabled = !busy, onClick = onDone) { Text(str("action_cancel")) }
        Button(
            enabled = !busy && bot != null && selected.isNotEmpty(),
            onClick = {
                store.telegramToken = token
                store.telegramBot = bot.orEmpty()
                store.telegramRecipients = found.filter { it.id in selected }
                store.escalateMin = delay
                onDone()
            },
        ) { Text(str("action_save")) }
    }
}

private const val BOTFATHER_URL = "https://t.me/BotFather"

/** Adım kartı: kart içinde aralıklı içerik. */
@Composable
private fun StepCard(content: @Composable () -> Unit) {
    SectionCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { content() }
    }
}

/** Bir bölümün altındaki "deneniyor…" ya da sonuç satırı; [here] değilse hiçbir şey göstermez. */
@Composable
private fun Status(busy: Boolean, message: String?, error: Boolean, here: Boolean) {
    if (!here) return
    if (busy) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            Text(str("desktop_tg_checking"), modifier = Modifier.padding(start = 10.dp), style = MaterialTheme.typography.bodyMedium)
        }
    }
    message?.let {
        Text(
            it, style = MaterialTheme.typography.bodyMedium,
            color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        )
    }
}

/** Ayarlar satırındaki özet: "5 dk kapatılmazsa · 2 kişi" ya da "Kapalı". */
fun escalationSummary(store: Store, strings: Strings): String {
    val n = store.telegramRecipients.size
    return if (!store.escalationReady) strings["blind_alert_off"] else strings.plural("tg_summary", n, store.escalateMin, n)
}
