package com.macerce.switchguard.desktop.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.runDesktopComposeUiTest
import com.macerce.switchguard.desktop.i18n.Strings
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO

/**
 * Etkileşimden sonraki ekranın görüntüsü (düzenleme modu, açılan pencereler): ekran test ortamında kurulur, [actions]
 * çalışır (tıklama vb.), sonra pencerenin tamamı — açık iletişim kutuları dahil — `build/snapshots/<ad>.png`'ye yazılır.
 */
@OptIn(ExperimentalTestApi::class)
object InteractiveSnapshots {
    fun render(
        name: String,
        width: Int = 1100,
        height: Int = 760,
        dark: Boolean = false,
        language: String = "tr",
        content: @Composable () -> Unit,
        actions: ComposeUiTest.() -> Unit,
    ): Path {
        val strings = Strings.load(language)
        var out: Path? = null
        runDesktopComposeUiTest(width, height) {
            setContent {
                SwitchGuardTheme(if (dark) "dark" else "light") {
                    CompositionLocalProvider(LocalStrings provides strings) {
                        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { content() }
                    }
                }
            }
            actions()
            waitForIdle()
            val image = onAllNodes(isRoot())[0].captureToImage().toAwtImage()
            Files.createDirectories(Snapshots.dir)
            val file = Snapshots.dir.resolve("$name.png")
            ImageIO.write(image, "png", file.toFile())
            out = file.toAbsolutePath()
        }
        return checkNotNull(out)
    }
}
