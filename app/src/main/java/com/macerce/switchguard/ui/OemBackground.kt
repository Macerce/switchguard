package com.macerce.switchguard.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.StringRes
import com.macerce.switchguard.R

/**
 * Üretici arka plan kısıtlamaları. Standart pil muafiyetine ek olarak bazı markalar
 * kendi "otomatik başlatma / uyutma" listelerini tutar; izin yoksa açılış ve güncelleme
 * yayınları uygulamaya ulaşmaz ya da servis arka planda öldürülür.
 * Ekranlar sürümden sürüme taşındığı için her marka için birkaç aday denenir; hiçbiri
 * açılmazsa uygulama bilgi sayfasına düşülür.
 */
enum class OemBackground(
    val label: String,
    @StringRes val hint: Int,
    private val screens: List<Pair<String, String>>,
) {
    XIAOMI(
        "Xiaomi", R.string.oem_hint_xiaomi,
        listOf("com.miui.securitycenter" to "com.miui.permcenter.autostart.AutoStartManagementActivity"),
    ),
    SAMSUNG(
        "Samsung", R.string.oem_hint_samsung,
        listOf(
            "com.samsung.android.lool" to "com.samsung.android.sm.battery.ui.BatteryActivity",
            "com.samsung.android.lool" to "com.samsung.android.sm.ui.battery.BatteryActivity",
            "com.samsung.android.sm" to "com.samsung.android.sm.ui.battery.BatteryActivity",
        ),
    ),
    HUAWEI(
        "Huawei / Honor", R.string.oem_hint_huawei,
        listOf(
            "com.huawei.systemmanager" to "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity",
            "com.huawei.systemmanager" to "com.huawei.systemmanager.optimize.process.ProtectActivity",
            "com.hihonor.systemmanager" to "com.hihonor.systemmanager.startupmgr.ui.StartupNormalAppListActivity",
        ),
    ),
    OPPO(
        "Oppo / Realme / OnePlus", R.string.oem_hint_oppo,
        listOf(
            "com.coloros.safecenter" to "com.coloros.safecenter.permission.startup.StartupAppListActivity",
            "com.coloros.safecenter" to "com.coloros.safecenter.startupapp.StartupAppListActivity",
            "com.oppo.safe" to "com.oppo.safe.permission.startup.StartupAppListActivity",
            "com.oneplus.security" to "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity",
        ),
    ),
    VIVO(
        "Vivo / iQOO", R.string.oem_hint_vivo,
        listOf(
            "com.vivo.permissionmanager" to "com.vivo.permissionmanager.activity.BgStartUpManagerActivity",
            "com.iqoo.secure" to "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager",
            "com.iqoo.secure" to "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity",
        ),
    ),
    TRANSSION(
        "Tecno / Infinix", R.string.oem_hint_transsion,
        listOf("com.transsion.phonemaster" to "com.cyin.himgr.autostart.AutoStartActivity"),
    ),
    ASUS(
        "Asus", R.string.oem_hint_asus,
        listOf("com.asus.mobilemanager" to "com.asus.mobilemanager.autostart.AutoStartActivity"),
    );

    /** Markanın ayar ekranını açar; açılamazsa uygulama bilgi sayfasına düşer. */
    fun open(context: Context) {
        val opened = screens.any { (pkg, cls) ->
            runCatching {
                context.startActivity(
                    Intent().setComponent(ComponentName(pkg, cls)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
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

    companion object {
        /** Bu telefonda ek ayar gerekiyorsa markası; stok Android ve Pixel için null. */
        fun current(): OemBackground? =
            when (Build.MANUFACTURER.lowercase()) {
                "xiaomi", "redmi", "poco" -> XIAOMI
                "samsung" -> SAMSUNG
                "huawei", "honor" -> HUAWEI
                "oppo", "realme", "oneplus" -> OPPO
                "vivo", "iqoo" -> VIVO
                "tecno", "infinix", "itel" -> TRANSSION
                "asus" -> ASUS
                else -> null
            }
    }
}
