package com.macerce.switchguard.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.macerce.switchguard.R
import com.macerce.switchguard.core.ActiveWindow
import com.macerce.switchguard.core.ChatCommand
import com.macerce.switchguard.core.ChatTemplate
import com.macerce.switchguard.core.DeviceGroup
import com.macerce.switchguard.core.Trigger
import com.macerce.switchguard.core.TriggerKind
import com.macerce.switchguard.data.Store
import com.macerce.switchguard.service.ChatRunner
import com.macerce.switchguard.ui.theme.raisedOnTile
import com.macerce.switchguard.ui.theme.tileColor
import java.util.UUID
import kotlinx.coroutines.launch

/** Sohbet satırı: kullanıcının yazdığı ya da uygulamanın yanıtı. */
private data class ChatLine(val mine: Boolean, val text: String)

/** Ekran kapanıp açılınca konuşma kaybolmasın (uygulama açık kaldıkça). */
private val lines = mutableStateListOf<ChatLine>()

/** Uygulama içi sohbet: yazarak cihazları açıp kapatma, durum/güç/sıcaklık sorma; altta hazır şablonlar. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val store = remember { Store.get(context) }
    val version by store.version.collectAsStateWithLifecycle()
    val templates = remember(version) { ChatRunner.templates(context) }
    val scope = rememberCoroutineScope()
    var input by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<ChatTemplate?>(null) }
    val listState = rememberLazyListState()
    val notUnderstood = stringResource(R.string.chat_not_understood)
    val chatTitle = stringResource(R.string.chat_title)

    fun send(text: String) {
        val t = text.trim()
        if (t.isEmpty() || busy) return
        lines += ChatLine(true, t)
        input = ""
        busy = true
        scope.launch {
            val answer = runCatching { ChatRunner.answer(context, t, chatTitle) }
                .getOrElse { "⚠️ " + it.message.orEmpty() } ?: notUnderstood
            lines += ChatLine(false, answer)
            busy = false
        }
    }
    LaunchedEffect(lines.size) { if (lines.isNotEmpty()) listState.animateScrollToItem(lines.size) }

    Column(Modifier.fillMaxSize().imePadding()) {
        AppTopBar(
            title = stringResource(R.string.chat_title),
            small = true,
            navigationIcon = {
                IconButton(modifier = Modifier.tvFocusRing(), onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_back))
                }
            },
            actions = {
                if (lines.isNotEmpty()) IconButton(modifier = Modifier.tvFocusRing(), onClick = { lines.clear() }) {
                    Icon(Icons.Rounded.DeleteSweep, stringResource(R.string.chat_clear))
                }
            },
        )
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            state = listState,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { Bubble(ChatLine(false, stringResource(R.string.chat_welcome))) }
            items(lines) { Bubble(it) }
        }
        Text(
            stringResource(R.string.chat_templates),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, top = 4.dp),
        )
        LazyRow(
            Modifier.fillMaxWidth().padding(vertical = 6.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(templates, key = { it.id }) { t ->
                val shape = RoundedCornerShape(16.dp)
                Row(
                    Modifier.background(raisedOnTile(), shape).tvFocusRing(shape)
                        .combinedClickable(onClick = { send(t.text) }, onLongClick = { editing = t })
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (t.automatic && t.enabled) {
                        Icon(Icons.Rounded.Schedule, stringResource(R.string.chat_tpl_auto), Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.size(6.dp))
                    }
                    Text(t.name, style = MaterialTheme.typography.labelLarge)
                }
            }
            item {
                val shape = RoundedCornerShape(16.dp)
                Row(
                    Modifier.background(raisedOnTile(), shape).tvFocusRing(shape)
                        .combinedClickable(onClick = { editing = ChatTemplate(UUID.randomUUID().toString(), "", "") })
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Add, null, Modifier.size(16.dp))
                    Spacer(Modifier.size(4.dp))
                    Text(stringResource(R.string.chat_add_template), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text(stringResource(R.string.chat_hint)) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { send(input) }),
            )
            IconButton(modifier = Modifier.tvFocusRing(), onClick = { send(input) }, enabled = input.isNotBlank() && !busy) {
                Icon(Icons.AutoMirrored.Rounded.Send, stringResource(R.string.chat_send), tint = MaterialTheme.colorScheme.primary)
            }
        }
    }

    editing?.let { t ->
        TemplateDialog(
            initial = t,
            isNew = templates.none { it.id == t.id },
            onTry = { send(it) },
            onSave = { saved ->
                val list = ChatRunner.templates(context)
                store.chatTemplates = if (list.any { it.id == saved.id }) list.map { if (it.id == saved.id) saved else it } else list + saved
                editing = null
            },
            onDelete = {
                store.chatTemplates = ChatRunner.templates(context).filterNot { it.id == t.id }
                editing = null
            },
            onDismiss = { editing = null },
        )
    }
}

@Composable
private fun Bubble(line: ChatLine) {
    val scheme = MaterialTheme.colorScheme
    Box(Modifier.fillMaxWidth(), contentAlignment = if (line.mine) Alignment.CenterEnd else Alignment.CenterStart) {
        Text(
            line.text,
            color = if (line.mine) scheme.onPrimary else scheme.onSurface,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.widthIn(max = 320.dp)
                .background(if (line.mine) scheme.primary else tileColor(), RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
        )
    }
}

private enum class When { MANUAL, TIME, EVENT }

/** Şablonla çalışabilen olaylar (cihaz değişimi, güç, sıcaklık, gün doğumu/batımı). */
private val EVENT_KINDS = listOf(
    TriggerKind.TURNED_ON, TriggerKind.TURNED_OFF, TriggerKind.WENT_OFFLINE, TriggerKind.CAME_ONLINE,
    TriggerKind.POWER_ABOVE, TriggerKind.POWER_BELOW, TriggerKind.TEMP_ABOVE, TriggerKind.TEMP_BELOW,
    TriggerKind.SUNRISE, TriggerKind.SUNSET,
)

