package com.macerce.switchguard.data

import com.macerce.switchguard.core.BackupCrypto
import com.macerce.switchguard.core.BackupException
import com.macerce.switchguard.core.DeviceRules
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.time.Instant

class BackupReadTest {
    private val pw = "123456".toCharArray()

    private fun typed(v: Any) = when (v) {
        is String -> JSONObject().put("t", "s").put("v", v)
        is Int -> JSONObject().put("t", "i").put("v", v)
        else -> JSONObject().put("t", "b").put("v", v)
    }

    @Test
    fun `yedek ozeti ve ayarlari okunur, oturum tokeni alinmaz`() {
        val rules = DeviceRules.mapToJson(mapOf("a" to DeviceRules(), "b" to DeviceRules(critical = true)))
        val settings = JSONObject()
            .put("appId", typed("app-1")).put("tuyaId", typed("t-1")).put("rules", typed(rules))
            .put("pollIntervalSec", typed(30)).put("at", typed("gizli-token"))
        val content = JSONObject().put("app", "android").put("appVersion", "2.10.0")
            .put("created", "2026-10-12T08:00:00Z").put("settings", settings)
        val sealed = BackupCrypto.seal(content.toString().toByteArray(), pw, Instant.parse("2026-10-12T08:00:00Z"), iterations = 1_000)

        val (summary, values) = Backup.read(sealed, pw)
        assertEquals(Backup.Summary("2026-10-12T08:00:00Z", "2.10.0", ewelinkKeys = true, tuya = true, rules = 2, automations = 0), summary)
        assertEquals(30, values["pollIntervalSec"])
        assertFalse(values.containsKey("at"))
        assertTrue(Backup.KEYS.containsAll(listOf("appId", "appSecret", "rules", "automations", "reminderMin")))
    }

    @Test
    fun `cok buyuk dosya yedek sayilmaz`() {
        try {
            Backup.read(ByteArray(Backup.MAX_BYTES + 1), pw)
            fail()
        } catch (e: BackupException) {
            assertEquals(BackupException.Kind.TOO_BIG, e.kind)
        }
    }
}
