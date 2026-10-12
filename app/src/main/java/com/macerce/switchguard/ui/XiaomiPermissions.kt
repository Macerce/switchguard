package com.macerce.switchguard.ui

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Process
import android.provider.Settings

/**
 * Xiaomi'nin (MIUI / HyperOS) kendi izinleri. Standart Android izinlerinin yanında AppOps'ta 10000'in üstündeki
 * numaralarla tutulur; resmi API'si yok, gizli `checkOpNoThrow` yansımayla çağrılır. Okunamazsa (başka marka,
 * ROM değişmiş) null döner: uygulama eski davranışa (kullanıcının "Yaptım" onayı) düşer.
 */
object XiaomiPermissions {
    private const val OP_AUTO_START = 10008
    private const val OP_SHOW_WHEN_LOCKED = 10020
    private const val OP_BACKGROUND_START_ACTIVITY = 10021

    /** Otomatik başlatma: kapalıysa telefon yeniden başlayınca ya da süreç ölünce izleme geri gelmez. */
    fun autostart(context: Context): Boolean? = check(context, OP_AUTO_START)

    /**
     * Alarm ekranı telefon kilitliyken açılabilir mi: "Kilit ekranında görüntüle" ve "Arka planda çalışırken yeni
     * pencereler açın" birlikte gerekir. HyperOS'ta tam ekran bildirim izni de ikincisine bağlıdır.
     */
    fun lockScreenPopup(context: Context): Boolean? {
        val showWhenLocked = check(context, OP_SHOW_WHEN_LOCKED) ?: return null
        val backgroundStart = check(context, OP_BACKGROUND_START_ACTIVITY) ?: return null
        return showWhenLocked && backgroundStart
    }

    /** "Diğer izinler" sayfasını açar; bu sürümde yoksa uygulama bilgi sayfasına düşer. */
    fun openOtherPermissions(context: Context) {
        val opened = EDITORS.any { cls ->
            runCatching {
                context.startActivity(
                    Intent("miui.intent.action.APP_PERM_EDITOR")
                        .setClassName("com.miui.securitycenter", cls)
                        .putExtra("extra_pkgname", context.packageName)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }.isSuccess
        }
        if (!opened) {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    private val EDITORS = listOf(
        "com.miui.permcenter.permissions.PermissionsEditorActivity",
        "com.miui.permcenter.permissions.AppPermissionsEditorActivity",
    )

    /** İzin açık / kapalı; başka bir değer (hata, varsayılan) ya da okunamama null: emin olmadan uyarılmaz. */
    private fun check(context: Context, op: Int): Boolean? {
        if (OemBackground.current(context) != OemBackground.XIAOMI) return null
        val mode = runCatching {
            val ops = context.getSystemService(AppOpsManager::class.java)
            AppOpsManager::class.java
                .getMethod("checkOpNoThrow", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, String::class.java)
                .invoke(ops, op, Process.myUid(), context.packageName) as Int
        }.getOrNull()
        return when (mode) {
            AppOpsManager.MODE_ALLOWED -> true
            AppOpsManager.MODE_IGNORED -> false
            else -> null
        }
    }
}
