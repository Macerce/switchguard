package com.macerce.switchguard.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.macerce.switchguard.service.Notifier
import com.macerce.switchguard.ui.theme.SwitchGuardTheme

class MainActivity : ComponentActivity() {
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
}
