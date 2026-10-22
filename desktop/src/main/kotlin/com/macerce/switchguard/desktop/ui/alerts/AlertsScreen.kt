package com.macerce.switchguard.desktop.ui.alerts

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.macerce.switchguard.core.AlertAction
import com.macerce.switchguard.core.DeviceKinds
import com.macerce.switchguard.core.DeviceRules
import com.macerce.switchguard.core.DeviceSnapshot
import com.macerce.switchguard.desktop.AppGraph
import com.macerce.switchguard.desktop.i18n.Strings
import com.macerce.switchguard.desktop.ui.Brand
import com.macerce.switchguard.desktop.ui.Hue
import com.macerce.switchguard.desktop.ui.IconBadge
import com.macerce.switchguard.desktop.ui.LocalDarkTheme
import com.macerce.switchguard.desktop.ui.LocalStrings
import com.macerce.switchguard.desktop.ui.MenuRow
import com.macerce.switchguard.desktop.ui.OnlineDot
import com.macerce.switchguard.desktop.ui.RowDivider
import com.macerce.switchguard.desktop.ui.SectionCard
import com.macerce.switchguard.desktop.ui.SectionTitle
import com.macerce.switchguard.desktop.ui.StatusColors
import com.macerce.switchguard.desktop.ui.automations.AutomationCard
import com.macerce.switchguard.desktop.ui.automations.AutomationEditor
import com.macerce.switchguard.desktop.ui.automations.AutomationMode
import com.macerce.switchguard.desktop.ui.automations.inMode
import com.macerce.switchguard.desktop.ui.devices.DeviceRulesSection
import com.macerce.switchguard.desktop.ui.devices.rememberDeviceRules
import com.macerce.switchguard.desktop.ui.icon
import com.macerce.switchguard.desktop.ui.raisedOnTile
import com.macerce.switchguard.desktop.ui.settings.AlarmPage
import com.macerce.switchguard.desktop.ui.settings.AlertSettingsSummaries
import com.macerce.switchguard.desktop.ui.settings.QuietPage
import com.macerce.switchguard.desktop.ui.settings.SmartPage
import com.macerce.switchguard.desktop.ui.HueIcon
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BeachAccess
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import com.macerce.switchguard.desktop.ui.settings.SettingsPageFrame
import com.macerce.switchguard.desktop.ui.settings.TEST_DELAY_SEC
import com.macerce.switchguard.desktop.ui.settings.TelegramPage
import com.macerce.switchguard.desktop.ui.settings.TestAlarmDialog
import com.macerce.switchguard.desktop.ui.settings.windowsIssues
import com.macerce.switchguard.desktop.ui.str
import com.macerce.switchguard.desktop.ui.tileColor
import kotlinx.coroutines.delay

/** Uyarılar sekmesinin alt sayfaları; bir yığındır (geri oku bir öncekine döner). */
sealed interface AlertsPage {
    /** Bir cihazın uyarı kuralları: cihaz sayfasındaki kurallar bölümünün aynısı. */
    data class Rules(val deviceId: String) : AlertsPage

    /** Koşullu uyarı düzenleyicisi (uyarı modunda); [automationId] null ise yeni uyarı. */
    data class Editor(val automationId: String?) : AlertsPage

    data object Alarm : AlertsPage

    data object Quiet : AlertsPage

    data object Telegram : AlertsPage

    /** Akıllı uyarılar (telefon 2.15). */
    data object Smart : AlertsPage
}

/**
 * Uyarılar sekmesi (telefon 2.13; "uyarıyla ilgili her şey" burada), yukarıdan aşağı: durum kartı (izleme kapalı /
 * Windows ayarı eksik / alarm hazır, test alarmı), cihaz uyarıları (her cihazın hangi değişiklikte alarm ya da bildirim
 * verdiği; satır o cihazın uyarı kuralları sayfasını açar), koşullu uyarılar (alarm/bildirim veren otomasyon kayıtları,
 * [AutomationMode.ALERT]) ve uyarı ayarları (Ayarlar'dan taşınan Alarm, Sessiz saatler, İkinci kişiye haber ver
 * sayfaları). Alt sayfalar bir yığındır; Uyarılar sekmesine yeniden tıklamak (`AppState.alertsHome`) başa döner.
 * Kural ve kayıt değişiklikleri motor üzerinden yazılır. [startPage] ve [testAlarm] testler ve ekran görüntüleri için
 * (testte bilgisayarda gerçekten alarm çalmasın).
 */
