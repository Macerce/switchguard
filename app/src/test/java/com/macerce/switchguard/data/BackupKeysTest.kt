package com.macerce.switchguard.data

import com.macerce.switchguard.core.BackupCrypto
import com.macerce.switchguard.core.Transfer
import com.macerce.switchguard.core.TypedPrefs
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.Instant

class BackupKeysTest {
    private val pw = "123456".toCharArray()
    private val templates = """[{"id":"t1","text":"Durum","at":420,"days":127}]"""

    /** Store.kt'deki bütün tercih anahtarları (kaynak koddan okunur; Store Android'e bağlı olduğundan JVM'de kurulamaz). */
    private fun storeKeys(): Set<String> {
        val src = listOf("src/main/java/com/macerce/switchguard/data/Store.kt", "app/src/main/java/com/macerce/switchguard/data/Store.kt")
            .map(::File).first { it.exists() }.readText()
        val accessor = "getString|getBoolean|getInt|getLong|getFloat|getStringSet|putString|putBoolean|putInt|putLong|putFloat|putStringSet|putSecret|remove|contains|str|secret"
        return Regex("""\b(?:$accessor)\("([A-Za-z0-9_]+)"""").findAll(src).map { it.groupValues[1] }.toSet() - "switchguard"
    }

    @Test
    fun everyStoreKeyIsBackedUpOrExcludedWithAReason() {
        val keys = storeKeys()
        assertTrue("Store anahtarları okunamadı", keys.size > 50)
        val forgotten = keys - Backup.KEYS - Backup.EXCLUDED.keys
        assertTrue("Yedeğe girmeyen ve EXCLUDED'da gerekçesi olmayan anahtar: $forgotten", forgotten.isEmpty())
        assertTrue(Backup.EXCLUDED.values.all { it.isNotBlank() })
        // Bir anahtar hem yedekte hem dışarıda olamaz.
        assertTrue((Backup.KEYS intersect Backup.EXCLUDED.keys).isEmpty())
    }

    @Test
    fun chatTemplatesStandbyAndStringSetSurviveBackupRoundTrip() {
        val prefs = mapOf(
            "chatTemplates" to templates,
            "chatDefaultsSeen" to setOf("status", "summary"),
            "chatTemplateUses" to """{"t1":3}""",
            "chatSpeak" to true,
            "standbyGrid" to "[]",
            "standbyDim" to 40,
            "pendingAlarm" to "çalıyor",
        )
        val content = JSONObject().put("app", "android").put("appVersion", "2.35.0").put("created", "2026-11-21T08:00:00Z")
            .put("keysVersion", Backup.KEYS_VERSION).put("settings", TypedPrefs.encode(prefs, Backup.KEYS))
        val sealed = BackupCrypto.seal(content.toString().toByteArray(), pw, Instant.parse("2026-11-21T08:00:00Z"), iterations = 1_000)

        val (summary, values) = Backup.read(sealed, pw)

        assertEquals(Backup.KEYS_VERSION, summary.keysVersion)
        assertEquals(prefs - "pendingAlarm", values)
    }

    @Test
    fun oldBackupDoesNotWipeChatTemplates() {
        val old = mapOf<String, Any>("rules" to "{}", "pollIntervalSec" to 30)

        val plan = Backup.plan(old, loggedIn = false, currentAppId = "", currentRegion = "eu", keysVersion = 1)

        Backup.KEYS_V2.forEach { assertFalse("$it silinmemeli", it in plan.remove) }
        assertTrue("quietEnabled" in plan.remove)
    }

    @Test
    fun newBackupWithoutTemplatesClearsThem() {
        val plan = Backup.plan(mapOf<String, Any>("rules" to "{}"), loggedIn = false, currentAppId = "", currentRegion = "eu")

        assertTrue("chatTemplates" in plan.remove)
    }

    @Test
    fun transferCarriesTemplatesAndDefaultPriceButNotStandbyLayout() {
        assertTrue(Transfer.KEYS.containsAll(listOf("chatTemplates", "chatDefaultsSeen", "chatTemplateUses", "energyPrice")))
        assertFalse("standbyGrid" in Transfer.KEYS)
        assertFalse("tuyaLive" in Transfer.KEYS)
    }

    @Test
    fun stringSetRoundTripsThroughTypedPrefs() {
        val keys = setOf("s")
        val json = TypedPrefs.encode(mapOf("s" to setOf("b", "a")), keys)

        assertEquals(mapOf("s" to setOf("a", "b")), TypedPrefs.decode(JSONObject(json.toString()), keys))
    }
}
