package com.macerce.switchguard.ui

import com.macerce.switchguard.ui.theme.raisedOnTile
import com.macerce.switchguard.core.DeviceKinds
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TextField
import androidx.compose.material.icons.rounded.TimerOff
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.ElectricBolt
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.border
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import com.macerce.switchguard.ui.theme.tileColor
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.PowerOff
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.background
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoMode
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.foundation.focusGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import kotlinx.coroutines.delay
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.macerce.switchguard.R
import com.macerce.switchguard.core.ActionKind
import com.macerce.switchguard.core.AutoAction
import com.macerce.switchguard.core.Automation
import com.macerce.switchguard.core.AutomationTab
import com.macerce.switchguard.core.DeviceSnapshot
import com.macerce.switchguard.core.Trigger
import com.macerce.switchguard.core.TriggerKind
import com.macerce.switchguard.data.Store
import java.util.UUID

/** Düzenleyicide yeni kayıt (kimliği kaydederken verilir). */
const val NEW_AUTOMATION = "new"

/** Otomasyon sekmesi: yalnızca cihaz açıp kapatan otomasyonlar (alarm/bildirim verenler Uyarılar'da). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutomationsScreen(modifier: Modifier) {
    val context = LocalContext.current
    val store = remember { Store.get(context) }
    val version by store.version.collectAsStateWithLifecycle()
    val automations = remember(version) { AutomationTab.AUTOMATIONS.of(store.automations) }
    val devices = remember(version) { store.snapshots }
    val monitoring = remember(version) { store.monitoringEnabled }
    var editing by rememberSaveable { mutableStateOf<String?>(null) }

    editing?.let { id ->
        BackHandler { editing = null }
        AutomationEditor(
            tab = AutomationTab.AUTOMATIONS,
            initial = automations.firstOrNull { it.id == id },
            devices = devices,
            modifier = modifier,
            onSave = { store.saveAutomation(it); editing = null },
            onDelete = { store.deleteAutomation(id); editing = null },
            onBack = { editing = null },
        )
        return
    }

    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            AppTopBar(stringResource(R.string.tab_automations))
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (!monitoring && automations.isNotEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.automations_need_monitoring),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 4.dp),
                        )
                    }
                }
                if (automations.isEmpty()) {
                    item { EmptyAutomations() }
                }
                items(automations, key = { it.id }) { a ->
                    AutomationCard(a, devices, onClick = { editing = a.id }, onToggle = { store.saveAutomation(a.copy(enabled = it)) })
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = { editing = NEW_AUTOMATION },
            icon = { Icon(Icons.Rounded.Add, null) },
            text = { Text(stringResource(R.string.automation_add)) },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).tvFocusRing(RoundedCornerShape(16.dp)),
        )
    }
}

@Composable
private fun EmptyAutomations() {
    Column(Modifier.fillMaxWidth().padding(vertical = 32.dp, horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Rounded.AutoMode, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.automations_empty), style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.automations_empty_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Otomasyon kartı (Otomasyon sekmesinde ve Uyarılar'daki koşullu uyarılarda): eylemin renkli simgesi, adı ve
 * "Eğer … / O zaman …" satırları (akıllı ev senaryosu dili).
 */
@Composable
fun AutomationCard(
    a: Automation,
    devices: List<DeviceSnapshot>,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val (icon, hue) = when (a.action.kind) {
        ActionKind.ALARM -> Icons.Rounded.Alarm to Hue.RED
        ActionKind.NOTIFY -> Icons.Rounded.NotificationsActive to Hue.BLUE
        ActionKind.TURN_ON -> Icons.Rounded.PowerSettingsNew to Hue.GREEN
        ActionKind.TURN_OFF -> Icons.Rounded.PowerOff to Hue.SLATE
    }
    val (whenText, thenText) = describe(a, devices)
    val shape = RoundedCornerShape(24.dp)
    Row(
        modifier.fillMaxWidth()
            .tvFocusRing(shape)
            .clip(shape)
            .background(tileColor())
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(Modifier.alpha(if (a.enabled) 1f else 0.45f)) { HueIcon(icon, hue, size = 40.dp) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(a.name, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Clause(stringResource(R.string.auto_if), whenText)
            Spacer(Modifier.height(2.dp))
            Clause(stringResource(R.string.auto_then), thenText)
        }
        Spacer(Modifier.width(8.dp))
        Switch(checked = a.enabled, onCheckedChange = onToggle)
    }
}

@Composable
private fun Clause(label: String, text: String) {
    // Etiket metnin başında: uzun koşul tam genişlikte kayar.
    val accent = MaterialTheme.colorScheme.primary
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(color = accent, fontWeight = FontWeight.SemiBold)) { append(label) }
            append("  ")
            append(text)
        },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** ("Rögar · Kanal 1 açılınca", "Lamba: aç · 5 dk sonra") */