@Composable
fun AlertsScreen(
    graph: AppGraph,
    strings: Strings,
    modifier: Modifier = Modifier,
    startPage: AlertsPage? = null,
    testAlarm: (silent: Boolean, delayMs: Long) -> Unit = graph.engine::testAlarm,
) {
    CompositionLocalProvider(LocalStrings provides strings) {
        var stack by remember { mutableStateOf(listOfNotNull(startPage)) }
        // Uyarılar sekmesine yeniden tıklanınca başa dön (ilk değer sayılmaz).
        val homeRequests by graph.appState.alertsHome.collectAsState()
        var seenRequests by remember { mutableIntStateOf(homeRequests) }
        LaunchedEffect(homeRequests) {
            if (homeRequests != seenRequests) {
                seenRequests = homeRequests
                stack = emptyList()
            }
        }
        val open: (AlertsPage) -> Unit = { stack = stack + it }
        val back: () -> Unit = { stack = stack.dropLast(1) }

        AnimatedContent(
            targetState = stack,
            transitionSpec = {
                // İleri: yeni sayfa sağdan gelir; geri: önceki soldan (Ayarlar'daki gibi).
                val forward = targetState.size > initialState.size
                (slideInHorizontally { w -> if (forward) w / 4 else -w / 4 } + fadeIn()) togetherWith
                    (slideOutHorizontally { w -> if (forward) -w / 4 else w / 4 } + fadeOut())
            },
            contentKey = { it.lastOrNull() },
            modifier = modifier.fillMaxSize(),
            label = "alertsPage",
        ) { pages ->
            when (val page = pages.lastOrNull()) {
                null -> AlertsHome(graph, strings, open, testAlarm)
                is AlertsPage.Rules -> SettingsPageFrame(str("device_alert_rules"), onBack = back) { RulesPage(graph, page.deviceId) }
                is AlertsPage.Editor -> ConditionalAlertEditor(graph, strings, page.automationId, onDone = back)
                AlertsPage.Alarm -> SettingsPageFrame(str("section_alarm"), onBack = back) {
                    AlarmPage(graph, strings, onTelegram = { open(AlertsPage.Telegram) }, testAlarm = testAlarm)
                }
                AlertsPage.Quiet -> SettingsPageFrame(str("section_quiet_hours"), onBack = back) { QuietPage(graph.store, strings) }
                AlertsPage.Telegram -> SettingsPageFrame(str("tg_title"), onBack = back) { TelegramPage(graph.store, onDone = back) }
                AlertsPage.Smart -> SettingsPageFrame(str("smart_title"), onBack = back) {
                    SmartPage(graph, strings, onTelegram = { open(AlertsPage.Telegram) })
                }
            }
        }
    }
}

