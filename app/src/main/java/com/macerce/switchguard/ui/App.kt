package com.macerce.switchguard.ui

import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoMode
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.runtime.key
import androidx.compose.foundation.layout.width
import androidx.compose.material3.VerticalDivider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.macerce.switchguard.R
import com.macerce.switchguard.data.EventLog
import com.macerce.switchguard.data.Store
import com.macerce.switchguard.ui.theme.tileColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Sekmeler (alt menü / yan menü) bu sırayla. Uyarılar uygulamanın asıl işi: Cihazlar'ın hemen yanında. */
private enum class Tab(val label: Int, val icon: ImageVector) {
    DEVICES(R.string.tab_devices, Icons.Rounded.Devices),
    ALERTS(R.string.tab_alerts, Icons.Rounded.NotificationsActive),
    AUTOMATIONS(R.string.tab_automations, Icons.Rounded.AutoMode),
    HISTORY(R.string.tab_history, Icons.Rounded.History),
    SETTINGS(R.string.tab_settings, Icons.Rounded.Settings),
}

@Composable
fun App() {
    val context = LocalContext.current
    val store = remember { Store.get(context) }
    val version by store.version.collectAsStateWithLifecycle()

    // version okunarak her ayar değişiminde yeniden değerlendirilir.
    val onboarded = remember(version) { store.onboardingDone }
    if (!onboarded) {
        OnboardingScreen(onFinished = { store.onboardingDone = true })
        return
    }

    var tab by rememberSaveable { mutableStateOf(Tab.DEVICES) }
    var tvHome by rememberSaveable { mutableStateOf(true) }
    // Seçili sekmeye yeniden dokunmak o sekmenin başına döner (Ayarlar: alt sayfadan listeye).
    var settingsHome by rememberSaveable { mutableIntStateOf(0) }
    val select: (Tab) -> Unit = { t ->
        if (t == tab && t == Tab.SETTINGS) settingsHome++
        tab = t
        if (t == Tab.DEVICES) tvHome = true
    }
    // Uyarılar'daki durum kartından Ayarlar › İzinler'e geçiş; Ayarlar o sayfayla açılınca söner.
    var openPermissions by rememberSaveable { mutableStateOf(false) }
    var openDeviceId by rememberSaveable { mutableStateOf<String?>(null) }
    var chatOpen by rememberSaveable { mutableStateOf(false) }
    // TV: klasik Cihazlar ızgarası yok; cihazların yeri sade pano (TvHome). Diğer sekmeler alt çubuktan açılır,
    // Geri ya da yan menüdeki Cihazlar panoya döner.
    val tv = LocalIsTv.current
    val showTvHome = tv && (tvHome || tab == Tab.DEVICES)
    // Detaydan dönünce kumandanın odağı açılan karta geri verilir (yoksa yan menüye düşer).
    var returnFocusTo by remember { mutableStateOf<String?>(null) }
    val closeDevice = {
        returnFocusTo = openDeviceId
        openDeviceId = null
    }
    // Liste detay açıkken ekrandan kalkar; kaydırma konumu ve açık kanal grupları dönüşte korunur.
    val listState = rememberSaveableStateHolder()

    // Geri: önce cihaz detayını kapat, sonra diğer sekmelerden Cihazlar'a dön; yalnızca Cihazlar'dan çık.
    // Sıra önemli: sonra kaydedilen BackHandler önce çalışır.
    BackHandler(enabled = tab != Tab.DEVICES) { tab = Tab.DEVICES }
    BackHandler(enabled = tv && !tvHome) { tvHome = true; tab = Tab.DEVICES }
    // Büyük tablet (yatay): Cihazlar'da liste solda, seçilen cihazın sayfası sağda (iki bölme); TV'de yok.
    val twoPane = isWideScreen() && !tv && LocalConfiguration.current.screenWidthDp >= TWO_PANE_DP
    BackHandler(enabled = openDeviceId != null && (!twoPane || tab == Tab.DEVICES)) { closeDevice() }
    BackHandler(enabled = chatOpen) { chatOpen = false }

    // Geçmiş rozeti: son bakıştan beri alarm/bildirim üretmiş olay sayısı. Geçmiş açıkken yeni olaylar da görülmüş sayılır.
    val log = remember { EventLog.get(context) }
    val logVersion by log.version.collectAsStateWithLifecycle()
    LaunchedEffect(tab, logVersion) {
        if (tab == Tab.HISTORY) withContext(Dispatchers.IO) { store.historySeenId = log.latestId() }
    }
    var unseen by remember { mutableIntStateOf(0) }
    LaunchedEffect(logVersion, version) {
        unseen = withContext(Dispatchers.IO) {
            // İlk kez: eski kayıtlar rozete dolmasın, sayım bundan sonrası için başlasın.
            val seen = store.historySeenId ?: log.latestId().also { store.historySeenId = it }
            log.unseenAlertCount(seen)
        }
    }

    // Bölge kuralı/bölge eklenince, değişince ya da yedekten dönünce sistemdeki bölge kayıtları eşitlenir.
    LaunchedEffect(version) { com.macerce.switchguard.service.ZoneWatcher.syncIfChanged(context) }

    val wide = isWideScreen()
    // Büyük tablet (yatay): Cihazlar'da liste solda, seçilen cihazın sayfası sağda (iki bölme); TV'de yok.
    val tabIcon: @Composable (Tab) -> Unit = { t ->
        if (t == Tab.HISTORY && unseen > 0) {
            BadgedBox(badge = { Badge { Text(if (unseen > 99) "99+" else unseen.toString()) } }) {
                Icon(t.icon, contentDescription = null)
            }
        } else {
            Icon(t.icon, contentDescription = null)
        }
    }
    // Taşınabilir içerik: telefon yan çevrilince (ya da tablette pencere boyutu değişince) alt menü ile yan menü
    // düzenleri arasında geçilir; içerik taşınırken durumunu (açık pencere, yazılan metin, kaydırma) korur.
    val tabContent = remember {
        movableContentOf { mod: Modifier ->
            when (tab) {
                Tab.DEVICES -> DevicesScreen(
                    mod,
                    onOpenDevice = { openDeviceId = it },
                    onOpenChat = { chatOpen = true },
                    restoreFocusTo = returnFocusTo,
                    onFocusRestored = { returnFocusTo = null },
                )
                Tab.ALERTS -> AlertsScreen(mod, onOpenPermissions = { openPermissions = true; tab = Tab.SETTINGS })
                Tab.AUTOMATIONS -> AutomationsScreen(mod)
                Tab.HISTORY -> HistoryScreen(mod)
                Tab.SETTINGS -> SettingsScreen(
                    mod,
                    homeRequests = settingsHome,
                    openPermissions = openPermissions,
                    onPermissionsOpened = { openPermissions = false },
                )
            }
        }
    }

    CrashPromptHost()

    if (chatOpen) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Box(Modifier.readableWidth()) { ChatScreen(onBack = { chatOpen = false }) }
        }
        return
    }

    AnimatedContent(
        targetState = openDeviceId.takeUnless { twoPane },
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "detail",
    ) { deviceId ->
        if (deviceId != null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                Box(Modifier.readableWidth()) { DeviceDetailScreen(deviceId = deviceId, onBack = closeDevice) }
            }
        } else if (showTvHome) {
            TvHome(
                onOpenDevice = { openDeviceId = it },
                onOpen = { dest ->
                    tab = when (dest) {
                        TvDest.ALERTS -> Tab.ALERTS
                        TvDest.AUTOMATIONS -> Tab.AUTOMATIONS
                        TvDest.HISTORY -> Tab.HISTORY
                        TvDest.SETTINGS -> Tab.SETTINGS
                    }
                    tvHome = false
                },
                onOpenChat = { chatOpen = true },
                unseenHistory = unseen,
                restoreFocusTo = returnFocusTo,
                onFocusRestored = { returnFocusTo = null },
            )
        } else listState.SaveableStateProvider("tabs") {
            if (wide) {
                // Tablet, Chromebook ve TV: alt menü yerine yan menü, içerik okunabilir genişlikte ortada.
                Row(Modifier.fillMaxSize()) {
                    NavigationRail(Modifier.fillMaxHeight(), containerColor = MaterialTheme.colorScheme.background) {
                        Spacer(Modifier.weight(1f))
                        Tab.entries.forEach { t ->
                            NavigationRailItem(
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                                ),
                                modifier = Modifier.tvFocusRing(RoundedCornerShape(16.dp), inside = true),
                                selected = tab == t,
                                onClick = { select(t) },
                                icon = { tabIcon(t) },
                                label = { Text(stringResource(t.label)) },
                            )
                        }
                        Spacer(Modifier.weight(1f))
                    }
                    Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
                        tabContent(Modifier.readableWidth())
                    }
                    val paneId = openDeviceId
                    if (twoPane && paneId != null && tab == Tab.DEVICES) {
                        VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Box(Modifier.width(DETAIL_PANE_DP.dp).fillMaxHeight()) {
                            key(paneId) { DeviceDetailScreen(deviceId = paneId, onBack = closeDevice) }
                        }
                    }
                }
            } else {
                Scaffold(
                    bottomBar = {
                        val scheme = MaterialTheme.colorScheme
                        val navColors = NavigationBarItemDefaults.colors(
                            selectedIconColor = scheme.primary,
                            selectedTextColor = scheme.onSurface,
                            indicatorColor = scheme.primary.copy(alpha = 0.22f),
                            unselectedIconColor = scheme.onSurfaceVariant,
                            unselectedTextColor = scheme.onSurfaceVariant,
                        )
                        NavigationBar(containerColor = tileColor()) {
                            Tab.entries.forEach { t ->
                                NavigationBarItem(
                                    selected = tab == t,
                                    onClick = { select(t) },
                                    icon = { tabIcon(t) },
                                    label = { TabLabel(stringResource(t.label)) },
                                    // Videodaki alt menü: seçili sekme turkuaz simge + koyu turkuaz hap.
                                    colors = navColors,
                                )
                            }
                        }
                    },
                ) { padding ->
                    tabContent(Modifier.padding(bottom = padding.calculateBottomPadding()))
                }
            }
        }
    }
}

/** Alt menü etiketi tek satır: uzun çevirilerde (ör. Fransızca "Automatisations") ikiye bölünmek yerine yazı küçülür. */
@Composable
private fun TabLabel(text: String) {
    val size = MaterialTheme.typography.labelMedium.fontSize
    Text(
        text,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
        autoSize = TextAutoSize.StepBased(minFontSize = 9.sp, maxFontSize = size, stepSize = 0.5.sp),
    )
}

/** İki bölmeli Cihazlar düzeni için en az ekran genişliği ve sağdaki cihaz bölmesinin genişliği. */
private const val TWO_PANE_DP = 1000
private const val DETAIL_PANE_DP = 460
