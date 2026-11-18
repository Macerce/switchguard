package com.macerce.switchguard.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.macerce.switchguard.R
import com.macerce.switchguard.data.CrashLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Önceki çalışmada çökme olduysa açılışta bir kez sorar: rapor gönderilsin mi? Kullanıcı Ayarlar'ı
 * kurcalamadan da hata bize ulaşır. Görülen en yeni çökme ayrı dosyada tutulur; aynı çökme ikinci kez sorulmaz.
 */
object CrashPrompt {
    private fun prefs(context: Context) = context.getSharedPreferences("switchguard_crash", Context.MODE_PRIVATE)

    /** Sorulacak yeni çökme varsa adı. */
    fun pending(context: Context): String? {
        val latest = CrashLog.latestName(context) ?: return null
        return latest.takeIf { it != prefs(context).getString("seen", null) }
    }

    fun markSeen(context: Context, name: String) {
        prefs(context).edit().putString("seen", name).apply()
    }
}

@Composable
fun CrashPromptHost() {
    val context = LocalContext.current
    var pending by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { pending = withContext(Dispatchers.IO) { CrashPrompt.pending(context) } }
    val noMail = stringResource(R.string.crash_report_no_mail)
    val name = pending ?: return
    val close = { CrashPrompt.markSeen(context, name); pending = null }
    AlertDialog(
        onDismissRequest = close,
        icon = { Icon(Icons.Rounded.BugReport, null) },
        title = { Text(stringResource(R.string.crash_prompt_title)) },
        text = { Text(stringResource(R.string.crash_prompt_text)) },
        confirmButton = {
            TextButton(
                modifier = Modifier.tvFocusRing(),
                onClick = {
                    close()
                    if (!Actions.sendProblemReport(context)) {
                        Toast.makeText(context, noMail, Toast.LENGTH_LONG).show()
                    }
                },
            ) { Text(stringResource(R.string.crash_prompt_send)) }
        },
        dismissButton = {
            TextButton(modifier = Modifier.tvFocusRing(), onClick = close) { Text(stringResource(R.string.action_later)) }
        },
    )
}
