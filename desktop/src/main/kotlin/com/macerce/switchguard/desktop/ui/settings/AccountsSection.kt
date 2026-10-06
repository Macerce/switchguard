package com.macerce.switchguard.desktop.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.macerce.switchguard.core.Cloud
import com.macerce.switchguard.desktop.AppGraph
import com.macerce.switchguard.desktop.accounts.TuyaConnect
import com.macerce.switchguard.desktop.i18n.Strings

/**
 * Ayarlar → Hesap: eWeLink ([EwelinkRows]), Tuya / Smart Life (bağla, değiştir, kaldır, canlı bildirimler) ve demo
 * cihazları. Okunan değerler `store.version` her değiştiğinde yeniden okunur (çağıranın `remember(version)`'ı).
 */
@Composable
fun AccountsSection(graph: AppGraph, strings: Strings, version: Int) {
    val store = graph.store
    val hasTuya = remember(version) { store.hasTuya }
    val tuyaDevices = remember(version) { store.snapshots.count { it.cloud == Cloud.TUYA } }
    val tuyaRegion = remember(version) { store.tuyaRegion }
    val tuyaLive = remember(version) { store.tuyaLive }
    val demo = remember(version) { store.demoMode }
    var dialog by remember { mutableStateOf<String?>(null) } // "tuya", "tuya-remove", "tuya-live"

    SectionTitle(strings["section_account"])

    // --- eWeLink ---
    EwelinkRows(graph, strings, version)

    // --- Tuya ---
    SettingRow(
        strings["tuya_account"],
        if (hasTuya) strings.format("tuya_connected_x", strings["tuya_region_$tuyaRegion"], tuyaDevices) else strings["desktop_tuya_not_connected"],
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (hasTuya) {
                TextButton(onClick = { dialog = "tuya" }) { Text(strings["action_change"]) }
                OutlinedButton(onClick = { dialog = "tuya-remove" }) { Text(strings["tuya_disconnect"]) }
            } else {
                Button(onClick = { dialog = "tuya" }) { Text(strings["desktop_connect"]) }
            }
        }
    }
    if (hasTuya) {
        SwitchRow(strings["desktop_tuya_live"], strings["desktop_tuya_live_desc"], tuyaLive) { on ->
            if (on) {
                dialog = "tuya-live"
            } else {
                store.tuyaLive = false
                applyEngineSettings(graph)
            }
        }
    }

    // --- Demo ---
    SwitchRow(strings["demo_devices"], strings["demo_devices_desc"], demo) { on ->
        if (on) {
            store.enableDemo(strings.demoNames())
        } else {
            store.disableDemo()
            if (store.monitoringEnabled && !store.hasAnyAccount) graph.engine.stop()
        }
    }

    when (dialog) {
        "tuya" -> TuyaDialog(graph, strings) { dialog = null }
        "tuya-remove" -> Confirm(strings, strings["desktop_tuya_disconnect_confirm"], strings["desktop_remove"], onDismiss = { dialog = null }) {
            TuyaConnect(store).disconnect()
            if (!store.hasAnyAccount) graph.engine.stop() else applyEngineSettings(graph)
        }
        "tuya-live" -> Confirm(strings, strings["desktop_tuya_live_warning"], strings["desktop_turn_on"], onDismiss = { dialog = null }) {
            store.tuyaLive = true
            applyEngineSettings(graph)
        }
    }
}

/** Onay penceresi: [confirm] düğmesi [onConfirm]'u çalıştırıp kapanır, "İptal" yalnızca kapanır. */
@Composable
fun Confirm(strings: Strings, text: String, confirm: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { Text(text) },
        confirmButton = {
            Button(onClick = {
                onDismiss()
                onConfirm()
            }) { Text(confirm) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings["action_cancel"]) } },
    )
}
