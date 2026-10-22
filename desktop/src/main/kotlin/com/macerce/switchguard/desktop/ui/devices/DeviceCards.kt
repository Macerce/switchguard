package com.macerce.switchguard.desktop.ui.devices

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.AlarmOff
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.GppMaybe
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RemoveModerator
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.WifiOff
import com.macerce.switchguard.desktop.ui.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import com.macerce.switchguard.desktop.ui.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.macerce.switchguard.core.DeviceSnapshot
import com.macerce.switchguard.data.ConnState
import com.macerce.switchguard.desktop.ui.Brand
import com.macerce.switchguard.desktop.ui.IconBadge
import com.macerce.switchguard.desktop.ui.LocalDarkTheme
import com.macerce.switchguard.desktop.ui.LocalStrings
import com.macerce.switchguard.desktop.ui.SectionCard
import com.macerce.switchguard.desktop.ui.StatusColors
import com.macerce.switchguard.desktop.ui.connTitle
import com.macerce.switchguard.desktop.ui.plural
import com.macerce.switchguard.desktop.ui.str
import com.macerce.switchguard.desktop.ui.tileColor
import kotlinx.coroutines.delay

// Cihazlar ekranının üstündeki kartlar (telefondaki DevicesScreen.kt 2.12: AlarmBanner, GuardCard, LoginPromptCard,
// EmptyDevices). Masaüstüne özgü: hesap yokken Ayarlar'a ve demoya yönlendiren kart, oturum ve kör kalma uyarı
// kartları (telefonda bunlar bildirim olarak gelir; masaüstünde `AppState.warning` / `AppState.blind`).

