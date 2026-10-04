package com.macerce.switchguard.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.UnfoldLess
import androidx.compose.material.icons.rounded.UnfoldMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.macerce.switchguard.R
import com.macerce.switchguard.core.Category
import com.macerce.switchguard.core.DeviceLayout
import com.macerce.switchguard.core.DeviceSection
import com.macerce.switchguard.core.DeviceSnapshot
import java.util.UUID

@Composable
fun sectionTitle(section: DeviceSection): String = when (section.kind) {
    DeviceSection.Kind.PRIORITY -> stringResource(R.string.section_priority)
    DeviceSection.Kind.CATEGORY -> section.category?.name.orEmpty()
    DeviceSection.Kind.OTHER -> stringResource(R.string.section_other)
    DeviceSection.Kind.ALL -> ""
}

/** Bölüm başlığı; dokununca bölüm katlanır/açılır. */
@Composable
fun LayoutSectionHeader(title: String, count: Int, collapsed: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(start = 20.dp, end = 16.dp, top = 8.dp, bottom = 0.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("$count", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Icon(
            Icons.Rounded.ExpandMore,
            contentDescription = stringResource(if (collapsed) R.string.action_expand else R.string.action_collapse),
            modifier = Modifier.padding(start = 4.dp).rotate(if (collapsed) 0f else 180f),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Düzenleme modunda ekranın üstündeki açıklama ve kategori yönetimi girişi. */
@Composable
fun LayoutEditHint(onCategories: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.layout_hint), style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onCategories, modifier = Modifier.padding(top = 4.dp).tvFocusRing()) {
                Icon(Icons.AutoMirrored.Rounded.Label, null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.categories_manage))
            }
        }
    }
}

/** Düzenleme modunda kartın altındaki araçlar: öncelik, sıra, kart boyutu, kategori. */
@Composable
fun LayoutEditBar(
    device: DeviceSnapshot,
    layout: DeviceLayout,
    devices: List<DeviceSnapshot>,
    canUp: Boolean,
    canDown: Boolean,
    onChange: (DeviceLayout) -> Unit,
) {
    val id = device.id
    var menu by remember { mutableStateOf(false) }
    var newCategory by remember { mutableStateOf(false) }
    HorizontalDivider(color = MaterialTheme.colorScheme.surface)
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        val priority = id in layout.priority
        IconButton(onClick = { onChange(layout.togglePriority(id)) }, modifier = Modifier.tvFocusRing()) {
            Icon(
                if (priority) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                contentDescription = stringResource(if (priority) R.string.layout_unpriority else R.string.layout_priority),
                tint = if (priority) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // Kart en üste/alta taşınınca o yöndeki ok pasifleşir ve kumandanın odağı kaybolur: karşı oka devret.
        val upFocus = remember { FocusRequester() }
        val downFocus = remember { FocusRequester() }
        var movedUp by remember { mutableStateOf(false) }
        var movedDown by remember { mutableStateOf(false) }
        LaunchedEffect(canUp, movedUp) {
            if (movedUp && !canUp) runCatching { downFocus.requestFocus() }
            movedUp = false
        }
        LaunchedEffect(canDown, movedDown) {
            if (movedDown && !canDown) runCatching { upFocus.requestFocus() }
            movedDown = false
        }
        IconButton(
            onClick = { movedUp = true; onChange(layout.move(id, -1, devices)) },
            enabled = canUp,
            modifier = Modifier.focusRequester(upFocus).tvFocusRing(),
        ) {
            Icon(Icons.Rounded.ArrowUpward, stringResource(R.string.layout_move_up))
        }
        IconButton(
            onClick = { movedDown = true; onChange(layout.move(id, +1, devices)) },
            enabled = canDown,
            modifier = Modifier.focusRequester(downFocus).tvFocusRing(),
        ) {
            Icon(Icons.Rounded.ArrowDownward, stringResource(R.string.layout_move_down))
        }
        val compact = id in layout.compact
        IconButton(onClick = { onChange(layout.toggleCompact(id)) }, modifier = Modifier.tvFocusRing()) {
            Icon(
                if (compact) Icons.Rounded.UnfoldMore else Icons.Rounded.UnfoldLess,
                contentDescription = stringResource(if (compact) R.string.layout_large else R.string.layout_compact),
            )
        }
        Spacer(Modifier.weight(1f))
        val current = layout.categories.firstOrNull { it.id == layout.categoryOf[id] }
        Column {
            AssistChip(
                onClick = { menu = true },
                label = { Text(current?.name ?: stringResource(R.string.category_none), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                leadingIcon = { Icon(Icons.AutoMirrored.Rounded.Label, null, Modifier.size(18.dp)) },
                modifier = Modifier.padding(end = 8.dp).tvFocusRing(RoundedCornerShape(8.dp)),
            )
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text(stringResource(R.string.category_none)) }, onClick = { onChange(layout.assign(id, null)); menu = false })
                layout.categories.forEach { c ->
                    DropdownMenuItem(text = { Text(c.name) }, onClick = { onChange(layout.assign(id, c.id)); menu = false })
                }
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.category_new)) },
                    leadingIcon = { Icon(Icons.Rounded.Add, null) },
                    onClick = { menu = false; newCategory = true },
                )
            }
        }
    }
    if (newCategory) {
        NameDialog(stringResource(R.string.category_new), "", onDismiss = { newCategory = false }) { name ->
            val c = Category(UUID.randomUUID().toString(), name)
            onChange(layout.addCategory(c).assign(id, c.id))
        }
    }
}

