package com.macerce.switchguard.ui

import android.content.Context
import android.net.wifi.WifiManager
import android.text.format.DateUtils
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Router
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.macerce.switchguard.R
import com.macerce.switchguard.api.EwelinkLan
import com.macerce.switchguard.api.LocalLan
import com.macerce.switchguard.api.ShellyLan
import com.macerce.switchguard.core.Cloud
import com.macerce.switchguard.core.DeviceSnapshot
import com.macerce.switchguard.core.LocalDevice
import com.macerce.switchguard.core.ShellyAuth
import com.macerce.switchguard.data.Store
import com.macerce.switchguard.service.MonitorService
import com.macerce.switchguard.ui.theme.StatusColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
 * "Cihaz ekle (yerel ağ)": rehberli ekleme. Önce marka seçilir; marka sayfasında numaralı hazırlık adımları,
 * canlı "Bu ağda bulunanlar" listesi, (Shelly'de) "IP adresiyle ekle" ve "Bulamıyor musun?" sorun giderme bölümü
 * vardır. Eklenen cihaz Cihazlar ekranına girer; telefonun o an bağlı olduğu ağ detayına "Eklendiği ağ" olarak yazılır.
 * İzleme kapalıysa arama yalnızca bu pencere açıkken çalışır.
 */
@Composable
fun LanAddDialog(onDismiss: () -> Unit) {
    var brand by remember { mutableStateOf<String?>(null) }
    val b = brand
    if (b == null) BrandPicker(onDismiss) { brand = it } else BrandPage(b, onBack = { brand = null }, onDismiss = onDismiss)
}

private fun brandName(brand: String) = if (brand == LocalDevice.BRAND_SHELLY) "Shelly" else "Sonoff"

@Composable
private fun BrandPicker(onDismiss: () -> Unit, onPick: (String) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Router, null) },
        title = { Text(stringResource(R.string.lan_add_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.lan_brand_hint), style = MaterialTheme.typography.bodyMedium)
                listOf(
                    LocalDevice.BRAND_SHELLY to R.string.lan_brand_shelly_desc,
                    LocalDevice.BRAND_SONOFF to R.string.lan_brand_sonoff_desc,
                ).forEach { (b, desc) ->
                    Row(
                        Modifier.fillMaxWidth().tvFocusRing(RoundedCornerShape(12.dp)).clickable { onPick(b) }.padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        HueIcon(Icons.Rounded.Router, if (b == LocalDevice.BRAND_SHELLY) Hue.BLUE else Hue.TEAL, size = 36.dp)
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(brandName(b), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(stringResource(desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Text(stringResource(R.string.lan_brand_soon), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { TextButton(modifier = Modifier.tvFocusRing(), onClick = onDismiss) { Text(stringResource(R.string.action_close)) } },
    )
}

/** Bulunan bir cihazın listedeki satırı (markalarda ortak). */
private class FoundRow(
    val id: String, val title: String, val detail: String, val addable: Boolean, val warning: String?,
    val defaultName: String,
    /** Şifreli Shelly: dokununca önce şifre sorulur. [credentials]: doğrulanmış şifre (eklenince kaydedilir). */
    val shelly: ShellyLan.Found? = null, val credentials: ShellyAuth.Credentials? = null,
    val make: (name: String, ssid: String?) -> LocalDevice,
)

@Composable
private fun BrandPage(brand: String, onBack: () -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val store = remember { Store.get(context) }
    val version by store.version.collectAsStateWithLifecycle()
    val ssid = remember { currentSsid(context) }
    val shelly = brand == LocalDevice.BRAND_SHELLY
    DisposableEffect(brand) {
        if (shelly) {
            ShellyLan.setCredentials(store.shellyAuth)
            ShellyLan.startDiscovery(context)
            onDispose { ShellyLan.stopDiscoveryIfIdle() }
        } else {
            val started = !EwelinkLan.running
            if (started) {
                EwelinkLan.setKeys(store.lanKeys)
                EwelinkLan.start(context) { _, _ -> }
            }
            onDispose { if (started && !store.monitoringEnabled) EwelinkLan.stop() }
        }
    }
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { while (true) { delay(1500); tick++ } }
    val known = remember(version) { store.snapshots.associateBy { it.id } }
    // IP ile bulunan (mDNS'te görünmeyen) ve şifresi doğrulanan Shelly'ler de listeye girer (aramadakinin yerine geçer).
    var manual by remember { mutableStateOf<Map<String, ShellyLan.Found>>(emptyMap()) }
    val rows = remember(tick, version, manual) {
        val res = resources
        fun ch(n: Int) = if (n > 1) res.getString(R.string.lan_channels, n) else null
        if (shelly) (ShellyLan.all() + manual).values.sortedBy { it.host }.map { f ->
            val existing = known[f.info.id]
            val model = "Shelly ${f.info.model}".trim()
            FoundRow(
                f.info.id, existing?.name ?: model, listOfNotNull(f.host, ch(f.info.channels)).joinToString(" · "),
                addable = existing == null && (f.supported || f.locked),
                warning = when {
                    existing != null -> null
                    f.locked -> res.getString(R.string.lan_shelly_auth)
                    !f.supported -> res.getString(R.string.lan_shelly_unsupported)
                    else -> null
                },
                defaultName = model, shelly = f, credentials = f.credentials,
            ) { name, net ->
                LocalDevice(f.info.id, name, f.info.channels.coerceIn(1, 4), net, f.host, System.currentTimeMillis(), LocalDevice.BRAND_SHELLY, f.info.gen)
            }
        } else EwelinkLan.all().entries.sortedBy { it.value.host }.map { (id, e) ->
            val existing = known[id]
            FoundRow(
                id, existing?.name ?: id, listOfNotNull(e.host, ch(e.switches.size)).joinToString(" · "),
                addable = existing == null && e.decoded,
                warning = if (existing == null && !e.decoded) res.getString(R.string.lan_add_encrypted) else null,
                defaultName = "Sonoff " + id.takeLast(4),
            ) { name, net -> LocalDevice(id, name, e.switches.size.coerceIn(1, 4), net, e.host, System.currentTimeMillis()) }
        }
    }
    var naming by remember { mutableStateOf<FoundRow?>(null) }
    var askPassword by remember { mutableStateOf<ShellyLan.Found?>(null) }
    var help by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Router, null) },
        title = { Text(stringResource(R.string.lan_brand_title, brandName(brand))) },
        text = {
            Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Wifi, null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        ssid?.let { stringResource(R.string.lan_add_network, it) } ?: stringResource(R.string.lan_network_unknown),
                        style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 8.dp),
                    )
                }
                Text(stringResource(R.string.lan_steps_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                val steps = if (shelly) listOf(R.string.lan_shelly_step1, R.string.lan_shelly_step2, R.string.lan_shelly_step3)
                else listOf(R.string.lan_sonoff_step1, R.string.lan_sonoff_step2, R.string.lan_sonoff_step3)
                steps.forEachIndexed { i, res -> StepLine(i + 1, stringResource(res)) }

                Text(
                    stringResource(R.string.lan_found_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp),
                )
                if (rows.isEmpty()) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text(stringResource(R.string.lan_add_searching), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                rows.forEach { r ->
                    FoundLine(r, added = known.containsKey(r.id)) { if (r.shelly?.locked == true) askPassword = r.shelly else naming = r }
                }

                if (shelly) IpAdd { f -> manual = manual + (f.info.id to f) }

                Row(
                    Modifier.fillMaxWidth().tvFocusRing(RoundedCornerShape(8.dp)).clickable { help = !help }.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.AutoMirrored.Rounded.HelpOutline, null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        stringResource(R.string.lan_help_title), style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f).padding(start = 8.dp),
                    )
                    Icon(if (help) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null, tint = MaterialTheme.colorScheme.primary)
                }
                if (help) Text(stringResource(R.string.lan_help_body), style = MaterialTheme.typography.bodySmall)
            }
        },
        dismissButton = { TextButton(modifier = Modifier.tvFocusRing(), onClick = onBack) { Text(stringResource(R.string.action_back)) } },
        confirmButton = { TextButton(modifier = Modifier.tvFocusRing(), onClick = onDismiss) { Text(stringResource(R.string.action_close)) } },
    )
    naming?.let { r ->
        NameDialog(stringResource(R.string.lan_add), r.defaultName, onDismiss = { naming = null }, label = R.string.automation_name) { name ->
            val d = r.make(name, ssid)
            r.credentials?.let { c ->
                store.shellyAuth = store.shellyAuth + (d.id to c)
                ShellyLan.setCredentials(store.shellyAuth)
            }
            store.localDevices = store.localDevices.filterNot { it.id == d.id } + d
            if (store.monitoringEnabled) MonitorService.send(context, MonitorService.ACTION_RESTART)
            naming = null
            Toast.makeText(context, resources.getString(R.string.lan_added_toast, name), Toast.LENGTH_SHORT).show()
            onDismiss()
        }
    }
    askPassword?.let { f ->
        ShellyPasswordDialog(f, onDismiss = { askPassword = null }) { ok ->
            askPassword = null
            manual = manual + (ok.info.id to ok)
            // Şifre doğru ve rölesi var: doğrudan ad sorulur; rölesizse satırda "desteklenmiyor" görünür.
            if (ok.supported) {
                val model = "Shelly ${ok.info.model}".trim()
                naming = FoundRow(ok.info.id, model, ok.host, true, null, model, ok, ok.credentials) { name, net ->
                    LocalDevice(ok.info.id, name, ok.info.channels.coerceIn(1, 4), net, ok.host, System.currentTimeMillis(), LocalDevice.BRAND_SHELLY, ok.info.gen)
                }
            }
        }
    }
}

