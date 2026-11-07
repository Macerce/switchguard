package com.macerce.switchguard.desktop.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.macerce.switchguard.desktop.i18n.Strings

/** [LocalStrings]'i verir ve Arapçada yerleşimi sağdan sola çevirir ([Strings.isRtl]). */
@Composable
fun ProvideStrings(strings: Strings, content: @Composable () -> Unit) = CompositionLocalProvider(
    LocalStrings provides strings,
    LocalLayoutDirection provides if (strings.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
    content = content,
)

/**
 * Seçili dilin metinleri; ana pencere ve diğer pencereler kendi içeriğini bununla sarar. Telefondaki
 * `stringResource(R.string.x)` karşılığı [str] (`str("x")`), `pluralStringResource` karşılığı [plural].
 */
val LocalStrings = staticCompositionLocalOf<Strings> { error("LocalStrings is not provided") }

@Composable
@ReadOnlyComposable
fun str(key: String): String = LocalStrings.current[key]

@Composable
@ReadOnlyComposable
fun str(key: String, vararg args: Any): String = LocalStrings.current.format(key, *args)

@Composable
@ReadOnlyComposable
fun plural(key: String, count: Int, vararg args: Any): String = LocalStrings.current.plural(key, count, *args)
