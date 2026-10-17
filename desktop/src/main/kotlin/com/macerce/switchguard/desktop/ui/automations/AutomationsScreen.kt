package com.macerce.switchguard.desktop.ui.automations

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.AutoMode
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PowerOff
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.macerce.switchguard.core.ActionKind
import com.macerce.switchguard.core.Automation
import com.macerce.switchguard.core.DeviceSnapshot
import com.macerce.switchguard.core.TriggerKind
import com.macerce.switchguard.desktop.AppGraph
import com.macerce.switchguard.desktop.i18n.Strings
import com.macerce.switchguard.desktop.ui.Hue
import com.macerce.switchguard.desktop.ui.HueIcon
import com.macerce.switchguard.desktop.ui.PageHeader
import com.macerce.switchguard.desktop.ui.readableWidth
import com.macerce.switchguard.desktop.ui.tileColor
import java.util.Locale

/** Yeni otomasyon düzenleyicisinin işareti. */
private const val NEW = "new"

/**
 * Otomasyon sekmesi (telefondaki 2.13 "Otomasyon" ekranı): yalnızca cihaz açan/kapatan kayıtlar
 * ([AutomationMode.AUTOMATION]; alarm/bildirim verenler Uyarılar sekmesindeki "Koşullu uyarılar"da) — "Eğer … /
 * O zaman …" kartları, eylemin renkli simgesi, aç/kapa — ve tam sayfa düzenleyici ([AutomationEditor], otomasyon
 * modunda). Ekleme, değiştirme ve silme motor üzerinden yapılır (`MonitorEngine.saveAutomation` / `deleteAutomation`);
 * değerlendirme zaten motorda.
 */
@Composable
fun AutomationsScreen(graph: AppGraph, strings: Strings, modifier: Modifier = Modifier) {
    val store = graph.store
    val version by store.version.collectAsState()
    val automations = remember(version) { store.automations.inMode(AutomationMode.AUTOMATION) }
    val devices = remember(version) { store.snapshots }
    val monitoring = remember(version) { store.monitoringEnabled }
    var editing by remember { mutableStateOf<String?>(null) }

    editing?.let { id ->
        AutomationEditor(
            initial = automations.firstOrNull { it.id == id },
            devices = devices,
            strings = strings,
            modifier = modifier,
            onSave = {
                graph.engine.saveAutomation(it)
                editing = null
            },
            onDelete = {
                graph.engine.deleteAutomation(id)
                editing = null
            },
            onBack = { editing = null },
            newMode = AutomationMode.AUTOMATION,
        )
        return
    }

    Box(modifier.fillMaxSize()) {
        Column(Modifier.align(Alignment.TopCenter).readableWidth().fillMaxSize()) {
            PageHeader(strings["tab_automations"])
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (!monitoring && automations.isNotEmpty()) {
                    item {
                        Text(
                            strings["automations_need_monitoring"],
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 8.dp),
                        )
                    }
                }
                if (automations.isEmpty()) {
                    item { EmptyAutomations(strings) }
                }
                items(automations, key = { it.id }) { a ->
                    AutomationCard(
                        a, devices, strings,
                        onClick = { editing = a.id },
                        onToggle = { on -> graph.engine.saveAutomation(a.copy(enabled = on)) },
                    )
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = { editing = NEW },
            icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
            text = { Text(strings["automation_add"]) },
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp).testTag("add-automation"),
        )
    }
}

@Composable
private fun EmptyAutomations(strings: Strings) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 32.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Rounded.AutoMode, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        Text(strings["automations_empty"], style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(12.dp))
        Text(
            strings["automations_empty_desc"],
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 560.dp),
        )
    }
}

/**
 * Kayıt kartı (koşullu uyarı ya da otomasyon): eylemin renkli simgesi (kapalıysa soluk), adı ve "Eğer … / O zaman …"
 * satırları; sağda aç/kapa. Uyarılar sekmesi de kullanır.
 */
