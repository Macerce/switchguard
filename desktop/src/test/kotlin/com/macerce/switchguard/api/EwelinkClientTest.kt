package com.macerce.switchguard.api

import com.macerce.switchguard.core.Signer
import com.macerce.switchguard.data.Store
import com.macerce.switchguard.desktop.data.PlainCodec
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class EwelinkClientTest {
    @get:Rule val tmp = TemporaryFolder()

    @Test
    fun loginUrlHasOAuthParams() {
        val store = Store(tmp.root.toPath().resolve("settings.json"), PlainCodec)
        store.appId = "APPID"
        store.appSecret = "SECRET"

        val url = EwelinkClient(store).buildLoginUrl("st").toHttpUrl()

        assertEquals("c2ccdn.coolkit.cc", url.host)
        assertEquals("/oauth/index.html", url.encodedPath)
        assertEquals("APPID", url.queryParameter("clientId"))
        assertEquals("https://127.0.0.1", url.queryParameter("redirectUrl"))
        assertEquals("authorization_code", url.queryParameter("grantType"))
        assertEquals("st", url.queryParameter("state"))
        assertEquals("false", url.queryParameter("showQRCode"))
        assertNotNull(url.queryParameter("nonce"))
        val seq = url.queryParameter("seq")!!
        assertEquals(Signer.sign("APPID_$seq", "SECRET"), url.queryParameter("authorization"))
        // Aynı 8 parametre, telefondaki sırayla.
        assertEquals(
            listOf("clientId", "redirectUrl", "grantType", "state", "nonce", "seq", "showQRCode", "authorization"),
            (0 until url.querySize).map { url.queryParameterName(it) },
        )
    }
}
