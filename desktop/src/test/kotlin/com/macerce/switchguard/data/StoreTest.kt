package com.macerce.switchguard.data

import com.macerce.switchguard.core.ActionKind
import com.macerce.switchguard.core.AutoAction
import com.macerce.switchguard.core.Automation
import com.macerce.switchguard.core.Cloud
import com.macerce.switchguard.core.Demo
import com.macerce.switchguard.core.DeviceLayout
import com.macerce.switchguard.core.DeviceSnapshot
import com.macerce.switchguard.core.QuietHours
import com.macerce.switchguard.core.Trigger
import com.macerce.switchguard.core.TriggerKind
import com.macerce.switchguard.desktop.Log
import com.macerce.switchguard.desktop.data.SecretCodec
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files
import java.nio.file.Path
import kotlin.concurrent.thread

class StoreTest {
    @get:Rule val tmp = TemporaryFolder()

    /** Gizli alanların ham dosyada çıplak durmadığını görmek için görünür bir önek ekler. */
    private object EncCodec : SecretCodec {
        override fun encode(plain: String) = "enc:$plain"
        override fun decode(stored: String): String? =
            if (stored.startsWith("enc:")) stored.removePrefix("enc:") else stored
    }

    private object NullCodec : SecretCodec {
        override fun encode(plain: String) = "enc:$plain"
        override fun decode(stored: String): String? = null
    }

    private fun file(): Path = tmp.root.toPath().resolve("settings.json")
    private fun store(codec: SecretCodec = EncCodec) = Store(file(), codec)
    private fun raw() = String(Files.readAllBytes(file()), UTF_8)
    private fun dirNames(): List<String> =
        Files.list(tmp.root.toPath()).use { st -> st.map { it.fileName.toString() }.toList() }

    private fun auto(id: String, name: String) = Automation(
        id, name, true, Trigger(TriggerKind.TURNED_ON, "dev"), AutoAction(ActionKind.NOTIFY),
    )

    @Test
    fun valuesSurviveReload() {
        val a = store()
        a.appId = "abc"
        a.pollIntervalSec = 45
        a.quietHours = QuietHours(true, 1380, 420)

        val b = store()
        assertEquals("abc", b.appId)
        assertEquals(45, b.pollIntervalSec)
        assertEquals(QuietHours(true, 1380, 420), b.quietHours)
    }

    @Test
    fun defaultsMatchSpec() {
        val s = store()
        assertEquals("eu", s.region)
        assertEquals(30, s.pollIntervalSec)
        assertTrue(s.alwaysAwake)
        assertTrue(s.autostart)
        assertFalse(s.tuyaLive)
        assertEquals("https://127.0.0.1", s.redirectUrl)
        assertEquals(1f, s.alarmVolume)
        assertTrue(s.isPro)
        assertEquals("", s.language)
        assertEquals("system", s.theme)
        assertEquals(QuietHours(false, 23 * 60, 7 * 60), s.quietHours)
        assertEquals(0, s.offlineGraceSec)
        assertEquals("eu", s.tuyaRegion)
        assertEquals(21 * 60, s.dailySummaryMinute)
        assertFalse(s.dailySummaryEnabled)
        assertFalse(s.monitoringEnabled)
        assertFalse(s.demoMode)
        assertFalse(s.isLoggedIn)
        assertFalse(s.hasAnyAccount)
        assertNull(s.alarmSoundUri)
        assertNull(s.historySeenId)
        assertNull(s.freeDeviceId)
        assertTrue(s.snapshots.isEmpty())
        assertTrue(s.rules.isEmpty())
        assertTrue(s.automations.isEmpty())
        assertTrue(s.pendingAlarm.isEmpty())
        assertTrue(s.tuyaScales.isEmpty())
        assertEquals(DeviceLayout(), s.deviceLayout)
    }

    @Test
    fun pollIntervalIsClampedToMinimum() {
        val s = store()
        s.pollIntervalSec = 5
        assertEquals(15, s.pollIntervalSec)
        assertEquals(15, store().pollIntervalSec)
    }

