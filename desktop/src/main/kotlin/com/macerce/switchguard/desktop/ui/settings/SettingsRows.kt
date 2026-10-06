package com.macerce.switchguard.desktop.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/** Bölüm başlığı (telefondaki ayarlar ekranı gibi renkli, küçük). */
@Composable
fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 8.dp),
    )
}

/** Başlık + isteğe bağlı açıklama; sağda [trailing]. Tıklanabilirse [onClick]. */
@Composable
fun SettingRow(
    title: String,
    description: String? = null,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit = {},
) {
    Row(
        modifier.fillMaxWidth()
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (!description.isNullOrEmpty()) {
                Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        trailing()
    }
}

/** Satırın tamamı anahtarı değiştirir (yalnızca küçük anahtar değil): fareyle de klavyeyle de kolay. */
@Composable
fun SwitchRow(title: String, description: String? = null, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    SettingRow(
        title,
        description,
        Modifier.toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onChange),
    ) {
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

/** Seçenekli satır: tıklayınca seçenek menüsü açılır; sağda seçili değerin metni. */
@Composable
fun <T> ChoiceRow(
    title: String,
    description: String? = null,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        SettingRow(title, description, onClick = { open = true }) {
            Text(
                options.firstOrNull { it.first == selected }?.second ?: selected.toString(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, modifier = Modifier.padding(end = 24.dp)) {
            for ((value, label) in options) {
                DropdownMenuItem(text = { Text(label) }, onClick = {
                    open = false
                    onSelect(value)
                })
            }
        }
    }
}

/**
 * Kaydırıcı satırı. Sürüklerken yalnızca ekrandaki değer değişir ([title] o değerle yeniden yazılır); kalıcı kayıt
 * bırakınca bir kez yapılır ([onFinished]): her sürükleme adımında ayar dosyası yazılmasın.
 */
@Composable
fun SliderRow(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    tag: String,
    title: (Float) -> String,
    onFinished: (Float) -> Unit,
) {
    var local by remember(value) { mutableStateOf(value) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
        Text(title(local), style = MaterialTheme.typography.bodyLarge)
        Slider(
            value = local,
            onValueChange = { local = it.coerceIn(range) },
            onValueChangeFinished = { onFinished(local) },
            valueRange = range,
            modifier = Modifier.fillMaxWidth().testTag(tag),
        )
    }
}

@Composable
fun SectionDivider() {
    HorizontalDivider(Modifier.padding(top = 8.dp))
}
