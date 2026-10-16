package com.macerce.switchguard.desktop.ui.devices

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CropLandscape
import androidx.compose.material.icons.rounded.CropSquare
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.macerce.switchguard.core.Category
import com.macerce.switchguard.core.DeviceLayout
import com.macerce.switchguard.core.DeviceSection
import com.macerce.switchguard.core.DeviceSnapshot
import com.macerce.switchguard.desktop.ui.RowDivider
import com.macerce.switchguard.desktop.ui.str
import java.util.UUID

// Telefondaki DeviceLayoutUi.kt (2.12): bölüm başlıkları, düzenleme modu araçları ve kategori yönetimi.
// Masaüstü farkı: düzenleme çubuğunun başında sürükleme tutamağı (basılı tutmadan hemen sürüklenir).

@Composable
fun sectionTitle(section: DeviceSection): String = when (section.kind) {
    DeviceSection.Kind.PRIORITY -> str("section_priority")
    DeviceSection.Kind.CATEGORY -> section.category?.name.orEmpty()
    DeviceSection.Kind.OTHER -> str("section_other")
    DeviceSection.Kind.ALL -> ""
}

/** Bölüm başlığı; tıklayınca bölüm katlanır/açılır. */
@Composable
fun LayoutSectionHeader(title: String, count: Int, collapsed: Boolean, onToggle: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onToggle).padding(start = 4.dp, end = 4.dp, top = 10.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        Text("$count", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Icon(
            Icons.Rounded.ExpandMore,
            contentDescription = str(if (collapsed) "action_expand" else "action_collapse"),
            modifier = Modifier.padding(start = 4.dp).rotate(if (collapsed) 0f else 180f),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Düzenleme modunda ekranın üstündeki açıklama ve kategori yönetimi girişi. */
@Composable
fun LayoutEditHint(onCategories: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(str("desktop_layout_hint"), style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onCategories, modifier = Modifier.padding(top = 4.dp)) {
                Icon(Icons.AutoMirrored.Rounded.Label, null)
                Spacer(Modifier.width(8.dp))
                Text(str("categories_manage"))
            }
        }
    }
}

/**
 * Düzenleme modunda kartın altındaki araçlar: sürükleme tutamağı ([dragHandle], ızgaranın sürükle-bırak
 * düzeninden), öncelik, sıra okları, kart boyutu ve kategori.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LayoutEditBar(
    device: DeviceSnapshot,
    layout: DeviceLayout,
    devices: List<DeviceSnapshot>,
    canUp: Boolean,
    canDown: Boolean,
    dragHandle: Modifier,
    onChange: (DeviceLayout) -> Unit,
) {
    val id = device.id
    var menu by remember { mutableStateOf(false) }
    var newCategory by remember { mutableStateOf(false) }
    RowDivider(inset = 0.dp)
    FlowRow(
        Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Rounded.DragIndicator,
            contentDescription = str("desktop_drag_handle"),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = dragHandle.pointerHoverIcon(PointerIcon.Hand).padding(12.dp).testTag("drag-$id"),
        )
        val priority = id in layout.priority
        IconButton(onClick = { onChange(layout.togglePriority(id)) }, modifier = Modifier.testTag("priority-$id")) {
            Icon(
                if (priority) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                contentDescription = str(if (priority) "layout_unpriority" else "layout_priority"),
                tint = if (priority) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // Kart en üste/alta taşınınca o yöndeki ok pasifleşir ve klavye odağı kaybolur: karşı oka devret.
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
            modifier = Modifier.focusRequester(upFocus),
        ) {
            Icon(Icons.Rounded.ArrowUpward, str("layout_move_up"))
        }
        IconButton(
            onClick = { movedDown = true; onChange(layout.move(id, +1, devices)) },
            enabled = canDown,
            modifier = Modifier.focusRequester(downFocus),
        ) {
            Icon(Icons.Rounded.ArrowDownward, str("layout_move_down"))
        }
        // Kart boyutu: düzenlemede tüm kartlar tam satır görünür, gerçek boyut bu düğmenin yazısıdır.
        val large = layout.isLarge(device)
        AssistChip(
            onClick = { onChange(layout.toggleSize(device)) },
            label = { Text(str(if (large) "layout_large" else "layout_compact"), maxLines = 1) },
            leadingIcon = { Icon(if (large) Icons.Rounded.CropLandscape else Icons.Rounded.CropSquare, null, Modifier.size(18.dp)) },
            modifier = Modifier.padding(start = 4.dp).testTag("size-$id"),
        )
        Spacer(Modifier.weight(1f))
        val current = layout.categories.firstOrNull { it.id == layout.categoryOf[id] }
        Column {
            AssistChip(
                onClick = { menu = true },
                label = { Text(current?.name ?: str("category_none"), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                leadingIcon = { Icon(Icons.AutoMirrored.Rounded.Label, null, Modifier.size(18.dp)) },
                modifier = Modifier.padding(end = 8.dp).widthIn(max = 200.dp).testTag("category-$id"),
            )
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text(str("category_none")) }, onClick = { onChange(layout.assign(id, null)); menu = false })
                layout.categories.forEach { c ->
                    DropdownMenuItem(text = { Text(c.name) }, onClick = { onChange(layout.assign(id, c.id)); menu = false })
                }
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text(str("category_new")) },
                    leadingIcon = { Icon(Icons.Rounded.Add, null) },
                    onClick = { menu = false; newCategory = true },
                )
            }
        }
    }
    if (newCategory) {
        NameDialog(str("category_new"), "", onDismiss = { newCategory = false }) { name ->
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
        title = { Text(str("categories_manage")) },
        text = {
            Column(Modifier.widthIn(min = 360.dp).heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                if (layout.categories.isEmpty()) {
                    Text(str("categories_empty"), style = MaterialTheme.typography.bodyMedium)
                }
                layout.categories.forEachIndexed { i, c ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(c.name, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        IconButton(onClick = { renaming = c }) { Icon(Icons.Rounded.Edit, str("action_rename")) }
                        IconButton(onClick = { onChange(layout.moveCategory(c.id, -1)) }, enabled = i > 0) {
                            Icon(Icons.Rounded.ArrowUpward, str("layout_move_up"))
                        }
                        IconButton(onClick = { onChange(layout.moveCategory(c.id, +1)) }, enabled = i < layout.categories.lastIndex) {
                            Icon(Icons.Rounded.ArrowDownward, str("layout_move_down"))
                        }
                        IconButton(onClick = { onChange(layout.deleteCategory(c.id)) }) {
                            Icon(Icons.Rounded.Delete, str("action_delete"))
                        }
                    }
                }
                TextButton(onClick = { adding = true }) {
                    Icon(Icons.Rounded.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text(str("category_new"))
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(str("action_done")) } },
    )
    renaming?.let { c ->
        NameDialog(str("action_rename"), c.name, onDismiss = { renaming = null }) { onChange(layout.renameCategory(c.id, it)) }
    }
    if (adding) {
        NameDialog(str("category_new"), "", onDismiss = { adding = false }) {
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
            OutlinedTextField(name, { name = it }, label = { Text(str("category_name")) }, singleLine = true)
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = { onSave(name.trim()); onDismiss() }) { Text(str("action_save")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(str("action_cancel")) } },
    )
}
