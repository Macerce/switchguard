package com.macerce.switchguard.ui

import android.widget.Toast
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.macerce.switchguard.R
import com.macerce.switchguard.core.DeviceSnapshot
import com.macerce.switchguard.core.Scene
import com.macerce.switchguard.core.SceneAction
import com.macerce.switchguard.ui.theme.tileColor
import kotlinx.coroutines.launch
import java.util.UUID

/** Otomasyon sekmesinin üstündeki sahne şeridi: dokununca çalışır, kalemle düzenlenir, sonda "Sahne ekle". */
@Composable
fun ScenesStrip(scenes: List<Scene>, onEdit: (String) -> Unit) {
    val context = LocalContext.current
    val resources = androidx.compose.ui.platform.LocalResources.current
    val scope = rememberCoroutineScope()
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(scenes, key = { it.id }) { s ->
            val shape = RoundedCornerShape(20.dp)
            Column(
                Modifier.width(150.dp).clip(shape).background(tileColor()).tvFocusRing(shape)
                    .clickable {
                        scope.launch {
                            val (ok, failed) = Actions.runScene(context, s)
                            Toast.makeText(context, resources.getString(R.string.scene_ran, s.name, ok, failed), Toast.LENGTH_SHORT).show()
                        }
                    }
                    .padding(start = 14.dp, top = 12.dp, bottom = 6.dp, end = 4.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HueIcon(Icons.Rounded.PlayArrow, Hue.PURPLE, size = 30.dp)
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { onEdit(s.id) }, modifier = Modifier.tvFocusRing()) {
                        Icon(Icons.Rounded.Edit, stringResource(R.string.scene_edit), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Text(s.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    androidx.compose.ui.res.pluralStringResource(R.plurals.scene_devices, s.actions.size, s.actions.size),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp),
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
                    Text(stringResource(R.string.scene_add), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

/** Sahne düzenleyici: ad ve her kanal için "Dokunma / Aç / Kapat". */
@Composable
fun SceneEditor(initial: Scene?, devices: List<DeviceSnapshot>, onSave: (Scene) -> Unit, onDelete: () -> Unit, onBack: () -> Unit) {
    var name by rememberSaveable { mutableStateOf(initial?.name ?: "") }
    // "cihaz/kanal" → true (aç) / false (kapat); yoksa dokunma.
    val choice = remember {
        mutableStateMapOf<String, Boolean>().apply { initial?.actions?.forEach { put("${it.deviceId}/${it.channel}", it.on) } }
    }
    var confirmDelete by remember { mutableStateOf(false) }
    val valid = name.isNotBlank() && choice.isNotEmpty()
    Column(Modifier.fillMaxSize()) {
        AppTopBar(
            stringResource(if (initial == null) R.string.scene_new else R.string.scene_edit),
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
                label = { Text(stringResource(R.string.scene_name)) },
                placeholder = { Text(stringResource(R.string.scene_name_hint)) },
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
                stringResource(R.string.scene_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            SectionCard {
                val rows = devices.flatMap { d -> d.switches.keys.sorted().map { d to it } }
                if (rows.isEmpty()) Text(stringResource(R.string.no_devices), Modifier.padding(20.dp))
                rows.forEachIndexed { i, (d, ch) ->
                    if (i > 0) RowDivider(16.dp)
                    val key = "${d.id}/$ch"
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                        Text(
                            if (d.isMultiChannel) stringResource(R.string.label_channel, d.name, ch + 1) else d.name,
                            style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.height(6.dp))
                        val options = listOf<Boolean?>(null, true, false)
                        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                            options.forEachIndexed { idx, o ->
                                SegmentedButton(
                                    selected = choice[key] == o,
                                    onClick = { if (o == null) choice.remove(key) else choice[key] = o },
                                    shape = SegmentedButtonDefaults.itemShape(idx, options.size),
                                    modifier = Modifier.tvFocusRing(),
                                ) {
                                    Text(stringResource(when (o) { null -> R.string.scene_skip; true -> R.string.auto_action_turn_on; false -> R.string.auto_action_turn_off }), maxLines = 1)
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
            title = stringResource(R.string.scene_delete_title), body = name, confirm = stringResource(R.string.action_delete),
            onConfirm = { confirmDelete = false; onDelete() }, onDismiss = { confirmDelete = false },
        )
    }
}

