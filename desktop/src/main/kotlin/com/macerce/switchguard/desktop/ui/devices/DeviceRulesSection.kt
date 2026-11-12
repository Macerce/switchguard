package com.macerce.switchguard.desktop.ui.devices

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import com.macerce.switchguard.desktop.ui.automations.labelKey
import com.macerce.switchguard.desktop.ui.automations.icon
import com.macerce.switchguard.desktop.ui.automations.NumberField
import com.macerce.switchguard.core.SensorKind
import com.macerce.switchguard.core.SensorAlerts
import com.macerce.switchguard.core.DeviceSnapshot
import com.macerce.switchguard.core.Automation
import com.macerce.switchguard.core.ActionKind
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CloudQueue
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PowerOff
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.macerce.switchguard.core.AlertAction
import com.macerce.switchguard.core.Demo
import com.macerce.switchguard.core.DeviceCategory
import com.macerce.switchguard.core.DeviceRules
import com.macerce.switchguard.core.EventType
import com.macerce.switchguard.desktop.AppGraph
import com.macerce.switchguard.desktop.platform.Sounds
import com.macerce.switchguard.desktop.ui.InfoRow
import com.macerce.switchguard.desktop.ui.LocalStrings
import com.macerce.switchguard.desktop.ui.RowDivider
import com.macerce.switchguard.desktop.ui.SectionCard
import com.macerce.switchguard.desktop.ui.SectionTitle
import com.macerce.switchguard.desktop.ui.alerts.titleKey
import com.macerce.switchguard.desktop.ui.history.titleKey
import com.macerce.switchguard.desktop.ui.settings.SoundPickerDialog
import com.macerce.switchguard.desktop.ui.str

/**
 * Sensör uyarıları (telefon 2.30): kapı/hareket/su ve pil için olay başına Alarm/Bildirim/Yok; sıcaklık/nem için en az /
 * en çok sınırı. Her biri sabit kimlikli koşullu uyarıdır ([SensorAlerts]); Uyarılar › Koşullu uyarılar'da da görünür.
 */
@Composable
private fun SensorAlertRules(graph: AppGraph, device: DeviceSnapshot) {
    val strings = LocalStrings.current
    val version by graph.store.version.collectAsState()
    val list = remember(version) { graph.store.automations }
    val kinds = SensorAlerts.kinds(device)
    if (kinds.isEmpty()) return
    fun ActionKind?.alert() = when (this) { ActionKind.ALARM -> AlertAction.ALARM; ActionKind.NOTIFY -> AlertAction.NOTIFY; else -> AlertAction.IGNORE }
    fun AlertAction.kind() = when (this) { AlertAction.ALARM -> ActionKind.ALARM; AlertAction.NOTIFY -> ActionKind.NOTIFY; AlertAction.IGNORE -> null }
    fun save(id: String, a: Automation?) { if (a != null) graph.engine.saveAutomation(a) else graph.engine.deleteAutomation(id) }

    SectionTitle(str("sensor_alerts"))
    SectionCard {
        kinds.forEachIndexed { i, kind ->
            if (i > 0) RowDivider()
            val name = device.name + " · " + strings[kind.labelKey()]
            if (kind.binary || kind == SensorKind.BATTERY) {
                val id = SensorAlerts.id(device.id, kind)
                val title = when (kind) {
                    SensorKind.DOOR -> "sensor_alert_door"
                    SensorKind.MOTION -> "sensor_alert_motion"
                    SensorKind.WATER -> "sensor_alert_water"
                    else -> "sensor_alert_battery"
                }
                SensorRuleRow(kind.icon(), if (kind == SensorKind.BATTERY) strings.format(title, SensorAlerts.LOW_BATTERY.toInt()) else strings[title], SensorAlerts.actionOf(list, id).alert(), "sensor-alert-${kind.name}") { a ->
                    save(id, SensorAlerts.simple(device.id, name, kind, a.kind()))
                }
            } else {
                val loId = SensorAlerts.id(device.id, kind, SensorAlerts.Bound.MIN)
                val hiId = SensorAlerts.id(device.id, kind, SensorAlerts.Bound.MAX)
                // Metin yerel tutulur: her tuşta kayıt yenilenince yarım yazı ("2,") silinmesin.
                var lo by remember(device.id, kind) { mutableStateOf(SensorAlerts.valueOf(list, loId)?.let(::plain) ?: "") }
                var hi by remember(device.id, kind) { mutableStateOf(SensorAlerts.valueOf(list, hiId)?.let(::plain) ?: "") }
                val action = (SensorAlerts.actionOf(list, hiId) ?: SensorAlerts.actionOf(list, loId)).alert()
                fun apply(a: AlertAction, loText: String, hiText: String) {
                    save(loId, SensorAlerts.bound(device.id, name, kind, SensorAlerts.Bound.MIN, loText.replace(',', '.').toDoubleOrNull(), a.kind()))
                    save(hiId, SensorAlerts.bound(device.id, name, kind, SensorAlerts.Bound.MAX, hiText.replace(',', '.').toDoubleOrNull(), a.kind()))
                }
                SensorRuleRow(kind.icon(), strings.format("sensor_alert_range", strings[kind.labelKey()]), action, "sensor-alert-${kind.name}", header = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.weight(1f)) {
                            NumberField(lo, strings["sensor_min"], kind.unit, "sensor-min-${kind.name}", allowNegative = kind == SensorKind.TEMPERATURE) {
                                lo = it; if (action != AlertAction.IGNORE) apply(action, it, hi)
                            }
                        }
                        Box(Modifier.weight(1f)) {
                            NumberField(hi, strings["sensor_max"], kind.unit, "sensor-max-${kind.name}", allowNegative = kind == SensorKind.TEMPERATURE) {
                                hi = it; if (action != AlertAction.IGNORE) apply(action, lo, it)
                            }
                        }
                    }
                }) { a ->
                    // Sınır yazılmadan açılırsa şu anki değerin biraz üstü önerilir.
                    if (a != AlertAction.IGNORE && lo.isBlank() && hi.isBlank()) {
                        device.sensors[kind]?.let { v -> hi = Math.round(v + if (kind == SensorKind.TEMPERATURE) 3 else 15).toString() }
                    }
                    apply(a, lo, hi)
                }
            }
        }
    }
    Text(
        str("sensor_alerts_hint"),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
    )
}

