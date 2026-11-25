package com.macerce.switchguard.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.macerce.switchguard.R
import com.macerce.switchguard.core.Automation
import com.macerce.switchguard.core.RuleShare
import com.macerce.switchguard.data.Store

/** Kuralların başvurabileceği her şeyin adı: cihaz, grup, yardımcı, sahne, döngü, bölge, kural. */
fun ruleRefNames(store: Store): Map<String, String> = buildMap {
    store.snapshotsForRules.forEach { put(it.id, it.name) }
    store.scenes.forEach { put(it.id, it.name) }
    store.cycles.forEach { put(it.id, it.name) }
    store.zones.forEach { put(it.id, it.name) }
    store.automations.forEach { put(it.id, it.name) }
}

/** Kuralı paylaş: QR kod (sığıyorsa), metni kopyala ya da Android'in paylaş menüsüyle gönder. */
@Composable
fun RuleShareDialog(rule: Automation, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val store = remember { Store.get(context) }
    val code = remember(rule) { RuleShare.encode(rule, ruleRefNames(store)) }
    val tv = LocalIsTv.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rule_share)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.rule_share_desc), style = MaterialTheme.typography.bodyMedium)
                if (code.length <= QR_MAX) QrCode(code, Modifier.size(220.dp))
                else Text(stringResource(R.string.rule_share_too_long), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (!tv) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = { copy(context, code) }, modifier = Modifier.tvFocusRing()) { Text(stringResource(R.string.ai_copy)) }
                    FilledTonalButton(onClick = {
                        val send = Intent(Intent.ACTION_SEND).setType("text/plain")
                            .putExtra(Intent.EXTRA_SUBJECT, rule.name).putExtra(Intent.EXTRA_TEXT, "SwitchGuard · ${rule.name}\n$code")
                        runCatching { context.startActivity(Intent.createChooser(send, null)) }
                    }, modifier = Modifier.tvFocusRing()) { Text(stringResource(R.string.action_share)) }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss, modifier = Modifier.tvFocusRing()) { Text(stringResource(R.string.action_done)) } },
    )
}

/** Paylaşılan kuralı içe aktar: yapıştırılan metinden (pano kural içeriyorsa kendiliğinden dolar). Kural kapalı eklenir. */
@Composable
fun RuleImportDialog(onDismiss: () -> Unit, onImported: (Automation) -> Unit) {
    val context = LocalContext.current
    val store = remember { Store.get(context) }
    var text by remember {
        mutableStateOf(runCatching {
            context.getSystemService(ClipboardManager::class.java).primaryClip?.getItemAt(0)?.text?.toString()
        }.getOrNull()?.let(RuleShare::find).orEmpty())
    }
    var error by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rule_import)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.rule_import_hint), style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = text, onValueChange = { text = it.take(20_000); error = false },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 96.dp), maxLines = 6,
                    isError = error, supportingText = if (error) ({ Text(stringResource(R.string.rule_import_bad)) }) else null,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val r = RuleShare.decode(text, ruleRefNames(store))
                if (r == null) { error = true; return@TextButton }
                store.saveAutomation(r.rule)
                val msg = context.getString(R.string.rule_import_done, r.rule.name) +
                    if (r.unresolved > 0) "\n" + context.resources.getQuantityString(R.plurals.rule_import_unresolved, r.unresolved, r.unresolved) else ""
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                onImported(r.rule)
            }, enabled = text.isNotBlank(), modifier = Modifier.tvFocusRing()) { Text(stringResource(R.string.rule_import_action)) }
        },
        dismissButton = { TextButton(onClick = onDismiss, modifier = Modifier.tvFocusRing()) { Text(stringResource(android.R.string.cancel)) } },
    )
}

private fun copy(context: Context, text: String) {
    context.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("SwitchGuard", text))
    Toast.makeText(context, context.getString(R.string.rule_share_copied), Toast.LENGTH_SHORT).show()
}

/** QR'a sığan en uzun metin (M düzeltme düzeyinde bayt kipi ~2300; telefon kameraları için pay bırakıldı). */
private const val QR_MAX = 1800
