package com.macerce.switchguard.ui

import android.text.format.DateUtils
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.AlarmOff
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.GppMaybe
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RemoveModerator
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.currentStateAsState
import com.macerce.switchguard.R
import com.macerce.switchguard.core.Demo
import com.macerce.switchguard.core.DeviceKinds
import com.macerce.switchguard.core.DeviceLayout
import com.macerce.switchguard.core.DeviceSection
import com.macerce.switchguard.core.DeviceSnapshot
import com.macerce.switchguard.core.Entitlement
import com.macerce.switchguard.core.Readiness
import com.macerce.switchguard.data.ConnState
import com.macerce.switchguard.data.LiveState
import com.macerce.switchguard.data.Store
import com.macerce.switchguard.ui.theme.Brand
import com.macerce.switchguard.ui.theme.LocalDarkTheme
import com.macerce.switchguard.ui.theme.StatusColors
import com.macerce.switchguard.ui.theme.raisedOnTile
import com.macerce.switchguard.ui.theme.tileColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun DevicesScreen(
    modifier: Modifier,
    onOpenDevice: (String) -> Unit,
    restoreFocusTo: String? = null,
    onFocusRestored: () -> Unit = {},
) {
    val tv = LocalIsTv.current
    val context = LocalContext.current
    val store = remember { Store.get(context) }
    val scope = rememberCoroutineScope()
    val version by store.version.collectAsStateWithLifecycle()
    val connection by LiveState.connection.collectAsStateWithLifecycle()
    // İzin ve ses ayarları uygulamaya dönünce yeniden okunsun.
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val resumed = lifecycle.isAtLeast(Lifecycle.State.RESUMED)

    val devices = remember(version) { store.snapshots }
    val alarm = remember(version) { store.pendingAlarm }
    val monitoring = remember(version) { store.monitoringEnabled }
    val readiness = remember(version, resumed) {
        Readiness.issues(readinessInput(context, store)).filter { monitoring || it in Readiness.SHOWN_WHEN_STOPPED }
    }
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
        AppTopBar(
            title = stringResource(if (editMode) R.string.action_edit_layout else R.string.app_name),
            actions = {
                if (editMode) {
                    val doneFocus = remember { FocusRequester() }
                    LaunchedEffect(Unit) { if (tv) runCatching { doneFocus.requestFocus() } }
                    TextButton(onClick = { editMode = false }, modifier = Modifier.focusRequester(doneFocus).tvFocusRing()) {
                        Text(stringResource(R.string.action_done))
                    }
                } else {
                    if (devices.isNotEmpty()) {
                        IconButton(modifier = Modifier.tvFocusRing(), onClick = { editMode = true }) {
                            Icon(Icons.Rounded.Tune, stringResource(R.string.action_edit_layout))
                        }
                    }
                    IconButton(modifier = Modifier.tvFocusRing(), onClick = doRefresh, enabled = loggedIn) {
                        Icon(Icons.Rounded.Refresh, stringResource(R.string.action_refresh))
                    }
                }
            },
        )
        // Detaydan dönünce kumandanın odağı açılan kartın başlığına verilir. Kart geçiş animasyonu bitmeden
        // yerleşmemiş olabilir: kısa bir süre denenir; kart yoksa (cihaz kaldırıldı) vazgeçilir.
        val restoreFocus = remember { FocusRequester() }
        LaunchedEffect(restoreFocusTo) {
            if (restoreFocusTo == null) return@LaunchedEffect
            if (tv) {
                for (attempt in 1..20) {
                    // Bağlanmamış istekçi hata fırlatır; yeni Compose sürümleri başarıyı Boolean olarak döndürür.
                    val result = runCatching { restoreFocus.requestFocus() }.getOrNull()
                    if (result != null && result != false) break
                    delay(50)
                }
            }
            onFocusRestored()
        }
        val wide = isWideScreen()
        val list: @Composable () -> Unit = {
            // Akıllı ev uygulamaları gibi kutucuk ızgarası: telefonda iki sütun, geniş ekranda ekrana sığdığı kadar.
            // Büyük kartlar ve bilgi kartları tam satır.
            val full: (LazyGridItemSpanScope.() -> GridItemSpan) = { GridItemSpan(maxLineSpan) }
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = if (wide) 220.dp else 150.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                item(span = full) {
                    androidx.compose.animation.AnimatedVisibility(alarm.isNotEmpty(), enter = expandVertically(), exit = shrinkVertically()) {
                        AlarmBanner(alarm) { Actions.dismissAlarm(context) }
                    }
                }
                if (!editMode) {
                    item(span = full) {
                        if (loggedIn) {
                            GuardCard(connection.state, connection.detail, connection.lastSync, monitoring, devices)
                        } else {
                            LoginPromptCard(sessionLost = remember(version) { store.sessionLost })
                        }
                    }
                }
                // Girişten önce yalnızca ikiz uyarısı: aynı eWeLink hesabıyla ikinci kopyada giriş, diğerinin oturumunu kapatır.
                val shownReadiness = if (loggedIn) readiness else readiness.filter { it in Readiness.SHOWN_WHEN_STOPPED }
                if (shownReadiness.isNotEmpty() && !editMode) {
                    item(span = full) { ReadinessCard(shownReadiness) }
                }
                if (loggedIn && devices.isEmpty()) {
                    item(span = full) { EmptyDevices(showTuyaHint = store.hasTuya) }
                }
                if (loggedIn && !isPro && !editMode && devices.count { !Demo.isDemo(it.id) } > Entitlement.FREE_DEVICES) {
                    item(span = full) { ProHintCard { showPro = true } }
                }
                if (editMode) item(key = "edit-hint", span = full) { LayoutEditHint { showCategories = true } }
                for (section in sections) {
                    // Düzenleme dışında boş kategori başlığı gösterilmez.
                    if (section.devices.isEmpty() && !editMode) continue
                    val headed = section.kind != DeviceSection.Kind.ALL
                    val collapsed = headed && section.key in layout.collapsed && !editMode
                    if (headed) {
                        item(key = "h-" + section.key, span = full) {
                            LayoutSectionHeader(sectionTitle(section), section.devices.size, collapsed) {
                                saveLayout(layout.toggleCollapsed(section.key))
                            }
                        }
                    }
                    if (section.devices.isEmpty()) {
                        item(key = "e-" + section.key, span = full) {
                            Text(
                                stringResource(R.string.category_no_devices),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 4.dp),
                            )
                        }
                    }
                    if (!collapsed) {
                        itemsIndexed(
                            section.devices,
                            key = { _, d -> d.id },
                            // Düzenlemede her kart tam satır: düzen araçları sığsın.
                            span = { _, d -> if (editMode || layout.isLarge(d)) GridItemSpan(maxLineSpan) else GridItemSpan(1) },
                        ) { i, device ->
                            DeviceTile(
                                device,
                                large = editMode || layout.isLarge(device),
                                monitored = device.id in monitoredIds,
                                priority = device.id in layout.priority,
                                onClick = { if (!editMode) onOpenDevice(device.id) },
                                onLongClick = { editMode = true },
                                focusRequester = if (device.id == restoreFocusTo) restoreFocus else null,
                                editBar = if (editMode) {
                                    { LayoutEditBar(device, layout, devices, canUp = i > 0, canDown = i < section.devices.lastIndex, onChange = saveLayout) }
                                } else null,
                            )
                        }
                    }
                }
            }
        }
        // TV'de çekip yenileme yok (dokunmatik hareket); göstergesi de çizilmesin.
        if (tv) Box(Modifier.fillMaxSize()) { list() }
        else PullToRefreshBox(isRefreshing = refreshing, onRefresh = doRefresh, modifier = Modifier.fillMaxSize()) { list() }
    }
}

