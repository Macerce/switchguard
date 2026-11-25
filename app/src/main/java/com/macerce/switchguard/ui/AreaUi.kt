package com.macerce.switchguard.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.material3.AssistChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CropLandscape
import androidx.compose.material.icons.rounded.CropSquare
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bathtub
import androidx.compose.material.icons.rounded.Bed
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.ChildCare
import androidx.compose.material.icons.rounded.DoorFront
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Garage
import androidx.compose.material.icons.rounded.Handyman
import androidx.compose.material.icons.rounded.Kitchen
import androidx.compose.material.icons.rounded.MeetingRoom
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Weekend
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material.icons.rounded.Yard
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.macerce.switchguard.R
import com.macerce.switchguard.core.AreaIcon
import com.macerce.switchguard.core.AreaSummary
import com.macerce.switchguard.core.Category
import com.macerce.switchguard.core.DeviceGroup
import com.macerce.switchguard.core.DeviceLayout
import com.macerce.switchguard.core.DeviceSnapshot
import com.macerce.switchguard.core.SensorKind
import com.macerce.switchguard.data.Store
import com.macerce.switchguard.ui.theme.Brand
import com.macerce.switchguard.ui.theme.LocalDarkTheme
import com.macerce.switchguard.ui.theme.raisedOnTile
import com.macerce.switchguard.ui.theme.tileColor
import java.util.UUID

fun AreaIcon.vector(): ImageVector = when (this) {
    AreaIcon.ROOM -> Icons.Rounded.MeetingRoom
    AreaIcon.KITCHEN -> Icons.Rounded.Kitchen
    AreaIcon.BED -> Icons.Rounded.Bed
    AreaIcon.BATH -> Icons.Rounded.Bathtub
    AreaIcon.GARDEN -> Icons.Rounded.Yard
    AreaIcon.GARAGE -> Icons.Rounded.Garage
    AreaIcon.OFFICE -> Icons.Rounded.Work
    AreaIcon.LIVING -> Icons.Rounded.Weekend
    AreaIcon.KIDS -> Icons.Rounded.ChildCare
    AreaIcon.UTILITY -> Icons.Rounded.Handyman
    AreaIcon.ENTRANCE -> Icons.Rounded.DoorFront
}

/** Özetin tek satırı: "2/5 açık · 340 W · 22,5 °C · 1 kapı açık". */
@Composable
fun areaStatusText(s: AreaSummary): String {
    val parts = mutableListOf<String>()
    if (s.devices == 0) return stringResource(R.string.area_empty_short)
    if (s.switchesTotal > 0) parts += stringResource(R.string.channels_on_short, s.switchesOn, s.switchesTotal)
    s.watts?.takeIf { it >= 1 }?.let { parts += formatWatts(it) }
    s.temperature?.let { parts += formatSensor(SensorKind.TEMPERATURE, it) }
    s.humidity?.let { parts += formatSensor(SensorKind.HUMIDITY, it) }
    if (s.doorsOpen > 0) parts += stringResource(R.string.area_doors_open, s.doorsOpen)
    if (s.motion > 0) parts += stringResource(R.string.area_motion)
    if (s.water > 0) parts += stringResource(R.string.area_water)
    if (s.offline > 0) parts += stringResource(R.string.area_offline, s.offline)
    return parts.joinToString(" · ")
}

