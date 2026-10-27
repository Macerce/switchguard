package com.macerce.switchguard.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.GppMaybe
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RemoveModerator
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.currentStateAsState
import com.macerce.switchguard.R
import com.macerce.switchguard.core.AutomationTab
import com.macerce.switchguard.core.DeviceAlertSummary
import com.macerce.switchguard.core.DeviceCategory
import com.macerce.switchguard.core.DeviceKinds
import com.macerce.switchguard.core.DeviceRules
import com.macerce.switchguard.core.DeviceSnapshot
import com.macerce.switchguard.core.EventType
import com.macerce.switchguard.core.QuietHours
import com.macerce.switchguard.core.Readiness
import com.macerce.switchguard.data.Store
import com.macerce.switchguard.ui.theme.Brand
import com.macerce.switchguard.ui.theme.LocalDarkTheme
import com.macerce.switchguard.ui.theme.tileColor
import kotlinx.coroutines.delay

/** Uyarılar sekmesinin alt sayfası ([id]: cihaz ya da koşullu uyarı kimliği); null = sekmenin kendisi. */
private data class AlertsRoute(val kind: Kind, val id: String = "") : java.io.Serializable {
    enum class Kind { DEVICE_RULES, EDITOR, ALARM, QUIET, SMART, CATEGORIES }
}

/**
 * Uyarılar sekmesi: uygulamanın asıl işi. Yukarıdan aşağı durum kartı (alarm hazır mı), cihaz uyarıları (cihaz başına
 * özet, dokununca uyarı kuralları), koşullu uyarılar (alarm/bildirim veren otomasyonlar) ve uyarı ayarları
 * (Alarm, Sessiz saatler, İkinci kişiye haber ver). Alt sayfalar bu sekmede açılır; geri tuşu listeye döner.
 * [onOpenPermissions]: durum kartındaki "Düzelt" Ayarlar › İzinler'i açar.
 */
@Composable
fun AlertsScreen(modifier: Modifier, onOpenPermissions: () -> Unit) {
    var route by rememberSaveable { mutableStateOf<AlertsRoute?>(null) }
    // Kumanda: alt sayfadan dönünce odak açan satıra verilir. Yalnızca dönüşte istenir: açılışta istenirse
    // geçiş sürerken çıkan listedeki satıra odaklanır, liste kalkınca odak kaybolur.
    var opened by rememberSaveable { mutableStateOf<String?>(null) }
    var restoreFocus by remember { mutableStateOf<String?>(null) }
    var dialog by remember { mutableStateOf<(@Composable () -> Unit)?>(null) }
    val showDialog: ((@Composable () -> Unit)?) -> Unit = { dialog = it }
    // Alt sayfadan dönünce liste kaldığı yerden sürsün.
    val scroll = rememberScrollState()
    val back = { restoreFocus = opened; route = null }
    BackHandler(enabled = route != null) { back() }

    AnimatedContent(
        targetState = route,
        transitionSpec = {
            // Ayarlar'daki gibi: ileri sağdan, geri soldan.
            val forward = targetState != null
            (slideInHorizontally { w -> if (forward) w / 4 else -w / 4 } + fadeIn()) togetherWith
                (slideOutHorizontally { w -> if (forward) -w / 4 else w / 4 } + fadeOut())
        },
        modifier = modifier.fillMaxSize(),
        label = "alertsPage",
    ) { target ->
        when (target?.kind) {
            null -> AlertsHome(
                scroll = scroll,
                focusKey = restoreFocus,
                onFocusRestored = { restoreFocus = null },
                onOpen = { r, key -> opened = key; route = r },
                showDialog = showDialog,
                onOpenPermissions = onOpenPermissions,
            )
            AlertsRoute.Kind.DEVICE_RULES -> DeviceAlertRulesPage(target.id, onBack = back)
            AlertsRoute.Kind.EDITOR -> {
                val context = LocalContext.current
                val store = remember { Store.get(context) }
                val version by store.version.collectAsStateWithLifecycle()
                // Açılıştaki kayıt: silince çıkış geçişinde başlık "Yeni uyarı"ya dönmesin.
                val initial = remember(target.id) { store.automations.firstOrNull { it.id == target.id } }
                AutomationEditor(
                    tab = AutomationTab.ALERTS,
                    initial = initial,
                    // Gruplar ve uyarı kategorileri de cihaz gibi seçilir.
                    devices = remember(version) { automationDevices(store.snapshotsWithGroups, context.resources) },
                    modifier = Modifier,
                    onSave = { store.saveAutomation(it); back() },
                    onDelete = { store.deleteAutomation(target.id); back() },
                    onBack = back,
                )
            }
            AlertsRoute.Kind.ALARM -> SettingsSubPage(SettingsPage.ALARM, onBack = back, showDialog = showDialog)
            AlertsRoute.Kind.QUIET -> SettingsSubPage(SettingsPage.QUIET, onBack = back, showDialog = showDialog)
            AlertsRoute.Kind.SMART -> SettingsSubPage(SettingsPage.SMART, onBack = back, showDialog = showDialog)
            AlertsRoute.Kind.CATEGORIES -> CategoriesPage(onBack = back)
        }
    }

    dialog?.invoke()
}

