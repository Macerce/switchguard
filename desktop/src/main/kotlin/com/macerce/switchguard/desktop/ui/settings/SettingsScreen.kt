package com.macerce.switchguard.desktop.ui.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.DesktopWindows
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Summarize
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isBackPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.unit.dp
import com.macerce.switchguard.data.Store
import com.macerce.switchguard.desktop.AppGraph
import com.macerce.switchguard.desktop.i18n.Strings
import com.macerce.switchguard.desktop.platform.Sounds
import com.macerce.switchguard.desktop.ui.Brand
import com.macerce.switchguard.desktop.ui.Hue
import com.macerce.switchguard.desktop.ui.LocalDarkTheme
import com.macerce.switchguard.desktop.ui.LocalStrings
import com.macerce.switchguard.desktop.ui.MenuRow
import com.macerce.switchguard.desktop.ui.PageHeader
import com.macerce.switchguard.desktop.ui.RowDivider
import com.macerce.switchguard.desktop.ui.SectionCard
import com.macerce.switchguard.desktop.ui.SectionTitle
import com.macerce.switchguard.desktop.ui.readableWidth
import com.macerce.switchguard.desktop.ui.str
import com.macerce.switchguard.desktop.ui.tileColor

/**
 * Ayarlar alt sayfaları ve başlıklarının metin anahtarı. Ana sayfa yalnızca bunların listesidir; her biri tıklanınca
 * açılır. Telefondaki `SettingsPage`; İzinler yerine Windows, dil ve tema için Dil ve görünüm, Telegram ayrı sayfa.
 */
enum class SettingsPage(val title: String) {
    ACCOUNTS("settings_accounts"),
    ALARM("section_alarm"),
    QUIET("section_quiet_hours"),
    TELEGRAM("tg_title"),
    SUMMARY("section_summary"),
    MONITORING("section_monitoring"),
    WINDOWS("desktop_settings_windows"),
    BACKUP("settings_backup"),
    APPEARANCE("desktop_settings_appearance"),
    ABOUT("settings_about"),
}

/** Kurulu uygulamanın sürümü (jpackage); geliştirme çalıştırmasında "dev". */
internal fun appVersion(): String = System.getProperty("jpackage.app-version")?.takeIf { it.isNotBlank() } ?: "dev"

/** Motorun yalnızca başlarken okuduğu bir ayar değişti: izleme açıksa yeniden başlatılır (kapalıysa dokunulmaz). */
internal fun applyEngineSettings(graph: AppGraph) {
    if (graph.store.monitoringEnabled) graph.engine.restart()
}

/**
 * Ayarlar (telefondaki 2.12 düzeni): üstte uygulama kartı, altında gruplanmış kategoriler; her satırda tek satırlık
 * özet, sorun varsa turuncu uyarı. Kategoriye tıklayınca kendi sayfası açılır (geri oku, farenin geri tuşu ya da
 * Ayarlar sekmesine yeniden tıklamak — `AppState.settingsHome` — listeye döner). Alt sayfalar bir yığındır: Alarm
 * sayfasından açılan "İkinci kişiye haber ver" geri okuyla Alarm'a döner. Her değişiklik hemen `Store`'a yazılır
 * (Telegram sayfası "Kaydet"te); arayüz `store.version` ile yeniden çizilir. [startPage] testler ve ekran görüntüleri için.
 */
@Composable
fun SettingsScreen(graph: AppGraph, strings: Strings, modifier: Modifier = Modifier, startPage: SettingsPage? = null) {
    CompositionLocalProvider(LocalStrings provides strings) {
        var stack by remember { mutableStateOf(listOfNotNull(startPage)) }
        // Ayarlar sekmesine yeniden tıklanınca listeye dön (ilk değer sayılmaz).
        val homeRequests by graph.appState.settingsHome.collectAsState()
        var seenRequests by remember { mutableIntStateOf(homeRequests) }
        LaunchedEffect(homeRequests) {
            if (homeRequests != seenRequests) {
                seenRequests = homeRequests
                stack = emptyList()
            }
        }
        // Başka ekrandan istenen alt sayfa (AppState.openSettings): liste yerine doğrudan o sayfa.
        val request by graph.appState.settingsOpen.collectAsState()
        LaunchedEffect(request) {
            val page = request?.let { name -> SettingsPage.entries.firstOrNull { it.name == name } } ?: return@LaunchedEffect
            stack = listOf(page)
            graph.appState.settingsOpen.value = null
        }
        val open: (SettingsPage) -> Unit = { stack = stack + it }
        val back: () -> Unit = { stack = stack.dropLast(1) }

        AnimatedContent(
            targetState = stack,
            transitionSpec = {
                // İleri: yeni sayfa sağdan gelir; geri: önceki soldan.
                val forward = targetState.size > initialState.size
                (slideInHorizontally { w -> if (forward) w / 4 else -w / 4 } + fadeIn()) togetherWith
                    (slideOutHorizontally { w -> if (forward) -w / 4 else w / 4 } + fadeOut())
            },
            contentKey = { it.lastOrNull() },
            modifier = modifier.fillMaxSize(),
            label = "settingsPage",
        ) { pages ->
            when (val page = pages.lastOrNull()) {
                null -> SettingsHome(graph, strings, open)
                else -> SettingsSubPage(graph, strings, page, onBack = back, open = open)
            }
        }
    }
}

