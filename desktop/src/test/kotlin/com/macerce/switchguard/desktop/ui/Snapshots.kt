package com.macerce.switchguard.desktop.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import com.macerce.switchguard.desktop.i18n.Strings
import org.jetbrains.skia.EncodedImageFormat
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Geliştirme yardımcısı: bir ekranı pencere açmadan PNG'ye çizer (`desktop/build/snapshots/<ad>.png`). Görsel kontrol
 * içindir, karşılaştırma yapmaz. Tema ve metinler uygulamadaki gibi sarılır; animasyonlar bitsin diye birkaç kare ilerletilir.
 */
object Snapshots {
    val dir: Path = Paths.get("build", "snapshots")

    fun render(
        name: String,
        width: Int = 1100,
        height: Int = 760,
        dark: Boolean = false,
        language: String = "tr",
        content: @Composable () -> Unit,
    ): Path {
        val strings = Strings.load(language)
        val scene = ImageComposeScene(width, height, Density(1f)) {
            SwitchGuardTheme(if (dark) "dark" else "light") {
                CompositionLocalProvider(LocalStrings provides strings) {
                    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { content() }
                }
            }
        }
        try {
            var image = scene.render(0)
            for (frame in 1..10) image = scene.render(frame * 100_000_000L)
            val data = checkNotNull(image.encodeToData(EncodedImageFormat.PNG)) { "PNG encode failed" }
            Files.createDirectories(dir)
            val out = dir.resolve("$name.png")
            Files.write(out, data.bytes)
            return out.toAbsolutePath()
        } finally {
            scene.close()
        }
    }
}
