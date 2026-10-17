package com.macerce.switchguard.desktop.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoMode
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.macerce.switchguard.desktop.AppGraph
import com.macerce.switchguard.desktop.AppState.Tab
import com.macerce.switchguard.desktop.i18n.Strings
import com.macerce.switchguard.desktop.ui.alerts.AlertsScreen
import com.macerce.switchguard.desktop.ui.automations.AutomationsScreen
import com.macerce.switchguard.desktop.ui.settings.SettingsScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Sol menüdeki sekmelerin adı (metin anahtarı) ve simgesi; telefondaki App.kt'nin Tab'ı (2.13). */
private fun Tab.label(): String = when (this) {
    Tab.DEVICES -> "tab_devices"
    Tab.ALERTS -> "tab_alerts"
    Tab.AUTOMATIONS -> "tab_automations"
    Tab.HISTORY -> "tab_history"
    Tab.SETTINGS -> "tab_settings"
}

private fun Tab.icon(): ImageVector = when (this) {
    Tab.DEVICES -> Icons.Rounded.Devices
    Tab.ALERTS -> Icons.Rounded.NotificationsActive
    Tab.AUTOMATIONS -> Icons.Rounded.AutoMode
    Tab.HISTORY -> Icons.Rounded.History
    Tab.SETTINGS -> Icons.Rounded.Settings
}

/** Okunabilir genişlik: geniş pencerede içerik ortada en çok [max] genişlikte (telefondaki readableWidth). */
fun Modifier.readableWidth(max: Dp = 840.dp): Modifier = this.widthIn(max = max)

/**
 * Sol menü (Cihazlar · Uyarılar · Otomasyon · Geçmiş · Ayarlar) ve seçili ekran; telefondaki App.kt'nin geniş ekran düzeni.
 * Seçim ve açık cihaz [com.macerce.switchguard.desktop.AppState]'te: pencere yeniden kurulsa da korunur. Geçmiş rozeti:
 * son bakıştan beri alarm/bildirim üretmiş olay sayısı; Geçmiş açıkken yeni olaylar da görülmüş sayılır.
 */
@Composable
fun AppNav(graph: AppGraph, strings: Strings) {
    val state = graph.appState
    val tab by state.tab.collectAsState()
    val openDevice by state.openDevice.collectAsState()
    val version by graph.store.version.collectAsState()
    val logVersion by graph.eventLog.version.collectAsState()

    LaunchedEffect(tab, logVersion) {
        if (tab == Tab.HISTORY) withContext(Dispatchers.IO) { graph.store.historySeenId = graph.eventLog.latestId() }
    }
    var unseen by remember { mutableIntStateOf(0) }
    // Cihazlar ızgarasının kaydırma yeri: cihaz sayfası ya da başka sekme açılıp dönülünce korunur.
    val devicesGrid = rememberLazyGridState()
    LaunchedEffect(logVersion, version) {
        unseen = withContext(Dispatchers.IO) {
            // İlk kez: eski kayıtlar rozete dolmasın, sayım bundan sonrası için başlasın.
            val seen = graph.store.historySeenId ?: graph.eventLog.latestId().also { graph.store.historySeenId = it }
            graph.eventLog.unseenAlertCount(seen)
        }
    }

    Row(Modifier.fillMaxSize()) {
        NavigationRail(Modifier.fillMaxHeight(), containerColor = MaterialTheme.colorScheme.background) {
            Spacer(Modifier.weight(1f))
            Tab.entries.forEach { t ->
                NavigationRailItem(
                    selected = tab == t,
                    onClick = { state.select(t) },
                    icon = {
                        if (t == Tab.HISTORY && unseen > 0) {
                            BadgedBox(badge = { Badge { Text(if (unseen > 99) "99+" else unseen.toString()) } }) {
                                Icon(t.icon(), contentDescription = null)
                            }
                        } else {
                            Icon(t.icon(), contentDescription = null)
                        }
                    },
                    label = { Text(strings[t.label()]) },
                )
            }
            Spacer(Modifier.weight(1f))
        }
        Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
            AnimatedContent(
                targetState = if (tab == Tab.DEVICES) openDevice else null,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "detail",
            ) { deviceId ->
                if (deviceId != null) {
                    DeviceDetailScreen(graph, deviceId, onBack = { state.openDevice.value = null })
                } else when (tab) {
                    Tab.DEVICES -> DevicesScreen(graph, strings, Modifier.fillMaxSize(), devicesGrid)
                    Tab.ALERTS -> AlertsScreen(graph, strings, Modifier.fillMaxSize())
                    Tab.AUTOMATIONS -> AutomationsScreen(graph, strings, Modifier.fillMaxSize())
                    Tab.HISTORY -> HistoryScreen(graph, Modifier.fillMaxSize())
                    Tab.SETTINGS -> SettingsScreen(graph, strings)
                }
            }
        }
    }
}
