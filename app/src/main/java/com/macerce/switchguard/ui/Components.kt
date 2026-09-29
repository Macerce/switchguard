package com.macerce.switchguard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.macerce.switchguard.R
import com.macerce.switchguard.data.ConnState
import com.macerce.switchguard.ui.theme.StatusColors

/** Bölüm başlığı. */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp),
    )
}

/** Yuvarlatılmış kart içinde gruplanmış satırlar. */
@Composable
fun SectionCard(modifier: Modifier = Modifier, horizontalPadding: Int = 16, content: @Composable () -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth().padding(horizontal = horizontalPadding.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column { content() }
    }
}

/** Yuvarlak zemin üzerinde simge. */
@Composable
fun IconBadge(icon: ImageVector, tint: Color, background: Color, size: Int = 44) {
    Box(
        Modifier.size(size.dp).background(background, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size((size * 0.55).dp))
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
    stringResource(if (online) R.string.label_online_title else R.string.label_offline_title),
)

fun connColor(state: ConnState): Color = when (state) {
    ConnState.LIVE -> StatusColors.online
    ConnState.POLLING, ConnState.CONNECTING -> StatusColors.warning
    ConnState.STOPPED -> Color.Gray
    else -> StatusColors.offline
}

fun connTitle(state: ConnState): Int = when (state) {
    ConnState.LIVE -> R.string.state_live_title
    ConnState.POLLING -> R.string.state_polling_title
    ConnState.CONNECTING -> R.string.state_connecting
    ConnState.NO_INTERNET -> R.string.state_no_internet
    ConnState.AUTH_ERROR -> R.string.state_auth_error
    ConnState.ERROR -> R.string.state_error
    ConnState.STOPPED -> R.string.state_stopped
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
