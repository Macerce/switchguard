package com.macerce.switchguard.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoMode
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.macerce.switchguard.R
import com.macerce.switchguard.data.Store

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

    AnimatedContent(
        targetState = openDeviceId,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "detail",
    ) { deviceId ->
        if (deviceId != null) {
            DeviceDetailScreen(deviceId = deviceId, onBack = { openDeviceId = null })
        } else {
            Scaffold(
                bottomBar = {
                    NavigationBar {
                        Tab.entries.forEach { t ->
                            NavigationBarItem(
                                selected = tab == t,
                                onClick = { tab = t },
                                icon = { Icon(t.icon, contentDescription = null) },
                                label = { Text(stringResource(t.label)) },
                            )
                        }
                    }
                },
            ) { padding ->
                val mod = Modifier.padding(bottom = padding.calculateBottomPadding())
                when (tab) {
                    Tab.DEVICES -> DevicesScreen(mod, onOpenDevice = { openDeviceId = it })
                    Tab.AUTOMATIONS -> AutomationsScreen(mod)
                    Tab.HISTORY -> HistoryScreen(mod)
                    Tab.SETTINGS -> SettingsScreen(mod)
                }
            }
        }
    }
}
