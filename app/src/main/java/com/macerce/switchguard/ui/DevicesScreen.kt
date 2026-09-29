package com.macerce.switchguard.ui

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.macerce.switchguard.R
import com.macerce.switchguard.core.DeviceSnapshot
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
    val loggedIn = remember(version) { store.isLoggedIn }
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
                IconButton(onClick = doRefresh, enabled = loggedIn) {
                    Icon(Icons.Rounded.Refresh, stringResource(R.string.action_refresh))
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
                        LoginPromptCard()
                    }
                }
                if (loggedIn && devices.isEmpty()) {
                    item { EmptyDevices() }
                }
                items(devices, key = { it.id }) { device ->
                    DeviceCard(device, monitored = store.rulesFor(device.id).monitored, onClick = { onOpenDevice(device.id) })
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
                        DateUtils.getRelativeTimeSpanString(lastSync, now, DateUtils.SECOND_IN_MILLIS).toString(),
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
private fun LoginPromptCard() {
    val context = LocalContext.current
    SectionCard {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(Icons.Rounded.Key, MaterialTheme.colorScheme.onPrimaryContainer, MaterialTheme.colorScheme.primaryContainer)
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(stringResource(R.string.login_needed_title), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.login_needed_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(12.dp))
            Button(onClick = { LoginActivity.start(context) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.action_login))
            }
        }
    }
}

@Composable
private fun EmptyDevices() {
    Column(
        Modifier.fillMaxWidth().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Rounded.CloudOff, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.no_devices), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DeviceCard(device: DeviceSnapshot, monitored: Boolean, onClick: () -> Unit) {
    val anyOn = device.switches.values.any { it }
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
    ) {
        Row(Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBadge(
                Icons.Rounded.PowerSettingsNew,
                tint = if (anyOn && device.online) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                background = if (anyOn && device.online) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(device.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                OnlineDot(device.online)
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
}

/** Bir kanal satırı: etiket, durum ve aç/kapa anahtarı. İstek sürerken ilerleme gösterir. */
@Composable
fun ChannelRow(device: DeviceSnapshot, channel: Int, on: Boolean) {
    val resources = LocalResources.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pending = remember { mutableStateMapOf<Int, Boolean>() }
    val shown = pending[channel] ?: on

    // Gerçek durum beklenen değere ulaşınca bekleme durumunu temizle.
    LaunchedEffect(on) { if (pending[channel] == on) pending.remove(channel) }

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
        Box(contentAlignment = Alignment.Center) {
            Switch(
                checked = shown,
                enabled = device.online && pending[channel] == null,
                onCheckedChange = { target ->
                    pending[channel] = target
                    scope.launch {
                        Actions.setSwitch(context, device, channel, target).onFailure {
                            pending.remove(channel)
                            Toast.makeText(context, resources.getString(R.string.control_failed, it.message ?: ""), Toast.LENGTH_LONG).show()
                        }
                        // Onay gelmezse 15 sn sonra bekleme durumunu bırak.
                        delay(15_000)
                        pending.remove(channel)
                    }
                },
            )
            if (pending[channel] != null) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
        }
    }
}