/**
 * Cihazlar ekranındaki alan kartı (Home Assistant'ın alan kartı gibi); dokununca alanın sayfası açılır.
 * Sağ üstte alanın hepsini aç/kapat düğmesi ([group]: alanın sanal grubu; kapatırken onaylı cihaz varsa sorulur).
 * [large]: tam satır; alandaki cihazlar tek dokunuşla açılıp kapanan çipler olarak altında listelenir.
 * [hidden]: ana ekranda gizli (yalnız düzenlemede soluk görünür). [editBar]: düzenleme araçları.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AreaTile(
    area: Category,
    summary: AreaSummary,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    large: Boolean = false,
    group: DeviceSnapshot? = null,
    members: List<DeviceSnapshot> = emptyList(),
    controls: Boolean = true,
    hidden: Boolean = false,
    editBar: (@Composable () -> Unit)? = null,
) {
    val dark = LocalDarkTheme.current
    val shape = RoundedCornerShape(20.dp)
    // Cihaz kartlarıyla aynı dil: açık alan düz kalır, simgesi amber dolar, kenarında ince amber çizgi.
    val lit = summary.anyOn && editBar == null
    val active = false
    val accent = com.macerce.switchguard.core.DeviceKind.SWITCH.onColor()
    val background: Brush =
        if (lit) SolidColor(accent.copy(alpha = if (dark) 0.10f else 0.08f).compositeOver(tileColor())) else SolidColor(tileColor())
    val fg = MaterialTheme.colorScheme.onSurface
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier.fillMaxWidth().tvFocusRing(shape).clip(shape)
            .background(background, shape)
            .then(
                when {
                    summary.water > 0 -> Modifier.border(1.5.dp, Brand.alarm, shape)
                    lit -> Modifier.border(1.5.dp, accent.copy(alpha = 0.6f), shape)
                    else -> Modifier
                }
            ),
    ) {
        Box {
            Box(Modifier.matchParentSize().clickable(onClick = onOpen))
            Column(
                Modifier.fillMaxWidth().heightIn(min = 104.dp).alpha(if (hidden) 0.5f else 1f).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    IconBadge(
                        area.iconKind.vector(),
                        tint = if (lit) Color(0xFF1C1C1C) else MaterialTheme.colorScheme.onSurfaceVariant,
                        background = if (lit) accent else raisedOnTile(),
                        size = 38,
                    )
                    Spacer(Modifier.weight(1f))
                    if (hidden) Icon(Icons.Rounded.VisibilityOff, stringResource(R.string.layout_show), tint = muted, modifier = Modifier.size(18.dp))
                    if (controls && group != null && summary.switchesTotal > 0) {
                        val on = group.switches[0] == true
                        val control = rememberSwitchControl(group, 0, on)
                        PowerButton(control.shown, control.busy, group.online, control.toggle, size = 38.dp, onBrand = active)
                    }
                }
                Column {
                    Text(area.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = fg,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(areaStatusText(summary), style = MaterialTheme.typography.bodySmall,
                        color = if (summary.offline > 0 && !active) Brand.alarm else muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                if (large && controls && members.isNotEmpty()) {
                    // Alandaki her anahtar tek dokunuşla (detaya girmeden); çok kanallıda kanal kanal.
                    val chips = members.flatMap { d -> d.switches.keys.sorted().map { d to it } }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        chips.take(MAX_CHIPS).forEach { (d, ch) -> MemberChip(d, ch, active) }
                        if (chips.size > MAX_CHIPS) {
                            Text("+" + (chips.size - MAX_CHIPS), color = muted, style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp))
                        }
                    }
                }
            }
        }
        editBar?.invoke()
    }
}

private const val MAX_CHIPS = 12

/** Büyük alan kartında bir anahtar: ad ve durum rengi; dokununca açılır/kapanır (kapatırken onay kuralı geçerli). */
@Composable
private fun MemberChip(d: DeviceSnapshot, channel: Int, onBrand: Boolean) {
    val control = rememberSwitchControl(d, channel, d.switches[channel] == true)
    val on = control.shown && d.online
    val shape = RoundedCornerShape(12.dp)
    val scheme = MaterialTheme.colorScheme
    val bg = when {
        on && onBrand -> Color.White.copy(alpha = 0.25f)
        on -> scheme.primaryContainer
        onBrand -> Color.Black.copy(alpha = 0.12f)
        else -> raisedOnTile()
    }
    val fg = when {
        !d.online -> Brand.alarm
        onBrand -> Brand.onActive
        on -> scheme.onPrimaryContainer
        else -> scheme.onSurfaceVariant
    }
    val name = if (d.isMultiChannel) "${d.name} · ${channel + 1}" else d.name
    val state = stringResource(if (!d.online) R.string.label_offline_title else if (on) R.string.state_on_title else R.string.state_off_title)
    Row(
        Modifier.tvFocusRing(shape).clip(shape).background(bg, shape)
            .clickable(enabled = d.online, role = androidx.compose.ui.semantics.Role.Switch) { if (!control.busy) control.toggle(!control.shown) }
            .semantics(mergeDescendants = true) { contentDescription = "$name, $state" }
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(if (d.online) Icons.Rounded.PowerSettingsNew else Icons.Rounded.CloudOff, null, tint = fg, modifier = Modifier.size(14.dp).alpha(if (control.busy) 0.4f else 1f))
        Spacer(Modifier.width(6.dp))
        Text(name, color = fg, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 150.dp))
    }
}

