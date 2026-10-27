package com.macerce.switchguard.desktop.ui.alerts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.macerce.switchguard.core.DeviceCategory
import com.macerce.switchguard.core.DeviceGroup
import com.macerce.switchguard.core.DeviceSnapshot
import com.macerce.switchguard.desktop.AppGraph
import com.macerce.switchguard.desktop.i18n.Strings
import com.macerce.switchguard.desktop.ui.Hue
import com.macerce.switchguard.desktop.ui.InfoRow
import com.macerce.switchguard.desktop.ui.LocalDarkTheme
import com.macerce.switchguard.desktop.ui.SectionCard
import com.macerce.switchguard.desktop.ui.SectionTitle
import com.macerce.switchguard.desktop.ui.settings.PageIntro
import com.macerce.switchguard.desktop.ui.str

fun DeviceCategory.titleKey(): String = when (this) {
    DeviceCategory.CRITICAL -> "category_critical"
    DeviceCategory.IMPORTANT -> "category_important"
    DeviceCategory.NORMAL -> "category_normal"
}

/** Otomasyon tetikleyicisindeki sanal cihazın adı ("Kritik cihazlardan biri"). */
fun DeviceCategory.anyKey(): String = when (this) {
    DeviceCategory.CRITICAL -> "category_any_critical"
    DeviceCategory.IMPORTANT -> "category_any_important"
    DeviceCategory.NORMAL -> "category_any_normal"
}

/** Rozet rengi; normal kategori rozetsizdir. */
fun DeviceCategory.hue(): Hue? = when (this) {
    DeviceCategory.CRITICAL -> Hue.RED
    DeviceCategory.IMPORTANT -> Hue.AMBER
    DeviceCategory.NORMAL -> null
}

/** Otomasyon düzenleyicisi ve kartları için cihazlar: gerçek cihazlar, gruplar ve kategorilerin sanal cihazları. */
fun automationDevices(snapshotsWithGroups: List<DeviceSnapshot>, strings: Strings): List<DeviceSnapshot> =
    snapshotsWithGroups + DeviceCategory.snapshots { strings[it.anyKey()] }

/** Kategorinin küçük renkli rozeti (Uyarılar'daki cihaz satırında); normal kategoride çizilmez. */
@Composable
fun CategoryBadge(category: DeviceCategory, modifier: Modifier = Modifier) {
    val hue = category.hue() ?: return
    val color = if (LocalDarkTheme.current) hue.dark else hue.light
    Text(
        str(category.titleKey()),
        style = MaterialTheme.typography.labelSmall,
        color = Color.White,
        maxLines = 1,
        modifier = modifier.clip(RoundedCornerShape(8.dp)).background(color).padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

/** Uyarılar'daki cihaz listesinin kategori süzgeci: Tümü + her kategori (sayısıyla). */
@Composable
fun CategoryFilter(counts: Map<DeviceCategory, Int>, selected: DeviceCategory?, onSelect: (DeviceCategory?) -> Unit) {
    Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = selected == null,
            onClick = { onSelect(null) },
            label = { Text(str("category_all") + " (" + counts.values.sum() + ")") },
            modifier = Modifier.testTag("category-filter-all"),
        )
        DeviceCategory.entries.forEach { c ->
            FilterChip(
                selected = selected == c,
                onClick = { onSelect(if (selected == c) null else c) },
                label = { Text(str(c.titleKey()) + " (" + (counts[c] ?: 0) + ")") },
                modifier = Modifier.testTag("category-filter-${c.name}"),
            )
        }
    }
}

/**
 * Uyarı kategorileri sayfası: kategorilerin nasıl belirlendiği, her kategori için cihaz sayısı ve "sessiz saatlerde de
 * alarm çal" seçimi.
 */
@Composable
fun CategoriesPage(graph: AppGraph, strings: Strings) {
    val store = graph.store
    val version by store.version.collectAsState()
    val bypass = remember(version) { store.quietBypass }
    val counts = remember(version) { categoryCounts(graph) }
    PageIntro(str("categories_intro"))
    DeviceCategory.entries.forEach { c ->
        SectionTitle(str(c.titleKey()))
        SectionCard {
            val n = counts[c] ?: 0
            InfoRow(
                Icons.Rounded.Bedtime,
                str("category_quiet"),
                (if (n == 0) str("category_empty") else strings.plural("category_devices_n", n, n)) + " · " +
                    str(if (c in bypass) "category_quiet_on" else "category_quiet_off"),
            ) {
                Switch(
                    checked = c in bypass,
                    onCheckedChange = { on -> store.quietBypass = if (on) bypass + c else bypass - c },
                    modifier = Modifier.testTag("category-quiet-${c.name}"),
                )
            }
        }
    }
}

/** Gerçek cihazların kategorilere göre sayısı (gruplar sayılmaz). */
fun categoryCounts(graph: AppGraph): Map<DeviceCategory, Int> =
    graph.store.snapshots.filterNot { DeviceGroup.isGroup(it.id) }.groupingBy { graph.store.categoryOf(it.id) }.eachCount()
