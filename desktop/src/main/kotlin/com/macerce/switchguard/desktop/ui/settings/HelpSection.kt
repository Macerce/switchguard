package com.macerce.switchguard.desktop.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.macerce.switchguard.desktop.AppGraph
import com.macerce.switchguard.desktop.Log
import com.macerce.switchguard.desktop.i18n.Strings
import com.macerce.switchguard.desktop.platform.Links
import com.macerce.switchguard.desktop.ui.InfoRow
import com.macerce.switchguard.desktop.ui.RowDivider
import com.macerce.switchguard.desktop.ui.SectionCard
import com.macerce.switchguard.desktop.ui.SectionTitle
import com.macerce.switchguard.desktop.ui.str
import java.awt.Desktop
import java.net.URI

/** Telefondaki `Actions.CONTACT_EMAIL` / `PRIVACY_URL`. */
internal const val CONTACT_EMAIL = "switchguardapp@gmail.com"
internal const val PRIVACY_URL = "https://sites.google.com/view/switchguard-privacy"

/**
 * Ayarlar → Yardım ve hakkında (telefondaki AboutPage): resimli rehberler, uygulama içi kurulum anlatımları
 * ([HelpSection]), iletişim, günlük klasörü, gizlilik politikası, sürüm ve marka uyarısı. Tarayıcı ya da e-posta
 * uygulaması açılamazsa adres panoya kopyalanır ve ilgili kartın altında söylenir.
 */
@Composable
fun AboutPage(graph: AppGraph, strings: Strings) {
    var note by remember { mutableStateOf<Pair<String, String>?>(null) } // bölüm → not
    fun openLink(at: String, url: String) {
        note = if (Links.open(url)) null else at to strings["desktop_link_copied"]
    }
    val openIcon: @Composable () -> Unit = {
        Icon(Icons.AutoMirrored.Rounded.OpenInNew, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }

    SectionTitle(str("settings_help"))
    SectionCard {
        InfoRow(
            Icons.AutoMirrored.Rounded.MenuBook, str("guide_ewelink"), str("guide_desc"),
            Modifier.clickable { openLink("help", Links.guide("ewelink", strings.language)) }, openIcon,
        )
        RowDivider()
        InfoRow(
            Icons.AutoMirrored.Rounded.MenuBook, str("guide_tuya"), str("guide_desc"),
            Modifier.clickable { openLink("help", Links.guide("tuya", strings.language)) }, openIcon,
        )
    }
    note?.takeIf { it.first == "help" }?.let { PageIntro(it.second, Modifier.padding(top = 8.dp)) }

    SectionTitle(str("desktop_settings_setup"))
    HelpSection(graph, strings)

    SectionTitle(str("settings_contact"))
    SectionCard {
        InfoRow(
            Icons.Rounded.Email, str("contact"), CONTACT_EMAIL,
            Modifier.clickable {
                note = if (openMail(CONTACT_EMAIL)) {
                    null
                } else {
                    runCatching { Links.copyToClipboard(CONTACT_EMAIL) }
                    "contact" to strings["desktop_contact_copied"]
                }
            },
        )
        RowDivider()
        InfoRow(
            Icons.Rounded.FolderOpen, str("desktop_open_logs"), str("desktop_open_logs_desc"),
            Modifier.clickable { Links.openFolder(graph.dataDir.resolve("logs")) },
        )
        RowDivider()
        InfoRow(Icons.Rounded.PrivacyTip, str("privacy_policy"), null, Modifier.clickable { openLink("contact", PRIVACY_URL) }, openIcon)
    }
    note?.takeIf { it.first == "contact" }?.let { PageIntro(it.second, Modifier.padding(top = 8.dp)) }

    SectionTitle(str("section_about"))
    SectionCard {
        InfoRow(Icons.Rounded.Info, str("version_x", appVersion()), str("disclaimer"))
    }
}

/**
 * Uygulama içi kurulum anlatımları: Windows'a kurulum, Tuya ve eWeLink (açılır kartlar; adımlar internet olmadan
 * okunur, düğmeler resimli rehbere, videoya ve geliştirici sitelerine gider). `spec-2-ayarlar.md` §7a.
 */
@Composable
fun HelpSection(graph: AppGraph, strings: Strings) {
    SetupSteps(strings["desktop_help_windows"], Setup.windowsSteps(strings), emptyList())
    SetupSteps(strings["desktop_help_tuya"], Setup.tuyaSteps(strings), Setup.tuyaActions(strings))
    SetupSteps(strings["desktop_help_ewelink"], Setup.ewelinkSteps(strings), Setup.ewelinkActions(strings, graph.store))
}

/** Varsayılan e-posta uygulamasında yeni ileti; açılamazsa false. */
private fun openMail(address: String): Boolean = try {
    val desktop = Desktop.getDesktop()
    if (!desktop.isSupported(Desktop.Action.MAIL)) throw UnsupportedOperationException("mail not supported")
    desktop.mail(URI("mailto:$address?subject=SwitchGuard"))
    true
} catch (e: Exception) {
    Log.w("Settings", "could not open a mail app: $e")
    false
}
