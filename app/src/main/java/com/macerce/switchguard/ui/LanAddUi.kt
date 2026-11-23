package com.macerce.switchguard.ui

import android.content.Context
import android.net.wifi.WifiManager
import android.text.format.DateUtils
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Router
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.macerce.switchguard.R
import com.macerce.switchguard.api.EwelinkLan
import com.macerce.switchguard.core.Cloud
import com.macerce.switchguard.core.DeviceSnapshot
import com.macerce.switchguard.core.LocalDevice
import com.macerce.switchguard.data.Store
import com.macerce.switchguard.service.MonitorService
import com.macerce.switchguard.ui.theme.StatusColors
import kotlinx.coroutines.delay

/**
 * Telefonun bağlı olduğu Wi-Fi ağının adı. Android ağ adını yalnızca konum izni verilmiş ve konum açıkken söyler;
 * okunamazsa null.
 */
fun currentSsid(context: Context): String? = runCatching {
    @Suppress("DEPRECATION")
    val raw = context.applicationContext.getSystemService(WifiManager::class.java)?.connectionInfo?.ssid
    raw?.removeSurrounding("\"")?.takeIf { it.isNotBlank() && it != WifiManager.UNKNOWN_SSID && it != "unknown ssid" }
}.getOrNull()

/**
 * "Sonoff ekle (yerel ağ)": aynı Wi-Fi'de mDNS ile duyurulan eWeLink/Sonoff cihazlarını listeler. Hesapta olanlar
 * "Ekli"; şifreli olup anahtarı bilinmeyenler eklenemez (DIY kipine alınmalı). Eklenen cihaz Cihazlar ekranına
 * girer ve telefonun o an bağlı olduğu ağ cihazın detayına "Eklendiği ağ" olarak yazılır.
 * İzleme kapalıysa keşif yalnızca bu pencere açıkken çalışır.
 */
@Composable
fun LanAddDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val store = remember { Store.get(context) }
    val version by store.version.collectAsStateWithLifecycle()
    val ssid = remember { currentSsid(context) }
    DisposableEffect(Unit) {
        val started = !EwelinkLan.running
        if (started) {
            EwelinkLan.setKeys(store.lanKeys)
            EwelinkLan.start(context) { _, _ -> }
        }
        onDispose { if (started && !store.monitoringEnabled) EwelinkLan.stop() }
    }
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { while (true) { delay(1500); tick++ } }
    val found = remember(tick) { EwelinkLan.all().entries.sortedBy { it.value.host } }
    val known = remember(version) { store.snapshots.associateBy { it.id } }
    var naming by remember { mutableStateOf<Pair<String, EwelinkLan.Entry>?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Router, null) },
        title = { Text(stringResource(R.string.lan_add_title)) },
        text = {
            Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Wifi, null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        ssid?.let { stringResource(R.string.lan_add_network, it) } ?: stringResource(R.string.lan_network_unknown),
                        style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 8.dp),
                    )
                }
                if (found.isEmpty()) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text(stringResource(R.string.lan_add_searching), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                found.forEach { (id, e) ->
                    val existing = known[id]
                    val addable = existing == null && e.decoded
                    Row(
                        Modifier.fillMaxWidth().tvFocusRing(RoundedCornerShape(8.dp))
                            .then(if (addable) Modifier.clickable { naming = id to e } else Modifier)
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            if (e.decoded || existing != null) Icons.Rounded.Router else Icons.Rounded.Lock, null,
                            tint = if (addable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(existing?.name ?: id, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                listOfNotNull(e.host, if (e.switches.size > 1) stringResource(R.string.lan_channels, e.switches.size) else null)
                                    .joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (existing == null && !e.decoded) {
                                Text(stringResource(R.string.lan_add_encrypted), style = MaterialTheme.typography.bodySmall, color = StatusColors.warning)
                            }
                        }
                        when {
                            existing != null -> Text(
                                stringResource(R.string.lan_add_added), style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            addable -> TextButton(onClick = { naming = id to e }, modifier = Modifier.tvFocusRing()) {
                                Text(stringResource(R.string.board_add))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(modifier = Modifier.tvFocusRing(), onClick = onDismiss) { Text(stringResource(R.string.action_close)) } },
    )
    naming?.let { (id, e) ->
        NameDialog(stringResource(R.string.lan_add), "Sonoff " + id.takeLast(4), onDismiss = { naming = null }) { name ->
            val d = LocalDevice(id, name, channels = e.switches.size.coerceIn(1, 4), ssid = ssid, ip = e.host, addedAt = System.currentTimeMillis())
            store.localDevices = store.localDevices.filterNot { it.id == id } + d
            if (store.monitoringEnabled) MonitorService.send(context, MonitorService.ACTION_RESTART)
            onDismiss()
        }
    }
}

/** Yerel ağdan eklenen cihazın detay kartı: eklendiği Wi-Fi ağı, şu anki adresi ve kaldırma. */
@Composable
fun LocalDeviceCard(device: DeviceSnapshot, onRemoved: () -> Unit) {
    if (device.cloud != Cloud.LAN) return
    val context = LocalContext.current
    val store = remember { Store.get(context) }
    val version by store.version.collectAsStateWithLifecycle()
    val local = remember(version) { store.localDevices.firstOrNull { it.id == device.id } } ?: return
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(device.id) { while (true) { delay(3000); tick++ } }
    val entry = remember(tick) { EwelinkLan.entry(device.id) }
    val ssidNow = remember(tick) { currentSsid(context) }
    var confirm by remember { mutableStateOf(false) }

    SectionTitle(stringResource(R.string.lan_device_title))
    SectionCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                HueIcon(Icons.Rounded.Wifi, Hue.TEAL, size = 32.dp)
                Column(Modifier.padding(start = 12.dp).weight(1f)) {
                    val added = if (local.addedAt > 0) DateUtils.formatDateTime(context, local.addedAt, DateUtils.FORMAT_SHOW_DATE) else ""
                    Text(
                        stringResource(R.string.lan_device_network, local.ssid ?: stringResource(R.string.lan_network_unnamed)),
                        style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium,
                    )
                    Text(
                        listOfNotNull(local.ip?.let { stringResource(R.string.lan_device_ip, it) }, added.ifEmpty { null }).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                if (entry != null) stringResource(R.string.route_seen, entry.host) else stringResource(R.string.route_not_seen),
                style = MaterialTheme.typography.bodyMedium,
                color = if (entry != null) MaterialTheme.colorScheme.onSurface else StatusColors.warning,
            )
            if (entry == null && local.ssid != null && ssidNow != null && ssidNow != local.ssid) {
                Text(stringResource(R.string.lan_device_other_network, ssidNow, local.ssid), style = MaterialTheme.typography.bodySmall, color = StatusColors.warning)
            }
            if (!store.monitoringEnabled) {
                Text(stringResource(R.string.lan_device_needs_monitoring), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = { confirm = true }, modifier = Modifier.tvFocusRing()) { Text(stringResource(R.string.lan_device_remove)) }
        }
    }
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text(stringResource(R.string.lan_device_remove)) },
            text = { Text(device.name) },
            confirmButton = {
                TextButton(modifier = Modifier.tvFocusRing(), onClick = {
                    confirm = false
                    store.localDevices = store.localDevices.filterNot { it.id == device.id }
                    onRemoved()
                }) { Text(stringResource(R.string.lan_device_remove)) }
            },
            dismissButton = { TextButton(modifier = Modifier.tvFocusRing(), onClick = { confirm = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}