private fun plain(v: Double): String = if (v == Math.floor(v)) v.toLong().toString() else v.toString()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SensorRuleRow(
    icon: ImageVector, title: String, current: AlertAction, tag: String,
    header: (@Composable () -> Unit)? = null, onChange: (AlertAction) -> Unit,
) {
    val strings = LocalStrings.current
    Column(Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(12.dp))
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        }
        header?.invoke()
        Spacer(Modifier.height(10.dp))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().testTag(tag)) {
            AlertAction.entries.forEachIndexed { i, action ->
                SegmentedButton(
                    selected = current == action,
                    onClick = { onChange(action) },
                    shape = SegmentedButtonDefaults.itemShape(i, AlertAction.entries.size),
                ) {
                    Text(strings[action.titleKey()], fontWeight = if (current == action) FontWeight.SemiBold else null)
                }
            }
        }
    }
}

/**
 * Bir cihazın ekranda görünen kuralları ve değiştirme işlevi. Değişiklik motora gider (`MonitorEngine.updateRules`);
 * motor meşgulken de ekran beklemesin diye seçim hemen gösterilir, motor yazınca ayardaki değerle birleşir.
 */
class DeviceRulesState(val deviceId: String, val rules: DeviceRules, val change: ((DeviceRules) -> DeviceRules) -> Unit)

/** [deviceId]'nin kuralları: ayardaki değer, üstünde motora gönderilmiş ama henüz yazılmamış değişiklik. */
@Composable
fun rememberDeviceRules(graph: AppGraph, deviceId: String): DeviceRulesState {
    val store = graph.store
    val version by store.version.collectAsState()
    val stored = remember(version, deviceId) { store.rulesFor(deviceId) }
    // Motora gönderilmiş ama henüz yazılmamış kural: ayardaki değer ona ulaşınca bırakılır.
    var pending by remember(deviceId) { mutableStateOf<DeviceRules?>(null) }
    LaunchedEffect(stored) { if (pending == stored) pending = null }
    val rules = pending ?: stored
    return DeviceRulesState(deviceId, rules) { f ->
        pending = f(rules)
        graph.engine.updateRules(deviceId, f)
    }
}

/**
 * Cihazın uyarı kuralları (telefondaki cihaz sayfasının kurallar bölümü): izleme anahtarı; izleniyorsa olay başına
 * Alarm / Bildirim / Yok, açıklama, cihaz sesi ve uyarı kategorisi. Cihaz sayfası ve Uyarılar sekmesindeki "Uyarı
 * kuralları" sayfası aynı bölümü gösterir. Ses seçim penceresi de burada açılır.
 */
