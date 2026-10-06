package com.macerce.switchguard.desktop.ui.settings

import androidx.compose.runtime.Composable
import com.macerce.switchguard.desktop.AppGraph
import com.macerce.switchguard.desktop.i18n.Strings

/**
 * Ayarlar → Yardım ve kurulum: Windows'a kurulum, Tuya ve eWeLink kurulum anlatımları (açılır kartlar; adımlar
 * internet olmadan okunur, düğmeler resimli rehbere, videoya ve geliştirici sitelerine gider). `spec-2-ayarlar.md` §7a.
 */
@Composable
fun HelpSection(graph: AppGraph, strings: Strings) {
    SetupSteps(strings["desktop_help_windows"], Setup.windowsSteps(strings), emptyList())
    SetupSteps(strings["desktop_help_tuya"], Setup.tuyaSteps(strings), Setup.tuyaActions(strings))
    SetupSteps(strings["desktop_help_ewelink"], Setup.ewelinkSteps(strings), Setup.ewelinkActions(strings, graph.store))
}
