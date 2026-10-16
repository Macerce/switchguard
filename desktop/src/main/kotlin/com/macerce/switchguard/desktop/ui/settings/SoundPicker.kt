package com.macerce.switchguard.desktop.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.macerce.switchguard.desktop.AppGraph
import com.macerce.switchguard.desktop.i18n.Strings
import com.macerce.switchguard.desktop.platform.Sounds
import com.macerce.switchguard.desktop.ui.InfoRow
import com.macerce.switchguard.desktop.ui.RowDivider
import com.macerce.switchguard.desktop.sound.ImportResult
import com.macerce.switchguard.desktop.sound.Reason
import com.macerce.switchguard.desktop.sound.SoundImport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.awt.Frame
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Ses seçici: yerleşik sesler ve yüklenmiş sesler (`user:`); her birinde ▶ dinle. Satıra tıklamak sesi hemen seçer
 * ve kaydeder ([onPick]); "Dosyadan yükle…" WAV/MP3'ü içe aktarır ([SoundImport]) ve yeni sesi seçer.
 * [default] (`Sounds.DEFAULT_ALARM` / `DEFAULT_NOTIFY`) listede önce gelen yerleşik sesi ve seçili değer null iken
 * hangisinin işaretleneceğini belirler.
 */
@Composable
fun SoundPickerDialog(
    graph: AppGraph,
    strings: Strings,
    title: String,
    current: String?,
    default: String,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val importer = remember { SoundImport(graph.soundsDir) }
    var loaded by remember { mutableStateOf(importer.list()) }
    var selected by remember { mutableStateOf(current ?: default) }
    var message by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val builtins = remember(default) {
        val all = Sounds.BUILTIN.map { Sounds.BUILTIN_PREFIX + it }
        listOf(default) + (all - default)
    }

    fun pick(id: String) {
        selected = id
        onPick(id)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    items(builtins + loaded, key = { it }) { id ->
                        Row(
                            Modifier.fillMaxWidth().clickable { pick(id) }.padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = id == selected, onClick = { pick(id) })
                            Text(Sounds.displayName(id, strings), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                            IconButton(onClick = { graph.sounds.once(id, graph.store.alarmVolume) }) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = strings["desktop_sound_play"])
                            }
                        }
                    }
                }
                OutlinedButton(enabled = !busy, onClick = {
                    val file = chooseSoundFile(strings["desktop_sound_load"]) ?: return@OutlinedButton
                    busy = true
                    message = null
                    scope.launch {
                        val result = withContext(Dispatchers.IO) { importer.import(file) }
                        busy = false
                        when (result) {
                            is ImportResult.Ok -> {
                                loaded = importer.list()
                                pick(result.id)
                                message = if (result.trimmed) strings["desktop_sound_trimmed"] else null
                            }
                            is ImportResult.Failed -> message = when (result.reason) {
                                Reason.TOO_BIG -> strings["desktop_sound_too_big"]
                                Reason.NOT_AUDIO -> strings["desktop_sound_not_audio"]
                                Reason.IO -> strings.format("desktop_sound_io", result.detail)
                            }
                        }
                    }
                }) { Text(strings["desktop_sound_load"]) }
                message?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(strings["action_done"]) } },
    )
}

/** Windows dosya seçicisi (WAV/MP3); vazgeçilirse null. Arayüz iş parçacığından çağrılır (kalıcı pencere). */
fun chooseSoundFile(title: String): Path? {
    val dialog = FileDialog(null as Frame?, title, FileDialog.LOAD).apply {
        file = "*.wav;*.mp3" // Windows'ta süzgeç olarak çalışır
        isMultipleMode = false
    }
    dialog.isVisible = true
    val dir = dialog.directory ?: return null
    val name = dialog.file ?: return null
    return Paths.get(dir, name)
}

/** Ayarlar → Alarm: alarm sesi ve bildirim sesi satırları (kart içinde, aralarında ayraç). */
@Composable
fun SoundRows(graph: AppGraph, strings: Strings, alarmSound: String?, notifySound: String?) {
    var editing by remember { mutableStateOf<String?>(null) } // "alarm" / "notify"
    InfoRow(
        Icons.Rounded.MusicNote, strings["alarm_sound"], Sounds.displayName(alarmSound ?: Sounds.DEFAULT_ALARM, strings),
        Modifier.clickable { editing = "alarm" },
    )
    RowDivider()
    InfoRow(
        Icons.Rounded.Notifications, strings["desktop_notify_sound"], Sounds.displayName(notifySound ?: Sounds.DEFAULT_NOTIFY, strings),
        Modifier.clickable { editing = "notify" },
    )
    when (editing) {
        "alarm" -> SoundPickerDialog(
            graph, strings, strings["alarm_sound"], alarmSound, Sounds.DEFAULT_ALARM,
            onPick = { graph.store.alarmSoundUri = it }, onDismiss = { editing = null },
        )
        "notify" -> SoundPickerDialog(
            graph, strings, strings["desktop_notify_sound"], notifySound, Sounds.DEFAULT_NOTIFY,
            onPick = { graph.store.notifySoundUri = it }, onDismiss = { editing = null },
        )
    }
}
