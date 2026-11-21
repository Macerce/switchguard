package com.macerce.switchguard.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Share
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.content.FileProvider
import android.content.Intent
import com.macerce.switchguard.ui.theme.Brand
import com.macerce.switchguard.ui.theme.tileColor
import androidx.compose.foundation.layout.width
import com.macerce.switchguard.ui.theme.raisedOnTile
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.ElectricBolt
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.AlarmOff
import androidx.compose.material.icons.rounded.AutoMode
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CloudQueue
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.NetworkCheck
import androidx.compose.material.icons.rounded.PowerOff
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Snooze
import androidx.compose.material.icons.rounded.SyncProblem
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.WifiOff
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.macerce.switchguard.R
import com.macerce.switchguard.core.AlertAction
import com.macerce.switchguard.core.EventStrings
import com.macerce.switchguard.core.EventType
import com.macerce.switchguard.core.HistoryRange
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalResources
import kotlinx.coroutines.launch
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
    // Videodaki tür çipleri: Tümü / Alarm / Çevrimdışı / Otomasyon (cihaz filtresi ayrı açılır menüde).
    var type by rememberSaveable { mutableStateOf(HistoryType.ALL) }
    var deviceMenu by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    var chooseRange by remember { mutableStateOf(false) }
    var exportRange by rememberSaveable { mutableStateOf(HistoryRange.ALL) }
    val scope = rememberCoroutineScope()
    val resources = LocalResources.current
    // Kullanıcı kaydedeceği yeri seçer (Dosyalar, Drive...); yazma işi arka planda.
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val device = filter
        scope.launch {
            val message = runCatching { HistoryExport.write(context, uri, device, exportRange) }.fold(
                onSuccess = { n -> resources.getQuantityString(R.plurals.export_done, n, n) },
                onFailure = { resources.getString(R.string.export_failed) },
            )
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }
    val devices = remember(version) { store.snapshots }
    val events = remember(logVersion, filter, type) { log.recent(filter).filter(type::matches) }

    Column(modifier.fillMaxSize()) {
        AppTopBar(
            stringResource(R.string.tab_history),
            actions = {
                // TV'de dosya kaydetme ekranı (DocumentsUI) yok.
                if (!LocalIsTv.current) {
                    IconButton(onClick = { chooseRange = true }, enabled = events.isNotEmpty()) {
                        Icon(Icons.Rounded.FileDownload, stringResource(R.string.action_export))
                    }
                }
                IconButton(modifier = Modifier.tvFocusRing(), onClick = { confirmClear = true }, enabled = events.isNotEmpty()) {
                    Icon(Icons.Rounded.DeleteSweep, stringResource(R.string.action_clear_history))
                }
            },
        )
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HistoryType.entries.forEach { t ->
                ChoicePill(
                    stringResource(t.label),
                    selected = type == t,
                    onClick = { type = t },
                    accent = if (t == HistoryType.ALARM) Brand.alarm else MaterialTheme.colorScheme.primary,
                )
            }
            // Cihaz filtresi: açılır menü
            Box {
                ChoicePill(
                    devices.firstOrNull { it.id == filter }?.name ?: stringResource(R.string.automation_device),
                    selected = filter != null,
                    onClick = { deviceMenu = true },
                    icon = Icons.Rounded.ArrowDropDown,
                )
                DropdownMenu(expanded = deviceMenu, onDismissRequest = { deviceMenu = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.filter_all)) }, onClick = { filter = null; deviceMenu = false })
                    devices.forEach { d ->
                        DropdownMenuItem(text = { Text(d.name) }, onClick = { filter = d.id; deviceMenu = false })
                    }
                }
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
                        // Videodaki turkuaz, küçük tarih başlığı.
                        Text(
                            dayLabel(dayEvents.first().time),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 18.dp, bottom = 8.dp),
                        )
                    }
                    item(key = "c" + dayEvents.first().id) {
                        SectionCard {
                            dayEvents.forEachIndexed { i, e ->
                                if (i > 0) RowDivider(70.dp)
                                EventRow(e, showDevice = filter == null)
                            }
                        }
                    }
                }
            }
        }
    }

    if (chooseRange) {
        val fileName = remember { HistoryExport.fileName(context) }
        ModalBottomSheet(
            onDismissRequest = { chooseRange = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
                Text(stringResource(R.string.export_title), style = MaterialTheme.typography.titleLarge)
                Text(
                    stringResource(if (filter == null) R.string.export_desc_all else R.string.export_desc_device),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HistoryRange.entries.forEach { r -> ChoicePill(rangeLabel(r), exportRange == r, { exportRange = r }) }
                }
                Row(
                    Modifier.fillMaxWidth().padding(top = 14.dp).clip(RoundedCornerShape(16.dp)).background(tileColor())
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Description, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Text(fileName, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PillButton(
                        stringResource(R.string.export_save_file),
                        onClick = {
                            chooseRange = false
                            runCatching { exporter.launch(fileName) }
                                .onFailure { Toast.makeText(context, resources.getString(R.string.export_failed), Toast.LENGTH_LONG).show() }
                        },
                        icon = Icons.Rounded.FileDownload,
                        modifier = Modifier.weight(1f),
                    )
                    PillButton(
                        stringResource(R.string.action_share),
                        onClick = {
                            chooseRange = false
                            val device = filter
                            scope.launch {
                                // Önbelleğe yazıp paylaşım penceresini aç (FileProvider; dosya yalnızca seçilen uygulamaya açılır).
                                runCatching {
                                    val dir = java.io.File(context.cacheDir, "exports").apply { mkdirs() }
                                    val file = java.io.File(dir, fileName)
                                    HistoryExport.write(context, android.net.Uri.fromFile(file), device, exportRange)
                                    val uri = FileProvider.getUriForFile(context, context.packageName + ".files", file)
                                    val send = Intent(Intent.ACTION_SEND).setType("text/csv")
                                        .putExtra(Intent.EXTRA_STREAM, uri)
                                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    context.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                }.onFailure { Toast.makeText(context, resources.getString(R.string.export_failed), Toast.LENGTH_LONG).show() }
                            }
                        },
                        style = PillStyle.TONAL,
                        icon = Icons.Rounded.Share,
                        modifier = Modifier.weight(1f),
                    )
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
                TextButton(modifier = Modifier.tvFocusRing(), onClick = { log.clear(); confirmClear = false }) { Text(stringResource(R.string.action_clear)) }
            },
            dismissButton = {
                TextButton(modifier = Modifier.tvFocusRing(), onClick = { confirmClear = false }) { Text(stringResource(R.string.action_cancel)) }
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

internal fun LoggedEvent.icon(): ImageVector = when (kind) {
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
    EventKind.ESCALATION, EventKind.TELEGRAM -> Icons.AutoMirrored.Rounded.Send
    EventKind.OUTAGE -> Icons.Rounded.ElectricBolt
    EventKind.ENERGY -> Icons.AutoMirrored.Rounded.TrendingUp
    EventKind.WATCHDOG -> Icons.Rounded.MonitorHeart
    else -> Icons.Rounded.Alarm
}

@Composable
private fun rangeLabel(r: HistoryRange): String = stringResource(
    when (r) {
        HistoryRange.DAY -> R.string.export_range_day
        HistoryRange.WEEK -> R.string.export_range_week
        HistoryRange.MONTH -> R.string.export_range_month
        HistoryRange.ALL -> R.string.export_range_all
    }
)

/** Seçili dildeki olay kalıpları; dil değişince yeniden oluşturulur. */
@Composable
internal fun eventStrings(): EventStrings {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    return remember(context, configuration) { EventLog.strings(context) }
}

/** Geçmiş satırı (videodaki gibi): sonucun renginde simge dairesi, metin + kaynak, sağda saat. */
@Composable
fun EventRow(e: LoggedEvent, showDevice: Boolean) {
    val action = runCatching { AlertAction.valueOf(e.action) }.getOrNull()
    val scheme = MaterialTheme.colorScheme
    // Simge zemini olayın sonucunu söyler: alarm kırmızı, bildirim turkuaz, diğerleri sade.
    val (bg, fg) = when (action) {
        AlertAction.ALARM -> Brand.alarm.copy(alpha = 0.18f) to Brand.alarm
        AlertAction.NOTIFY -> scheme.primary.copy(alpha = 0.18f) to scheme.primary
        else -> raisedOnTile() to scheme.onSurfaceVariant
    }
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        IconBadge(e.icon(), fg, bg, size = 40)
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(e.text(eventStrings()), style = MaterialTheme.typography.bodyLarge)
            val sub = when (e.kind) {
                EventKind.USER -> stringResource(R.string.event_by_user)
                EventKind.SHORT_DROP -> stringResource(R.string.event_short_drop_sub)
                EventKind.AUTOMATION -> stringResource(R.string.event_by_automation)
                else -> null
            }
            val line = listOfNotNull(if (showDevice) e.deviceName.ifEmpty { null } else null, sub).joinToString(" · ")
            if (line.isNotEmpty()) {
                Text(line, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(
            DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(e.time)),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
    Surface(color = bg, shape = CircleShape) {
        Text(
            stringResource(a.title()),
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

/** Geçmiş tür filtresi (videodaki çipler). */
private enum class HistoryType(val label: Int) {
    ALL(R.string.filter_all),
    ALARM(R.string.action_alarm),
    OFFLINE(R.string.label_offline_title),
    AUTOMATION(R.string.tab_automations);

    fun matches(e: LoggedEvent): Boolean = when (this) {
        ALL -> true
        ALARM -> e.action == AlertAction.ALARM.name
        OFFLINE -> e.kind == EventType.WENT_OFFLINE.name || e.kind == EventType.CAME_ONLINE.name || e.kind == EventKind.SHORT_DROP
        AUTOMATION -> e.kind == EventKind.AUTOMATION
    }
}