@Composable
private fun describe(a: Automation, devices: List<DeviceSnapshot>): Pair<String, String> {
    val t = a.trigger
    val src = devices.firstOrNull { it.id == t.deviceId }
    val srcName = src?.let { channelLabel(it, t.channel, t.kind.usesChannel) } ?: stringResource(R.string.device_missing)
    val trigger = when (t.kind) {
        TriggerKind.STAYED_ON, TriggerKind.STAYED_OFF -> stringResource(t.kind.title()) + " " + pluralStringResource(R.plurals.minutes_n, t.minutes, t.minutes)
        TriggerKind.POWER_ABOVE, TriggerKind.POWER_BELOW -> stringResource(t.kind.title()) + " " + formatWatts(t.watts)
        TriggerKind.CHANNEL_POWER_LOW -> stringResource(t.kind.title()) + " " + formatWatts(t.watts) + " " +
            (if (t.graceMinutes > 0) stringResource(R.string.automation_low_for, t.minutes, t.graceMinutes)
            else stringResource(R.string.automation_low_for_nograce, t.minutes))
        else -> stringResource(t.kind.title())
    }
    val act = a.action
    val action = if (act.kind.usesDevice) {
        val dst = devices.firstOrNull { it.id == act.deviceId }
        val dstName = dst?.let { channelLabel(it, act.channel, true) } ?: stringResource(R.string.device_missing)
        "$dstName: " + stringResource(act.kind.title())
    } else stringResource(act.kind.title())
    val delay = if (a.delayMinutes > 0) " · " + stringResource(R.string.automation_after, a.delayMinutes) else ""
    return "$srcName · $trigger" to "$action$delay"
}

@Composable
private fun channelLabel(d: DeviceSnapshot, channel: Int, withChannel: Boolean): String =
    if (withChannel && d.isMultiChannel) "${d.name} · " + stringResource(R.string.channel_n, channel + 1) else d.name

fun TriggerKind.title(): Int = when (this) {
    TriggerKind.TURNED_ON -> R.string.trigger_turned_on
    TriggerKind.TURNED_OFF -> R.string.trigger_turned_off
    TriggerKind.STAYED_ON -> R.string.trigger_stayed_on
    TriggerKind.STAYED_OFF -> R.string.trigger_stayed_off
    TriggerKind.WENT_OFFLINE -> R.string.trigger_went_offline
    TriggerKind.CAME_ONLINE -> R.string.trigger_came_online
    TriggerKind.POWER_ABOVE -> R.string.trigger_power_above
    TriggerKind.POWER_BELOW -> R.string.trigger_power_below
    TriggerKind.CHANNEL_POWER_LOW -> R.string.trigger_channel_power_low
}

fun ActionKind.title(): Int = when (this) {
    ActionKind.TURN_ON -> R.string.auto_action_turn_on
    ActionKind.TURN_OFF -> R.string.auto_action_turn_off
    ActionKind.ALARM -> R.string.auto_action_alarm
    ActionKind.NOTIFY -> R.string.auto_action_notify
}

// ---------------------------------------------------------------- düzenleyici

/**
 * Otomasyon ve koşullu uyarı düzenleyicisi. [tab] işlem seçeneklerini belirler: Uyarılar'da yalnızca alarm çal /
 * bildirim gönder (hedef cihaz yok), Otomasyon'da yalnızca cihazı aç / kapat. Var olan kayıt kendi sekmesinden açılır.
 */
