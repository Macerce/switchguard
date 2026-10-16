package com.macerce.switchguard.desktop.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.macerce.switchguard.api.TelegramException
import com.macerce.switchguard.core.Recipient
import com.macerce.switchguard.desktop.AppGraph
import com.macerce.switchguard.desktop.ui.settings.TelegramApi
import com.macerce.switchguard.desktop.ui.settings.TelegramPage
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** "İkinci kişiye haber ver" sayfası; Telegram yerine sahte istemci (gerçek isteğe gitmez). */
class TelegramPageTest {
    @get:Rule val tmp = TemporaryFolder()
    @get:Rule val compose = createComposeRule()

    private lateinit var graph: AppGraph
    private val fake = FakeTelegram()
    private var done = 0

    private val token = "123456789:" + "A".repeat(35)

    @Before
    fun setUp() {
        graph = AppGraph(tmp.root.toPath())
        graph.store.language = "en"
    }

    @After
    fun tearDown() = graph.shutdown()

    private fun show() = compose.setContent {
        CompositionLocalProvider(LocalStrings provides graph.strings()) {
            Column(Modifier.verticalScroll(rememberScrollState())) { TelegramPage(graph.store, onDone = { done++ }, api = fake) }
        }
    }

    private fun waitFor(text: String) =
        compose.waitUntil(10_000) { compose.onAllNodes(hasText(text, substring = true)).fetchSemanticsNodes().isNotEmpty() }

    @Test
    fun fullSetupIsSavedOnlyOnSave() {
        fake.chats = listOf(Recipient(42, "Ayşe"))
        show()

        compose.onNodeWithText("Find people").assertIsNotEnabled()
        compose.onNodeWithText("Bot token").performTextInput(token)
        compose.onNodeWithText("Check token").performClick()
        waitFor("Bot ready: @home_bot")

        compose.onNodeWithText("Find people").performClick()
        waitFor("Found 1 chat")
        compose.onNodeWithText("Ayşe").assertExists() // tek kişi kendiliğinden seçilir
        compose.onNodeWithText("10 min").performScrollTo().performClick()
        assertEquals(0, graph.store.escalateMin) // henüz kaydedilmedi

        compose.onNodeWithText("Save").performScrollTo().assertIsEnabled().performClick()
        compose.waitForIdle()

        assertEquals(token, graph.store.telegramToken)
        assertEquals("home_bot", graph.store.telegramBot)
        assertEquals(listOf(Recipient(42, "Ayşe")), graph.store.telegramRecipients)
        assertEquals(10, graph.store.escalateMin)
        assertTrue(graph.store.escalationReady)
        assertEquals(1, done)
    }

    @Test
    fun testMessageGoesToTheSelectedPeople() {
        graph.store.telegramToken = token
        graph.store.telegramBot = "home_bot"
        graph.store.telegramRecipients = listOf(Recipient(1, "Ayşe"), Recipient(-2, "Aile"))
        graph.store.escalateMin = 5
        show()

        compose.onNodeWithText("Aile").performScrollTo().performClick() // seçimi kaldır
        compose.onNodeWithText("Send a test message").performScrollTo().performClick()
        waitFor("Test message sent")

        assertEquals(listOf(1L), fake.sent.map { it.first })
        assertTrue(fake.sent.single().second.contains("5 min"))
    }

    @Test
    fun invalidTokenIsExplained() {
        fake.failWith = TelegramException(401, "Unauthorized")
        show()

        compose.onNodeWithText("Bot token").performTextInput(token)
        compose.onNodeWithText("Check token").performClick()
        waitFor("The bot token is invalid")

        compose.onNodeWithText("Find people").assertIsNotEnabled()
    }

    @Test
    fun turningOffClearsTheSetup() {
        graph.store.telegramToken = token
        graph.store.telegramBot = "home_bot"
        graph.store.telegramRecipients = listOf(Recipient(1, "Ayşe"))
        graph.store.escalateMin = 5
        show()

        compose.onNodeWithText("Turn off").performScrollTo().performClick()
        compose.waitForIdle()

        assertFalse(graph.store.escalationReady)
        assertEquals(0, graph.store.escalateMin)
        assertEquals("", graph.store.telegramToken)
        assertEquals(1, done)
    }

    @Test
    fun cancelLeavesTheStoreAlone() {
        show()

        compose.onNodeWithText("Bot token").performTextInput(token)
        compose.onNodeWithText("Turn off").assertDoesNotExist() // kapalıyken yok
        compose.onNodeWithText("Cancel").performScrollTo().performClick()
        compose.waitForIdle()

        assertEquals("", graph.store.telegramToken)
        assertEquals(1, done)
    }

    private class FakeTelegram : TelegramApi {
        var chats: List<Recipient> = emptyList()
        var failWith: Exception? = null
        val sent = mutableListOf<Pair<Long, String>>()

        override fun botUsername(token: String): String {
            failWith?.let { throw it }
            return "home_bot"
        }

        override fun findChats(token: String): List<Recipient> = chats

        override fun send(token: String, chatId: Long, text: String) {
            synchronized(sent) { sent += chatId to text }
        }
    }
}
