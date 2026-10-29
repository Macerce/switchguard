package com.macerce.switchguard.desktop.ui.chat

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.onClick
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.macerce.switchguard.core.ActiveWindow
import com.macerce.switchguard.core.ChatCommand
import com.macerce.switchguard.core.ChatTemplate
import com.macerce.switchguard.core.DeviceGroup
import com.macerce.switchguard.core.Trigger
import com.macerce.switchguard.core.TriggerKind
import com.macerce.switchguard.desktop.AppGraph
import com.macerce.switchguard.desktop.engine.ChatRunner
import com.macerce.switchguard.desktop.i18n.Strings
import com.macerce.switchguard.desktop.ui.Button
import com.macerce.switchguard.desktop.ui.FilledTonalButton
import com.macerce.switchguard.desktop.ui.PageHeader
import com.macerce.switchguard.desktop.ui.TextButton
import com.macerce.switchguard.desktop.ui.automations.DayChips
import com.macerce.switchguard.desktop.ui.automations.LocationSetup
import com.macerce.switchguard.desktop.ui.automations.shortTitleKey
import com.macerce.switchguard.desktop.ui.raisedOnTile
import com.macerce.switchguard.desktop.ui.readableWidth
import com.macerce.switchguard.desktop.ui.settings.TimeDialog
import com.macerce.switchguard.desktop.ui.settings.formatMinute
import com.macerce.switchguard.desktop.ui.tileColor
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID

/** Yanıtın altında gösterilen düğme. */
private enum class ChatAction { SET_LOCATION }

/** Sohbet satırı: kullanıcının yazdığı ya da uygulamanın yanıtı. */
private data class ChatLine(
    val mine: Boolean,
    val text: String,
    val at: Long = System.currentTimeMillis(),
    val action: ChatAction? = null,
    /** [action] tamamlanınca yeniden gönderilecek komut (ör. konum seçilince "Kaç derece?"). */
    val retry: String? = null,
)

/** Mesaj cümle gibi görünsün: "kaç derece" → "Kaç derece" (Türkçede i → İ). */
private fun sentence(text: String, locale: Locale): String = text.replaceFirstChar { it.titlecase(locale) }

/** Sekme değişip dönülünce konuşma kaybolmasın (uygulama açık kaldıkça). */
private val lines = mutableStateListOf<ChatLine>()