@Composable
fun AutomationEditor(
    tab: AutomationTab,
    initial: Automation?,
    devices: List<DeviceSnapshot>,
    modifier: Modifier,
    onSave: (Automation) -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
) {
    val alert = tab == AutomationTab.ALERTS
    val first = devices.firstOrNull()
    var name by rememberSaveable { mutableStateOf(initial?.name ?: "") }
    var triggerDevice by rememberSaveable { mutableStateOf(initial?.trigger?.deviceId ?: first?.id.orEmpty()) }
    var triggerKind by rememberSaveable { mutableStateOf(initial?.trigger?.kind ?: TriggerKind.TURNED_ON) }
    var triggerChannel by rememberSaveable { mutableStateOf(initial?.trigger?.channel ?: 0) }
    var minutes by rememberSaveable { mutableStateOf(initial?.trigger?.minutes?.takeIf { it > 0 } ?: 30) }
    var watts by rememberSaveable { mutableStateOf(initial?.trigger?.watts?.takeIf { it > 0 }?.let(::formatNumber) ?: "1000") }
    var lowMinutes by rememberSaveable { mutableStateOf(initial?.trigger?.takeIf { it.kind.usesChannelPower }?.minutes ?: 1) }
    // Kayıtlı 0 ("bekleme yok") korunur; yalnızca yeni ya da başka türden otomasyonda varsayılan 2 dk.
    var grace by rememberSaveable { mutableStateOf(initial?.trigger?.takeIf { it.kind.usesChannelPower }?.graceMinutes ?: 2) }
    // Yeni kayıtta sekmenin ilk işlemi (Uyarılar: alarm çal, Otomasyon: cihazı aç).
    var actionKind by rememberSaveable { mutableStateOf(initial?.action?.kind?.takeIf { it in tab.actions } ?: tab.actions.first()) }
    var actionDevice by rememberSaveable { mutableStateOf(initial?.action?.deviceId?.ifEmpty { null } ?: first?.id.orEmpty()) }
    var actionChannel by rememberSaveable { mutableStateOf(initial?.action?.channel ?: 0) }
    var delay by rememberSaveable { mutableStateOf(initial?.delayMinutes ?: 0) }
    var confirmDelete by remember { mutableStateOf(false) }
    // Kumanda: açan öğe (Ekle, kart) ekrandan kalkınca odak yan menüye düşüyordu; düzenleyicide geri okuna ver.
    // Ad alanına verilmez: programla odaklanınca TV klavyesi kendiliğinden açılıyor.
    val tv = LocalIsTv.current
    val entry = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        if (!tv) return@LaunchedEffect
        for (attempt in 1..20) {
            val ok = runCatching { entry.requestFocus() }.getOrNull()
            if (ok != null && ok != false) break
            delay(50)
        }
    }

    val src = devices.firstOrNull { it.id == triggerDevice }
    val dst = devices.firstOrNull { it.id == actionDevice }
    // Güç tetikleyicileri yalnızca enerji ölçen, kanal gücü tetikleyicisi kanal başına ölçen cihazlarda sunulur.
    val kinds = TriggerKind.entries.filter {
        when {
            it.usesChannelPower -> src?.hasChannelPower == true
            it.usesWatts -> src?.hasEnergy == true
            else -> true
        }
    }
    if (triggerKind !in kinds) triggerKind = TriggerKind.TURNED_ON

    val wattsValue = watts.replace(',', '.').toDoubleOrNull()
    val valid = name.isNotBlank() && src != null &&
        (!triggerKind.usesWatts || (wattsValue != null && wattsValue > 0)) &&
        (!actionKind.usesDevice || dst != null)

    // Kaydedilecek otomasyon; aşağıdaki özet de bundan okunur.
    val draft = Automation(
        id = initial?.id ?: NEW_AUTOMATION,
        name = name.trim(),
        enabled = initial?.enabled ?: true,
        trigger = Trigger(
            triggerKind, triggerDevice,
            channel = if (triggerKind.usesChannel) triggerChannel else 0,
            minutes = when {
                triggerKind.usesMinutes -> minutes
                triggerKind.usesChannelPower -> lowMinutes
                else -> 0
            },
            watts = if (triggerKind.usesWatts) wattsValue ?: 0.0 else 0.0,
            graceMinutes = if (triggerKind.usesChannelPower) grace else 0,
        ),
        action = if (actionKind.usesDevice) AutoAction(actionKind, actionDevice, actionChannel) else AutoAction(actionKind),
        delayMinutes = delay,
    )

    Column(modifier.fillMaxSize()) {
        AppTopBar(
            stringResource(
                when {
                    alert -> if (initial == null) R.string.alert_new else R.string.alert_edit
                    else -> if (initial == null) R.string.automation_new else R.string.automation_edit
                }
            ),
            navigationIcon = { Box(Modifier.focusRequester(entry).focusGroup()) { BackButton(onBack) } },
            small = true,
            actions = {
                if (initial != null) {
                    IconButton(modifier = Modifier.tvFocusRing(), onClick = { confirmDelete = true }) {
                        Icon(Icons.Rounded.Delete, stringResource(R.string.action_delete))
                    }
                }
            },
        )
        if (devices.isEmpty()) {
            Text(stringResource(R.string.no_devices), Modifier.padding(24.dp))
            return
        }
        Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
            // Ad
            TextField(
                value = name, onValueChange = { name = it },
                label = { Text(stringResource(R.string.automation_name)) },
                placeholder = { Text(stringResource(R.string.automation_name_hint)) },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                keyboardOptions = tvKeyboardOptions(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = tileColor(),
                    unfocusedContainerColor = tileColor(),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).tvTextField(),
            )

            // ------------------------------------------------ Eğer
            StepCard(Icons.Rounded.Sensors, Hue.BLUE, stringResource(R.string.auto_if), stringResource(R.string.automation_when)) {
                FieldLabel(stringResource(R.string.automation_device))
                DeviceChooser(devices, triggerDevice) { triggerDevice = it.id; triggerChannel = 0 }
                if (triggerKind.usesChannel && src?.isMultiChannel == true) {
                    FieldLabel(stringResource(R.string.automation_channel))
                    ChannelChooser(src, triggerChannel) { triggerChannel = it }
                }
                FieldLabel(stringResource(R.string.automation_event))
                OptionGrid(kinds, triggerKind, { it.icon() }, { stringResource(it.shortTitle()) }) { triggerKind = it }
                if (triggerKind.usesMinutes) {
                    Stepper(stringResource(R.string.automation_minutes_short), minutes, MINUTE_STEPS.filter { it > 0 }) { minutes = it }
                }
                if (triggerKind.usesWatts) {
                    WattField(watts) { watts = it }
                }
                if (triggerKind.usesChannelPower) {
                    Stepper(stringResource(R.string.automation_low_minutes_short), lowMinutes, LOW_STEPS) { lowMinutes = it }
                    Stepper(stringResource(R.string.automation_grace_short), grace, GRACE_STEPS) { grace = it }
                }
                val now = when {
                    triggerKind.usesChannelPower -> src?.channelPowerOf(triggerChannel)
                    triggerKind.usesWatts -> src?.power
                    else -> null
                }
                if (now != null) {
                    Text(
                        stringResource(R.string.automation_current_power, formatWatts(now)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }

            // ------------------------------------------------ O zaman
            StepCard(Icons.Rounded.Bolt, Hue.ORANGE, stringResource(R.string.auto_then), stringResource(R.string.automation_then)) {
                FieldLabel(stringResource(R.string.automation_action))
                OptionGrid(tab.actions, actionKind, { it.icon() }, { stringResource(it.editorTitle()) }, hue = { it.hue() }) { actionKind = it }
                if (actionKind.usesDevice) {
                    FieldLabel(stringResource(R.string.automation_device))
                    DeviceChooser(devices, actionDevice) { actionDevice = it.id; actionChannel = 0 }
                    if (dst?.isMultiChannel == true) {
                        FieldLabel(stringResource(R.string.automation_channel))
                        ChannelChooser(dst, actionChannel) { actionChannel = it }
                    }
                }
                Stepper(stringResource(R.string.automation_delay_short), delay, DELAY_STEPS, zeroLabel = stringResource(R.string.automation_now)) { delay = it }
            }

            // ------------------------------------------------ Özet
            if (src != null) {
                val (whenText, thenText) = describe(draft, devices)
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f))
                        .padding(16.dp),
                ) {
                    Text(stringResource(R.string.automation_summary), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(6.dp))
                    Clause(stringResource(R.string.auto_if), whenText)
                    Spacer(Modifier.height(2.dp))
                    Clause(stringResource(R.string.auto_then), thenText)
                }
            }
            // Zamanlayıcı önerisi cihaz açıp kapatan otomasyonlar için; koşullu uyarıda yeri yok.
            if (!alert) {
                Text(
                    stringResource(R.string.automation_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )
            }

            Button(
                enabled = valid,
                onClick = { onSave(draft.copy(id = initial?.id ?: UUID.randomUUID().toString())) },
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).height(56.dp).tvFocusRing(RoundedCornerShape(18.dp)),
            ) {
                Icon(Icons.Rounded.Check, null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.action_save), style = MaterialTheme.typography.titleSmall)
            }
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(if (alert) R.string.alert_delete_title else R.string.automation_delete_title),
            body = name,
            confirm = stringResource(R.string.action_delete),
            onConfirm = { confirmDelete = false; onDelete() },
            onDismiss = { confirmDelete = false },
        )
    }
}

