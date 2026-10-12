package com.macerce.switchguard

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.media.AudioManager
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.macerce.switchguard.data.Store
import com.macerce.switchguard.service.Notifier
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Alarm bildiriminin telefondaki davranışı: kendi özetli grubunda (Android 16 zorla gruplamasın), tam ekran
 * amacı taşıyor, alarm sesi en yükseğe çıkıp kapatınca eski seviyeye dönüyor.
 */
@RunWith(AndroidJUnit4::class)
class AlarmNotificationDeviceTest {
    @get:Rule
    val notifications: GrantPermissionRule =
        if (Build.VERSION.SDK_INT >= 33) GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS) else GrantPermissionRule.grant()

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val store = Store.get(context)
    private val audio = context.getSystemService(AudioManager::class.java)
    private val nm = context.getSystemService(NotificationManager::class.java)
    private lateinit var notifier: Notifier
    private var volumeBefore = 0
    private var boostBefore = true

    @Before
    fun setUp() {
        notifier = Notifier(context, store)
        volumeBefore = audio.getStreamVolume(AudioManager.STREAM_ALARM)
        boostBefore = store.alarmBoost
    }

    @After
    fun tearDown() {
        notifier.stopAlarm()
        store.alarmBoost = boostBefore
        audio.setStreamVolume(AudioManager.STREAM_ALARM, volumeBefore, 0)
    }

    @Test
    fun alarmHasItsOwnSummarizedGroupAndAFullScreenIntent() {
        notifier.showAlarm(listOf("12:00  Test"))
        val active = nm.activeNotifications
        val alarm = active.first { it.id == Notifier.ID_ALARM }.notification
        assertEquals(Notifier.GROUP_ALARM, alarm.group)
        assertNotNull(alarm.fullScreenIntent)
        assertTrue(active.any { it.id == Notifier.ID_ALARM_SUMMARY && it.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0 })
        notifier.stopAlarm()
        assertTrue(nm.activeNotifications.none { it.id == Notifier.ID_ALARM || it.id == Notifier.ID_ALARM_SUMMARY })
    }

    @Test
    fun alarmVolumeIsRaisedWhileRingingAndRestoredAfter() {
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        val min = if (Build.VERSION.SDK_INT >= 28) audio.getStreamMinVolume(AudioManager.STREAM_ALARM) else 0
        val low = min.coerceAtLeast(1)
        audio.setStreamVolume(AudioManager.STREAM_ALARM, low, 0)
        store.alarmBoost = true
        notifier.showAlarm(listOf("12:00  Test"))
        assertEquals(max, audio.getStreamVolume(AudioManager.STREAM_ALARM))
        notifier.stopAlarm()
        assertEquals(low, audio.getStreamVolume(AudioManager.STREAM_ALARM))
    }
}
