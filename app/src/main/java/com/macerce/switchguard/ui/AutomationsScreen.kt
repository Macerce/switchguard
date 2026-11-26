package com.macerce.switchguard.ui

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Rule
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import com.macerce.switchguard.ui.theme.Brand
import com.macerce.switchguard.ui.theme.LocalDarkTheme
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.SensorsOff
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.BatteryStd
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.DoorFront
import androidx.compose.material.icons.rounded.Water
import com.macerce.switchguard.core.SensorKind
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import com.macerce.switchguard.core.PresenceRule
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.runtime.key
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.res.stringArrayResource
import com.macerce.switchguard.core.ActiveWindow
import com.macerce.switchguard.core.Condition
import com.macerce.switchguard.core.ConditionKind
import com.macerce.switchguard.core.ConditionMatch
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.DoneAll
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
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Memory
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
import androidx.compose.material3.TextButton
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
import com.macerce.switchguard.core.DeviceCategory
import androidx.compose.ui.res.pluralStringResource
import com.macerce.switchguard.core.AutomationEngine
import com.macerce.switchguard.core.DeviceGroup
import com.macerce.switchguard.core.DeviceSnapshot
import com.macerce.switchguard.core.Trigger
import com.macerce.switchguard.core.TriggerKind
import com.macerce.switchguard.data.Store
import com.macerce.switchguard.service.MonitorService
import java.util.UUID
import com.macerce.switchguard.core.StepTree
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Loop
import androidx.compose.material.icons.rounded.AltRoute
import androidx.compose.material.icons.rounded.Pending
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Numbers

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
    // Gruplar ve uyarı kategorileri otomasyonda cihaz gibi seçilir.
    val devices = remember(version) { automationDevices(store.snapshotsForRules, context.resources) }
    val monitoring = remember(version) { store.monitoringEnabled }
    val helpers = remember(version) { store.helpers }
    var editingHelper by rememberSaveable { mutableStateOf<String?>(null) }
    editingHelper?.let { id -> HelperDialog(helpers.firstOrNull { it.id == id }) { editingHelper = null } }
    var editing by rememberSaveable { mutableStateOf<String?>(null) }
    var templateName by rememberSaveable { mutableStateOf<String?>(null) }
    var pickTemplate by rememberSaveable { mutableStateOf(false) }
    var editingScene by rememberSaveable { mutableStateOf<String?>(null) }
    var editingGroup by rememberSaveable { mutableStateOf<String?>(null) }
    var editingCycle by rememberSaveable { mutableStateOf<String?>(null) }
    val cycles = remember(version) { store.cycles }
    val cycleRuns = remember(version) { store.cycleRuns }
    val scenes = remember(version) { store.scenes }
    val groups = remember(version) { store.groups }

    editingGroup?.let { id ->
        BackHandler { editingGroup = null }
        Box(modifier) {
            GroupEditor(
                initial = groups.firstOrNull { it.id == id },
                devices = remember(version) { store.deviceLayout.sections(store.snapshots).flatMap { it.devices } },
                onSave = { store.saveGroup(it); editingGroup = null },
                onDelete = { store.deleteGroup(id); editingGroup = null },
                onBack = { editingGroup = null },
            )
        }
        return
    }

    editingCycle?.let { id ->
        BackHandler { editingCycle = null }
        Box(modifier) {
            CycleEditor(
                initial = cycles.firstOrNull { it.id == id },
                devices = remember(version) { devices.filterNot { DeviceCategory.isCategory(it.id) } },
                onSave = { store.saveCycle(it); editingCycle = null },
                onDelete = {
                    if (id in cycleRuns) MonitorService.cycle(context, id, start = false, delete = true) else store.deleteCycle(id)
                    editingCycle = null
                },
                onBack = { editingCycle = null },
            )
        }
        return
    }

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

    // Cihaza yazılmış kural değişti / silindi: cihazdaki zamanlayıcılar için sor.
    var deviceSync by remember { mutableStateOf<Pair<String, Automation?>?>(null) }
    deviceSync?.let { (rid, rule) ->
        val plan = remember(rule) { rule?.let { com.macerce.switchguard.core.RuleToDevice.plan(it, store.snapshots.associateBy { d -> d.id }) } }
        RuleDeviceSyncDialog(rid, rule, plan) { deviceSync = null }
    }
    editing?.let { id ->
        BackHandler { editing = null; templateName = null }
        val template = templateName?.let { t -> runCatching { com.macerce.switchguard.core.AutomationTemplate.valueOf(t) }.getOrNull() }
            ?.let { t -> t.pickDevice(devices.filterNot { DeviceCategory.isCategory(it.id) })?.let { d -> t.build(d, stringResource(t.title())) } }
        AutomationEditor(
            tab = template?.let { AutomationTab.tabOf(it) } ?: AutomationTab.AUTOMATIONS,
            initial = automations.firstOrNull { it.id == id },
            devices = devices,
            modifier = modifier,
            onSave = { a ->
                val old = automations.firstOrNull { it.id == a.id }
                store.saveAutomation(a)
                if (old != null && store.ruleDevices(a.id).isNotEmpty()) {
                    val devs = store.snapshots.associateBy { it.id }
                    val plan = { r: Automation -> com.macerce.switchguard.core.RuleToDevice.plan(r, devs) }
                    if (plan(old.copy(enabled = true, name = "")) != plan(a.copy(enabled = true, name = ""))) deviceSync = a.id to a
                }
                editing = null; templateName = null
            },
            onDelete = {
                store.deleteAutomation(id); com.macerce.switchguard.data.TraceLog.clear(context, id); editing = null
                if (store.ruleDevices(id).isNotEmpty()) deviceSync = id to null
            },
            onBack = { editing = null; templateName = null },
            template = template,
        )
        return
    }
    var importing by rememberSaveable { mutableStateOf(false) }
    if (importing) RuleImportDialog(onDismiss = { importing = false }) { importing = false; if (AutomationTab.AUTOMATIONS.of(listOf(it)).isNotEmpty()) editing = it.id }
    if (pickTemplate) {
        TemplatePicker(devices.filterNot { DeviceCategory.isCategory(it.id) }, onDismiss = { pickTemplate = false }) { t ->
            pickTemplate = false
            templateName = t?.name
            editing = NEW_AUTOMATION
        }
    }

    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            AppTopBar(stringResource(R.string.tab_automations), actions = {
                IconButton(modifier = Modifier.tvFocusRing(), onClick = { importing = true }) {
                    Icon(Icons.Rounded.Download, stringResource(R.string.rule_import))
                }
            })
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
                        Text(stringResource(R.string.groups_title), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 4.dp, top = 8.dp))
                        GroupsStrip(groups, store.snapshotsWithGroups) { editingGroup = it }
                        Text(stringResource(R.string.cycles_title), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 4.dp, top = 8.dp))
                        CyclesStrip(cycles, cycleRuns) { editingCycle = it }
                        Text(stringResource(R.string.helpers_title), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 4.dp, top = 8.dp))
                        HelpersStrip(helpers) { editingHelper = it }
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
            onClick = { pickTemplate = true },
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
    val icon = a.action.kind.icon()
    val hue = a.action.kind.hue()
    val context = LocalContext.current
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
            LastRunLine(a.id)
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Switch(checked = a.enabled, onCheckedChange = onToggle)
            // Şimdi çalıştır: tetikleyici ve koşullar atlanır, adımlar hemen başlar.
            val started = stringResource(R.string.rule_run_now_done, a.name)
            IconButton(modifier = Modifier.tvFocusRing(), onClick = {
                MonitorService.runRule(context, a.id)
                android.widget.Toast.makeText(context, started, android.widget.Toast.LENGTH_SHORT).show()
            }) { Icon(Icons.Rounded.PlayArrow, stringResource(R.string.rule_run_now), tint = MaterialTheme.colorScheme.primary) }
        }
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
internal fun describe(a: Automation, devices: List<DeviceSnapshot>): Pair<String, String> {
    val or = " " + stringResource(R.string.cond_join_or) + " "
    val triggers = a.allTriggers.map { triggerText(it, devices) }.joinToString(or)
    // İlk eylem ve ek eylemler: "Pompa: Kapat → 5 dk sonra: Vana: Kapat".
    val action = a.allActions.mapIndexed { i, act ->
        val text = actionText(act, devices)
        if (i > 0 && act.waitMinutes > 0) stringResource(R.string.rule_step_after, holdLabel(act.waitMinutes), text) else text
    }.joinToString(" → ")
    val delay = if (a.delayMinutes > 0) " · " + stringResource(R.string.automation_after, a.delayMinutes) else ""
    val extras = buildList {
        a.window?.let { add(windowText(it)) }
        if (a.conditions.isNotEmpty()) add(conditionsText(a.conditions, a.conditionMatch, devices, a.groupMatch))
        a.presence?.let { add(stringResource(if (it == PresenceRule.AWAY) R.string.presence_only_away else R.string.presence_only_home)) }
    }.joinToString("") { " · $it" }
    return triggers + extras to "$action$delay"
}

/** Tek tetikleyicinin metni: "Rögar · Kanal 1 açılınca", "Saatte 07:00 · her gün". */
@Composable
internal fun triggerText(t: Trigger, devices: List<DeviceSnapshot>): String {
    val src = devices.firstOrNull { it.id == t.deviceId }
    val zoneContext = LocalContext.current
    val srcName = when {
        t.kind.usesZone -> remember(t.deviceId) { Store.get(zoneContext).zones.firstOrNull { it.id == t.deviceId }?.name }
            ?: stringResource(R.string.device_missing)
        t.kind.usesCycle -> remember(t.deviceId) { Store.get(zoneContext).cycles.firstOrNull { it.id == t.deviceId }?.name }
            ?: stringResource(R.string.device_missing)
        !t.kind.usesDevice -> null
        else -> src?.let { channelLabel(it, t.channel, t.kind.usesChannel) } ?: stringResource(R.string.device_missing)
    }
    val trigger = when (t.kind) {
        // "Saatte 07:00 · Pzt, Sal, Çar".
        TriggerKind.TIME -> stringResource(t.kind.title()) + " " + formatMinute(zoneContext, t.minutes) + " · " +
            (if (t.days == com.macerce.switchguard.core.ActiveWindow.ALL_DAYS) stringResource(R.string.days_every)
            else daysLabel((0..6).filter { t.days and (1 shl it) != 0 }.map { (it + 1) % 7 }.toSet()))
        TriggerKind.SUNRISE, TriggerKind.SUNSET -> stringResource(t.kind.title()) + offsetSuffix(t.minutes)
        TriggerKind.TEMP_ABOVE, TriggerKind.TEMP_BELOW -> stringResource(t.kind.title()) + " " + formatTemp(t.watts)
        // "Salon sensörü · Sıcaklık > 26 °C", "Ön kapı · Kapı açık".
        TriggerKind.SENSOR_ABOVE, TriggerKind.SENSOR_BELOW -> t.sensor?.let {
            stringResource(it.label()) + (if (t.kind == TriggerKind.SENSOR_ABOVE) " > " else " < ") + formatSensor(it, t.watts)
        } ?: stringResource(t.kind.title())
        TriggerKind.SENSOR_ACTIVE, TriggerKind.SENSOR_CLEAR -> t.sensor?.let { stringResource(it.stateLabel(t.kind == TriggerKind.SENSOR_ACTIVE)) }
            ?: stringResource(t.kind.title())
        TriggerKind.STAYED_ON, TriggerKind.STAYED_OFF -> stringResource(t.kind.title()) + " " + pluralStringResource(R.plurals.minutes_n, t.minutes, t.minutes)
        TriggerKind.POWER_ABOVE, TriggerKind.POWER_BELOW -> stringResource(t.kind.title()) + " " + formatWatts(t.watts) +
            (if (t.minutes > 0) " " + stringResource(R.string.automation_power_for, holdLabel(t.minutes)) else "")
        TriggerKind.CHANNEL_POWER_LOW -> stringResource(t.kind.title()) + " " + formatWatts(t.watts) + " " +
            (if (t.graceMinutes > 0) stringResource(R.string.automation_low_for, t.minutes, t.graceMinutes)
            else stringResource(R.string.automation_low_for_nograce, t.minutes))
        else -> stringResource(t.kind.title())
    }
    return if (srcName != null) "$srcName · $trigger" else trigger
}

