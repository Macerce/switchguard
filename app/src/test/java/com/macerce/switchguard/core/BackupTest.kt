package com.macerce.switchguard.core

import org.json.JSONObject
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.fail
import org.junit.Test
import java.time.Instant

class BackupTest {
    private val pw = "gizli-şifre-123".toCharArray()
    private val created = Instant.parse("2026-10-12T08:00:00Z")
    private fun seal(plain: String) = BackupCrypto.seal(plain.toByteArray(), pw, created, iterations = 1_000)

    private fun expect(kind: BackupException.Kind, block: () -> Unit) {
        try {
            block()
            fail("$kind bekleniyordu")
        } catch (e: BackupException) {
            assertEquals(kind, e.kind)
        }
    }

    @Test
    fun `dogru sifreyle acilir`() {
        val sealed = seal("{\"a\":1}")
        assertArrayEquals("{\"a\":1}".toByteArray(), BackupCrypto.open(sealed, pw))
        // Zarf okunabilir JSON, içerik şifreli.
        val env = JSONObject(String(sealed))
        assertEquals(BackupCrypto.FORMAT, env.getString("format"))
        assertFalse(env.getString("data").contains("\"a\""))
    }

    @Test
    fun `yanlis sifre`() = expect(BackupException.Kind.WRONG_PASSWORD_OR_DAMAGED) {
        BackupCrypto.open(seal("x"), "baska".toCharArray())
    }

    @Test
    fun `zarf alani degistirilirse acilmaz`() = expect(BackupException.Kind.WRONG_PASSWORD_OR_DAMAGED) {
        val env = JSONObject(String(seal("x"))).put("created", "2030-01-01T00:00:00Z")
        BackupCrypto.open(env.toString().toByteArray(), pw)
    }

    @Test
    fun `yedek olmayan dosya`() = expect(BackupException.Kind.NOT_BACKUP) {
        BackupCrypto.open("merhaba".toByteArray(), pw)
    }

    @Test
    fun `daha yeni surumun yedegi`() = expect(BackupException.Kind.TOO_NEW) {
        val env = JSONObject(String(seal("x"))).put("version", BackupCrypto.VERSION + 1)
        BackupCrypto.open(env.toString().toByteArray(), pw)
    }

    @Test
    fun `ayarlar tipleriyle gidip gelir, liste disindakiler atilir`() {
        val all = mapOf<String, Any?>(
            "appId" to "id1", "pollIntervalSec" to 30, "alarmBoost" to false, "alarmVolume" to 0.8f,
            "tuyaTokenExp" to 123L, "at" to "oturum-tokeni", "boş" to null,
        )
        val keys = setOf("appId", "pollIntervalSec", "alarmBoost", "alarmVolume", "tuyaTokenExp")
        val json = TypedPrefs.encode(all, keys)
        assertFalse(json.has("at"))
        val back = TypedPrefs.decode(JSONObject(json.toString()), keys)
        assertEquals(mapOf("appId" to "id1", "pollIntervalSec" to 30, "alarmBoost" to false, "alarmVolume" to 0.8f, "tuyaTokenExp" to 123L), back)
        // Okurken de izin listesi uygulanır.
        assertEquals(mapOf("appId" to "id1"), TypedPrefs.decode(json, setOf("appId")))
    }
}
