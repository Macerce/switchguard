package com.macerce.switchguard.ui

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

/** Cihazlar ekranındaki alan kartı (Home Assistant'ın alan kartı gibi); dokununca alanın sayfası açılır. */
@Composable
fun AreaTile(area: Category, summary: AreaSummary, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val dark = LocalDarkTheme.current
    val shape = RoundedCornerShape(20.dp)
    val active = summary.anyOn
    val background: Brush = if (active) Brand.active(dark) else SolidColor(tileColor())
    val fg = if (active) Brand.onActive else MaterialTheme.colorScheme.onSurface
    val muted = if (active) Brand.onActive.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier.fillMaxWidth().tvFocusRing(shape).heightIn(min = 104.dp)
            .background(background, shape)
            .then(if (summary.water > 0) Modifier.border(1.5.dp, Brand.alarm, shape) else Modifier)
            .clickable(onClick = onOpen).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        IconBadge(
            AreaSummary.iconKind(area.name).vector(),
            tint = if (active) Brand.onActive else MaterialTheme.colorScheme.onSurfaceVariant,
            background = if (active) androidx.compose.ui.graphics.Color.White.copy(alpha = 0.2f) else raisedOnTile(),
            size = 38,
        )
        Column {
            Text(area.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = fg,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(areaStatusText(summary), style = MaterialTheme.typography.bodySmall, color = muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
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
                IconBadge(AreaSummary.iconKind(area.name).vector(), MaterialTheme.colorScheme.onPrimaryContainer,
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
                current?.let { AreaSummary.iconKind(it.name).vector() } ?: Icons.Rounded.MeetingRoom,
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
