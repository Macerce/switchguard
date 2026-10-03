package com.macerce.switchguard.ui

import com.macerce.switchguard.core.DeviceSection
import com.macerce.switchguard.core.DeviceLayout
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Star
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import android.text.format.DateUtils
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.AlarmOff
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.macerce.switchguard.R
import com.macerce.switchguard.core.DeviceSnapshot
import com.macerce.switchguard.core.Demo
import com.macerce.switchguard.core.Entitlement
import com.macerce.switchguard.data.ConnState
import com.macerce.switchguard.data.LiveState
import com.macerce.switchguard.data.Store
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicesScreen(modifier: Modifier, onOpenDevice: (String) -> Unit) {
    val context = LocalContext.current
    val store = remember { Store.get(context) }
    val scope = rememberCoroutineScope()
    val version by store.version.collectAsStateWithLifecycle()
    val connection by LiveState.connection.collectAsStateWithLifecycle()

    val devices = remember(version) { store.snapshots }
    val alarm = remember(version) { store.pendingAlarm }
    val monitoring = remember(version) { store.monitoringEnabled }
    // eWeLink'e giriş ya da Tuya bağlantısı: ikisinden biri yeter.
    val loggedIn = remember(version) { store.hasAnyAccount }
    val monitoredIds = remember(version) { store.monitoredIds() }
    val isPro = remember(version) { store.isPro }
    var showPro by remember { mutableStateOf(false) }
    if (showPro) ProDialog(onDismiss = { showPro = false })
    val layout = remember(version) { store.deviceLayout }
    val sections = remember(version) { layout.sections(devices) }
    var editMode by rememberSaveable { mutableStateOf(false) }
    var showCategories by remember { mutableStateOf(false) }
    val saveLayout: (DeviceLayout) -> Unit = { store.deviceLayout = it }
    if (showCategories) CategoriesDialog(layout, saveLayout) { showCategories = false }
    BackHandler(enabled = editMode) { editMode = false }
    var refreshing by remember { mutableStateOf(false) }

    val doRefresh: () -> Unit = {
        scope.launch {
            refreshing = true
            Actions.refresh(context)
            delay(1200) // Servis senkronu kısa sürer; göstergeyi bir süre tut.
            refreshing = false
        }
    }

    Column(modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(R.string.app_name), fontWeight = FontWeight.SemiBold) },
            actions = {
                if (editMode) {
                    TextButton(onClick = { editMode = false }) { Text(stringResource(R.string.action_done)) }
                } else {
                    if (devices.isNotEmpty()) {
                        IconButton(onClick = { editMode = true }) {
                            Icon(Icons.Rounded.Tune, stringResource(R.string.action_edit_layout))
                        }
                    }
                    IconButton(onClick = doRefresh, enabled = loggedIn) {
                        Icon(Icons.Rounded.Refresh, stringResource(R.string.action_refresh))
                    }
                }
            },
        )
        PullToRefreshBox(isRefreshing = refreshing, onRefresh = doRefresh, modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                item {
                    androidx.compose.animation.AnimatedVisibility(alarm.isNotEmpty(), enter = expandVertically(), exit = shrinkVertically()) {
                        AlarmBanner(alarm) { Actions.dismissAlarm(context) }
                    }
                }
                item {
                    if (loggedIn) {
                        ConnectionCard(connection.state, connection.detail, connection.lastSync, monitoring, devices.size)
                    } else {
                        LoginPromptCard(sessionLost = remember(version) { store.sessionLost })
                    }
                }
                if (loggedIn && devices.isEmpty()) {
                    item { EmptyDevices(showTuyaHint = store.hasTuya) }
                }
                if (loggedIn && !isPro && devices.count { !Demo.isDemo(it.id) } > Entitlement.FREE_DEVICES) {
                    item { ProHintCard { showPro = true } }
                }
                if (editMode) item(key = "edit-hint") { LayoutEditHint { showCategories = true } }
                for (section in sections) {
                    // Düzenleme dışında boş kategori başlığı gösterilmez.
                    if (section.devices.isEmpty() && !editMode) continue
                    val headed = section.kind != DeviceSection.Kind.ALL
                    val collapsed = headed && section.key in layout.collapsed && !editMode
                    if (headed) {
                        item(key = "h-" + section.key) {
                            LayoutSectionHeader(sectionTitle(section), section.devices.size, collapsed) {
                                saveLayout(layout.toggleCollapsed(section.key))
                            }
                        }
                    }
                    if (section.devices.isEmpty()) {
                        item(key = "e-" + section.key) {
                            Text(
                                stringResource(R.string.category_no_devices),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 20.dp),
                            )
                        }
                    }
                    if (!collapsed) {
                        itemsIndexed(section.devices, key = { _, d -> d.id }) { i, device ->
                            DeviceCard(
                                device,
                                monitored = device.id in monitoredIds,
                                compact = device.id in layout.compact,
                                priority = device.id in layout.priority,
                                onClick = { if (!editMode) onOpenDevice(device.id) },
                                onLongClick = { editMode = true },
                                editBar = if (editMode) {
                                    { LayoutEditBar(device, layout, devices, canUp = i > 0, canDown = i < section.devices.lastIndex, onChange = saveLayout) }
                                } else null,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AlarmBanner(lines: List<String>, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Alarm, null, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(12.dp))
                Text(
                    stringResource(R.string.alarm_banner_title, lines.size),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(12.dp))
            lines.reversed().take(6).forEach {
                Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 2.dp))
            }
            if (lines.size > 6) {
                Text(stringResource(R.string.alarm_banner_more, lines.size - 6), style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) {
                Icon(Icons.Rounded.AlarmOff, null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.action_dismiss_alarm), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun ConnectionCard(state: ConnState, detail: String, lastSync: Long, monitoring: Boolean, deviceCount: Int) {
    val context = LocalContext.current
    // "2 dk önce" metni kendiliğinden güncellensin.
    val now by produceState(System.currentTimeMillis()) {
        while (true) { delay(30_000); value = System.currentTimeMillis() }
    }
    val shownState = if (monitoring) state else ConnState.STOPPED

    SectionCard {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBadge(
                Icons.Rounded.Shield,
                tint = connColor(shownState),
                background = connColor(shownState).copy(alpha = 0.15f),
                size = 52,
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(connTitle(shownState)), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                val sub = when {
                    !monitoring -> stringResource(R.string.monitoring_off_hint)
                    detail.isNotEmpty() -> detail
                    lastSync > 0 -> stringResource(
                        R.string.last_update,
                        // "now" 30 sn'de bir tazelenir; arada gelen senkron onu geçerse "… sonra" yazmasın.
                        DateUtils.getRelativeTimeSpanString(lastSync, maxOf(now, lastSync), DateUtils.SECOND_IN_MILLIS).toString(),
                    )
                    else -> stringResource(R.string.device_count, deviceCount)
                }
                Text(sub, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.surface)
        Box(Modifier.fillMaxWidth().padding(12.dp)) {
            if (monitoring) {
                FilledTonalButton(onClick = { Actions.stopMonitoring(context) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.Stop, null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.action_stop_monitoring))
                }
            } else {
                Button(onClick = { Actions.startMonitoring(context) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.PlayArrow, null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.action_start_monitoring))
                }
            }
        }
    }
}

@Composable
private fun LoginPromptCard(sessionLost: Boolean) {
    val context = LocalContext.current
    SectionCard {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (sessionLost) {
                    IconBadge(Icons.Rounded.Key, MaterialTheme.colorScheme.onErrorContainer, MaterialTheme.colorScheme.errorContainer)
                } else {
                    IconBadge(Icons.Rounded.Key, MaterialTheme.colorScheme.onPrimaryContainer, MaterialTheme.colorScheme.primaryContainer)
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        stringResource(if (sessionLost) R.string.session_lost_title else R.string.login_needed_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (sessionLost) MaterialTheme.colorScheme.error else Color.Unspecified,
                    )
                    Text(
                        stringResource(if (sessionLost) R.string.session_lost_body else R.string.login_needed_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (sessionLost) {
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.session_lost_tip), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(12.dp))
            Button(onClick = { LoginActivity.start(context) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(if (sessionLost) R.string.action_relogin else R.string.action_login))
            }
        }
    }
}

/** Ücretsiz sürümde birden fazla cihaz varken: yalnızca biri izleniyor. */
@Composable
private fun ProHintCard(onUpgrade: () -> Unit) {
    SectionCard {
        Row(Modifier.padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.WorkspacePremium, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Text(
                stringResource(R.string.pro_hint, Entitlement.FREE_DEVICES),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onUpgrade) { Text(stringResource(R.string.pro_upgrade)) }
        }
    }
}

@Composable
private fun EmptyDevices(showTuyaHint: Boolean) {
    Column(
        Modifier.fillMaxWidth().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Rounded.CloudOff, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.no_devices), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (showTuyaHint) {
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.tuya_shared_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DeviceCard(
    device: DeviceSnapshot,
    monitored: Boolean,
    compact: Boolean,
    priority: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    editBar: (@Composable () -> Unit)?,
) {
    val anyOn = device.switches.values.any { it }
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(if (compact) 18.dp else 24.dp),
    ) {
        if (compact) {
            CompactContent(device, anyOn, priority)
        } else {
            FullContent(device, anyOn, monitored, priority)
        }
        editBar?.invoke()
    }
}

/** Küçük kart: tek satırda durum, ad, güç ve (tek kanallıysa) anahtar. */
@Composable
private fun CompactContent(device: DeviceSnapshot, anyOn: Boolean, priority: Boolean) {
    Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        IconBadge(
            Icons.Rounded.PowerSettingsNew,
            tint = if (anyOn && device.online) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            background = if (anyOn && device.online) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
            size = 34,
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(device.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                if (priority) PriorityStar()
            }
            if (device.isMultiChannel && device.online) {
                Text(
                    stringResource(R.string.channels_on_short, device.switches.values.count { it }, device.switches.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (anyOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                OnlineDot(device.online)
            }
        }
        if (device.hasEnergy && device.online) {
            Text(
                formatWatts(device.power ?: 0.0),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }
        if (!device.isMultiChannel && device.switches.size == 1) {
            val (channel, on) = device.switches.entries.first()
            ChannelSwitch(device, rememberSwitchControl(device, channel, on))
        }
    }
}

@Composable
private fun PriorityStar() = Icon(
    Icons.Rounded.Star,
    contentDescription = stringResource(R.string.section_priority),
    tint = MaterialTheme.colorScheme.primary,
    modifier = Modifier.padding(start = 4.dp).size(16.dp),
)

@Composable
private fun FullContent(device: DeviceSnapshot, anyOn: Boolean, monitored: Boolean, priority: Boolean) {
        Row(Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBadge(
                Icons.Rounded.PowerSettingsNew,
                tint = if (anyOn && device.online) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                background = if (anyOn && device.online) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(device.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    if (priority) PriorityStar()
                }
                OnlineDot(device.online)
            }
            // Yalnızca enerji ölçen cihazlarda anlık güç.
            if (device.hasEnergy && device.online) {
                Text(
                    formatWatts(device.power ?: 0.0),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 12.dp),
                )
            }
            Icon(
                if (monitored) Icons.Rounded.NotificationsActive else Icons.Rounded.NotificationsOff,
                contentDescription = stringResource(if (monitored) R.string.monitored else R.string.not_monitored),
                tint = if (monitored) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            )
        }
        Column(Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
            if (device.isMultiChannel) {
                // Çok kanallı cihazlarda kanallar varsayılan olarak katlı; özet satırı açıp kapatır.
                var expanded by rememberSaveable(device.id) { mutableStateOf(false) }
                val arrowAngle by animateFloatAsState(if (expanded) 180f else 0f, label = "arrow")
                val onCount = device.switches.values.count { it }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { expanded = !expanded }
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.channels_summary, device.switches.size, onCount),
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (onCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        Icons.Rounded.ExpandMore,
                        contentDescription = stringResource(if (expanded) R.string.action_collapse else R.string.action_expand),
                        modifier = Modifier.rotate(arrowAngle),
                    )
                }
                AnimatedVisibility(expanded, enter = expandVertically(), exit = shrinkVertically()) {
                    Column {
                        device.switches.toSortedMap().forEach { (channel, on) ->
                            ChannelRow(device, channel, on)
                        }
                    }
                }
            } else {
                device.switches.toSortedMap().forEach { (channel, on) ->
                    ChannelRow(device, channel, on)
                }
            }
        }
}

/** Bir kanal satırı: etiket, durum ve aç/kapa anahtarı. İstek sürerken ilerleme gösterir. */
@Composable
fun ChannelRow(device: DeviceSnapshot, channel: Int, on: Boolean) {
    val control = rememberSwitchControl(device, channel, on)
    val shown = control.shown
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            if (device.isMultiChannel) stringResource(R.string.channel_n, channel + 1) else stringResource(R.string.power),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Text(
            stringResource(if (shown) R.string.label_on else R.string.label_off),
            style = MaterialTheme.typography.labelLarge,
            color = if (shown) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(12.dp))
        ChannelSwitch(device, control)
    }
}

/** Bir kanalın anahtar durumu: istek sürerken hedef değer gösterilir ve anahtar kilitlenir. */
class SwitchControl(val shown: Boolean, val busy: Boolean, val toggle: (Boolean) -> Unit)

@Composable
fun rememberSwitchControl(device: DeviceSnapshot, channel: Int, on: Boolean): SwitchControl {
    val resources = LocalResources.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pending by remember(device.id, channel) { mutableStateOf<Boolean?>(null) }
    // Gerçek durum beklenen değere ulaşınca bekleme durumunu temizle.
    LaunchedEffect(on) { if (pending == on) pending = null }
    return SwitchControl(pending ?: on, pending != null) { target ->
        pending = target
        scope.launch {
            Actions.setSwitch(context, device, channel, target).onFailure {
                pending = null
                Toast.makeText(context, resources.getString(R.string.control_failed, it.message ?: ""), Toast.LENGTH_LONG).show()
            }
            // Onay gelmezse 15 sn sonra bekleme durumunu bırak.
            delay(15_000)
            pending = null
        }
    }
}

@Composable
fun ChannelSwitch(device: DeviceSnapshot, control: SwitchControl) {
    Box(contentAlignment = Alignment.Center) {
        Switch(checked = control.shown, enabled = device.online && !control.busy, onCheckedChange = control.toggle)
        if (control.busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
    }
}
