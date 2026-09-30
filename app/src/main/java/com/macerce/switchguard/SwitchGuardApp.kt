package com.macerce.switchguard

import android.app.Application
import android.content.Context
import com.macerce.switchguard.core.AppLanguage

/** Yalnızca Android 12 ve altında seçili dili uygulama bağlamına uygulamak için var. */
class SwitchGuardApp : Application() {
    override fun attachBaseContext(base: Context) = super.attachBaseContext(AppLanguage.wrap(base))
}
