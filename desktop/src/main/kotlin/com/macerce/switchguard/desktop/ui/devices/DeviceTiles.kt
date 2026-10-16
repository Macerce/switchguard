package com.macerce.switchguard.desktop.ui.devices

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.macerce.switchguard.core.DeviceKinds
import com.macerce.switchguard.core.DeviceSnapshot
import com.macerce.switchguard.desktop.ui.Brand
import com.macerce.switchguard.desktop.ui.IconBadge
import com.macerce.switchguard.desktop.ui.LocalDarkTheme
import com.macerce.switchguard.desktop.ui.PowerButton
import com.macerce.switchguard.desktop.ui.StatusColors
import com.macerce.switchguard.desktop.ui.icon
import com.macerce.switchguard.desktop.ui.raisedOnTile
import com.macerce.switchguard.desktop.ui.str
import com.macerce.switchguard.desktop.ui.tileColor

// Telefondaki DevicesScreen.kt'nin (2.12) kutucukları. Farklar: TV odak grubu yok; uzun basış düzenleme modunu
// açmaz (masaüstünde basılı tutup sürüklemek sıralar, bkz. DevicesScreen); kutucuğun üstünde fare el imleci.

/**
 * Cihaz kutucuğu. Küçüğü yarım satırdır (ızgarada bir hücre): simge, ad, durum ve tek kanallıysa güç düğmesi. Büyüğü
 * tam satırdır; çok kanallı cihazda her kanal için ayrı düğme taşır. Açık (ve çevrimiçi) tek kanallı cihazın kutucuğu
 * marka renginde parlar; çevrimdışı cihaz soluk görünür. Kutucuğa tıklamak cihaz sayfasını açar ([onClick]);
 * düğmeler kendi tıklamalarını alır. [editBar] doluysa (düzenleme modu) denetimler gizlenir, altına araçlar gelir.
 */
@Composable
fun DeviceTile(
    device: DeviceSnapshot,
    large: Boolean,
    monitored: Boolean,
    priority: Boolean,
    onClick: () -> Unit,
    editBar: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val dark = LocalDarkTheme.current
    val anyOn = device.switches.values.any { it }
    // Çok kanallıda kanal düğmeleri kendi durumunu gösterir; kutucuğun kendisi renklenmez.
    val active = anyOn && device.online && !device.isMultiChannel && editBar == null
    val shape = RoundedCornerShape(24.dp)
    val background: Brush = if (active) Brand.active(dark) else SolidColor(tileColor())
    val controls = editBar == null

    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(background)
            .testTag("tile-${device.id}"),
    ) {
        Box {
            // Zemin: tıklayınca cihaz sayfası. İçerik onun üstündedir; düğmeler kendi tıklamalarını alır.
            if (controls) {
                Box(
                    Modifier
                        .matchParentSize()
                        .pointerHoverIcon(PointerIcon.Hand)
                        .clickable(onClick = onClick, role = Role.Button),
                )
            }
            if (large) LargeTileContent(device, active, monitored, priority, controls)
            else SmallTileContent(device, active, monitored, priority, controls)
        }
        editBar?.invoke()
    }
}

/** Kutucuğun simgesi: cihaz türüne göre; açıkken beyaz halka, kapalıyken sade, çevrimdışıyken bulut. */
@Composable
fun DeviceGlyph(device: DeviceSnapshot, active: Boolean, size: Dp = 42.dp) {
    val scheme = MaterialTheme.colorScheme
    val kind = remember(device.name, device.isMultiChannel, device.hasEnergy, device.cloud) { DeviceKinds.of(device) }
    val anyOn = device.switches.values.any { it }
    val raised = raisedOnTile()
    val (bg, tint) = when {
        !device.online -> raised to scheme.outline
        active -> Color.White.copy(alpha = 0.2f) to Color.White
        anyOn -> scheme.primaryContainer to scheme.onPrimaryContainer
        else -> raised to scheme.onSurfaceVariant
    }
    IconBadge(if (device.online) kind.icon() else Icons.Rounded.CloudOff, tint, bg, size)
}

