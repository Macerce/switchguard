package com.macerce.switchguard.desktop.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.DoorFront
import androidx.compose.material.icons.rounded.ElectricalServices
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Power
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.Workspaces
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.macerce.switchguard.core.DeviceKind
import com.macerce.switchguard.data.ConnState

// Telefondaki ui/Components.kt (2.12) karşılığı. Farklar: TV odak halkası yok (fare ve klavye: Material'ın kendi
// odak/üzerine gelme katmanı yeter); metinler LocalStrings'ten ([str]). Sekme başlıkları TopAppBar yerine [PageHeader].

/**
 * Sekme ve alt sayfa başlığı: büyük, kalın başlık; solda isteğe bağlı geri oku, sağda eylemler. Masaüstünde pencere
 * geniş olduğundan Material TopAppBar yerine sade bir satır (yükseklik ve kaydırma davranışı gerekmez).
 */
@Composable
fun PageHeader(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    /** Alt sayfalarda (geri oku olan) başlık bir boy küçük. */
    small: Boolean = onBack != null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier.fillMaxWidth().padding(start = if (onBack != null) 12.dp else 28.dp, end = 16.dp, top = 22.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            BackButton(onBack)
            Spacer(Modifier.width(4.dp))
        }
        Text(
            title,
            style = if (small) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        actions()
    }
}

/** Bölüm başlığı. */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 8.dp),
    )
}

/** Yuvarlatılmış kart içinde gruplanmış satırlar. */
@Composable
fun SectionCard(modifier: Modifier = Modifier, horizontalPadding: Dp = 16.dp, content: @Composable () -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth().padding(horizontal = horizontalPadding),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = tileColor()),
        border = androidx.compose.foundation.BorderStroke(1.dp, cardStroke()),
    ) {
        Column { content() }
    }
}

/** Yuvarlak zemin üzerinde simge. */
@Composable
fun IconBadge(icon: ImageVector, tint: Color, background: Color, size: Dp = 44.dp) {
    Box(Modifier.size(size).background(background, CircleShape), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.55f))
    }
}

/** Renkli nokta + metin. */
@Composable
fun StatusDot(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).background(color, CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun OnlineDot(online: Boolean) = StatusDot(
    if (online) StatusColors.online else StatusColors.offline,
    str(if (online) "label_online_title" else "label_offline_title"),
)

/** Bağlantı durumunun rengi (ekrandaki noktalar; tepsi simgesi [StatusColors.of]). */
fun connColor(state: ConnState): Color = when (state) {
    ConnState.LIVE -> StatusColors.online
    ConnState.POLLING, ConnState.CONNECTING -> StatusColors.warning
    ConnState.STOPPED -> Color.Gray
    else -> StatusColors.offline
}

/** Bağlantı durumunun kısa adının metin anahtarı. */
fun connTitle(state: ConnState): String = when (state) {
    ConnState.LIVE -> "state_live_title"
    ConnState.POLLING -> "state_polling_title"
    ConnState.CONNECTING -> "state_connecting"
    ConnState.NO_INTERNET -> "state_no_internet"
    ConnState.AUTH_ERROR -> "state_auth_error"
    ConnState.ERROR -> "state_error"
    ConnState.STOPPED -> "state_stopped"
}

/** Kart içinde iki satırlık bilgi: başlık + açıklama, solda simge, sağda isteğe bağlı içerik. */
@Composable
fun InfoRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        trailing?.invoke()
    }
}

/** Ayarlar menüsündeki kategori satırının simge kutusu renkleri. */
enum class Hue(val light: Color, val dark: Color) {
    TEAL(Color(0xFF00897B), Color(0xFF26A69A)),
    BLUE(Color(0xFF1E88E5), Color(0xFF42A5F5)),
    INDIGO(Color(0xFF5C6BC0), Color(0xFF7986CB)),
    PURPLE(Color(0xFF8E24AA), Color(0xFFAB47BC)),
    RED(Color(0xFFE53935), Color(0xFFEF5350)),
    ORANGE(Color(0xFFF4511E), Color(0xFFFF7043)),
    AMBER(Color(0xFFF9A825), Color(0xFFFBC02D)),
    GREEN(Color(0xFF43A047), Color(0xFF66BB6A)),
    SLATE(Color(0xFF546E7A), Color(0xFF78909C)),
}

