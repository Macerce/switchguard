package com.macerce.switchguard.desktop.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.macerce.switchguard.desktop.CrashLog
import com.macerce.switchguard.desktop.Log
import com.macerce.switchguard.desktop.i18n.Strings
import com.macerce.switchguard.desktop.platform.Links
import com.macerce.switchguard.desktop.ui.settings.CONTACT_EMAIL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.net.URI
import java.net.URLEncoder

/**
 * Telefon 2.33 `CrashPromptHost`: önceki çalışmada beklenmedik hata olduysa açılışta bir kez sorar, rapor
 * gönderilsin mi. "Gönder" e-posta uygulamasında hazır ileti açar; açılamazsa rapor panoya kopyalanır ve söylenir.
 * Aynı hata ikinci kez sorulmaz.
 */
@Composable
fun CrashPromptHost(strings: Strings) {
    var pending by remember { mutableStateOf<Pair<String, String>?>(null) }
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { pending = withContext(Dispatchers.IO) { CrashLog.pending() } }
    val (name, report) = pending ?: return
    val close = {
        CrashLog.markSeen(name)
        pending = null
    }
    AlertDialog(
        onDismissRequest = close,
        icon = { Icon(Icons.Rounded.BugReport, null) },
        title = { Text(strings["desktop_crash_title"]) },
        text = { Text(if (copied) strings.format("desktop_crash_copied", CONTACT_EMAIL) else strings["desktop_crash_text"]) },
        confirmButton = {
            if (copied) {
                TextButton(onClick = close) { Text(strings["action_done"]) }
            } else {
                TextButton(modifier = Modifier.testTag("crash-send"), onClick = {
                    if (sendMail(report)) {
                        close()
                    } else {
                        runCatching { Links.copyToClipboard(report) }
                        copied = true
                    }
                }) { Text(strings["crash_prompt_send"]) }
            }
        },
        dismissButton = { if (!copied) TextButton(onClick = close) { Text(strings["action_later"]) } },
    )
}

/** Varsayılan e-posta uygulamasında rapor iletisi; açılamazsa false. */
private fun sendMail(report: String): Boolean = try {
    val desktop = Desktop.getDesktop()
    if (!desktop.isSupported(Desktop.Action.MAIL)) throw UnsupportedOperationException("mail not supported")
    fun enc(s: String) = URLEncoder.encode(s, Charsets.UTF_8).replace("+", "%20")
    desktop.mail(URI("mailto:$CONTACT_EMAIL?subject=${enc("SwitchGuard Windows problem report")}&body=${enc(CrashLog.short(report))}"))
    true
} catch (e: Exception) {
    Log.w("CrashPrompt", "could not open a mail app: $e")
    false
}