/** Şablon ekleme/düzenleme: ad, mesaj ve isteğe bağlı kendiliğinden çalışma (saat ya da olay). */
@Composable
private fun TemplateDialog(
    initial: ChatTemplate,
    isNew: Boolean,
    onTry: (String) -> Unit,
    onSave: (ChatTemplate) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val store = remember { Store.get(context) }
    val devices = remember { store.snapshotsWithGroups }
    val timePicker = rememberTimePicker()
    var name by rememberSaveable { mutableStateOf(initial.name) }
    var text by rememberSaveable { mutableStateOf(initial.text) }
    var mode by rememberSaveable {
        mutableStateOf(if (initial.trigger != null) When.EVENT else if (initial.atMinute != null) When.TIME else When.MANUAL)
    }
    var minute by rememberSaveable { mutableStateOf(initial.atMinute ?: (19 * 60)) }
    var days by rememberSaveable { mutableStateOf(initial.days) }
    var kind by rememberSaveable { mutableStateOf(initial.trigger?.kind ?: TriggerKind.TURNED_ON) }
    // Cihaz + kanal "id/kanal" olarak tutulur.
    var target by rememberSaveable { mutableStateOf(initial.trigger?.takeIf { it.kind.usesDevice }?.let { "${it.deviceId}/${it.channel}" }) }
    var value by rememberSaveable { mutableStateOf(initial.trigger?.watts?.takeIf { it != 0.0 }?.let { fmt(it) } ?: "") }
    var enabled by rememberSaveable { mutableStateOf(initial.enabled) }
    val parses = ChatCommand.parse(text) != null

    val targets = remember(kind) {
        devices.filter { d -> !kind.usesWatts || d.hasEnergy }.flatMap { d ->
            if (kind.usesChannel && d.isMultiChannel && !DeviceGroup.isGroup(d.id)) d.switches.keys.sorted().map { "${d.id}/$it" }
            else listOf("${d.id}/${d.switches.keys.minOrNull() ?: 0}")
        }
    }
    val number = value.replace(',', '.').toDoubleOrNull()
    val needsValue = kind.usesWatts || kind.usesTemp
    val valid = name.isNotBlank() && parses && when (mode) {
        When.MANUAL, When.TIME -> true
        When.EVENT -> (!kind.usesDevice || target in targets) && (!needsValue || number != null)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (isNew) R.string.chat_add_template else R.string.chat_edit_template)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.chat_tpl_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    text, { text = it },
                    label = { Text(stringResource(R.string.chat_tpl_message)) },
                    supportingText = {
                        Text(stringResource(if (text.isBlank() || parses) R.string.chat_tpl_message_help else R.string.chat_tpl_invalid))
                    },
                    isError = text.isNotBlank() && !parses,
                    modifier = Modifier.fillMaxWidth(),
                )
                Picker(
                    stringResource(R.string.chat_tpl_when), When.entries, mode,
                    text = {
                        stringResource(when (it) {
                            When.MANUAL -> R.string.chat_when_manual
                            When.TIME -> R.string.chat_when_time
                            When.EVENT -> R.string.chat_when_event
                        })
                    },
                    onPick = { mode = it },
                )
                when (mode) {
                    When.MANUAL -> Unit
                    When.TIME -> {
                        val title = stringResource(R.string.chat_tpl_time)
                        TextButton(onClick = { timePicker.pick(title, minute) { minute = it } }, modifier = Modifier.tvFocusRing()) {
                            Icon(Icons.Rounded.Schedule, null)
                            Spacer(Modifier.size(8.dp))
                            Text("$title: %02d:%02d".format(minute / 60, minute % 60))
                        }
                        DayChips(days) { days = it }
                    }
                    When.EVENT -> {
                        Picker(stringResource(R.string.chat_tpl_event), EVENT_KINDS, kind, text = { stringResource(it.title()) }, onPick = { kind = it })
                        if (kind.usesDevice) {
                            val byId = devices.associateBy { it.id }
                            Picker(
                                stringResource(R.string.chat_tpl_device), targets, target?.takeIf { it in targets },
                                text = { key ->
                                    val d = byId[key.substringBefore('/')]
                                    val ch = key.substringAfter('/').toIntOrNull() ?: 0
                                    if (d == null) key
                                    else if (kind.usesChannel && d.isMultiChannel) "${d.name} · " + stringResource(R.string.channel_n, ch + 1) else d.name
                                },
                                onPick = { target = it },
                            )
                        }
                        if (needsValue) {
                            OutlinedTextField(
                                value, { value = it },
                                label = { Text(stringResource(if (kind.usesTemp) R.string.chat_tpl_degrees else R.string.chat_tpl_watts)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
                if (mode != When.MANUAL) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.chat_tpl_enabled), Modifier.weight(1f))
                        Switch(enabled, { enabled = it }, Modifier.tvFocusRing())
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { onTry(text) }, enabled = parses, modifier = Modifier.tvFocusRing()) {
                        Text(stringResource(R.string.chat_tpl_try))
                    }
                    if (!isNew) TextButton(onClick = onDelete, modifier = Modifier.tvFocusRing()) {
                        Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = valid, modifier = Modifier.tvFocusRing(), onClick = {
                val trigger = if (mode != When.EVENT) null else Trigger(
                    kind,
                    deviceId = if (kind.usesDevice) target!!.substringBefore('/') else "",
                    channel = if (kind.usesDevice) target!!.substringAfter('/').toIntOrNull() ?: 0 else 0,
                    watts = if (needsValue) number ?: 0.0 else 0.0,
                )
                onSave(
                    initial.copy(
                        name = name.trim(), text = text.trim(), enabled = enabled,
                        atMinute = if (mode == When.TIME) minute else null,
                        days = if (mode == When.TIME) days else ActiveWindow.ALL_DAYS,
                        trigger = trigger,
                    )
                )
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.tvFocusRing()) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

private fun fmt(v: Double): String = if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString()
