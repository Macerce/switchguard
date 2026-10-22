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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.macerce.switchguard.core.DeviceSnapshot
import com.macerce.switchguard.core.Scene
import com.macerce.switchguard.core.SceneAction
import com.macerce.switchguard.desktop.i18n.Strings
import com.macerce.switchguard.desktop.ui.Hue
import com.macerce.switchguard.desktop.ui.HueIcon
import com.macerce.switchguard.desktop.ui.PageHeader
import com.macerce.switchguard.desktop.ui.RowDivider
import com.macerce.switchguard.desktop.ui.SectionCard
import com.macerce.switchguard.desktop.ui.readableWidth
import com.macerce.switchguard.desktop.ui.tileColor
import java.util.UUID

/** Yeni sahne düzenleyicisinin işareti. */
internal const val NEW_SCENE = "new-scene"

/**
 * Otomasyon sekmesinin üstündeki sahneler (telefondaki `ScenesStrip`): tıklayınca çalışır, kalemle düzenlenir, sonda
 * "Sahne ekle". Masaüstünde yatay kaydırma yerine satıra sığmayan kart alta iner.
 */
@Composable
fun ScenesStrip(scenes: List<Scene>, strings: Strings, onRun: (Scene) -> Unit, onEdit: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        scenes.forEach { s ->
            val shape = RoundedCornerShape(12.dp)
            Column(
                Modifier.width(180.dp).clip(shape).background(tileColor())
                    .clickable { onRun(s) }
                    .padding(start = 14.dp, top = 10.dp, bottom = 8.dp, end = 4.dp)
                    .testTag("scene-${s.id}"),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HueIcon(Icons.Rounded.PlayArrow, Hue.PURPLE, size = 28.dp)
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { onEdit(s.id) }, modifier = Modifier.testTag("edit-scene-${s.id}")) {
                        Icon(Icons.Rounded.Edit, strings["scene_edit"], tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Text(s.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    strings.plural("scene_devices", s.actions.size, s.actions.size),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        val shape = RoundedCornerShape(12.dp)
        Box(
            Modifier.width(140.dp).heightIn(min = 92.dp).clip(shape).background(tileColor())
                .clickable { onEdit(NEW_SCENE) }.testTag("add-scene"),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.Add, null, tint = MaterialTheme.colorScheme.primary)
                Text(strings["scene_add"], style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

/** Sahne düzenleyici (telefondaki `SceneEditor`): ad ve her kanal için "Dokunma / Aç / Kapat". Esc geri döner. */
@Composable
fun SceneEditor(
    initial: Scene?, devices: List<DeviceSnapshot>, strings: Strings, modifier: Modifier,
    onSave: (Scene) -> Unit, onDelete: () -> Unit, onBack: () -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    // "cihaz/kanal" → true (aç) / false (kapat); yoksa dokunma.
    val choice = remember {
        mutableStateMapOf<String, Boolean>().apply { initial?.actions?.forEach { put("${it.deviceId}/${it.channel}", it.on) } }
    }
    var confirmDelete by remember { mutableStateOf(false) }
    val valid = name.isNotBlank() && choice.isNotEmpty()
    Box(
        modifier.fillMaxSize().onPreviewKeyEvent {
            if (it.type == KeyEventType.KeyDown && it.key == Key.Escape) { onBack(); true } else false
        },
    ) {
        Column(Modifier.align(Alignment.TopCenter).readableWidth().fillMaxSize()) {
            PageHeader(
                strings[if (initial == null) "scene_new" else "scene_edit"], onBack = onBack,
                actions = {
                    if (initial != null) {
                        IconButton(onClick = { confirmDelete = true }, modifier = Modifier.testTag("delete-scene")) {
                            Icon(Icons.Rounded.Delete, strings["action_delete"])
                        }
                    }
                },
            )
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
                TextField(
                    value = name, onValueChange = { name = it.take(40) },
                    label = { Text(strings["scene_name"]) },
                    placeholder = { Text(strings["scene_name_hint"]) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = tileColor(), unfocusedContainerColor = tileColor(),
                        focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                    ),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).testTag("scene-name"),
                )
                Text(
                    strings["scene_desc"], style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )
                SectionCard {
                    val rows = devices.flatMap { d -> d.switches.keys.sorted().map { d to it } }
                    if (rows.isEmpty()) Text(strings["no_devices"], Modifier.padding(20.dp))
                    rows.forEachIndexed { i, (d, ch) ->
                        if (i > 0) RowDivider(16.dp)
                        val key = "${d.id}/$ch"
                        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (d.isMultiChannel) strings.format("label_channel", d.name, ch + 1) else d.name,
                                style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            val options = listOf<Boolean?>(null, true, false)
                            SingleChoiceSegmentedButtonRow(Modifier.widthIn(min = 300.dp)) {
                                options.forEachIndexed { idx, o ->
                                    SegmentedButton(
                                        selected = choice[key] == o,
                                        onClick = { if (o == null) choice.remove(key) else choice[key] = o },
                                        shape = SegmentedButtonDefaults.itemShape(idx, options.size),
                                        modifier = Modifier.testTag("scene-$key-$idx"),
                                    ) {
                                        Text(strings[when (o) { null -> "scene_skip"; true -> "auto_action_turn_on"; false -> "auto_action_turn_off" }], maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                }
                Button(
                    enabled = valid,
                    onClick = {
                        val actions = choice.map { (k, on) -> SceneAction(k.substringBeforeLast('/'), k.substringAfterLast('/').toInt(), on) }
                        onSave(Scene(initial?.id ?: UUID.randomUUID().toString(), name.trim(), actions))
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp).height(44.dp).testTag("save-scene"),
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
            title = { Text(strings["scene_delete_title"]) },
            text = { Text(name) },
            confirmButton = { TextButton(onClick = { confirmDelete = false; onDelete() }) { Text(strings["action_delete"]) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(strings["action_cancel"]) } },
        )
    }
}
