package com.macerce.switchguard.ui

import com.macerce.switchguard.core.EventStrings
import androidx.compose.ui.platform.LocalConfiguration
import android.text.format.DateUtils
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.AutoMode
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CloudQueue
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.NetworkCheck
import androidx.compose.material.icons.rounded.PowerOff
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.macerce.switchguard.R
import com.macerce.switchguard.core.AlertAction
import com.macerce.switchguard.core.EventType
import com.macerce.switchguard.data.EventKind
import com.macerce.switchguard.data.EventLog
import com.macerce.switchguard.data.LoggedEvent
import com.macerce.switchguard.data.Store
import java.text.DateFormat
import java.util.Calendar
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(modifier: Modifier) {
    val context = LocalContext.current
    val log = remember { EventLog.get(context) }
    val store = remember { Store.get(context) }
    val logVersion by log.version.collectAsStateWithLifecycle()
    val version by store.version.collectAsStateWithLifecycle()

    var filter by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmClear by remember { mutableStateOf(false) }
    val devices = remember(version) { store.snapshots }
    val events = remember(logVersion, filter) { log.recent(filter) }

    Column(modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(R.string.tab_history), fontWeight = FontWeight.SemiBold) },
            actions = {
                IconButton(onClick = { confirmClear = true }, enabled = events.isNotEmpty()) {
                    Icon(Icons.Rounded.DeleteSweep, stringResource(R.string.action_clear_history))
                }
            },
        )
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(selected = filter == null, onClick = { filter = null }, label = { Text(stringResource(R.string.filter_all)) })
            devices.forEach { d ->
                FilterChip(selected = filter == d.id, onClick = { filter = d.id }, label = { Text(d.name) })
            }
        }

        if (events.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.History, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.outline)
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.no_events), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            val grouped = remember(events) { events.groupBy { dayKey(it.time) } }
            LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                grouped.forEach { (_, dayEvents) ->
                    item(key = "h" + dayEvents.first().id) {
                        SectionTitle(dayLabel(dayEvents.first().time))
                    }
                    item(key = "c" + dayEvents.first().id) {
                        SectionCard {
                            dayEvents.forEachIndexed { i, e ->
                                if (i > 0) androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.surface)
                                EventRow(e, showDevice = false)
                            }
                        }
                    }
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.clear_history_title)) },
            text = { Text(stringResource(R.string.clear_history_body)) },
            confirmButton = {
                TextButton(onClick = { log.clear(); confirmClear = false }) { Text(stringResource(R.string.action_clear)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

private fun dayKey(time: Long): Int = Calendar.getInstance().run {
    timeInMillis = time
    get(Calendar.YEAR) * 1000 + get(Calendar.DAY_OF_YEAR)
}

@Composable
private fun dayLabel(time: Long): String = when {
    DateUtils.isToday(time) -> stringResource(R.string.today)
    DateUtils.isToday(time + DateUtils.DAY_IN_MILLIS) -> stringResource(R.string.yesterday)
    else -> DateFormat.getDateInstance(DateFormat.FULL).format(Date(time))
}

private fun LoggedEvent.icon(): ImageVector = when (kind) {
    EventType.TURNED_OFF.name -> Icons.Rounded.PowerOff
    EventType.TURNED_ON.name -> Icons.Rounded.PowerSettingsNew
    EventType.WENT_OFFLINE.name -> Icons.Rounded.CloudOff
    EventType.CAME_ONLINE.name -> Icons.Rounded.CloudQueue
    EventKind.USER -> Icons.Rounded.TouchApp
    EventKind.SHORT_DROP -> Icons.Rounded.NetworkCheck
    EventKind.AUTOMATION -> Icons.Rounded.AutoMode
    else -> Icons.Rounded.Alarm
}

/** Seçili dildeki olay kalıpları; dil değişince yeniden oluşturulur. */
@Composable
private fun eventStrings(): EventStrings {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    return remember(context, configuration) { EventLog.strings(context) }
}

/** Geçmiş satırı: simge, metin, saat ve yapılan işlem rozeti. */
@Composable
fun EventRow(e: LoggedEvent, showDevice: Boolean) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(e.icon(), null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(e.text(eventStrings()), style = MaterialTheme.typography.bodyLarge)
            val time = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(e.time))
            val sub = when (e.kind) {
                EventKind.USER -> stringResource(R.string.event_by_user)
                EventKind.SHORT_DROP -> stringResource(R.string.event_short_drop_sub)
                EventKind.AUTOMATION -> stringResource(R.string.event_by_automation)
                else -> null
            }
            Text(
                listOfNotNull(time, sub, if (showDevice) e.deviceName else null).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        ActionBadge(e.action)
    }
}

@Composable
private fun ActionBadge(action: String) {
    val a = runCatching { AlertAction.valueOf(action) }.getOrNull() ?: return
    val (bg, fg) = when (a) {
        AlertAction.ALARM -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        AlertAction.NOTIFY -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        AlertAction.IGNORE -> Color.Transparent to MaterialTheme.colorScheme.outline
    }
    Surface(color = bg, shape = MaterialTheme.shapes.small) {
        Text(
            stringResource(a.title()),
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}
