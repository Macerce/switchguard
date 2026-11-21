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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Close
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import com.macerce.switchguard.ui.theme.Brand
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
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
                    // Geri tuşu alarmı susturmasın; yalnızca kaydırarak (TV'de düğmeyle) kapanır.
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
        initialValue = 1f, targetValue = 1.08f,
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

    Box(Modifier.fillMaxSize().background(Brand.alarmScreen).safeDrawingPadding(), contentAlignment = Alignment.Center) {
        Column(
            Modifier.fillMaxSize().widthIn(max = 720.dp).padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Spacer(Modifier.height(8.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Tanıtım videosundaki çan: sabit iki halka + dışa yayılan dalgalar + nabız atan iç daire.
                val waves = rememberInfiniteTransition(label = "waves")
                val wave by waves.animateFloat(
                    initialValue = 0f, targetValue = 1f,
                    animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing)), label = "wave",
                )
                Box(
                    Modifier.size(if (tv) 220.dp else 240.dp).drawBehind {
                        val r = size.minDimension / 2
                        drawCircle(Color.White.copy(alpha = 0.10f), radius = r * 0.92f, style = Stroke(width = 2.dp.toPx()))
                        drawCircle(Color.White.copy(alpha = 0.16f), radius = r * 0.72f, style = Stroke(width = 2.dp.toPx()))
                        val base = r * 0.5f
                        for (k in 0..1) {
                            val t = (wave + k / 2f) % 1f
                            drawCircle(
                                Color.White.copy(alpha = 0.25f * (1f - t)),
                                radius = base + (r - base) * t,
                                style = Stroke(width = 3.dp.toPx()),
                            )
                        }
                    },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier.size(120.dp).graphicsLayer { scaleX = pulse; scaleY = pulse }
                            .background(Color.White.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.NotificationsActive, null, tint = Color.White, modifier = Modifier.size(64.dp))
                    }
                }
                Spacer(Modifier.height(20.dp))
                Text(
                    stringResource(R.string.alarm_screen_title),
                    color = Color.White, fontSize = if (tv) 44.sp else 40.sp, lineHeight = 1.15.em,
                    fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, letterSpacing = 0.04.em,
                )
                Spacer(Modifier.height(10.dp))
                // En yeni olay en üstte; çok sayıda olayda ilk dördü.
                lines.reversed().take(4).forEachIndexed { i, line ->
                    Text(
                        line,
                        color = Color.White.copy(alpha = if (i == 0) 1f else 0.75f),
                        fontSize = if (i == 0) 22.sp else 17.sp,
                        fontWeight = if (i == 0) FontWeight.SemiBold else FontWeight.Normal,
                        lineHeight = 1.25.em,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 3.dp),
                    )
                }
                if (lines.size > 4) {
                    Text("+${lines.size - 4}", color = Color.White.copy(alpha = 0.7f), fontSize = 16.sp)
                }
                Spacer(Modifier.height(20.dp))
                // "Sessiz modda bile çalar" hapı: alarm, alarm ses kanalından çalar.
                Row(
                    Modifier.background(Color.White.copy(alpha = 0.16f), CircleShape).padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.AutoMirrored.Rounded.VolumeUp, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.alarm_rings_silent), color = Color.White, fontSize = 14.sp)
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (tv) {
                    // Kumandada kaydırma yok: odaklı düğme.
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.focusRequester(focus).tvFocusRing(color = Color.White).height(64.dp).widthIn(min = 280.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF8B1A1A)),
                    ) {
                        Text(stringResource(R.string.action_dismiss_alarm), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    SwipeToDismiss(onDismiss)
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onOpenApp, modifier = Modifier.tvFocusRing(color = Color.White)) {
                    Text(stringResource(R.string.alarm_open_app), color = Color.White.copy(alpha = 0.85f))
                }
            }
        }
    }
}

/**
 * "Kapatmak için kaydırın": beyaz yuvarlak tutamak sona sürüklenince alarm kapanır, yarıda bırakılırsa geri
 * döner (cepte yanlışlıkla kapanmasın). TalkBack'te iz tek dokunuşla kapatılabilen bir düğmedir.
 */
@Composable
private fun SwipeToDismiss(onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    val offset = remember { Animatable(0f) }
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val dismissLabel = stringResource(R.string.action_dismiss_alarm)
    var done by remember { mutableStateOf(false) }
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .widthIn(max = 420.dp)
            .height(68.dp)
            .background(Color.White.copy(alpha = 0.18f), CircleShape)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = dismissLabel
                onClick(label = dismissLabel) { if (!done) { done = true; onDismiss() }; true }
            },
    ) {
        val thumb = 56.dp
        val pad = 6.dp
        val max = with(LocalDensity.current) { (maxWidth - thumb - pad * 2).toPx() }
        val progress = if (max > 0) (offset.value / max).coerceIn(0f, 1f) else 0f
        Text(
            stringResource(R.string.alarm_swipe_dismiss) + "  ›",
            color = Color.White.copy(alpha = 0.9f * (1f - progress)),
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.Center).padding(start = thumb),
        )
        Box(
            Modifier
                .padding(pad)
                .offset { IntOffset(((if (rtl) -1 else 1) * offset.value).roundToInt(), 0) }
                .size(thumb)
                .background(Color.White, CircleShape)
                .pointerInput(max) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                if (offset.value > max * 0.85f) {
                                    offset.animateTo(max, tween(120))
                                    if (!done) { done = true; onDismiss() }
                                } else {
                                    offset.animateTo(0f, spring())
                                }
                            }
                        },
                        onDragCancel = { scope.launch { offset.animateTo(0f, spring()) } },
                    ) { change, delta ->
                        change.consume()
                        scope.launch { offset.snapTo((offset.value + (if (rtl) -delta else delta)).coerceIn(0f, max)) }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Close, null, tint = Color(0xFFC62830), modifier = Modifier.size(28.dp))
        }
    }
}
