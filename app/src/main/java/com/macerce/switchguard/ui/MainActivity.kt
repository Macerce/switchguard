package com.macerce.switchguard.ui

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.macerce.switchguard.billing.Billing
import com.macerce.switchguard.core.AppLanguage
import com.macerce.switchguard.data.Store
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
        setContent {
            SwitchGuardTheme {
                // Surface, renk verilmemiş tüm yazılara temanın onBackground rengini verir.
                // Onsuz Compose varsayılan olarak siyah yazar ve koyu temada yazılar kaybolur
                // (Scaffold kullanmayan kurulum sihirbazı ve detay ekranında olduğu gibi).
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    App()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Satın alma/iade başka cihazda ya da Play'de olmuş olabilir: her açılışta tazele.
        Billing.get(this).refresh()
        // İzleme açık görünüyor ama servis yok (güncelleme/yeniden başlatma yayını engellendi, ör. Xiaomi): yeniden başlat.
        if (Store.get(this).monitoringEnabled && MonitorService.instance == null) {
            MonitorService.send(this, MonitorService.ACTION_START)
        }
    }
}
