package com.macerce.switchguard.ui

import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import com.macerce.switchguard.core.PresenceRule
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.runtime.key
import androidx.compose.ui.res.stringArrayResource
import com.macerce.switchguard.core.ActiveWindow
import com.macerce.switchguard.core.Condition
import com.macerce.switchguard.core.ConditionKind
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
    var editingScene by rememberSaveable { mutableStateOf<String?>(null) }
    val scenes = remember(version) { store.scenes }

    editingScene?.let { id ->
        BackHandler { editingScene = null }
        Box(modifier) {
            SceneEditor(
                initial = scenes.firstOrNull { it.id == id },
                devices = remember(version) { store.deviceLayout.sections(store.snapshots).flatMap { it.devices } },
                onSave = { store.saveScene(it); editingScene = null },
                onDelete = { store.deleteScene(id); editingScene = null },
                onBack = { editingScene = null },
            )
        }
        return
    }

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
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.scenes_title), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp))
                        ScenesStrip(scenes) { editingScene = it }
                        Text(stringResource(R.string.automations_rules_title), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 4.dp, top = 8.dp))
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
        ActionKind.RUN_SCENE -> Icons.Rounded.PlayArrow to Hue.PURPLE
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
    val srcName = when {
        !t.kind.usesDevice -> null
        else -> src?.let { channelLabel(it, t.channel, t.kind.usesChannel) } ?: stringResource(R.string.device_missing)
    }
    val trigger = when (t.kind) {
        TriggerKind.SUNRISE, TriggerKind.SUNSET -> stringResource(t.kind.title()) + offsetSuffix(t.minutes)
        TriggerKind.TEMP_ABOVE, TriggerKind.TEMP_BELOW -> stringResource(t.kind.title()) + " " + formatTemp(t.watts)
        TriggerKind.STAYED_ON, TriggerKind.STAYED_OFF -> stringResource(t.kind.title()) + " " + pluralStringResource(R.plurals.minutes_n, t.minutes, t.minutes)
        TriggerKind.POWER_ABOVE, TriggerKind.POWER_BELOW -> stringResource(t.kind.title()) + " " + formatWatts(t.watts) +
            (if (t.minutes > 0) " " + stringResource(R.string.automation_power_for, holdLabel(t.minutes)) else "")
        TriggerKind.CHANNEL_POWER_LOW -> stringResource(t.kind.title()) + " " + formatWatts(t.watts) + " " +
            (if (t.graceMinutes > 0) stringResource(R.string.automation_low_for, t.minutes, t.graceMinutes)
            else stringResource(R.string.automation_low_for_nograce, t.minutes))
        else -> stringResource(t.kind.title())
    }
    val act = a.action
    val action = when {
        act.kind.usesDevice -> {
            val dst = devices.firstOrNull { it.id == act.deviceId }
            val dstName = dst?.let { channelLabel(it, act.channel, true) } ?: stringResource(R.string.device_missing)
            "$dstName: " + stringResource(act.kind.title())
        }
        act.kind == ActionKind.RUN_SCENE -> {
            val context = LocalContext.current
            val scene = remember(act.deviceId) { Store.get(context).scenes.firstOrNull { it.id == act.deviceId } }
            stringResource(R.string.auto_action_scene, scene?.name ?: stringResource(R.string.device_missing))
        }
        else -> stringResource(act.kind.title())
    }
    val delay = if (a.delayMinutes > 0) " · " + stringResource(R.string.automation_after, a.delayMinutes) else ""
    val extras = buildList {
        a.window?.let { add(windowText(it)) }
        a.condition?.let { c ->
            val d = devices.firstOrNull { it.id == c.deviceId }
            val name = d?.let { channelLabel(it, c.channel, c.kind == ConditionKind.IS_ON || c.kind == ConditionKind.IS_OFF) }
                ?: stringResource(R.string.device_missing)
            add(stringResource(R.string.cond_clause, name, stringResource(c.kind.title())))
        }
        a.presence?.let { add(stringResource(if (it == PresenceRule.AWAY) R.string.presence_only_away else R.string.presence_only_home)) }
    }.joinToString("") { " · $it" }
    return (if (srcName != null) "$srcName · $trigger" else trigger) + extras to "$action$delay"
}

/** " · 15 dk önce", "" (tam vaktinde). */
@Composable
private fun offsetSuffix(minutes: Int): String = when {
    minutes == 0 -> ""
    else -> " · " + offsetLabel(minutes)
}