/**
 * Şifreli Shelly'nin giriş bilgisi. Gen1'de kullanıcı adı ayarlanabilir (varsayılan admin), Gen2+'da her zaman admin
 * olduğu için yalnızca şifre sorulur. Şifre cihazdan durum okunarak doğrulanır; doğruysa [onVerified].
 */
@Composable
private fun ShellyPasswordDialog(found: ShellyLan.Found, onDismiss: () -> Unit, onVerified: (ShellyLan.Found) -> Unit) {
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    var user by remember { mutableStateOf(ShellyAuth.DEFAULT_USER) }
    var password by remember { mutableStateOf("") }
    var checking by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Lock, null) },
        title = { Text(stringResource(R.string.lan_shelly_pw_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Shelly ${found.info.model} · ${found.host}", style = MaterialTheme.typography.titleSmall)
                Text(stringResource(R.string.lan_shelly_pw_body), style = MaterialTheme.typography.bodySmall)
                if (found.info.gen <= 1) OutlinedTextField(
                    value = user, onValueChange = { user = it.take(64); error = null }, singleLine = true,
                    label = { Text(stringResource(R.string.lan_shelly_user)) }, modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = password, onValueChange = { password = it.take(128); error = null }, singleLine = true,
                    label = { Text(stringResource(R.string.lan_shelly_pw)) }, visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth(),
                )
                if (checking) LinearProgressIndicator(Modifier.fillMaxWidth())
                error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = StatusColors.warning) }
            }
        },
        dismissButton = { TextButton(modifier = Modifier.tvFocusRing(), onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
        confirmButton = {
            TextButton(
                enabled = !checking && password.isNotEmpty() && user.isNotBlank(), modifier = Modifier.tvFocusRing(),
                onClick = {
                    checking = true
                    val cred = ShellyAuth.Credentials(user.trim(), password)
                    scope.launch {
                        val r = withContext(Dispatchers.IO) { ShellyLan.probe(found.host, cred) }
                        checking = false
                        when {
                            r == null -> error = resources.getString(R.string.lan_ip_not_found, found.host)
                            r.wrongPassword || r.locked -> error = resources.getString(R.string.lan_shelly_pw_wrong)
                            else -> onVerified(r)
                        }
                    }
                },
            ) { Text(stringResource(if (checking) R.string.lan_ip_checking else R.string.lan_shelly_pw_ok)) }
        },
    )
}

