package com.macerce.switchguard.desktop.accounts

import com.macerce.switchguard.api.ApiException
import com.macerce.switchguard.data.Store
import com.macerce.switchguard.desktop.data.PlainCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException

class EwelinkLoginTest {
    @get:Rule val tmp = TemporaryFolder()

    private val redirect = "https://127.0.0.1"

    private fun parse(text: String, state: String = "S") = EwelinkRedirect.parse(text, redirect, state)

    @Test
    fun parsesCodeRegionAndState() {
        assertEquals(Parsed.Code("abc", "eu"), parse("https://127.0.0.1/?code=abc&region=eu&state=S"))
        assertEquals(Parsed.Code("abc", "eu"), parse("https://127.0.0.1?code=abc&region=eu&state=S"))
        assertEquals(Parsed.Code("abc", "as"), parse("  https://127.0.0.1/?state=S&region=as&code=abc \n"))
        assertEquals(Parsed.Code("a+b", null), parse("https://127.0.0.1/?code=a%2Bb&state=S"))
    }

    @Test
    fun wrongStateIsRefused() {
        assertEquals(Parsed.WrongState, parse("https://127.0.0.1/?code=abc&region=eu&state=OLD"))
        assertEquals(Parsed.WrongState, parse("https://127.0.0.1/?code=abc&region=eu"))
    }

    @Test
    fun otherAddressIsNotARedirect() {
        assertEquals(Parsed.NotRedirect, parse("https://example.com/?code=x&state=S"))
        assertEquals(Parsed.NotRedirect, parse("http://127.0.0.1/?code=x&state=S"))
        assertEquals(Parsed.NotRedirect, parse("https://127.0.0.10/?code=x&state=S"))
        assertEquals(Parsed.NotRedirect, parse("https://127.0.0.1/?state=S"))
        assertEquals(Parsed.NotRedirect, parse("bir alışveriş listesi"))
    }

    @Test
    fun newStateIsRandomHex() {
        val a = EwelinkRedirect.newState()
        val b = EwelinkRedirect.newState()
        assertTrue(a, a.matches(Regex("[0-9a-f]{32}")))
        assertTrue(a != b)
    }

    private fun store() = Store(tmp.root.toPath().resolve("settings.json"), PlainCodec)

    @Test
    fun missingRegionUsesTheStoredRegion() {
        val s = store().apply { region = "us" }
        val calls = mutableListOf<Pair<String, String>>()

        val result = EwelinkLogin(s) { c, r -> calls += c to r }.complete("abc", null)

        assertEquals(LoginResult.Ok, result)
        assertEquals(listOf("abc" to "us"), calls)
    }

    @Test
    fun apiRejectionIsReported() {
        val result = EwelinkLogin(store()) { _, _ -> throw ApiException(401, "redirect mismatch") }.complete("abc", "eu")

        assertEquals(LoginResult.Rejected("eWeLink 401: redirect mismatch"), result)
    }

    @Test
    fun noInternetIsReported() {
        assertEquals(LoginResult.NoInternet, EwelinkLogin(store()) { _, _ -> throw IOException("dns") }.complete("abc", "eu"))
    }

    @Test
    fun removeAccountDeletesKeysAndSession() {
        val s = store().apply {
            appId = "app"
            appSecret = "secret"
            accessToken = "at"
            refreshToken = "rt"
            accountEmail = "a@b.c"
        }

        EwelinkLogin(s) { _, _ -> }.removeAccount()

        assertEquals("", s.appId)
        assertEquals("", s.appSecret)
        assertEquals("", s.accessToken)
        assertEquals("", s.refreshToken)
        assertEquals("", s.accountEmail)
    }

    @Test
    fun emailIsMasked() {
        assertEquals("ma***@gmail.com", EwelinkLogin.maskEmail("macerce@gmail.com"))
        assertEquals("a***@x.io", EwelinkLogin.maskEmail("a@x.io"))
        assertEquals("", EwelinkLogin.maskEmail(""))
    }
}