/**
 * Sayfa iskeleti: sabit başlık, altında kaydırılabilir içerik (geniş pencerede ortada okunabilir genişlikte) ve
 * sağda kaydırma çubuğu. [onBack] doluysa başlıkta geri oku olur ve farenin geri tuşu da geri gider.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun SettingsPageFrame(title: String, onBack: (() -> Unit)?, content: @Composable ColumnScope.() -> Unit) {
    val scroll = rememberScrollState()
    Column(
        Modifier.fillMaxSize().let { m ->
            if (onBack == null) m else m.onPointerEvent(PointerEventType.Press) { if (it.buttons.isBackPressed) onBack() }
        },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PageHeader(title, Modifier.readableWidth(), onBack = onBack)
        Box(Modifier.weight(1f).fillMaxWidth()) {
            Column(Modifier.fillMaxSize().verticalScroll(scroll), horizontalAlignment = Alignment.CenterHorizontally) {
                Column(Modifier.readableWidth().padding(bottom = 32.dp), content = content)
            }
            // Varsayılan çubuk siyah tonlu; koyu temada da görünsün diye yazı renginden.
            val ink = MaterialTheme.colorScheme.onSurface
            VerticalScrollbar(
                rememberScrollbarAdapter(scroll),
                Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(vertical = 4.dp),
                style = LocalScrollbarStyle.current.copy(unhoverColor = ink.copy(alpha = 0.18f), hoverColor = ink.copy(alpha = 0.45f)),
            )
        }
    }
}

@Composable
private fun SettingsHome(graph: AppGraph, strings: Strings, onOpen: (SettingsPage) -> Unit) {
    val store = graph.store
    val version by store.version.collectAsState()
    val s = remember(version, strings) { HomeSummaries(store, strings) }

    SettingsPageFrame(str("tab_settings"), onBack = null) {
        AppCard()

        SectionTitle(str("settings_group_connections"))
        SectionCard {
            MenuRow(
                Icons.Rounded.Hub, Hue.TEAL, str("settings_accounts"),
                onClick = { onOpen(SettingsPage.ACCOUNTS) }, summary = s.accounts, warning = s.accountsWarning,
            )
        }

        SectionTitle(str("settings_group_alerts"))
        SectionCard {
            MenuRow(Icons.Rounded.NotificationsActive, Hue.RED, str("section_alarm"), onClick = { onOpen(SettingsPage.ALARM) }, summary = s.alarm)
            RowDivider(68.dp)
            MenuRow(Icons.Rounded.Bedtime, Hue.INDIGO, str("section_quiet_hours"), onClick = { onOpen(SettingsPage.QUIET) }, summary = s.quiet)
            RowDivider(68.dp)
            MenuRow(Icons.AutoMirrored.Rounded.Send, Hue.BLUE, str("tg_title"), onClick = { onOpen(SettingsPage.TELEGRAM) }, summary = s.telegram)
            RowDivider(68.dp)
            MenuRow(Icons.Rounded.Summarize, Hue.AMBER, str("section_summary"), onClick = { onOpen(SettingsPage.SUMMARY) }, summary = s.summary)
        }

        SectionTitle(str("settings_group_app"))
        SectionCard {
            MenuRow(Icons.Rounded.Sync, Hue.GREEN, str("section_monitoring"), onClick = { onOpen(SettingsPage.MONITORING) }, summary = s.monitoring)
            RowDivider(68.dp)
            MenuRow(
                Icons.Rounded.DesktopWindows, Hue.ORANGE, str("desktop_settings_windows"),
                onClick = { onOpen(SettingsPage.WINDOWS) }, summary = s.windows, warning = s.windowsWarning,
            )
            RowDivider(68.dp)
            MenuRow(Icons.Rounded.Backup, Hue.PURPLE, str("settings_backup"), onClick = { onOpen(SettingsPage.BACKUP) }, summary = str("desktop_settings_backup_desc"))
            RowDivider(68.dp)
            MenuRow(Icons.Rounded.Language, Hue.SLATE, str("desktop_settings_appearance"), onClick = { onOpen(SettingsPage.APPEARANCE) }, summary = s.appearance)
        }

        SectionTitle(str("settings_group_support"))
        SectionCard {
            MenuRow(
                Icons.AutoMirrored.Rounded.HelpOutline, Hue.TEAL, str("settings_about"),
                onClick = { onOpen(SettingsPage.ABOUT) }, summary = str("settings_about_desc", appVersion()),
            )
        }
    }
}

/**
 * Ayarların başındaki uygulama kartı (telefondaki PlanCard): logo, ad ve plan. Masaüstünde Pro her zaman açık;
 * satın alma yok, kart tıklanmaz.
 */