/** Renkli, köşeleri yuvarlatılmış kare içinde beyaz simge (akıllı ev uygulamalarının menü dili). */
@Composable
fun HueIcon(icon: ImageVector, hue: Hue, size: Dp = 36.dp) {
    val dark = LocalDarkTheme.current
    Box(
        Modifier.size(size).background(if (dark) hue.dark else hue.light, RoundedCornerShape(size * 0.3f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(size * 0.58f))
    }
}

/**
 * Menü satırı: renkli simge, başlık, kısa durum özeti ve sağda ok (ya da [trailing]).
 * [warning] doluysa özetin yerine uyarı rengiyle o yazılır (ör. "2 izin eksik").
 */
@Composable
fun MenuRow(
    icon: ImageVector,
    hue: Hue,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    warning: String? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier
            .clickable(onClick = onClick, role = Role.Button)
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HueIcon(icon, hue)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val line = warning ?: summary
            if (line != null) {
                Text(
                    line,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (warning != null) StatusColors.warning else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        if (trailing != null) trailing()
        else Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Cihaz türünün simgesi. */
fun DeviceKind.icon(): ImageVector = when (this) {
    DeviceKind.LIGHT -> Icons.Rounded.Lightbulb
    DeviceKind.WATER -> Icons.Rounded.WaterDrop
    DeviceKind.COLD -> Icons.Rounded.AcUnit
    DeviceKind.AIR -> Icons.Rounded.Air
    DeviceKind.HEAT -> Icons.Rounded.LocalFireDepartment
    DeviceKind.DOOR -> Icons.Rounded.DoorFront
    DeviceKind.PLUG -> Icons.Rounded.Power
    DeviceKind.RELAYS -> Icons.Rounded.ElectricalServices
    DeviceKind.SWITCH -> Icons.Rounded.PowerSettingsNew
    DeviceKind.GROUP -> Icons.Rounded.Workspaces
    DeviceKind.CATEGORY -> Icons.AutoMirrored.Rounded.Label
}

/**
 * Yuvarlak güç düğmesi. Açıkken dolu (marka rengi ya da [onBrand] zeminde beyaz), kapalıyken çerçeveli.
 * İstek sürerken çevresinde ilerleme halkası döner, basış yok sayılır. Çevrimdışı cihazda pasiftir.
 */
@Composable
fun PowerButton(
    on: Boolean,
    busy: Boolean,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    onBrand: Boolean = false,
    label: String? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val fill by animateColorAsState(
        when {
            !enabled -> Color.Transparent
            on && onBrand -> Color.White
            on -> scheme.primary
            onBrand -> Color.White.copy(alpha = 0.18f)
            else -> Color.Transparent
        },
        label = "powerFill",
    )
    val tint = when {
        !enabled -> scheme.outline
        on && onBrand -> Brand.deep
        on -> scheme.onPrimary
        onBrand -> Color.White
        else -> scheme.onSurfaceVariant
    }
    val edge = when {
        on && enabled -> Color.Transparent
        onBrand -> Color.White.copy(alpha = 0.6f)
        else -> scheme.outlineVariant
    }
    val stateText = str(if (on) "label_on" else "label_off")
    val description = label ?: str("power")
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(fill)
            .border(1.5.dp, edge, CircleShape)
            .clickable(enabled = enabled, role = Role.Switch) { if (!busy) onToggle(!on) }
            .semantics {
                contentDescription = description
                stateDescription = stateText
            },
        contentAlignment = Alignment.Center,
    ) {
        if (label != null && size >= 56.dp) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.PowerSettingsNew, null, tint = tint, modifier = Modifier.size(size * 0.34f))
                Text(label, style = MaterialTheme.typography.labelMedium, color = tint, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        } else {
            Icon(Icons.Rounded.PowerSettingsNew, null, tint = tint, modifier = Modifier.size(size * 0.5f))
        }
        if (busy) {
            CircularProgressIndicator(
                Modifier.size(size - 4.dp),
                strokeWidth = 2.dp,
                color = when {
                    onBrand && on -> Brand.deep
                    onBrand -> Color.White
                    on && enabled -> scheme.onPrimary
                    else -> scheme.primary
                },
            )
        }
    }
}

/** Alt sayfaların geri oku. */
@Composable
fun BackButton(onBack: () -> Unit) {
    IconButton(onClick = onBack) {
        Icon(Icons.AutoMirrored.Rounded.ArrowBack, str("action_back"))
    }
}

/** Kart içindeki satırların ayracı: ince ve (simgeli satırlarda) simgeden sonra başlar. */
@Composable
fun RowDivider(inset: Dp = 56.dp) = HorizontalDivider(
    Modifier.padding(start = inset),
    thickness = 1.dp,
    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (LocalDarkTheme.current) 0.7f else 0.6f),
)