@Composable
private fun AlertsHome(graph: AppGraph, strings: Strings, open: (AlertsPage) -> Unit, testAlarm: (Boolean, Long) -> Unit) {
    val store = graph.store
    val version by store.version.collectAsState()
    val monitoring = remember(version) { store.monitoringEnabled }
    val hasAccount = remember(version) { store.hasAnyAccount }
    val issues = remember(version) { windowsIssues(store.autostart, store.alwaysAwake) }
    val devices = remember(version) { store.deviceLayout.sorted(store.snapshots) }
    val rows = remember(version, strings) { devices.map { DeviceAlertRowData(it, store.rulesFor(it.id), strings) } }
    val alerts = remember(version) { store.automations.inMode(AutomationMode.ALERT) }
    val summaries = remember(version, strings) { AlertSettingsSummaries(store, strings) }

    SettingsPageFrame(str("tab_alerts"), onBack = null) {
        StatusCard(
            readiness = alertReadiness(monitoring, issues),
            issues = issues.joinToString(" · ") { strings[it] },
            hasAccount = hasAccount,
            onStart = { graph.engine.start() },
            onAddAccount = { graph.appState.openSettings("ACCOUNTS") },
            onFixWindows = { graph.appState.openSettings("WINDOWS") },
            testAlarm = testAlarm,
        )
        PresenceCard(graph, strings)

        // ------------------------------------------------ Cihaz uyarıları
        SectionTitle(str("section_device_alerts"))
        SectionHint(str("section_device_alerts_desc"))
        SectionCard {
            if (rows.isEmpty()) {
                Text(str("no_devices"), Modifier.padding(20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            rows.forEachIndexed { i, row ->
                if (i > 0) RowDivider(72.dp)
                DeviceAlertRow(row) { open(AlertsPage.Rules(row.device.id)) }
            }
        }

        // ------------------------------------------------ Koşullu uyarılar
        // "Ekle" başlıkla aynı hizada (başlığın üst boşluğu kadar aşağıda), kartlardan uzak.
        Row(verticalAlignment = Alignment.Top) {
            SectionTitle(str("section_conditional_alerts"), Modifier.weight(1f).padding(top = 8.dp))
            FilledTonalButton(
                onClick = { open(AlertsPage.Editor(null)) },
                enabled = devices.isNotEmpty(),
                modifier = Modifier.padding(top = 20.dp, end = 16.dp).height(36.dp).testTag("add-alert"),
            ) {
                Icon(Icons.Rounded.Add, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(str("automation_add"))
            }
        }
        SectionHint(str("conditional_alerts_desc"))
        if (alerts.isEmpty()) {
            SectionCard {
                Text(
                    str("conditional_alerts_empty"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(20.dp).testTag("conditional-empty"),
                )
            }
        } else {
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                alerts.forEach { a ->
                    AutomationCard(
                        a, store.snapshots, strings,
                        onClick = { open(AlertsPage.Editor(a.id)) },
                        onToggle = { on -> graph.engine.saveAutomation(a.copy(enabled = on)) },
                    )
                }
            }
        }

        // ------------------------------------------------ Uyarı ayarları
        SectionTitle(str("section_alert_settings"))
        SectionCard {
            MenuRow(
                Icons.Rounded.NotificationsActive, Hue.RED, str("section_alarm"),
                onClick = { open(AlertsPage.Alarm) }, summary = summaries.alarm, modifier = Modifier.testTag("alert-settings-alarm"),
            )
            RowDivider(68.dp)
            MenuRow(
                Icons.Rounded.Bedtime, Hue.INDIGO, str("section_quiet_hours"),
                onClick = { open(AlertsPage.Quiet) }, summary = summaries.quiet, modifier = Modifier.testTag("alert-settings-quiet"),
            )
            RowDivider(68.dp)
            MenuRow(
                Icons.AutoMirrored.Rounded.Send, Hue.BLUE, str("tg_title"),
                onClick = { open(AlertsPage.Telegram) }, summary = summaries.telegram, modifier = Modifier.testTag("alert-settings-telegram"),
            )
            RowDivider(68.dp)
            MenuRow(
                Icons.Rounded.AutoAwesome, Hue.AMBER, str("smart_title"),
                onClick = { open(AlertsPage.Smart) }, summary = str("smart_summary"), modifier = Modifier.testTag("alert-settings-smart"),
            )
        }
    }
}

/** Evde / Dışarıda seçimi (telefondaki `PresenceCard`); tatildeyse bitiş günü yazar ve seçim kilitlenir. */
@Composable
private fun PresenceCard(graph: AppGraph, strings: Strings) {
    val store = graph.store
    val version by store.version.collectAsState()
    val vacation = remember(version) { store.onVacation() }
    val away = remember(version) { store.awayMode }
    SectionCard(Modifier.padding(top = 8.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                if (vacation) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        HueIcon(Icons.Rounded.BeachAccess, Hue.AMBER, size = 28.dp)
                        Text(
                            strings.format("vacation_until", java.text.SimpleDateFormat("d MMM", strings.locale).format(java.util.Date(store.vacationUntil - 1))),
                            style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 12.dp),
                        )
                    }
                } else {
                    Text(str("presence_section"), style = MaterialTheme.typography.titleSmall)
                }
                Text(
                    str(if (vacation) "vacation_effect" else if (away) "presence_away_desc" else "presence_home_desc"),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            if (!vacation) {
                Spacer(Modifier.width(16.dp))
                SingleChoiceSegmentedButtonRow {
                    listOf(false, true).forEachIndexed { i, a ->
                        SegmentedButton(
                            selected = away == a,
                            onClick = { graph.engine.setAway(a) },
                            shape = SegmentedButtonDefaults.itemShape(i, 2),
                            icon = { Icon(if (a) Icons.AutoMirrored.Rounded.DirectionsWalk else Icons.Rounded.Home, null, Modifier.size(18.dp)) },
                            modifier = Modifier.testTag("presence-$a"),
                        ) { Text(str(if (a) "presence_away" else "presence_home")) }
                    }
                }
            }
        }
    }
}

/** Bölüm başlığının altındaki tek satırlık açıklama. */
@Composable
private fun SectionHint(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 8.dp),
    )
}

