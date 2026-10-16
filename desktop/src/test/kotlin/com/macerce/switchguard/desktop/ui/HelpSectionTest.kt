package com.macerce.switchguard.desktop.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.macerce.switchguard.desktop.AppGraph
import com.macerce.switchguard.desktop.ui.settings.SettingsPage
import com.macerce.switchguard.desktop.ui.settings.SettingsScreen
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class HelpSectionTest {
    @get:Rule val tmp = TemporaryFolder()
    @get:Rule val compose = createComposeRule()

    private lateinit var graph: AppGraph

    @Before
    fun setUp() {
        graph = AppGraph(tmp.root.toPath())
        graph.store.language = "tr"
        compose.setContent { SettingsScreen(graph, graph.strings(), startPage = SettingsPage.ABOUT) }
    }

    @After
    fun tearDown() = graph.shutdown()

    @Test
    fun settingsHaveTheThreeSetupGuides() {
        compose.onNodeWithText("Kurulum anlatımları").performScrollTo()
        compose.onNodeWithText("Windows'a kurulum").performScrollTo()
        compose.onNodeWithText("Tuya / Smart Life kurulumu").performScrollTo()
        compose.onNodeWithText("eWeLink kurulumu").performScrollTo()
    }

    @Test
    fun windowsGuideOpensToItsSteps() {
        compose.onNodeWithText("Windows'a kurulum").performScrollTo().performClick()

        compose.onNodeWithText("Ek bilgi → Yine de çalıştır", substring = true).performScrollTo()
        compose.onNodeWithText("Yüklü uygulamalar → SwitchGuard → Kaldır", substring = true).performScrollTo()
        compose.onNodeWithText("önce eski sürümü kaldırın", substring = true).performScrollTo()
    }

    @Test
    fun aboutPageHasGuidesContactAndVersion() {
        compose.onNodeWithText("eWeLink kurulum rehberi").performScrollTo()
        compose.onNodeWithText("Tuya / Smart Life kurulum rehberi").performScrollTo()
        compose.onNodeWithText("switchguardapp@gmail.com").performScrollTo()
        compose.onNodeWithText("Gizlilik politikası").performScrollTo()
        compose.onNodeWithText("Sürüm dev").performScrollTo()
    }
}
