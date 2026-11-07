package com.macerce.switchguard.desktop.ui

import com.macerce.switchguard.desktop.AppGraph
import com.macerce.switchguard.desktop.AppState
import com.macerce.switchguard.desktop.i18n.Strings
import java.nio.file.Files
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Yeni dillerden ikisinde (Almanca uzun sözcükler, Arapça sağdan sola) cihazlar ve ayarlar sekmesi:
 * `build/snapshots/lang-*.png`. Karşılaştırma yapmaz, gözle bakmak içindir.
 */
class LanguageSnapshotTest {
    @get:Rule val tmp = TemporaryFolder()

    private var graph: AppGraph? = null

    @After
    fun tearDown() { graph?.shutdown() }

    @Test
    fun germanAndArabic() {
        for (language in listOf("de", "ar")) {
            graph?.shutdown()
            val g = AppGraph(tmp.newFolder(language).toPath())
            graph = g
            val strings = Strings.load(language)
            g.store.language = language
            g.store.enableDemo(strings.demoNames())
            for (tab in listOf(AppState.Tab.DEVICES, AppState.Tab.SETTINGS)) {
                g.appState.select(tab)
                val out = Snapshots.render("lang-$language-${tab.name.lowercase()}", 1280, 800, language = language) {
                    AppNav(g, strings)
                }
                assertTrue(Files.size(out) > 1000)
            }
        }
    }
}
