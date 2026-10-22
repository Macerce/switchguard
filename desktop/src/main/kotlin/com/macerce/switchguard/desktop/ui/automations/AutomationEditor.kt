package com.macerce.switchguard.desktop.ui.automations

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ElectricBolt
import androidx.compose.material.icons.rounded.PowerOff
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TimerOff
import androidx.compose.material3.AlertDialog
import com.macerce.switchguard.desktop.ui.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.macerce.switchguard.desktop.ui.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.macerce.switchguard.core.ActionKind
import com.macerce.switchguard.core.ConditionKind
import com.macerce.switchguard.core.PresenceRule
import com.macerce.switchguard.core.Scene
import com.macerce.switchguard.data.Store
import com.macerce.switchguard.desktop.ui.settings.TimeDialog
import com.macerce.switchguard.desktop.ui.settings.formatMinute
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.Switch
import com.macerce.switchguard.core.Automation
import com.macerce.switchguard.core.DeviceKinds
import com.macerce.switchguard.core.DeviceSnapshot
import com.macerce.switchguard.core.TriggerKind
import com.macerce.switchguard.desktop.i18n.Strings
import com.macerce.switchguard.desktop.ui.Hue
import com.macerce.switchguard.desktop.ui.HueIcon
import com.macerce.switchguard.desktop.ui.IconBadge
import com.macerce.switchguard.desktop.ui.PageHeader
import com.macerce.switchguard.desktop.ui.icon
import com.macerce.switchguard.desktop.ui.raisedOnTile
import com.macerce.switchguard.desktop.ui.readableWidth
import com.macerce.switchguard.desktop.ui.tileColor
import java.util.UUID

/**
 * Telefondaki 2.12 düzenleyicisi, tam sayfa: ad; "Eğer" adımı (cihaz kartları, numaralı kanal düğmeleri, olay
 * kutucukları, süreler için − / + adımlayıcı, güç eşiği alanı); "O zaman" adımı (işlem kutucukları, hedef cihaz ve
 * kanal, bekleme); "Eğer / O zaman" özeti; Kaydet. Sil başlıktadır ve önce sorar. Durum, varsayılanlar ve doğrulama
 * [AutomationForm]'da; bu dosya yalnızca alanları değiştirir. Masaüstü farkları: cihaz kartları yatay kaydırma yerine
 * satıra sığmayınca alta iner (fare tekerleği dikey kaydırır), geniş pencerede olay kutucukları üç sütun, Esc geri döner.
 *
 * Mod (telefon 2.13): uyarı modunda işlem kutucukları yalnız Alarm çal / Bildirim gönder (hedef cihaz yok), otomasyon
 * modunda yalnız Cihazı aç / kapat; başlık ve silme sorusu da moda göre. [newMode] yeni kaydın modudur; var olan kayıt
 * işleminin modunda açılır.
 */
