package com.macerce.switchguard.desktop.ui.automations

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Workspaces
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.macerce.switchguard.core.DeviceGroup
import com.macerce.switchguard.core.DeviceSnapshot
import com.macerce.switchguard.core.GroupMember
import com.macerce.switchguard.desktop.i18n.Strings
import com.macerce.switchguard.desktop.ui.Button
import com.macerce.switchguard.desktop.ui.Hue
import com.macerce.switchguard.desktop.ui.HueIcon
import com.macerce.switchguard.desktop.ui.PageHeader
import com.macerce.switchguard.desktop.ui.RowDivider
import com.macerce.switchguard.desktop.ui.SectionCard
import com.macerce.switchguard.desktop.ui.TextButton
import com.macerce.switchguard.desktop.ui.readableWidth
import com.macerce.switchguard.desktop.ui.tileColor
import java.util.UUID

/** Yeni grup düzenleyicisinin işareti. */
internal const val NEW_GROUP = "new-group"

/**
 * Otomasyon sekmesindeki gruplar (telefondaki `GroupsStrip`): tıklayınca düzenlenir, sonda "Grup ekle". Gruplar
 * Cihazlar ekranında bir cihaz gibi açılıp kapanır.
 */
@Composable
fun GroupsStrip(groups: List<DeviceGroup>, devices: List<DeviceSnapshot>, strings: Strings, onEdit: (String) -> Unit) {
    val byId = remember(devices) { devices.associateBy { it.id } }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        groups.forEach { g ->
            val shape = RoundedCornerShape(6.dp)
            val (on, total) = g.counts(byId)
            Column(
                Modifier.width(180.dp).heightIn(min = 92.dp).clip(shape).background(tileColor())
                    .clickable { onEdit(g.id) }
                    .padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 14.dp)
                    .testTag("group-${g.id}"),
            ) {
                HueIcon(Icons.Rounded.Workspaces, Hue.BLUE, size = 28.dp)
                Spacer(Modifier.height(8.dp))
                Text(g.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    strings.format("channels_on_short", on, total),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        val shape = RoundedCornerShape(6.dp)
        Box(
            Modifier.width(140.dp).heightIn(min = 92.dp).clip(shape).background(tileColor())
                .clickable { onEdit(NEW_GROUP) }.testTag("add-group"),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.Add, null, tint = MaterialTheme.colorScheme.primary)
                Text(strings["group_add"], style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

/** Grup düzenleyici (telefondaki `GroupEditor`): ad ve gruba girecek cihazlar/kanallar. Esc geri döner. */
@Composable
fun GroupEditor(
    initial: DeviceGroup?, devices: List<DeviceSnapshot>, strings: Strings, modifier: Modifier,
    onSave: (DeviceGroup) -> Unit, onDelete: () -> Unit, onBack: () -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    val chosen = remember { mutableStateListOf<GroupMember>().apply { initial?.members?.let { addAll(it) } } }
    var confirmDelete by remember { mutableStateOf(false) }
    val valid = name.isNotBlank() && chosen.isNotEmpty()
    Box(
        modifier.fillMaxSize().onPreviewKeyEvent {
            if (it.type == KeyEventType.KeyDown && it.key == Key.Escape) { onBack(); true } else false
        },
    ) {
        Column(Modifier.align(Alignment.TopCenter).readableWidth().fillMaxSize()) {
            PageHeader(
                strings[if (initial == null) "group_new" else "group_edit"], onBack = onBack,
                actions = {
                    if (initial != null) {
                        IconButton(onClick = { confirmDelete = true }, modifier = Modifier.testTag("delete-group")) {
                            Icon(Icons.Rounded.Delete, strings["action_delete"])
                        }
                    }
                },
            )
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
                TextField(
                    value = name, onValueChange = { name = it.take(40) },
                    label = { Text(strings["group_name"]) },
                    placeholder = { Text(strings["group_name_hint"]) },
                    singleLine = true,
                    shape = RoundedCornerShape(6.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = tileColor(), unfocusedContainerColor = tileColor(),
                        focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                    ),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).testTag("group-name"),
                )
                Text(
                    strings["group_desc"], style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )
                SectionCard {
                    val rows = devices.filterNot { DeviceGroup.isGroup(it.id) }.flatMap { d -> d.switches.keys.sorted().map { d to it } }
                    if (rows.isEmpty()) Text(strings["no_devices"], Modifier.padding(20.dp))
                    rows.forEachIndexed { i, (d, ch) ->
                        if (i > 0) RowDivider(16.dp)
                        val member = GroupMember(d.id, ch)
                        val checked = member in chosen
                        Row(
                            Modifier.fillMaxWidth()
                                .toggleable(checked, role = Role.Checkbox) { if (it) chosen.add(member) else chosen.remove(member) }
                                .padding(horizontal = 16.dp, vertical = 2.dp)
                                .testTag("group-${d.id}/$ch"),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                if (d.isMultiChannel) strings.format("label_channel", d.name, ch + 1) else d.name,
                                style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            // Satırın tamamı tıklanabilir; kutu yalnızca durumu gösterir.
                            Checkbox(checked = checked, onCheckedChange = null, modifier = Modifier.padding(8.dp))
                        }
                    }
                }
                if (chosen.isNotEmpty()) {
                    Text(
                        strings.plural("scene_devices", chosen.size, chosen.size),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp),
                    )
                }
                Button(
                    enabled = valid,
                    onClick = { onSave(DeviceGroup(initial?.id ?: UUID.randomUUID().toString(), name.trim(), chosen.toList())) },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp).height(36.dp).testTag("save-group"),
                ) {
                    Icon(Icons.Rounded.Check, null)
                    Spacer(Modifier.size(8.dp))
                    Text(strings["action_save"])
                }
            }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(strings["group_delete_title"]) },
            text = { Text(name) },
            confirmButton = { TextButton(onClick = { confirmDelete = false; onDelete() }) { Text(strings["action_delete"]) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(strings["action_cancel"]) } },
        )
    }
}