/**
 * Düzenleme modunda alan kartının altındaki araçlar: sola/sağa taşı, küçük/büyük, simge, ana ekranda gizle,
 * yeniden adlandır, sil (cihazlar silinmez, alansız kalır).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AreaEditBar(area: Category, layout: DeviceLayout, canBack: Boolean, canForward: Boolean, onChange: (DeviceLayout) -> Unit) {
    var iconMenu by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    RowDivider(inset = 0.dp)
    FlowRow(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp), itemVerticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onChange(layout.moveCategory(area.id, -1)) }, enabled = canBack, modifier = Modifier.tvFocusRing()) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.layout_move_up))
        }
        IconButton(onClick = { onChange(layout.moveCategory(area.id, +1)) }, enabled = canForward, modifier = Modifier.tvFocusRing()) {
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, stringResource(R.string.layout_move_down))
        }
        val large = area.id in layout.areaLarge
        AssistChip(
            onClick = { onChange(layout.toggleAreaLarge(area.id)) },
            label = { Text(stringResource(if (large) R.string.layout_large else R.string.layout_compact), maxLines = 1) },
            leadingIcon = { Icon(if (large) Icons.Rounded.CropLandscape else Icons.Rounded.CropSquare, null, Modifier.size(18.dp)) },
            modifier = Modifier.padding(horizontal = 4.dp).tvFocusRing(RoundedCornerShape(8.dp)),
        )
        Box {
            AssistChip(
                onClick = { iconMenu = true },
                label = { Text(stringResource(R.string.area_icon), maxLines = 1) },
                leadingIcon = { Icon(area.iconKind.vector(), null, Modifier.size(18.dp)) },
                modifier = Modifier.padding(horizontal = 4.dp).tvFocusRing(RoundedCornerShape(8.dp)),
            )
        }
        Spacer(Modifier.weight(1f))
        val hidden = area.id in layout.hidden
        IconButton(onClick = { onChange(layout.toggleHidden(area.id)) }, modifier = Modifier.tvFocusRing()) {
            Icon(
                if (hidden) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                stringResource(if (hidden) R.string.layout_show else R.string.layout_hide),
                tint = if (hidden) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = { renaming = true }, modifier = Modifier.tvFocusRing()) { Icon(Icons.Rounded.Edit, stringResource(R.string.action_rename)) }
        IconButton(onClick = { deleting = true }, modifier = Modifier.tvFocusRing()) { Icon(Icons.Rounded.Delete, stringResource(R.string.action_delete)) }
    }
    if (iconMenu) AlertDialog(
        onDismissRequest = { iconMenu = false },
        title = { Text(stringResource(R.string.area_icon)) },
        text = {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // İlk seçenek: addan tahmin (otomatik).
                val options = listOf<AreaIcon?>(null) + AreaIcon.entries
                options.forEach { icon ->
                    val selected = area.icon == icon?.name
                    val shape = RoundedCornerShape(14.dp)
                    Box(
                        Modifier.size(52.dp).tvFocusRing(shape).clip(shape)
                            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else raisedOnTile(), shape)
                            .clickable { onChange(layout.setAreaIcon(area.id, icon)); iconMenu = false },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (icon == null) Text(stringResource(R.string.area_icon_auto), style = MaterialTheme.typography.labelSmall, maxLines = 1)
                        else Icon(icon.vector(), null, tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { iconMenu = false }, modifier = Modifier.tvFocusRing()) { Text(stringResource(R.string.action_cancel)) } },
    )
    if (renaming) NameDialog(stringResource(R.string.action_rename), area.name, onDismiss = { renaming = false }) { onChange(layout.renameCategory(area.id, it)) }
    if (deleting) AlertDialog(
        onDismissRequest = { deleting = false },
        title = { Text(stringResource(R.string.area_delete_confirm, area.name)) },
        confirmButton = {
            TextButton(onClick = { deleting = false; onChange(layout.deleteCategory(area.id)) }, modifier = Modifier.tvFocusRing()) {
                Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = { deleting = false }, modifier = Modifier.tvFocusRing()) { Text(stringResource(R.string.action_cancel)) } },
    )
}

/** Hiç alan yokken Cihazlar ekranında bir kez görünen davet kartı. */
@Composable
fun AreaInviteCard(onCreate: () -> Unit, onDismiss: () -> Unit) {
    SectionCard(horizontalPadding = 0) {
        Column {
            InfoRow(Icons.Rounded.MeetingRoom, stringResource(R.string.areas_invite_title), stringResource(R.string.areas_invite_desc),
                Modifier.clickable(onClick = onCreate))
            Row(Modifier.fillMaxWidth().padding(start = 52.dp, end = 8.dp, bottom = 4.dp)) {
                TextButton(onClick = onCreate, modifier = Modifier.tvFocusRing()) { Text(stringResource(R.string.area_new)) }
                TextButton(onClick = onDismiss, modifier = Modifier.tvFocusRing()) { Text(stringResource(R.string.action_hide)) }
            }
        }
    }
}

