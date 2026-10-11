package com.macerce.switchguard.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadinessTest {
    private val ok = ReadinessInput(
        notificationsEnabled = true,
        alarmChannelHigh = true,
        dndTotalSilence = false,
        alarmVolume = 5,
        alarmVolumeMax = 7,
        alarmBoost = false,
        batteryExempt = true,
        fullScreenAllowed = true,
        overlayAllowed = null,
        autostartConfirmed = null,
    )

    @Test
    fun `her sey tamamsa sorun yok`() = assertTrue(Readiness.issues(ok).isEmpty())

    @Test
    fun `bildirim kapaliysa kanal ayrica sayilmaz`() =
        assertEquals(listOf(ReadinessIssue.NOTIFICATIONS_OFF), Readiness.issues(ok.copy(notificationsEnabled = false, alarmChannelHigh = false)))

    @Test
    fun `alarm kanali kisilmis`() =
        assertEquals(listOf(ReadinessIssue.ALARM_CHANNEL_MUTED), Readiness.issues(ok.copy(alarmChannelHigh = false)))

    @Test
    fun `alarm sesi sifir ya da cok kisiksa ve yukseltme kapaliysa sorun`() {
        assertEquals(listOf(ReadinessIssue.ALARM_VOLUME_LOW), Readiness.issues(ok.copy(alarmVolume = 0)))
        // Yeni Android'lerde alarm sesi 0 olamaz; en düşük seviye (1/7) de neredeyse duyulmaz.
        assertEquals(listOf(ReadinessIssue.ALARM_VOLUME_LOW), Readiness.issues(ok.copy(alarmVolume = 1)))
        assertTrue(Readiness.issues(ok.copy(alarmVolume = 2)).isEmpty())
        assertTrue(Readiness.issues(ok.copy(alarmVolume = 1, alarmBoost = true)).isEmpty())
    }

    @Test
    fun `uygulanmayan izinler sorun sayilmaz`() =
        assertTrue(Readiness.issues(ok.copy(fullScreenAllowed = null, overlayAllowed = null, autostartConfirmed = null)).isEmpty())

    @Test
    fun `sorunlar onem sirasiyla`() {
        val all = ReadinessInput(
            notificationsEnabled = true, alarmChannelHigh = false, dndTotalSilence = true, alarmVolume = 0, alarmVolumeMax = 7, alarmBoost = false,
            batteryExempt = false, fullScreenAllowed = false, overlayAllowed = false, autostartConfirmed = false,
        )
        assertEquals(
            listOf(
                ReadinessIssue.ALARM_CHANNEL_MUTED, ReadinessIssue.DND_TOTAL_SILENCE, ReadinessIssue.ALARM_VOLUME_LOW,
                ReadinessIssue.BATTERY_RESTRICTED, ReadinessIssue.FULL_SCREEN_DENIED, ReadinessIssue.OVERLAY_DENIED,
                ReadinessIssue.AUTOSTART_UNCONFIRMED,
            ),
            Readiness.issues(all),
        )
    }
}