@Composable
internal fun AutomationEditor(
    initial: Automation?,
    devices: List<DeviceSnapshot>,
    strings: Strings,
    modifier: Modifier,
    onSave: (Automation) -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
    newMode: AutomationMode = AutomationMode.ALERT,
    /** Gün doğumu ve sıcaklık kuralları için konum ve son dış sıcaklık; sahne listesi. Testlerde verilmeyebilir. */
    store: Store? = null,
    outdoorTemp: Double? = null,
) {
    val scenes = store?.scenes.orEmpty()
    var pickTime by remember { mutableStateOf<Pair<String, Int>?>(null) }
    var locationVersion by remember { mutableStateOf(0) }
    val mode = initial?.let { AutomationMode.of(it) } ?: newMode
    var form by remember { mutableStateOf(AutomationForm.from(initial, devices, mode)) }
    var confirmDelete by remember { mutableStateOf(false) }
    val f = form.normalized(devices).inMode(mode)
    val src = devices.firstOrNull { it.id == f.triggerDevice }
    val dst = devices.firstOrNull { it.id == f.actionDevice }

    Box(
        modifier.fillMaxSize().onPreviewKeyEvent {
            if (it.type == KeyEventType.KeyDown && it.key == Key.Escape) {
                onBack()
                true
            } else {
                false
            }
        },
    ) {
        Column(Modifier.align(Alignment.TopCenter).readableWidth().fillMaxSize()) {
            PageHeader(
                strings[if (initial == null) mode.newTitle else mode.editTitle],
                onBack = onBack,
                actions = {
                    if (initial != null) {
                        IconButton(onClick = { confirmDelete = true }, modifier = Modifier.testTag("delete-automation")) {
                            Icon(Icons.Rounded.Delete, strings["action_delete"])
                        }
                    }
                },
            )
            if (devices.isEmpty()) {
                Text(strings["no_devices"], Modifier.padding(24.dp))
                return@Column
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
                NameField(f.name, strings) { form = f.copy(name = it) }

                // ------------------------------------------------ Eğer
                StepCard(Icons.Rounded.Sensors, Hue.BLUE, strings["auto_if"], strings["automation_when"]) {
                    FieldLabel(strings["automation_event"])
                    OptionGrid(
                        AutomationForm.kindsFor(src), f.triggerKind, wideColumns = 4,
                        icon = { it.icon() }, label = { strings[it.shortTitleKey()] }, tag = { "event-${it.name}" },
                    ) { form = f.copy(triggerKind = it) }
                    if (f.triggerKind.usesDevice) {
                        FieldLabel(strings["automation_device"])
                        DeviceChooser(devices, f.triggerDevice, "trigger-device") { form = f.copy(triggerDevice = it.id, triggerChannel = 0) }
                        if (f.triggerKind.usesChannel && src?.isMultiChannel == true) {
                            FieldLabel(strings["automation_channel"])
                            ChannelChooser(src, f.triggerChannel, "trigger-channel") { form = f.copy(triggerChannel = it) }
                        }
                    }
                    if (f.triggerKind.isSun) {
                        // "Gün doğumundan 15 dk önce", "gün batımından 1 sa sonra".
                        Stepper(strings["sun_offset"], f.offset, Steps.OFFSET, strings, "offset", format = { offsetLabel(it, strings) }) {
                            form = f.copy(offset = it)
                        }
                    }
                    if (f.triggerKind.usesTemp) {
                        NumberField(f.temp, strings["automation_temp"], "°C", "temp", allowNegative = true) { form = f.copy(temp = it) }
                        outdoorTemp?.let {
                            Text(
                                strings.format("automation_current_temp", formatTemp(it)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                    }
                    if ((f.triggerKind.isSun || f.triggerKind.usesTemp) && store != null) {
                        androidx.compose.runtime.key(locationVersion) {
                            LocationSetup(store, strings, onChanged = { locationVersion++ }, modifier = Modifier.padding(top = 12.dp))
                        }
                    }
                    if (f.triggerKind.usesMinutes) {
                        Stepper(strings["automation_minutes_short"], f.minutes.toIntOrNull() ?: 30, Steps.MINUTES, strings, "minutes") {
                            form = f.copy(minutes = it.toString())
                        }
                    }
                    if (f.triggerKind.usesWatts) {
                        NumberField(f.watts, strings["automation_watts"], "W", "watts") { form = f.copy(watts = it) }
                    }
                    if (f.triggerKind.allowsHold) {
                        // Ör. "4 saattir hiç çalışmadı" (dondurucu arızası), "6 saattir durmadı" (kompresör).
                        Stepper(strings["automation_hold_short"], f.holdMinutes, Steps.HOLD, strings, "hold", zeroLabel = strings["automation_now"]) {
                            form = f.copy(holdMinutes = it)
                        }
                    }
                    if (f.triggerKind.usesChannelPower) {
                        Stepper(strings["automation_low_minutes_short"], f.lowMinutes.toIntOrNull() ?: 1, Steps.LOW, strings, "low-minutes") {
                            form = f.copy(lowMinutes = it.toString())
                        }
                        Stepper(strings["automation_grace_short"], f.grace.toIntOrNull() ?: 2, Steps.GRACE, strings, "grace") {
                            form = f.copy(grace = it.toString())
                        }
                    }
                    val now = when {
                        f.triggerKind.usesChannelPower -> src?.channelPowerOf(f.triggerChannel)
                        f.triggerKind.usesWatts -> src?.power
                        else -> null
                    }
                    if (now != null) {
                        Text(
                            strings.format("automation_current_power", formatWatts(now)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }

                // ------------------------------------------------ Yalnızca / Ve
                StepCard(Icons.Rounded.Tune, Hue.PURPLE, strings["auto_only"], strings["auto_only_desc"]) {
                    ToggleLine(strings["window_toggle"], f.windowOn, "window") { form = f.copy(windowOn = it) }
                    if (f.windowOn) {
                        val startTitle = strings["window_start"]
                        val endTitle = strings["window_end"]
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                            TimeChip(startTitle, formatMinute(strings, f.windowStart), Modifier.weight(1f)) { pickTime = "start" to f.windowStart }
                            TimeChip(endTitle, formatMinute(strings, f.windowEnd), Modifier.weight(1f)) { pickTime = "end" to f.windowEnd }
                        }
                        DayChips(f.windowDays, strings) { form = f.copy(windowDays = it) }
                        if (f.windowStart == f.windowEnd) {
                            Text(strings["window_all_day_hint"], style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
                        }
                    }
                    ToggleLine(strings["presence_toggle"], f.presence != null, "presence") { form = f.copy(presence = if (it) PresenceRule.AWAY else null) }
                    f.presence?.let { rule ->
                        Spacer(Modifier.height(8.dp))
                        OptionGrid(
                            PresenceRule.entries, rule, wideColumns = 2,
                            icon = { if (it == PresenceRule.AWAY) Icons.AutoMirrored.Rounded.DirectionsWalk else Icons.Rounded.Home },
                            label = { strings[if (it == PresenceRule.AWAY) "presence_away" else "presence_home"] }, tag = { "presence-${it.name}" },
                        ) { form = f.copy(presence = it) }
                    }
                    ToggleLine(strings["cond_toggle"], f.condOn, "cond") { form = f.copy(condOn = it) }
                    if (f.condOn) {
                        val condSrc = devices.firstOrNull { it.id == f.condDevice }
                        FieldLabel(strings["automation_device"])
                        DeviceChooser(devices, f.condDevice, "cond-device") { form = f.copy(condDevice = it.id, condChannel = 0) }
                        if (f.condKind.usesChannel && condSrc?.isMultiChannel == true) {
                            FieldLabel(strings["automation_channel"])
                            ChannelChooser(condSrc, f.condChannel, "cond-channel") { form = f.copy(condChannel = it) }
                        }
                        FieldLabel(strings["cond_state"])
                        OptionGrid(
                            ConditionKind.entries, f.condKind, wideColumns = 4,
                            icon = { it.icon() }, label = { strings[it.titleKey()] }, tag = { "cond-${it.name}" },
                        ) { form = f.copy(condKind = it) }
                    }
                }

                // ------------------------------------------------ O zaman
                StepCard(Icons.Rounded.Bolt, Hue.ORANGE, strings["auto_then"], strings["automation_then"]) {
                    FieldLabel(strings["automation_action"])
                    OptionGrid(
                        mode.actions, f.actionKind, wideColumns = 2,
                        icon = { it.icon() }, label = { strings[it.editorTitleKey()] }, tag = { "action-${it.name}" }, hue = { it.hue() },
                    ) { form = f.copy(actionKind = it) }
                    if (f.actionKind == ActionKind.RUN_SCENE) {
                        FieldLabel(strings["scenes_title"])
                        if (scenes.isEmpty()) {
                            Text(strings["scene_none_yet"], style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            OptionGrid(
                                scenes.map { it.id }, f.sceneId, wideColumns = 3,
                                icon = { Icons.Rounded.PlayArrow }, label = { id -> scenes.first { it.id == id }.name }, tag = { "scene-$it" },
                            ) { form = f.copy(sceneId = it) }
                        }
                    }
                    if (f.actionKind.usesDevice) {
                        FieldLabel(strings["automation_device"])
                        DeviceChooser(devices, f.actionDevice, "action-device") { form = f.copy(actionDevice = it.id, actionChannel = 0) }
                        if (dst?.isMultiChannel == true) {
                            FieldLabel(strings["automation_channel"])
                            ChannelChooser(dst, f.actionChannel, "action-channel") { form = f.copy(actionChannel = it) }
                        }
                    }
                    Stepper(
                        strings["automation_delay_short"], f.delay.toIntOrNull() ?: 0, Steps.DELAY, strings, "delay",
                        zeroLabel = strings["automation_now"],
                    ) { form = f.copy(delay = it.toString()) }
                }

                // ------------------------------------------------ Özet
                if (src != null || !f.triggerKind.usesDevice) {
                    val (whenText, thenText) = describe(f.build(initial?.id.orEmpty(), initial?.enabled ?: true), devices, strings, scenes)
                    Column(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f))
                            .padding(16.dp)
                            .testTag("automation-summary"),
                    ) {
                        Text(strings["automation_summary"], style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(6.dp))
                        Clause(strings["auto_if"], whenText)
                        Spacer(Modifier.height(2.dp))
                        Clause(strings["auto_then"], thenText)
                    }
                }
                Text(
                    strings[mode.hint],
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )

                val saveShape = RoundedCornerShape(8.dp)
                Button(
                    enabled = f.isValid(devices, scenes),
                    onClick = { onSave(f.build(initial?.id ?: UUID.randomUUID().toString(), initial?.enabled ?: true)) },
                    shape = saveShape,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).height(36.dp).testTag("save-automation"),
                ) {
                    Icon(Icons.Rounded.Check, null)
                    Spacer(Modifier.width(8.dp))
                    Text(strings["action_save"], style = MaterialTheme.typography.titleSmall)
                }
            }
        }
    }

    pickTime?.let { (which, initialMinute) ->
        TimeDialog(
            strings[if (which == "start") "window_start" else "window_end"], initialMinute, strings.locale,
            onPick = { m ->
                form = if (which == "start") form.copy(windowStart = m) else form.copy(windowEnd = m)
                pickTime = null
            },
            onDismiss = { pickTime = null },
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(strings[mode.deleteTitle]) },
            text = { Text(f.name) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }, modifier = Modifier.testTag("confirm-delete")) { Text(strings["action_delete"]) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(strings["action_cancel"]) } },
        )
    }
}

/** Ad alanı: kutucuk renginde, çizgisiz, yuvarlak (telefondaki gibi). */
@Composable
private fun NameField(value: String, strings: Strings, onChange: (String) -> Unit) {
    TextField(
        value = value,
        onValueChange = onChange,
        label = { Text(strings["automation_name"]) },
        placeholder = { Text(strings["automation_name_hint"]) },
        singleLine = true,
        shape = RoundedCornerShape(8.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = tileColor(),
            unfocusedContainerColor = tileColor(),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).testTag("automation-name"),
    )
}

/** Düzenleyicinin bir adımı: renkli simge, büyük başlık (Eğer / O zaman) ve alt başlık, altında alanlar. */
@Composable
private fun StepCard(icon: ImageVector, hue: Hue, title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(10.dp)).background(tileColor()).padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            HueIcon(icon, hue, size = 40.dp)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        content()
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
    )
}

