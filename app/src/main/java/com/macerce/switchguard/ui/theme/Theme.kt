package com.macerce.switchguard.ui.theme

import android.app.Activity
import android.content.pm.PackageManager
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

// Marka: logodaki zümrüt → turkuaz → petrol mavisi geçişi (#00B894 → #00897B → #01465E).
// Uygulama her telefonda aynı görünsün diye duvar kâğıdından türeyen (dinamik) renkler kullanılmaz.

private val Light = lightColorScheme(
    primary = Color(0xFF00796B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC4EEE5),
    onPrimaryContainer = Color(0xFF00201B),
    secondary = Color(0xFF4A635E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCCE8E1),
    onSecondaryContainer = Color(0xFF06201B),
    tertiary = Color(0xFF3F6374),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFC3E8FC),
    onTertiaryContainer = Color(0xFF001F2A),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF2F5F4),
    onBackground = Color(0xFF161D1B),
    surface = Color(0xFFF2F5F4),
    onSurface = Color(0xFF161D1B),
    // Tüm yüzey tonları tanımlı olmalı: eksik olanlar Material'ın mor varsayılanına düşer (Android 11 ve altı).
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7FAF9),
    surfaceContainer = Color(0xFFEAEFED),
    surfaceContainerHigh = Color(0xFFE4E9E7),
    surfaceContainerHighest = Color(0xFFDEE4E2),
    surfaceVariant = Color(0xFFDAE5E1),
    onSurfaceVariant = Color(0xFF3F4946),
    outline = Color(0xFF6F7976),
    outlineVariant = Color(0xFFBEC9C5),
    inverseSurface = Color(0xFF2B3230),
    inverseOnSurface = Color(0xFFECF2EF),
    inversePrimary = Color(0xFF5FDBC1),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF5FDBC1),
    onPrimary = Color(0xFF00382F),
    primaryContainer = Color(0xFF005144),
    onPrimaryContainer = Color(0xFF7CF8DC),
    secondary = Color(0xFFB1CCC5),
    onSecondary = Color(0xFF1C3530),
    secondaryContainer = Color(0xFF334B46),
    onSecondaryContainer = Color(0xFFCCE8E1),
    tertiary = Color(0xFFA7CCE0),
    onTertiary = Color(0xFF0B3544),
    tertiaryContainer = Color(0xFF264B5B),
    onTertiaryContainer = Color(0xFFC3E8FC),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0D1412),
    onBackground = Color(0xFFDDE4E1),
    surface = Color(0xFF0D1412),
    onSurface = Color(0xFFDDE4E1),
    surfaceContainerLowest = Color(0xFF080E0D),
    surfaceContainerLow = Color(0xFF151C1A),
    surfaceContainer = Color(0xFF19201E),
    surfaceContainerHigh = Color(0xFF232A28),
    surfaceContainerHighest = Color(0xFF2E3533),
    surfaceVariant = Color(0xFF3F4946),
    onSurfaceVariant = Color(0xFFBEC9C5),
    outline = Color(0xFF899390),
    outlineVariant = Color(0xFF3F4946),
    inverseSurface = Color(0xFFDDE4E1),
    inverseOnSurface = Color(0xFF2B3230),
    inversePrimary = Color(0xFF00796B),
)

/** Durum renkleri: temadan bağımsız, anlamı sabit. */
object StatusColors {
    val online = Color(0xFF2E7D32)
    val offline = Color(0xFFC62828)
    val warning = Color(0xFFEF6C00)
}

/** Koyu tema mı: kutucuk tonu ve marka geçişleri buna göre seçilir. */
val LocalDarkTheme = staticCompositionLocalOf { false }

object Brand {
    /** Açık cihaz kutucuğu ve koruma kartı: logodaki geçiş. Üstündeki yazı her zaman beyazdır. */
    fun active(dark: Boolean): Brush = Brush.linearGradient(
        if (dark) listOf(Color(0xFF0E9C80), Color(0xFF005B50)) else listOf(Color(0xFF12B394), Color(0xFF00796B)),
    )

    /** Koruma kartı (izleme canlı): daha derin, petrol mavisine inen geçiş. */
    fun guard(dark: Boolean): Brush = Brush.linearGradient(
        if (dark) listOf(Color(0xFF00806C), Color(0xFF023A4E)) else listOf(Color(0xFF00A887), Color(0xFF01465E)),
    )

    /** Sorun var (bağlantı koptu, oturum düştü): turuncu-kırmızı geçiş. */
    fun problem(dark: Boolean): Brush = Brush.linearGradient(
        if (dark) listOf(Color(0xFFB4501A), Color(0xFF7A1F1F)) else listOf(Color(0xFFE9762B), Color(0xFFB3261E)),
    )

    val onActive = Color.White

    /** Beyaz zemin üstündeki marka rengi (açık cihazın beyaz güç düğmesi). */
    val deep = Color(0xFF00796B)
}

/** Kartların ve kutucukların zemini: açık temada beyaz, koyu temada zeminden bir ton açık. */
@Composable
@ReadOnlyComposable
fun tileColor(): Color =
    if (LocalDarkTheme.current) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainerLowest

/** Kutucuğun üstündeki yükseltilmiş öğelerin (simge zemini, kapalı kanal düğmesi) tonu. */
@Composable
@ReadOnlyComposable
fun raisedOnTile(): Color =
    if (LocalDarkTheme.current) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surfaceContainerHigh

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

private val AppTypography = Typography().run {
    copy(
        headlineMedium = headlineMedium.copy(fontWeight = FontWeight.Bold),
        headlineSmall = headlineSmall.copy(fontWeight = FontWeight.Bold),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold),
        titleSmall = titleSmall.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = labelLarge.copy(fontWeight = FontWeight.SemiBold),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwitchGuardTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    // TV'de her zaman koyu tema: oturma odası ışığında açık zemin göz yorar; Google TV dili de koyudur.
    val tv = remember(context) { context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK) }
    val dark = tv || isSystemInDarkTheme()
    val scheme = if (dark) Dark else Light
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
    MaterialTheme(colorScheme = scheme, shapes = AppShapes, typography = AppTypography) {
        CompositionLocalProvider(LocalDarkTheme provides dark) {
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
}
