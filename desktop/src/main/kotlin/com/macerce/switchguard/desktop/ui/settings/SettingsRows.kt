package com.macerce.switchguard.desktop.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import com.macerce.switchguard.desktop.ui.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.macerce.switchguard.desktop.ui.InfoRow
import com.macerce.switchguard.desktop.ui.str

// Ayarlar sayfalarının satırları. Kart içindeki satırlar ortak InfoRow'dur (telefondaki gibi); burada yalnızca
// masaüstüne uyarlanmış çeşitleri var: satırın tamamı anahtarı değiştirir, seçenekler radyo düğmeli pencerede.

/**
 * Kart içinde anahtarlı satır (telefondaki `InfoRow { Switch }`). Satırın tamamı tıklanır: fareyle de klavyeyle
 * (Boşluk) de kolay, küçük anahtarı tutturmak gerekmez.
 */
@Composable
fun SwitchInfoRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    enabled: Boolean = true,
    onChange: (Boolean) -> Unit,
) {
    InfoRow(
        icon, title, subtitle,
        Modifier.toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onChange),
    ) {
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

/**
 * Seçenek penceresi (telefondaki `ChoiceDialog`): açıklama ve radyo düğmeli seçenekler. Bir seçeneğe tıklamak onu
 * kaydeder ve pencereyi kapatır; Esc ya da "İptal" vazgeçer.
 */
@Composable
fun <T> ChoiceDialog(
    title: String,
    description: String?,
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onPick: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState())) {
                if (!description.isNullOrEmpty()) {
                    Text(description, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 12.dp))
                }
                options.forEach { o ->
                    Row(
                        Modifier.fillMaxWidth()
                            .selectable(selected = o == selected, role = Role.RadioButton, onClick = { onPick(o); onDismiss() })
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = o == selected, onClick = null)
                        Text(label(o), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 12.dp))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(str("action_cancel")) } },
    )
}

/** Sayfa başındaki ya da kart altındaki kısa açıklama (telefondaki `PageIntro`). */
@Composable
fun PageIntro(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 4.dp),
    )
}

// ---------------------------------------------------------------- eski satırlar
// Cihaz seçenekleri penceresi (DeviceOptionsDialog) bunları kullanıyor; o pencere cihaz sayfasına taşınınca kalkabilir.

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
