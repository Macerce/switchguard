package com.macerce.switchguard.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.macerce.switchguard.service.Notifier
import com.macerce.switchguard.ui.theme.SwitchGuardTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Kanallar ilk açılışta oluşsun ki kullanıcı ayarlardan görebilsin.
        Notifier.createChannels(this)
        setContent {
            SwitchGuardTheme { App() }
        }
    }
}
