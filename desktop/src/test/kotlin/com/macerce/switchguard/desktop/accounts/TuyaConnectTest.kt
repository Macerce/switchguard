package com.macerce.switchguard.desktop.accounts

import com.macerce.switchguard.api.TuyaAuthException
import com.macerce.switchguard.api.TuyaExpiredException
import com.macerce.switchguard.data.Store
import com.macerce.switchguard.desktop.data.PlainCodec
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException

class TuyaConnectTest {
    @get:Rule val tmp = TemporaryFolder()

    private lateinit var store: Store
    private var probe: () -> Int = { 0 }
    private lateinit var connect: TuyaConnect

    @Before
    fun setUp() {
        store = Store(tmp.root.toPath().resolve("settings.json"), PlainCodec)
        connect = TuyaConnect(store) { probe() }
    }

    private fun previouslyConnected() {
        store.tuyaAccessId = "old-id"
        store.tuyaSecret = "old-secret"
        store.tuyaRegion = "us"
        store.tuyaToken = "old-token"
    }

    @Test
    fun successSavesTheKeysAndCountsDevices() {
        probe = {
            // Deneme, yeni bilgilerle yapılır (TuyaClient store'dan okur).
            assertEquals("new-id", store.tuyaAccessId)
            3
        }

        val result = connect.connect(" new-id ", "new-secret", "eu")

        assertEquals(TuyaResult.Connected(3), result)
        assertEquals("new-id", store.tuyaAccessId)
        assertEquals("new-secret", store.tuyaSecret)
        assertEquals("eu", store.tuyaRegion)
    }

    @Test
    fun authErrorRestoresThePreviousKeys() {
        previouslyConnected()
        probe = { throw TuyaAuthException(1004, "sign invalid") }

        val result = connect.connect("bad-id", "bad-secret", "eu")

        assertEquals(TuyaResult.AuthError, result)
        assertEquals("old-id", store.tuyaAccessId)
        assertEquals("old-secret", store.tuyaSecret)
        assertEquals("us", store.tuyaRegion)
        assertEquals("old-token", store.tuyaToken)
    }

    @Test
    fun expiredTrialIsReported() {
        probe = { throw TuyaExpiredException(28841004, "expired") }

        assertEquals(TuyaResult.Expired, connect.connect("id", "secret", "eu"))
        assertEquals("", store.tuyaAccessId)
    }

    @Test
    fun noInternetIsReportedAndRestores() {
        previouslyConnected()
        probe = { throw IOException("unreachable") }

        assertEquals(TuyaResult.NoInternet, connect.connect("id", "secret", "eu"))
        assertEquals("old-id", store.tuyaAccessId)
    }

    @Test
    fun disconnectClearsTheKeys() {
        previouslyConnected()

        connect.disconnect()

        assertEquals("", store.tuyaAccessId)
        assertEquals("", store.tuyaSecret)
    }
}