/**
 * Durum kartı: izleme kapalıysa "İzleme kapalı" ve "İzlemeyi başlat" (hesap yoksa "Hesap ekle"); izleme açık ama Windows
 * ile başlat ya da uyanık tut kapalıysa turuncu uyarı (tıklayınca Ayarlar › Windows); yoksa marka renginde "Alarm hazır".
 * Her halde "Test alarmı" (sesli/sessiz, istenirse 10 sn sonra). Cihazlar ekranındaki koruma kartının yerini almaz.
 */
@Composable
private fun StatusCard(
    readiness: AlertReadiness,
    issues: String,
    hasAccount: Boolean,
    onStart: () -> Unit,
    onAddAccount: () -> Unit,
    onFixWindows: () -> Unit,
    testAlarm: (Boolean, Long) -> Unit,
) {
    val dark = LocalDarkTheme.current
    val scheme = MaterialTheme.colorScheme
    var dialog by remember { mutableStateOf(false) }
    // Gecikmeli test alarmı kuruldu: kartta kısa süre "N saniye sonra çalacak" yazar.
    var scheduled by remember { mutableIntStateOf(0) }
    LaunchedEffect(scheduled) {
        if (scheduled > 0) {
            delay(TEST_DELAY_SEC * 1000L)
            scheduled = 0
        }
    }

    val ready = readiness == AlertReadiness.READY
    val background: Brush = if (ready) Brand.guard(dark) else SolidColor(tileColor())
    val content = if (ready) Brand.onActive else scheme.onSurface
    val muted = if (ready) Brand.onActive.copy(alpha = 0.85f) else scheme.onSurfaceVariant

    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(28.dp)).background(background).padding(20.dp)
            .testTag("alerts-status-" + readiness.name.lowercase()),
    ) {
        val warning = readiness == AlertReadiness.WARNING
        Row(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .then(if (warning) Modifier.clickable(role = Role.Button, onClick = onFixWindows) else Modifier),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val (icon, iconBg, iconTint) = when (readiness) {
                AlertReadiness.READY -> Triple(Icons.Rounded.Shield, Color.White.copy(alpha = 0.18f), Color.White)
                AlertReadiness.WARNING -> Triple(Icons.Rounded.GppMaybe, StatusColors.warning, Color.White)
                AlertReadiness.MONITORING_OFF -> Triple(Icons.Rounded.RemoveModerator, raisedOnTile(), scheme.onSurfaceVariant)
            }
            Box(Modifier.size(52.dp).background(iconBg, CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = iconTint, modifier = Modifier.size(30.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    when (readiness) {
                        AlertReadiness.READY -> str("alerts_ready")
                        AlertReadiness.WARNING -> issues
                        AlertReadiness.MONITORING_OFF -> str("alerts_monitoring_off")
                    },
                    style = MaterialTheme.typography.titleLarge,
                    color = if (warning) StatusColors.warning else content,
                )
                Text(
                    when (readiness) {
                        AlertReadiness.READY -> str("alerts_ready_desc")
                        AlertReadiness.WARNING -> str("desktop_alerts_windows_desc")
                        AlertReadiness.MONITORING_OFF -> str("alerts_monitoring_off_desc")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = muted,
                )
            }
            if (warning) Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = scheme.onSurfaceVariant)
        }

        // İzleme kapalıyken de Windows sorunu görünsün (izleme başlayınca kartın kendisi uyarıya döner).
        if (readiness == AlertReadiness.MONITORING_OFF && issues.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(raisedOnTile())
                    .clickable(role = Role.Button, onClick = onFixWindows).padding(horizontal = 14.dp, vertical = 10.dp)
                    .testTag("alerts-windows-warning"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Warning, null, tint = StatusColors.warning, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Text(issues, style = MaterialTheme.typography.bodyMedium, color = StatusColors.warning, modifier = Modifier.weight(1f))
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = scheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(16.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (readiness == AlertReadiness.MONITORING_OFF) {
                if (hasAccount) {
                    Button(onClick = onStart, modifier = Modifier.height(48.dp).testTag("alerts-start")) {
                        Icon(Icons.Rounded.PlayArrow, null)
                        Spacer(Modifier.width(8.dp))
                        Text(str("action_start_monitoring"))
                    }
                } else {
                    Button(onClick = onAddAccount, modifier = Modifier.height(48.dp).testTag("alerts-add-account")) {
                        Text(str("desktop_add_account"))
                    }
                }
            }
            OutlinedButton(
                onClick = { dialog = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = if (ready) Color.White else scheme.primary),
                border = BorderStroke(1.dp, if (ready) Color.White.copy(alpha = 0.6f) else scheme.outline),
                modifier = Modifier.height(48.dp).testTag("alerts-test"),
            ) {
                Icon(Icons.Rounded.NotificationsActive, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(str("test_alarm"))
            }
        }
        if (scheduled > 0) {
            Text(
                str("test_alarm_scheduled", TEST_DELAY_SEC),
                style = MaterialTheme.typography.bodySmall,
                color = muted,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }

    if (dialog) {
        TestAlarmDialog(
            onStart = { silent, delayMs ->
                testAlarm(silent, delayMs)
                if (delayMs > 0) scheduled++
            },
            onDismiss = { dialog = false },
        )
    }
}

/** Cihaz uyarıları satırının verisi; `store.version` değişince bir kez kurulur. */
private class DeviceAlertRowData(val device: DeviceSnapshot, rules: DeviceRules, strings: Strings) {
    val monitored = rules.monitored
    val critical = rules.monitored && rules.critical
    val customSound = rules.monitored && rules.soundUri != null
    val lines = deviceAlertLines(rules, strings)
    val kind = DeviceKinds.of(device)
}

/**
 * Bir cihazın satırı: türünün simgesi (izlenmiyorsa soluk), adı, kritikse küçük rozet, cihaza özel ses varsa nota;
 * altında alarm ve bildirim verilen olaylar (renkli noktalı satırlar). Tıklayınca o cihazın uyarı kuralları açılır.
 */
@Composable
private fun DeviceAlertRow(row: DeviceAlertRowData, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val dark = LocalDarkTheme.current
    Row(
        Modifier.fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("device-alerts-${row.device.id}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(
            row.kind.icon(),
            tint = if (row.monitored) scheme.onPrimaryContainer else scheme.outline,
            background = if (row.monitored) scheme.primaryContainer else raisedOnTile(),
            size = 40.dp,
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    row.device.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (row.critical) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        str("device_critical"),
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onErrorContainer,
                        maxLines = 1,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(scheme.errorContainer)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
                if (row.customSound) {
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Rounded.MusicNote, str("device_sound"), tint = scheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                }
            }
            row.lines.forEach { line ->
                // Alarm satırı kırmızı, bildirim satırı mavi noktalı (Geçmiş'teki renkler); özet satırında nokta yok.
                val dot = when (line.action) {
                    AlertAction.ALARM -> if (dark) Hue.RED.dark else Hue.RED.light
                    AlertAction.NOTIFY -> if (dark) Hue.BLUE.dark else Hue.BLUE.light
                    else -> null
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (dot != null) {
                        Box(Modifier.size(7.dp).background(dot, CircleShape))
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(line.text, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = scheme.onSurfaceVariant)
    }
}

/** Kurallar sayfası: üstte cihazın kimliği, altında cihaz sayfasındakiyle aynı kurallar bölümü. */
@Composable
private fun RulesPage(graph: AppGraph, deviceId: String) {
    val version by graph.store.version.collectAsState()
    val device = remember(version) { graph.store.snapshots.firstOrNull { it.id == deviceId } }
    if (device == null) {
        Text(str("device_missing"), Modifier.padding(24.dp))
        return
    }
    val kind = remember(device.name, device.isMultiChannel, device.hasEnergy, device.cloud) { DeviceKinds.of(device) }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(24.dp)).background(tileColor()).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(kind.icon(), MaterialTheme.colorScheme.onPrimaryContainer, MaterialTheme.colorScheme.primaryContainer, size = 44.dp)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(device.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            OnlineDot(device.online)
        }
    }
    DeviceRulesSection(graph, rememberDeviceRules(graph, deviceId))
}

/** Koşullu uyarı düzenleyicisi: kayıt motor üzerinden yazılır, sonra sekmenin başına dönülür. */
@Composable
private fun ConditionalAlertEditor(graph: AppGraph, strings: Strings, automationId: String?, onDone: () -> Unit) {
    val version by graph.store.version.collectAsState()
    // Düzenleyici açıkken kayıt değişse de form açıldığı haliyle kalır.
    val initial = remember(automationId) { automationId?.let { id -> graph.store.automations.firstOrNull { it.id == id } } }
    val devices = remember(version) { graph.store.snapshots }
    AutomationEditor(
        initial = initial,
        devices = devices,
        strings = strings,
        modifier = Modifier.fillMaxSize(),
        onSave = {
            graph.engine.saveAutomation(it)
            onDone()
        },
        onDelete = {
            automationId?.let(graph.engine::deleteAutomation)
            onDone()
        },
        onBack = onDone,
        newMode = AutomationMode.ALERT,
        store = graph.store,
        outdoorTemp = graph.runtime.outdoorTemp,
    )
}
