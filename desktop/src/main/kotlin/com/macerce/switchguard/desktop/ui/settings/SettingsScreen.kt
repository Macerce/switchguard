package com.macerce.switchguard.desktop.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.macerce.switchguard.desktop.AppGraph
import com.macerce.switchguard.desktop.i18n.Strings
import com.macerce.switchguard.desktop.platform.Autostart
import com.macerce.switchguard.desktop.platform.Links
import com.macerce.switchguard.desktop.syncAutostart
import kotlin.math.roundToInt

/** Sorgulama aralığı seçenekleri (sn). */
private val POLL_CHOICES = listOf(15, 30, 60, 120, 300)

/** Çevrimdışı tolerans seçenekleri (sn); 0 = kapalı. */
private val GRACE_CHOICES = listOf(0, 30, 60, 120, 300)

/** Sessiz saat seçenekleri: 15 dakikalık adımlar. */
private val QUIET_CHOICES = (0 until 24 * 60 step 15).toList()

/** Kurulu uygulamanın sürümü (jpackage); geliştirme çalıştırmasında "dev". */
internal fun appVersion(): String = System.getProperty("jpackage.app-version")?.takeIf { it.isNotBlank() } ?: "dev"

/** Motorun yalnızca başlarken okuduğu bir ayar değişti: izleme açıksa yeniden başlatılır (kapalıysa dokunulmaz). */
internal fun applyEngineSettings(graph: AppGraph) {
    if (graph.store.monitoringEnabled) graph.engine.restart()
}

private fun hhmm(minute: Int) = "%02d:%02d".format(minute / 60, minute % 60)

/**
 * Ayarlar: telefondaki gibi başlıklı bölümler, tek kaydırılabilir sayfa (`spec-2-ayarlar.md` §3). Her değişiklik
 * hemen `Store`'a yazılır; arayüz `store.version` ile yeniden çizilir.
 */
@Composable
fun SettingsScreen(graph: AppGraph, strings: Strings) {
    val store = graph.store
    val version by store.version.collectAsState()
    val s = remember(version) { Snapshot(graph) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {
        Column(Modifier.widthIn(max = 900.dp)) {
            // --- Hesaplar ---
            AccountsSection(graph, strings, version)
            SectionDivider()

            // --- İzleme ---
            SectionTitle(strings["section_monitoring"])
            ChoiceRow(
                strings["poll_interval"],
                options = POLL_CHOICES.map { it to strings.format("poll_interval_value", it) },
                selected = s.poll,
                onSelect = { store.pollIntervalSec = it },
            )
            ChoiceRow(
                strings["offline_grace"],
                options = GRACE_CHOICES.map { it to if (it == 0) strings["grace_off"] else strings.format("seconds_n", it) },
                selected = s.grace,
                onSelect = { store.offlineGraceSec = it },
            )
            SwitchRow(strings["desktop_keep_awake"], strings["desktop_keep_awake_desc"], s.awake) {
                store.alwaysAwake = it
                applyEngineSettings(graph)
            }
            SwitchRow(strings["desktop_autostart"], strings["desktop_autostart_desc"], s.autostart) {
                store.autostart = it
                syncAutostart(Autostart(), it)
            }
            SectionDivider()

            // --- Alarm ---
            SectionTitle(strings["section_alarm"])
            SoundRows(graph, strings, s.alarmSound, s.notifySound)
            SliderRow(
                value = s.volume,
                range = 0.05f..1f,
                tag = "alarm-volume",
                title = { strings.format("alarm_volume", (it * 100).roundToInt()) },
                onFinished = { store.alarmVolume = it },
            )
            SwitchRow(strings["quiet_hours"], strings["quiet_hours_desc"], s.quiet.enabled) {
                store.quietHours = store.quietHours.copy(enabled = it)
            }
            if (s.quiet.enabled) {
                ChoiceRow(
                    strings["quiet_start"],
                    options = QUIET_CHOICES.map { it to hhmm(it) },
                    selected = s.quiet.startMinute,
                    onSelect = { store.quietHours = store.quietHours.copy(startMinute = it) },
                )
                ChoiceRow(
                    strings["quiet_end"],
                    options = QUIET_CHOICES.map { it to hhmm(it) },
                    selected = s.quiet.endMinute,
                    onSelect = { store.quietHours = store.quietHours.copy(endMinute = it) },
                )
            }
            SettingRow(strings["test_alarm"], strings["test_alarm_desc"], onClick = { graph.engine.testAlarm() })
            SectionDivider()

            // --- Genel ---
            SectionTitle(strings["section_general"])
            ChoiceRow(
                strings["language"],
                options = listOf("" to strings["desktop_language_system"], "tr" to "Türkçe", "en" to "English"),
                selected = s.language,
                onSelect = { store.language = it },
            )
            ChoiceRow(
                strings["desktop_theme"],
                options = listOf(
                    "system" to strings["desktop_theme_system"],
                    "light" to strings["desktop_theme_light"],
                    "dark" to strings["desktop_theme_dark"],
                ),
                selected = s.theme,
                onSelect = { store.theme = it },
            )
            SectionDivider()

            // --- Hakkında ---
            SectionTitle(strings["section_about"])
            SettingRow(strings.format("version_x", appVersion()))
            SettingRow(strings["desktop_open_logs"], onClick = { Links.openFolder(graph.dataDir.resolve("logs")) })
        }
    }
}

/** Ekranın okuduğu ayarlar; `store.version` her değiştiğinde bir kez okunur. */
private class Snapshot(graph: AppGraph) {
    private val store = graph.store
    val poll = store.pollIntervalSec
    val grace = store.offlineGraceSec
    val awake = store.alwaysAwake
    val autostart = store.autostart
    val volume = store.alarmVolume
    val alarmSound = store.alarmSoundUri
    val notifySound = store.notifySoundUri
    val quiet = store.quietHours
    val language = store.language
    val theme = store.theme
}