/** Kategorileri ekle, yeniden adlandır, sırala, sil. */
@Composable
fun CategoriesDialog(layout: DeviceLayout, onChange: (DeviceLayout) -> Unit, onDismiss: () -> Unit) {
    var renaming by remember { mutableStateOf<Category?>(null) }
    var adding by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.categories_manage)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (layout.categories.isEmpty()) {
                    Text(stringResource(R.string.categories_empty), style = MaterialTheme.typography.bodyMedium)
                }
                layout.categories.forEachIndexed { i, c ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(c.name, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        IconButton(modifier = Modifier.tvFocusRing(), onClick = { renaming = c }) { Icon(Icons.Rounded.Edit, stringResource(R.string.action_rename)) }
                        IconButton(modifier = Modifier.tvFocusRing(), onClick = { onChange(layout.moveCategory(c.id, -1)) }, enabled = i > 0) {
                            Icon(Icons.Rounded.ArrowUpward, stringResource(R.string.layout_move_up))
                        }
                        IconButton(modifier = Modifier.tvFocusRing(), onClick = { onChange(layout.moveCategory(c.id, +1)) }, enabled = i < layout.categories.lastIndex) {
                            Icon(Icons.Rounded.ArrowDownward, stringResource(R.string.layout_move_down))
                        }
                        IconButton(modifier = Modifier.tvFocusRing(), onClick = { onChange(layout.deleteCategory(c.id)) }) {
                            Icon(Icons.Rounded.Delete, stringResource(R.string.action_delete))
                        }
                    }
                }
                TextButton(modifier = Modifier.tvFocusRing(), onClick = { adding = true }) {
                    Icon(Icons.Rounded.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.category_new))
                }
            }
        },
        confirmButton = { TextButton(modifier = Modifier.tvFocusRing(), onClick = onDismiss) { Text(stringResource(R.string.action_done)) } },
    )
    renaming?.let { c ->
        NameDialog(stringResource(R.string.action_rename), c.name, onDismiss = { renaming = null }) { onChange(layout.renameCategory(c.id, it)) }
    }
    if (adding) {
        NameDialog(stringResource(R.string.category_new), "", onDismiss = { adding = false }) {
            onChange(layout.addCategory(Category(UUID.randomUUID().toString(), it)))
        }
    }
}

@Composable
private fun NameDialog(title: String, initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.category_name)) }, singleLine = true)
        },
        confirmButton = {
            TextButton(modifier = Modifier.tvFocusRing(), enabled = name.isNotBlank(), onClick = { onSave(name.trim()); onDismiss() }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(modifier = Modifier.tvFocusRing(), onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
