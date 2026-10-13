package com.macerce.switchguard.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TwinAppTest {
    @Test
    fun `ana kullanici ikiz degildir`() = assertFalse(TwinApp.isTwin(0, isProfile = false, isManagedProfile = false))

    @Test
    fun `uretici ikiz kullanicilari eski Android'de de taninir`() {
        // Xiaomi Çift uygulama (999), Samsung Dual Messenger (95), vivo (666): profil bilgisi okunamasa da.
        for (user in listOf(999, 95, 666)) assertTrue(user.toString(), TwinApp.isTwin(user, isProfile = null, isManagedProfile = null))
    }

    @Test
    fun `klon profil ve ozel alan ikizdir`() = assertTrue(TwinApp.isTwin(11, isProfile = true, isManagedProfile = false))

    @Test
    fun `is profili ve ikincil kullanici ikiz sayilmaz`() {
        assertFalse(TwinApp.isTwin(10, isProfile = true, isManagedProfile = true))
        // Tablette başka bir kişinin kullanıcısı: profil değil, ayrı kurulum.
        assertFalse(TwinApp.isTwin(10, isProfile = false, isManagedProfile = false))
        assertFalse(TwinApp.isTwin(10, isProfile = null, isManagedProfile = null))
    }
}