/**
 * Cihaz seçimi: simgeli kartlar; seçili olan dolu ve çerçeveli. Telefonda yatay kayar; masaüstünde satıra sığmayan
 * kart alt satıra iner (fareyle yatay kaydırma zahmetli, gizli kart kalmasın).
 */
@Composable
private fun DeviceChooser(devices: List<DeviceSnapshot>, selectedId: String, tag: String, onPick: (DeviceSnapshot) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        devices.forEach { d ->
            val selected = d.id == selectedId
            val shape = RoundedCornerShape(8.dp)
            Column(
                Modifier.width(120.dp)
                    .clip(shape)
                    .background(if (selected) scheme.primaryContainer else raisedOnTile())
                    .then(if (selected) Modifier.border(2.dp, scheme.primary, shape) else Modifier)
                    .selectable(selected = selected, onClick = { onPick(d) }, role = Role.RadioButton)
                    .padding(12.dp)
                    .testTag("$tag-${d.id}"),
            ) {
                val kind = remember(d.name, d.isMultiChannel, d.hasEnergy, d.cloud) { DeviceKinds.of(d) }
                IconBadge(
                    kind.icon(),
                    tint = if (selected) scheme.onPrimary else scheme.onSurfaceVariant,
                    background = if (selected) scheme.primary else tileColor(),
                    size = 36.dp,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    d.name,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) scheme.onPrimaryContainer else scheme.onSurface,
                    maxLines = 2,
                    minLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Kanal seçimi: numaralı kare düğmeler. */
@Composable
private fun ChannelChooser(device: DeviceSnapshot, selected: Int, tag: String, onPick: (Int) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        device.switches.keys.sorted().forEach { ch ->
            val on = ch == selected
            val shape = RoundedCornerShape(6.dp)
            Box(
                Modifier.size(52.dp)
                    .clip(shape)
                    .background(if (on) scheme.primary else raisedOnTile())
                    .selectable(selected = on, onClick = { onPick(ch) }, role = Role.RadioButton)
                    .testTag("$tag-$ch"),
                contentAlignment = Alignment.Center,
            ) {
                Text("${ch + 1}", style = MaterialTheme.typography.titleMedium, color = if (on) scheme.onPrimary else scheme.onSurface)
            }
        }
    }
}

/**
 * Seçenek kutucukları: simge + kısa ad, seçili olan dolu ve çerçeveli. Telefonda iki sütun; geniş pencerede
 * [wideColumns] sütun. Bir satırdaki kutucuklar aynı boydadır (ad iki satıra inse de).
 */
@Composable
private fun <T> OptionGrid(
    options: List<T>,
    selected: T,
    wideColumns: Int,
    icon: (T) -> ImageVector,
    label: (T) -> String,
    tag: (T) -> String,
    hue: ((T) -> Hue)? = null,
    onPick: (T) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    BoxWithConstraints {
        val columns = if (maxWidth >= 600.dp) wideColumns else 2
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.chunked(columns).forEach { row ->
                Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { o ->
                        val on = o == selected
                        val shape = RoundedCornerShape(8.dp)
                        Row(
                            Modifier.weight(1f).fillMaxHeight().heightIn(min = 56.dp)
                                .clip(shape)
                                .background(if (on) scheme.primaryContainer else raisedOnTile())
                                .then(if (on) Modifier.border(2.dp, scheme.primary, shape) else Modifier)
                                .selectable(selected = on, onClick = { onPick(o) }, role = Role.RadioButton)
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                .testTag(tag(o)),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val h = hue?.invoke(o)
                            if (h != null) {
                                HueIcon(icon(o), h, size = 30.dp)
                            } else {
                                Icon(icon(o), null, tint = if (on) scheme.primary else scheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(
                                label(o),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (on) scheme.onPrimaryContainer else scheme.onSurface,
                            )
                        }
                    }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

/** Dakika seçici: etiket, ortada değer, iki yanda − / + ([Steps] ile anlamlı adımlar); uçta düğme pasif. */
@Composable
private fun Stepper(
    label: String, value: Int, steps: List<Int>, strings: Strings, tag: String, zeroLabel: String? = null,
    format: ((Int) -> String)? = null, onChange: (Int) -> Unit,
) {
    val lower = Steps.lower(value, steps)
    val higher = Steps.higher(value, steps)
    Row(
        Modifier.fillMaxWidth().padding(top = 12.dp).clip(RoundedCornerShape(8.dp)).background(raisedOnTile())
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f).padding(start = 8.dp))
        IconButton(enabled = lower != null, onClick = { lower?.let(onChange) }, modifier = Modifier.testTag("$tag-minus")) {
            Icon(Icons.Rounded.Remove, strings["action_decrease"])
        }
        Text(
            when {
                format != null -> format(value)
                value == 0 && zeroLabel != null -> zeroLabel
                else -> holdLabel(value, strings)
            },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 88.dp).testTag("$tag-value"),
        )
        IconButton(enabled = higher != null, onClick = { higher?.let(onChange) }, modifier = Modifier.testTag("$tag-plus")) {
            Icon(Icons.Rounded.Add, strings["action_increase"])
        }
    }
}

/** Sayı alanı (güç eşiği W, sıcaklık °C; Türkçe klavyenin virgülü de); [allowNegative] eksi değere izin verir. */
@Composable
private fun NumberField(value: String, label: String, suffix: String, tag: String, allowNegative: Boolean = false, onChange: (String) -> Unit) {
    TextField(
        value = value,
        onValueChange = { v -> onChange(v.filterIndexed { i, c -> c.isDigit() || c == '.' || c == ',' || (allowNegative && c == '-' && i == 0) }) },
        label = { Text(label) },
        suffix = { Text(suffix) },
        singleLine = true,
        shape = RoundedCornerShape(8.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = raisedOnTile(),
            unfocusedContainerColor = raisedOnTile(),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp).testTag(tag),
    )
}

/** Başlık ve anahtar: "Yalnızca şu saatlerde", "Ve şu koşulda". */
@Composable
private fun ToggleLine(text: String, checked: Boolean, tag: String, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(top = 12.dp).clip(RoundedCornerShape(6.dp)).background(raisedOnTile())
            .selectable(selected = checked, onClick = { onChange(!checked) }, role = Role.Switch)
            .padding(horizontal = 16.dp, vertical = 8.dp).testTag("toggle-$tag"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun TimeChip(label: String, value: String, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(6.dp)
    Column(
        modifier.clip(shape).background(raisedOnTile())
            .selectable(selected = false, onClick = onClick, role = Role.Button)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    }
}

/** Haftanın günleri (Pzt…Paz); en az bir gün seçili kalır. */
@Composable
private fun DayChips(days: Int, strings: Strings, onChange: (Int) -> Unit) {
    val names = strings.array("day_short")
    val scheme = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        names.forEachIndexed { i, n ->
            val on = days and (1 shl i) != 0
            val shape = RoundedCornerShape(10.dp)
            Box(
                Modifier.weight(1f).heightIn(min = 36.dp).clip(shape)
                    .background(if (on) scheme.primary else raisedOnTile())
                    .selectable(selected = on, role = Role.Checkbox, onClick = {
                        val next = days xor (1 shl i)
                        if (next != 0) onChange(next)
                    }).testTag("day-$i"),
                contentAlignment = Alignment.Center,
            ) {
                Text(n, style = MaterialTheme.typography.labelMedium, color = if (on) scheme.onPrimary else scheme.onSurface, maxLines = 1)
            }
        }
    }
}

private fun ConditionKind.icon(): ImageVector = when (this) {
    ConditionKind.IS_ON -> Icons.Rounded.PowerSettingsNew
    ConditionKind.IS_OFF -> Icons.Rounded.PowerOff
    ConditionKind.IS_ONLINE -> Icons.Rounded.Cloud
    ConditionKind.IS_OFFLINE -> Icons.Rounded.CloudOff
}

private fun TriggerKind.icon(): ImageVector = when (this) {
    TriggerKind.TURNED_ON -> Icons.Rounded.PowerSettingsNew
    TriggerKind.TURNED_OFF -> Icons.Rounded.PowerOff
    TriggerKind.STAYED_ON -> Icons.Rounded.Timer
    TriggerKind.STAYED_OFF -> Icons.Rounded.TimerOff
    TriggerKind.WENT_OFFLINE -> Icons.Rounded.CloudOff
    TriggerKind.CAME_ONLINE -> Icons.Rounded.Cloud
    TriggerKind.POWER_ABOVE -> Icons.AutoMirrored.Rounded.TrendingUp
    TriggerKind.POWER_BELOW -> Icons.AutoMirrored.Rounded.TrendingDown
    TriggerKind.CHANNEL_POWER_LOW -> Icons.Rounded.ElectricBolt
    TriggerKind.SUNRISE -> Icons.Rounded.WbSunny
    TriggerKind.SUNSET -> Icons.Rounded.WbTwilight
    TriggerKind.TEMP_ABOVE -> Icons.Rounded.LocalFireDepartment
    TriggerKind.TEMP_BELOW -> Icons.Rounded.AcUnit
}

/** Kutucuklardaki kısa olay adları ("Açık kalınca"). */
private fun TriggerKind.shortTitleKey(): String = when (this) {
    TriggerKind.TURNED_ON -> "evt_turned_on"
    TriggerKind.TURNED_OFF -> "evt_turned_off"
    TriggerKind.STAYED_ON -> "evt_stayed_on"
    TriggerKind.STAYED_OFF -> "evt_stayed_off"
    TriggerKind.WENT_OFFLINE -> "evt_went_offline"
    TriggerKind.CAME_ONLINE -> "evt_came_online"
    TriggerKind.POWER_ABOVE -> "evt_power_above"
    TriggerKind.POWER_BELOW -> "evt_power_below"
    TriggerKind.CHANNEL_POWER_LOW -> "evt_channel_power_low"
    TriggerKind.SUNRISE -> "evt_sunrise"
    TriggerKind.SUNSET -> "evt_sunset"
    TriggerKind.TEMP_ABOVE -> "evt_temp_above"
    TriggerKind.TEMP_BELOW -> "evt_temp_below"
}

/** İşlem kutucuklarındaki adlar: cihaz işlemleri "Cihazı aç / kapat" (kartta kısa "Aç / Kapat"). */
private fun ActionKind.editorTitleKey(): String = when (this) {
    ActionKind.ALARM -> "auto_action_alarm"
    ActionKind.NOTIFY -> "auto_action_notify"
    ActionKind.TURN_ON -> "act_turn_on"
    ActionKind.TURN_OFF -> "act_turn_off"
    ActionKind.RUN_SCENE -> "act_run_scene"
}