@Composable
private fun AlertsHome(
    scroll: ScrollState,
    focusKey: String?,
    onFocusRestored: () -> Unit,
    onOpen: (AlertsRoute, String) -> Unit,
    showDialog: ((@Composable () -> Unit)?) -> Unit,
    onOpenPermissions: () -> Unit,
) {
    val context = LocalContext.current
    val tv = LocalIsTv.current
    val store = remember { Store.get(context) }
    val version by store.version.collectAsStateWithLifecycle()
    // İzin ve ses ayarları uygulamaya dönünce yeniden okunsun.
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val resumed = lifecycle.isAtLeast(Lifecycle.State.RESUMED)
    val s = remember(version, resumed) { store }
    val monitoring = remember(version) { store.monitoringEnabled }
    val issues = remember(version, resumed) { Readiness.issues(readinessInput(context, store)) }
    // Cihazlar ekranındaki sırayla (öncelikliler, kategoriler).
    val devices = remember(version) { store.deviceLayout.sections(store.snapshots).flatMap { it.devices } }
    val monitoredIds = remember(version) { store.monitoredIds() }
    val rules = remember(version) { store.rules }
    val alerts = remember(version) { AutomationTab.ALERTS.of(store.automations) }
    val allDevices = remember(version) { automationDevices(store.snapshotsWithGroups, context.resources) }
    val categoryOf = remember(version) { { id: String -> (rules[id] ?: DeviceRules()).effectiveCategory } }
    val counts = remember(version) { categoryCounts(store.snapshots, categoryOf) }
    var filter by rememberSaveable { mutableStateOf<DeviceCategory?>(null) }
    val shown = devices.filter { filter == null || categoryOf(it.id) == filter }

    val requesters = remember { mutableMapOf<String, FocusRequester>() }
    fun focus(key: String) = Modifier.focusRequester(requesters.getOrPut(key) { FocusRequester() })
    LaunchedEffect(focusKey) {
        val key = focusKey ?: return@LaunchedEffect
        if (tv) {
            // Çıkan alt sayfa geçiş bitince kaldırılır; odağı ondan önce verirsek kaldırılırken sıfırlanabiliyor.
            delay(450)
            for (attempt in 1..20) {
                val ok = runCatching { requesters[key]?.requestFocus() }.getOrNull()
                if (ok != null && ok != false) break
                delay(50)
            }
        }
        onFocusRestored()
    }

    Column(Modifier.fillMaxSize()) {
        AppTopBar(stringResource(R.string.tab_alerts))
        Column(Modifier.verticalScroll(scroll).padding(bottom = 24.dp)) {
            AlertStatusCard(
                monitoring = monitoring,
                hasAccount = s.hasAnyAccount,
                issues = issues.size,
                onFix = onOpenPermissions,
                onTest = { showDialog { TestAlarmDialog(onDismiss = { showDialog(null) }) } },
            )
            PresenceCard(s)

            // ------------------------------------------------ Cihaz uyarıları
            SectionTitle(stringResource(R.string.section_device_alerts), Modifier.padding(top = 8.dp))
            SectionDescription(stringResource(R.string.section_device_alerts_desc))
            if (devices.isNotEmpty()) CategoryFilter(counts, filter) { filter = it }
            SectionCard {
                if (devices.isEmpty() || shown.isEmpty()) {
                    Text(
                        stringResource(if (devices.isEmpty()) R.string.no_devices else R.string.category_empty),
                        Modifier.padding(20.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                shown.forEachIndexed { i, d ->
                    if (i > 0) RowDivider(68.dp)
                    val summary = DeviceAlertSummary.of(rules[d.id] ?: DeviceRules(), d.id in monitoredIds)
                    val key = "dev:" + d.id
                    DeviceAlertRow(d, summary, modifier = focus(key)) { onOpen(AlertsRoute(AlertsRoute.Kind.DEVICE_RULES, d.id), key) }
                }
            }

            // ------------------------------------------------ Koşullu uyarılar
            SectionTitle(stringResource(R.string.section_conditional_alerts))
            SectionDescription(stringResource(R.string.conditional_alerts_desc))
            if (alerts.isEmpty()) {
                SectionCard {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        HueIcon(Icons.Rounded.Timer, Hue.SLATE)
                        Spacer(Modifier.width(16.dp))
                        Text(
                            stringResource(R.string.conditional_alerts_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    alerts.forEach { a ->
                        val key = "alert:" + a.id
                        AutomationCard(
                            a, allDevices,
                            onClick = { onOpen(AlertsRoute(AlertsRoute.Kind.EDITOR, a.id), key) },
                            onToggle = { store.saveAutomation(a.copy(enabled = it)) },
                            modifier = focus(key),
                        )
                    }
                }
            }
            // Listenin altında tam genişlik: kumandada yukarı/aşağıyla doğrudan ulaşılır (başlığın sağında
            // dururken tam genişlik satırlar arasında atlanıyordu).
            AddTile(stringResource(R.string.automation_add), focus("add").padding(start = 16.dp, end = 16.dp, top = 12.dp)) {
                onOpen(AlertsRoute(AlertsRoute.Kind.EDITOR, NEW_AUTOMATION), "add")
            }

            // ------------------------------------------------ Uyarı ayarları (Ayarlar'dan taşındı)
            SectionTitle(stringResource(R.string.section_alert_settings))
            SectionCard {
                MenuRow(
                    Icons.Rounded.NotificationsActive, Hue.RED, stringResource(R.string.section_alarm),
                    onClick = { onOpen(AlertsRoute(AlertsRoute.Kind.ALARM), "alarm") }, modifier = focus("alarm"),
                    summary = alarmSummary(s),
                )
                RowDivider(68.dp)
                MenuRow(
                    Icons.AutoMirrored.Rounded.Label, Hue.ORANGE, stringResource(R.string.categories_title),
                    onClick = { onOpen(AlertsRoute(AlertsRoute.Kind.CATEGORIES), "categories") }, modifier = focus("categories"),
                    summary = stringResource(
                        R.string.categories_summary,
                        counts[DeviceCategory.CRITICAL] ?: 0, counts[DeviceCategory.IMPORTANT] ?: 0, counts[DeviceCategory.NORMAL] ?: 0,
                    ),
                )
                RowDivider(68.dp)
                MenuRow(
                    Icons.Rounded.Bedtime, Hue.INDIGO, stringResource(R.string.section_quiet_hours),
                    onClick = { onOpen(AlertsRoute(AlertsRoute.Kind.QUIET), "quiet") }, modifier = focus("quiet"),
                    summary = quietSummary(s.quietHours),
                )
                RowDivider(68.dp)
                MenuRow(
                    Icons.AutoMirrored.Rounded.Send, Hue.BLUE, stringResource(R.string.tg_title),
                    onClick = { showDialog { TelegramDialog(store) { showDialog(null) } } },
                    summary = escalationSummary(s),
                )
                RowDivider(68.dp)
                MenuRow(
                    Icons.Rounded.AutoAwesome, Hue.AMBER, stringResource(R.string.smart_title),
                    onClick = { onOpen(AlertsRoute(AlertsRoute.Kind.SMART), "smart") }, modifier = focus("smart"),
                    summary = stringResource(R.string.smart_summary),
                )
            }
        }
    }
}

/** "Ekle" kutucuğu: listenin altında, marka renginde artı ve metin. */
@Composable
private fun AddTile(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Row(
        modifier.fillMaxWidth().height(56.dp)
            .tvFocusRing(shape)
            .clip(shape)
            .background(tileColor())
            .clickable(onClick = onClick, role = Role.Button),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Add, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
    }
}

/** Bölüm başlığının altındaki kısa açıklama. */
@Composable
private fun SectionDescription(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 10.dp),
    )
}

@Composable
private fun quietSummary(q: QuietHours): String {
    val context = LocalContext.current
    return if (!q.enabled) stringResource(R.string.state_off_title)
    else stringResource(R.string.time_range, formatMinute(context, q.startMinute), formatMinute(context, q.endMinute))
}

/**
 * Durum kartı: izleme kapalı (başlat), alarm ulaşmayabilir (telefon ayarı sorunları → Ayarlar › İzinler) ya da
 * alarm hazır; her durumda test alarmı. Cihazlar ekranındaki koruma ve "Alarm size ulaşmayabilir" kartlarını
 * özetler, onların yerini almaz.
 */
@Composable
private fun AlertStatusCard(monitoring: Boolean, hasAccount: Boolean, issues: Int, onFix: () -> Unit, onTest: () -> Unit) {
    val context = LocalContext.current
    val dark = LocalDarkTheme.current
    val problem = monitoring && issues > 0
    val background: Brush = when {
        !monitoring -> SolidColor(tileColor())
        problem -> Brand.problem(dark)
        else -> Brand.guard(dark)
    }
    val branded = monitoring
    val content = if (branded) Brand.onActive else MaterialTheme.colorScheme.onSurface
    val muted = if (branded) Brand.onActive.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
    val (title, body) = when {
        !monitoring -> stringResource(R.string.alerts_monitoring_off) to stringResource(R.string.alerts_monitoring_off_desc)
        problem -> stringResource(R.string.ready_title) to pluralStringResource(R.plurals.alerts_issues_n, issues, issues)
        else -> stringResource(R.string.alerts_ready) to stringResource(R.string.alerts_ready_desc)
    }

    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(28.dp)).background(background).padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(52.dp).background(
                    if (branded) Color.White.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape,
                ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    when {
                        !monitoring -> Icons.Rounded.RemoveModerator
                        problem -> Icons.Rounded.GppMaybe
                        else -> Icons.Rounded.Shield
                    },
                    contentDescription = null,
                    tint = if (branded) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(30.dp),
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge, color = content)
                Text(body, style = MaterialTheme.typography.bodyMedium, color = muted)
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Asıl işlem: izlemeyi başlat (hesap yoksa giriş) ya da sorunları düzelt; alarm hazırsa yalnızca test.
            val primary = when {
                !monitoring && hasAccount -> R.string.action_start_monitoring
                !monitoring -> R.string.action_login
                problem -> R.string.ready_fix
                else -> null
            }
            if (primary != null) {
                Button(
                    onClick = {
                        when {
                            problem -> onFix()
                            hasAccount -> Actions.startMonitoring(context)
                            else -> LoginActivity.start(context)
                        }
                    },
                    modifier = Modifier.weight(1f).height(48.dp).tvFocusRing(),
                    colors = if (branded) ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Brand.deep)
                    else ButtonDefaults.buttonColors(),
                ) {
                    if (!monitoring && hasAccount) {
                        Icon(Icons.Rounded.PlayArrow, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(stringResource(primary), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            OutlinedButton(
                onClick = onTest,
                modifier = (if (primary != null) Modifier.weight(1f) else Modifier).height(48.dp).tvFocusRing(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = content),
                border = BorderStroke(1.dp, if (branded) Color.White.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline),
            ) {
                Icon(Icons.Rounded.NotificationsActive, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.test_alarm), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** Cihaz uyarıları satırı: tür simgesi, ad (özel ses, kritik rozeti) ve alarm/bildirim verilen olaylar. */
@Composable
private fun DeviceAlertRow(device: DeviceSnapshot, summary: DeviceAlertSummary, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val resources = LocalResources.current
    val scheme = MaterialTheme.colorScheme
    val kind = remember(device.name, device.isMultiChannel, device.hasEnergy, device.cloud) { DeviceKinds.of(device) }
    val texts = DeviceAlertSummary.Texts(
        notMonitored = stringResource(R.string.device_alerts_not_monitored),
        none = stringResource(R.string.device_alerts_none),
        alarm = { resources.getString(R.string.device_alerts_alarm, it) },
        notify = { resources.getString(R.string.device_alerts_notify, it) },
        event = { resources.getString(it.shortTitle()) },
    )
    Row(
        modifier
            .tvFocusRing(RoundedCornerShape(16.dp), inside = true)
            .clickable(onClick = onClick, role = Role.Button)
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HueIcon(kind.icon(), if (summary.monitored) Hue.TEAL else Hue.SLATE)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    device.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (summary.monitored && summary.customSound) {
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Rounded.MusicNote, stringResource(R.string.device_sound), Modifier.size(16.dp), tint = scheme.onSurfaceVariant)
                }
                if (summary.category.hue() != null) {
                    Spacer(Modifier.width(8.dp))
                    CategoryBadge(summary.category)
                }
            }
            summary.lines(texts).forEach { line ->
                Text(line, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.width(8.dp))
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = scheme.onSurfaceVariant)
    }
}

/** Olayların kısa adı (cihaz uyarıları özetinde: "Kapanınca, Çevrimdışı olunca"). */
private fun EventType.shortTitle(): Int = when (this) {
    EventType.TURNED_OFF -> R.string.evt_turned_off
    EventType.TURNED_ON -> R.string.evt_turned_on
    EventType.WENT_OFFLINE -> R.string.evt_went_offline
    EventType.CAME_ONLINE -> R.string.evt_came_online
}

/** Uyarı kategorileri sayfası: kategori başına sessiz saat seçimi. */
@Composable
private fun CategoriesPage(onBack: () -> Unit) {
    val context = LocalContext.current
    val store = remember { Store.get(context) }
    val version by store.version.collectAsStateWithLifecycle()
    val bypass = remember(version) { store.quietBypass }
    val counts = remember(version) { categoryCounts(store.snapshots, store::categoryOf) }
    Column(Modifier.fillMaxSize()) {
        AppTopBar(stringResource(R.string.categories_title), navigationIcon = { BackButton(onBack) }, small = true)
        Column(Modifier.focusGroup().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
            CategoriesContent(counts, bypass) { store.quietBypass = it }
        }
    }
}

/** Bir cihazın uyarı kuralları sayfası (Uyarılar › Cihaz uyarıları'ndan): cihaz sayfasındaki bölümün aynısı. */
@Composable
private fun DeviceAlertRulesPage(deviceId: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val tv = LocalIsTv.current
    val store = remember { Store.get(context) }
    val version by store.version.collectAsStateWithLifecycle()
    val device = remember(version) { store.snapshots.firstOrNull { it.id == deviceId } }
    // Kumanda: sayfa açılınca odak ilk öğeye, izle anahtarına (gruba istenirse sağdaki anahtar atlanıp
    // soldaki ilk kural düğmesine gidiyor, sayfa da aşağı kayıyor).
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        if (!tv) return@LaunchedEffect
        for (attempt in 1..20) {
            val ok = runCatching { first.requestFocus() }.getOrNull()
            if (ok != null && ok != false) break
            delay(50)
        }
    }
    Column(Modifier.fillMaxSize()) {
        AppTopBar(stringResource(R.string.device_alert_rules), navigationIcon = { BackButton(onBack) }, small = true)
        if (device == null) {
            Text(stringResource(R.string.device_missing), Modifier.padding(24.dp))
            return
        }
        Column(Modifier.focusGroup().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
            Row(Modifier.padding(horizontal = 24.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                DeviceGlyph(device, active = false, size = 40)
                Spacer(Modifier.width(12.dp))
                Text(device.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            DeviceAlertRules(deviceId, focusRequester = first)
        }
    }
}
