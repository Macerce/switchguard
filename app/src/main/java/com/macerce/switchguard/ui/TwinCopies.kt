package com.macerce.switchguard.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.net.Uri
import android.os.Build
import android.os.Process
import android.os.UserManager
import android.provider.Settings
import com.macerce.switchguard.core.TwinApp

/** Telefondaki ikiz kopyaları bulur (bkz. [TwinApp]); "Alarm size ulaşmayabilir" kartı kullanır. */
object TwinCopies {
    /** Android'de her kullanıcının uygulama kimlikleri bu aralıkta: kullanıcı no = uid / 100000. */
    private const val PER_USER_RANGE = 100_000

    /** Xiaomi'nin "Çift uygulamalar" ayarı (HyperOS ve MIUI). */
    private val DUAL_APP_SETTINGS = listOf(
        ComponentName("com.miui.securitycore", "com.miui.xspace.ui.activity.XSpaceSettingActivity"),
    )

    fun runningAsTwin(context: Context): Boolean {
        val users = context.getSystemService(UserManager::class.java)
        // Klon profil ve özel alan Android 14+'ta; profil sorgusu Android 13'ten beri herkese açık.
        val profile = if (Build.VERSION.SDK_INT >= 33) runCatching { users.isProfile }.getOrNull() else null
        val managed = if (Build.VERSION.SDK_INT >= 30) runCatching { users.isManagedProfile }.getOrNull() else null
        return TwinApp.isTwin(Process.myUid() / PER_USER_RANGE, profile, managed)
    }

    /** Başka bir profilde (ikiz, iş profili, klon) bu uygulama kurulu mu. Sistem izin vermezse false. */
    fun otherCopyInstalled(context: Context): Boolean = runCatching {
        val launcher = context.getSystemService(LauncherApps::class.java)
        val me = Process.myUserHandle()
        launcher.profiles.any { it != me && launcher.getActivityList(context.packageName, it).isNotEmpty() }
    }.getOrDefault(false)

    /** İkiz kopyadan: bu kopyanın uygulama bilgisi (oradaki "Kaldır" yalnızca bu kopyayı kaldırır). */
    fun openThisCopyInfo(context: Context) {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    /** Ana kopyadan: üreticinin çift uygulama ayarı, yoksa uygulama listesi (ikiz orada işaretli görünür). */
    fun openDualAppSettings(context: Context) {
        val opened = DUAL_APP_SETTINGS.any { component ->
            runCatching {
                context.startActivity(Intent().setComponent(component).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }.isSuccess
        }
        if (!opened) runCatching {
            context.startActivity(Intent(Settings.ACTION_APPLICATION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}