@Composable
private fun AlarmBanner(lines: List<String>, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
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
                    pluralStringResource(R.plurals.alarm_banner_title, lines.size, lines.size),
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
                modifier = Modifier.fillMaxWidth().height(56.dp).tvFocusRing(),
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

/**
 * Koruma kartı: izlemenin durumu, son güncelleme ve cihaz sayaçları. Canlıyken marka renginde, sorun varken
 * turuncu-kırmızı, izleme kapalıyken sade kart.
 */
@Composable
private fun GuardCard(state: ConnState, detail: String, lastSync: Long, monitoring: Boolean, devices: List<DeviceSnapshot>) {
    val context = LocalContext.current
    val dark = LocalDarkTheme.current
    // "2 dk önce" metni kendiliğinden güncellensin.
    val now by produceState(System.currentTimeMillis()) {
        while (true) { delay(30_000); value = System.currentTimeMillis() }
    }
    val shown = if (monitoring) state else ConnState.STOPPED
    val problem = shown in setOf(ConnState.NO_INTERNET, ConnState.AUTH_ERROR, ConnState.ERROR)
    val branded = shown != ConnState.STOPPED
    val background: Brush = when {
        problem -> Brand.problem(dark)
        branded -> Brand.guard(dark)
        else -> SolidColor(tileColor())
    }
    val content = if (branded) Brand.onActive else MaterialTheme.colorScheme.onSurface
    val muted = if (branded) Brand.onActive.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(background).padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(52.dp).background(if (branded) Color.White.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    when {
                        problem -> Icons.Rounded.GppMaybe
                        branded -> Icons.Rounded.Shield
                        else -> Icons.Rounded.RemoveModerator
                    },
                    contentDescription = null,
                    tint = if (branded) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(30.dp),
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(connTitle(shown)), style = MaterialTheme.typography.titleLarge, color = content)
                val sub = when {
                    !monitoring -> stringResource(R.string.monitoring_off_hint)
                    detail.isNotEmpty() -> detail
                    lastSync > 0 -> stringResource(
                        R.string.last_update,
                        // "now" 30 sn'de bir tazelenir; arada gelen senkron onu geçerse "… sonra" yazmasın.
                        DateUtils.getRelativeTimeSpanString(lastSync, maxOf(now, lastSync), DateUtils.SECOND_IN_MILLIS).toString(),
                    )
                    devices.isEmpty() -> pluralStringResource(R.plurals.device_count, devices.size, devices.size)
                    else -> stringResource(R.string.guard_ready)
                }
                Text(sub, style = MaterialTheme.typography.bodyMedium, color = muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        if (devices.isNotEmpty()) {
            Spacer(Modifier.height(18.dp))
            val on = devices.count { d -> d.online && d.switches.values.any { it } }
            val offline = devices.count { !it.online }
            val power = devices.filter { it.online && it.hasEnergy }.sumOf { it.power ?: 0.0 }
            Row(Modifier.fillMaxWidth()) {
                Stat(devices.size.toString(), stringResource(R.string.stat_devices), content, muted, Modifier.weight(1f))
                Stat(on.toString(), stringResource(R.string.stat_on), content, muted, Modifier.weight(1f))
                Stat(
                    offline.toString(), stringResource(R.string.stat_offline),
                    if (offline > 0 && !branded) StatusColors.offline else content, muted, Modifier.weight(1f),
                )
                if (devices.any { it.hasEnergy }) Stat(formatWatts(power), stringResource(R.string.stat_power), content, muted, Modifier.weight(1.3f))
            }
        }
        Spacer(Modifier.height(16.dp))
        if (monitoring) {
            OutlinedButton(
                onClick = { Actions.stopMonitoring(context) },
                modifier = Modifier.tvFocusRing(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = content),
                border = BorderStroke(1.dp, if (branded) Color.White.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline),
            ) {
                Icon(Icons.Rounded.Stop, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.action_stop_monitoring))
            }
        } else {
            Button(onClick = { Actions.startMonitoring(context) }, modifier = Modifier.fillMaxWidth().height(52.dp).tvFocusRing()) {
                Icon(Icons.Rounded.PlayArrow, null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.action_start_monitoring), style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

@Composable
private fun Stat(value: String, label: String, color: Color, muted: Color, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = color, maxLines = 1)
        Text(label, style = MaterialTheme.typography.labelMedium, color = muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun LoginPromptCard(sessionLost: Boolean) {
    val context = LocalContext.current
    SectionCard(horizontalPadding = 0) {
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
            Button(onClick = { LoginActivity.start(context) }, modifier = Modifier.fillMaxWidth().tvFocusRing()) {
                Text(stringResource(if (sessionLost) R.string.action_relogin else R.string.action_login))
            }
        }
    }
}

/** Ücretsiz sürümde birden fazla cihaz varken: yalnızca biri izleniyor. */
@Composable
private fun ProHintCard(onUpgrade: () -> Unit) {
    SectionCard(horizontalPadding = 0) {
        Row(Modifier.padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.WorkspacePremium, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Text(
                stringResource(R.string.pro_hint, Entitlement.FREE_DEVICES),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton(modifier = Modifier.tvFocusRing(), onClick = onUpgrade) { Text(stringResource(R.string.pro_upgrade)) }
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

/**
 * Cihaz kutucuğu. Küçüğü yarım satırdır: simge, ad, durum ve tek kanallıysa güç düğmesi. Büyüğü tam satırdır;
 * çok kanallı cihazda her kanal için ayrı düğme taşır. Açık (ve çevrimiçi) tek kanallı cihazın kutucuğu marka
 * renginde parlar; çevrimdışı cihaz soluk görünür.
 *
 * Dokunmatikte kutucuğun tamamı detayı açar (uzun basış düzenleme). Kumandada kutucuk bir odak grubudur:
 * zemin detayı açar, düğmeler ayrı ayrı seçilir; içlerinden biri seçiliyken kutucuğun çerçevesi yanar.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DeviceTile(
    device: DeviceSnapshot,
    large: Boolean,
    monitored: Boolean,
    priority: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    editBar: (@Composable () -> Unit)?,
    focusRequester: FocusRequester? = null,
) {
    val tv = LocalIsTv.current
    val dark = LocalDarkTheme.current
    val anyOn = device.switches.values.any { it }
    // Çok kanallıda kanal düğmeleri kendi durumunu gösterir; kutucuğun kendisi renklenmez.
    val active = anyOn && device.online && !device.isMultiChannel && editBar == null
    val shape = RoundedCornerShape(24.dp)
    val background: Brush = if (active) Brand.active(dark) else SolidColor(tileColor())
    val controls = editBar == null

    Column(
        Modifier
            .fillMaxWidth()
            .then(if (tv && controls) Modifier.tvFocusRing(shape).focusGroup() else Modifier)
            .clip(shape)
            .background(background),
    ) {
        Box {
            // Zemin: dokununca detay. İçerik onun üstündedir; düğmeler kendi dokunuşlarını alır.
            if (controls) {
                Box(
                    Modifier
                        .matchParentSize()
                        .then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier)
                        .combinedClickable(onClick = onClick, onLongClick = onLongClick),
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
fun DeviceGlyph(device: DeviceSnapshot, active: Boolean, size: Int = 42) {
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

/** Durum satırı: "Açık · 118 W", "2/4 açık", "Kapalı" ya da kırmızı "Çevrimdışı". */
@Composable
private fun TileStatus(device: DeviceSnapshot, active: Boolean, monitored: Boolean) {
    val muted = if (active) Brand.onActive.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (!device.online) {
            Box(Modifier.size(8.dp).background(StatusColors.offline, CircleShape))
            Spacer(Modifier.width(6.dp))
        }
        val onCount = device.switches.values.count { it }
        val text = buildString {
            append(
                when {
                    !device.online -> stringResource(R.string.label_offline_title)
                    device.isMultiChannel -> stringResource(R.string.channels_on_short, onCount, device.switches.size)
                    onCount > 0 -> stringResource(R.string.state_on_title)
                    else -> stringResource(R.string.state_off_title)
                }
            )
            if (device.online && device.hasEnergy) append(" · ").append(formatWatts(device.power ?: 0.0))
        }
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
            contentDescription = stringResource(if (monitored) R.string.monitored else R.string.not_monitored),
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
                contentDescription = stringResource(R.string.section_priority),
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
            DeviceGlyph(device, active, size = 46)
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
fun ChannelButtons(device: DeviceSnapshot, modifier: Modifier = Modifier, height: Int = 64) {
    val channels = device.switches.toSortedMap().entries.toList()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        channels.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (channel, on) ->
                    ChannelButton(device, channel, on, Modifier.weight(1f).height(height.dp))
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
    val label = stringResource(R.string.channel_n, channel + 1)
    val state = stringResource(if (shown) R.string.state_on_title else R.string.state_off_title)
    Box(
        modifier
            .tvFocusRing(shape)
            .clip(shape)
            .background(bg)
            .then(if (!shown && enabled) Modifier.border(1.dp, scheme.outlineVariant, shape) else Modifier)
            .clickable(enabled = enabled, role = Role.Switch) { if (!control.busy) control.toggle(!shown) }
            .semantics(mergeDescendants = true) { contentDescription = "$label, $state" },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.PowerSettingsNew, null, tint = fg, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("${channel + 1}", style = MaterialTheme.typography.titleMedium, color = fg)
            }
            val watts = device.channelPower[channel]
            if (watts != null && enabled) {
                Text(formatWatts(watts), style = MaterialTheme.typography.labelSmall, color = fg.copy(alpha = 0.85f), maxLines = 1)
            }
        }
        if (control.busy) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = fg)
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