    @Test
    fun secretsAreStoredEncoded() {
        val s = store()
        s.accessToken = "tok"
        val text = raw()
        assertTrue(text.contains("\"enc:tok\""))
        assertFalse(text.contains("\"tok\""))
        assertEquals("tok", store().accessToken)
    }

    @Test
    fun everySecretKeyIsEncodedOnDisk() {
        val keys = listOf("appSecret", "at", "rt", "apikey", "tuyaSecret", "tuyaToken")
        assertEquals(keys.toSet(), Store.SECRET_KEYS)
        val s = store()
        s.appSecret = "S1"
        s.accessToken = "S2"
        s.refreshToken = "S3"
        s.userApiKey = "S4"
        s.tuyaSecret = "S5"
        s.tuyaToken = "S6"
        val text = raw()
        val o = JSONObject(text)
        keys.forEachIndexed { i, key ->
            assertEquals("enc:S${i + 1}", o.getString(key))
            assertFalse(text.contains("\"S${i + 1}\""))
        }
        val b = store()
        assertEquals("S1", b.appSecret)
        assertEquals("S2", b.accessToken)
        assertEquals("S3", b.refreshToken)
        assertEquals("S4", b.userApiKey)
        assertEquals("S5", b.tuyaSecret)
        assertEquals("S6", b.tuyaToken)
    }

    @Test
    fun undecodableSecretReadsAsEmpty() {
        store().accessToken = "tok"
        val s = store(NullCodec)
        assertEquals("", s.accessToken)
        assertFalse(s.isLoggedIn)
    }

    @Test
    fun corruptFileIsBackedUpAndDefaultsUsed() {
        Files.write(file(), "{not json".toByteArray(UTF_8))
        val s = store()
        assertEquals("", s.appId)
        val backups = dirNames().filter { it.startsWith("settings.json.corrupt-") }
        assertEquals(1, backups.size)
        // bozuk içerik yedekte duruyor, yeni yazmalar temiz dosyaya gidiyor
        assertEquals("{not json", String(Files.readAllBytes(tmp.root.toPath().resolve(backups[0])), UTF_8))
        s.appId = "yeni"
        assertEquals("yeni", store().appId)
    }

    @Test
    fun fileWithUtf8BomIsRead() {
        val bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
        Files.write(file(), bom + "{\"appId\":\"bomlu\"}".toByteArray(UTF_8))
        assertEquals("bomlu", store().appId)
        assertEquals(listOf("settings.json"), dirNames())
    }

    @Test
    fun nonObjectTopLevelIsQuarantinedLikeAnyParseFailure() {
        Files.write(file(), "[1, 2]".toByteArray(UTF_8))
        val s = store()
        assertEquals("", s.appId)
        assertEquals(1, dirNames().count { it.startsWith("settings.json.corrupt-") })
    }

    @Test
    fun readFailureDoesNotQuarantineOrOverwrite() {
        // Dizin olan settings.json okunamaz (IOException) ama ayrıştırma hatası değildir.
        Files.createDirectory(file())
        assertThrows(IOException::class.java) { store() }
        assertTrue(Files.isDirectory(file()))
        assertEquals(listOf("settings.json"), dirNames())
    }

    @Test
    fun transientReadFailureIsRetried() {
        Files.createDirectory(file())
        val fixer = thread {
            Thread.sleep(100)
            Files.delete(file())
            Files.write(file(), "{\"appId\":\"geldi\"}".toByteArray(UTF_8))
        }
        val s = store()
        fixer.join()
        assertEquals("geldi", s.appId)
        assertEquals(listOf("settings.json"), dirNames())
    }