/** Alan sayfasının üstündeki kart: özet, hepsini aç/kapat, cihaz seç, yeniden adlandır. */
@Composable
fun AreaHero(
    area: Category,
    summary: AreaSummary,
    onAll: (Boolean) -> Unit,
    onMembers: () -> Unit,
    onRename: () -> Unit,
    excluded: Int = 0,
    onExclude: () -> Unit = {},
) {
    SectionCard(horizontalPadding = 0) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(area.iconKind.vector(), MaterialTheme.colorScheme.onPrimaryContainer,
                    MaterialTheme.colorScheme.primaryContainer, 44)
                Column(Modifier.padding(start = 12.dp).weight(1f)) {
                    Text(area.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(areaStatusText(summary), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (summary.switchesTotal > 0) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = { onAll(true) }, modifier = Modifier.weight(1f).tvFocusRing()) {
                        Icon(Icons.Rounded.PowerSettingsNew, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.area_all_on), maxLines = 1)
                    }
                    OutlinedButton(onClick = { onAll(false) }, modifier = Modifier.weight(1f).tvFocusRing()) {
                        Text(stringResource(R.string.area_all_off), maxLines = 1)
                    }
                }
                if (excluded > 0) {
                    Text(stringResource(R.string.area_excluded_count, excluded), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onMembers, modifier = Modifier.tvFocusRing()) {
                    Icon(Icons.Rounded.Add, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.area_choose_devices))
                }
                if (summary.switchesTotal > 0) TextButton(onClick = onExclude, modifier = Modifier.tvFocusRing()) {
                    Icon(Icons.Rounded.Block, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.area_exclude))
                }
                TextButton(onClick = onRename, modifier = Modifier.tvFocusRing()) {
                    Icon(Icons.Rounded.Edit, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.action_rename))
                }
            }
        }
    }
}

/**
 * Alandaki cihazları seç: işaretli cihazlar bu alana alınır (başka alandaysa oradan çıkar), işareti kaldırılan
 * alansız kalır. Gruplar ve alan sanal cihazları listelenmez.
 */
