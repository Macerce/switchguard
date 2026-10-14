package com.macerce.switchguard.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
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
        } else {
            // Android 8.0: manifest özellikleri ve yukarıdaki çağrılar yok; eski pencere bayrakları gerekir.
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        // Ses tuşları alarmın sesini ayarlasın (yoksa medya sesi değişir, alarm aynı şiddette sürer).
        volumeControlStream = AudioManager.STREAM_ALARM
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

    override fun onStart() {
        super.onStart()
        visible = true
    }

    override fun onStop() {
        super.onStop()
        visible = false
        // Home'a basıldı ya da başka uygulama öne geçti ama alarm susturulmadı: ekran geri gelsin.
        if (!isFinishing) {
            val app = applicationContext
            Handler(Looper.getMainLooper()).postDelayed({ ensureShown(app) }, REASSERT_MS)
        }
    }

    companion object {
        private const val REASSERT_MS = 1500L

        /** Alarm ekranı şu an görünüyor mu. */
        @Volatile
        var visible = false
            private set

        /** SwitchGuard ana ekranı önde mi (orada alarm şeridi ve "Alarmı kapat" zaten var). */
        @Volatile
        var appInFront = false

        /**
         * TV'de alarm susturulmadıkça ekranın önde olmasını sağlar. Servis periyodik olarak ve
         * ekrandan/uygulamadan çıkıldığında çağırır; SwitchGuard önündeyse dokunmaz.
         */
        fun ensureShown(context: Context) {
            if (visible || appInFront || !FormFactor.isTv(context)) return
            if (Store.get(context).pendingAlarm.isEmpty()) return
            show(context)
        }

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

        /** İzin satırı yalnızca TV'de ve Android 10+'da anlamlı (telefonda bildirim yeterli, eski TV'de izin yok). */
        fun needsOverlayPermission(context: Context): Boolean = FormFactor.isTv(context) && Build.VERSION.SDK_INT >= 29

        fun openOverlaySettings(context: Context) {
            val direct = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, android.net.Uri.parse("package:${context.packageName}"))
            runCatching { context.startActivity(direct) }
                .onFailure { runCatching { context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)) } }
        }
    }
}

@Composable
private fun AlarmScreen(lines: List<String>, onDismiss: () -> Unit, onOpenApp: () -> Unit) {
    val tv = LocalIsTv.current
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 1f, targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "pulse",
    )
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    // Kırmızı zeminde durum çubuğu simgeleri her temada açık renk (temanın açık-tema ayarını ezer).
    val view = LocalView.current
    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
    }

    Box(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF7A1010), Color(0xFF2A0606)))),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.widthIn(max = 720.dp).padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Simgenin çevresinde dışa yayılan halkalar (logodaki ses dalgaları gibi).
            val waves = rememberInfiniteTransition(label = "waves")
            val wave by waves.animateFloat(
                initialValue = 0f, targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing)), label = "wave",
            )
            Box(
                Modifier.size(200.dp).drawBehind {
                    val base = 60.dp.toPx()
                    for (k in 0..2) {
                        val t = (wave + k / 3f) % 1f
                        drawCircle(
                            Color.White.copy(alpha = 0.28f * (1f - t)),
                            radius = base + (size.minDimension / 2 - base) * t,
                            style = Stroke(width = 3.dp.toPx()),
                        )
                    }
                },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier.size(120.dp).graphicsLayer { scaleX = pulse; scaleY = pulse }
                        .background(Color.White.copy(alpha = 0.14f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.NotificationsActive, null, tint = Color.White, modifier = Modifier.size(64.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            // Uzun başlık iki satıra bölünebilir: satır yüksekliği yazı boyutuna göre verilmezse satırlar üst üste biner.
            Text(
                stringResource(R.string.notif_alarm_title, lines.size),
                color = Color.White, fontSize = if (tv) 34.sp else 30.sp, lineHeight = 1.2.em,
                fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            // En yeni olay en üstte; çok sayıda olayda ilk dördü.
            lines.reversed().take(4).forEachIndexed { i, line ->
                Text(
                    line,
                    color = Color.White.copy(alpha = if (i == 0) 1f else 0.7f),
                    fontSize = if (i == 0) 24.sp else 18.sp,
                    lineHeight = 1.25.em,
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
