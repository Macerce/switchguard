package com.macerce.switchguard.ui

import android.app.Activity
import android.content.Context
import android.os.Build
import com.google.android.play.core.review.ReviewManagerFactory
import com.macerce.switchguard.core.ReviewPrompt
import com.macerce.switchguard.data.Store

/**
 * Play değerlendirme penceresini uygun anda bir kez ister (bkz. [ReviewPrompt]). Sayaçlar ayrı dosyada:
 * Store'a yazmak her açılışta tüm arayüzü tazelerdi. Play'den kurulmamışsa (GitHub APK) ya da TV'de hiç denenmez.
 */
object ReviewAsk {
    private const val PLAY = "com.android.vending"

    private fun prefs(context: Context) = context.getSharedPreferences("switchguard_review", Context.MODE_PRIVATE)

    /** Uygulama öne geldiğinde çağrılır. */
    fun onOpen(activity: Activity) {
        runCatching {
            val prefs = prefs(activity)
            val opens = prefs.getInt("opens", 0) + 1
            prefs.edit().putInt("opens", opens).apply()
            val store = Store.get(activity)
            val due = ReviewPrompt.due(
                installedAt = activity.packageManager.getPackageInfo(activity.packageName, 0).firstInstallTime,
                now = System.currentTimeMillis(),
                opens = opens,
                asked = prefs.getBoolean("asked", false),
                monitoring = store.monitoringEnabled,
                monitoredDevices = store.monitoredIds().size,
                alarmPending = store.pendingAlarm.isNotEmpty(),
            )
            if (!due || FormFactor.isTv(activity) || installer(activity) != PLAY) return
            val manager = ReviewManagerFactory.create(activity)
            manager.requestReviewFlow().addOnCompleteListener { request ->
                if (!request.isSuccessful || activity.isFinishing || activity.isDestroyed) return@addOnCompleteListener
                // Play pencereyi göstermese de (kota) bir daha istenmez.
                prefs.edit().putBoolean("asked", true).apply()
                manager.launchReviewFlow(activity, request.result)
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun installer(context: Context): String? = runCatching {
        if (Build.VERSION.SDK_INT >= 30) context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
        else context.packageManager.getInstallerPackageName(context.packageName)
    }.getOrNull()
}
