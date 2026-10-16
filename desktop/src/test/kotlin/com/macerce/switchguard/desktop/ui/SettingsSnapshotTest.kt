package com.macerce.switchguard.desktop.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.macerce.switchguard.core.Recipient
import com.macerce.switchguard.core.QuietHours
import com.macerce.switchguard.desktop.AppGraph
import com.macerce.switchguard.desktop.AppState
import com.macerce.switchguard.desktop.ui.settings.ChoiceDialog
import com.macerce.switchguard.desktop.ui.settings.SettingsPage
import com.macerce.switchguard.desktop.ui.settings.SettingsScreen
import com.macerce.switchguard.desktop.ui.settings.TestAlarmDialog
import com.macerce.switchguard.desktop.ui.settings.TimeDialog
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files
import java.util.Locale

/**
 * Ayarların görsel örnekleri (açık + koyu, Türkçe): kategori listesi, her alt sayfa ve pencereler;
 * `build/snapshots/settings-*.png`. Karşılaştırma yapmaz, yalnızca çizilebildiğini denetler.
 */
class SettingsSnapshotTest {
    @get:Rule val tmp = TemporaryFolder()

    private lateinit var graph: AppGraph

    @Before
    fun setUp() {
        graph = AppGraph(tmp.root.toPath())
        graph.store.apply {
            language = "tr"
            appId = "a1b2c3d4e5f6"
            appSecret = "secret"
            accessToken = "token"
            accountEmail = "macerce@gmail.com"
            region = "eu"
            tuyaAccessId = "tid"
            tuyaSecret = "tsecret"
            quietHours = QuietHours(true, 23 * 60, 7 * 60)
            dailySummaryEnabled = true
            reminderMin = 10
            telegramToken = "123456789:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
            telegramBot = "evim_alarm_bot"
            telegramRecipients = listOf(Recipient(1, "Ayşe"), Recipient(-2, "Aile"))
            escalateMin = 5
            autostart = false
        }
    }

    @After
    fun tearDown() = graph.shutdown()

    private fun shot(name: String, dark: Boolean, height: Int = 760, content: @androidx.compose.runtime.Composable () -> Unit) {
        val out = Snapshots.render("settings-$name-" + if (dark) "dark" else "light", width = 1020, height = height, dark = dark, content = content)
        assertTrue(Files.size(out) > 1000)
    }

    @Test
    fun homeAndPages() {
        val strings = graph.strings()
        for (dark in listOf(false, true)) {
            shot("home", dark) { SettingsScreen(graph, strings) }
            for (page in SettingsPage.entries) {
                shot(page.name.lowercase(), dark) { SettingsScreen(graph, strings, startPage = page) }
            }
            // Uzun sayfaların tamamı (kaydırmadan).
            shot("home-full", dark, height = 1280) { SettingsScreen(graph, strings) }
            shot("telegram-full", dark, height = 1300) { SettingsScreen(graph, strings, startPage = SettingsPage.TELEGRAM) }
            shot("about-full", dark, height = 1300) { SettingsScreen(graph, strings, startPage = SettingsPage.ABOUT) }
        }
    }

    @Test
    fun appNavWithSettings() {
        graph.appState.tab.value = AppState.Tab.SETTINGS
        for (dark in listOf(false, true)) {
            val out = Snapshots.render("settings-appnav-" + if (dark) "dark" else "light", dark = dark) { AppNav(graph, graph.strings()) }
            assertTrue(Files.size(out) > 1000)
        }
    }

    @Test
    fun dialogs() {
        for (dark in listOf(false, true)) {
            shot("dialog-test-alarm", dark) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { TestAlarmDialog(onStart = { _, _ -> }, onDismiss = {}) }
            }
            shot("dialog-time", dark) {
                Box(Modifier.fillMaxSize()) { TimeDialog("Başlangıç", 23 * 60, Locale.forLanguageTag("tr"), onPick = {}, onDismiss = {}) }
            }
            shot("dialog-time-en", dark) {
                Box(Modifier.fillMaxSize()) { TimeDialog("Starts", 21 * 60 + 30, Locale.forLanguageTag("en"), onPick = {}, onDismiss = {}) }
            }
            shot("dialog-choice", dark) {
                Box(Modifier.fillMaxSize()) {
                    ChoiceDialog("Düzelmezse yeniden çal", str("reminder_desc"), listOf(0, 10, 30, 60), 10, { if (it == 0) "Kapalı" else "$it dakikada bir" }, onPick = {}, onDismiss = {})
                }
            }
        }
    }
}
