package com.macerce.switchguard.desktop.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.rounded.Nightlight
import androidx.compose.material3.Switch
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.macerce.switchguard.data.Store
import com.macerce.switchguard.desktop.AppGraph
import com.macerce.switchguard.desktop.i18n.Strings
import com.macerce.switchguard.desktop.platform.Autostart
import com.macerce.switchguard.desktop.syncAutostart
import com.macerce.switchguard.desktop.ui.InfoRow
import com.macerce.switchguard.desktop.ui.RowDivider
import com.macerce.switchguard.desktop.ui.SectionCard
import com.macerce.switchguard.desktop.ui.SectionTitle
import com.macerce.switchguard.desktop.ui.str

/** Sorgulama aralığı seçenekleri (sn). */
private val POLL_CHOICES = listOf(15, 30, 60, 120, 300)

/** Çevrimdışı tolerans seçenekleri (sn); 0 = kapalı. */
private val GRACE_CHOICES = listOf(0, 30, 60, 120, 300)

/** Kör kalma uyarısı seçenekleri (dk); 0 = kapalı. */
private val BLIND_CHOICES = listOf(0, 5, 10, 15, 30, 60)

// ---------------------------------------------------------------- İzleme

/**
 * Ayarlar → İzleme (telefondaki MonitoringPage): kısa kopmaları yok sayma, kör kalma uyarısı, yedek kontrol aralığı;
 * Tuya bağlıysa canlı bildirimler (açmadan önce telefonla paylaşım uyarısı). Motor bunları her tick'te okur; yalnızca
 * Tuya canlı bağlantısı izlemeyi yeniden başlatmayı gerektirir.
 */
@Composable
fun MonitoringPage(graph: AppGraph, strings: Strings) {
    val store = graph.store
    val version by store.version.collectAsState()
    val grace = remember(version) { store.offlineGraceSec }
    val blind = remember(version) { store.blindAlertMin }
    val poll = remember(version) { store.pollIntervalSec }
    val hasTuya = remember(version) { store.hasTuya }
    val tuyaLive = remember(version) { store.tuyaLive }
    var dialog by remember { mutableStateOf<String?>(null) } // "grace", "blind", "poll", "tuya-live"
    val close = { dialog = null }

    PageIntro(str("settings_monitoring_intro"))
    SectionCard(Modifier.padding(top = 12.dp)) {
        InfoRow(Icons.Rounded.Timer, str("offline_grace"), graceLabel(strings, grace), Modifier.clickable { dialog = "grace" })
        RowDivider()
        InfoRow(Icons.Rounded.WifiOff, str("blind_alert"), blindLabel(strings, blind), Modifier.clickable { dialog = "blind" })
        RowDivider()
        InfoRow(Icons.Rounded.Sync, str("poll_interval"), str("poll_interval_value", poll), Modifier.clickable { dialog = "poll" })
    }

    if (hasTuya) {
        SectionTitle(str("tuya_account"))
        SectionCard {
            SwitchInfoRow(Icons.Rounded.Cloud, str("desktop_tuya_live"), str("desktop_tuya_live_desc"), tuyaLive) { on ->
                if (on) {
                    dialog = "tuya-live"
                } else {
                    store.tuyaLive = false
                    applyEngineSettings(graph)
                }
            }
        }
    }

    when (dialog) {
        "grace" -> ChoiceDialog(
            str("offline_grace"), str("offline_grace_desc"), GRACE_CHOICES, grace, { graceLabel(strings, it) },
            onPick = { store.offlineGraceSec = it }, onDismiss = close,
        )
        "blind" -> ChoiceDialog(
            str("blind_alert"), str("blind_alert_desc"), BLIND_CHOICES, blind, { blindLabel(strings, it) },
            onPick = { store.blindAlertMin = it }, onDismiss = close,
        )
        "poll" -> ChoiceDialog(
            str("poll_interval"), str("poll_interval_desc"), POLL_CHOICES, poll, { strings.format("poll_interval_value", it) },
            onPick = { store.pollIntervalSec = it }, onDismiss = close,
        )
        "tuya-live" -> Confirm(strings, strings["desktop_tuya_live_warning"], strings["desktop_turn_on"], onDismiss = close) {
            store.tuyaLive = true
            applyEngineSettings(graph)
        }
    }
}

// ---------------------------------------------------------------- Windows

/**
 * Ayarlar → Windows (telefondaki İzinler yerine): Windows ile başlat ve bilgisayarı uyanık tut. İkisi de alarmın
 * ulaşması için gerekli; kapalı olanlar ana listede turuncu uyarı olarak görünür. [applyAutostart] kayıt defterini
 * eşitler; kurulu uygulama dışında (geliştirme, testler) kayıt defterine dokunmaz, testlerde ayrıca sahtesi verilir.
 */
@Composable
fun WindowsPage(
    graph: AppGraph,
    strings: Strings,
    applyAutostart: (Boolean) -> Unit = { syncAutostart(Autostart(), it) },
) {
    val store = graph.store
    val version by store.version.collectAsState()
    val autostart = remember(version) { store.autostart }
    val awake = remember(version) { store.alwaysAwake }

    PageIntro(str("desktop_settings_windows_intro"))
    SectionCard(Modifier.padding(top = 12.dp)) {
        SwitchInfoRow(Icons.Rounded.PowerSettingsNew, str("desktop_autostart"), str("desktop_autostart_desc"), autostart) {
            store.autostart = it
            applyAutostart(it)
        }
        RowDivider()
        SwitchInfoRow(Icons.Rounded.Bolt, str("desktop_keep_awake"), str("desktop_keep_awake_desc"), awake) {
            store.alwaysAwake = it
            applyEngineSettings(graph)
        }
    }
}

