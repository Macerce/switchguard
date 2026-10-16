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

/**
 * [Snapshots.render] gibi, ama kareler arasında gerçek zamanda bekler: veritabanını IO iş parçacığında okuyan ekranlar
 * (geçmiş, enerji, son olaylar) sonuçları gelmeden çizilmesin. Çıktı yine `build/snapshots/<ad>.png`.
 */
object SettledSnapshots {
    fun render(
        name: String,
        width: Int = 1100,
        height: Int = 760,
        dark: Boolean = false,
        language: String = "tr",
        settleMs: Long = 1_200,
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
            val frames = (settleMs / 60).toInt().coerceAtLeast(10)
            for (frame in 1..frames) {
                Thread.sleep(60)
                image = scene.render(frame * 100_000_000L)
            }
            val data = checkNotNull(image.encodeToData(EncodedImageFormat.PNG)) { "PNG encode failed" }
            Files.createDirectories(Snapshots.dir)
            val out = Snapshots.dir.resolve("$name.png")
            Files.write(out, data.bytes)
            return out.toAbsolutePath()
        } finally {
            scene.close()
        }
    }
}
