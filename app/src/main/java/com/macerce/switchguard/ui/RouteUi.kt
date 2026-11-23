package com.macerce.switchguard.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Router
import androidx.compose.material.icons.rounded.SyncAlt
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.macerce.switchguard.R
import com.macerce.switchguard.api.EwelinkLan
import com.macerce.switchguard.core.Cloud
import com.macerce.switchguard.core.ControlRoute
import com.macerce.switchguard.core.DeviceSnapshot
import com.macerce.switchguard.data.Store
import com.macerce.switchguard.ui.theme.StatusColors
import kotlinx.coroutines.delay

/**
 * Cihaz detayında "Bağlantı yolu": eWeLink cihazının yerel ağdan mı, buluttan mı, yoksa ikisinden (önce yerel ağ)
 * mi kontrol edileceği ve şu an yerel ağda görülüp görülmediği. Tuya cihazlarında yerel ağ yok: gösterilmez.
 */
@Composable
fun RouteCard(device: DeviceSnapshot) {
    if (device.cloud != Cloud.EWELINK) return
    val context = LocalContext.current
    val store = remember { Store.get(context) }
    val version by store.version.collectAsStateWithLifecycle()
    val lanOn = remember(version) { store.lanControl }
    val route = remember(version) { store.lanRoutes[device.id] ?: ControlRoute.AUTO }
    // Yerel ağ kaydı servis içinde tutulur; ekran açıkken birkaç saniyede bir bakılır.
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(device.id) { while (true) { delay(3000); tick++ } }
    val entry = remember(tick, version) { EwelinkLan.entry(device.id) }

    SectionTitle(stringResource(R.string.route_title))
    SectionCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                HueIcon(if (entry != null) Icons.Rounded.Router else Icons.Rounded.Cloud, if (entry != null) Hue.TEAL else Hue.BLUE, size = 32.dp)
                Column(Modifier.padding(start = 12.dp).weight(1f)) {
                    Text(
                        when {
                            !lanOn -> stringResource(R.string.route_lan_disabled)
                            entry != null -> stringResource(R.string.route_seen, entry.host)
                            else -> stringResource(R.string.route_not_seen)
                        },
                        style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium,
                    )
                    Text(
                        stringResource(
                            when (if (lanOn) route else ControlRoute.CLOUD) {
                                ControlRoute.AUTO -> R.string.route_auto_desc
                                ControlRoute.LAN -> R.string.route_lan_desc
                                ControlRoute.CLOUD -> R.string.route_cloud_desc
                            },
                        ),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (lanOn) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        Triple(ControlRoute.AUTO, R.string.route_auto, Icons.Rounded.SyncAlt),
                        Triple(ControlRoute.LAN, R.string.route_lan, Icons.Rounded.Router),
                        Triple(ControlRoute.CLOUD, R.string.route_cloud, Icons.Rounded.Cloud),
                    ).forEach { (r, label, icon) ->
                        ChoicePill(stringResource(label), selected = route == r, icon = icon, onClick = {
                            // Yol her komutta ve her durum güncellemesinde okunur: servisi yeniden başlatmak gerekmez.
                            if (route != r) store.lanRoutes = store.lanRoutes + (device.id to r)
                        })
                    }
                }
                if (route == ControlRoute.LAN && entry == null) {
                    Text(stringResource(R.string.route_lan_warn), style = MaterialTheme.typography.bodySmall, color = StatusColors.warning)
                }
            }
        }
    }
}
