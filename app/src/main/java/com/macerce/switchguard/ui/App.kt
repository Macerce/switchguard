package com.macerce.switchguard.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoMode
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.macerce.switchguard.R
import com.macerce.switchguard.data.EventLog
import com.macerce.switchguard.data.Store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private enum class Tab(val label: Int, val icon: ImageVector) {
    DEVICES(R.string.tab_devices, Icons.Rounded.Devices),
    AUTOMATIONS(R.string.tab_automations, Icons.Rounded.AutoMode),
    HISTORY(R.string.tab_history, Icons.Rounded.History),
    SETTINGS(R.string.tab_settings, Icons.Rounded.Settings),
}

@Composable
fun App() {
    val context = LocalContext.current
    val store = remember { Store.get(context) }
    val version by store.version.collectAsStateWithLifecycle()

    // version okunarak her ayar değişiminde yeniden değerlendirilir.
    val onboarded = remember(version) { store.onboardingDone }
    if (!onboarded) {
        OnboardingScreen(onFinished = { store.onboardingDone = true })
        return
    }

    var tab by rememberSaveable { mutableStateOf(Tab.DEVICES) }
    var openDeviceId by rememberSaveable { mutableStateOf<String?>(null) }

    // Geri: önce cihaz detayını kapat, sonra diğer sekmelerden Cihazlar'a dön; yalnızca Cihazlar'dan çık.
    // Sıra önemli: sonra kaydedilen BackHandler önce çalışır.
    BackHandler(enabled = tab != Tab.DEVICES) { tab = Tab.DEVICES }
    BackHandler(enabled = openDeviceId != null) { openDeviceId = null }

    // Geçmiş rozeti: son bakıştan beri alarm/bildirim üretmiş olay sayısı. Geçmiş açıkken yeni olaylar da görülmüş sayılır.
    val log = remember { EventLog.get(context) }
    val logVersion by log.version.collectAsStateWithLifecycle()
    LaunchedEffect(tab, logVersion) {
        if (tab == Tab.HISTORY) withContext(Dispatchers.IO) { store.historySeenId = log.latestId() }
    }
    var unseen by remember { mutableIntStateOf(0) }
    LaunchedEffect(logVersion, version) {
        unseen = withContext(Dispatchers.IO) {
            // İlk kez: eski kayıtlar rozete dolmasın, sayım bundan sonrası için başlasın.
            val seen = store.historySeenId ?: log.latestId().also { store.historySeenId = it }
            log.unseenAlertCount(seen)
        }
    }

    val wide = isWideScreen()
    val tabIcon: @Composable (Tab) -> Unit = { t ->
        if (t == Tab.HISTORY && unseen > 0) {
            BadgedBox(badge = { Badge { Text(if (unseen > 99) "99+" else unseen.toString()) } }) {
                Icon(t.icon, contentDescription = null)
            }
        } else {
            Icon(t.icon, contentDescription = null)
        }
    }
    val tabContent: @Composable (Modifier) -> Unit = { mod ->
        when (tab) {
            Tab.DEVICES -> DevicesScreen(mod, onOpenDevice = { openDeviceId = it })
            Tab.AUTOMATIONS -> AutomationsScreen(mod)
            Tab.HISTORY -> HistoryScreen(mod)
            Tab.SETTINGS -> SettingsScreen(mod)
        }
    }

    AnimatedContent(
        targetState = openDeviceId,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "detail",
    ) { deviceId ->
        if (deviceId != null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                Box(Modifier.readableWidth()) { DeviceDetailScreen(deviceId = deviceId, onBack = { openDeviceId = null }) }
            }
        } else if (wide) {
            // Tablet, Chromebook ve TV: alt menü yerine yan menü, içerik okunabilir genişlikte ortada.
            Row(Modifier.fillMaxSize()) {
                NavigationRail(Modifier.fillMaxHeight()) {
                    Spacer(Modifier.weight(1f))
                    Tab.entries.forEach { t ->
                        NavigationRailItem(
                            modifier = Modifier.tvFocusRing(RoundedCornerShape(16.dp), inside = true),
                            selected = tab == t,
                            onClick = { tab = t },
                            icon = { tabIcon(t) },
                            label = { Text(stringResource(t.label)) },
                        )
                    }
                    Spacer(Modifier.weight(1f))
                }
                Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
                    tabContent(Modifier.readableWidth())
                }
            }
        } else {
            Scaffold(
                bottomBar = {
                    NavigationBar {
                        Tab.entries.forEach { t ->
                            NavigationBarItem(
                                selected = tab == t,
                                onClick = { tab = t },
                                icon = { tabIcon(t) },
                                label = { Text(stringResource(t.label)) },
                            )
                        }
                    }
                },
            ) { padding ->
                tabContent(Modifier.padding(bottom = padding.calculateBottomPadding()))
            }
        }
    }
}
