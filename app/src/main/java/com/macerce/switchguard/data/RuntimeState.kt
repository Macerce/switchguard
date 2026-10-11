package com.macerce.switchguard.data

import android.content.Context
import android.content.SharedPreferences

/**
 * İzleme servisinin yeniden başlatmalarda korunması gereken iç durumu. [Store]'dan ayrı dosyada tutulur:
 * Store'a her yazma arayüzü yeniden çizer, bu bilgiler ise sık değişir ve arayüzü ilgilendirmez.
 * Değeri değişmeyen yazmalar diske gitmez (SharedPreferences fark yoksa yazmaz).
 */
class RuntimeState private constructor(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("switchguard_runtime", Context.MODE_PRIVATE)

    private fun put(block: SharedPreferences.Editor.() -> Unit) = prefs.edit().apply(block).apply()

    /** Süresi dolmamış "kopma" beklemeleri ([com.macerce.switchguard.core.OfflineGrace.toJson]). */
    var grace: String?
        get() = prefs.getString("grace", null)
        set(v) = put { putString("grace", v) }

    /** Otomasyon motorunun durumu ([com.macerce.switchguard.core.AutomationEngine.saveState]). */
    var automationState: String?
        get() = prefs.getString("automation", null)
        set(v) = put { putString("automation", v) }

    /** Servisin son "canlıyım" işareti (duvar saati, ms); kesinti tespiti için. */
    var lastAliveAt: Long
        get() = prefs.getLong("aliveAt", 0)
        set(v) = put { putLong("aliveAt", v) }

    /** İzleme kullanıcı tarafından kapatıldı (beklenmedik kapanma değil). */
    var cleanStop: Boolean
        get() = prefs.getBoolean("cleanStop", true)
        set(v) = put { putBoolean("cleanStop", v) }

    /** Kullanıcı izlemeyi kapatınca bekleyen işler de iptal olur. */
    fun clearEngine() = put { remove("grace"); remove("automation") }

    companion object {
        @Volatile private var instance: RuntimeState? = null
        fun get(context: Context): RuntimeState =
            instance ?: synchronized(this) { instance ?: RuntimeState(context).also { instance = it } }
    }
}
