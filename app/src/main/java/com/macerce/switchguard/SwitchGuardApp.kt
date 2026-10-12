package com.macerce.switchguard

import android.app.Application
import android.content.Context
import com.macerce.switchguard.core.AppLanguage
import com.macerce.switchguard.data.CrashLog

/** Android 12 ve altında seçili dili uygulama bağlamına uygular; çökmeleri yerel olarak kaydeder. */
class SwitchGuardApp : Application() {
    override fun attachBaseContext(base: Context) = super.attachBaseContext(AppLanguage.wrap(base))

    override fun onCreate() {
        super.onCreate()
        CrashLog.install(this)
    }
}
