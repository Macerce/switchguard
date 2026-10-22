package com.macerce.switchguard.wear

import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text

/**
 * Bilekte alarm: kırmızı ekran, son olay, güçlü ve tekrarlayan titreşim. "Kapat" telefondaki alarmı kapatır;
 * alarm başka yerden (telefon, Telegram) kapatılınca ekran kendiliğinden kapanır.
 */
class AlarmActivity : ComponentActivity() {
    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= 31) getSystemService(VibratorManager::class.java).defaultVibrator
        else @Suppress("DEPRECATION") getSystemService(Vibrator::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WearState.load(this)
        vibrator.vibrate(VibrationEffect.createWaveform(PATTERN, 0))
        setContent {
            MaterialTheme {
                val status by WearState.status.collectAsStateWithLifecycle()
                LaunchedEffect(status.alarming) { if (!status.alarming) finish() }
                Column(
                    Modifier.fillMaxSize().background(Color(0xFF8C1D18)).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(stringResource(R.string.alarm_title), color = Color.White, style = MaterialTheme.typography.titleMedium)
                    Text(
                        status.alarm.lastOrNull().orEmpty(),
                        color = Color.White, textAlign = TextAlign.Center, maxLines = 3,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Button(
                        onClick = {
                            vibrator.cancel()
                            PhoneLink.dismiss(this@AlarmActivity)
                            finish()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF8C1D18)),
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.dismiss), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        vibrator.cancel()
        super.onDestroy()
    }

    companion object {
        /** Bekle, titre, bekle, titre… (ms). */
        val PATTERN = longArrayOf(0, 700, 300, 700, 300, 1200, 800)
    }
}
