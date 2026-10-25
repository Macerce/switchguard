package com.macerce.switchguard.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SystemUpdateAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.macerce.switchguard.desktop.AppGraph
import com.macerce.switchguard.desktop.platform.Updater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Yeni masaüstü sürümü çıktığında içeriğin üstündeki şerit. "Güncelle" kurulumu indirir, başlatır ve uygulamayı
 * kapatır; kurulum bitince uygulama yeniden açılır (bkz. [Updater]).
 */
@Composable
fun UpdateBanner(graph: AppGraph) {
    val updater = graph.updater
    val release by updater.available.collectAsState()
    val phase by updater.phase.collectAsState()
    val scope = rememberCoroutineScope()
    val r = release ?: return
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().background(scheme.primaryContainer).padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("update-banner"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.SystemUpdateAlt, null, tint = scheme.onPrimaryContainer)
        Spacer(Modifier.width(12.dp))
        Text(
            when (phase) {
                Updater.Phase.DOWNLOADING -> str("update_downloading", r.version)
                Updater.Phase.FAILED -> str("update_failed")
                Updater.Phase.IDLE -> str("update_available", r.version)
            },
            color = scheme.onPrimaryContainer,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        if (phase != Updater.Phase.DOWNLOADING) {
            TextButton(onClick = updater::dismiss) { Text(str("update_later")) }
            TextButton(modifier = Modifier.testTag("update-install"), onClick = {
                scope.launch {
                    val started = withContext(Dispatchers.IO) { updater.downloadAndLaunch() }
                    if (started) graph.appState.exitForUpdate.value = true
                }
            }) { Text(str("update_install")) }
        }
    }
}
