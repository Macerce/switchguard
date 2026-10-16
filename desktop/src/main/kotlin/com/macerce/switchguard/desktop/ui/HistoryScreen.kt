package com.macerce.switchguard.desktop.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.macerce.switchguard.desktop.AppGraph

/** Geçmiş sekmesi (telefondaki HistoryScreen). Yer tutucu: Cihazlar/Geçmiş işinde yazılacak. */
@Composable
fun HistoryScreen(graph: AppGraph, modifier: Modifier = Modifier) {
    Column(modifier) {
        PageHeader(str("tab_history"))
    }
}
