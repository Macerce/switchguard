package com.macerce.switchguard.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.macerce.switchguard.R
import com.macerce.switchguard.core.TelegramAuth
import com.macerce.switchguard.data.Store

/** Gruplarda kontrol özetinin metni: kaç kişi seçili (hiçbiri ise "kimse"). */
@Composable
fun groupControlSummary(store: Store): String {
    val n = store.telegramRecipients.filter { TelegramAuth.isGroup(it.id) && it.control == true }.sumOf { it.controllers.size }
    return if (n == 0) stringResource(R.string.tg_group_control_nobody) else stringResource(R.string.tg_group_control_count, n)
}

/**
 * Telegram gruplarında kim aç/kapa yapabilir. Grupta kontrol varsayılan olarak kapalı; açılan grupta yalnızca
 * işaretlenen kişiler (gruba yazmış olanlar listelenir) komut verebilir. Bkz. [TelegramAuth].
 */
@Composable
fun GroupControlDialog(store: Store, onDismiss: () -> Unit) {
    val groups = remember { store.telegramRecipients.filter { TelegramAuth.isGroup(it.id) } }
    val senders = remember { store.telegramSenders }
    var edited by remember { mutableStateOf(groups.associateBy { it.id }) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.tg_group_control_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                edited.values.forEach { g ->
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(g.name.ifEmpty { g.id.toString() }, style = MaterialTheme.typography.titleSmall)
                            Text(stringResource(R.string.tg_group_control_on), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = g.control == true,
                            onCheckedChange = { on -> edited = edited + (g.id to g.copy(control = on)) },
                            modifier = Modifier.tvFocusRing(),
                        )
                    }
                    if (g.control == true) {
                        val people = senders[g.id].orEmpty()
                        // Seçilmiş ama listede olmayanlar (eski yazanlar) da gösterilsin; kimlikle.
                        val extra = g.controllers.filter { id -> people.none { it.first == id } }.map { it to "#$it" }
                        (people + extra).forEach { (id, name) ->
                            val checked = id in g.controllers
                            Row(
                                Modifier.fillMaxWidth().toggleable(checked, role = Role.Checkbox) { on ->
                                    val next = if (on) g.controllers + id else g.controllers - id
                                    edited = edited + (g.id to g.copy(controllers = next))
                                }.padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(checked = checked, onCheckedChange = null)
                                Text(name.ifEmpty { "#$id" }, style = MaterialTheme.typography.bodyLarge, fontWeight = if (checked) FontWeight.Medium else null)
                            }
                        }
                        Text(
                            stringResource(R.string.tg_group_control_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        },
        confirmButton = {
            TextButton(modifier = Modifier.tvFocusRing(), onClick = {
                store.telegramRecipients = store.telegramRecipients.map { r -> edited[r.id] ?: r }
                onDismiss()
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(modifier = Modifier.tvFocusRing(), onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
