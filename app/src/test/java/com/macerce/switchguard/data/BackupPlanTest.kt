package com.macerce.switchguard.data

import com.macerce.switchguard.core.BackupCrypto
import com.macerce.switchguard.core.BackupException
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.time.Instant

class BackupPlanTest {
    private val full = mapOf<String, Any>(
        "appId" to "app-1", "appSecret" to "s", "region" to "eu",
        "tuyaId" to "t-1", "tuyaSecret" to "ts", "rules" to "{}", "pollIntervalSec" to 30,
    )

    @Test
    fun `yedekte olmayan ayar temizlenir, yedektekiler yazilir`() {
        val plan = Backup.plan(full, loggedIn = false, currentAppId = "", currentRegion = "eu")
        assertTrue("quietEnabled" in plan.remove)
        assertEquals(full, plan.put)
        assertTrue("tuyaToken" in plan.remove)
    }

    @Test
    fun `yedekte eWeLink yoksa mevcut eWeLink anahtarlari ve oturum korunur`() {
        val tuyaOnly = full - setOf("appId", "appSecret", "region")
        val plan = Backup.plan(tuyaOnly, loggedIn = true, currentAppId = "app-9", currentRegion = "us")
        assertTrue(plan.keepSession)
        assertFalse("appId" in plan.remove)
        assertFalse("appSecret" in plan.remove)
        assertFalse("region" in plan.remove)
    }

    @Test
    fun `yedekte Tuya yoksa mevcut Tuya baglantisi korunur`() {
        val ewOnly = full - setOf("tuyaId", "tuyaSecret")
        val plan = Backup.plan(ewOnly, loggedIn = false, currentAppId = "", currentRegion = "eu")
        assertFalse("tuyaId" in plan.remove)
        assertFalse("tuyaSecret" in plan.remove)
        assertFalse("tuyaToken" in plan.remove)
    }

    @Test
    fun `oturum ayni App ID ve bolgede korunur, bolge degisince kapanir`() {
        assertTrue(Backup.plan(full, loggedIn = true, currentAppId = "app-1", currentRegion = "eu").keepSession)
        assertFalse(Backup.plan(full, loggedIn = true, currentAppId = "app-1", currentRegion = "us").keepSession)
        assertFalse(Backup.plan(full, loggedIn = true, currentAppId = "app-2", currentRegion = "eu").keepSession)
    }

    @Test
    fun `Windows surumunun yedegi telefonda geri yuklenmez`() {
        val pw = "123456".toCharArray()
        val desktop = JSONObject().put("app", "SwitchGuard 0.3.1").put("created", "2026-10-07T10:00:00Z")
            .put("settings", JSONObject().put("alarmVolume", 0.8).put("appId", "x"))
        val sealed = BackupCrypto.seal(desktop.toString().toByteArray(), pw, Instant.parse("2026-10-07T10:00:00Z"), iterations = 1_000)
        try {
            Backup.read(sealed, pw)
            fail()
        } catch (e: BackupException) {
            assertEquals(BackupException.Kind.OTHER_PLATFORM, e.kind)
        }
    }
}
