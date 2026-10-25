package com.macerce.switchguard.ui

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.macerce.switchguard.BuildConfig
import com.macerce.switchguard.R
import com.macerce.switchguard.core.UpdateCheck
import java.net.HttpURLConnection
import java.net.URL

/**
 * Yeni sürüm denetimi, günde en fazla bir kez. Play'den kurulduysa Play'in kendi güncelleme ekranı açılır
 * (Play politikası: Play uygulaması başka yoldan güncellenmez). GitHub'dan ya da elle kurulduysa GitHub yayınlarına
 * bakılır ve yeni sürüm varsa indirme sayfası önerilir; kullanıcı "Sonra" derse o sürüm bir daha sorulmaz.
 */
object AppUpdate {
    private const val PLAY = "com.android.vending"
    private const val INTERVAL_MS = 24 * 60 * 60 * 1000L

    private fun prefs(context: Context) = context.getSharedPreferences("switchguard_update", Context.MODE_PRIVATE)

    /** Uygulama öne geldiğinde çağrılır. */
    fun onOpen(activity: Activity) {
        runCatching {
            val prefs = prefs(activity)
            val now = System.currentTimeMillis()
            if (now - prefs.getLong("checkedAt", 0) < INTERVAL_MS) return
            prefs.edit().putLong("checkedAt", now).apply()
            if (installer(activity) == PLAY) checkPlay(activity) else checkGitHub(activity)
        }
    }

    private fun checkPlay(activity: Activity) {
        val manager = AppUpdateManagerFactory.create(activity)
        manager.appUpdateInfo.addOnSuccessListener { info ->
            if (activity.isFinishing || activity.isDestroyed) return@addOnSuccessListener
            if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE && info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                manager.startUpdateFlow(info, activity, AppUpdateOptions.defaultOptions(AppUpdateType.IMMEDIATE))
            }
        }
    }

    private fun checkGitHub(activity: Activity) {
        Thread {
            val release = runCatching {
                val conn = URL(UpdateCheck.RELEASES_URL).openConnection() as HttpURLConnection
                conn.connectTimeout = 15_000
                conn.readTimeout = 15_000
                conn.setRequestProperty("Accept", "application/vnd.github+json")
                val body = conn.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
                UpdateCheck.newer(body, UpdateCheck.PHONE_TAG, BuildConfig.VERSION_NAME)
            }.getOrNull() ?: return@Thread
            if (prefs(activity).getString("skipped", null) == release.version) return@Thread
            activity.runOnUiThread { offer(activity, release) }
        }.apply { isDaemon = true }.start()
    }

    private fun offer(activity: Activity, release: UpdateCheck.Release) {
        if (activity.isFinishing || activity.isDestroyed) return
        AlertDialog.Builder(activity)
            .setTitle(activity.getString(R.string.update_title, release.version))
            .setMessage(activity.getString(R.string.update_body, BuildConfig.VERSION_NAME))
            .setPositiveButton(R.string.update_download) { _, _ ->
                runCatching { activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(release.pageUrl))) }
            }
            .setNegativeButton(R.string.update_later) { _, _ -> prefs(activity).edit().putString("skipped", release.version).apply() }
            .show()
    }

    @Suppress("DEPRECATION")
    private fun installer(context: Context): String? = runCatching {
        if (Build.VERSION.SDK_INT >= 30) context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
        else context.packageManager.getInstallerPackageName(context.packageName)
    }.getOrNull()
}