@Composable
fun DeviceRulesSection(graph: AppGraph, state: DeviceRulesState) {
    val strings = LocalStrings.current
    val rules = state.rules
    val change = state.change
    var pickSound by remember { mutableStateOf(false) }

    SectionTitle(str("section_monitoring"))
    SectionCard {
        InfoRow(
            Icons.Rounded.NotificationsActive,
            str("monitor_this_device"),
            str("monitor_this_device_desc"),
        ) {
            Switch(
                checked = rules.monitored,
                onCheckedChange = { on -> change { it.copy(monitored = on) } },
                modifier = Modifier.testTag("monitor-switch"),
            )
        }
    }

    val version by graph.store.version.collectAsState()
    val device = remember(version, state.deviceId) { graph.store.snapshots.firstOrNull { it.id == state.deviceId } }
    // Sensör uyarıları koşullu uyarı kaydıdır: "izle" anahtarından bağımsız çalışır.
    if (device != null && device.hasSensors) SensorAlertRules(graph, device)

    if (rules.monitored) {
        // Yalnız sensör olan cihazın rölesi yok: açıldı/kapandı olayları anlamsız.
        val types = if (device?.sensorOnly == true) listOf(EventType.WENT_OFFLINE, EventType.CAME_ONLINE) else EventType.entries
        SectionTitle(str("section_rules"))
        SectionCard {
            types.forEachIndexed { i, type ->
                if (i > 0) RowDivider()
                RuleRow(type, rules.actionFor(type)) { action -> change { it.with(type, action) } }
            }
        }
        Text(
            str("rules_hint"),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
        SectionCard {
            val custom = rules.soundUri
            InfoRow(
                Icons.Rounded.MusicNote,
                str("device_sound"),
                if (custom == null) str("device_sound_default") else Sounds.displayName(custom, strings),
                Modifier.clickable { pickSound = true }.testTag("device-sound"),
            ) {
                if (custom != null) {
                    IconButton(onClick = { change { it.copy(soundUri = null) } }) {
                        Icon(Icons.Rounded.Close, str("device_sound_reset"))
                    }
                }
            }
            RowDivider()
            CategoryRow(rules) { c -> change { it.copy(category = c) } }
        }
    }

    if (pickSound) {
        // Seçili görünen: bu cihazın sesi, yoksa genel alarm sesi (çalacak olan).
        SoundPickerDialog(
            graph, strings, str("device_sound"),
            current = rules.soundUri ?: graph.store.alarmSoundUri,
            default = Sounds.DEFAULT_ALARM,
            onPick = { id -> change { it.copy(soundUri = id) } },
            onDismiss = { pickSound = false },
        )
    }
}

/**
 * Uyarı kategorisi: Otomatik (kurallardan; şu anki sonucu yazar) ya da elle Kritik / Önemli / Normal.
 * Kategori sessiz saatleri ve kategori tetikleyicili otomasyonları belirler.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryRow(rules: DeviceRules, onChange: (DeviceCategory?) -> Unit) {
    val auto = DeviceCategory.auto(rules)
    Column(Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Rounded.Label, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(str("alert_category"), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(
                    if (rules.category == null) str("category_auto_now", str(auto.titleKey())) else str("alert_category_desc"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        val options = listOf<DeviceCategory?>(null) + DeviceCategory.entries
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            options.forEachIndexed { i, c ->
                val selected = rules.category == c
                SegmentedButton(
                    selected = selected,
                    onClick = { onChange(c) },
                    shape = SegmentedButtonDefaults.itemShape(i, options.size, baseShape = RoundedCornerShape(4.dp)),
                    modifier = Modifier.testTag("category-${c?.name ?: "AUTO"}"),
                ) {
                    Text(str(c?.titleKey() ?: "category_auto"), fontWeight = if (selected) FontWeight.SemiBold else null)
                }
            }
        }
    }
}

private fun EventType.icon(): ImageVector = when (this) {
    EventType.TURNED_OFF -> Icons.Rounded.PowerOff
    EventType.TURNED_ON -> Icons.Rounded.PowerSettingsNew
    EventType.WENT_OFFLINE -> Icons.Rounded.CloudOff
    EventType.CAME_ONLINE -> Icons.Rounded.CloudQueue
}

private fun EventType.titleKey(): String = when (this) {
    EventType.TURNED_OFF -> "event_turned_off"
    EventType.TURNED_ON -> "event_turned_on"
    EventType.WENT_OFFLINE -> "event_went_offline"
    EventType.CAME_ONLINE -> "event_came_online"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RuleRow(type: EventType, current: AlertAction, onChange: (AlertAction) -> Unit) {
    Column(Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(type.icon(), null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(12.dp))
            Text(str(type.titleKey()), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(10.dp))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            AlertAction.entries.forEachIndexed { i, action ->
                SegmentedButton(
                    selected = current == action,
                    onClick = { onChange(action) },
                    shape = SegmentedButtonDefaults.itemShape(i, AlertAction.entries.size, baseShape = RoundedCornerShape(4.dp)),
                    modifier = Modifier.testTag("rule-${type.name}-${action.name}"),
                ) {
                    Text(str(action.titleKey()), fontWeight = if (current == action) FontWeight.SemiBold else null)
                }
            }
        }
    }
}