// ---------------------------------------------------------------- Dil ve görünüm

/** Ayarlar → Dil ve görünüm: arayüz dili (telefondaki dil satırı) ve tema (masaüstünde elle de seçilir). */
@Composable
fun AppearancePage(
    store: Store, strings: Strings, onOpenStandby: () -> Unit = {},
    screenSaver: com.macerce.switchguard.desktop.platform.ScreenSaver? = null,
) {
    val version by store.version.collectAsState()
    val language = remember(version) { store.language }
    val theme = remember(version) { store.theme }
    var dialog by remember { mutableStateOf<String?>(null) } // "language" / "theme"
    val close = { dialog = null }

    SectionCard(Modifier.padding(top = 4.dp)) {
        InfoRow(Icons.Rounded.Language, str("language"), languageLabel(strings, language), Modifier.clickable { dialog = "language" })
        RowDivider()
        InfoRow(Icons.Rounded.Palette, str("desktop_theme"), themeLabel(strings, theme), Modifier.clickable { dialog = "theme" })
    }

    // Bekleme ekranı (telefon 2.25): saat, hava, güç ve son hareketler; tam ekran, Esc kapatır.
    val dim = remember(version) { store.standbyDim }
    SectionCard(Modifier.padding(top = 12.dp)) {
        InfoRow(
            Icons.Rounded.Nightlight, str("board_start"), str("desktop_board_start_desc"),
            Modifier.clickable(onClick = onOpenStandby).testTag("settings-open-standby"),
        )
        RowDivider()
        InfoRow(Icons.Rounded.Nightlight, str("board_dim"), str("board_dim_desc")) {
            Switch(checked = dim, onCheckedChange = { store.standbyDim = it }, modifier = Modifier.testTag("standby-dim"))
        }
        if (screenSaver != null) {
            RowDivider()
            ScreenSaverRows(screenSaver, strings)
        }
    }

    when (dialog) {
        "language" -> ChoiceDialog(
            str("language"), str("language_desc"), listOf("") + Strings.LANGUAGES, language, { languageLabel(strings, it) },
            onPick = { store.language = it }, onDismiss = close,
        )
        "theme" -> ChoiceDialog(
            str("desktop_theme"), str("desktop_theme_desc"), listOf("system", "light", "dark"), theme, { themeLabel(strings, it) },
            onPick = { store.theme = it }, onDismiss = close,
        )
    }
}

/**
 * Windows ekran koruyucusu (telefondaki "ekran koruyucu olarak kullan"): açınca SwitchGuard ekran koruyucu olur,
 * bilgisayar seçilen süre boşta kalınca bekleme ekranı açılır; fare ya da klavye kapatır.
 */
@Composable
private fun ScreenSaverRows(saver: com.macerce.switchguard.desktop.platform.ScreenSaver, strings: Strings) {
    var refresh by remember { mutableStateOf(0) }
    val installed = remember(refresh) { saver.isInstalled() }
    val minutes = remember(refresh) { saver.timeoutMinutes()?.takeIf { it > 0 } ?: 5 }
    var failed by remember { mutableStateOf(false) }
    var pickMinutes by remember { mutableStateOf(false) }
    val subtitle = when {
        !saver.available -> str("desktop_saver_unavailable")
        failed -> str("desktop_saver_failed")
        installed -> strings.format("desktop_saver_on", strings.plural("minutes_n", minutes, minutes))
        else -> str("desktop_saver_desc")
    }
    InfoRow(Icons.Rounded.Nightlight, str("desktop_saver"), subtitle) {
        Switch(
            checked = installed, enabled = saver.available,
            onCheckedChange = { on ->
                failed = !(if (on) saver.install(minutes) else saver.uninstall())
                refresh++
            },
            modifier = Modifier.testTag("screen-saver"),
        )
    }
    if (installed) {
        RowDivider()
        InfoRow(
            Icons.Rounded.Nightlight, str("desktop_saver_wait"), strings.plural("minutes_n", minutes, minutes),
            Modifier.clickable { pickMinutes = true }.testTag("screen-saver-minutes"),
        )
        RowDivider()
        InfoRow(
            Icons.Rounded.Nightlight, str("desktop_saver_windows"), str("desktop_saver_windows_desc"),
            Modifier.clickable { saver.openWindowsSettings() },
        )
    }
    if (pickMinutes) {
        ChoiceDialog(
            str("desktop_saver_wait"), str("desktop_saver_wait_desc"),
            com.macerce.switchguard.desktop.platform.ScreenSaver.MINUTES, minutes, { strings.plural("minutes_n", it, it) },
            onPick = { failed = !saver.install(it); refresh++ }, onDismiss = { pickMinutes = false },
        )
    }
}

// ---------------------------------------------------------------- Yedekleme ve aktarım

/**
 * Ayarlar → Yedekleme ve aktarım: şifreli dosyaya yedekle / dosyadan geri yükle ([BackupSection]). Telefondaki
 * Wi-Fi ile başka cihaza aktarma masaüstünde yok; başka bilgisayara geçiş yedek dosyasıyla yapılır.
 */
@Composable
fun BackupPage(graph: AppGraph, strings: Strings) {
    SectionTitle(str("section_backup"))
    SectionCard { BackupSection(graph, strings) }
    PageIntro(str("desktop_backup_move_note"), Modifier.padding(top = 8.dp))
}
