package com.macerce.switchguard.desktop.backup

import org.json.JSONObject
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.fail
import org.junit.Test
import java.nio.charset.StandardCharsets.UTF_8
import java.time.Instant

class BackupCryptoTest {
    private val created = Instant.parse("2026-10-07T12:00:00Z")
    private val plain = """{"settings":{"tuyaSecret":"çok gizli"}}""".toByteArray(UTF_8)

    private fun seal(password: String = "parola123") = BackupCrypto.seal(plain, password.toCharArray(), created, iterations = 1_000)

    private fun kindOf(block: () -> Unit): BackupException.Kind {
        try {
            block()
        } catch (e: BackupException) {
            return e.kind
        }
        fail("expected BackupException")
        error("unreachable")
    }

    @Test
    fun sealedDataOpensWithThePassword() {
        assertArrayEquals(plain, BackupCrypto.open(seal(), "parola123".toCharArray()))
    }

    @Test
    fun envelopeIsReadableJsonWithoutThePlainText() {
        val text = String(seal(), UTF_8)
        val json = JSONObject(text)

        assertEquals("switchguard-backup", json.getString("format"))
        assertEquals(1, json.getInt("version"))
        assertEquals("2026-10-07T12:00:00Z", json.getString("created"))
        assertEquals("PBKDF2WithHmacSHA256", json.getJSONObject("kdf").getString("name"))
        assertEquals("AES/GCM/NoPadding", json.getJSONObject("cipher").getString("name"))
        assertFalse(text.contains("gizli"))
    }

    @Test
    fun defaultIterationsAre600k() {
        val json = JSONObject(String(BackupCrypto.seal(plain, "parola123".toCharArray(), created), UTF_8))
        assertEquals(600_000, json.getJSONObject("kdf").getInt("iterations"))
    }

    @Test
    fun wrongPasswordIsRefused() {
        assertEquals(BackupException.Kind.WRONG_PASSWORD_OR_DAMAGED, kindOf { BackupCrypto.open(seal(), "yanlış-parola".toCharArray()) })
    }

    @Test
    fun turkishPasswordWorks() {
        assertArrayEquals(plain, BackupCrypto.open(seal("Şifreğı1"), "Şifreğı1".toCharArray()))
        assertEquals(BackupException.Kind.WRONG_PASSWORD_OR_DAMAGED, kindOf { BackupCrypto.open(seal("Şifreğı1"), "Sifregi1".toCharArray()) })
    }

    @Test
    fun changedEnvelopeFieldIsDetected() {
        val json = JSONObject(String(seal(), UTF_8)).put("created", "2030-01-01T00:00:00Z")

        assertEquals(
            BackupException.Kind.WRONG_PASSWORD_OR_DAMAGED,
            kindOf { BackupCrypto.open(json.toString().toByteArray(UTF_8), "parola123".toCharArray()) },
        )
    }

    @Test
    fun newerVersionIsRefused() {
        val json = JSONObject(String(seal(), UTF_8)).put("version", 2)

        assertEquals(BackupException.Kind.TOO_NEW, kindOf { BackupCrypto.open(json.toString().toByteArray(UTF_8), "parola123".toCharArray()) })
    }

    @Test
    fun notABackupIsRefused() {
        assertEquals(BackupException.Kind.NOT_BACKUP, kindOf { BackupCrypto.open("alışveriş listesi".toByteArray(UTF_8), "x".toCharArray()) })
        assertEquals(BackupException.Kind.NOT_BACKUP, kindOf { BackupCrypto.open("""{"format":"other"}""".toByteArray(UTF_8), "x".toCharArray()) })
        assertEquals(
            BackupException.Kind.NOT_BACKUP,
            kindOf { BackupCrypto.open("""{"format":"switchguard-backup","version":1}""".toByteArray(UTF_8), "x".toCharArray()) },
        )
    }
}
