package com.macerce.switchguard.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.macerce.switchguard.core.AlertAction
import com.macerce.switchguard.ui.theme.Brand
import com.macerce.switchguard.ui.theme.raisedOnTile

/** Uyarı eyleminin rengi (tanıtım videosundaki gibi): Alarm kırmızı, Bildirim marka turkuazı, Yok nötr. */
@Composable
fun AlertAction.color(): Color = when (this) {
    AlertAction.ALARM -> Brand.alarm
    AlertAction.NOTIFY -> MaterialTheme.colorScheme.primary
    AlertAction.IGNORE -> raisedOnTile()
}

/** Kapsül içinde üçlü seçim: Alarm / Bildirim / Yok; seçili olan kendi renginde dolar. */
@Composable
fun AlertActionPicker(current: AlertAction, onChange: (AlertAction) -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier
            .height(40.dp)
            .clip(CircleShape)
            .border(1.dp, scheme.outlineVariant, CircleShape)
            .padding(3.dp)
            .selectableGroup(),
    ) {
        AlertAction.entries.forEach { a ->
            val selected = a == current
            val bg by animateColorAsState(if (selected) a.color() else Color.Transparent, label = "alertChoice")
            val fg = when {
                !selected -> scheme.onSurfaceVariant
                a == AlertAction.ALARM -> Brand.onAlarm
                a == AlertAction.NOTIFY -> scheme.onPrimary
                else -> scheme.onSurface
            }
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .tvFocusRing(CircleShape)
                    .clip(CircleShape)
                    .background(bg)
                    .selectable(selected = selected, role = Role.RadioButton) { onChange(a) }
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(a.title()),
                    style = MaterialTheme.typography.labelLarge,
                    color = fg,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * Uyarı satırı: solda simge + başlık, sağda üçlü seçim. Dar ekranda, büyük yazıda ya da seçenek adları uzunsa
 * (ör. Almanca "Benachrichtigen") seçim başlığın altına tam genişlikte iner. [below] başlık ile seçim arasına
 * (sensör sınırları gibi) ek alan koyar.
 */
@Composable
fun AlertChoiceRow(
    icon: ImageVector,
    title: String,
    current: AlertAction,
    onChange: (AlertAction) -> Unit,
    below: (@Composable () -> Unit)? = null,
) {
    val longest = AlertAction.entries.maxOf { stringResource(it.title()).length }
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        val inline = maxWidth >= 330.dp && fontScale <= 1.15f && longest <= 9
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(12.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                )
                if (inline) {
                    Spacer(Modifier.width(10.dp))
                    AlertActionPicker(current, onChange, Modifier.width(216.dp))
                }
            }
            if (below != null) {
                Spacer(Modifier.height(8.dp))
                below()
            }
            if (!inline) {
                Spacer(Modifier.height(10.dp))
                AlertActionPicker(current, onChange, Modifier.fillMaxWidth())
            }
        }
    }
}
