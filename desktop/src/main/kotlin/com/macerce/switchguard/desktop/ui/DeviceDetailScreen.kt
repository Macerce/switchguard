package com.macerce.switchguard.desktop.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.macerce.switchguard.desktop.AppGraph

/** Cihaz sayfası (telefondaki DeviceDetailScreen). Yer tutucu: Cihazlar/Geçmiş işinde yazılacak. */
@Composable
fun DeviceDetailScreen(graph: AppGraph, deviceId: String, onBack: () -> Unit) {
    val version by graph.store.version.collectAsState()
    val device = graph.store.snapshots.firstOrNull { it.id == deviceId }
    Column(Modifier.fillMaxSize()) {
        PageHeader(device?.name ?: str("device_missing"), onBack = onBack)
    }
}
