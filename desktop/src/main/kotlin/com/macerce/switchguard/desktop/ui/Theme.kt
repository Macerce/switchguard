package com.macerce.switchguard.desktop.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.macerce.switchguard.data.ConnState

// Telefondaki ui/theme/Theme.kt ile aynı renkler (dinamik renk ve TV dalı olmadan).
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

/** Durum renkleri: temadan bağımsız, anlamı sabit (telefondaki StatusColors + tepsi simgesi renkleri). */
object StatusColors {
    val online = Color(0xFF2E7D32)
    val offline = Color(0xFFC62828)
    val warning = Color(0xFFEF6C00)

    private val live = Color(0xFF2E9E44)
    private val connecting = Color(0xFFF9A825)
    private val failing = Color(0xFFD32F2F)
    private val stopped = Color(0xFF9E9E9E)

    /** Tepsi simgesi ve durum noktası: yeşil canlı, sarı bağlanıyor/sorgulama, kırmızı hata, gri durdu. */
    fun of(state: ConnState): Color = when (state) {
        ConnState.LIVE -> live
        ConnState.CONNECTING, ConnState.POLLING -> connecting
        ConnState.NO_INTERNET, ConnState.ERROR, ConnState.AUTH_ERROR -> failing
        ConnState.STOPPED -> stopped
    }
}

/** [theme]: "system" (Windows temasını izler), "light" ya da "dark" (bkz. Store.theme). */
@Composable
fun SwitchGuardTheme(theme: String = "system", content: @Composable () -> Unit) {
    val dark = when (theme) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    MaterialTheme(colorScheme = if (dark) Dark else Light, content = content)
}
