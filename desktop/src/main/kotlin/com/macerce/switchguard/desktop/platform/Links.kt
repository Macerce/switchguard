package com.macerce.switchguard.desktop.platform

import com.macerce.switchguard.desktop.Log
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import kotlin.concurrent.thread

/**
 * Dış bağlantılar (rehberler, videolar, geliştirici siteleri) ve klasör açma. Tarayıcı açılamazsa adres panoya
 * kopyalanır: kullanıcı en azından yapıştırıp açabilir (arayüz bunu söyler).
 */
object Links {
    const val TUYA_PLATFORM = "https://platform.tuya.com/cloud"
    const val EWELINK_DEV = "https://dev.ewelink.cc"
    const val TUYA_VIDEO = "https://youtu.be/sCdvMs7CSic"
    const val EWELINK_VIDEO = "https://youtu.be/jK_KcG2OPao"

    /** Telefonun da bağlandığı resimli rehberler (depodaki `docs/guide`). */
    private const val GUIDE_BASE = "https://github.com/Macerce/switchguard/blob/master/docs/guide/"

    /** [cloud] "tuya" ya da "ewelink"; [language] "tr" değilse İngilizce rehber. */
    fun guide(cloud: String, language: String): String = "$GUIDE_BASE$cloud.${if (language == "tr") "tr" else "en"}.md"

    /** Varsayılan tarayıcıda açar; açamazsa adresi panoya kopyalar ve false döner. */
    fun open(url: String, browse: (URI) -> Unit = ::desktopBrowse, copy: (String) -> Unit = ::copyToClipboard): Boolean =
        try {
            browse(URI(url))
            true
        } catch (e: Exception) {
            Log.w(TAG, "could not open a browser for $url: $e")
            runCatching { copy(url) }
            false
        }

    /** Klasörü Dosya Gezgini'nde açar (yoksa oluşturur); arayüzü bekletmesin diye ayrı iş parçacığında. */
    fun openFolder(dir: Path) {
        thread(name = "open-folder", isDaemon = true) {
            try {
                Files.createDirectories(dir)
                Desktop.getDesktop().open(dir.toFile())
            } catch (e: Exception) {
                Log.w(TAG, "could not open $dir: $e")
            }
        }
    }

    fun copyToClipboard(text: String) {
        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
    }

    private fun desktopBrowse(uri: URI) {
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            throw UnsupportedOperationException("browse not supported")
        }
        Desktop.getDesktop().browse(uri)
    }

    private const val TAG = "Links"
}
