package com.macerce.switchguard.desktop.backup

import com.macerce.switchguard.core.AlertAction
import com.macerce.switchguard.core.DeviceRules
import com.macerce.switchguard.core.EventType
import com.macerce.switchguard.data.Store
import com.macerce.switchguard.desktop.data.PlainCodec
import com.macerce.switchguard.desktop.data.SecretCodec
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.RandomAccessFile
import java.nio.file.Files
import java.nio.file.Path

class BackupServiceTest {
    @get:Rule val tmp = TemporaryFolder()

    /** Başka bir bilgisayarın DPAPI'si gibi: aynı değeri farklı kodlar. */
    private object OtherPcCodec : SecretCodec {
        override fun encode(plain: String) = "pc2:$plain"
        override fun decode(stored: String): String? = stored.removePrefix("pc2:")
    }

    /** Bir "bilgisayar": kendi veri klasörü, ayarları ve ses klasörü. */
    private inner class Pc(name: String, codec: SecretCodec = PlainCodec) {
        val dir: Path = tmp.newFolder(name).toPath()
        val settings: Path = dir.resolve("settings.json")
        val sounds: Path = dir.resolve("sounds")
        val store = Store(settings, codec)
        val service = BackupService(store, sounds, settings, "0.2.0", clock = { 1_790_000_000_000L }, iterations = 1_000)
    }

    private fun kindOf(block: () -> Unit): BackupException.Kind {
        try {
            block()
        } catch (e: BackupException) {
            return e.kind
        }
        fail("expected BackupException")
        error("unreachable")
    }

    private fun Pc.withEverything(): ByteArray {
        Files.createDirectories(sounds)
        val soundBytes = Files.readAllBytes(Files.write(sounds.resolve("0123456789ab-siren.wav"), ByteArray(64) { it.toByte() }))
        store.tuyaAccessId = "tid"
        store.tuyaSecret = "tsecret"
        store.appId = "app"
        store.appSecret = "asecret"
        store.accessToken = "oturum"
        store.alarmSoundUri = "user:0123456789ab-siren.wav"
        store.setRules("pompa", DeviceRules(actions = mapOf(EventType.TURNED_OFF to AlertAction.NOTIFY)))
        store.monitoringEnabled = true
        return soundBytes
    }

    @Test
    fun roundTripToAnotherComputer() {
        val a = Pc("a")
        val soundBytes = a.withEverything()
        val file = a.dir.resolve("yedek.sgbackup")
        a.service.write(file, "parola123".toCharArray())

        val b = Pc("b", OtherPcCodec)
        b.service.apply(b.service.read(file, "parola123".toCharArray()))

        val reopened = Store(b.settings, OtherPcCodec) // diske de yazıldı mı
        assertEquals("tid", reopened.tuyaAccessId)
        assertEquals("tsecret", reopened.tuyaSecret)
        assertEquals("asecret", reopened.appSecret)
        assertEquals("", reopened.accessToken)
        assertEquals(AlertAction.NOTIFY, reopened.rulesFor("pompa").actionFor(EventType.TURNED_OFF))
        assertEquals(true, reopened.monitoringEnabled)
        assertEquals("user:0123456789ab-siren.wav", reopened.alarmSoundUri)
        assertArrayEquals(soundBytes, Files.readAllBytes(b.sounds.resolve("0123456789ab-siren.wav")))
        assertTrue(String(Files.readAllBytes(b.settings)).contains("pc2:tsecret"))
    }

    @Test
    fun turkishPathWorks() {
        val a = Pc("a").apply { withEverything() }
        val folder = Files.createDirectories(a.dir.resolve("Masaüstü"))
        val file = folder.resolve("yedek ğş.sgbackup")

        a.service.write(file, "Şifreğı1".toCharArray())

        assertEquals("tid", a.service.read(file, "Şifreğı1".toCharArray()).content.getJSONObject("settings").getString("tuyaId"))
    }

    @Test
    fun summaryIsShownBeforeRestore() {
        val a = Pc("a").apply { withEverything() }
        val file = a.dir.resolve("yedek.sgbackup")
        a.service.write(file, "parola123".toCharArray())

        val s = a.service.read(file, "parola123".toCharArray()).summary

        assertEquals("0.2.0", s.appVersion)
        assertEquals(true, s.tuya)
        assertEquals(true, s.ewelinkKeys)
        assertEquals(1, s.rules)
        assertEquals(1, s.sounds)
    }

    @Test
    fun restoreKeepsACopyOfTheOldSettings() {
        val a = Pc("a").apply { withEverything() }
        val file = a.dir.resolve("yedek.sgbackup")
        a.service.write(file, "parola123".toCharArray())
        val b = Pc("b")
        b.store.appId = "eski-ayar"
        val before = Files.readAllBytes(b.settings)

        b.service.apply(b.service.read(file, "parola123".toCharArray()))

        val copy = Files.list(b.dir).use { s -> s.filter { it.fileName.toString().startsWith("settings.json.before-restore-") }.toList() }
        assertEquals(1, copy.size)
        assertArrayEquals(before, Files.readAllBytes(copy[0]))
    }

    @Test
    fun restoreClearsThePendingAlarm() {
        val a = Pc("a").apply { withEverything() }
        val file = a.dir.resolve("yedek.sgbackup")
        a.service.write(file, "parola123".toCharArray())
        val b = Pc("b")
        b.store.pendingAlarm = listOf("12:00  Pompa kapandı")

        b.service.apply(b.service.read(file, "parola123".toCharArray()))

        assertTrue(b.store.pendingAlarm.isEmpty())
    }

    @Test
    fun wrongPasswordChangesNothing() {
        val a = Pc("a").apply { withEverything() }
        val file = a.dir.resolve("yedek.sgbackup")
        a.service.write(file, "parola123".toCharArray())
        val b = Pc("b")
        b.store.appId = "dokunulmasın"
        val before = Files.readAllBytes(b.settings)

        assertEquals(BackupException.Kind.WRONG_PASSWORD_OR_DAMAGED, kindOf { b.service.read(file, "yanlis-parola".toCharArray()) })

        assertArrayEquals(before, Files.readAllBytes(b.settings))
        assertEquals("dokunulmasın", b.store.appId)
    }

    @Test
    fun oversizedFileIsRefused() {
        val b = Pc("b")
        val big = b.dir.resolve("big.sgbackup")
        RandomAccessFile(big.toFile(), "rw").use { it.setLength(101L * 1024 * 1024) }

        assertEquals(BackupException.Kind.TOO_BIG, kindOf { b.service.read(big, "x".toCharArray()) })
    }

    @Test
    fun soundNameFromABackupCannotEscapeTheSoundsFolder() {
        val b = Pc("b")
        val content = org.json.JSONObject()
            .put("app", "0.2.0").put("created", "x")
            .put("settings", org.json.JSONObject())
            .put("sounds", org.json.JSONObject().put("user:../evil.wav", "AAAA"))

        b.service.apply(BackupService.Loaded(content, BackupContent.summary(content)))

        assertTrue(!Files.exists(b.dir.resolve("evil.wav")))
    }

    @Test
    fun defaultFileNameHasTheDate() {
        assertEquals("SwitchGuard-yedek-2026-10-07.sgbackup", BackupService.defaultFileName(java.time.LocalDate.of(2026, 10, 7)))
    }
}
