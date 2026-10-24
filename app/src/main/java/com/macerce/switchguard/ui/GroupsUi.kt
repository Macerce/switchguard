package com.macerce.switchguard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Workspaces
import androidx.compose.material3.Button
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.macerce.switchguard.R
import com.macerce.switchguard.core.DeviceGroup
import com.macerce.switchguard.core.DeviceSnapshot
import com.macerce.switchguard.core.GroupMember
import com.macerce.switchguard.ui.theme.tileColor
import java.util.UUID

/** Otomasyon sekmesindeki grup şeridi: dokununca düzenlenir, sonda "Grup ekle". Gruplar Cihazlar ekranında açılıp kapanır. */
@Composable
fun GroupsStrip(groups: List<DeviceGroup>, devices: List<DeviceSnapshot>, onEdit: (String) -> Unit) {
    val byId = remember(devices) { devices.associateBy { it.id } }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(groups, key = { it.id }) { g ->
            val shape = RoundedCornerShape(20.dp)
            val (on, total) = g.counts(byId)
            Column(
                Modifier.width(150.dp).heightIn(min = 104.dp).clip(shape).background(tileColor()).tvFocusRing(shape)
                    .clickable { onEdit(g.id) }
                    .padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 14.dp),
            ) {
                HueIcon(Icons.Rounded.Workspaces, Hue.BLUE, size = 30.dp)
                Spacer(Modifier.height(10.dp))
                Text(g.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    stringResource(R.string.channels_on_short, on, total),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            val shape = RoundedCornerShape(20.dp)
            Box(
                Modifier.width(120.dp).heightIn(min = 104.dp).clip(shape).background(tileColor()).tvFocusRing(shape)
                    .clickable { onEdit(NEW_AUTOMATION) },
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.Add, null, tint = MaterialTheme.colorScheme.primary)
                    Text(stringResource(R.string.group_add), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

/** Grup düzenleyici: ad ve gruba girecek cihazlar/kanallar (sahne düzenleyicisiyle aynı görünüm). */
@Composable
fun GroupEditor(initial: DeviceGroup?, devices: List<DeviceSnapshot>, onSave: (DeviceGroup) -> Unit, onDelete: () -> Unit, onBack: () -> Unit) {
    var name by rememberSaveable { mutableStateOf(initial?.name ?: "") }
    val chosen = remember { mutableStateListOf<GroupMember>().apply { initial?.members?.let { addAll(it) } } }
    var confirmDelete by remember { mutableStateOf(false) }
    val valid = name.isNotBlank() && chosen.isNotEmpty()
    Column(Modifier.fillMaxSize()) {
        AppTopBar(
            stringResource(if (initial == null) R.string.group_new else R.string.group_edit),
            navigationIcon = { BackButton(onBack) }, small = true,
            actions = {
                if (initial != null) {
                    IconButton(modifier = Modifier.tvFocusRing(), onClick = { confirmDelete = true }) {
                        Icon(Icons.Rounded.Delete, stringResource(R.string.action_delete))
                    }
                }
            },
        )
        Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
            TextField(
                value = name, onValueChange = { name = it.take(40) },
                label = { Text(stringResource(R.string.group_name)) },
                placeholder = { Text(stringResource(R.string.group_name_hint)) },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                keyboardOptions = tvKeyboardOptions(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = tileColor(), unfocusedContainerColor = tileColor(),
                    focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                ),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).tvTextField(),
            )
            Text(
                stringResource(R.string.group_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            SectionCard {
                val rows = devices.filterNot { DeviceGroup.isGroup(it.id) }.flatMap { d -> d.switches.keys.sorted().map { d to it } }
                if (rows.isEmpty()) Text(stringResource(R.string.no_devices), Modifier.padding(20.dp))
                rows.forEachIndexed { i, (d, ch) ->
                    if (i > 0) RowDivider(16.dp)
                    val member = GroupMember(d.id, ch)
                    val checked = member in chosen
                    Row(
                        Modifier.fillMaxWidth()
                            .tvFocusRing()
                            .toggleable(checked, role = Role.Checkbox) { if (it) chosen.add(member) else chosen.remove(member) }
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            if (d.isMultiChannel) stringResource(R.string.label_channel, d.name, ch + 1) else d.name,
                            style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        // Satırın tamamı dokunulabilir; kutu yalnızca durumu gösterir.
                        Checkbox(checked = checked, onCheckedChange = null, modifier = Modifier.padding(12.dp))
                    }
                }
            }
            if (chosen.isNotEmpty()) {
                Text(
                    pluralStringResource(R.plurals.scene_devices, chosen.size, chosen.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
            }
            Button(
                enabled = valid,
                onClick = { onSave(DeviceGroup(initial?.id ?: UUID.randomUUID().toString(), name.trim(), chosen.toList())) },
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp).height(56.dp).tvFocusRing(RoundedCornerShape(18.dp)),
            ) {
                Icon(Icons.Rounded.Check, null)
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.action_save))
            }
        }
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.group_delete_title), body = name, confirm = stringResource(R.string.action_delete),
            onConfirm = { confirmDelete = false; onDelete() }, onDismiss = { confirmDelete = false },
        )
    }
}