/** Kapatılmamış alarm: satırlar ve büyük "Alarmı kapat" düğmesi (alarm penceresiyle aynı işlem). */
@Composable
fun AlarmBanner(lines: List<String>, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Alarm, null, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(12.dp))
                Text(
                    plural("alarm_banner_title", lines.size, lines.size),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(12.dp))
            lines.reversed().take(6).forEach {
                Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 2.dp))
            }
            if (lines.size > 6) {
                Text(str("alarm_banner_more", lines.size - 6), style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) {
                Icon(Icons.Rounded.AlarmOff, null)
                Spacer(Modifier.width(8.dp))
                Text(str("action_dismiss_alarm"), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

/**
 * Koruma kartı: izlemenin durumu, son güncelleme ve cihaz sayaçları. Canlıyken marka renginde, sorun varken
 * turuncu-kırmızı, izleme kapalıyken sade kart. Başlat/Durdur motora gider.
 */
@Composable
fun GuardCard(
    state: ConnState,
    detail: String,
    lastSync: Long,
    monitoring: Boolean,
    devices: List<DeviceSnapshot>,
    onStart: () -> Unit,
    onStop: () -> Unit,
) {
    val dark = LocalDarkTheme.current
    val strings = LocalStrings.current
    // "2 dk önce" metni kendiliğinden güncellensin.
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            delay(30_000)
            value = System.currentTimeMillis()
        }
    }
    val shown = if (monitoring) state else ConnState.STOPPED
    val problem = shown in setOf(ConnState.NO_INTERNET, ConnState.AUTH_ERROR, ConnState.ERROR)
    val branded = shown != ConnState.STOPPED
    val background: Brush = when {
        problem -> Brand.problem(dark)
        branded -> Brand.guard(dark)
        else -> SolidColor(tileColor())
    }
    val content = if (branded) Brand.onActive else MaterialTheme.colorScheme.onSurface
    val muted = if (branded) Brand.onActive.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant

    // Masaüstü: tek satır — durum (simge, başlık, açıklama), sayılar, sağda başlat/durdur.
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(background).padding(horizontal = 20.dp, vertical = 16.dp)
            .testTag("guard-card"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(44.dp).background(
                if (branded) Color.White.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceContainerHighest,
                RoundedCornerShape(8.dp),
            ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                when {
                    problem -> Icons.Rounded.GppMaybe
                    branded -> Icons.Rounded.Shield
                    else -> Icons.Rounded.RemoveModerator
                },
                contentDescription = null,
                tint = if (branded) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(26.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(str(connTitle(shown)), style = MaterialTheme.typography.titleLarge, color = content)
            val sub = when {
                !monitoring -> str("monitoring_off_hint")
                detail.isNotEmpty() -> detail
                // "now" 30 sn'de bir tazelenir; arada gelen senkron onu geçerse "… sonra" yazmasın.
                lastSync > 0 -> str("last_update", relativeAgo(maxOf(now, lastSync) - lastSync, strings))
                devices.isEmpty() -> plural("device_count", devices.size, devices.size)
                else -> str("guard_ready")
            }
            Text(sub, style = MaterialTheme.typography.bodyMedium, color = muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        if (devices.isNotEmpty()) {
            val on = devices.count { d -> d.online && d.switches.values.any { it } }
            val offline = devices.count { !it.online }
            val power = devices.filter { it.online && it.hasEnergy }.sumOf { it.power ?: 0.0 }
            Row(horizontalArrangement = Arrangement.spacedBy(28.dp), modifier = Modifier.padding(horizontal = 20.dp)) {
                Stat(devices.size.toString(), str("stat_devices"), content, muted)
                Stat(on.toString(), str("stat_on"), content, muted)
                Stat(offline.toString(), str("stat_offline"), if (offline > 0 && !branded) StatusColors.offline else content, muted)
                if (devices.any { it.hasEnergy }) Stat(watts(power), str("stat_power"), content, muted)
            }
        }
        if (monitoring) {
            OutlinedButton(
                onClick = onStop,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = content),
                border = BorderStroke(1.dp, if (branded) Color.White.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline),
                modifier = Modifier.testTag("monitoring-stop"),
            ) {
                Icon(Icons.Rounded.Stop, null, Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(str("action_stop_monitoring"))
            }
        } else {
            Button(onClick = onStart, modifier = Modifier.testTag("monitoring-start")) {
                Icon(Icons.Rounded.PlayArrow, null, Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(str("action_start_monitoring"))
            }
        }
    }
}

@Composable
private fun Stat(value: String, label: String, color: Color, muted: Color, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = color, maxLines = 1)
        Text(label, style = MaterialTheme.typography.labelMedium, color = muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/**
 * Hiç hesap yok (telefondaki LoginPromptCard'ın masaüstü hali): giriş Ayarlar › Hesaplar'da yapılır; hesapsız
 * denemek için demo cihazları açılabilir.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NoAccountCard(onAccounts: () -> Unit, onDemo: () -> Unit) {
    SectionCard(horizontalPadding = 0.dp) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(Icons.Rounded.Key, MaterialTheme.colorScheme.onPrimaryContainer, MaterialTheme.colorScheme.primaryContainer)
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(str("desktop_no_account_title"), style = MaterialTheme.typography.titleMedium)
                    Text(str("desktop_no_account_body"), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(16.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onAccounts, modifier = Modifier.testTag("add-account")) { Text(str("desktop_add_account")) }
                OutlinedButton(onClick = onDemo, modifier = Modifier.testTag("try-demo")) { Text(str("onb_platform_demo")) }
            }
        }
    }
}

/** Uyarı kartı (oturum düştü, izleme kör kaldı): kırmızımsı zemin, simge, başlık, metin ve isteğe bağlı düğme. */
@Composable
fun WarningCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    body: String,
    action: String? = null,
    onAction: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = tileColor()),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                IconBadge(icon, MaterialTheme.colorScheme.onErrorContainer, MaterialTheme.colorScheme.errorContainer)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                    Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (action != null) {
                Spacer(Modifier.height(12.dp))
                Button(onClick = onAction, modifier = Modifier.padding(start = 60.dp)) { Text(action) }
            }
        }
    }
}

/** Oturum düştü (eWeLink'e başka yerde giriş yapıldı ya da süre doldu): Ayarlar'dan yeniden giriş. */
@Composable
fun SessionLostCard(text: String, onRelogin: () -> Unit) = WarningCard(
    Icons.Rounded.Key, str("session_lost_title"), text, str("action_relogin"), onRelogin, Modifier.testTag("session-card"),
)

/** İzleme uzun süredir cihazları göremiyor (bkz. `Store.blindAlertMin`); düzelince kendiliğinden kalkar. */
@Composable
fun BlindCard(text: String) = WarningCard(Icons.Rounded.WifiOff, str("notif_blind_title"), text, modifier = Modifier.testTag("blind-card"))

/** Hesap var ama cihaz yok. */
@Composable
fun EmptyDevices(showTuyaHint: Boolean) {
    Column(Modifier.fillMaxWidth().padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Rounded.CloudOff, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        Text(str("no_devices"), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (showTuyaHint) {
            Spacer(Modifier.height(8.dp))
            Text(
                str("tuya_shared_hint"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 560.dp),
            )
        }
    }
}
