package com.macerce.switchguard.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoMode
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.macerce.switchguard.data.ConnState
import com.macerce.switchguard.desktop.AppState.Tab
import com.macerce.switchguard.desktop.i18n.Strings
import com.macerce.switchguard.desktop.ui.settings.appVersion

/** Sekmenin adı (metin anahtarı), simgesi ve klavye kısayolu (Ctrl+1 … Ctrl+5). */
internal fun Tab.label(): String = when (this) {
    Tab.DEVICES -> "tab_devices"
    Tab.ALERTS -> "tab_alerts"
    Tab.AUTOMATIONS -> "tab_automations"
    Tab.HISTORY -> "tab_history"
    Tab.SETTINGS -> "tab_settings"
}

internal fun Tab.icon(): ImageVector = when (this) {
    Tab.DEVICES -> Icons.Rounded.Devices
    Tab.ALERTS -> Icons.Rounded.NotificationsActive
    Tab.AUTOMATIONS -> Icons.Rounded.AutoMode
    Tab.HISTORY -> Icons.Rounded.History
    Tab.SETTINGS -> Icons.Rounded.Settings
}

/**
 * Pencerenin sol kenar çubuğu (Windows 11 Ayarlar uygulaması gibi): üstte uygulamanın adı ve simgesi, etiketli
 * sekmeler (seçilide solda marka renginde dikey çizgi, fareyle üzerine gelince hafif zemin), altta izlemenin durumu ve
 * başlat/durdur. Ayarlar sekmesi altta ayrı durur. Telefondaki alt menünün masaüstü karşılığı.
 */
@Composable
fun Sidebar(
    tab: Tab,
    unseen: Int,
    strings: Strings,
    onSelect: (Tab) -> Unit,
    state: ConnState,
    statusText: String,
    monitoring: Boolean,
    onToggleMonitoring: () -> Unit,
) {
    Row(Modifier.fillMaxHeight()) {
        Column(
            Modifier.width(232.dp).fillMaxHeight().background(sidebarColor()).padding(horizontal = 8.dp, vertical = 12.dp),
        ) {
            // Uygulamanın adı
            Row(Modifier.padding(start = 10.dp, top = 4.dp, bottom = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(28.dp).clip(RoundedCornerShape(7.dp)).background(Brand.guard(LocalDarkTheme.current)),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Shield, null, tint = Color.White, modifier = Modifier.size(17.dp)) }
                Spacer(Modifier.width(10.dp))
                Text(strings["app_name"], style = MaterialTheme.typography.titleMedium)
            }
            Tab.entries.filter { it != Tab.SETTINGS }.forEach { t ->
                NavItem(t, strings, selected = tab == t, badge = if (t == Tab.HISTORY) unseen else 0) { onSelect(t) }
            }
            Spacer(Modifier.weight(1f))
            MonitorBox(state, statusText, monitoring, strings, onToggleMonitoring)
            Spacer(Modifier.height(8.dp))
            NavItem(Tab.SETTINGS, strings, selected = tab == Tab.SETTINGS, badge = 0) { onSelect(Tab.SETTINGS) }
        }
        VerticalDivider(color = cardStroke())
    }
}

@Composable
private fun NavItem(t: Tab, strings: Strings, selected: Boolean, badge: Int, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val hover = remember { MutableInteractionSource() }
    val hovered by hover.collectIsHoveredAsState()
    val bg = when {
        selected -> scheme.onSurface.copy(alpha = 0.07f)
        hovered -> scheme.onSurface.copy(alpha = 0.04f)
        else -> Color.Transparent
    }
    Row(
        Modifier.fillMaxWidth().height(38.dp).padding(vertical = 1.dp).clip(RoundedCornerShape(4.dp)).background(bg)
            .hoverable(hover)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .testTag("nav-${t.name}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Seçili sekmenin solundaki marka çizgisi (Fluent NavigationView).
        Box(Modifier.width(3.dp).height(16.dp).clip(RoundedCornerShape(2.dp)).background(if (selected) scheme.primary else Color.Transparent))
        Spacer(Modifier.width(10.dp))
        Icon(t.icon(), null, Modifier.size(19.dp), tint = if (selected) scheme.primary else scheme.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Text(
            strings[t.label()], style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        if (badge > 0) {
            Box(
                Modifier.padding(end = 10.dp).height(18.dp).clip(CircleShape).background(scheme.error).padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(if (badge > 99) "99+" else badge.toString(), style = MaterialTheme.typography.labelSmall, color = scheme.onError)
            }
        }
    }
}

/** Kenar çubuğunun altındaki izleme kutusu: renkli nokta, durum ve başlat/durdur. */
@Composable
private fun MonitorBox(state: ConnState, text: String, monitoring: Boolean, strings: Strings, onToggle: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(tileColor()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(StatusColors.of(state)))
            Spacer(Modifier.width(8.dp))
            Text(
                text.ifEmpty { strings["state_stopped"] }, style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
        }
        FilledTonalButton(onClick = onToggle, modifier = Modifier.fillMaxWidth().testTag("sidebar-monitoring")) {
            Icon(if (monitoring) Icons.Rounded.Stop else Icons.Rounded.PlayArrow, null, Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(strings[if (monitoring) "action_stop_monitoring" else "action_start_monitoring"], style = MaterialTheme.typography.labelLarge)
        }
    }
}

/**
 * Pencerenin altındaki durum çubuğu (masaüstü uygulamalarının alışılmış satırı): izleme durumu, cihaz sayısı, son
 * bulut kontrolü ve sürüm.
 */
@Composable
fun StatusBar(state: ConnState, text: String, devices: Int, lastSync: Long, strings: Strings) {
    val scheme = MaterialTheme.colorScheme
    Column {
        HorizontalDivider(color = cardStroke())
        Row(
            Modifier.fillMaxWidth().height(26.dp).background(sidebarColor()).padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(StatusColors.of(state)))
            Text(text.ifEmpty { strings["state_stopped"] }, style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false).padding(start = 0.dp))
            Spacer(Modifier.weight(1f))
            Text(strings.plural("desktop_status_devices", devices, devices), style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
            if (lastSync > 0) {
                Text(strings.format("desktop_status_synced", strings.time(lastSync)), style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
            }
            Text("v" + appVersion(), style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant.copy(alpha = 0.7f))
        }
    }
}

/** Kenar çubuğundaki logonun gradyanı ile aynı: başka yerde de kullanılabilsin. */
internal fun brandBrush(dark: Boolean): Brush = Brand.guard(dark)