@Composable
private fun offsetLabel(minutes: Int): String = when {
    minutes == 0 -> stringResource(R.string.sun_exact)
    minutes < 0 -> stringResource(R.string.sun_before, holdLabel(-minutes))
    else -> stringResource(R.string.sun_after, holdLabel(minutes))
}

private fun formatTemp(v: Double): String = formatNumber(v) + " °C"

/** "22:00–06:00, hafta içi". */
@Composable
private fun windowText(w: ActiveWindow): String {
    val context = LocalContext.current
    val time = if (w.startMinute == w.endMinute) stringResource(R.string.window_all_day)
        else stringResource(R.string.time_range, formatMinute(context, w.startMinute), formatMinute(context, w.endMinute))
    val days = when (w.days) {
        ActiveWindow.ALL_DAYS -> null
        0b0011111 -> stringResource(R.string.days_weekdays)
        0b1100000 -> stringResource(R.string.days_weekend)
        else -> stringArrayResource(R.array.day_short).filterIndexed { i, _ -> w.days and (1 shl i) != 0 }.joinToString(" ")
    }
    return if (days != null) "$time, $days" else time
}

fun ConditionKind.title(): Int = when (this) {
    ConditionKind.IS_ON -> R.string.cond_is_on
    ConditionKind.IS_OFF -> R.string.cond_is_off
    ConditionKind.IS_ONLINE -> R.string.cond_is_online
    ConditionKind.IS_OFFLINE -> R.string.cond_is_offline
}

/** "45 dk", "6 sa", "1 sa 30 dk". */
@Composable
private fun holdLabel(minutes: Int): String = when {
    minutes < 60 -> pluralStringResource(R.plurals.minutes_n, minutes, minutes)
    minutes % 60 == 0 -> pluralStringResource(R.plurals.hours_n, minutes / 60, minutes / 60)
    else -> stringResource(R.string.dur_hour_min, minutes / 60, minutes % 60)
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
    TriggerKind.SUNRISE -> R.string.trigger_sunrise
    TriggerKind.SUNSET -> R.string.trigger_sunset
    TriggerKind.TEMP_ABOVE -> R.string.trigger_temp_above
    TriggerKind.TEMP_BELOW -> R.string.trigger_temp_below
}

