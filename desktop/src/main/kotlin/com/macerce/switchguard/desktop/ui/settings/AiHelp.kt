package com.macerce.switchguard.desktop.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.macerce.switchguard.desktop.i18n.Strings
import com.macerce.switchguard.desktop.platform.Links
import com.macerce.switchguard.desktop.ui.TextButton

/**
 * Yardım ve hakkında → "Yapay zekâ ile yardım" (telefon 2.32 `ui/AiHelp.kt`): ChatGPT/Gemini/Claude'a yapıştırılacak
 * hazır istemler. İstem = masaüstü uygulamasının tarifi (`desktop_ai_guide`: kenar çubuğu, Ctrl kısayolları, PC'deki
 * ayarlar) + görev. Herkese aynıdır: cihaz adı ya da kişisel bilgi eklenmez. Masaüstünde yalnızca Kopyala var.
 */
private enum class AiPrompt(val icon: ImageVector, val title: String, val desc: String, val body: String) {
    ASK(Icons.Rounded.Search, "ai_p_ask", "ai_p_ask_desc", "ai_p_ask_body"),
    SETUP(Icons.Rounded.Tune, "ai_p_setup", "ai_p_setup_desc", "ai_p_setup_body"),
    COMMANDS(Icons.AutoMirrored.Rounded.Chat, "ai_p_commands", "ai_p_commands_desc", "ai_p_commands_body"),
    IDEAS(Icons.Rounded.Lightbulb, "ai_p_ideas", "ai_p_ideas_desc", "ai_p_ideas_body"),
    FIX(Icons.Rounded.Build, "ai_p_fix", "ai_p_fix_desc", "desktop_ai_p_fix_body"),
}

/** Uygulamanın dili kendi adıyla ("Türkçe", "Deutsch"): yapay zekâ bu dilde yanıt versin. */
private fun appLanguage(strings: Strings): String =
    strings.locale.getDisplayLanguage(strings.locale).replaceFirstChar { it.titlecase(strings.locale) }

private fun buildPrompt(strings: Strings, p: AiPrompt): String =
    strings.format("desktop_ai_guide", appLanguage(strings), appVersion()) + "\n\n" + strings[p.body]

@Composable
fun AiHelpDialog(strings: Strings, onDismiss: () -> Unit) {
    var open by remember { mutableStateOf<AiPrompt?>(null) }
    var copied by remember { mutableStateOf(false) }
    val current = open
    if (current == null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            modifier = Modifier.widthIn(max = 600.dp),
            title = { Text(strings["ai_help"]) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(strings["ai_help_intro"], style = MaterialTheme.typography.bodyMedium)
                    AiPrompt.entries.forEach { p ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        ) {
                            Row(
                                Modifier.clickable { copied = false; open = p }.padding(12.dp).testTag("ai-${p.name}"),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Icon(p.icon, null, tint = MaterialTheme.colorScheme.primary)
                                Column {
                                    Text(strings[p.title], style = MaterialTheme.typography.titleSmall)
                                    Text(strings[p.desc], style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                    Text(
                        strings["ai_help_privacy"],
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            },
            confirmButton = { TextButton(onClick = onDismiss) { Text(strings["action_done"]) } },
        )
    } else {
        val text = remember(current, strings) { buildPrompt(strings, current) }
        AlertDialog(
            onDismissRequest = { open = null },
            modifier = Modifier.widthIn(max = 640.dp),
            title = { Text(strings[current.title]) },
            text = {
                Column {
                    Text(strings["desktop_ai_prompt_hint"], style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp).heightIn(max = 340.dp),
                    ) {
                        SelectionContainer {
                            Text(text, style = MaterialTheme.typography.bodySmall, modifier = Modifier.verticalScroll(rememberScrollState()).padding(12.dp))
                        }
                    }
                    Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = {
                            runCatching { Links.copyToClipboard(text) }
                            copied = true
                        }, modifier = Modifier.testTag("ai-copy")) {
                            Icon(Icons.Rounded.ContentCopy, null)
                            Text(strings["ai_copy"], Modifier.padding(start = 6.dp))
                        }
                    }
                    if (copied) Text(strings["ai_copied"], style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            },
            confirmButton = { TextButton(onClick = { open = null }) { Text(strings["action_back"]) } },
        )
    }
}
