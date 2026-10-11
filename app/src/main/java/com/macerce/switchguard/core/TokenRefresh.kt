package com.macerce.switchguard.core

/**
 * Token yenilemeyi tek seferde yapar. Servis ve ekran aynı anda süresi dolmuş token'la hata alırsa
 * yalnızca biri yeniler, diğeri yenilenmiş token'la devam eder: eWeLink her yenilemede refresh token'ı
 * değiştirdiğinden ikinci yenileme eski token'la reddedilir ve kullanıcı gereksiz yere çıkış yapmış olurdu.
 */
class TokenRefresh {
    private val lock = Any()

    /** [failedToken] ile yapılan istek token hatası aldı; token o arada yenilenmişse tekrar yenilemez. */
    fun refreshIfStale(failedToken: String, currentToken: () -> String, refresh: () -> Unit) {
        synchronized(lock) {
            val now = currentToken()
            if (now.isNotEmpty() && now != failedToken) return
            refresh()
        }
    }

    companion object {
        /** Sunucu tarafı geçici hata (ya da beklenmeyen yanıt): oturum geçerli sayılır, sonra yeniden denenir. */
        fun isTransient(code: Int) = code == -1 || code == 429 || code in 500..599
    }
}
