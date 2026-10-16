package com.macerce.switchguard.desktop.ui

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.macerce.switchguard.core.HistoryRange
import com.macerce.switchguard.data.LoggedEvent
import com.macerce.switchguard.desktop.AppGraph
import com.macerce.switchguard.desktop.Log
import com.macerce.switchguard.desktop.i18n.Strings
import com.macerce.switchguard.desktop.ui.devices.HintIconButton
import com.macerce.switchguard.desktop.ui.history.EventRow
import com.macerce.switchguard.desktop.ui.history.HistoryExport
import com.macerce.switchguard.desktop.ui.history.chooseCsvTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.file.Path
import java.text.DateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

/**
 * Geçmiş sekmesi (telefondaki HistoryScreen.kt, 2.12): cihaz süzgeci, gün başlıklarıyla gruplanmış olaylar (renkli
 * dairede olay simgesi, işlem rozeti), "Geçmişi temizle" ve CSV dışa aktarma (aralık seçimi + Windows "Farklı kaydet"
 * penceresi, [chooseTarget]). Veritabanı okuma/yazma IO iş parçacığında; içerik okunabilir genişlikte ortalanır.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HistoryScreen(
    graph: AppGraph,
    modifier: Modifier = Modifier,
    chooseTarget: (defaultName: String, title: String) -> Path? = ::chooseCsvTarget,
) {
    val strings = LocalStrings.current
    val log = graph.eventLog
    val store = graph.store
    val logVersion by log.version.collectAsState()
    val version by store.version.collectAsState()

    var filter by remember { mutableStateOf<String?>(null) }
    var confirmClear by remember { mutableStateOf(false) }
    var chooseRange by remember { mutableStateOf(false) }
    var exportRange by remember { mutableStateOf(HistoryRange.ALL) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val devices = remember(version) { store.snapshots }
    // Süzülen cihaz listeden çıktıysa (ör. demo kapatıldı) "Tümü"ne dönülür; çipi görünmeyen süzgeç kalmasın.
    LaunchedEffect(devices) { if (filter != null && devices.none { it.id == filter }) filter = null }
    // null: henüz okunmadı (boş durum yanıp sönmesin).
    val events by produceState<List<LoggedEvent>?>(null, logVersion, filter) {
        value = withContext(Dispatchers.IO) { runCatching { log.recent(filter) }.getOrDefault(emptyList()) }
    }
    val list = events.orEmpty()

    fun export(range: HistoryRange) {
        exportRange = range
        val target = chooseTarget(HistoryExport.fileName(strings), strings["export_title"]) ?: return
        val device = filter
        scope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { HistoryExport.write(target, log, device, range, strings) } }
            val message = result.fold(
                onSuccess = { n -> strings.plural("export_done", n, n) },
                onFailure = {
                    Log.w("History", "export failed", it)
                    strings["export_failed"]
                },
            )
            snackbar.showSnackbar(message)
        }
    }

    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.readableWidth().fillMaxSize()) {
            PageHeader(
                str("tab_history"),
                actions = {
                    HintIconButton(Icons.Rounded.FileDownload, str("action_export"), { chooseRange = true }, Modifier.testTag("history-export"), enabled = list.isNotEmpty())
                    HintIconButton(Icons.Rounded.DeleteSweep, str("action_clear_history"), { confirmClear = true }, Modifier.testTag("history-clear"), enabled = list.isNotEmpty())
                },
            )
            // Telefonda yatay kayan çipler; masaüstünde fare tekerleği yatay kaydırmadığından alt satıra geçer.
            FlowRow(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(selected = filter == null, onClick = { filter = null }, label = { Text(str("filter_all")) })
                devices.forEach { d ->
                    FilterChip(selected = filter == d.id, onClick = { filter = d.id }, label = { Text(d.name) }, modifier = Modifier.testTag("filter-${d.id}"))
                }
            }

            if (events != null && list.isEmpty()) {
                Column(Modifier.fillMaxSize().padding(48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.History, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.height(12.dp))
                    Text(str("no_events"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                val zone = remember { ZoneId.systemDefault() }
                val grouped = remember(list) { list.groupBy { Instant.ofEpochMilli(it.time).atZone(zone).toLocalDate() } }
                val today = LocalDate.now(zone)
                val listState = rememberLazyListState()
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    LazyColumn(state = listState, contentPadding = PaddingValues(bottom = 24.dp), modifier = Modifier.fillMaxSize().testTag("history-list")) {
                        grouped.forEach { (day, dayEvents) ->
                            item(key = "h" + dayEvents.first().id) {
                                SectionTitle(dayLabel(day, today, zone, strings))
                            }
                            item(key = "c" + dayEvents.first().id) {
                                SectionCard {
                                    dayEvents.forEachIndexed { i, e ->
                                        if (i > 0) RowDivider(70.dp)
                                        EventRow(e, showDevice = false)
                                    }
                                }
                            }
                        }
                    }
                    VerticalScrollbar(
                        rememberScrollbarAdapter(listState),
                        Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(vertical = 4.dp),
                    )
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(16.dp))
    }

    if (chooseRange) {
        RangeDialog(
            description = str(if (filter == null) "export_desc_all" else "export_desc_device"),
            selected = exportRange,
            onPick = { range ->
                chooseRange = false
                export(range)
            },
            onDismiss = { chooseRange = false },
        )
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(str("clear_history_title")) },
            text = { Text(str("clear_history_body")) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmClear = false
                        scope.launch { withContext(Dispatchers.IO) { runCatching { log.clear() } } }
                    },
                    modifier = Modifier.testTag("history-clear-confirm"),
                ) { Text(str("action_clear")) }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text(str("action_cancel")) } },
        )
    }
}

/** "Bugün", "Dün" ya da seçili dilde uzun tarih ("16 Ekim 2026 Cuma"). */
private fun dayLabel(day: LocalDate, today: LocalDate, zone: ZoneId, strings: Strings): String = when (day) {
    today -> strings["today"]
    today.minusDays(1) -> strings["yesterday"]
    else -> DateFormat.getDateInstance(DateFormat.FULL, strings.locale).format(Date.from(day.atStartOfDay(zone).toInstant()))
}

private fun rangeKey(r: HistoryRange): String = when (r) {
    HistoryRange.DAY -> "export_range_day"
    HistoryRange.WEEK -> "export_range_week"
    HistoryRange.MONTH -> "export_range_month"
    HistoryRange.ALL -> "export_range_all"
}

/** Telefondaki ChoiceDialog: dışa aktarılacak aralık (son 24 saat / 7 gün / 30 gün / tümü). */
@Composable
private fun RangeDialog(description: String, selected: HistoryRange, onPick: (HistoryRange) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(str("export_title")) },
        text = {
            Column {
                Text(description, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 12.dp))
                HistoryRange.entries.forEach { r ->
                    Row(
                        Modifier.fillMaxWidth()
                            .selectable(selected = r == selected, onClick = { onPick(r) })
                            .padding(vertical = 8.dp)
                            .testTag("range-${r.name}"),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = r == selected, onClick = null)
                        Text(str(rangeKey(r)), modifier = Modifier.padding(start = 12.dp))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(str("action_cancel")) } },
    )
}
