package com.macerce.switchguard.desktop.i18n

import com.macerce.switchguard.core.EventArgs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory

class StringsTest {
    @Test
    fun unescapesAndroidEscapes() {
        assertEquals("Smart Life'ta", Strings.unescape("Smart Life\\'ta"))
        assertEquals("a\nb", Strings.unescape("a\\nb"))
        assertEquals("\"x\"", Strings.unescape("\\\"x\\\""))
        assertEquals("  x ", Strings.unescape("\"  x \""))
        assertEquals("a\tb", Strings.unescape("a\\tb"))
        assertEquals("a\\b", Strings.unescape("a\\\\b"))
    }

    @Test
    fun turkishTableLoaded() {
        assertEquals("ÇEVRİMİÇİ", Strings.load("tr")["label_online"])
        assertEquals("tr", Strings.load("tr").language)
    }

    @Test
    fun missingTrKeyFallsBackToEnglish() {
        assertEquals("SwitchGuard", Strings.load("tr")["app_name"])
    }

    @Test
    fun missingKeyEverywhereReturnsTheKeyItself() {
        assertEquals("no_such_key_anywhere", Strings.load("en")["no_such_key_anywhere"])
        assertEquals("no_such_key_anywhere", Strings.load("tr")["no_such_key_anywhere"])
    }

    @Test
    fun formatsPositionalArgs() {
        val text = Strings.load("en").format("status_polling", 3, 30)
        assertTrue(text, text.contains("3"))
        assertTrue(text, text.contains("30"))
        assertEquals("Watching 3 device(s) · every 30 s", text)
    }

    @Test
    fun desktopKeysAreMerged() {
        assertEquals("Open SwitchGuard", Strings.load("en")["tray_open"])
        assertEquals("SwitchGuard'ı aç", Strings.load("tr")["tray_open"])
        assertEquals(
            "Bilgisayar 7 dk uykudaydı — bu sürede kısa olaylar kaçmış olabilir.",
            Strings.load("tr").format("desktop_slept", 7),
        )
    }

    @Test
    fun everyEnglishKeyHasTurkish() {
        for (file in listOf("strings.xml", "desktop.xml")) {
            val english = translatableKeys("/strings/values/$file")
            val turkish = Strings.parse(resource("/strings/values-tr/$file")).keys
            val missing = english - turkish
            assertTrue("$file: no Turkish text for $missing", missing.isEmpty())
        }
    }

    @Test
    fun loadedTextsHaveNoLeftoverEscapes() {
        for (dir in listOf("values", "values-tr")) for (file in listOf("strings.xml", "desktop.xml")) {
            for ((key, text) in Strings.parse(resource("/strings/$dir/$file"))) {
                assertTrue("$dir/$file $key: $text", !text.contains('\\') && !text.contains("&amp;"))
            }
        }
        assertEquals("Contact & feedback", Strings.load("en")["contact"])
    }

    @Test
    fun desktopKeysNeverShadowPhoneKeys() {
        for (dir in listOf("values", "values-tr")) {
            val phone = Strings.parse(resource("/strings/$dir/strings.xml")).keys
            val desktop = Strings.parse(resource("/strings/$dir/desktop.xml")).keys
            assertTrue("$dir: desktop.xml redefines ${phone intersect desktop}", (phone intersect desktop).isEmpty())
        }
    }

    @Test
    fun eventStringsRenderInBothLanguages() {
        val tr = EventArgs.Test.render("x", Strings.load("tr").eventStrings())
        val en = EventArgs.Test.render("x", Strings.load("en").eventStrings())
        assertNotEquals(en, tr)
        assertEquals(Strings.load("tr")["test_alarm_line"], tr)
    }

    @Test
    fun eventStringsUseTheSameKeysAsThePhone() {
        val s = Strings.load("en")
        val e = s.eventStrings()
        assertEquals("ON", e.labels.on)
        assertEquals("OFF", e.labels.off)
        assertEquals("ONLINE", e.labels.online)
        assertEquals("OFFLINE", e.labels.offline)
        assertEquals(s.format("label_channel", "Pump", 2), e.labels.channel("Pump", 2))
        assertEquals(s.format("event_short_drop", "Pump"), e.shortDrop("Pump"))
        assertEquals(s["test_alarm_line"], e.test)
        assertEquals(s.format("automation_fired", "Night"), e.automationFired("Night"))
        assertEquals(s["device_missing"], e.deviceMissing)
        assertEquals(s.format("automation_failed", "text", "boom"), e.automationFailed("text", "boom"))
        assertEquals(s.format("power_data_stale", "Pump"), e.powerStale("Pump"))
    }

    @Test
    fun demoNamesComeFromTheLanguageTable() {
        val tr = Strings.load("tr")
        val n = tr.demoNames()
        assertEquals(tr["demo_freezer"], n.freezer)
        assertEquals(tr["demo_pumps"], n.pumps)
        assertEquals(tr["demo_light"], n.light)
        assertEquals(tr["demo_auto_pump"], n.stalledPump)
        assertEquals(tr["demo_auto_light"], n.lightLeftOn)
        assertNotEquals(Strings.load("en").demoNames(), n)
    }

    @Test
    fun emptyLanguageFollowsTheSystemLocale() {
        val saved = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"))
            assertEquals("tr", Strings.load("").language)
            Locale.setDefault(Locale.forLanguageTag("de-DE"))
            assertEquals("en", Strings.load("").language)
            assertEquals("en", Strings.load("xx").language)
        } finally {
            Locale.setDefault(saved)
        }
    }

    private fun resource(path: String) =
        checkNotNull(StringsTest::class.java.getResourceAsStream(path)) { "missing resource $path" }

    /** `translatable="false"` olmayan `<string>` anahtarları. */
    private fun translatableKeys(path: String): Set<String> {
        val nodes = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(resource(path)).getElementsByTagName("string")
        return (0 until nodes.length)
            .map { nodes.item(it) as Element }
            .filter { it.getAttribute("translatable") != "false" }
            .map { it.getAttribute("name") }
            .toSet()
    }
}