@Composable
fun AreaMembersDialog(area: Category, layout: DeviceLayout, devices: List<DeviceSnapshot>, onChange: (DeviceLayout) -> Unit, onDismiss: () -> Unit) {
    val real = remember(devices) { layout.sorted(devices.filterNot { DeviceGroup.isGroup(it.id) }) }
    val names = remember(layout) { layout.categories.associate { it.id to it.name } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.area_devices_title, area.name)) },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                real.forEach { d ->
                    val current = layout.categoryOf[d.id]
                    val checked = current == area.id
                    val toggle = { onChange(layout.assign(d.id, if (checked) null else area.id)) }
                    Row(
                        Modifier.fillMaxWidth().tvFocusRing(RoundedCornerShape(8.dp)).clickable(onClick = toggle).padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = checked, onCheckedChange = { toggle() })
                        Column(Modifier.weight(1f)) {
                            Text(d.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            // Başka alandaysa adı yazılır: işaretlenince oradan taşınır.
                            if (current != null && !checked) names[current]?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(modifier = Modifier.tvFocusRing(), onClick = onDismiss) { Text(stringResource(R.string.action_done)) } },
    )
}

/**
 * "Hariç tut": alandaki anahtarlı cihazlardan hangileri Hepsini aç / Hepsini kapat'a (ve alan grubunun otomasyon,
 * sohbet, Telegram komutlarına) dahil. İşareti kaldırılan cihaz alanda kalır, yalnızca toplu komuttan etkilenmez.
 */
@Composable
fun AreaExcludeDialog(area: Category, layout: DeviceLayout, devices: List<DeviceSnapshot>, onChange: (DeviceLayout) -> Unit, onDismiss: () -> Unit) {
    val members = remember(devices, layout) {
        layout.sorted(devices.filter { !DeviceGroup.isGroup(it.id) && layout.categoryOf[it.id] == area.id && it.switches.isNotEmpty() })
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.area_exclude_title)) },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                Text(stringResource(R.string.area_exclude_desc), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
                members.forEach { d ->
                    val included = d.id !in layout.allExcluded
                    val toggle = { onChange(layout.toggleAllExcluded(d.id)) }
                    Row(
                        Modifier.fillMaxWidth().tvFocusRing(RoundedCornerShape(8.dp)).clickable(onClick = toggle).padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = included, onCheckedChange = { toggle() })
                        Text(d.name, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        },
        confirmButton = { TextButton(modifier = Modifier.tvFocusRing(), onClick = onDismiss) { Text(stringResource(R.string.action_done)) } },
    )
}

/** Cihaz detayında "Alan" satırı: cihazı bir alana koy, alandan çıkar ya da yeni alan oluştur. */
@Composable
fun DeviceAreaRow(deviceId: String) {
    val context = LocalContext.current
    val store = remember { Store.get(context) }
    val version by store.version.collectAsStateWithLifecycle()
    val layout = remember(version) { store.deviceLayout }
    val current = layout.categories.firstOrNull { it.id == layout.categoryOf[deviceId] }
    var menu by remember { mutableStateOf(false) }
    var creating by remember { mutableStateOf(false) }
    SectionCard {
        Column {
            InfoRow(
                current?.iconKind?.vector() ?: Icons.Rounded.MeetingRoom,
                stringResource(R.string.area_title),
                current?.name ?: stringResource(R.string.category_none),
                Modifier.clickable { menu = true },
            ) {
                TextButton(onClick = { menu = true }, modifier = Modifier.tvFocusRing()) { Text(stringResource(R.string.action_change)) }
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text(stringResource(R.string.category_none)) }, onClick = { store.deviceLayout = layout.assign(deviceId, null); menu = false })
                layout.categories.forEach { c ->
                    DropdownMenuItem(text = { Text(c.name) }, onClick = { store.deviceLayout = layout.assign(deviceId, c.id); menu = false })
                }
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.area_new)) },
                    leadingIcon = { Icon(Icons.Rounded.Add, null) },
                    onClick = { menu = false; creating = true },
                )
            }
        }
    }
    if (creating) {
        NameDialog(stringResource(R.string.area_new), "", onDismiss = { creating = false }) { name ->
            val c = Category(UUID.randomUUID().toString(), name)
            store.deviceLayout = store.deviceLayout.addCategory(c).assign(deviceId, c.id)
        }
    }
}
