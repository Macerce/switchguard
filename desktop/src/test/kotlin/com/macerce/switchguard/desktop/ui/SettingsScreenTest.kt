package com.macerce.switchguard.desktop.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import com.macerce.switchguard.desktop.AppGraph
import com.macerce.switchguard.desktop.AppState
import com.macerce.switchguard.desktop.ui.settings.AlarmPage
import com.macerce.switchguard.desktop.ui.settings.SettingsPage
import com.macerce.switchguard.desktop.ui.settings.SettingsScreen
import com.macerce.switchguard.desktop.ui.settings.SummaryPage
import com.macerce.switchguard.desktop.ui.settings.WindowsPage
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SettingsScreenTest {
    @get:Rule val tmp = TemporaryFolder()
    @get:Rule val compose = createComposeRule()

    private lateinit var graph: AppGraph

    @Before
    fun setUp() {
        graph = AppGraph(tmp.root.toPath())
        graph.store.language = "en"
    }

    @After
    fun tearDown() = graph.shutdown()

    private fun show(page: SettingsPage? = null) = compose.setContent { SettingsScreen(graph, graph.strings(), startPage = page) }

    /** Sayfa doğrudan (motor/kayıt defteri yerine sahte işlevlerle) çizilir; metinler SettingsScreen'deki gibi sağlanır. */
    private fun showPage(content: @androidx.compose.runtime.Composable () -> Unit) = compose.setContent {
        CompositionLocalProvider(LocalStrings provides graph.strings()) { content() }
    }

    // ---------------------------------------------------------------- kategori listesi ve gezinme

    @Test
    fun homeListsTheCategoriesWithSummaries() {
        graph.store.reminderMin = 10
        show()

        for (title in listOf("Accounts", "Alarm", "Quiet hours", "Tell someone else", "Daily summary", "Monitoring", "Windows", "Backup & transfer", "Language & appearance", "Help & about")) {
            compose.onNodeWithText(title).performScrollTo()
        }
        compose.onNodeWithText("No account connected").performScrollTo()
        compose.onNodeWithText("English · System").performScrollTo()
    }

    @Test
    fun windowsRowWarnsWhenAutostartOrKeepAwakeIsOff() {
        graph.store.autostart = false
        graph.store.alwaysAwake = false
        show()

        compose.onNodeWithText("Doesn't start with Windows · The computer may go to sleep").performScrollTo()
    }

    @Test
    fun categoryOpensItsPageAndBackReturnsToTheList() {
        show()

        compose.onNodeWithText("Alarm").performClick()
        compose.onNodeWithText("Alarm sound").assertExists()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.waitForIdle()

        compose.onNodeWithText("Alarm sound").assertDoesNotExist()
        compose.onNodeWithText("Accounts").assertExists()
    }

    @Test
    fun clickingTheSettingsTabAgainReturnsToTheList() {
        show(SettingsPage.MONITORING)
        compose.onNodeWithText("Backup check interval").assertExists()

        graph.appState.tab.value = AppState.Tab.SETTINGS
        graph.appState.select(AppState.Tab.SETTINGS)
        compose.waitForIdle()

        compose.onNodeWithText("Backup check interval").assertDoesNotExist()
        compose.onNodeWithText("Accounts").assertExists()
    }

    @Test
    fun telegramOpenedFromTheAlarmPageGoesBackToAlarm() {
        show(SettingsPage.ALARM)

        compose.onNodeWithText("Tell someone else").performClick()
        compose.onNodeWithText("Check token").assertExists()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.waitForIdle()

        compose.onNodeWithText("Alarm sound").assertExists()
    }

    // ---------------------------------------------------------------- ayarlar

    @Test
    fun windowsPageWritesBothSwitches() {
        // Kayıt defteri yerine sahte: yalnızca istenen değer kaydedilir.
        val applied = mutableListOf<Boolean>()
        showPage { WindowsPage(graph, graph.strings(), applyAutostart = { applied += it }) }

        compose.onNodeWithText("Start with Windows").performClick()
        compose.onNodeWithText("Keep the computer awake").performClick()
        compose.waitForIdle()

        assertEquals(false, graph.store.autostart)
        assertEquals(listOf(false), applied)
        assertEquals(false, graph.store.alwaysAwake)
    }

    @Test
    fun pollIntervalChoiceIsSaved() {
        show(SettingsPage.MONITORING)

        compose.onNodeWithText("Backup check interval").performClick()
        compose.onNodeWithText("Every 60 s").performClick()
        compose.waitForIdle()

        assertEquals(60, graph.store.pollIntervalSec)
    }

    @Test
    fun blindAlertChoiceIsSaved() {
        show(SettingsPage.MONITORING)

        compose.onNodeWithText("Warn if monitoring stops").performClick()
        compose.onNodeWithText("30 min").performClick()
        compose.waitForIdle()

        assertEquals(30, graph.store.blindAlertMin)
    }

    @Test
    fun tuyaLiveIsOnlyOfferedWithTuya() {
        show(SettingsPage.MONITORING)

        compose.onNodeWithText("Tuya live updates on this computer").assertDoesNotExist()
    }

    @Test
    fun themeChoiceIsSaved() {
        show(SettingsPage.APPEARANCE)

        compose.onNodeWithText("Theme").performClick()
        compose.onNodeWithText("Dark").performClick()
        compose.waitForIdle()

        assertEquals("dark", graph.store.theme)
    }

    @Test
    fun languageChoiceIsSaved() {
        show(SettingsPage.APPEARANCE)

        compose.onNodeWithText("Language").performClick()
        compose.onNodeWithText("Türkçe").performClick()
        compose.waitForIdle()

        assertEquals("tr", graph.store.language)
    }

    @Test
    fun volumeSliderStaysWithinLimits() {
        show(SettingsPage.ALARM)

        compose.onNodeWithTag("alarm-volume").performScrollTo().performSemanticsAction(SemanticsActions.SetProgress) { it(0f) }
        compose.waitForIdle()

        assertEquals(0.05f, graph.store.alarmVolume)
    }

    @Test
    fun reminderChoiceIsSaved() {
        show(SettingsPage.ALARM)

        compose.onNodeWithText("Ring again if not resolved").performClick()
        compose.onNodeWithText("Every 30 min").performClick()
        compose.waitForIdle()

        assertEquals(30, graph.store.reminderMin)
        compose.onNodeWithText("Every 30 min").assertExists() // satırın özeti
    }

    @Test
    fun testAlarmDialogStartsASilentDelayedAlarm() {
        // Gerçek alarm çalmasın: motor yerine sahte.
        val started = mutableListOf<Pair<Boolean, Long>>()
        showPage { AlarmPage(graph, graph.strings(), open = {}, testAlarm = { silent, delay -> started += silent to delay }) }

        compose.onNodeWithText("Test alarm").performClick()
        compose.onNodeWithText("Silent (screen only)").performClick()
        compose.onNodeWithText("Ring in 10 seconds").performClick()
        compose.onNodeWithText("Start").performClick()
        compose.waitForIdle()

        assertEquals(listOf(true to 10_000L), started)
        compose.onNodeWithText("The test alarm rings in 10 seconds").assertExists()
    }

    @Test
    fun cancellingTheTestAlarmStartsNothing() {
        val started = mutableListOf<Pair<Boolean, Long>>()
        showPage { AlarmPage(graph, graph.strings(), open = {}, testAlarm = { silent, delay -> started += silent to delay }) }

        compose.onNodeWithText("Test alarm").performClick()
        compose.onNodeWithText("Cancel").performClick()
        compose.waitForIdle()

        assertTrue(started.isEmpty())
    }

    @Test
    fun quietHoursStartIsPickedWithTheArrows() {
        show(SettingsPage.QUIET)

        compose.onNode(hasText("Quiet hours") and isToggleable()).performClick()
        compose.onNodeWithText("Starts").performClick()
        // Varsayılan 23:00; saatin altındaki ok bir saat geri alır.
        compose.onAllNodesWithContentDescription("Decrease")[0].performClick()
        compose.onNodeWithText("Save").performClick()
        compose.waitForIdle()

        assertEquals(true, graph.store.quietHours.enabled)
        assertEquals(22 * 60, graph.store.quietHours.startMinute)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun quietHoursEndCanBeTyped() {
        graph.store.quietHours = graph.store.quietHours.copy(enabled = true)
        show(SettingsPage.QUIET)

        compose.onNodeWithText("Ends").performClick()
        compose.onNodeWithTag("time-hour").performClick().performKeyInput { pressKey(Key.Zero); pressKey(Key.Six) }
        compose.onNodeWithTag("time-minute").performClick().performKeyInput { pressKey(Key.Three); pressKey(Key.Zero); pressKey(Key.Enter) }
        compose.waitForIdle()

        assertEquals(6 * 60 + 30, graph.store.quietHours.endMinute)
    }

    @Test
    fun dailySummarySwitchAndPreview() {
        var previews = 0
        var lines: List<String>? = null
        showPage { SummaryPage(graph.store, graph.strings(), preview = { previews++; it(lines) }) }

        compose.onNode(hasText("Daily summary notification") and isToggleable()).performClick()
        compose.onNodeWithText("Show today’s summary now").performClick()
        compose.waitForIdle()

        assertEquals(true, graph.store.dailySummaryEnabled)
        assertEquals(1, previews)
        compose.onNodeWithText("No data recorded yet. Data is collected while monitoring is on.").assertExists()
        compose.onNodeWithTag("summary-close").performClick()
        compose.waitForIdle()

        // Özet satırları pencerede görünür (telefondaki gibi; bildirim değil).
        lines = listOf("Pump: turned on 2×, on for 10 min")
        compose.onNodeWithText("Show today’s summary now").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Today’s summary").assertExists()
        compose.onNodeWithText("Pump: turned on 2×, on for 10 min").assertExists()
    }
}