/** Dakika adımları: − / + bir sonraki anlamlı değere atlar (30 → 45 → 60 …). */
private val MINUTE_STEPS = listOf(0, 1, 2, 3, 5, 10, 15, 20, 30, 45, 60, 90, 120, 180, 240, 360, 480, 720, 1440)
private val DELAY_STEPS = listOf(0, 1, 2, 3, 5, 10, 15, 20, 30, 45, 60, 90, 120)
private val LOW_STEPS = listOf(0, 1, 2, 3, 5, 10, 15, 20, 30)
private val GRACE_STEPS = listOf(0, 1, 2, 3, 5, 10, 15, 20, 30)

/** Düzenleyicinin bir adımı: renkli simge, büyük başlık (Eğer / O zaman), altında alanlar. */
@Composable
private fun StepCard(icon: ImageVector, hue: Hue, title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(24.dp)).background(tileColor()).padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            HueIcon(icon, hue, size = 40.dp)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        content()
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
    )
}

/** Cihaz seçimi: yatay kaydırılan, simgeli kartlar. */
@Composable
private fun DeviceChooser(devices: List<DeviceSnapshot>, selectedId: String, onPick: (DeviceSnapshot) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        devices.forEach { d ->
            val selected = d.id == selectedId
            val shape = RoundedCornerShape(18.dp)
            val scheme = MaterialTheme.colorScheme
            Column(
                Modifier.width(112.dp)
                    .tvFocusRing(shape)
                    .clip(shape)
                    .background(if (selected) scheme.primaryContainer else raisedOnTile())
                    .then(if (selected) Modifier.border(2.dp, scheme.primary, shape) else Modifier)
                    .selectable(selected = selected, onClick = { onPick(d) }, role = Role.RadioButton)
                    .padding(12.dp),
            ) {
                val kind = remember(d.name, d.isMultiChannel, d.hasEnergy, d.cloud) { DeviceKinds.of(d) }
                IconBadge(
                    kind.icon(),
                    tint = if (selected) scheme.onPrimary else scheme.onSurfaceVariant,
                    background = if (selected) scheme.primary else tileColor(),
                    size = 36,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    d.name,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) scheme.onPrimaryContainer else scheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    minLines = 2,
                )
            }
        }
    }
}