    @Test
    fun failedQuarantineMoveThrowsAndKeepsTheOriginal() {
        Files.write(file(), "{not json".toByteArray(UTF_8))
        // Yedek adı sabitlenip o ada dolu bir dizin kondu: taşıma DirectoryNotEmptyException ile başarısız olur.
        val blocker = tmp.root.toPath().resolve("settings.json.corrupt-1234")
        Files.createDirectory(blocker)
        Files.write(blocker.resolve("x"), byteArrayOf(1))
        assertThrows(IOException::class.java) { Store(file(), EncCodec) { 1234L } }
        assertEquals("{not json", raw())
        assertEquals(listOf("settings.json", "settings.json.corrupt-1234"), dirNames().sorted())
    }

    @Test
    fun jsonKeysMatchThePhoneStore() {
        val s = store()
        s.appId = "id"
        s.redirectUrl = "https://x.test"
        s.region = "us"
        s.accountEmail = "a@b.c"
        s.transferReceived = true
        s.transferSource = "src"
        s.tuyaAccessId = "tid"
        s.tuyaRegion = "cn"
        s.tuyaTokenExpiry = 1234567890123L
        s.tuyaScales = mapOf("pid" to mapOf("cur_power" to 1))
        s.demoMode = true
        s.sessionLost = true
        s.onboardingDone = true
        s.monitoringEnabled = true
        s.alwaysAwake = false
        s.pollIntervalSec = 60
        s.offlineGraceSec = 20
        s.alarmSoundUri = "file:///a.wav"
        s.alarmVolume = 0.5f
        s.quietHours = QuietHours(true, 60, 120)
        s.deviceLayout = DeviceLayout(priority = setOf("x"))
        s.historySeenId = 77L
        s.freeDeviceId = "dev"
        s.dailySummaryEnabled = true
        s.dailySummaryMinute = 600
        s.pendingAlarm = listOf("a", "b")
        s.autostart = false
        s.tuyaLive = true
        s.language = "tr"
        s.theme = "dark"
        val expected = setOf(
            "appId", "redirectUrl", "region", "email", "transferReceived", "transferSource",
            "tuyaId", "tuyaRegion", "tuyaTokenExp", "tuyaScales", "demo", "sessionLost", "onboardingDone",
            "monitoring", "alwaysAwake", "pollIntervalSec", "offlineGraceSec", "alarmSound", "alarmVolume",
            "quietEnabled", "quietStart", "quietEnd", "layout", "historySeen", "freeDevice",
            "summaryEnabled", "summaryMinute", "pendingAlarm", "autostart", "tuyaLive", "language", "theme",
        )
        assertEquals(expected, JSONObject(raw()).keySet())
        val b = store()
        assertEquals(1234567890123L, b.tuyaTokenExpiry)
        assertEquals(mapOf("pid" to mapOf("cur_power" to 1)), b.tuyaScales)
        assertEquals(0.5f, b.alarmVolume)
        assertEquals("file:///a.wav", b.alarmSoundUri)
        assertEquals(77L, b.historySeenId)
        assertEquals("dev", b.freeDeviceId)
        assertFalse(b.alwaysAwake)
        assertFalse(b.autostart)
        assertTrue(b.tuyaLive)
        assertEquals("tr", b.language)
        assertEquals("dark", b.theme)
        assertEquals(DeviceLayout(priority = setOf("x")), b.deviceLayout)
    }

    @Test
    fun logoutClearsSessionButKeepsCredentials() {
        val s = store()
        s.appId = "id"
        s.appSecret = "sec"
        s.accessToken = "a"
        s.refreshToken = "r"
        s.userApiKey = "k"
        s.accountEmail = "e@f.g"
        assertTrue(s.isLoggedIn)
        s.logout()
        val b = store()
        assertFalse(b.isLoggedIn)
        assertEquals("", b.refreshToken)
        assertEquals("", b.userApiKey)
        assertEquals("", b.accountEmail)
        assertEquals("id", b.appId)
        assertEquals("sec", b.appSecret)
        assertFalse(JSONObject(raw()).has("at"))
    }

    @Test
    fun nullableFieldsCanBeCleared() {
        val s = store()
        s.alarmSoundUri = "x"
        s.historySeenId = 5L
        s.freeDeviceId = "d"
        s.alarmSoundUri = null
        s.historySeenId = null
        s.freeDeviceId = null
        val b = store()
        assertNull(b.alarmSoundUri)
        assertNull(b.historySeenId)
        assertNull(b.freeDeviceId)
    }

