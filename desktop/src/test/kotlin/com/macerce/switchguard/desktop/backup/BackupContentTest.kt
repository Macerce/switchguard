package com.macerce.switchguard.desktop.backup

import com.macerce.switchguard.core.DeviceRules
import com.macerce.switchguard.core.DeviceSnapshot
import com.macerce.switchguard.data.Store
import com.macerce.switchguard.desktop.data.PlainCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files
import java.nio.file.Path
import java.util.Base64

class BackupContentTest {
    @get:Rule val tmp = TemporaryFolder()

    private lateinit var store: Store
    private lateinit var userDir: Path

    @Before
    fun setUp() {
        store = Store(tmp.root.toPath().resolve("settings.json"), PlainCodec)
        userDir = tmp.newFolder("sounds").toPath()
    }

    private fun sound(name: String, bytes: ByteArray = byteArrayOf(1, 2, 3)): String {
        Files.write(userDir.resolve(name), bytes)
        return "user:$name"
    }

    @Test
    fun containsKeysAndSecretsButNoSession() {
        store.tuyaAccessId = "tid"
        store.tuyaSecret = "tsecret"
        store.appId = "app"
        store.appSecret = "asecret"
        store.accessToken = "token"
        store.refreshToken = "refresh"
        store.tuyaToken = "ttoken"
        store.snapshots = listOf(DeviceSnapshot("d1", "Pompa", online = true, switches = mapOf(0 to true)))
        store.pendingAlarm = listOf("12:00  Pompa")
        store.monitoringEnabled = true

        val settings = BackupContent.build(store, userDir, "0.2.0", "2026-10-07T12:00:00Z").getJSONObject("settings")

        assertEquals("tsecret", settings.getString("tuyaSecret"))
        assertEquals("asecret", settings.getString("appSecret"))
        assertEquals(true, settings.getBoolean("monitoring"))
        for (k in listOf("at", "rt", "tuyaToken", "snapshots", "pendingAlarm", "apikey", "email")) assertFalse(k, settings.has(k))
    }

    @Test
    fun embedsUserSoundsReferencedAnywhere() {
        store.alarmSoundUri = sound("aaaaaaaaaaaa-siren.wav", byteArrayOf(9, 8, 7))
        store.notifySoundUri = sound("bbbbbbbbbbbb-zil.wav")
        store.setRules("d1", DeviceRules(soundUri = sound("cccccccccccc-pompa.wav")))
        sound("dddddddddddd-kullanilmayan.wav")
        store.setRules("d2", DeviceRules(soundUri = "builtin:alarm_beep"))

        val sounds = BackupContent.build(store, userDir, "0.2.0", "x").getJSONObject("sounds")

        assertEquals(setOf("user:aaaaaaaaaaaa-siren.wav", "user:bbbbbbbbbbbb-zil.wav", "user:cccccccccccc-pompa.wav"), sounds.keySet())
        assertEquals(listOf<Byte>(9, 8, 7), Base64.getDecoder().decode(sounds.getString("user:aaaaaaaaaaaa-siren.wav")).toList())
    }

    @Test
    fun summaryCounts() {
        store.tuyaAccessId = "tid"
        store.setRules("d1", DeviceRules())
        store.setRules("d2", DeviceRules(monitored = false))
        store.alarmSoundUri = sound("aaaaaaaaaaaa-siren.wav")
        val content = BackupContent.build(store, userDir, "0.2.0", "2026-10-07T12:00:00Z")

        val s = BackupContent.summary(content)

        assertEquals(Summary("2026-10-07T12:00:00Z", "0.2.0", tuya = true, ewelinkKeys = false, rules = 2, automations = 0, sounds = 1), s)
    }

    @Test
    fun clearedKeysAndBackupKeysDoNotOverlap() {
        assertTrue((BackupContent.KEYS intersect BackupContent.CLEARED).isEmpty())
        assertTrue("monitoring" in BackupContent.KEYS && "notifySound" in BackupContent.KEYS)
    }
}
