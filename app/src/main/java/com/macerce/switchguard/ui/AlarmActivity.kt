package com.macerce.switchguard.ui

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.macerce.switchguard.R
import com.macerce.switchguard.core.AppLanguage
import com.macerce.switchguard.data.Store
import com.macerce.switchguard.service.MonitorService
import com.macerce.switchguard.ui.theme.SwitchGuardTheme

/**
 * Tam ekran alarm. Android TV'de bildirimler ekranda görünmediği için alarm, o an ne açıksa
 * (YouTube, kanal…) onun üstüne bu ekranla çıkar; "Alarmı kapat" kumandada hazır seçili gelir.
 * Alarm başka yerden susturulursa ([Store.pendingAlarm] boşalırsa) kendiliğinden kapanır.
 * Ayrı görev (taskAffinity) olduğu için kapanınca kullanıcı önceki uygulamasına döner.
 */
class AlarmActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) = super.attachBaseContext(AppLanguage.wrap(newBase))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val store = Store.get(this)
        setContent {
            SwitchGuardTheme {
                CompositionLocalProvider(LocalIsTv provides FormFactor.isTv(this)) {
                    val version by store.version.collectAsStateWithLifecycle()
                    val lines = remember(version) { store.pendingAlarm }
                    LaunchedEffect(lines.isEmpty()) { if (lines.isEmpty()) finish() }
                    // Geri tuşu alarmı susturmasın; yalnızca düğmeyle kapanır.
                    BackHandler {}
                    AlarmScreen(
                        lines = lines,
                        onDismiss = {
                            MonitorService.send(this, MonitorService.ACTION_DISMISS)
                            finish()
                        },
                        onOpenApp = {
                            startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        },
                    )
                }
            }
        }
    }

    companion object {
        /** Alarm ekranını açar. Android 10+ arka plandan açmak için "diğer uygulamaların üzerinde göster" izni gerekir. */
        fun show(context: Context) {
            if (Build.VERSION.SDK_INT >= 29 && !Settings.canDrawOverlays(context)) return
            runCatching {
                context.startActivity(
                    Intent(context, AlarmActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                )
            }
        }

        /** Bu cihazda alarm ekranı kendiliğinden açılabilir mi (izin gerekiyorsa verilmiş mi). */
        fun canShowOverApps(context: Context): Boolean = Build.VERSION.SDK_INT < 29 || Settings.canDrawOverlays(context)
    }
}

@Composable
private fun AlarmScreen(lines: List<String>, onDismiss: () -> Unit, onOpenApp: () -> Unit) {
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 1f, targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "pulse",
    )
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    Box(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF7A1010), Color(0xFF2A0606)))),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.widthIn(max = 720.dp).padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                Modifier.size(120.dp).graphicsLayer { scaleX = pulse; scaleY = pulse }
                    .background(Color.White.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.NotificationsActive, null, tint = Color.White, modifier = Modifier.size(64.dp))
            }
            Spacer(Modifier.height(28.dp))
            Text(
                stringResource(R.string.notif_alarm_title, lines.size),
                color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            // En yeni olay en üstte; çok sayıda olayda ilk dördü.
            lines.reversed().take(4).forEachIndexed { i, line ->
                Text(
                    line,
                    color = Color.White.copy(alpha = if (i == 0) 1f else 0.7f),
                    fontSize = if (i == 0) 24.sp else 18.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 3.dp),
                )
            }
            if (lines.size > 4) {
                Text("+${lines.size - 4}", color = Color.White.copy(alpha = 0.6f), fontSize = 16.sp)
            }
            Spacer(Modifier.height(36.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.focusRequester(focus).tvFocusRing(color = Color.White).height(64.dp).widthIn(min = 280.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF8B1A1A)),
            ) {
                Text(stringResource(R.string.action_dismiss_alarm), fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onOpenApp, modifier = Modifier.tvFocusRing(color = Color.White)) {
                Text(stringResource(R.string.alarm_open_app), color = Color.White.copy(alpha = 0.85f))
            }
        }
    }
}
