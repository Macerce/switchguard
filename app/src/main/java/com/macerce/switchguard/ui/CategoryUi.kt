package com.macerce.switchguard.ui

import android.content.res.Resources
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.macerce.switchguard.R
import com.macerce.switchguard.core.DeviceCategory
import com.macerce.switchguard.core.DeviceRules
import com.macerce.switchguard.core.DeviceSnapshot
import com.macerce.switchguard.ui.theme.LocalDarkTheme

fun DeviceCategory.title(): Int = when (this) {
    DeviceCategory.CRITICAL -> R.string.category_critical
    DeviceCategory.IMPORTANT -> R.string.category_important
    DeviceCategory.NORMAL -> R.string.category_normal
}

/** Otomasyon tetikleyicisindeki sanal cihazın adı ("Kritik cihazlardan biri"). */
fun DeviceCategory.anyTitle(): Int = when (this) {
    DeviceCategory.CRITICAL -> R.string.category_any_critical
    DeviceCategory.IMPORTANT -> R.string.category_any_important
    DeviceCategory.NORMAL -> R.string.category_any_normal
}

/** Rozet rengi; normal kategori rozetsizdir. */
fun DeviceCategory.hue(): Hue? = when (this) {
    DeviceCategory.CRITICAL -> Hue.RED
    DeviceCategory.IMPORTANT -> Hue.AMBER
    DeviceCategory.NORMAL -> null
}

/** Otomasyon düzenleyicisi ve kartları için cihazlar: gerçek cihazlar, gruplar ve kategorilerin sanal cihazları. */
fun automationDevices(snapshotsWithGroups: List<DeviceSnapshot>, res: Resources): List<DeviceSnapshot> =
    snapshotsWithGroups + DeviceCategory.snapshots { res.getString(it.anyTitle()) }

/** Kategorinin küçük renkli rozeti (Uyarılar'daki cihaz satırında); normal kategoride çizilmez. */
@Composable
fun CategoryBadge(category: DeviceCategory, modifier: Modifier = Modifier) {
    val hue = category.hue() ?: return
    Text(
        stringResource(category.title()),
        style = MaterialTheme.typography.labelSmall,
        color = Color.White,
        maxLines = 1,
        modifier = modifier.clip(CircleShape).background(if (LocalDarkTheme.current) hue.dark else hue.light)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

/** Uyarılar'daki cihaz listesinin kategori süzgeci: Tümü + her kategori (sayısıyla); dar ekranda yatay kayar. */
@Composable
fun CategoryFilter(counts: Map<DeviceCategory, Int>, selected: DeviceCategory?, onSelect: (DeviceCategory?) -> Unit) {
    Row(
        Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val shape = RoundedCornerShape(8.dp)
        FilterChip(
            selected = selected == null,
            onClick = { onSelect(null) },
            label = { Text(stringResource(R.string.category_all) + " (" + counts.values.sum() + ")") },
            modifier = Modifier.tvFocusRing(shape),
        )
        DeviceCategory.entries.forEach { c ->
            FilterChip(
                selected = selected == c,
                onClick = { onSelect(if (selected == c) null else c) },
                label = { Text(stringResource(c.title()) + " (" + (counts[c] ?: 0) + ")") },
                modifier = Modifier.tvFocusRing(shape),
            )
        }
    }
}

/**
 * Cihaz kurallarındaki uyarı kategorisi: Otomatik (kurallardan; şu anki sonucu yazar) ya da elle Kritik / Önemli /
 * Normal. Kategori sessiz saatleri ve kategori tetikleyicili otomasyonları belirler.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryRow(rules: DeviceRules, onChange: (DeviceCategory?) -> Unit) {
    val auto = DeviceCategory.auto(rules)
    Column(Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Rounded.Label, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(stringResource(R.string.alert_category), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(
                    if (rules.category == null) stringResource(R.string.category_auto_now, stringResource(auto.title()))
                    else stringResource(R.string.alert_category_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        val options = listOf<DeviceCategory?>(null) + DeviceCategory.entries
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            options.forEachIndexed { i, c ->
                val selected = rules.category == c
                SegmentedButton(
                    selected = selected,
                    onClick = { onChange(c) },
                    shape = SegmentedButtonDefaults.itemShape(i, options.size),
                    icon = {},
                ) {
                    Text(
                        stringResource(c?.title() ?: R.string.category_auto),
                        fontWeight = if (selected) FontWeight.SemiBold else null,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/**
 * Uyarı kategorileri sayfasının içeriği: kategorilerin nasıl belirlendiği, her kategori için cihaz sayısı ve
 * "sessiz saatlerde de alarm çal" seçimi.
 */
@Composable
fun CategoriesContent(counts: Map<DeviceCategory, Int>, bypass: Set<DeviceCategory>, onBypass: (Set<DeviceCategory>) -> Unit) {
    Text(
        stringResource(R.string.categories_intro),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 4.dp),
    )
    DeviceCategory.entries.forEach { c ->
        SectionTitle(stringResource(c.title()))
        SectionCard {
            val n = counts[c] ?: 0
            val toggle = { onBypass(if (c in bypass) bypass - c else bypass + c) }
            InfoRow(
                Icons.Rounded.Bedtime,
                stringResource(R.string.category_quiet),
                (if (n == 0) stringResource(R.string.category_empty) else pluralStringResource(R.plurals.category_devices_n, n, n)) +
                    " · " + stringResource(if (c in bypass) R.string.category_quiet_on else R.string.category_quiet_off),
                Modifier.clickable(onClick = toggle),
            ) {
                Switch(checked = c in bypass, onCheckedChange = { toggle() })
            }
        }
    }
}

/** Gerçek cihazların kategorilere göre sayısı. */
fun categoryCounts(devices: List<DeviceSnapshot>, categoryOf: (String) -> DeviceCategory): Map<DeviceCategory, Int> =
    devices.groupingBy { categoryOf(it.id) }.eachCount()
