package com.macerce.switchguard.desktop.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material3.AlertDialog
import com.macerce.switchguard.desktop.ui.Button
import androidx.compose.material3.Text
import com.macerce.switchguard.desktop.ui.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.macerce.switchguard.core.Cloud
import com.macerce.switchguard.desktop.AppGraph
import com.macerce.switchguard.desktop.accounts.TuyaConnect
import com.macerce.switchguard.desktop.i18n.Strings
import com.macerce.switchguard.desktop.ui.InfoRow
import com.macerce.switchguard.desktop.ui.SectionCard
import com.macerce.switchguard.desktop.ui.SectionTitle
import com.macerce.switchguard.desktop.ui.str

/**
 * Ayarlar → Hesaplar (telefondaki AccountsPage): eWeLink ([EwelinkRows]), Tuya / Smart Life (bağla, değiştir,
 * bağlantıyı kes) ve demo cihazları. Tuya canlı bildirimleri İzleme sayfasında. Okunan değerler `store.version` her
 * değiştiğinde yeniden okunur.
 */
@Composable
fun AccountsPage(graph: AppGraph, strings: Strings) {
    val store = graph.store
    val version by store.version.collectAsState()
    val hasTuya = remember(version) { store.hasTuya }
    val tuyaDevices = remember(version) { store.snapshots.count { it.cloud == Cloud.TUYA } }
    val tuyaRegion = remember(version) { store.tuyaRegion }
    val demo = remember(version) { store.demoMode }
    var dialog by remember { mutableStateOf<String?>(null) } // "tuya", "tuya-remove"

    // --- eWeLink ---
    SectionTitle("eWeLink")
    EwelinkRows(graph, strings, version)

    // --- Tuya ---
    SectionTitle(str("tuya_account"))
    SectionCard {
        InfoRow(
            Icons.Rounded.Hub,
            str("tuya_account"),
            if (hasTuya) strings.plural("tuya_connected_x", tuyaDevices, strings["tuya_region_$tuyaRegion"], tuyaDevices)
            else str("desktop_tuya_not_connected"),
            Modifier.clickable { dialog = "tuya" },
        ) {
            if (hasTuya) {
                TextButton(onClick = { dialog = "tuya-remove" }) { Text(str("tuya_disconnect")) }
            } else {
                Button(onClick = { dialog = "tuya" }) { Text(str("desktop_connect")) }
            }
        }
    }

    // --- Demo --- (telefonda "Demo ve Pro"; masaüstünde Pro her zaman açık)
    SectionTitle(str("demo_short"))
    SectionCard {
        SwitchInfoRow(Icons.Rounded.Science, str("demo_devices"), str("demo_devices_desc"), demo) { on ->
            graph.engine.setDemo(on, strings.demoNames()) // motor üzerinden; hesap kalmadıysa izleme de durur
        }
    }

    when (dialog) {
        "tuya" -> TuyaDialog(graph, strings) { dialog = null }
        "tuya-remove" -> Confirm(strings, strings["desktop_tuya_disconnect_confirm"], strings["desktop_remove"], onDismiss = { dialog = null }) {
            TuyaConnect(store).disconnect()
            if (!store.hasAnyAccount) graph.engine.stop() else applyEngineSettings(graph)
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
