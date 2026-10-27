package com.macerce.switchguard.desktop.ui.devices

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
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.macerce.switchguard.core.Entitlement
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
        if (rules.monitored) FreeLimitRow(graph, state.deviceId)
    }

    if (rules.monitored) {
        SectionTitle(str("section_rules"))
        SectionCard {
            EventType.entries.forEachIndexed { i, type ->
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

/**
 * Lisans yokken izlenmek istenen ama ücretsiz sürümün tek cihaz sınırına takılan cihaz: hangi cihazın izlendiği
 * söylenir ve izlenen cihaz bununla değiştirilebilir (telefondaki pro_limit penceresi).
 */
@Composable
private fun FreeLimitRow(graph: AppGraph, deviceId: String) {
    val store = graph.store
    val version by store.version.collectAsState()
    val other = remember(version, deviceId) {
        if (store.isPro || Demo.isDemo(deviceId) || deviceId in store.monitoredIds()) return@remember null
        val ids = store.monitoredIds()
        store.snapshots.firstOrNull { it.id in ids && !Demo.isDemo(it.id) }?.name ?: ""
    } ?: return
    RowDivider()
    InfoRow(
        Icons.Rounded.WorkspacePremium,
        str("pro_limit_title"),
        if (other.isEmpty()) str("pro_limit_detail", Entitlement.FREE_DEVICES) else str("pro_limit_body", Entitlement.FREE_DEVICES, other),
    ) {
        TextButton(onClick = { store.freeDeviceId = deviceId }, modifier = Modifier.testTag("free-switch")) {
            Text(str("pro_limit_switch"))
        }
    }
}
