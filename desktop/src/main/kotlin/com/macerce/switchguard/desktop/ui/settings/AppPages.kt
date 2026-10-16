package com.macerce.switchguard.desktop.ui.settings

import androidx.compose.foundation.clickable
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
fun AppearancePage(store: Store, strings: Strings) {
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

    when (dialog) {
        "language" -> ChoiceDialog(
            str("language"), str("language_desc"), listOf("", "tr", "en"), language, { languageLabel(strings, it) },
            onPick = { store.language = it }, onDismiss = close,
        )
        "theme" -> ChoiceDialog(
            str("desktop_theme"), str("desktop_theme_desc"), listOf("system", "light", "dark"), theme, { themeLabel(strings, it) },
            onPick = { store.theme = it }, onDismiss = close,
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