@Composable
internal fun AutomationCard(a: Automation, devices: List<DeviceSnapshot>, strings: Strings, onClick: () -> Unit, onToggle: (Boolean) -> Unit) {
    val (whenText, thenText) = describe(a, devices, strings)
    val shape = RoundedCornerShape(24.dp)
    Row(
        Modifier.fillMaxWidth()
            .clip(shape)
            .background(tileColor())
            .clickable(role = Role.Button, onClick = onClick)
            .padding(16.dp)
            .testTag("automation-${a.id}"),
        verticalAlignment = Alignment.Top,
    ) {
        Box(Modifier.alpha(if (a.enabled) 1f else 0.45f)) { HueIcon(a.action.kind.icon(), a.action.kind.hue(), size = 40.dp) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(a.name, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Clause(strings["auto_if"], whenText)
            Spacer(Modifier.height(2.dp))
            Clause(strings["auto_then"], thenText)
        }
        Spacer(Modifier.width(8.dp))
        Switch(checked = a.enabled, onCheckedChange = onToggle, modifier = Modifier.testTag("enabled-${a.id}"))
    }
}

/** "Eğer  Rögar · Kanal 1 açılınca": etiket vurgulu, metin aynı satırda devam eder (uzun koşul tam genişlikte kayar). */
@Composable
internal fun Clause(label: String, text: String) {
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

/**
 * Kartın ve düzenleyici özetinin iki satırı (telefondaki `describe`): ("Rögar · Kanal 1 · açılınca",
 * "Lamba: Aç · (5 dk sonra)").
 */
internal fun describe(a: Automation, devices: List<DeviceSnapshot>, strings: Strings): Pair<String, String> {
    val t = a.trigger
    val srcName = devices.firstOrNull { it.id == t.deviceId }?.let { channelLabel(it, t.channel, t.kind.usesChannel, strings) } ?: strings["device_missing"]
    val trigger = when (t.kind) {
        TriggerKind.STAYED_ON, TriggerKind.STAYED_OFF -> strings[t.kind.titleKey()] + " " + strings.plural("minutes_n", t.minutes, t.minutes)
        TriggerKind.POWER_ABOVE, TriggerKind.POWER_BELOW -> strings[t.kind.titleKey()] + " " + formatWatts(t.watts)
        TriggerKind.CHANNEL_POWER_LOW -> strings[t.kind.titleKey()] + " " + formatWatts(t.watts) + " " +
            if (t.graceMinutes > 0) strings.format("automation_low_for", t.minutes, t.graceMinutes)
            else strings.format("automation_low_for_nograce", t.minutes)
        else -> strings[t.kind.titleKey()]
    }
    val act = a.action
    val action = if (act.kind.usesDevice) {
        val dstName = devices.firstOrNull { it.id == act.deviceId }?.let { channelLabel(it, act.channel, true, strings) } ?: strings["device_missing"]
        "$dstName: " + strings[act.kind.titleKey()]
    } else {
        strings[act.kind.titleKey()]
    }
    val delay = if (a.delayMinutes > 0) " · " + strings.format("automation_after", a.delayMinutes) else ""
    return "$srcName · $trigger" to "$action$delay"
}

private fun channelLabel(d: DeviceSnapshot, channel: Int, withChannel: Boolean, strings: Strings): String =
    if (withChannel && d.isMultiChannel) "${d.name} · " + strings.format("channel_n", channel + 1) else d.name

/** Olayın cümle içindeki adı ("şu süre açık kalınca:"); kartta ve özette. */
internal fun TriggerKind.titleKey(): String = when (this) {
    TriggerKind.TURNED_ON -> "trigger_turned_on"
    TriggerKind.TURNED_OFF -> "trigger_turned_off"
    TriggerKind.STAYED_ON -> "trigger_stayed_on"
    TriggerKind.STAYED_OFF -> "trigger_stayed_off"
    TriggerKind.WENT_OFFLINE -> "trigger_went_offline"
    TriggerKind.CAME_ONLINE -> "trigger_came_online"
    TriggerKind.POWER_ABOVE -> "trigger_power_above"
    TriggerKind.POWER_BELOW -> "trigger_power_below"
    TriggerKind.CHANNEL_POWER_LOW -> "trigger_channel_power_low"
}

/** İşlemin kısa adı ("Aç", "Alarm çal"); kartta ve özette. */
internal fun ActionKind.titleKey(): String = when (this) {
    ActionKind.TURN_ON -> "auto_action_turn_on"
    ActionKind.TURN_OFF -> "auto_action_turn_off"
    ActionKind.ALARM -> "auto_action_alarm"
    ActionKind.NOTIFY -> "auto_action_notify"
}

/** İşlemin simgesi ve rengi: kartta ve düzenleyicinin işlem kutucuklarında aynı. */
internal fun ActionKind.icon(): ImageVector = when (this) {
    ActionKind.ALARM -> Icons.Rounded.Alarm
    ActionKind.NOTIFY -> Icons.Rounded.NotificationsActive
    ActionKind.TURN_ON -> Icons.Rounded.PowerSettingsNew
    ActionKind.TURN_OFF -> Icons.Rounded.PowerOff
}

internal fun ActionKind.hue(): Hue = when (this) {
    ActionKind.ALARM -> Hue.RED
    ActionKind.NOTIFY -> Hue.BLUE
    ActionKind.TURN_ON -> Hue.GREEN
    ActionKind.TURN_OFF -> Hue.SLATE
}

internal fun formatWatts(w: Double): String =
    if (w >= 1000) String.format(Locale.getDefault(), "%.2f kW", w / 1000) else String.format(Locale.getDefault(), "%.0f W", w)