fun ActionKind.title(): Int = when (this) {
    ActionKind.TURN_ON -> R.string.auto_action_turn_on
    ActionKind.TURN_OFF -> R.string.auto_action_turn_off
    ActionKind.ALARM -> R.string.auto_action_alarm
    ActionKind.NOTIFY -> R.string.auto_action_notify
    ActionKind.RUN_SCENE -> R.string.act_run_scene
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
    var offset by rememberSaveable { mutableStateOf(initial?.trigger?.takeIf { it.kind.isSun }?.minutes ?: 0) }
    var temp by rememberSaveable { mutableStateOf(initial?.trigger?.takeIf { it.kind.usesTemp }?.watts?.let(::formatNumber) ?: "5") }
    var windowOn by rememberSaveable { mutableStateOf(initial?.window != null) }
    var windowStart by rememberSaveable { mutableStateOf(initial?.window?.startMinute ?: 22 * 60) }
    var windowEnd by rememberSaveable { mutableStateOf(initial?.window?.endMinute ?: 6 * 60) }
    var windowDays by rememberSaveable { mutableStateOf(initial?.window?.days ?: ActiveWindow.ALL_DAYS) }
    var presenceRule by rememberSaveable { mutableStateOf(initial?.presence) }
    var sceneId by rememberSaveable { mutableStateOf(initial?.action?.takeIf { it.kind == ActionKind.RUN_SCENE }?.deviceId ?: "") }
    val sceneList = Store.get(LocalContext.current).scenes.let { remember(it) { it } }
    var condOn by rememberSaveable { mutableStateOf(initial?.condition != null) }
    var condKind by rememberSaveable { mutableStateOf(initial?.condition?.kind ?: ConditionKind.IS_ON) }
    var condDevice by rememberSaveable { mutableStateOf(initial?.condition?.deviceId ?: first?.id.orEmpty()) }
    var condChannel by rememberSaveable { mutableStateOf(initial?.condition?.channel ?: 0) }
    var locationVersion by remember { mutableStateOf(0) }
    val timePicker = rememberTimePicker()
    var holdMinutes by rememberSaveable { mutableStateOf(initial?.trigger?.takeIf { it.kind.allowsHold }?.minutes ?: 0) }
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
    val tempValue = temp.replace(',', '.').toDoubleOrNull()
    val condSrc = devices.firstOrNull { it.id == condDevice }
    val valid = name.isNotBlank() && (src != null || !triggerKind.usesDevice) &&
        (!triggerKind.usesWatts || (wattsValue != null && wattsValue > 0)) &&
        (!triggerKind.usesTemp || tempValue != null) &&
        (!condOn || condSrc != null) &&
        (!actionKind.usesDevice || dst != null) &&
        (actionKind != ActionKind.RUN_SCENE || sceneList.any { it.id == sceneId })

    // Kaydedilecek otomasyon; aşağıdaki özet de bundan okunur.
    val draft = Automation(
        id = initial?.id ?: NEW_AUTOMATION,
        name = name.trim(),
        enabled = initial?.enabled ?: true,
        trigger = Trigger(
            triggerKind, if (triggerKind.usesDevice) triggerDevice else "",
            channel = if (triggerKind.usesChannel) triggerChannel else 0,
            minutes = when {
                triggerKind.usesMinutes -> minutes
                triggerKind.usesChannelPower -> lowMinutes
                triggerKind.allowsHold -> holdMinutes
                triggerKind.isSun -> offset
                else -> 0
            },
            watts = when {
                triggerKind.usesWatts -> wattsValue ?: 0.0
                triggerKind.usesTemp -> tempValue ?: 0.0
                else -> 0.0
            },
            graceMinutes = if (triggerKind.usesChannelPower) grace else 0,
        ),
        action = when {
            actionKind.usesDevice -> AutoAction(actionKind, actionDevice, actionChannel)
            actionKind == ActionKind.RUN_SCENE -> AutoAction(actionKind, sceneId)
            else -> AutoAction(actionKind)
        },
        delayMinutes = delay,
        window = if (windowOn) ActiveWindow(windowStart, windowEnd, windowDays) else null,
        presence = presenceRule,
        condition = if (condOn) Condition(condKind, condDevice, if (condKind == ConditionKind.IS_ON || condKind == ConditionKind.IS_OFF) condChannel else 0) else null,
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
                FieldLabel(stringResource(R.string.automation_event))
                OptionGrid(kinds, triggerKind, { it.icon() }, { stringResource(it.shortTitle()) }) { triggerKind = it }
                if (triggerKind.usesDevice) {
                    FieldLabel(stringResource(R.string.automation_device))
                    DeviceChooser(devices, triggerDevice) { triggerDevice = it.id; triggerChannel = 0 }
                    if (triggerKind.usesChannel && src?.isMultiChannel == true) {
                        FieldLabel(stringResource(R.string.automation_channel))
                        ChannelChooser(src, triggerChannel) { triggerChannel = it }
                    }
                }
                if (triggerKind.isSun) {
                    // "Gün doğumundan 15 dk önce", "gün batımından 1 sa sonra".
                    Stepper(stringResource(R.string.sun_offset), offset, OFFSET_STEPS, format = { offsetLabel(it) }) { offset = it }
                }
                if (triggerKind.usesTemp) {
                    NumberField(temp, stringResource(R.string.automation_temp), "°C", allowNegative = true) { temp = it }
                    runtimeTemp(locationVersion)?.let {
                        Text(
                            stringResource(R.string.automation_current_temp, formatTemp(it)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
                if (triggerKind.isSun || triggerKind.usesTemp) {
                    val context = LocalContext.current
                    val store = remember { Store.get(context) }
                    key(locationVersion) {
                        LocationSetup(store, onChanged = { locationVersion++ }, modifier = Modifier.padding(top = 12.dp))
                    }
                }
                if (triggerKind.usesMinutes) {
                    Stepper(stringResource(R.string.automation_minutes_short), minutes, MINUTE_STEPS.filter { it > 0 }) { minutes = it }
                }
                if (triggerKind.usesWatts) {
                    NumberField(watts, stringResource(R.string.automation_watts), "W") { watts = it }
                }
                if (triggerKind.allowsHold) {
                    // Ör. "4 saattir hiç çalışmadı" (dondurucu arızası), "6 saattir durmadı" (kompresör).
                    Stepper(stringResource(R.string.automation_hold_short), holdMinutes, MINUTE_STEPS, zeroLabel = stringResource(R.string.automation_now)) { holdMinutes = it }
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

            // ------------------------------------------------ Yalnızca / Ve
            StepCard(Icons.Rounded.Tune, Hue.PURPLE, stringResource(R.string.auto_only), stringResource(R.string.auto_only_desc)) {
                ToggleLine(stringResource(R.string.window_toggle), windowOn) { windowOn = it }
                if (windowOn) {
                    val context = LocalContext.current
                    val startTitle = stringResource(R.string.window_start)
                    val endTitle = stringResource(R.string.window_end)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                        TimeChip(startTitle, formatMinute(context, windowStart), Modifier.weight(1f)) { timePicker.pick(startTitle, windowStart) { windowStart = it } }
                        TimeChip(endTitle, formatMinute(context, windowEnd), Modifier.weight(1f)) { timePicker.pick(endTitle, windowEnd) { windowEnd = it } }
                    }
                    DayChips(windowDays) { windowDays = it }
                    if (windowStart == windowEnd) {
                        Text(stringResource(R.string.window_all_day_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
                    }
                }
                ToggleLine(stringResource(R.string.presence_toggle), presenceRule != null) { presenceRule = if (it) PresenceRule.AWAY else null }
                presenceRule?.let { rule ->
                    OptionGrid(PresenceRule.entries, rule, { if (it == PresenceRule.AWAY) Icons.AutoMirrored.Rounded.DirectionsWalk else Icons.Rounded.Home },
                        { stringResource(if (it == PresenceRule.AWAY) R.string.presence_away else R.string.presence_home) }) { presenceRule = it }
                }
                ToggleLine(stringResource(R.string.cond_toggle), condOn) { condOn = it }
                if (condOn) {
                    FieldLabel(stringResource(R.string.automation_device))
                    DeviceChooser(devices, condDevice) { condDevice = it.id; condChannel = 0 }
                    val needsChannel = condKind == ConditionKind.IS_ON || condKind == ConditionKind.IS_OFF
                    if (needsChannel && condSrc?.isMultiChannel == true) {
                        FieldLabel(stringResource(R.string.automation_channel))
                        ChannelChooser(condSrc, condChannel) { condChannel = it }
                    }
                    FieldLabel(stringResource(R.string.cond_state))
                    OptionGrid(ConditionKind.entries, condKind, { it.icon() }, { stringResource(it.title()) }) { condKind = it }
                }
            }

            // ------------------------------------------------ O zaman
            StepCard(Icons.Rounded.Bolt, Hue.ORANGE, stringResource(R.string.auto_then), stringResource(R.string.automation_then)) {
                FieldLabel(stringResource(R.string.automation_action))
                OptionGrid(tab.actions, actionKind, { it.icon() }, { stringResource(it.editorTitle()) }, hue = { it.hue() }) { actionKind = it }
                if (actionKind == ActionKind.RUN_SCENE) {
                    FieldLabel(stringResource(R.string.scenes_title))
                    if (sceneList.isEmpty()) {
                        Text(stringResource(R.string.scene_none_yet), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        OptionGrid(sceneList.map { it.id }, sceneId, { Icons.Rounded.PlayArrow }, { id -> sceneList.first { it.id == id }.name }) { sceneId = it }
                    }
                }
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
            if (src != null || !triggerKind.usesDevice) {
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
/** Gün doğumu/batımı kayması: eksi önce, artı sonra (dakikadan saate). */
private val OFFSET_STEPS = listOf(-240, -180, -120, -90, -60, -45, -30, -20, -15, -10, -5, 0, 5, 10, 15, 20, 30, 45, 60, 90, 120, 180, 240)
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
private fun Stepper(
    label: String,
    value: Int,
    steps: List<Int>,
    zeroLabel: String? = null,
    format: (@Composable (Int) -> String)? = null,
    onChange: (Int) -> Unit,
) {
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
            when {
                format != null -> format(value)
                value == 0 && zeroLabel != null -> zeroLabel
                else -> holdLabel(value)
            },
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

/** Sayı alanı (güç eşiği W, sıcaklık °C); [allowNegative] eksi değere izin verir. */
@Composable
private fun NumberField(value: String, label: String, suffix: String, allowNegative: Boolean = false, onChange: (String) -> Unit) {
    TextField(
        value = value,
        onValueChange = { v ->
            onChange(v.filterIndexed { i, c -> c.isDigit() || c == '.' || c == ',' || (allowNegative && c == '-' && i == 0) })
        },
        label = { Text(label) },
        suffix = { Text(suffix) },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        keyboardOptions = tvKeyboardOptions(KeyboardOptions(keyboardType = if (allowNegative) KeyboardType.Text else KeyboardType.Decimal)),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = raisedOnTile(),
            unfocusedContainerColor = raisedOnTile(),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp).tvTextField(),
    )
}

/** Son okunan dış sıcaklık (izleme servisi yarım saatte bir okur). */
@Composable
private fun runtimeTemp(version: Int): Double? {
    val context = LocalContext.current
    return remember(version) { com.macerce.switchguard.data.RuntimeState.get(context).outdoorTemp }
}

/** Başlık ve anahtar: "Yalnızca şu saatlerde", "Ve şu koşulda". */
@Composable
private fun ToggleLine(text: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(top = 12.dp).clip(RoundedCornerShape(16.dp)).background(raisedOnTile())
            .tvFocusRing(RoundedCornerShape(16.dp))
            .selectable(selected = checked, onClick = { onChange(!checked) }, role = Role.Switch)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun TimeChip(label: String, value: String, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier.clip(shape).background(raisedOnTile()).tvFocusRing(shape)
            .selectable(selected = false, onClick = onClick, role = Role.Button)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    }
}

/** Haftanın günleri (Pzt…Paz); en az bir gün seçili kalır. */
@Composable
private fun DayChips(days: Int, onChange: (Int) -> Unit) {
    val names = stringArrayResource(R.array.day_short)
    val scheme = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        names.forEachIndexed { i, n ->
            val on = days and (1 shl i) != 0
            val shape = RoundedCornerShape(12.dp)
            Box(
                Modifier.weight(1f).heightIn(min = 40.dp).clip(shape)
                    .background(if (on) scheme.primary else raisedOnTile())
                    .tvFocusRing(shape)
                    .selectable(selected = on, role = Role.Checkbox, onClick = {
                        val next = days xor (1 shl i)
                        if (next != 0) onChange(next)
                    }),
                contentAlignment = Alignment.Center,
            ) {
                Text(n, style = MaterialTheme.typography.labelMedium, color = if (on) scheme.onPrimary else scheme.onSurface, maxLines = 1)
            }
        }
    }
}

private fun ConditionKind.icon(): ImageVector = when (this) {
    ConditionKind.IS_ON -> Icons.Rounded.PowerSettingsNew
    ConditionKind.IS_OFF -> Icons.Rounded.PowerOff
    ConditionKind.IS_ONLINE -> Icons.Rounded.Cloud
    ConditionKind.IS_OFFLINE -> Icons.Rounded.CloudOff
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
    TriggerKind.SUNRISE -> Icons.Rounded.WbTwilight
    TriggerKind.SUNSET -> Icons.Rounded.NightsStay
    TriggerKind.TEMP_ABOVE -> Icons.Rounded.WbSunny
    TriggerKind.TEMP_BELOW -> Icons.Rounded.AcUnit
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
    TriggerKind.SUNRISE -> R.string.evt_sunrise
    TriggerKind.SUNSET -> R.string.evt_sunset
    TriggerKind.TEMP_ABOVE -> R.string.evt_temp_above
    TriggerKind.TEMP_BELOW -> R.string.evt_temp_below
}

private fun ActionKind.icon(): ImageVector = when (this) {
    ActionKind.ALARM -> Icons.Rounded.Alarm
    ActionKind.NOTIFY -> Icons.Rounded.NotificationsActive
    ActionKind.TURN_ON -> Icons.Rounded.PowerSettingsNew
    ActionKind.TURN_OFF -> Icons.Rounded.PowerOff
    ActionKind.RUN_SCENE -> Icons.Rounded.PlayArrow
}

private fun ActionKind.hue(): Hue = when (this) {
    ActionKind.ALARM -> Hue.RED
    ActionKind.NOTIFY -> Hue.BLUE
    ActionKind.TURN_ON -> Hue.GREEN
    ActionKind.TURN_OFF -> Hue.SLATE
    ActionKind.RUN_SCENE -> Hue.PURPLE
}

private fun ActionKind.editorTitle(): Int = when (this) {
    ActionKind.ALARM -> R.string.auto_action_alarm
    ActionKind.NOTIFY -> R.string.auto_action_notify
    ActionKind.TURN_ON -> R.string.act_turn_on
    ActionKind.TURN_OFF -> R.string.act_turn_off
    ActionKind.RUN_SCENE -> R.string.act_run_scene
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