/** Kanal seçimi: numaralı düğmeler. */
@Composable
private fun ChannelChooser(device: DeviceSnapshot, selected: Int, onPick: (Int) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        device.switches.keys.sorted().forEach { ch ->
            val on = ch == selected
            val shape = RoundedCornerShape(14.dp)
            Box(
                Modifier.size(52.dp)
                    .tvFocusRing(shape)
                    .clip(shape)
                    .background(if (on) scheme.primary else raisedOnTile())
                    .selectable(selected = on, onClick = { onPick(ch) }, role = Role.RadioButton),
                contentAlignment = Alignment.Center,
            ) {
                Text("${ch + 1}", style = MaterialTheme.typography.titleMedium, color = if (on) scheme.onPrimary else scheme.onSurface)
            }
        }
    }
}

/** Seçenek kutucukları (iki sütun): simge + kısa ad; seçili olan dolu. */
@Composable
private fun <T> OptionGrid(
    options: List<T>,
    selected: T,
    icon: (T) -> ImageVector,
    label: @Composable (T) -> String,
    hue: ((T) -> Hue)? = null,
    onPick: (T) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { o ->
                    val on = o == selected
                    val shape = RoundedCornerShape(16.dp)
                    Row(
                        Modifier.weight(1f).heightIn(min = 56.dp)
                            .tvFocusRing(shape)
                            .clip(shape)
                            .background(if (on) scheme.primaryContainer else raisedOnTile())
                            .then(if (on) Modifier.border(2.dp, scheme.primary, shape) else Modifier)
                            .selectable(selected = on, onClick = { onPick(o) }, role = Role.RadioButton)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val h = hue?.invoke(o)
                        if (h != null) {
                            HueIcon(icon(o), h, size = 30.dp)
                        } else {
                            Icon(icon(o), null, tint = if (on) scheme.primary else scheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            label(o),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (on) scheme.onPrimaryContainer else scheme.onSurface,
                        )
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/** Sayı seçici: etiket, ortada değer, iki yanda − / + (anlamlı adımlarla). */
@Composable
private fun Stepper(label: String, value: Int, steps: List<Int>, zeroLabel: String? = null, onChange: (Int) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().padding(top = 12.dp).clip(RoundedCornerShape(16.dp)).background(raisedOnTile())
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f).padding(start = 8.dp))
        val lower = steps.lastOrNull { it < value }
        val higher = steps.firstOrNull { it > value }
        IconButton(enabled = lower != null, onClick = { lower?.let(onChange) }, modifier = Modifier.tvFocusRing()) {
            Icon(Icons.Rounded.Remove, stringResource(R.string.action_decrease))
        }
        Text(
            if (value == 0 && zeroLabel != null) zeroLabel else pluralStringResource(R.plurals.minutes_n, value, value),
            style = MaterialTheme.typography.titleMedium,
            color = scheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 72.dp),
        )
        IconButton(enabled = higher != null, onClick = { higher?.let(onChange) }, modifier = Modifier.tvFocusRing()) {
            Icon(Icons.Rounded.Add, stringResource(R.string.action_increase))
        }
    }
}

