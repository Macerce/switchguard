package com.macerce.switchguard.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SystemUpdateAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import java.util.Locale
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
    val progress by updater.progress.collectAsState()
    val locale = LocalStrings.current.locale
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
        Column(Modifier.weight(1f)) {
            Text(
                when (phase) {
                    Updater.Phase.DOWNLOADING -> str("update_downloading", r.version)
                    Updater.Phase.FAILED -> str("update_failed")
                    Updater.Phase.IDLE -> str("update_available", r.version)
                },
                color = scheme.onPrimaryContainer,
                style = MaterialTheme.typography.bodyMedium,
            )
            val p = progress
            if (phase == Updater.Phase.DOWNLOADING && p != null) {
                val fraction = p.total?.let { (p.bytes.toFloat() / it).coerceIn(0f, 1f) }
                if (fraction != null) {
                    LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
                } else {
                    LinearProgressIndicator(Modifier.fillMaxWidth().padding(vertical = 4.dp))
                }
                val done = mb(p.bytes, locale)
                val speed = mb(p.bytesPerSec.toLong(), locale)
                Text(
                    if (p.total != null && fraction != null) {
                        str("update_progress", done, mb(p.total, locale), speed, (fraction * 100).toInt())
                    } else {
                        str("update_progress_unknown", done, speed)
                    },
                    color = scheme.onPrimaryContainer,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
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

/** Bayt → "12,3" (MB, seçili dilin ondalık ayracıyla). */
private fun mb(bytes: Long, locale: Locale): String = String.format(locale, "%.1f", bytes / 1_048_576.0)