/** Tek eylemin metni: "Pompa: Kapat", "Sahne: Gece", "Döngüyü başlat: Sulama". */
@Composable
internal fun actionText(act: com.macerce.switchguard.core.AutoAction, devices: List<DeviceSnapshot>): String {
    val context = LocalContext.current
    flowText(act, devices)?.let { return it }
    return when {
        act.kind.usesDevice -> {
            val dst = devices.firstOrNull { it.id == act.deviceId }
            val dstName = dst?.let { channelLabel(it, act.channel, true) } ?: stringResource(R.string.device_missing)
            "$dstName: " + stringResource(act.kind.title())
        }
        act.kind.usesCycle -> {
            val cycle = Store.get(context).cycles.firstOrNull { it.id == act.deviceId }
            stringResource(act.kind.title()) + ": " + (cycle?.name ?: stringResource(R.string.device_missing))
        }
        act.kind == ActionKind.RUN_SCENE -> {
            val scene = Store.get(context).scenes.firstOrNull { it.id == act.deviceId }
            stringResource(R.string.auto_action_scene, scene?.name ?: stringResource(R.string.device_missing))
        }
        act.kind == ActionKind.SAVE_STATE -> {
            val target = Store.get(context).scenes.firstOrNull { it.id == act.deviceId }?.name
                ?: devices.firstOrNull { it.id == act.deviceId }?.name
            stringResource(act.kind.title()) + ": " + (target ?: stringResource(R.string.device_missing))
        }
        else -> stringResource(act.kind.title())
    }
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

/** "23,5 °C", "55 %", "120 lx". */
fun formatSensor(kind: SensorKind, v: Double): String = formatNumber(v) + " " + kind.unit

fun SensorKind.label(): Int = when (this) {
    SensorKind.TEMPERATURE -> R.string.sensor_temperature
    SensorKind.HUMIDITY -> R.string.sensor_humidity
    SensorKind.BATTERY -> R.string.sensor_battery
    SensorKind.ILLUMINANCE -> R.string.sensor_illuminance
    SensorKind.DOOR -> R.string.sensor_door
    SensorKind.MOTION -> R.string.sensor_motion
    SensorKind.WATER -> R.string.sensor_water
    SensorKind.COUNT -> R.string.sensor_count
}

/** İkili sensörün durumu: "Kapı açık" / "Kapı kapalı". */
fun SensorKind.stateLabel(active: Boolean): Int = when (this) {
    SensorKind.DOOR -> if (active) R.string.sensor_door_open else R.string.sensor_door_closed
    SensorKind.MOTION -> if (active) R.string.sensor_motion_on else R.string.sensor_motion_off
    SensorKind.WATER -> if (active) R.string.sensor_water_on else R.string.sensor_water_off
    else -> label()
}

fun SensorKind.icon(): ImageVector = when (this) {
    SensorKind.TEMPERATURE -> Icons.Rounded.Thermostat
    SensorKind.HUMIDITY -> Icons.Rounded.WaterDrop
    SensorKind.BATTERY -> Icons.Rounded.BatteryStd
    SensorKind.ILLUMINANCE -> Icons.Rounded.LightMode
    SensorKind.DOOR -> Icons.Rounded.DoorFront
    SensorKind.MOTION -> Icons.AutoMirrored.Rounded.DirectionsWalk
    SensorKind.WATER -> Icons.Rounded.Water
    SensorKind.COUNT -> Icons.Rounded.Numbers
}

/** "22:00–06:00, hafta içi". */
@Composable
internal fun windowText(w: ActiveWindow): String {
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
    ConditionKind.POWER_ABOVE -> R.string.cond_power_above
    ConditionKind.POWER_BELOW -> R.string.cond_power_below
    ConditionKind.SENSOR_ABOVE -> R.string.cond_sensor_above
    ConditionKind.SENSOR_BELOW -> R.string.cond_sensor_below
    ConditionKind.SENSOR_ACTIVE -> R.string.cond_sensor_active
    ConditionKind.SENSOR_CLEAR -> R.string.cond_sensor_clear
    ConditionKind.TEMP_ABOVE -> R.string.cond_temp_above
    ConditionKind.TEMP_BELOW -> R.string.cond_temp_below
    ConditionKind.ON_FOR -> R.string.cond_on_for
    ConditionKind.OFF_FOR -> R.string.cond_off_for
    ConditionKind.TIME_BETWEEN -> R.string.cond_time_between
    ConditionKind.DARK -> R.string.cond_dark
    ConditionKind.LIGHT -> R.string.cond_light
    ConditionKind.HOME -> R.string.cond_home
    ConditionKind.AWAY -> R.string.cond_away
    ConditionKind.TRIGGERED_BY -> R.string.cond_triggered_by
}

/** "45 dk", "6 sa", "1 sa 30 dk". */
@Composable
internal fun holdLabel(minutes: Int): String = when {
    minutes < 60 -> pluralStringResource(R.plurals.minutes_n, minutes, minutes)
    minutes % 60 == 0 -> pluralStringResource(R.plurals.hours_n, minutes / 60, minutes / 60)
    else -> stringResource(R.string.dur_hour_min, minutes / 60, minutes % 60)
}

@Composable
internal fun channelLabel(d: DeviceSnapshot, channel: Int, withChannel: Boolean): String =
    if (withChannel && d.isMultiChannel) "${d.name} · " + stringResource(R.string.channel_n, channel + 1) else d.name

fun TriggerKind.title(): Int = when (this) {
    TriggerKind.TIME -> R.string.trigger_time
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
    TriggerKind.ZONE_ENTER -> R.string.trigger_zone_enter
    TriggerKind.ZONE_EXIT -> R.string.trigger_zone_exit
    TriggerKind.CYCLE_STARTED -> R.string.trigger_cycle_started
    TriggerKind.CYCLE_FINISHED -> R.string.trigger_cycle_finished
    TriggerKind.SENSOR_ABOVE -> R.string.trigger_sensor_above
    TriggerKind.SENSOR_BELOW -> R.string.trigger_sensor_below
    TriggerKind.SENSOR_ACTIVE -> R.string.trigger_sensor_active
    TriggerKind.SENSOR_CLEAR -> R.string.trigger_sensor_clear
}

fun ActionKind.title(): Int = when (this) {
    ActionKind.TURN_ON -> R.string.auto_action_turn_on
    ActionKind.TURN_OFF -> R.string.auto_action_turn_off
    ActionKind.ALARM -> R.string.auto_action_alarm
    ActionKind.NOTIFY -> R.string.auto_action_notify
    ActionKind.RUN_SCENE -> R.string.act_run_scene
    ActionKind.START_CYCLE -> R.string.act_start_cycle
    ActionKind.STOP_CYCLE -> R.string.act_stop_cycle
    ActionKind.SAVE_STATE -> R.string.act_save_state
    ActionKind.RESTORE_STATE -> R.string.act_restore_state
    ActionKind.TOGGLE -> R.string.act_toggle
    ActionKind.SET_LEVEL -> R.string.act_set_level
    ActionKind.DELAY -> R.string.act_delay
    ActionKind.WAIT_UNTIL -> R.string.act_wait_until
    ActionKind.IF -> R.string.act_if
    ActionKind.REPEAT -> R.string.act_repeat
    ActionKind.STOP -> R.string.act_stop
    ActionKind.RUN_AUTOMATION -> R.string.act_run_rule
    ActionKind.SEND -> R.string.act_send
    ActionKind.COUNTER_ADD -> R.string.act_counter_add
    ActionKind.COUNTER_SET -> R.string.act_counter_set
}

// ---------------------------------------------------------------- düzenleyici

/**
 * Otomasyon ve koşullu uyarı düzenleyicisi. [tab] işlem seçeneklerini belirler: Uyarılar'da yalnızca alarm çal /
 * bildirim gönder (hedef cihaz yok), Otomasyon'da yalnızca cihazı aç / kapat. Var olan kayıt kendi sekmesinden açılır.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutomationEditor(
    tab: AutomationTab,
    initial: Automation?,
    devices: List<DeviceSnapshot>,
    modifier: Modifier,
    onSave: (Automation) -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
    /** Yeni kayıt bir şablondan dolduruluyor (kimliği yok, kaydedilince yeni kural olur). */
    template: Automation? = null,
) {
    val existing = initial
    @Suppress("NAME_SHADOWING") val initial = initial ?: template
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
    var sensorKind by rememberSaveable { mutableStateOf(initial?.trigger?.sensor) }
    var sensorValue by rememberSaveable { mutableStateOf(initial?.trigger?.takeIf { it.kind.usesSensorValue }?.watts?.let(::formatNumber) ?: "") }
    var windowOn by rememberSaveable { mutableStateOf(initial?.window != null) }
    var windowStart by rememberSaveable { mutableStateOf(initial?.window?.startMinute ?: 22 * 60) }
    var windowEnd by rememberSaveable { mutableStateOf(initial?.window?.endMinute ?: 6 * 60) }
    var windowDays by rememberSaveable { mutableStateOf(initial?.window?.days ?: ActiveWindow.ALL_DAYS) }
    var presenceRule by rememberSaveable { mutableStateOf(initial?.presence) }
    var sceneId by rememberSaveable { mutableStateOf(initial?.action?.takeIf { it.kind == ActionKind.RUN_SCENE }?.deviceId ?: "") }
    // "Durumu hatırla" hedefi: tek cihaz ya da bir sahnenin cihazları; hiçbir zaman "hepsi" değil.
    var saveScope by rememberSaveable { mutableStateOf(initial?.action?.takeIf { it.kind == ActionKind.SAVE_STATE }?.deviceId ?: "") }
    val sceneList = Store.get(LocalContext.current).scenes.let { remember(it) { it } }
    val cycleList = Store.get(LocalContext.current).cycles.let { remember(it) { it } }
    var triggerCycle by rememberSaveable { mutableStateOf(initial?.trigger?.takeIf { it.kind.usesCycle }?.deviceId ?: cycleList.firstOrNull()?.id.orEmpty()) }
    var actionCycle by rememberSaveable { mutableStateOf(initial?.action?.takeIf { it.kind.usesCycle }?.deviceId ?: cycleList.firstOrNull()?.id.orEmpty()) }
    var conds by rememberSaveable(stateSaver = CondDraft.ListSaver) { mutableStateOf(initial?.conditions.orEmpty().map(CondDraft::of)) }
    var condMatch by rememberSaveable { mutableStateOf(initial?.conditionMatch ?: ConditionMatch.ALL) }
    var locationVersion by remember { mutableStateOf(0) }
    var zoneId by rememberSaveable { mutableStateOf(initial?.trigger?.takeIf { it.kind.usesZone }?.deviceId ?: "") }
    var zonesVersion by remember { mutableStateOf(0) }
    var zoneEditor by remember { mutableStateOf(false) }
    val zoneContext = LocalContext.current
    val zones = remember(zonesVersion) { Store.get(zoneContext).zones }
    if (zoneId.isEmpty() && zones.size == 1) zoneId = zones[0].id
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
    // Saat tetikleyicisi: SS:DD ve günler.
    var clockMinute by rememberSaveable { mutableStateOf(initial?.trigger?.takeIf { it.kind.isClock }?.minutes ?: 7 * 60) }
    var clockDays by rememberSaveable { mutableStateOf(initial?.trigger?.takeIf { it.kind.isClock }?.days ?: ActiveWindow.ALL_DAYS) }
    // Ek eylemler (yalnız Otomasyon sekmesinde): ilk eylemden sonra sırayla, her biri kendi beklemesiyle.
    val extras = rememberSaveable(saver = extraActionsSaver) { mutableStateListOf<AutoAction>().apply { initial?.extraActions?.let(::addAll) } }
    fun setExtras(list: List<AutoAction>) { extras.clear(); extras.addAll(list) }
    // Bütün tetikleyiciler; düzenlenen [editingTrigger]'ın güncel hali yukarıdaki alanlarda.
    val trigList = rememberSaveable(saver = triggersSaver) { mutableStateListOf<Trigger>().apply { initial?.allTriggers?.let(::addAll) } }
    var editingTrigger by rememberSaveable { mutableStateOf(0) }
    var groupMatchJson by rememberSaveable { mutableStateOf(com.macerce.switchguard.core.CondSet.groupMatchToJson(initial?.groupMatch.orEmpty()).toString()) }
    val groupMatch = remember(groupMatchJson) { com.macerce.switchguard.core.CondSet.groupMatchFromJson(org.json.JSONObject(groupMatchJson)) }
    var runMode by rememberSaveable { mutableStateOf(initial?.mode ?: com.macerce.switchguard.core.RunMode.RESTART) }
    var maxRuns by rememberSaveable { mutableStateOf(initial?.maxRuns ?: 3) }
    var confirmDelete by remember { mutableStateOf(false) }
    var showTrace by rememberSaveable { mutableStateOf(false) }
    var loopGuard by rememberSaveable { mutableStateOf(initial?.loopGuard ?: 0) }
    if (showTrace && existing != null) TraceSheet(existing, devices) { showTrace = false }
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
            // Kategori: yalnız anahtar ve bağlantı olayları (gün doğumu/sıcaklık cihazsız, yine sunulur).
            // Bölge telefonun konumuyla çalışır; TV'de konum yok.
            it.usesZone -> !tv
            src != null && DeviceCategory.isCategory(src.id) -> !it.usesDevice || it in DeviceCategory.TRIGGER_KINDS
            it.usesChannelPower -> src?.hasChannelPower == true
            it.usesWatts -> src?.hasEnergy == true
            it.usesSensorValue -> src?.sensors?.keys?.any { k -> !k.binary } == true
            it.usesSensorState -> src?.sensors?.keys?.any { k -> k.binary } == true
            // Yalnız sensör olan cihazın rölesi yok: aç/kapa olayları sunulmaz.
            it.usesChannel || it.usesMinutes -> src?.sensorOnly != true
            else -> true
        }
    }
    if (triggerKind !in kinds) triggerKind = kinds.firstOrNull() ?: TriggerKind.TURNED_ON
    // Seçili olaya uyan sensörler (sayısal ya da ikili); seçim uymuyorsa ilkine geçilir.
    val sensorChoices = src?.sensors?.keys?.filter { triggerKind.usesSensor && it.binary == triggerKind.usesSensorState }.orEmpty()
    if (triggerKind.usesSensor && sensorKind !in sensorChoices) sensorKind = sensorChoices.firstOrNull()
    // Yeni eşik alanı sensörün şu anki değeriyle (yuvarlanmış) dolar: kullanıcı neyin üstünde/altında olduğunu görsün.
    if (triggerKind.usesSensorValue && sensorValue.isEmpty()) {
        sensorKind?.let { k -> src?.sensors?.get(k) }?.let { sensorValue = formatNumber(Math.round(it).toDouble()) }
    }
    val sensorThreshold = sensorValue.replace(',', '.').toDoubleOrNull()

    val wattsValue = watts.replace(',', '.').toDoubleOrNull()
    val tempValue = temp.replace(',', '.').toDoubleOrNull()
    // Kategoriler yalnız tetikleyicide seçilir; koşul ve hedef gerçek cihaz (ya da grup) ister.
    val targets = remember(devices) { devices.filterNot { DeviceCategory.isCategory(it.id) } }
    // Eylem hedefi açılıp kapatılabilen cihaz olmalı (yalnız sensör olanlar değil).
    val actionTargets = remember(targets) { targets.filterNot { it.sensorOnly } }
    if (dst?.sensorOnly == true) actionDevice = actionTargets.firstOrNull()?.id.orEmpty()

    // Düzenlenen tetikleyici (alanlardan).
    val current = Trigger(
            triggerKind,
            when {
                triggerKind.usesDevice -> triggerDevice
                triggerKind.usesZone -> zoneId
                triggerKind.usesCycle -> triggerCycle
                else -> ""
            },
            channel = if (triggerKind.usesChannel) triggerChannel else 0,
            minutes = when {
                triggerKind.isClock -> clockMinute
                triggerKind.usesMinutes -> minutes
                triggerKind.usesChannelPower -> lowMinutes
                triggerKind.allowsHold -> holdMinutes
                triggerKind.isSun -> offset
                else -> 0
            },
            watts = when {
                triggerKind.usesWatts -> wattsValue ?: 0.0
                triggerKind.usesTemp -> tempValue ?: 0.0
                triggerKind.usesSensorValue -> sensorThreshold ?: 0.0
                else -> 0.0
            },
            graceMinutes = if (triggerKind.usesChannelPower) grace else 0,
            sensor = if (triggerKind.usesSensor) sensorKind else null,
            days = if (triggerKind.isClock) clockDays else ActiveWindow.ALL_DAYS,
        )
    val allTriggers = if (trigList.isEmpty()) listOf(current) else trigList.toMutableList().also { it[editingTrigger.coerceIn(0, it.size - 1)] = current }
    /** Başka bir tetikleyiciyi düzenlemeye geç: şu ankini listeye yaz, onunkini alanlara yükle. */
    fun editTrigger(i: Int) {
        if (trigList.isEmpty()) trigList.add(current) else trigList[editingTrigger.coerceIn(0, trigList.size - 1)] = current
        val t = trigList.getOrNull(i) ?: return
        editingTrigger = i
        triggerKind = t.kind
        when {
            t.kind.usesDevice -> triggerDevice = t.deviceId
            t.kind.usesZone -> zoneId = t.deviceId
            t.kind.usesCycle -> triggerCycle = t.deviceId
        }
        triggerChannel = t.channel
        when {
            t.kind.isClock -> { clockMinute = t.minutes; clockDays = t.days }
            t.kind.usesMinutes -> minutes = t.minutes.coerceAtLeast(1)
            t.kind.usesChannelPower -> { lowMinutes = t.minutes; grace = t.graceMinutes }
            t.kind.allowsHold -> holdMinutes = t.minutes
            t.kind.isSun -> offset = t.minutes
        }
        when {
            t.kind.usesWatts -> watts = formatNumber(t.watts)
            t.kind.usesTemp -> temp = formatNumber(t.watts)
            t.kind.usesSensorValue -> sensorValue = formatNumber(t.watts)
        }
        sensorKind = t.sensor
    }
    fun addTrigger() {
        editTrigger(editingTrigger)
        trigList.add(current.copy())
        editTrigger(trigList.size - 1)
    }
    fun removeTrigger(i: Int) {
        if (trigList.size <= 1) return
        editTrigger(if (i == 0) 1 else 0)
        trigList.removeAt(i)
        editTrigger(if (editingTrigger > i) editingTrigger - 1 else editingTrigger.coerceAtMost(trigList.size - 1))
    }

    val helperList = Store.get(LocalContext.current).helpers.let { remember(it) { it } }
    val ruleList = Store.get(LocalContext.current).automations.let { list -> remember(list) { list.filter { it.id != existing?.id } } }
    val storeForLights = Store.get(LocalContext.current)
    val stepCtx = StepCtx(
        targets = actionTargets, condTargets = targets, scenes = sceneList, cycles = cycleList, helpers = helperList, rules = ruleList,
        lights = remember(actionTargets) { lightTargets(storeForLights, actionTargets) }, triggerCount = allTriggers.size, locationVersion = locationVersion,
    )

    val valid = name.isNotBlank() && (src != null || !triggerKind.usesDevice) &&
        (!triggerKind.usesWatts || (wattsValue != null && wattsValue > 0)) &&
        (!triggerKind.usesTemp || tempValue != null) &&
        (!triggerKind.usesSensor || sensorKind != null) &&
        (!triggerKind.usesSensorValue || sensorThreshold != null) &&
        (!triggerKind.usesZone || zones.any { it.id == zoneId }) &&
        conds.all { it.valid(targets) } &&
        (!actionKind.usesDevice || (dst != null && !DeviceCategory.isCategory(dst.id) && !dst.sensorOnly)) &&
        (actionKind != ActionKind.RUN_SCENE || sceneList.any { it.id == sceneId }) &&
        (actionKind != ActionKind.SAVE_STATE || snapshotTargetValid(saveScope, actionTargets, sceneList)) &&
        (!triggerKind.usesCycle || cycleList.any { it.id == triggerCycle }) &&
        (!actionKind.usesCycle || cycleList.any { it.id == actionCycle }) &&
        (!triggerKind.isClock || clockDays != 0) &&
        (alert || extras.all { x -> stepValid(x, stepCtx) })

    // Kaydedilecek otomasyon; aşağıdaki özet de bundan okunur.
    val draft = Automation(
        id = existing?.id ?: NEW_AUTOMATION,
        name = name.trim(),
        enabled = initial?.enabled ?: true,
        trigger = allTriggers.first(),
        extraTriggers = allTriggers.drop(1),
        groupMatch = groupMatch.filterKeys { g -> conds.any { it.group == g } },
        mode = if (alert) com.macerce.switchguard.core.RunMode.RESTART else runMode,
        maxRuns = maxRuns,
        action = when {
            actionKind.usesDevice -> AutoAction(actionKind, actionDevice, actionChannel)
            actionKind == ActionKind.RUN_SCENE -> AutoAction(actionKind, sceneId)
            actionKind == ActionKind.SAVE_STATE -> AutoAction(actionKind, saveScope)
            actionKind.usesCycle -> AutoAction(actionKind, actionCycle)
            else -> AutoAction(actionKind)
        },
        delayMinutes = delay,
        window = if (windowOn) ActiveWindow(windowStart, windowEnd, windowDays) else null,
        presence = presenceRule,
        conditions = conds.map { it.toCondition() },
        conditionMatch = condMatch,
        extraActions = if (alert) emptyList() else extras.toList(),
        loopGuard = if (alert) 0 else loopGuard,
    )

    var showShare by remember { mutableStateOf(false) }
    if (showShare && existing != null) RuleShareDialog(existing) { showShare = false }
    // Basit saatli kural cihazın kendi zamanlayıcısına da yazılabilir (telefon kapalıyken de çalışsın).
    val toDeviceContext = LocalContext.current
    val toDevice = remember(existing) {
        existing?.let { com.macerce.switchguard.core.RuleToDevice.plan(it, Store.get(toDeviceContext).snapshots.associateBy { d -> d.id }) }
    }
    var askToDevice by remember { mutableStateOf(false) }
    if (askToDevice && existing != null && toDevice != null) RuleToDeviceDialog(existing, toDevice) { askToDevice = false }
    Column(modifier.fillMaxSize()) {
        AppTopBar(
            stringResource(
                when {
                    alert -> if (existing == null) R.string.alert_new else R.string.alert_edit
                    else -> if (existing == null) R.string.automation_new else R.string.automation_edit
                }
            ),
            navigationIcon = { Box(Modifier.focusRequester(entry).focusGroup()) { BackButton(onBack) } },
            small = true,
            actions = {
                if (existing != null) {
                    val runContext = LocalContext.current
                    val started = stringResource(R.string.rule_run_now_done, existing.name)
                    IconButton(modifier = Modifier.tvFocusRing(), onClick = {
                        MonitorService.runRule(runContext, existing.id)
                        android.widget.Toast.makeText(runContext, started, android.widget.Toast.LENGTH_SHORT).show()
                    }) { Icon(Icons.Rounded.PlayArrow, stringResource(R.string.rule_run_now)) }
                    IconButton(modifier = Modifier.tvFocusRing(), onClick = { showTrace = true }) {
                        Icon(Icons.Rounded.History, stringResource(R.string.trace_title))
                    }
                    if (!alert) IconButton(modifier = Modifier.tvFocusRing(), onClick = { showShare = true }) {
                        Icon(Icons.Rounded.Share, stringResource(R.string.rule_share))
                    }
                    if (toDevice != null) IconButton(modifier = Modifier.tvFocusRing(), onClick = { askToDevice = true }) {
                        Icon(Icons.Rounded.Memory, stringResource(R.string.rule_to_device))
                    }
                    IconButton(modifier = Modifier.tvFocusRing(), onClick = { confirmDelete = true }) {
                        Icon(Icons.Rounded.Delete, stringResource(R.string.action_delete))
                    }
                }
            },
        )
        if (targets.isEmpty()) {
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
            // Cihaza yazılmış kural: cihaz kendi başına çalıştırır; değişince kayıtta cihazı güncellemek sorulur.
            val onDevice = remember(existing) { existing?.let { Store.get(toDeviceContext).ruleDevices(it.id).size } ?: 0 }
            if (onDevice > 0) Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Memory, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                Text(
                    stringResource(R.string.rule_on_device_note),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // ------------------------------------------------ Eğer
            // Videodaki düzen: EĞER ve İSE kartlarında seçili tetikleyici / eylem renkli çerçeveli satırdır; dokununca
            // alttan açılan sayfada değiştirilir (kumandada alanlar yerinde kalır). Koşullar ayrı, isteğe bağlı bir liste.
            val summary = if (src != null || !triggerKind.usesDevice) describe(draft, devices) else null
            val triggerContent: @Composable ColumnScope.() -> Unit = {
                FieldLabel(stringResource(R.string.automation_event))
                OptionChips(kinds, triggerKind, { it.icon() }, { stringResource(it.shortTitle()) }) { triggerKind = it }
                if (triggerKind.usesDevice) {
                    FieldLabel(stringResource(R.string.automation_device))
                    DeviceChooser(devices, triggerDevice) { if (it.id != triggerDevice) sensorValue = ""; triggerDevice = it.id; triggerChannel = 0 }
                    if (triggerKind.usesChannel && src?.isMultiChannel == true) {
                        FieldLabel(stringResource(R.string.automation_channel))
                        ChannelChooser(src, triggerChannel) { triggerChannel = it }
                    }
                }
                if (triggerKind.usesSensor && sensorChoices.isNotEmpty()) {
                    FieldLabel(stringResource(R.string.sensor_choose))
                    OptionChips(sensorChoices, sensorKind ?: sensorChoices.first(), { it.icon() }, { stringResource(it.label()) }) { if (it != sensorKind) { sensorKind = it; sensorValue = "" } }
                    val kind = sensorKind
                    if (triggerKind.usesSensorValue && kind != null) {
                        NumberField(sensorValue, stringResource(R.string.sensor_threshold), kind.unit, allowNegative = kind == SensorKind.TEMPERATURE) { sensorValue = it }
                    }
                    val current = kind?.let { src?.sensors?.get(it) }
                    if (kind != null && current != null) {
                        Text(
                            stringResource(
                                R.string.automation_current_sensor,
                                if (kind.binary) stringResource(kind.stateLabel(current >= 0.5)) else formatSensor(kind, current),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
                if (triggerKind.isClock) {
                    val context = LocalContext.current
                    val title = stringResource(R.string.rule_clock_time)
                    TimeChip(title, formatMinute(context, clockMinute), Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        timePicker.pick(title, clockMinute) { clockMinute = it }
                    }
                    DayChips(clockDays) { clockDays = it }
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
                if (triggerKind.usesCycle) CyclePick(cycleList, triggerCycle) { triggerCycle = it }
                if (triggerKind.usesZone) {
                    FieldLabel(stringResource(R.string.zone_choose))
                    if (zones.isEmpty()) {
                        Text(stringResource(R.string.zone_none), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        OptionGrid(zones.map { it.id }, zoneId, { Icons.Rounded.Place }, { id -> zones.first { it.id == id }.name }) { zoneId = it }
                    }
                    TextButton(modifier = Modifier.padding(top = 4.dp).tvFocusRing(), onClick = { zoneEditor = true }) {
                        Icon(Icons.Rounded.Add, null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.zone_add))
                    }
                    ZonePermissionCard(Modifier.padding(top = 8.dp))
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
            val actionContent: @Composable ColumnScope.() -> Unit = {
                FieldLabel(stringResource(R.string.automation_action))
                OptionGrid(tab.actions, actionKind, { it.icon() }, { stringResource(it.editorTitle()) }, hue = { it.hue() }) { actionKind = it }
                if (actionKind.usesCycle) CyclePick(cycleList, actionCycle) { actionCycle = it }
                if (actionKind == ActionKind.SAVE_STATE) {
                    if (!snapshotTargetValid(saveScope, actionTargets, sceneList)) saveScope = snapshotDefault(actionTargets)
                    SnapshotScope(actionTargets, sceneList, saveScope) { saveScope = it }
                }
                if (actionKind == ActionKind.RESTORE_STATE) {
                    Text(stringResource(R.string.snap_restore_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp))
                }
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
                    DeviceChooser(actionTargets, actionDevice) { actionDevice = it.id; actionChannel = 0 }
                    if (dst?.isMultiChannel == true) {
                        FieldLabel(stringResource(R.string.automation_channel))
                        ChannelChooser(dst, actionChannel) { actionChannel = it }
                    }
                }
                        }
            var sheet by rememberSaveable { mutableStateOf<String?>(null) }
            StepCard(Icons.Rounded.Sensors, Hue.TEAL, stringResource(R.string.auto_if), stringResource(R.string.automation_when)) {
                if (tv) {
                    // Kumandada: hangi tetikleyici düzenleniyor (birden çoksa) ve alanları yerinde.
                    if (allTriggers.size > 1) {
                        OptionChips(allTriggers.indices.toList(), editingTrigger, { Icons.Rounded.Bolt }, { stringResource(R.string.cond_trigger_n, it + 1) }) { editTrigger(it) }
                    }
                    triggerContent()
                } else {
                    allTriggers.forEachIndexed { i, t ->
                        if (i > 0) Text(stringResource(R.string.cond_join_or), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp))
                        val text = if (src != null || !t.kind.usesDevice || i != editingTrigger) triggerText(t, devices) else stringResource(R.string.rule_tap_to_change)
                        ChoiceSummary(text, t.kind.icon(), Brand.amber) { editTrigger(i); sheet = "trigger" }
                    }
                }
                TextButton(modifier = Modifier.padding(top = 4.dp).tvFocusRing(), onClick = { addTrigger(); if (!tv) sheet = "trigger" }) {
                    Icon(Icons.Rounded.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.rule_add_trigger))
                }
                if (tv && allTriggers.size > 1) {
                    TextButton(modifier = Modifier.tvFocusRing(), onClick = { removeTrigger(editingTrigger) }) {
                        Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.rule_trigger_remove), color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            // ------------------------------------------------ Koşullar (isteğe bağlı)
            StepCard(Icons.Rounded.Tune, Hue.PURPLE, stringResource(R.string.auto_only), stringResource(R.string.rule_conditions_optional)) {
                var openCond by rememberSaveable { mutableStateOf<String?>(null) }
                fun toggle(k: String) { openCond = if (openCond == k) null else k }
                val context = LocalContext.current
                // Bekleme (işlemden önce)
                CondRow(Icons.Rounded.HourglassTop, stringResource(R.string.rule_cond_delay),
                    if (delay == 0) stringResource(R.string.automation_now) else holdLabel(delay), openCond == "delay") { toggle("delay") }
                if (openCond == "delay") {
                    Stepper(stringResource(R.string.automation_delay_short), delay, DELAY_STEPS, zeroLabel = stringResource(R.string.automation_now)) { delay = it }
                }
                // Saat aralığı: başlangıç = bitiş "tüm gün" demektir (yalnız günler seçiliyse böyle saklanır).
                val timed = windowOn && windowStart != windowEnd
                CondRow(Icons.Rounded.Schedule, stringResource(R.string.rule_cond_time),
                    if (!timed) stringResource(R.string.rule_any_time)
                    else stringResource(R.string.time_range, formatMinute(context, windowStart), formatMinute(context, windowEnd)),
                    openCond == "time") { toggle("time") }
                if (openCond == "time") {
                    ToggleLine(stringResource(R.string.window_toggle), timed) { on ->
                        if (on) {
                            windowOn = true
                            if (windowStart == windowEnd) { windowStart = 22 * 60; windowEnd = 6 * 60 }
                        } else if (windowDays == ActiveWindow.ALL_DAYS) {
                            windowOn = false
                        } else {
                            windowStart = 0; windowEnd = 0
                        }
                    }
                    if (timed) {
                        val startTitle = stringResource(R.string.window_start)
                        val endTitle = stringResource(R.string.window_end)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                            TimeChip(startTitle, formatMinute(context, windowStart), Modifier.weight(1f)) { timePicker.pick(startTitle, windowStart) { windowStart = it } }
                            TimeChip(endTitle, formatMinute(context, windowEnd), Modifier.weight(1f)) { timePicker.pick(endTitle, windowEnd) { windowEnd = it } }
                        }
                    }
                }
                // Günler
                val days = if (windowOn) windowDays else ActiveWindow.ALL_DAYS
                CondRow(Icons.Rounded.CalendarMonth, stringResource(R.string.rule_cond_days),
                    if (days == ActiveWindow.ALL_DAYS) stringResource(R.string.days_every) else daysLabel((0..6).filter { days and (1 shl it) != 0 }.map { (it + 1) % 7 }.toSet()), openCond == "days") { toggle("days") }
                if (openCond == "days") {
                    DayChips(days) { d ->
                        if (!windowOn) { windowOn = true; windowStart = 0; windowEnd = 0 }
                        windowDays = d
                        if (d == ActiveWindow.ALL_DAYS && windowStart == windowEnd) windowOn = false
                    }
                }
                // Ek koşullar
                CondRow(Icons.Rounded.Rule, stringResource(R.string.rule_cond_extra),
                    if (conds.isEmpty()) stringResource(R.string.rule_cond_none) else stringResource(R.string.rule_conds_count, conds.size),
                    openCond == "extra") { toggle("extra") }
                if (openCond == "extra") {
                fun newCond() = CondDraft(ConditionKind.IS_ON, targets.firstOrNull { !it.sensorOnly }?.id ?: targets.first().id)
                ToggleLine(stringResource(R.string.cond_toggle), conds.isNotEmpty()) { conds = if (it) listOf(newCond()) else emptyList() }
                if (conds.isNotEmpty()) {
                    CondListEditor(conds, condMatch, groupMatch, targets, locationVersion, allTriggers.size) { d, m, g ->
                        conds = d; condMatch = m; groupMatchJson = com.macerce.switchguard.core.CondSet.groupMatchToJson(g).toString()
                    }
                }
                            }
                // Evde / dışarıda
                CondRow(Icons.Rounded.Home, stringResource(R.string.rule_cond_presence),
                    stringResource(
                        when (presenceRule) {
                            null -> R.string.rule_presence_any
                            PresenceRule.AWAY -> R.string.presence_away
                            else -> R.string.presence_home
                        }
                    ),
                    openCond == "presence") { toggle("presence") }
                if (openCond == "presence") {
                ToggleLine(stringResource(R.string.presence_toggle), presenceRule != null) { presenceRule = if (it) PresenceRule.AWAY else null }
                presenceRule?.let { rule ->
                    OptionGrid(PresenceRule.entries, rule, { if (it == PresenceRule.AWAY) Icons.AutoMirrored.Rounded.DirectionsWalk else Icons.Rounded.Home },
                        { stringResource(if (it == PresenceRule.AWAY) R.string.presence_away else R.string.presence_home) }) { presenceRule = it }
                }
                }
            }

            // ------------------------------------------------ İse
            StepCard(Icons.Rounded.Bolt, Hue.BLUE, stringResource(R.string.auto_then), stringResource(R.string.automation_then)) {
                if (tv) actionContent()
                else ChoiceSummary(
                    actionText(draft.action, devices),
                    actionKind.icon(),
                    if (actionKind == ActionKind.ALARM) Brand.alarm else MaterialTheme.colorScheme.primary,
                ) { sheet = "action" }
                if (!alert) {
                    extras.forEachIndexed { i, x ->
                        if (tv) {
                            // Kumandada adım yerinde düzenlenir; iç adımlara bu kutunun içinde geçilir.
                            var open by rememberSaveable(i) { mutableStateOf(i.toString()) }
                            val path = StepTree.Path.parse(open)?.takeIf { it.top == i && StepTree.get(extras, it) != null } ?: StepTree.Path(i)
                            StepTree.get(extras, path)?.let { step ->
                                StepEditor(path, step, stepCtx, name,
                                    onChange = { n -> setExtras(StepTree.update(extras, path) { n }) },
                                    onDelete = { setExtras(StepTree.remove(extras, path)); open = (path.parent() ?: StepTree.Path(i)).toString() },
                                    onOpen = { open = it.toString() },
                                    onAdd = { b -> val (l, np) = StepTree.append(extras, path, b, AutoAction(ActionKind.TURN_OFF, actionTargets.firstOrNull()?.id.orEmpty())); setExtras(l); open = np.toString() },
                                    onMove = { d -> val (l, np) = StepTree.move(extras, path, d); setExtras(l); open = np.toString() })
                            }
                        } else {
                            val text = actionText(x, devices)
                            ChoiceSummary(
                                if (x.waitMinutes > 0) stringResource(R.string.rule_step_after, holdLabel(x.waitMinutes), text) else text,
                                x.kind.icon(),
                                MaterialTheme.colorScheme.primary,
                            ) { sheet = "step:$i" }
                        }
                    }
                    // Ek eylem: varsayılanı ilk hedefi kapatmak; alttan açılan sayfada değiştirilir.
                    TextButton(modifier = Modifier.padding(top = 4.dp).tvFocusRing(), onClick = {
                        extras.add(AutoAction(ActionKind.TURN_OFF, actionTargets.firstOrNull()?.id.orEmpty()))
                        if (!tv) sheet = "step:" + (extras.size - 1)
                    }) {
                        Icon(Icons.Rounded.Add, null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.rule_add_action))
                    }
                }
            }


            // ------------------------------------------------ Kural seçenekleri
            // Adımların altında değil ayrı kartta: "Eylem ekle" adımların hemen altında kalsın.
            if (!alert) StepCard(Icons.Rounded.Settings, Hue.SLATE, stringResource(R.string.rule_options_tag), stringResource(R.string.rule_options)) {
                // Döngü koruması: bir saatte çok kez çalışırsa cihazı kapalı bırak, alarm ver, kuralı durdur.
                ToggleLine(stringResource(R.string.loop_guard_toggle), loopGuard > 0) {
                    loopGuard = if (it) AutomationEngine.LOOP_GUARD_DEFAULT else 0
                }
                if (loopGuard > 0) {
                    Stepper(stringResource(R.string.loop_guard_times), loopGuard, (2..10).toList(),
                        format = { n -> stringResource(R.string.loop_guard_n, n) }) { loopGuard = it }
                    Text(stringResource(R.string.loop_guard_hint), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp))
                }
                // Yeniden tetiklenince ne olacağı (bekleyen adımı olan kurallarda anlamlı).
                RunModeBlock(runMode, maxRuns) { m, n -> runMode = m; maxRuns = n }
            }

            if (sheet != null && !tv) {
                ModalBottomSheet(
                    onDismissRequest = { sheet = null },
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ) {
                    Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
                        Text(
                            stringResource(if (sheet == "trigger") R.string.rule_triggers else R.string.rule_actions),
                            style = MaterialTheme.typography.titleLarge,
                        )
                        val s = sheet.orEmpty()
                        when {
                            s == "trigger" -> {
                                if (allTriggers.size > 1) Text(stringResource(R.string.cond_trigger_n, editingTrigger + 1), style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 4.dp))
                                triggerContent()
                                if (allTriggers.size > 1) {
                                    TextButton(modifier = Modifier.padding(top = 8.dp).tvFocusRing(), onClick = { removeTrigger(editingTrigger); sheet = null }) {
                                        Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error)
                                        Spacer(Modifier.width(8.dp))
                                        Text(stringResource(R.string.rule_trigger_remove), color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                            s.startsWith("step:") -> {
                                val path = StepTree.Path.parse(s.removePrefix("step:"))
                                val x = path?.let { StepTree.get(extras, it) }
                                if (path != null && x != null) {
                                    StepEditor(path, x, stepCtx, name,
                                        onChange = { n -> setExtras(StepTree.update(extras, path) { n }) },
                                        onDelete = { setExtras(StepTree.remove(extras, path)); sheet = path.parent()?.let { "step:$it" } },
                                        onOpen = { sheet = "step:$it" },
                                        onAdd = { b ->
                                            val (l, np) = StepTree.append(extras, path, b, AutoAction(ActionKind.TURN_OFF, actionTargets.firstOrNull()?.id.orEmpty()))
                                            setExtras(l); sheet = "step:$np"
                                        },
                                        onMove = { d -> val (l, np) = StepTree.move(extras, path, d); setExtras(l); sheet = "step:$np" })
                                }
                            }
                            else -> actionContent()
                        }
                        PillButton(stringResource(R.string.action_done), onClick = { sheet = null }, modifier = Modifier.fillMaxWidth().padding(top = 20.dp))
                    }
                }
            }

            // ------------------------------------------------ Özet
            if (tv && (src != null || !triggerKind.usesDevice)) {
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

            PillButton(
                stringResource(R.string.action_save),
                onClick = { onSave(draft.copy(id = existing?.id ?: UUID.randomUUID().toString())) },
                enabled = valid,
                icon = Icons.Rounded.Check,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }

    if (zoneEditor) {
        ZoneEditorDialog(
            initial = null,
            onSaved = { zoneId = it.id; zonesVersion++; zoneEditor = false },
            onDismiss = { zoneEditor = false },
        )
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
internal val DELAY_STEPS = listOf(0, 1, 2, 3, 5, 10, 15, 20, 30, 45, 60, 90, 120)
private val LOW_STEPS = listOf(0, 1, 2, 3, 5, 10, 15, 20, 30)
private val GRACE_STEPS = listOf(0, 1, 2, 3, 5, 10, 15, 20, 30)

/** Düzenleyicinin bir adımı: renkli simge, büyük başlık (Eğer / O zaman), altında alanlar. */
@Composable
internal fun StepCard(icon: ImageVector, hue: Hue, title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(20.dp)).background(tileColor()).padding(16.dp),
    ) {
        // Videodaki "EĞER" / "İSE" rozetleri: renkli hap + yanında kısa açıklama.
        Row(verticalAlignment = Alignment.CenterVertically) {
            PillBadge(title, if (LocalDarkTheme.current) hue.dark else hue.light)
            Spacer(Modifier.width(10.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
        }
        content()
    }
}

@Composable
internal fun FieldLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
    )
}

/** Cihaz seçimi: yatay kaydırılan, simgeli kartlar. */
@Composable
internal fun DeviceChooser(devices: List<DeviceSnapshot>, selectedId: String, onPick: (DeviceSnapshot) -> Unit) {
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
internal fun ChannelChooser(device: DeviceSnapshot, selected: Int, onPick: (Int) -> Unit) {
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

/** Otomasyonda döngü seçimi; hiç döngü yoksa nereden ekleneceği söylenir. */
@Composable
internal fun CyclePick(cycles: List<com.macerce.switchguard.core.Cycle>, selected: String, onPick: (String) -> Unit) {
    FieldLabel(stringResource(R.string.cycle_choose))
    if (cycles.isEmpty()) {
        Text(stringResource(R.string.cycle_none_yet), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    } else {
        OptionChips(cycles.map { it.id }, selected, { Icons.Rounded.Repeat }, { id -> cycles.first { it.id == id }.name }, onPick)
    }
}

/** Sıkışık seçenek kutucukları: simge + kısa ad yan yana, satıra sığdığı kadar (çok seçenekli "Eğer" listesi için). */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun <T> OptionChips(
    options: List<T>,
    selected: T,
    icon: (T) -> ImageVector,
    label: @Composable (T) -> String,
    onPick: (T) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        options.forEach { o ->
            val on = o == selected
            val shape = CircleShape
            Row(
                Modifier.heightIn(min = 38.dp)
                    .tvFocusRing(shape)
                    .clip(shape)
                    .background(if (on) scheme.primary.copy(alpha = 0.22f) else raisedOnTile())
                    .border(1.5.dp, if (on) scheme.primary else scheme.outlineVariant, shape)
                    .selectable(selected = on, onClick = { onPick(o) }, role = Role.RadioButton)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(icon(o), null, tint = if (on) scheme.primary else scheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    label(o),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (on) scheme.onSurface else scheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

/** Seçenek kutucukları (iki sütun): simge + kısa ad; seçili olan dolu. */
@Composable
internal fun <T> OptionGrid(
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
internal fun Stepper(
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
internal fun NumberField(value: String, label: String, suffix: String, allowNegative: Boolean = false, onChange: (String) -> Unit) {
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
internal fun ToggleLine(text: String, checked: Boolean, onChange: (Boolean) -> Unit) {
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
internal fun TimeChip(label: String, value: String, modifier: Modifier, onClick: () -> Unit) {
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
internal fun DayChips(days: Int, onChange: (Int) -> Unit) {
    val names = stringArrayResource(R.array.day_short)
    val scheme = MaterialTheme.colorScheme
    // Videodaki gibi yuvarlak gün daireleri; dar ekranda daireler küçülür.
    Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        names.forEachIndexed { i, n ->
            val on = days and (1 shl i) != 0
            val shape = CircleShape
            Box(
                Modifier.weight(1f).aspectRatio(1f).widthIn(max = 44.dp).clip(shape)
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

internal fun ConditionKind.icon(): ImageVector = when (this) {
    ConditionKind.IS_ON -> Icons.Rounded.PowerSettingsNew
    ConditionKind.IS_OFF -> Icons.Rounded.PowerOff
    ConditionKind.IS_ONLINE -> Icons.Rounded.Cloud
    ConditionKind.IS_OFFLINE -> Icons.Rounded.CloudOff
    ConditionKind.POWER_ABOVE -> Icons.AutoMirrored.Rounded.TrendingUp
    ConditionKind.POWER_BELOW -> Icons.AutoMirrored.Rounded.TrendingDown
    ConditionKind.SENSOR_ABOVE -> Icons.Rounded.Thermostat
    ConditionKind.SENSOR_BELOW -> Icons.Rounded.AcUnit
    ConditionKind.SENSOR_ACTIVE -> Icons.Rounded.Sensors
    ConditionKind.SENSOR_CLEAR -> Icons.Rounded.SensorsOff
    ConditionKind.TEMP_ABOVE -> Icons.Rounded.WbSunny
    ConditionKind.TEMP_BELOW -> Icons.Rounded.AcUnit
    ConditionKind.ON_FOR -> Icons.Rounded.Timer
    ConditionKind.OFF_FOR -> Icons.Rounded.TimerOff
    ConditionKind.TIME_BETWEEN -> Icons.Rounded.Schedule
    ConditionKind.DARK -> Icons.Rounded.NightsStay
    ConditionKind.LIGHT -> Icons.Rounded.WbSunny
    ConditionKind.HOME -> Icons.Rounded.Home
    ConditionKind.AWAY -> Icons.AutoMirrored.Rounded.DirectionsWalk
    ConditionKind.TRIGGERED_BY -> Icons.Rounded.Bolt
}

/** Düzenleyicideki bir koşul; eşik yazıldığı gibi tutulur. */
internal data class CondDraft(
    val kind: ConditionKind,
    val deviceId: String,
    val channel: Int = 0,
    val value: String = "",
    val sensor: SensorKind? = null,
    val negate: Boolean = false,
    val group: Int = 0,
    val start: Int = 22 * 60,
    val end: Int = 6 * 60,
    val days: Int = ActiveWindow.ALL_DAYS,
) {
    fun number(): Double? = value.replace(',', '.').toDoubleOrNull()
    fun toCondition() = Condition(
        kind, if (kind.usesDevice) deviceId else "", if (kind.usesChannel || kind.isTrigger) channel else 0,
        if (kind.usesValue) number() ?: 0.0 else 0.0, if (kind.usesSensor) sensor else null,
        negate = negate, group = group,
        start = if (kind.isTime) start else 0, end = if (kind.isTime) end else 0, days = if (kind.isTime) days else ActiveWindow.ALL_DAYS,
    )

    companion object {
        fun of(c: Condition) = CondDraft(
            c.kind, c.deviceId, c.channel, if (c.kind.usesValue) formatNumber(c.value) else "", c.sensor, c.negate, c.group,
            if (c.kind.isTime) c.start else 22 * 60, if (c.kind.isTime) c.end else 6 * 60, c.days,
        )

        fun encode(d: CondDraft): String = org.json.JSONObject().put("k", d.kind.name).put("d", d.deviceId).put("c", d.channel).put("v", d.value)
            .put("s", d.sensor?.name ?: "").put("n", d.negate).put("g", d.group).put("a", d.start).put("b", d.end).put("y", d.days).toString()

        fun decode(s: String): CondDraft {
            val o = org.json.JSONObject(s)
            return CondDraft(ConditionKind.valueOf(o.getString("k")), o.getString("d"), o.getInt("c"), o.getString("v"),
                o.getString("s").takeIf { it.isNotEmpty() }?.let { SensorKind.valueOf(it) }, o.optBoolean("n"), o.optInt("g"),
                o.optInt("a", 22 * 60), o.optInt("b", 6 * 60), o.optInt("y", ActiveWindow.ALL_DAYS))
        }

        val ListSaver = androidx.compose.runtime.saveable.listSaver<List<CondDraft>, String>(
            save = { list -> list.map(::encode) },
            restore = { list -> list.map(::decode) },
        )
    }
}

/** Seçili cihazda anlamlı koşul türleri; cihaz dışı olanlar (sıcaklık, saat, güneş, evde) her zaman. */
internal fun conditionKinds(d: DeviceSnapshot?, triggerCount: Int = 1): List<ConditionKind> = ConditionKind.entries.filter {
    when {
        it.isTrigger -> triggerCount > 1
        it.deviceless -> true
        d == null -> !it.usesPower && !it.usesSensor
        it.usesChannel -> !d.sensorOnly && d.switches.isNotEmpty()
        it.usesPower -> d.hasEnergy
        it.usesSensorValue -> d.sensors.keys.any { k -> !k.binary }
        it.usesSensorState -> d.sensors.keys.any { k -> k.binary }
        else -> true
    }
}

/** Koşul hatasız mı (cihaz var, eşik yazılı, sensör seçili). */
internal fun CondDraft.valid(targets: List<DeviceSnapshot>): Boolean =
    (!kind.usesDevice || targets.any { it.id == deviceId }) && (!kind.isTime || days != 0) && (!kind.usesValue || number() != null) && (!kind.usesSensor || sensor != null)

/** Bir koşulun düzenleme kutusu: cihaz, durum, kanal/sensör, eşik. */
@Composable
internal fun ConditionBlock(
    index: Int,
    raw: CondDraft,
    targets: List<DeviceSnapshot>,
    locationVersion: Int,
    onChange: (CondDraft) -> Unit,
    onRemove: () -> Unit,
    triggerCount: Int = 1,
    /** Grup seçimi sunulsun mu (koşul 2'den fazlaysa anlamlı). */
    groups: Boolean = false,
) {
    val d = targets.firstOrNull { it.id == raw.deviceId }
    val kinds = conditionKinds(d, triggerCount)
    // Cihaz değişince seçili tür artık uymuyorsa düzeltilir (sensör cihazında önce sensör eşiği); sensör türü de uyacak
    // şekilde seçilir, boş eşik şu anki değerle dolar. Düzeltme aynı karede gösterilir, kayda SideEffect ile yazılır.
    var draft = raw
    if (draft.kind !in kinds) {
        draft = draft.copy(kind = kinds.firstOrNull { it.usesSensorValue } ?: kinds.firstOrNull { it.usesSensorState } ?: kinds.first(), value = "", sensor = null)
    }
    val sensorChoices = d?.sensors?.keys?.filter { draft.kind.usesSensor && it.binary == draft.kind.usesSensorState }.orEmpty()
    if (draft.kind.usesSensor && draft.sensor !in sensorChoices) draft = draft.copy(sensor = sensorChoices.firstOrNull(), value = "")
    if (draft.kind.usesValue && draft.value.isEmpty()) {
        val now = when {
            draft.kind.usesPower -> d?.power
            draft.kind.usesSensorValue -> draft.sensor?.let { d?.sensors?.get(it) }
            else -> null
        }
        (now?.let { formatNumber(Math.round(it).toDouble()) } ?: if (draft.kind.usesTemp) "20" else if (draft.kind.usesDuration) "10" else null)?.let { draft = draft.copy(value = it) }
    }
    if (draft != raw) { val fixed = draft; SideEffect { onChange(fixed) } }
    val shape = RoundedCornerShape(18.dp)
    Column(Modifier.fillMaxWidth().padding(top = 10.dp).clip(shape).border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape).padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.cond_n, index + 1), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            IconButton(modifier = Modifier.tvFocusRing(), onClick = onRemove) { Icon(Icons.Rounded.Delete, stringResource(R.string.cond_remove)) }
        }
        // Cihaz dışı koşulda seçili cihaz yok; cihaz seçilince cihaz koşuluna geçilir.
        FieldLabel(stringResource(R.string.automation_device))
        DeviceChooser(targets, if (draft.kind.deviceless) "" else draft.deviceId) {
            onChange(draft.copy(deviceId = it.id, channel = 0, value = "", sensor = null, kind = if (draft.kind.deviceless) ConditionKind.IS_ON else draft.kind))
        }
        FieldLabel(stringResource(R.string.cond_state))
        OptionChips(kinds, draft.kind, { it.icon() }, { stringResource(it.title()) }) { if (it != draft.kind) onChange(draft.copy(kind = it, value = "")) }
        if (draft.kind.usesChannel && d?.isMultiChannel == true) {
            FieldLabel(stringResource(R.string.automation_channel))
            ChannelChooser(d, draft.channel) { onChange(draft.copy(channel = it)) }
        }
        if (draft.kind.usesSensor && sensorChoices.size > 1) {
            FieldLabel(stringResource(R.string.sensor_choose))
            OptionChips(sensorChoices, draft.sensor ?: sensorChoices.first(), { it.icon() }, { stringResource(it.label()) }) {
                if (it != draft.sensor) onChange(draft.copy(sensor = it, value = ""))
            }
        }
        when {
            draft.kind.usesPower -> NumberField(draft.value, stringResource(R.string.automation_watts), "W") { onChange(draft.copy(value = it)) }
            draft.kind.usesSensorValue -> draft.sensor?.let { k ->
                NumberField(draft.value, stringResource(R.string.sensor_threshold), k.unit, allowNegative = k == SensorKind.TEMPERATURE) { onChange(draft.copy(value = it)) }
            }
            draft.kind.usesTemp -> NumberField(draft.value, stringResource(R.string.automation_temp), "°C", allowNegative = true) { onChange(draft.copy(value = it)) }
            draft.kind.usesDuration -> NumberField(draft.value, stringResource(R.string.cond_minutes), "") { onChange(draft.copy(value = it)) }
        }
        if (draft.kind.isTime) {
            val context = LocalContext.current
            val picker = rememberTimePicker()
            val startTitle = stringResource(R.string.window_start)
            val endTitle = stringResource(R.string.window_end)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                TimeChip(startTitle, formatMinute(context, draft.start), Modifier.weight(1f)) { picker.pick(startTitle, draft.start) { onChange(draft.copy(start = it)) } }
                TimeChip(endTitle, formatMinute(context, draft.end), Modifier.weight(1f)) { picker.pick(endTitle, draft.end) { onChange(draft.copy(end = it)) } }
            }
            DayChips(draft.days) { onChange(draft.copy(days = it)) }
        }
        if (draft.kind.isTrigger) {
            FieldLabel(stringResource(R.string.cond_triggered_by))
            OptionChips((0 until triggerCount).toList(), draft.channel.coerceIn(0, (triggerCount - 1).coerceAtLeast(0)), { Icons.Rounded.Bolt },
                { stringResource(R.string.cond_trigger_n, it + 1) }) { onChange(draft.copy(channel = it)) }
        }
        ToggleLine(stringResource(R.string.cond_not), draft.negate) { onChange(draft.copy(negate = it)) }
        if (groups) {
            FieldLabel(stringResource(R.string.cond_group))
            OptionChips(listOf(0, 1, 2, 3), draft.group, { if (it == 0) Icons.Rounded.Remove else Icons.Rounded.Rule },
                { if (it == 0) stringResource(R.string.cond_group_none) else stringResource(R.string.cond_group_n, it) }) { onChange(draft.copy(group = it)) }
        }
        val current: String? = when {
            draft.kind.usesPower -> d?.power?.let { stringResource(R.string.automation_current_power, formatWatts(it)) }
            draft.kind.usesSensor -> draft.sensor?.let { k -> d?.sensors?.get(k)?.let { v ->
                stringResource(R.string.automation_current_sensor, if (k.binary) stringResource(k.stateLabel(v >= 0.5)) else formatSensor(k, v))
            } }
            draft.kind.usesTemp -> runtimeTemp(locationVersion)?.let { stringResource(R.string.automation_current_temp, formatTemp(it)) }
            else -> null
        }
        if (current != null) {
            Text(current, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

/** Özet ve kart metni: "Salon sensörü sıcaklık < 20 °C", "Kombi açık", "Dış sıcaklık > 25 °C". */
@Composable
internal fun conditionText(c: Condition, devices: List<DeviceSnapshot>): String {
    val plain = conditionTextPlain(c, devices)
    return if (c.negate) stringResource(R.string.cond_not_text, plain) else plain
}

@Composable
private fun conditionTextPlain(c: Condition, devices: List<DeviceSnapshot>): String {
    when {
        c.kind.isTime -> return windowText(ActiveWindow(c.start, c.end, c.days))
        c.kind.isTrigger -> return stringResource(R.string.cond_trigger_n, c.channel + 1)
        c.kind.isSun || c.kind.isPresence -> return stringResource(c.kind.title())
    }
    val name = if (c.kind.usesTemp) null else devices.firstOrNull { it.id == c.deviceId }?.let { channelLabel(it, c.channel, c.kind.usesChannel) }
        ?: stringResource(R.string.device_missing)
    val cmp = if (c.kind.above) " > " else " < "
    val state = when {
        c.kind.usesPower -> stringResource(R.string.cond_power) + cmp + formatWatts(c.value)
        c.kind.usesSensorValue -> c.sensor?.let { stringResource(it.label()) + cmp + formatSensor(it, c.value) } ?: stringResource(c.kind.title())
        c.kind.usesSensorState -> c.sensor?.let { stringResource(it.stateLabel(c.kind == ConditionKind.SENSOR_ACTIVE)) } ?: stringResource(c.kind.title())
        c.kind.usesTemp -> stringResource(R.string.cond_outdoor) + cmp + formatTemp(c.value)
        c.kind == ConditionKind.ON_FOR -> stringResource(R.string.cond_on_for_text, holdLabel(c.value.toInt()))
        c.kind == ConditionKind.OFF_FOR -> stringResource(R.string.cond_off_for_text, holdLabel(c.value.toInt()))
        else -> stringResource(c.kind.title())
    }
    return when {
        name == null -> stringResource(R.string.cond_clause_plain, state)
        c.kind == ConditionKind.IS_ON || c.kind == ConditionKind.IS_OFF || c.kind == ConditionKind.IS_ONLINE || c.kind == ConditionKind.IS_OFFLINE -> stringResource(R.string.cond_clause, name, state)
        else -> stringResource(R.string.cond_clause_value, name, state)
    }
}

/** Birden çok koşul "ve" / "veya" ile tek cümlede. */
@Composable
internal fun conditionsText(list: List<Condition>, match: ConditionMatch, devices: List<DeviceSnapshot>, groupMatch: Map<Int, ConditionMatch> = emptyMap()): String {
    val and = " " + stringResource(R.string.cond_join_and) + " "
    val or = " " + stringResource(R.string.cond_join_or) + " "
    val join = if (match == ConditionMatch.ANY) or else and
    // Gruplar parantez içinde, kendi ve/veya bağlacıyla: "A ve (B veya C)".
    val texts = list.map { conditionText(it, devices) }
    val top = list.indices.filter { list[it].group <= 0 }.map { texts[it] }
    val groups = list.indices.filter { list[it].group > 0 }.groupBy { list[it].group }.map { (g, idx) ->
        val j = if ((groupMatch[g] ?: ConditionMatch.ALL) == ConditionMatch.ANY) or else and
        val inner = idx.joinToString(j) { texts[it] }
        if (idx.size > 1) "($inner)" else inner
    }
    return (top + groups).joinToString(join)
}

private fun TriggerKind.icon(): ImageVector = when (this) {
    TriggerKind.TIME -> Icons.Rounded.Schedule
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
    TriggerKind.ZONE_ENTER -> Icons.AutoMirrored.Rounded.Login
    TriggerKind.ZONE_EXIT -> Icons.AutoMirrored.Rounded.Logout
    TriggerKind.CYCLE_STARTED -> Icons.Rounded.Repeat
    TriggerKind.CYCLE_FINISHED -> Icons.Rounded.Stop
    TriggerKind.SENSOR_ABOVE -> Icons.Rounded.Thermostat
    TriggerKind.SENSOR_BELOW -> Icons.Rounded.AcUnit
    TriggerKind.SENSOR_ACTIVE -> Icons.Rounded.Sensors
    TriggerKind.SENSOR_CLEAR -> Icons.Rounded.SensorsOff
}

/** Kutucuklardaki kısa olay adları ("Açık kalınca"). */
fun TriggerKind.shortTitle(): Int = when (this) {
    TriggerKind.TIME -> R.string.evt_time
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
    TriggerKind.ZONE_ENTER -> R.string.evt_zone_enter
    TriggerKind.ZONE_EXIT -> R.string.evt_zone_exit
    TriggerKind.CYCLE_STARTED -> R.string.evt_cycle_started
    TriggerKind.CYCLE_FINISHED -> R.string.evt_cycle_finished
    TriggerKind.SENSOR_ABOVE -> R.string.evt_sensor_above
    TriggerKind.SENSOR_BELOW -> R.string.evt_sensor_below
    TriggerKind.SENSOR_ACTIVE -> R.string.evt_sensor_active
    TriggerKind.SENSOR_CLEAR -> R.string.evt_sensor_clear
}

internal fun ActionKind.icon(): ImageVector = when (this) {
    ActionKind.ALARM -> Icons.Rounded.Alarm
    ActionKind.NOTIFY -> Icons.Rounded.NotificationsActive
    ActionKind.TURN_ON -> Icons.Rounded.PowerSettingsNew
    ActionKind.TURN_OFF -> Icons.Rounded.PowerOff
    ActionKind.RUN_SCENE -> Icons.Rounded.PlayArrow
    ActionKind.START_CYCLE -> Icons.Rounded.Repeat
    ActionKind.STOP_CYCLE -> Icons.Rounded.Stop
    ActionKind.SAVE_STATE -> Icons.Rounded.Bookmark
    ActionKind.RESTORE_STATE -> Icons.Rounded.Restore
    ActionKind.TOGGLE -> Icons.Rounded.SwapHoriz
    ActionKind.SET_LEVEL -> Icons.Rounded.Lightbulb
    ActionKind.DELAY -> Icons.Rounded.HourglassTop
    ActionKind.WAIT_UNTIL -> Icons.Rounded.Pending
    ActionKind.IF -> Icons.Rounded.AltRoute
    ActionKind.REPEAT -> Icons.Rounded.Loop
    ActionKind.STOP -> Icons.Rounded.Block
    ActionKind.RUN_AUTOMATION -> Icons.Rounded.AutoMode
    ActionKind.SEND -> Icons.AutoMirrored.Rounded.Send
    ActionKind.COUNTER_ADD, ActionKind.COUNTER_SET -> Icons.Rounded.Numbers
}

internal fun ActionKind.hue(): Hue = when (this) {
    ActionKind.ALARM -> Hue.RED
    ActionKind.NOTIFY -> Hue.BLUE
    ActionKind.TURN_ON -> Hue.GREEN
    ActionKind.TURN_OFF -> Hue.SLATE
    ActionKind.RUN_SCENE -> Hue.PURPLE
    ActionKind.START_CYCLE -> Hue.TEAL
    ActionKind.STOP_CYCLE -> Hue.SLATE
    ActionKind.SAVE_STATE, ActionKind.RESTORE_STATE -> Hue.AMBER
    ActionKind.TOGGLE -> Hue.GREEN
    ActionKind.SET_LEVEL -> Hue.AMBER
    ActionKind.DELAY, ActionKind.WAIT_UNTIL -> Hue.SLATE
    ActionKind.IF, ActionKind.REPEAT, ActionKind.STOP, ActionKind.RUN_AUTOMATION -> Hue.INDIGO
    ActionKind.SEND -> Hue.BLUE
    ActionKind.COUNTER_ADD, ActionKind.COUNTER_SET -> Hue.ORANGE
}

internal fun ActionKind.editorTitle(): Int = when (this) {
    ActionKind.ALARM -> R.string.auto_action_alarm
    ActionKind.NOTIFY -> R.string.auto_action_notify
    ActionKind.TURN_ON -> R.string.act_turn_on
    ActionKind.TURN_OFF -> R.string.act_turn_off
    ActionKind.RUN_SCENE -> R.string.act_run_scene
    ActionKind.START_CYCLE -> R.string.act_start_cycle
    ActionKind.STOP_CYCLE -> R.string.act_stop_cycle
    ActionKind.SAVE_STATE -> R.string.act_save_state
    ActionKind.RESTORE_STATE -> R.string.act_restore_state
    else -> title()
}

/** "Durumu hatırla" hedefi geçerli mi: gerçek bir cihaz (grup/kategori değil) ya da var olan bir sahne. Boş = geçersiz. */
internal fun snapshotTargetValid(id: String, targets: List<DeviceSnapshot>, scenes: List<com.macerce.switchguard.core.Scene>): Boolean =
    id.isNotEmpty() && (scenes.any { it.id == id } || targets.any { it.id == id && !DeviceGroup.isGroup(it.id) && !DeviceCategory.isCategory(it.id) })

internal fun snapshotDefault(targets: List<DeviceSnapshot>): String =
    targets.firstOrNull { !DeviceGroup.isGroup(it.id) && !DeviceCategory.isCategory(it.id) }?.id.orEmpty()

/** "Durumu hatırla" hedefi: tek cihaz ya da (varsa) bir sahnenin cihazları. Tüm hesap hiçbir zaman seçilemez. */
@Composable
internal fun SnapshotScope(targets: List<DeviceSnapshot>, scenes: List<com.macerce.switchguard.core.Scene>, selected: String, onPick: (String) -> Unit) {
    val devices = remember(targets) { targets.filter { !DeviceGroup.isGroup(it.id) && !DeviceCategory.isCategory(it.id) } }
    FieldLabel(stringResource(R.string.automation_device))
    DeviceChooser(devices, selected) { onPick(it.id) }
    if (scenes.isNotEmpty()) {
        FieldLabel(stringResource(R.string.snap_or_scene))
        // Seçili bir cihazsa hiçbir sahne çipi seçili görünmez.
        OptionGrid(scenes.map { it.id }, selected, { Icons.Rounded.PlayArrow },
            { id: String -> scenes.firstOrNull { it.id == id }?.name.orEmpty() }) { onPick(it) }
    }
    Text(stringResource(R.string.snap_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 6.dp))
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

/** Seçili tetikleyici / eylem: renkli çerçeveli satır (videodaki "Banyo su sensörü · Etkin"); dokununca değiştirilir. */
@Composable
internal fun ChoiceSummary(text: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier.fillMaxWidth().padding(top = 12.dp)
            .tvFocusRing(shape)
            .clip(shape)
            .background(color.copy(alpha = 0.12f))
            .border(1.5.dp, color, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        Icon(Icons.Rounded.Edit, stringResource(R.string.action_change), tint = color, modifier = Modifier.size(20.dp))
    }
}

/** Koşul satırı: simge, ad, sağda şu anki değer ve ok; dokununca altında ayarı açılır. */
@Composable
private fun CondRow(icon: ImageVector, title: String, value: String, open: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp)
            .tvFocusRing(shape)
            .clip(shape)
            .background(raisedOnTile())
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(
            value,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 160.dp),
        )
        Icon(
            if (open) Icons.Rounded.KeyboardArrowUp else Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            null,
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}

/** Ek eylemlerin kaydedilebilir listesi (döndürmede kaybolmasın). */
private val extraActionsSaver = listSaver<androidx.compose.runtime.snapshots.SnapshotStateList<AutoAction>, String>(
    save = { list -> list.map { AutoAction.toJson(it).toString() } },
    restore = { saved ->
        mutableStateListOf<AutoAction>().apply {
            saved.forEach { line -> runCatching { add(AutoAction.fromJson(org.json.JSONObject(line))) } }
        }
    },
)

/** Tetikleyici listesinin kaydedicisi (döndürmede kaybolmasın). */
private val triggersSaver = listSaver<androidx.compose.runtime.snapshots.SnapshotStateList<Trigger>, String>(
    save = { list -> list.map { Automation.triggerToJson(it).toString() } },
    restore = { saved ->
        mutableStateListOf<Trigger>().apply {
            saved.forEach { line -> runCatching { add(Automation.triggerFromJson(org.json.JSONObject(line))) } }
        }
    },
)

/** Ek eylem kaydedilebilir mi: cihaz eyleminde hedef, sahnede/döngüde var olan kayıt. */
internal fun extraValid(x: AutoAction, targets: List<DeviceSnapshot>, scenes: List<com.macerce.switchguard.core.Scene>, cycles: List<com.macerce.switchguard.core.Cycle>): Boolean = when {
    x.kind.usesDevice -> targets.any { it.id == x.deviceId }
    x.kind == ActionKind.RUN_SCENE -> scenes.any { it.id == x.deviceId }
    x.kind.usesCycle -> cycles.any { it.id == x.deviceId }
    x.kind == ActionKind.SAVE_STATE -> snapshotTargetValid(x.deviceId, targets, scenes)
    else -> true
}

/** Bir ek eylemin düzenleyicisi: tür, hedef, önceki eylemden sonra bekleme ve silme. */
@Composable
private fun ExtraActionEditor(
    x: AutoAction,
    index: Int,
    tab: AutomationTab,
    targets: List<DeviceSnapshot>,
    scenes: List<com.macerce.switchguard.core.Scene>,
    cycles: List<com.macerce.switchguard.core.Cycle>,
    onChange: (AutoAction) -> Unit,
    onDelete: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(top = 12.dp)) {
        FieldLabel(stringResource(R.string.rule_extra_n, index + 2))
        OptionGrid(tab.actions, x.kind, { it.icon() }, { stringResource(it.editorTitle()) }, hue = { it.hue() }) { k ->
            val target = when {
                k.usesDevice -> x.deviceId.takeIf { id -> targets.any { it.id == id } } ?: targets.firstOrNull()?.id.orEmpty()
                k == ActionKind.RUN_SCENE -> scenes.firstOrNull()?.id.orEmpty()
                k == ActionKind.SAVE_STATE -> x.deviceId.takeIf { snapshotTargetValid(it, targets, scenes) } ?: snapshotDefault(targets)
                k.usesCycle -> cycles.firstOrNull()?.id.orEmpty()
                else -> ""
            }
            onChange(x.copy(kind = k, deviceId = target, channel = if (k.usesDevice) x.channel else 0))
        }
        if (x.kind.usesDevice) {
            FieldLabel(stringResource(R.string.automation_device))
            DeviceChooser(targets, x.deviceId) { onChange(x.copy(deviceId = it.id, channel = 0)) }
            targets.firstOrNull { it.id == x.deviceId }?.takeIf { it.isMultiChannel }?.let { d ->
                FieldLabel(stringResource(R.string.automation_channel))
                ChannelChooser(d, x.channel) { onChange(x.copy(channel = it)) }
            }
        }
        if (x.kind == ActionKind.RUN_SCENE) {
            FieldLabel(stringResource(R.string.scenes_title))
            if (scenes.isEmpty()) Text(stringResource(R.string.scene_none_yet), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            else OptionGrid(scenes.map { it.id }, x.deviceId, { Icons.Rounded.PlayArrow }, { id -> scenes.first { it.id == id }.name }) { onChange(x.copy(deviceId = it)) }
        }
        if (x.kind.usesCycle) CyclePick(cycles, x.deviceId) { onChange(x.copy(deviceId = it)) }
        if (x.kind == ActionKind.SAVE_STATE) SnapshotScope(targets, scenes, x.deviceId) { onChange(x.copy(deviceId = it)) }
        Stepper(stringResource(R.string.rule_action_wait), x.waitMinutes, DELAY_STEPS, zeroLabel = stringResource(R.string.automation_now)) { onChange(x.copy(waitMinutes = it)) }
        TextButton(modifier = Modifier.padding(top = 4.dp).tvFocusRing(), onClick = onDelete) {
            Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
        }
    }
}