/** Güç eşiği: sayı alanı, sonunda "W". */
@Composable
private fun WattField(value: String, onChange: (String) -> Unit) {
    TextField(
        value = value,
        onValueChange = { v -> onChange(v.filter { it.isDigit() || it == '.' || it == ',' }) },
        label = { Text(stringResource(R.string.automation_watts)) },
        suffix = { Text("W") },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        keyboardOptions = tvKeyboardOptions(KeyboardOptions(keyboardType = KeyboardType.Decimal)),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = raisedOnTile(),
            unfocusedContainerColor = raisedOnTile(),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp).tvTextField(),
    )
}

private fun TriggerKind.icon(): ImageVector = when (this) {
    TriggerKind.TURNED_ON -> Icons.Rounded.PowerSettingsNew
    TriggerKind.TURNED_OFF -> Icons.Rounded.PowerOff
    TriggerKind.STAYED_ON -> Icons.Rounded.Timer
    TriggerKind.STAYED_OFF -> Icons.Rounded.TimerOff
    TriggerKind.WENT_OFFLINE -> Icons.Rounded.CloudOff
    TriggerKind.CAME_ONLINE -> Icons.Rounded.Cloud
    TriggerKind.POWER_ABOVE -> Icons.AutoMirrored.Rounded.TrendingUp
    TriggerKind.POWER_BELOW -> Icons.AutoMirrored.Rounded.TrendingDown
    TriggerKind.CHANNEL_POWER_LOW -> Icons.Rounded.ElectricBolt
}

/** Kutucuklardaki kısa olay adları ("Açık kalınca"). */
private fun TriggerKind.shortTitle(): Int = when (this) {
    TriggerKind.TURNED_ON -> R.string.evt_turned_on
    TriggerKind.TURNED_OFF -> R.string.evt_turned_off
    TriggerKind.STAYED_ON -> R.string.evt_stayed_on
    TriggerKind.STAYED_OFF -> R.string.evt_stayed_off
    TriggerKind.WENT_OFFLINE -> R.string.evt_went_offline
    TriggerKind.CAME_ONLINE -> R.string.evt_came_online
    TriggerKind.POWER_ABOVE -> R.string.evt_power_above
    TriggerKind.POWER_BELOW -> R.string.evt_power_below
    TriggerKind.CHANNEL_POWER_LOW -> R.string.evt_channel_power_low
}

private fun ActionKind.icon(): ImageVector = when (this) {
    ActionKind.ALARM -> Icons.Rounded.Alarm
    ActionKind.NOTIFY -> Icons.Rounded.NotificationsActive
    ActionKind.TURN_ON -> Icons.Rounded.PowerSettingsNew
    ActionKind.TURN_OFF -> Icons.Rounded.PowerOff
}

private fun ActionKind.hue(): Hue = when (this) {
    ActionKind.ALARM -> Hue.RED
    ActionKind.NOTIFY -> Hue.BLUE
    ActionKind.TURN_ON -> Hue.GREEN
    ActionKind.TURN_OFF -> Hue.SLATE
}

private fun ActionKind.editorTitle(): Int = when (this) {
    ActionKind.ALARM -> R.string.auto_action_alarm
    ActionKind.NOTIFY -> R.string.auto_action_notify
    ActionKind.TURN_ON -> R.string.act_turn_on
    ActionKind.TURN_OFF -> R.string.act_turn_off
}

/** Açılır seçim kutusu. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> Picker(label: String, options: List<T>, selected: T?, text: @Composable (T) -> String, onPick: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected?.let { text(it) } ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { o ->
                DropdownMenuItem(text = { Text(text(o)) }, onClick = { onPick(o); expanded = false })
            }
        }
    }
}
