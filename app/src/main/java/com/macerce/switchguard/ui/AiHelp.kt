package com.macerce.switchguard.ui

import android.content.ClipData
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.FilledTonalButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.macerce.switchguard.ui.theme.tileColor
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import java.util.Locale

/**
 * Yardım → "Yapay zekâ ile yardım": ChatGPT/Gemini/Claude'a yapıştırılacak hazır istemler.
 * Her istem = uygulamanın tarifi (menüler, özellikler, Sohbet komutları) + görev; herkese aynıdır. Cihaz adlarını
 * kullanıcı kendisi yazar, yapay zekâ gerekirse sorar. "Sohbet komutu" istemi Sohbet'in anladığı komutları yazdırır;
 * kullanıcı onları Sohbet'e yapıştırarak işlemi yaptırır.
 */
private enum class AiPrompt(val icon: ImageVector, val hue: Hue, val title: Int, val desc: Int, val body: Int) {
    ASK(Icons.Rounded.Search, Hue.BLUE, R.string.ai_p_ask, R.string.ai_p_ask_desc, R.string.ai_p_ask_body),
    SETUP(Icons.Rounded.Tune, Hue.TEAL, R.string.ai_p_setup, R.string.ai_p_setup_desc, R.string.ai_p_setup_body),
    COMMANDS(Icons.AutoMirrored.Rounded.Chat, Hue.INDIGO, R.string.ai_p_commands, R.string.ai_p_commands_desc, R.string.ai_p_commands_body),
    IDEAS(Icons.Rounded.Lightbulb, Hue.AMBER, R.string.ai_p_ideas, R.string.ai_p_ideas_desc, R.string.ai_p_ideas_body),
    FIX(Icons.Rounded.Build, Hue.ORANGE, R.string.ai_p_fix, R.string.ai_p_fix_desc, R.string.ai_p_fix_body),
}

/** Uygulamanın dili (yapay zekâ yanıtı bu dilde versin), kendi adıyla: "Türkçe", "Deutsch". */
private fun appLanguage(context: Context): String {
    val locale = context.resources.configuration.locales[0] ?: Locale.getDefault()
    return locale.getDisplayLanguage(locale).replaceFirstChar { it.titlecase(locale) }
}

/** Herkese aynı istem: uygulamanın tarifi + görev. Kullanıcının cihazları ya da kişisel bilgisi eklenmez. */
private fun buildPrompt(context: Context, p: AiPrompt): String {
    val r = context.resources
    return r.getString(R.string.ai_guide, appLanguage(context), BuildConfig.VERSION_NAME) + "\n\n" + r.getString(p.body)
}

/**
 * Tam sayfa: üstte ne işe yaradığı, altında her istem için büyük kart (renkli simge, başlık, ne yapar). Karta dokununca
 * kart açılır: istemin önizlemesi ile Kopyala ve Paylaş. Altta gizlilik notu.
 */
@Composable
internal fun AiHelpDialog(close: () -> Unit) {
    Dialog(onDismissRequest = close, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().statusBarsPadding()) {
                AppTopBar(stringResource(R.string.ai_help), navigationIcon = { BackButton(close) }, small = true)
                AiHelpBody()
            }
        }
    }
}

@Composable
private fun AiHelpBody() {
    val context = LocalContext.current
    var open by remember { mutableStateOf<AiPrompt?>(null) }
    val scheme = MaterialTheme.colorScheme
    Column(
        Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 16.dp).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.ai_help_intro), style = MaterialTheme.typography.bodyLarge, color = scheme.onSurfaceVariant)
        AiPrompt.entries.forEach { p ->
            val expanded = open == p
            val shape = RoundedCornerShape(20.dp)
            Column(
                Modifier.fillMaxWidth().tvFocusRing(shape).clip(shape).background(tileColor())
                    .clickable { open = if (expanded) null else p }.padding(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HueIcon(p.icon, p.hue, size = 44.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(p.title), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(p.desc), style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
                    }
                    Icon(
                        if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null,
                        tint = scheme.onSurfaceVariant,
                    )
                }
                AnimatedVisibility(expanded) {
                    val text = remember(p) { buildPrompt(context, p) }
                    val copied = stringResource(R.string.ai_copied)
                    Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(stringResource(R.string.ai_prompt_hint), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                        Surface(shape = RoundedCornerShape(12.dp), color = scheme.surfaceContainerHighest, modifier = Modifier.fillMaxWidth().heightIn(max = 260.dp)) {
                            Text(text, style = MaterialTheme.typography.bodySmall, modifier = Modifier.verticalScroll(rememberScrollState()).padding(12.dp))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(onClick = {
                                context.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("SwitchGuard", text))
                                Toast.makeText(context, copied, Toast.LENGTH_SHORT).show()
                            }, modifier = Modifier.tvFocusRing()) {
                                Icon(Icons.Rounded.ContentCopy, null, Modifier.size(18.dp))
                                Text(stringResource(R.string.ai_copy), Modifier.padding(start = 6.dp))
                            }
                            FilledTonalButton(onClick = {
                                val share = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
                                runCatching { context.startActivity(Intent.createChooser(share, null)) }
                            }, modifier = Modifier.tvFocusRing()) {
                                Icon(Icons.Rounded.Share, null, Modifier.size(18.dp))
                                Text(stringResource(R.string.ai_share), Modifier.padding(start = 6.dp))
                            }
                        }
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(top = 4.dp)) {
            Icon(Icons.Rounded.Lock, null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.ai_help_privacy), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
        }
    }
}
