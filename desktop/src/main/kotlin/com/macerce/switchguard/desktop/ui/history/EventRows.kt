package com.macerce.switchguard.desktop.ui.history

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.AlarmOff
import androidx.compose.material.icons.rounded.AutoMode
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CloudQueue
import androidx.compose.material.icons.rounded.NetworkCheck
import androidx.compose.material.icons.rounded.PowerOff
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Snooze
import androidx.compose.material.icons.rounded.SyncProblem
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.macerce.switchguard.core.AlertAction
import com.macerce.switchguard.core.EventType
import com.macerce.switchguard.data.EventKind
import com.macerce.switchguard.data.LoggedEvent
import com.macerce.switchguard.desktop.ui.IconBadge
import com.macerce.switchguard.desktop.ui.LocalStrings
import com.macerce.switchguard.desktop.ui.raisedOnTile
import com.macerce.switchguard.desktop.ui.str

// Telefondaki HistoryScreen.kt'nin (2.12) olay satırı: Geçmiş sekmesi ve cihaz sayfasının "Son olaylar" bölümü.

/** Olay türünün simgesi; masaüstüne özgü [EventKind.SYSTEM] (bilgisayar uykudaydı) ay simgesiyle. */
fun LoggedEvent.icon(): ImageVector = when (kind) {
    EventType.TURNED_OFF.name -> Icons.Rounded.PowerOff
    EventType.TURNED_ON.name -> Icons.Rounded.PowerSettingsNew
    EventType.WENT_OFFLINE.name -> Icons.Rounded.CloudOff
    EventType.CAME_ONLINE.name -> Icons.Rounded.CloudQueue
    EventKind.USER -> Icons.Rounded.TouchApp
    EventKind.SHORT_DROP -> Icons.Rounded.NetworkCheck
    EventKind.AUTOMATION -> Icons.Rounded.AutoMode
    EventKind.MONITOR_GAP -> Icons.Rounded.SyncProblem
    EventKind.MONITOR_BLIND -> Icons.Rounded.WifiOff
    EventKind.DISMISSED -> Icons.Rounded.AlarmOff
    EventKind.REMINDER -> Icons.Rounded.Snooze
    EventKind.ESCALATION -> Icons.AutoMirrored.Rounded.Send
    EventKind.SYSTEM -> Icons.Rounded.Bedtime
    else -> Icons.Rounded.Alarm
}

/** Yapılan işlemin metin anahtarı (Alarm / Bildirim / Yok). */
fun AlertAction.titleKey(): String = when (this) {
    AlertAction.ALARM -> "action_alarm"
    AlertAction.NOTIFY -> "action_notify"
    AlertAction.IGNORE -> "action_ignore"
}

/** Geçmiş satırı: renkli dairede simge (alarm kırmızı, bildirim turkuaz), metin, saat ve yapılan işlem rozeti. */
@Composable
fun EventRow(e: LoggedEvent, showDevice: Boolean) {
    val strings = LocalStrings.current
    val eventStrings = remember(strings) { strings.eventStrings() }
    val action = runCatching { AlertAction.valueOf(e.action) }.getOrNull()
    val scheme = MaterialTheme.colorScheme
    // Simge zemini olayın sonucunu söyler: alarm kırmızı, bildirim turkuaz, diğerleri sade.
    val (bg, fg) = when (action) {
        AlertAction.ALARM -> scheme.errorContainer to scheme.onErrorContainer
        AlertAction.NOTIFY -> scheme.primaryContainer to scheme.onPrimaryContainer
        else -> raisedOnTile() to scheme.onSurfaceVariant
    }
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        IconBadge(e.icon(), fg, bg, size = 40.dp)
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(e.text(eventStrings), style = MaterialTheme.typography.bodyLarge)
            val sub = when (e.kind) {
                EventKind.USER -> str("event_by_user")
                EventKind.SHORT_DROP -> str("event_short_drop_sub")
                EventKind.AUTOMATION -> str("event_by_automation")
                else -> null
            }
            Text(
                listOfNotNull(strings.time(e.time), sub, if (showDevice) e.deviceName else null).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        ActionBadge(action)
    }
}

@Composable
private fun ActionBadge(action: AlertAction?) {
    action ?: return
    val (bg, fg) = when (action) {
        AlertAction.ALARM -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        AlertAction.NOTIFY -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        AlertAction.IGNORE -> Color.Transparent to MaterialTheme.colorScheme.outline
    }
    Surface(color = bg, shape = CircleShape) {
        Text(
            str(action.titleKey()),
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}
