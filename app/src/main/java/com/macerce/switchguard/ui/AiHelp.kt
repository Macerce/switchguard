package com.macerce.switchguard.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.macerce.switchguard.BuildConfig
import com.macerce.switchguard.R
import com.macerce.switchguard.data.Store
import java.util.Locale

/**
 * Yardım → "Yapay zekâ ile yardım": ChatGPT/Gemini/Claude'a yapıştırılacak hazır istemler.
 * Her istem = uygulamanın tarifi (menüler, özellikler, Sohbet komutları) + kullanıcının cihaz/grup/sahne/döngü adları
 * + görev. "Sohbet komutu" istemi yapay zekâya uygulamanın Sohbet'inin anladığı komutları yazdırır; kullanıcı onları
 * Sohbet'e yapıştırarak işlemi yaptırır. Hesap bilgisi, anahtar, konum eklenmez.
 */
private enum class AiPrompt(val icon: ImageVector, val title: Int, val desc: Int, val body: Int) {
    ASK(Icons.Rounded.Search, R.string.ai_p_ask, R.string.ai_p_ask_desc, R.string.ai_p_ask_body),
    SETUP(Icons.Rounded.Tune, R.string.ai_p_setup, R.string.ai_p_setup_desc, R.string.ai_p_setup_body),
    COMMANDS(Icons.AutoMirrored.Rounded.Chat, R.string.ai_p_commands, R.string.ai_p_commands_desc, R.string.ai_p_commands_body),
    IDEAS(Icons.Rounded.Lightbulb, R.string.ai_p_ideas, R.string.ai_p_ideas_desc, R.string.ai_p_ideas_body),
    FIX(Icons.Rounded.Build, R.string.ai_p_fix, R.string.ai_p_fix_desc, R.string.ai_p_fix_body),
}

/** Uygulamanın dili (yapay zekâ yanıtı bu dilde versin), kendi adıyla: "Türkçe", "Deutsch". */
private fun appLanguage(context: Context): String {
    val locale = context.resources.configuration.locales[0] ?: Locale.getDefault()
    return locale.getDisplayLanguage(locale).replaceFirstChar { it.titlecase(locale) }
}

/** Kullanıcının kurulumu: yalnız adlar ve yetenekler (kimlik, anahtar, konum yok). */
private fun setupContext(context: Context, store: Store): String {
    val r = context.resources
    val none = r.getString(R.string.ai_ctx_none)
    val devices = store.snapshots.joinToString("\n") { d ->
        buildList {
            if (d.isMultiChannel) add(r.getString(R.string.ai_ctx_channels, d.switches.size))
            if (d.sensorOnly) add(r.getString(R.string.ai_ctx_sensor_only))
            if (d.hasEnergy) add(r.getString(R.string.ai_ctx_power))
            if (d.hasSensors) add(d.sensors.keys.joinToString { it.name.lowercase() })
        }.let { extra -> "• ${d.name}" + if (extra.isEmpty()) "" else " (${extra.joinToString(", ")})" }
    }.ifEmpty { none }
    fun names(list: List<String>) = list.joinToString(", ").ifEmpty { none }
    return r.getString(
        R.string.ai_ctx,
        devices,
        names(store.groups.map { it.name }),
        names(store.scenes.map { it.name }),
        names(store.cycles.map { it.name }),
    )
}

private fun buildPrompt(context: Context, store: Store, p: AiPrompt): String {
    val r = context.resources
    return r.getString(R.string.ai_guide, appLanguage(context), BuildConfig.VERSION_NAME) +
        "\n\n" + setupContext(context, store) +
        "\n\n" + r.getString(p.body)
}

@Composable
internal fun AiHelpDialog(store: Store, close: () -> Unit) {
    val context = LocalContext.current
    var open by remember { mutableStateOf<AiPrompt?>(null) }
    val current = open
    if (current == null) {
        AlertDialog(
            onDismissRequest = close,
            title = { Text(stringResource(R.string.ai_help)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(stringResource(R.string.ai_help_intro), style = MaterialTheme.typography.bodyMedium)
                    AiPrompt.entries.forEach { p ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        ) {
                            Row(
                                Modifier.clickable { open = p }.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Icon(p.icon, null, tint = MaterialTheme.colorScheme.primary)
                                Column {
                                    Text(stringResource(p.title), style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        stringResource(p.desc),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        stringResource(R.string.ai_help_privacy),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            },
            confirmButton = { TextButton(onClick = close) { Text(stringResource(R.string.action_done)) } },
        )
    } else {
        val text = remember(current) { buildPrompt(context, store, current) }
        val copied = stringResource(R.string.ai_copied)
        AlertDialog(
            onDismissRequest = { open = null },
            title = { Text(stringResource(current.title)) },
            text = {
                Column {
                    Text(
                        stringResource(R.string.ai_prompt_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp).heightIn(max = 320.dp),
                    ) {
                        Text(
                            text,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.verticalScroll(rememberScrollState()).padding(12.dp),
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 12.dp)) {
                        OutlinedButton(onClick = {
                            context.getSystemService(ClipboardManager::class.java)
                                .setPrimaryClip(ClipData.newPlainText("SwitchGuard", text))
                            Toast.makeText(context, copied, Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Rounded.ContentCopy, null)
                            Text(stringResource(R.string.ai_copy), Modifier.padding(start = 6.dp))
                        }
                        OutlinedButton(onClick = {
                            val share = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
                            runCatching { context.startActivity(Intent.createChooser(share, null)) }
                        }) {
                            Icon(Icons.Rounded.Share, null)
                            Text(stringResource(R.string.ai_share), Modifier.padding(start = 6.dp))
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { open = null }) { Text(stringResource(R.string.action_back)) } },
        )
    }
}