/**
 * Uygulama içi sohbet (telefondaki `ui/ChatScreen.kt`): yazarak cihazları açıp kapatma, durum/güç/sıcaklık sorma;
 * altta hazır şablonlar. Masaüstü farkları: kenar çubuğunda ayrı sekme (telefonda Cihazlar'ın üst çubuğundan açılır),
 * şablonlar satıra sığmayınca alta iner, şablon sağ tıkla düzenlenir (telefonda basılı tutarak), Enter gönderir,
 * yanıtlar seçilip kopyalanabilir.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun ChatScreen(graph: AppGraph, strings: Strings, modifier: Modifier = Modifier) {
    val store = graph.store
    val version by store.version.collectAsState()
    val templates = remember(version, strings) { ChatRunner.templates(store, strings) }
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<ChatTemplate?>(null) }
    var locationFor by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    val inputFocus = remember { FocusRequester() }

    fun send(text: String) {
        val t = text.trim()
        if (t.isEmpty() || busy) return
        lines += ChatLine(true, sentence(t, strings.locale))
        input = ""
        busy = true
        val intent = ChatCommand.parse(t)
        graph.engine.chat(t, strings["chat_title"]) { answer ->
            scope.launch {
                // Konum yoksa sıcaklık yanıtının altında doğrudan konum seçme düğmesi.
                val needsLocation = intent == ChatCommand.Intent.Temperature && (store.latitude == null || store.longitude == null)
                lines += ChatLine(
                    false, answer ?: strings["chat_not_understood"],
                    action = if (needsLocation) ChatAction.SET_LOCATION else null, retry = if (needsLocation) t else null,
                )
                busy = false
            }
        }
    }
    // Sona kaydır: hoş geldin + satırlar + (varsa) "yanıt hazırlanıyor".
    LaunchedEffect(lines.size, busy) { listState.animateScrollToItem(lines.size + if (busy) 1 else 0) }
    LaunchedEffect(Unit) { runCatching { inputFocus.requestFocus() } }

    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.readableWidth(860.dp).fillMaxSize()) {
            PageHeader(strings["chat_title"], actions = {
                if (lines.isNotEmpty()) IconButton(onClick = { lines.clear() }, modifier = Modifier.testTag("chat-clear")) {
                    Icon(Icons.Rounded.DeleteSweep, strings["chat_clear"])
                }
            })
            LazyColumn(
                Modifier.weight(1f).fillMaxWidth().testTag("chat-lines"),
                state = listState,
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item { Bubble(remember(strings) { ChatLine(false, strings["desktop_chat_welcome"], at = 0) }, strings) }
                items(lines) { line -> Bubble(line, strings, onAction = { locationFor = line.retry ?: "" }) }
                if (busy) item { Typing(strings) }
            }
            Text(
                strings["chat_templates"],
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 24.dp, top = 6.dp),
            )
            FlowRow(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                templates.forEach { t ->
                    val shape = RoundedCornerShape(16.dp)
                    Row(
                        Modifier.background(raisedOnTile(), shape)
                            .onClick(matcher = PointerMatcher.mouse(PointerButton.Secondary)) { editing = t }
                            .clickable { send(t.text) }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                            .testTag("chat-tpl-${t.id}"),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (t.automatic && t.enabled) {
                            Icon(Icons.Rounded.Schedule, strings["chat_tpl_auto"], Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(t.name, style = MaterialTheme.typography.labelLarge)
                    }
                }
                val shape = RoundedCornerShape(16.dp)
                Row(
                    Modifier.background(raisedOnTile(), shape)
                        .clickable { editing = ChatTemplate(UUID.randomUUID().toString(), "", "") }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                        .testTag("chat-tpl-add"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Add, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(strings["chat_add_template"], style = MaterialTheme.typography.labelLarge)
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(start = 24.dp, end = 16.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = { Text(strings["chat_hint"]) },
                    modifier = Modifier.weight(1f).focusRequester(inputFocus).testTag("chat-input").onPreviewKeyEvent {
                        if (it.type == KeyEventType.KeyDown && (it.key == Key.Enter || it.key == Key.NumPadEnter) && !it.isShiftPressed) {
                            send(input)
                            true
                        } else {
                            false
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                )
                IconButton(onClick = { send(input) }, enabled = input.isNotBlank() && !busy, modifier = Modifier.testTag("chat-send")) {
                    Icon(Icons.AutoMirrored.Rounded.Send, strings["chat_send"], tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }

    locationFor?.let { retry ->
        AlertDialog(
            onDismissRequest = { locationFor = null },
            title = { Text(strings["location_title"]) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(strings["location_desc"], style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LocationSetup(store, strings, onChanged = {
                        locationFor = null
                        // Konum seçildi: soru kendiliğinden yeniden sorulur.
                        if (retry.isNotBlank()) send(retry)
                    })
                }
            },
            confirmButton = { TextButton(onClick = { locationFor = null }) { Text(strings["action_cancel"]) } },
        )
    }

    editing?.let { t ->
        TemplateDialog(
            graph, strings,
            initial = t,
            isNew = templates.none { it.id == t.id },
            onTry = { send(it) },
            onSave = { saved ->
                val list = ChatRunner.templates(store, strings)
                graph.engine.saveChatTemplates(if (list.any { it.id == saved.id }) list.map { if (it.id == saved.id) saved else it } else list + saved)
                editing = null
            },
            onDelete = {
                graph.engine.saveChatTemplates(ChatRunner.templates(store, strings).filterNot { it.id == t.id })
                editing = null
            },
            onDismiss = { editing = null },
        )
    }
}

/** Mesaj balonu: üstte gönderen ve saat, gönderen tarafın köşesi sivri; yanıtta varsa düğme. */
@Composable
private fun Bubble(line: ChatLine, strings: Strings, onAction: () -> Unit = {}) {
    val scheme = MaterialTheme.colorScheme
    val shape = if (line.mine) RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp) else RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp)
    Column(Modifier.fillMaxWidth(), horizontalAlignment = if (line.mine) Alignment.End else Alignment.Start) {
        Text(
            strings[if (line.mine) "chat_you" else "app_name"] + if (line.at > 0) " · " + strings.time(line.at) else "",
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
        Column(
            Modifier.widthIn(max = 520.dp)
                .background(if (line.mine) scheme.primary else tileColor(), shape)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            SelectionContainer {
                Text(line.text, color = if (line.mine) scheme.onPrimary else scheme.onSurface, style = MaterialTheme.typography.bodyMedium)
            }
            if (line.action == ChatAction.SET_LOCATION) {
                FilledTonalButton(onClick = onAction, modifier = Modifier.padding(top = 8.dp).testTag("chat-set-location")) {
                    Icon(Icons.Rounded.Place, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(strings["chat_set_location"])
                }
            }
        }
    }
}

/** Yanıt beklenirken: "Yanıt hazırlanıyor…". */
@Composable
private fun Typing(strings: Strings) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
        Text(
            strings["chat_typing"],
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.background(tileColor(), RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp),
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
    graph: AppGraph,
    strings: Strings,
    initial: ChatTemplate,
    isNew: Boolean,
    onTry: (String) -> Unit,
    onSave: (ChatTemplate) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val devices = remember { graph.store.snapshotsWithGroups }
    var name by remember { mutableStateOf(initial.name) }
    var text by remember { mutableStateOf(initial.text) }
    var mode by remember {
        mutableStateOf(if (initial.trigger != null) When.EVENT else if (initial.atMinute != null) When.TIME else When.MANUAL)
    }
    var minute by remember { mutableStateOf(initial.atMinute ?: (19 * 60)) }
    var pickTime by remember { mutableStateOf(false) }
    var days by remember { mutableStateOf(initial.days) }
    var kind by remember { mutableStateOf(initial.trigger?.kind?.takeIf { it in EVENT_KINDS } ?: TriggerKind.TURNED_ON) }
    // Cihaz + kanal "id/kanal" olarak tutulur.
    var target by remember { mutableStateOf(initial.trigger?.takeIf { it.kind.usesDevice }?.let { "${it.deviceId}/${it.channel}" }) }
    var value by remember { mutableStateOf(initial.trigger?.watts?.takeIf { it != 0.0 }?.let { fmt(it) } ?: "") }
    var enabled by remember { mutableStateOf(initial.enabled) }
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
        title = { Text(strings[if (isNew) "chat_add_template" else "chat_edit_template"]) },
        text = {
            Column(Modifier.widthIn(min = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text(strings["chat_tpl_name"]) }, singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("tpl-name"))
                OutlinedTextField(
                    text, { text = it },
                    label = { Text(strings["chat_tpl_message"]) },
                    supportingText = { Text(strings[if (text.isBlank() || parses) "chat_tpl_message_help" else "chat_tpl_invalid"]) },
                    isError = text.isNotBlank() && !parses,
                    modifier = Modifier.fillMaxWidth().testTag("tpl-text"),
                )
                Choice(strings["chat_tpl_when"], When.entries, mode, "tpl-when", label = {
                    strings[when (it) {
                        When.MANUAL -> "chat_when_manual"
                        When.TIME -> "chat_when_time"
                        When.EVENT -> "chat_when_event"
                    }]
                }) { mode = it }
                when (mode) {
                    When.MANUAL -> Unit
                    When.TIME -> {
                        TextButton(onClick = { pickTime = true }, modifier = Modifier.testTag("tpl-time")) {
                            Icon(Icons.Rounded.Schedule, null)
                            Spacer(Modifier.width(8.dp))
                            Text(strings["chat_tpl_time"] + ": " + formatMinute(strings, minute))
                        }
                        DayChips(days, strings) { days = it }
                    }
                    When.EVENT -> {
                        Choice(strings["chat_tpl_event"], EVENT_KINDS, kind, "tpl-event", label = { strings[it.shortTitleKey()] }) { kind = it }
                        if (kind.usesDevice) {
                            val byId = devices.associateBy { it.id }
                            Choice(strings["chat_tpl_device"], targets, target?.takeIf { it in targets }, "tpl-device", label = { key ->
                                val d = byId[key.substringBefore('/')]
                                val ch = key.substringAfter('/').toIntOrNull() ?: 0
                                if (d == null) key
                                else if (kind.usesChannel && d.isMultiChannel) "${d.name} · " + strings.format("channel_n", ch + 1) else d.name
                            }) { target = it }
                        }
                        if (needsValue) {
                            OutlinedTextField(
                                value, { value = it },
                                label = { Text(strings[if (kind.usesTemp) "chat_tpl_degrees" else "chat_tpl_watts"]) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth().testTag("tpl-value"),
                            )
                        }
                    }
                }
                if (mode != When.MANUAL) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(strings["chat_tpl_enabled"], Modifier.weight(1f))
                        Switch(enabled, { enabled = it })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { onTry(text) }, enabled = parses) { Text(strings["chat_tpl_try"]) }
                    if (!isNew) TextButton(onClick = onDelete, modifier = Modifier.testTag("tpl-delete")) {
                        Text(strings["action_delete"], color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            Button(enabled = valid, modifier = Modifier.testTag("tpl-save"), onClick = {
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
            }) { Text(strings["action_save"]) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings["action_cancel"]) } },
    )
    if (pickTime) {
        TimeDialog(strings["chat_tpl_time"], minute, strings.locale, onPick = { minute = it; pickTime = false }, onDismiss = { pickTime = false })
    }
}

/** Başlıklı açılır seçim: düğmede seçili değer, tıklayınca liste. */
@Composable
private fun <T> Choice(title: String, options: List<T>, selected: T?, tag: String, label: @Composable (T) -> String, onPick: (T) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box {
            FilledTonalButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth().testTag(tag)) {
                Text(selected?.let { label(it) } ?: "—", Modifier.weight(1f))
                Icon(Icons.Rounded.ArrowDropDown, null)
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                options.forEach { o ->
                    DropdownMenuItem(text = { Text(label(o)) }, onClick = { open = false; onPick(o) })
                }
            }
        }
    }
}

private fun fmt(v: Double): String = if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString()
