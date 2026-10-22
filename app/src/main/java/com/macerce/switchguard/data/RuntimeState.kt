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

    /** Hatırlatma bekleyen alarmlar ([com.macerce.switchguard.core.AlarmReminders.toJson]). */
    var reminders: String?
        get() = prefs.getString("reminders", null)
        set(v) = put { putString("reminders", v) }

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

    /** Çalan alarm yalnızca sessiz test alarmından oluşuyor: ses ve titreşim yok (gerçek bir olay gelince kalkar). */
    var silentAlarm: Boolean
        get() = prefs.getBoolean("silentAlarm", false)
        set(v) = put { putBoolean("silentAlarm", v) }

    /** Çalan alarmdaki ilk gerçek (test olmayan) olayın zamanı; ikinci kişiye haber verme buna göre. 0 = yok. */
    var realAlarmAt: Long
        get() = prefs.getLong("realAlarmAt", 0)
        set(v) = put { putLong("realAlarmAt", v) }

    /** Haber verilen alarmın [realAlarmAt] değeri: aynı alarm için ikinci kez gönderilmez. */
    var escalatedFor: Long
        get() = prefs.getLong("escalatedFor", 0)
        set(v) = put { putLong("escalatedFor", v) }

    /** "İzleme durdu" uyarısının son gösterildiği an; bekçi her dakika denerken uyarı tekrar tekrar çalmasın. */
    var stoppedWarnedAt: Long
        get() = prefs.getLong("stoppedWarnedAt", 0)
        set(v) = put { putLong("stoppedWarnedAt", v) }

    /** Elektrik kesintisi algılamanın durumu ([com.macerce.switchguard.core.OutageWatch.toJson]). */
    var outage: String?
        get() = prefs.getString("outage", null)
        set(v) = put { putString("outage", v) }

    /** Bu kurulumun kısa kimliği: aynı botu kullanan cihazlar "Gördüm" düğmesinin kime ait olduğunu bilsin. Yedeğe girmez. */
    val installTag: String
        get() = prefs.getString("installTag", null) ?: java.util.UUID.randomUUID().toString().take(8).also { tag -> put { putString("installTag", tag) } }

    /** Telegram: onaylanan son güncelleme (getUpdates offset) ve işlenen en büyük güncelleme numarası. */
    var tgOffset: Long
        get() = prefs.getLong("tgOffset", 0)
        set(v) = put { putLong("tgOffset", v) }
    var tgHandled: Long
        get() = prefs.getLong("tgHandled", 0)
        set(v) = put { putLong("tgHandled", v) }

    /** Bekçi (gönderen): sabitlenen mesajın sohbeti ve numarası; 0 = henüz yok. */
    var beatChat: Long
        get() = prefs.getLong("beatChat", 0)
        set(v) = put { putLong("beatChat", v) }
    var beatMessage: Long
        get() = prefs.getLong("beatMessage", 0)
        set(v) = put { putLong("beatMessage", v) }
    /** Bekçi (denetleyen): diğer cihazın son sinyali ve sessizlik alarmı verildi mi. */
    var watchLastBeat: Long
        get() = prefs.getLong("watchBeat", 0)
        set(v) = put { putLong("watchBeat", v) }
    var watchAlarmed: Boolean
        get() = prefs.getBoolean("watchAlarmed", false)
        set(v) = put { putBoolean("watchAlarmed", v) }

    /** Tüketim: anormallik denetiminin yapıldığı gün (gün başlangıcı, ms) ve bütçe uyarısı verilen ay ("2026-10"). */
    var energyCheckedDay: Long
        get() = prefs.getLong("energyDay", 0)
        set(v) = put { putLong("energyDay", v) }
    var budgetWarnedMonth: String
        get() = prefs.getString("budgetMonth", "")!!
        set(v) = put { putString("budgetMonth", v) }

    /** Şarjın kesildiği an (şarjdan kesinti algılama); 0 = şarjda. */
    var chargerLostAt: Long
        get() = prefs.getLong("chargerLost", 0)
        set(v) = put { putLong("chargerLost", v) }
    /** Son okunan dış sıcaklık (°C): düzenleyicide "şu an 12 °C" için. */
    var outdoorTemp: Double?
        get() = prefs.getString("outdoorTemp", null)?.toDoubleOrNull()
        set(v) = put { putString("outdoorTemp", v?.toString()) }

    /** Kademeli haber: çalan alarm için haber verilen ve vazgeçilen (kalıcı hata) sohbetler. */
    var escalatedChats: Set<Long>
        get() = prefs.getString("escalatedChats", "")!!.split(',').mapNotNull { it.toLongOrNull() }.toSet()
        set(v) = put { putString("escalatedChats", v.joinToString(",")) }
    /** Tatil modunun bittiği bildirilen tatil (bitiş anı). */
    var vacationEndNotified: Long
        get() = prefs.getLong("vacationEnd", 0)
        set(v) = put { putLong("vacationEnd", v) }

    /** Kullanıcı izlemeyi kapatınca bekleyen işler de iptal olur. */
    fun clearEngine() = put { remove("grace"); remove("automation"); remove("reminders"); remove("outage") }

    companion object {
        @Volatile private var instance: RuntimeState? = null
        fun get(context: Context): RuntimeState =
            instance ?: synchronized(this) { instance ?: RuntimeState(context).also { instance = it } }
    }
}