    @Test
    fun turkishTextRoundTrips() {
        val snap = DeviceSnapshot("d1", "Rögar Su Seviye Bildirimi", true, mapOf(0 to true))
        val s = store()
        s.snapshots = listOf(snap)
        assertTrue(raw().contains("Rögar"))
        assertEquals(listOf(snap), store().snapshots)
    }

    @Test
    fun versionIncrementsOnWrite() {
        val s = store()
        val before = s.version.value
        s.appId = "x"
        assertTrue(s.version.value > before)
        val mid = s.version.value
        s.logout()
        assertTrue(s.version.value > mid)
    }

    @Test
    fun pendingAlarmKeepsLast20() {
        val s = store()
        s.pendingAlarm = (1..25).map { "satır $it" }
        val expected = (6..25).map { "satır $it" }
        assertEquals(expected, s.pendingAlarm)
        assertEquals(expected, store().pendingAlarm)
    }

    @Test
    fun demoEnableDisableRoundTrip() {
        val names = Demo.Names("Dondurucu", "Pompalar", "Işık", "Pompa takıldı", "Işık açık kaldı")
        val s = store()
        s.enableDemo(names)
        assertTrue(s.demoMode)
        assertTrue(s.hasAnyAccount)
        assertTrue(s.snapshots.any { it.cloud == Cloud.DEMO })
        assertTrue(s.automations.isNotEmpty())
        s.enableDemo(names) // ikinci çağrı çoğaltmaz
        assertEquals(Demo.devices(names).size, s.snapshots.size)
        s.disableDemo()
        assertFalse(s.demoMode)
        assertTrue(s.snapshots.isEmpty())
        assertTrue(s.automations.isEmpty())
    }

    @Test
    fun automationsSaveAndDelete() {
        val s = store()
        s.saveAutomation(auto("a1", "ilk"))
        s.saveAutomation(auto("a2", "ikinci"))
        s.saveAutomation(auto("a1", "yeni ad"))
        assertEquals(listOf("a1" to "yeni ad", "a2" to "ikinci"), store().automations.map { it.id to it.name })
        s.deleteAutomation("a1")
        assertEquals(listOf("a2"), store().automations.map { it.id })
    }

    @Test
    fun disconnectTuyaDropsTuyaData() {
        val s = store()
        s.tuyaAccessId = "id"
        s.tuyaSecret = "sec"
        s.tuyaToken = "tok"
        s.tuyaTokenExpiry = 9L
        s.tuyaScales = mapOf("p" to mapOf("c" to 1))
        s.snapshots = listOf(
            DeviceSnapshot("t1", "Tuya", true, mapOf(0 to true), cloud = Cloud.TUYA),
            DeviceSnapshot("e1", "eWeLink", true, mapOf(0 to true)),
        )
        s.disconnectTuya()
        val b = store()
        assertFalse(b.hasTuya)
        assertEquals("", b.tuyaToken)
        assertEquals(0L, b.tuyaTokenExpiry)
        assertTrue(b.tuyaScales.isEmpty())
        assertEquals(listOf("e1"), b.snapshots.map { it.id })
    }

    @Test
    fun monitoredIdsIsNeverLimitedByTheFreeTier() {
        val s = store()
        s.snapshots = listOf(
            DeviceSnapshot("a", "A", true, mapOf(0 to true)),
            DeviceSnapshot("b", "B", true, mapOf(0 to true)),
        )
        s.setRules("a", s.rulesFor("a").copy(monitored = true))
        s.setRules("b", s.rulesFor("b").copy(monitored = true))
        assertEquals(setOf("a", "b"), s.monitoredIds())
    }