@Composable
private fun StepLine(n: Int, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Text(
            "$n", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.background(MaterialTheme.colorScheme.primary, CircleShape).size(22.dp).wrapContentSize(Alignment.Center),
        )
        Text(text, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 10.dp).weight(1f))
    }
}

@Composable
private fun FoundLine(r: FoundRow, added: Boolean, onAdd: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().tvFocusRing(RoundedCornerShape(8.dp))
            .then(if (r.addable) Modifier.clickable(onClick = onAdd) else Modifier)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (r.warning == null) Icons.Rounded.Router else Icons.Rounded.Lock, null,
            tint = if (r.addable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(r.title, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(r.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            r.warning?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = StatusColors.warning) }
        }
        when {
            added -> Text(stringResource(R.string.lan_add_added), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            r.addable -> TextButton(onClick = onAdd, modifier = Modifier.tvFocusRing()) { Text(stringResource(R.string.board_add)) }
        }
    }
}

/** "IP adresiyle ekle": mDNS'te görünmeyen Shelly'yi adresinden sorgular; bulunursa listeye girer. */
@Composable
private fun IpAdd(onFound: (ShellyLan.Found) -> Unit) {
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    var ip by remember { mutableStateOf("") }
    var checking by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    Text(
        stringResource(R.string.lan_add_by_ip), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 4.dp),
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = ip, onValueChange = { ip = it.filter { c -> c.isDigit() || c == '.' || c == ':' }.take(21); error = null },
            singleLine = true, placeholder = { Text(stringResource(R.string.lan_ip_hint)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri), modifier = Modifier.weight(1f),
        )
        TextButton(
            enabled = !checking && ip.trim().count { it == '.' } == 3, modifier = Modifier.tvFocusRing(),
            onClick = {
                checking = true
                val host = ip.trim()
                scope.launch {
                    val f = withContext(Dispatchers.IO) { ShellyLan.probe(host) }
                    checking = false
                    if (f == null) error = resources.getString(R.string.lan_ip_not_found, host) else onFound(f)
                }
            },
        ) { Text(stringResource(if (checking) R.string.lan_ip_checking else R.string.lan_ip_find)) }
    }
    error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = StatusColors.warning) }
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
    val seenHost = remember(tick) { LocalLan.seenHost(local) }
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
                        listOfNotNull(brandName(local.brand), local.ip?.let { stringResource(R.string.lan_device_ip, it) }, added.ifEmpty { null })
                            .joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                if (seenHost != null) stringResource(R.string.route_seen, seenHost) else stringResource(R.string.route_not_seen),
                style = MaterialTheme.typography.bodyMedium,
                color = if (seenHost != null) MaterialTheme.colorScheme.onSurface else StatusColors.warning,
            )
            if (seenHost == null && local.ssid != null && ssidNow != null && ssidNow != local.ssid) {
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
                    if (device.id in store.shellyAuth) store.shellyAuth = store.shellyAuth - device.id
                    onRemoved()
                }) { Text(stringResource(R.string.lan_device_remove)) }
            },
            dismissButton = { TextButton(modifier = Modifier.tvFocusRing(), onClick = { confirm = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}
