package com.macerce.switchguard.ui.theme

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val Light = lightColorScheme(
    primary = Color(0xFF006A6A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF9CF1F0),
    onPrimaryContainer = Color(0xFF002020),
    secondary = Color(0xFF4A6363),
    secondaryContainer = Color(0xFFCCE8E7),
    tertiary = Color(0xFF4B607C),
    tertiaryContainer = Color(0xFFD3E4FF),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    background = Color(0xFFF4FBFA),
    surface = Color(0xFFF4FBFA),
    // Tüm yüzey tonları tanımlı olmalı: eksik olanlar Material'ın mor varsayılanına düşer (Android 11 ve altı).
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFEFF5F4),
    surfaceContainer = Color(0xFFE9EFEE),
    surfaceContainerHigh = Color(0xFFE3E9E8),
    surfaceContainerHighest = Color(0xFFDDE4E3),
    surfaceVariant = Color(0xFFDAE5E4),
    onSurfaceVariant = Color(0xFF3F4948),
    outline = Color(0xFF6F7979),
    outlineVariant = Color(0xFFBEC9C8),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF80D5D4),
    onPrimary = Color(0xFF003737),
    primaryContainer = Color(0xFF004F4F),
    onPrimaryContainer = Color(0xFF9CF1F0),
    secondary = Color(0xFFB0CCCB),
    secondaryContainer = Color(0xFF324B4B),
    tertiary = Color(0xFFB3C8E8),
    tertiaryContainer = Color(0xFF33485F),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    background = Color(0xFF0E1514),
    surface = Color(0xFF0E1514),
    surfaceContainerLowest = Color(0xFF090F0F),
    surfaceContainerLow = Color(0xFF161D1D),
    surfaceContainer = Color(0xFF1A2121),
    surfaceContainerHigh = Color(0xFF252B2B),
    surfaceContainerHighest = Color(0xFF2F3636),
    surfaceVariant = Color(0xFF3F4948),
    onSurfaceVariant = Color(0xFFBEC9C8),
    outline = Color(0xFF889392),
    outlineVariant = Color(0xFF3F4948),
)

/** Durum renkleri: temadan bağımsız, anlamı sabit. */
object StatusColors {
    val online = Color(0xFF2E7D32)
    val offline = Color(0xFFC62828)
    val warning = Color(0xFFEF6C00)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwitchGuardTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    // TV'de her zaman koyu tema: oturma odası ışığında açık zemin göz yorar; Google TV dili de koyudur.
    val tv = remember(context) { context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK) }
    val dark = tv || isSystemInDarkTheme()
    val scheme = when {
        tv -> Dark
        Build.VERSION.SDK_INT >= 31 -> if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> Dark
        else -> Light
    }
    // Durum/gezinme çubuğu simgeleri temanın gerçek rengine uysun (sistemin gece modu ara ayarlarında
    // ör. "bedtime" otomatik algılama yanılabiliyor).
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
    MaterialTheme(colorScheme = scheme) {
        // TV'de kumandayla gezilir: seçili öğe, Material'ın hafif odak katmanıyla seçilmiyor.
        // Odak katmanını belirginleştir (dokunmatik cihazlarda varsayılanlar kalır).
        if (tv) {
            CompositionLocalProvider(
                LocalRippleConfiguration provides RippleConfiguration(
                    rippleAlpha = RippleAlpha(draggedAlpha = 0.16f, focusedAlpha = 0.16f, hoveredAlpha = 0.08f, pressedAlpha = 0.24f),
                ),
                content = content,
            )
        } else {
            content()
        }
    }
}
