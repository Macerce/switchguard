package com.macerce.switchguard.ui

import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.macerce.switchguard.billing.Billing
import com.macerce.switchguard.core.AppLanguage
import com.macerce.switchguard.data.Store
import com.macerce.switchguard.service.AppShortcuts
import com.macerce.switchguard.service.MonitorService
import com.macerce.switchguard.service.Notifier
import com.macerce.switchguard.ui.theme.SwitchGuardTheme

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) = super.attachBaseContext(AppLanguage.wrap(newBase))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Kanallar ilk açılışta oluşsun ki kullanıcı ayarlardan görebilsin.
        Notifier.createChannels(this)
        val store = Store.get(this)
        setContent {
            // Alarm çalarken ses tuşları alarmın sesini ayarlasın (yoksa medya sesi değişir, alarm aynı şiddette sürer).
            val version by store.version.collectAsStateWithLifecycle()
            LaunchedEffect(version) {
                volumeControlStream =
                    if (store.pendingAlarm.isNotEmpty()) AudioManager.STREAM_ALARM else AudioManager.USE_DEFAULT_STREAM_TYPE
            }
            SwitchGuardTheme {
                // Surface, renk verilmemiş tüm yazılara temanın onBackground rengini verir.
                // Onsuz Compose varsayılan olarak siyah yazar ve koyu temada yazılar kaybolur
                // (Scaffold kullanmayan kurulum sihirbazı ve detay ekranında olduğu gibi).
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val tv = remember { FormFactor.isTv(this) }
                    CompositionLocalProvider(LocalIsTv provides tv) {
                        // TV ekranları kenarları kesebilir: içerik güvenli alanda kalsın.
                        val frame = if (tv) Modifier.padding(FormFactor.TV_OVERSCAN_H, FormFactor.TV_OVERSCAN_V) else Modifier
                        Box(Modifier.fillMaxSize().then(frame)) { App() }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        AlarmActivity.appInFront = true
        // Satın alma/iade başka cihazda ya da Play'de olmuş olabilir: her açılışta tazele.
        Billing.get(this).refresh()
        // İzleme açık görünüyor ama servis yok (güncelleme/yeniden başlatma yayını engellendi, ör. Xiaomi): yeniden başlat.
        if (Store.get(this).monitoringEnabled && MonitorService.instance == null) {
            MonitorService.send(this, MonitorService.ACTION_START)
        }
        AppShortcuts.update(this)
        // Açılır açılmaz değil, ekran yerleştikten sonra; bu arada başka ekrana geçildiyse sorulmaz.
        Handler(Looper.getMainLooper()).postDelayed({
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                ReviewAsk.onOpen(this)
                AppUpdate.onOpen(this)
            }
        }, 3000)
    }

    override fun onPause() {
        super.onPause()
        AlarmActivity.appInFront = false
        // Sahneler uygulamada eklenip silinmiş olabilir.
        AppShortcuts.update(this)
        val app = applicationContext
        Handler(Looper.getMainLooper()).postDelayed({ AlarmActivity.ensureShown(app) }, 1500)
    }
}
