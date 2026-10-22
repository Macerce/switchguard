package com.macerce.switchguard.desktop.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.SystemFont
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import com.macerce.switchguard.data.ConnState

// Telefondaki ui/theme/Theme.kt (2.12) ile aynı renkler. Marka: logodaki zümrüt → turkuaz → petrol mavisi geçişi
// (#00B894 → #00897B → #01465E). Masaüstünde tema elle de seçilebilir (Store.theme); TV dalı yok.

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
    background = Color(0xFFF3F3F3),
    onBackground = Color(0xFF1B1B1B),
    surface = Color(0xFFF3F3F3),
    onSurface = Color(0xFF1B1B1B),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF9F9F9),
    surfaceContainer = Color(0xFFEDEDED),
    surfaceContainerHigh = Color(0xFFEAEAEA),
    surfaceContainerHighest = Color(0xFFE2E2E2),
    surfaceVariant = Color(0xFFE6E6E6),
    onSurfaceVariant = Color(0xFF5C5C5C),
    outline = Color(0xFF8A8A8A),
    outlineVariant = Color(0xFFE0E0E0),
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
    background = Color(0xFF202020),
    onBackground = Color(0xFFE6E6E6),
    surface = Color(0xFF202020),
    onSurface = Color(0xFFE6E6E6),
    surfaceContainerLowest = Color(0xFF1A1A1A),
    surfaceContainerLow = Color(0xFF272727),
    surfaceContainer = Color(0xFF2B2B2B),
    surfaceContainerHigh = Color(0xFF2D2D2D),
    surfaceContainerHighest = Color(0xFF383838),
    surfaceVariant = Color(0xFF3A3A3A),
    onSurfaceVariant = Color(0xFFB4B4B4),
    outline = Color(0xFF7A7A7A),
    outlineVariant = Color(0xFF353535),
    inverseSurface = Color(0xFFDDE4E1),
    inverseOnSurface = Color(0xFF2B3230),
    inversePrimary = Color(0xFF00796B),
)

/** Durum renkleri: temadan bağımsız, anlamı sabit (telefondaki StatusColors + tepsi simgesi renkleri). */
object StatusColors {
    val online = Color(0xFF2E7D32)
    val offline = Color(0xFFC62828)
    val warning = Color(0xFFEF6C00)

    private val live = Color(0xFF2E9E44)
    private val connecting = Color(0xFFF9A825)
    private val failing = Color(0xFFD32F2F)
    private val stopped = Color(0xFF9E9E9E)

    /** Tepsi simgesi: yeşil canlı, sarı bağlanıyor/sorgulama, kırmızı hata, gri durdu. */
    fun of(state: ConnState): Color = when (state) {
        ConnState.LIVE -> live
        ConnState.CONNECTING, ConnState.POLLING -> connecting
        ConnState.NO_INTERNET, ConnState.ERROR, ConnState.AUTH_ERROR -> failing
        ConnState.STOPPED -> stopped
    }
}

/** Koyu tema mı: kutucuk tonu ve marka geçişleri buna göre seçilir. */
val LocalDarkTheme = staticCompositionLocalOf { false }

object Brand {
    /** Açık cihaz kutucuğu: logodaki geçiş. Üstündeki yazı her zaman beyazdır. */
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

/** Kartların ve kutucukların zemini: açık temada beyaz, koyu temada zeminden bir ton açık (Windows 11 kart rengi). */
@Composable
@ReadOnlyComposable
fun tileColor(): Color =
    if (LocalDarkTheme.current) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainerLowest

/** Kartın ince kenarlığı (Fluent "card stroke"). */
@Composable
@ReadOnlyComposable
fun cardStroke(): Color = if (LocalDarkTheme.current) Color(0x14FFFFFF) else Color(0x0F000000)

/** Kenar çubuğunun zemini: pencere zemininden bir ton koyu/açık (Windows Ayarlar uygulaması gibi). */
@Composable
@ReadOnlyComposable
fun sidebarColor(): Color = if (LocalDarkTheme.current) Color(0xFF1C1C1C) else Color(0xFFEBEBEB)

/** Kutucuğun üstündeki yükseltilmiş öğelerin (simge zemini, kapalı kanal düğmesi) tonu. */
@Composable
@ReadOnlyComposable
fun raisedOnTile(): Color =
    if (LocalDarkTheme.current) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surfaceContainerHigh

// Masaüstü (Windows 11 / Fluent): küçük köşeler, Segoe UI, telefondan bir boy küçük yazı ölçeği (fare ve 100–150 %
// ölçekli ekran için; telefonun 16 sp gövdesi masaüstünde iri durur).
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(8.dp),
    extraLarge = RoundedCornerShape(6.dp),
)

/** Windows'un arayüz yazı tipi; yoksa Skia benzerini seçer. */
@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
val UiFont = FontFamily(
    SystemFont("Segoe UI", FontWeight.Normal),
    SystemFont("Segoe UI Semibold", FontWeight.SemiBold),
    SystemFont("Segoe UI", FontWeight.Bold),
    SystemFont("Segoe UI Semilight", FontWeight.Light),
)

private val AppTypography = Typography().run {
    fun TextStyle.ui(size: Int, line: Int, weight: FontWeight = FontWeight.Normal) =
        copy(fontFamily = UiFont, fontSize = size.sp, lineHeight = line.sp, fontWeight = weight, letterSpacing = 0.sp)
    copy(
        displayLarge = displayLarge.ui(48, 56, FontWeight.SemiBold),
        displayMedium = displayMedium.ui(40, 48, FontWeight.SemiBold),
        displaySmall = displaySmall.ui(34, 40, FontWeight.SemiBold),
        headlineLarge = headlineLarge.ui(28, 36, FontWeight.SemiBold),
        headlineMedium = headlineMedium.ui(26, 32, FontWeight.SemiBold),
        headlineSmall = headlineSmall.ui(24, 30, FontWeight.SemiBold),
        titleLarge = titleLarge.ui(18, 24, FontWeight.SemiBold),
        titleMedium = titleMedium.ui(15, 20, FontWeight.SemiBold),
        titleSmall = titleSmall.ui(14, 20, FontWeight.SemiBold),
        bodyLarge = bodyLarge.ui(14, 20),
        bodyMedium = bodyMedium.ui(13, 18),
        bodySmall = bodySmall.ui(12, 16),
        labelLarge = labelLarge.ui(13, 18, FontWeight.SemiBold),
        labelMedium = labelMedium.ui(12, 16),
        labelSmall = labelSmall.ui(11, 14),
    )
}

/** [theme]: "system" (Windows temasını izler), "light" ya da "dark" (bkz. Store.theme). */
@Composable
fun SwitchGuardTheme(theme: String = "system", content: @Composable () -> Unit) {
    val dark = when (theme) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    MaterialTheme(colorScheme = if (dark) Dark else Light, shapes = AppShapes, typography = AppTypography) {
        CompositionLocalProvider(LocalDarkTheme provides dark, content = content)
    }
}