/** Durum satırı: "Açık · 118 W", "2/4 açık", "Kapalı" ya da kırmızı "Çevrimdışı"; sağda izleme zili. */
@Composable
private fun TileStatus(device: DeviceSnapshot, active: Boolean, monitored: Boolean) {
    val muted = if (active) Brand.onActive.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (!device.online) {
            Box(Modifier.size(8.dp).background(StatusColors.offline, CircleShape))
            Spacer(Modifier.width(6.dp))
        }
        val onCount = device.switches.values.count { it }
        val state = when {
            !device.online -> str("label_offline_title")
            device.isMultiChannel -> str("channels_on_short", onCount, device.switches.size)
            onCount > 0 -> str("state_on_title")
            else -> str("state_off_title")
        }
        val text = if (device.online && device.hasEnergy) state + " · " + watts(device.power ?: 0.0) else state
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (!device.online) StatusColors.offline else muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        Spacer(Modifier.width(6.dp))
        Icon(
            if (monitored) Icons.Rounded.NotificationsActive else Icons.Rounded.NotificationsOff,
            contentDescription = str(if (monitored) "monitored" else "not_monitored"),
            tint = when {
                active -> Brand.onActive.copy(alpha = if (monitored) 0.95f else 0.6f)
                monitored -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.outline
            },
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun TileName(device: DeviceSnapshot, active: Boolean, priority: Boolean, large: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            device.name,
            style = if (large) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleSmall,
            color = if (active) Brand.onActive else MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (priority) {
            Icon(
                Icons.Rounded.Star,
                contentDescription = str("section_priority"),
                tint = if (active) Brand.onActive else MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp).size(16.dp),
            )
        }
    }
}

@Composable
private fun SmallTileContent(device: DeviceSnapshot, active: Boolean, monitored: Boolean, priority: Boolean, controls: Boolean) {
    // En az yükseklik: aynı satırdaki kutucuklar (bir ad tek, biri iki satır) aynı boyda görünsün.
    Column(
        Modifier.fillMaxWidth().heightIn(min = 148.dp).padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.Top) {
            DeviceGlyph(device, active)
            Spacer(Modifier.weight(1f))
            if (controls && !device.isMultiChannel && device.switches.size == 1) {
                val (channel, on) = device.switches.entries.first()
                val control = rememberSwitchControl(device, channel, on)
                PowerButton(control.shown, control.busy, device.online, control.toggle, size = 42.dp, onBrand = active)
            }
        }
        Column(Modifier.padding(top = 14.dp)) {
            TileName(device, active, priority, large = false)
            Spacer(Modifier.height(2.dp))
            TileStatus(device, active, monitored)
        }
    }
}

@Composable
private fun LargeTileContent(device: DeviceSnapshot, active: Boolean, monitored: Boolean, priority: Boolean, controls: Boolean) {
    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            DeviceGlyph(device, active, size = 46.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                TileName(device, active, priority, large = true)
                Spacer(Modifier.height(2.dp))
                TileStatus(device, active, monitored)
            }
            if (controls && !device.isMultiChannel && device.switches.size == 1) {
                Spacer(Modifier.width(12.dp))
                val (channel, on) = device.switches.entries.first()
                val control = rememberSwitchControl(device, channel, on)
                PowerButton(control.shown, control.busy, device.online, control.toggle, size = 48.dp, onBrand = active)
            }
        }
        if (controls && device.isMultiChannel) {
            Spacer(Modifier.height(14.dp))
            ChannelButtons(device)
        }
    }
}

/** Çok kanallı cihazın kanal düğmeleri: satır başına en çok dört, her biri kanal numarası ve (ölçülüyorsa) gücü. */
@Composable
fun ChannelButtons(device: DeviceSnapshot, modifier: Modifier = Modifier, height: Dp = 64.dp) {
    val channels = device.switches.toSortedMap().entries.toList()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        channels.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (channel, on) ->
                    ChannelButton(device, channel, on, Modifier.weight(1f).height(height))
                }
                // Son satır eksikse düğmeler genişlemesin: boş yer tutucular.
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun ChannelButton(device: DeviceSnapshot, channel: Int, on: Boolean, modifier: Modifier) {
    val control = rememberSwitchControl(device, channel, on)
    val scheme = MaterialTheme.colorScheme
    val shown = control.shown
    val enabled = device.online
    val shape = RoundedCornerShape(18.dp)
    val bg = when {
        !enabled -> Color.Transparent
        shown -> scheme.primary
        else -> raisedOnTile()
    }
    val fg = when {
        !enabled -> scheme.outline
        shown -> scheme.onPrimary
        else -> scheme.onSurfaceVariant
    }
    val label = str("channel_n", channel + 1)
    val state = str(if (shown) "state_on_title" else "state_off_title")
    Box(
        modifier
            .clip(shape)
            .background(bg)
            .then(if (!shown && enabled) Modifier.border(1.dp, scheme.outlineVariant, shape) else Modifier)
            .clickable(enabled = enabled, role = Role.Switch) { if (!control.busy) control.toggle(!shown) }
            .semantics(mergeDescendants = true) { contentDescription = "$label, $state" }
            .testTag("channel-${device.id}-$channel"),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.PowerSettingsNew, null, tint = fg, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("${channel + 1}", style = MaterialTheme.typography.titleMedium, color = fg)
            }
            val w = device.channelPower[channel]
            if (w != null && enabled) {
                Text(watts(w), style = MaterialTheme.typography.labelSmall, color = fg.copy(alpha = 0.85f), maxLines = 1)
            }
        }
        if (control.busy) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = fg)
    }
}
