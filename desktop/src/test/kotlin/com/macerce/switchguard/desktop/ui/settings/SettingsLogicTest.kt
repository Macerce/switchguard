package com.macerce.switchguard.desktop.ui.settings

import com.macerce.switchguard.core.Recipient
import com.macerce.switchguard.data.Store
import com.macerce.switchguard.desktop.data.SecretCodec
import com.macerce.switchguard.desktop.i18n.Strings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.util.Locale

/** Ayarların arayüzsüz mantığı: saat adımları, saat biçimi, kategori özetleri. */
class SettingsLogicTest {
    @get:Rule val tmp = TemporaryFolder()

    private val en = Strings.load("en")
    private val tr = Strings.load("tr")

    private fun store(): Store = Store(tmp.root.toPath().resolve("settings.json"), PlainCodec)

    @Test
    fun hourAndMinuteWrapIndependently() {
        assertEquals(0 * 60 + 15, TimeStep.hour(23 * 60 + 15, +1))
        assertEquals(23 * 60 + 15, TimeStep.hour(0 * 60 + 15, -1))
        assertEquals(7 * 60, TimeStep.minute(7 * 60 + 59, +1))
        assertEquals(7 * 60 + 59, TimeStep.minute(7 * 60, -1))
    }

    @Test
    fun typedDigitsCombineWhenValid() {
        assertEquals(2, TimeStep.typed(null, 2, 0..23))
        assertEquals(23, TimeStep.typed(2, 3, 0..23))
        assertEquals(5, TimeStep.typed(7, 5, 0..23)) // 75 olmaz: yeni giriş
        assertEquals(59, TimeStep.typed(5, 9, 0..59))
        assertNull(TimeStep.typed(null, 0, 1..12))
        assertEquals(10, TimeStep.typed(1, 0, 1..12))
    }

    @Test
    fun settingHourOrMinuteKeepsTheOther() {
        assertEquals(6 * 60 + 45, TimeStep.withHour(23 * 60 + 45, 6))
        assertEquals(23 * 60 + 5, TimeStep.withMinute(23 * 60 + 45, 5))
        // 12 saat düzeni: ÖÖ/ÖS korunur.
        assertEquals(21 * 60 + 30, TimeStep.withHour12(14 * 60 + 30, 9))
        assertEquals(0 * 60 + 30, TimeStep.withHour12(5 * 60 + 30, 12))
        assertEquals(12 * 60, TimeStep.withHour12(13 * 60, 12))
    }

    @Test
    fun timeFormatFollowsTheLanguage() {
        assertTrue(is24Hour(Locale.forLanguageTag("tr")))
        assertFalse(is24Hour(Locale.forLanguageTag("en")))
        assertEquals("07:05", formatMinute(tr, 7 * 60 + 5))
        assertTrue(formatMinute(en, 21 * 60).startsWith("9:00"))
    }

    @Test
    fun homeSummariesDescribeTheSettings() {
        val s = store().apply {
            tuyaAccessId = "id"
            tuyaSecret = "secret"
            demoMode = true
            quietHours = quietHours.copy(enabled = true, startMinute = 22 * 60, endMinute = 6 * 60 + 30)
            offlineGraceSec = 120
            pollIntervalSec = 60
            language = "tr"
            theme = "dark"
        }
        val h = HomeSummaries(s, tr)

        assertEquals("Tuya · Demo", h.accounts)
        assertNull(h.accountsWarning)
        val a = AlertSettingsSummaries(s, tr)
        assertEquals("22:00 – 06:30", a.quiet)
        assertEquals("Kapalı", a.telegram)
        assertEquals("Kapalı", h.summary)
        assertEquals("Kopunca 2 dakika bekler · 60 sn'de bir", h.monitoring)
        assertNull(h.windowsWarning)
        assertEquals("Türkçe · Koyu", h.appearance)
    }

    @Test
    fun homeSummariesWarnAboutProblems() {
        val s = store().apply {
            sessionLost = true
            autostart = false
            telegramToken = "123456789:" + "A".repeat(35)
            telegramRecipients = listOf(Recipient(1, "A"), Recipient(2, "B"))
            escalateMin = 5
            dailySummaryEnabled = true
        }
        val h = HomeSummaries(s, en)

        assertEquals("Monitoring stopped — signed out", h.accountsWarning)
        assertEquals("Doesn't start with Windows", h.windowsWarning)
        assertEquals("No account connected", h.accounts)
        val telegram = AlertSettingsSummaries(s, en).telegram
        assertTrue(telegram, telegram.contains("5 min") && telegram.contains("2 people"))
        assertTrue(h.summary, h.summary.contains("9:00"))
    }

    @Test
    fun labelsMatchThePhone() {
        assertEquals("Off", reminderLabel(en, 0))
        assertEquals("Every 30 min", reminderLabel(en, 30))
        assertEquals("15 min", blindLabel(en, 15))
        assertEquals("Off (alert immediately)", graceLabel(en, 0))
        assertEquals("30 seconds", graceLabel(en, 30))
        assertEquals("2 minutes", graceLabel(en, 120))
        assertEquals("English", languageLabel(tr, "en"))
        assertEquals("Sistem dili", languageLabel(tr, ""))
    }

    /** Şifresiz kodlayıcı: DPAPI'ye gerek yok. */
    private object PlainCodec : SecretCodec {
        override fun encode(plain: String) = plain
        override fun decode(stored: String): String? = stored
    }
}
