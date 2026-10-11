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

    /** Alarm için yükseltilmeden önceki alarm ses seviyesi; -1 = yükseltilmedi. */
    var savedAlarmVolume: Int
        get() = prefs.getInt("savedAlarmVolume", -1)
        set(v) = put { putInt("savedAlarmVolume", v) }

    /** Çalan alarmın ilk olayının zamanı (tepki süresi için); 0 = alarm yok. */
    var alarmStartedAt: Long
        get() = prefs.getLong("alarmStartedAt", 0)
        set(v) = put { putLong("alarmStartedAt", v) }

    /** Kullanıcı izlemeyi kapatınca bekleyen işler de iptal olur. */
    fun clearEngine() = put { remove("grace"); remove("automation") }

    companion object {
        @Volatile private var instance: RuntimeState? = null
        fun get(context: Context): RuntimeState =
            instance ?: synchronized(this) { instance ?: RuntimeState(context).also { instance = it } }
    }
}
