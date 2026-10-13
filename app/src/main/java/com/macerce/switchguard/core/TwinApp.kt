package com.macerce.switchguard.core

/**
 * Uygulamanın aynı telefondaki ikinci kopyası: Xiaomi "Çift uygulama", Samsung "Dual Messenger", Android 14+
 * klon profil, Android 15 özel alan. İki kopya birbirinden habersiz izler: alarmlar iki kez çalar, aynı eWeLink
 * hesabıyla giriş yapan kopyalar birbirinin oturumunu kapatır (hesap başına tek oturum), Xiaomi'de ana kopyanın
 * izinleri kapalı görünür.
 */
object TwinApp {
    /** Üreticilerin ikiz uygulamalara ayırdığı sabit kullanıcı numaraları: Xiaomi/OPPO/realme/OnePlus 999, Samsung 95, vivo 666. */
    val OEM_CLONE_USERS = setOf(95, 666, 999)

    /**
     * Bu kopya ikiz mi. [isProfile] ve [isManagedProfile] Android 11 öncesinde okunamaz (null).
     * İkincil kullanıcı (tablette başka bir kişi) ve iş profili ayrı bir kurulumdur, ikiz sayılmaz.
     */
    fun isTwin(userId: Int, isProfile: Boolean?, isManagedProfile: Boolean?): Boolean = when {
        userId == 0 -> false
        userId in OEM_CLONE_USERS -> true
        // İş profili olmayan profil: klon profil ya da özel alan.
        else -> isProfile == true && isManagedProfile == false
    }
}