    @Test
    fun writeFailureGoesToTheAppLog() {
        val logDir = tmp.newFolder("app").toPath()
        Log.init(logDir)
        val s = store()
        // Geçici dosyanın yerinde dolu bir dizin: yazma IOException ile başarısız olur.
        val blocker = tmp.root.toPath().resolve("settings.json.tmp")
        Files.createDirectory(blocker)
        Files.write(blocker.resolve("x"), byteArrayOf(1))

        s.appId = "yazılamadı"

        assertEquals("yazılamadı", s.appId) // bellekteki durum geçerli kalır
        val log = String(Files.readAllBytes(logDir.resolve("logs").resolve("switchguard.log")), UTF_8)
        assertTrue(log, log.contains("W/Store: could not write"))
    }

    @Test
    fun quarantineGoesToTheAppLog() {
        val logDir = tmp.newFolder("app").toPath()
        Log.init(logDir)
        Files.write(file(), "{not json".toByteArray(UTF_8))

        store()

        val log = String(Files.readAllBytes(logDir.resolve("logs").resolve("switchguard.log")), UTF_8)
        assertTrue(log, log.contains("W/Store: ") && log.contains("is not valid JSON"))
    }

    @Test
    fun concurrentWritesLeaveAValidFile() {
        val s = store()
        val failures = java.util.concurrent.ConcurrentLinkedQueue<Throwable>()
        val threads = (0 until 4).map { t ->
            thread {
                try {
                    repeat(60) { i ->
                        s.appId = "t$t-$i"
                        s.pendingAlarm = listOf("t$t", "$i")
                        s.saveAutomation(auto("auto-$t-$i", "ad $t-$i"))
                    }
                } catch (e: Throwable) {
                    failures.add(e)
                }
            }
        }
        threads.forEach { it.join() }
        assertTrue("worker failed: $failures", failures.isEmpty())
        // read-modify-write kayıpsız: hiçbir otomasyon kaybolmadı; dosya bütün ve bellekle aynı
        assertEquals(4 * 60, s.automations.map { it.id }.toSet().size)
        val b = store()
        assertEquals(s.appId, b.appId)
        assertEquals(s.pendingAlarm, b.pendingAlarm)
        assertEquals(s.automations, b.automations)
        // geçici dosya geride kalmaz
        assertEquals(listOf("settings.json"), dirNames())
    }

    // ---------------------------------------------------------------- DPAPI geçici hatası (M6)

    /** İlk [failures] çözme denemesinde null döner (DPAPI'nin geçici hatası), sonra çözer. */
    private class FlakyCodec(var failures: Int) : SecretCodec {
        override fun encode(plain: String) = "enc:$plain"
        override fun decode(stored: String): String? {
            if (failures > 0) {
                failures--
                return null
            }
            return stored.removePrefix("enc:")
        }
    }

    @Test
    fun secretThatFailsToDecodeOnceIsRetried() {
        store().accessToken = "tok"

        assertEquals("tok", store(FlakyCodec(failures = 1)).accessToken)
    }

    @Test
    fun secretThatNeverDecodesIsEmptyAndLoggedWithoutItsValue() {
        val logDir = tmp.newFolder("app").toPath()
        Log.init(logDir)
        store().accessToken = "gizli-token"

        assertEquals("", store(NullCodec).accessToken)
        val log = String(Files.readAllBytes(logDir.resolve("logs").resolve("switchguard.log")), UTF_8)
        assertTrue(log, log.contains("W/Store: could not decode secret 'at'"))
        assertFalse(log, log.contains("gizli-token"))
    }

    @Test
    fun undecodableSecretIsRetriedAndLoggedOnlyOnce() {
        val logDir = tmp.newFolder("app").toPath()
        Log.init(logDir)
        store().accessToken = "x"
        val s = store(NullCodec)

        repeat(5) { assertEquals("", s.accessToken) } // her tick'te okunur: her seferinde beklemesin, yazmasın

        val log = String(Files.readAllBytes(logDir.resolve("logs").resolve("switchguard.log")), UTF_8)
        assertEquals(log, 1, Regex("could not decode secret 'at'").findAll(log).count())
    }
}
