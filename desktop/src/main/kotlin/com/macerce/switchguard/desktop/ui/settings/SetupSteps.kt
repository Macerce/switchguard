package com.macerce.switchguard.desktop.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.macerce.switchguard.data.Store
import com.macerce.switchguard.desktop.i18n.Strings
import com.macerce.switchguard.desktop.platform.Links

/** Bir düğme: metni ve tıklanınca yapılan iş; iş kullanıcıya gösterilecek kısa bir not döndürebilir. */
class SetupAction(val label: String, val run: () -> String?)

/** Açılır kurulum anlatımı: numaralı adımlar ve altında bağlantı/kopyalama düğmeleri. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SetupSteps(title: String, steps: List<String>, actions: List<SetupAction>, initiallyOpen: Boolean = false) {
    var open by remember { mutableStateOf(initiallyOpen) }
    var note by remember { mutableStateOf<String?>(null) }
    Card(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            Modifier.fillMaxWidth().clickable { open = !open }.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
            Icon(if (open) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, contentDescription = null)
        }
        AnimatedVisibility(open) {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                steps.forEachIndexed { i, step ->
                    Row {
                        Text("${i + 1}.", Modifier.padding(end = 8.dp), style = MaterialTheme.typography.bodyMedium)
                        Text(step, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                if (actions.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (a in actions) OutlinedButton(onClick = { note = a.run() }) { Text(a.label) }
                    }
                }
                note?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
            }
        }
    }
}

/** Kurulum anlatımlarının adımları ve düğmeleri; metinler `desktop.xml`'de (spec-2 §7a). */
object Setup {
    fun tuyaSteps(strings: Strings) = (1..6).map { strings["desktop_tuya_step_$it"] }
    fun ewelinkSteps(strings: Strings) = (1..6).map { strings["desktop_ewelink_step_$it"] }
    fun windowsSteps(strings: Strings) = (1..8).map { strings["desktop_windows_step_$it"] }

    private fun link(strings: Strings, label: String, url: String) =
        SetupAction(strings[label]) { if (Links.open(url)) null else strings["desktop_link_copied"] }

    fun tuyaActions(strings: Strings) = listOf(
        link(strings, "desktop_open_tuya_platform", Links.TUYA_PLATFORM),
        link(strings, "desktop_open_guide", Links.guide("tuya", strings.language)),
        link(strings, "desktop_watch_video", Links.TUYA_VIDEO),
    )

    fun ewelinkActions(strings: Strings, store: Store) = listOf(
        link(strings, "desktop_open_ewelink_dev", Links.EWELINK_DEV),
        SetupAction(strings["desktop_copy_redirect"]) {
            Links.copyToClipboard(store.redirectUrl)
            strings["desktop_copied"]
        },
        link(strings, "desktop_open_guide", Links.guide("ewelink", strings.language)),
        link(strings, "desktop_watch_video", Links.EWELINK_VIDEO),
    )
}