@Composable
private fun AppCard() {
    val dark = LocalDarkTheme.current
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(tileColor())
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(56.dp).clip(RoundedCornerShape(18.dp)).background(Brand.guard(dark)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Shield, null, tint = Color.White, modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(str("app_name"), style = MaterialTheme.typography.titleLarge)
            Text(str("plan_pro"), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Rounded.WorkspacePremium, null, tint = MaterialTheme.colorScheme.primary)
    }
}

/** Kategori satırlarının özetleri ve uyarıları; `store.version` her değiştiğinde bir kez hesaplanır. */
internal class HomeSummaries(store: Store, strings: Strings) {
    val accounts: String = buildList {
        if (store.isLoggedIn) add("eWeLink")
        if (store.hasTuya) add("Tuya")
        if (store.demoMode) add(strings["demo_short"])
    }.let { if (it.isEmpty()) strings["settings_accounts_none"] else it.joinToString(" · ") }

    val accountsWarning: String? = when {
        store.sessionLost -> strings["session_lost_title"]
        store.secretsUnreadable -> strings["desktop_settings_secrets_warn"]
        else -> null
    }

    val alarm: String = Sounds.displayName(store.alarmSoundUri ?: Sounds.DEFAULT_ALARM, strings) + " · " +
        strings.format("volume_pct", Math.round(store.alarmVolume * 100))

    val quiet: String = store.quietHours.let { q ->
        if (!q.enabled) strings["state_off_title"]
        else strings.format("time_range", formatMinute(strings, q.startMinute), formatMinute(strings, q.endMinute))
    }

    val telegram: String = escalationSummary(store, strings)

    val summary: String =
        if (store.dailySummaryEnabled) strings.format("daily_at", formatMinute(strings, store.dailySummaryMinute))
        else strings["state_off_title"]

    val monitoring: String = monitoringSummary(strings, store.offlineGraceSec, strings.format("poll_interval_value", store.pollIntervalSec))

    /** Windows: ikisi de açıksa özet; kapalı olan her biri uyarıdır (bilgisayar yeniden başlayınca ya da uyuyunca alarm kaçar). */
    val windowsWarning: String? = listOfNotNull(
        strings["desktop_settings_windows_no_autostart"].takeIf { !store.autostart },
        strings["desktop_settings_windows_may_sleep"].takeIf { !store.alwaysAwake },
    ).takeIf { it.isNotEmpty() }?.joinToString(" · ")

    val windows: String = strings["desktop_settings_windows_ok"]

    val appearance: String = languageLabel(strings, store.language) + " · " + themeLabel(strings, store.theme)
}

@Composable
private fun SettingsSubPage(graph: AppGraph, strings: Strings, page: SettingsPage, onBack: () -> Unit, open: (SettingsPage) -> Unit) {
    SettingsPageFrame(str(page.title), onBack = onBack) {
        when (page) {
            SettingsPage.ACCOUNTS -> AccountsPage(graph, strings)
            SettingsPage.ALARM -> AlarmPage(graph, strings, open)
            SettingsPage.QUIET -> QuietPage(graph.store, strings)
            SettingsPage.TELEGRAM -> TelegramPage(graph.store, onDone = onBack)
            SettingsPage.SUMMARY -> SummaryPage(graph.store, strings, preview = graph.engine::summaryPreview)
            SettingsPage.MONITORING -> MonitoringPage(graph, strings)
            SettingsPage.WINDOWS -> WindowsPage(graph, strings)
            SettingsPage.BACKUP -> BackupPage(graph, strings)
            SettingsPage.APPEARANCE -> AppearancePage(graph.store, strings)
            SettingsPage.ABOUT -> AboutPage(graph, strings)
        }
    }
}

// ---------------------------------------------------------------- etiketler (telefondaki yardımcılar)

/** Dil adları her zaman kendi dilinde yazılır: yanlış dili seçen de geri dönebilsin. */
internal fun languageLabel(strings: Strings, tag: String): String = when (tag) {
    "en" -> "English"
    "tr" -> "Türkçe"
    else -> strings["desktop_language_system"]
}

internal fun themeLabel(strings: Strings, theme: String): String = when (theme) {
    "light" -> strings["desktop_theme_light"]
    "dark" -> strings["desktop_theme_dark"]
    else -> strings["desktop_theme_system"]
}

internal fun reminderLabel(strings: Strings, min: Int): String =
    if (min == 0) strings["blind_alert_off"] else strings.format("reminder_every", min)

internal fun blindLabel(strings: Strings, min: Int): String =
    if (min == 0) strings["blind_alert_off"] else strings.format("dur_minutes", min)

internal fun graceLabel(strings: Strings, sec: Int): String = when {
    sec == 0 -> strings["grace_off"]
    sec < 60 -> strings.plural("seconds_n", sec, sec)
    else -> strings.plural("minutes_n", sec / 60, sec / 60)
}

internal fun monitoringSummary(strings: Strings, graceSec: Int, poll: String): String =
    if (graceSec == 0) strings.format("monitoring_summary_now", poll)
    else strings.format("monitoring_summary_grace", graceLabel(strings, graceSec), poll)
