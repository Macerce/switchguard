package com.macerce.switchguard.desktop.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.macerce.switchguard.desktop.AppGraph
import com.macerce.switchguard.desktop.AppState.Tab
import com.macerce.switchguard.desktop.i18n.Strings
import com.macerce.switchguard.desktop.ui.automations.AutomationsScreen
import com.macerce.switchguard.desktop.ui.settings.SettingsScreen

/** Sol menü (Cihazlar · Otomasyonlar · Ayarlar) ve seçili ekran. Seçim [com.macerce.switchguard.desktop.AppState.tab]'da: pencere yeniden kurulsa da korunur. */
@Composable
fun AppNav(graph: AppGraph, strings: Strings) {
    val tab by graph.appState.tab.collectAsState()
    Row(Modifier.fillMaxSize()) {
        NavigationRail(Modifier.fillMaxHeight().padding(top = 8.dp)) {
            NavigationRailItem(
                selected = tab == Tab.DEVICES,
                onClick = { graph.appState.tab.value = Tab.DEVICES },
                icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                label = { Text(strings["tab_devices"]) },
            )
            NavigationRailItem(
                selected = tab == Tab.AUTOMATIONS,
                onClick = { graph.appState.tab.value = Tab.AUTOMATIONS },
                icon = { Icon(Icons.Filled.Refresh, contentDescription = null) },
                label = { Text(strings["tab_automations"]) },
            )
            NavigationRailItem(
                selected = tab == Tab.SETTINGS,
                onClick = { graph.appState.tab.value = Tab.SETTINGS },
                icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                label = { Text(strings["tab_settings"]) },
            )
        }
        VerticalDivider()
        when (tab) {
            Tab.DEVICES -> DevicesScreen(graph, strings, Modifier.weight(1f))
            Tab.AUTOMATIONS -> AutomationsScreen(graph, strings, Modifier.weight(1f))
            Tab.SETTINGS -> SettingsScreen(graph, strings)
        }
    }
}
