package com.macerce.switchguard.ui

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.NotificationsPaused
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.ScreenLockPortrait
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.macerce.switchguard.R
import com.macerce.switchguard.core.ReadinessInput
import com.macerce.switchguard.core.ReadinessIssue
import com.macerce.switchguard.data.Store
import com.macerce.switchguard.service.Notifier

/** Telefonun alarmla ilgili ayarlarını okur (bkz. [com.macerce.switchguard.core.Readiness]). */
fun readinessInput(context: Context, store: Store): ReadinessInput {
    val nm = context.getSystemService(NotificationManager::class.java)
    val audio = context.getSystemService(AudioManager::class.java)
    val channel = nm.getNotificationChannel(Notifier.CH_ALARM)
    return ReadinessInput(
        notificationsEnabled = Actions.notificationsEnabled(context),
        // Kanal henüz oluşmadıysa uygulamanın verdiği önem (yüksek) geçerlidir.
        alarmChannelHigh = channel == null || channel.importance >= NotificationManager.IMPORTANCE_HIGH,
        dndTotalSilence = nm.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_NONE,
        alarmVolume = audio.getStreamVolume(AudioManager.STREAM_ALARM),
        alarmVolumeMax = audio.getStreamMaxVolume(AudioManager.STREAM_ALARM),
        alarmBoost = store.alarmBoost,
        batteryExempt = Actions.batteryExempt(context),
        fullScreenAllowed = Actions.fullScreenAllowed(context),
        overlayAllowed = if (AlarmActivity.needsOverlayPermission(context)) AlarmActivity.canShowOverApps(context) else null,
        lockScreenPopupAllowed = XiaomiPermissions.lockScreenPopup(context),
        // Xiaomi'de gerçek durum okunur; okunamayan markalarda kullanıcının onayı geçerlidir.
        autostartConfirmed = OemBackground.current(context)?.let { XiaomiPermissions.autostart(context) ?: store.autostartConfirmed },
        runningAsTwin = TwinCopies.runningAsTwin(context),
        otherCopyInstalled = TwinCopies.otherCopyInstalled(context),
        twinAcknowledged = store.twinAcknowledged,
    )
}

/**
 * Ana ekrandaki "Alarm size ulaşmayabilir" kartı: alarmı engelleyebilecek her ayar için kısa açıklama ve
 * tek dokunuşla düzeltme. Sorun yoksa hiç görünmez.
 */
@Composable
fun ReadinessCard(issues: List<ReadinessIssue>) {
    if (issues.isEmpty()) return
    val context = LocalContext.current
    val store = Store.get(context)
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.WarningAmber, null, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.ready_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.padding(top = 6.dp))
            Text(stringResource(R.string.ready_body), style = MaterialTheme.typography.bodyMedium)
            issues.forEach { issue ->
                HorizontalDivider(Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.15f))
                IssueRow(issue, context, store)
            }
        }
    }
}

@Composable
private fun IssueRow(issue: ReadinessIssue, context: Context, store: Store) {
    val oem = OemBackground.current(context)
    val (icon, title, desc) = when (issue) {
        ReadinessIssue.TWIN_THIS_COPY ->
            Triple(Icons.Rounded.ContentCopy, stringResource(R.string.ready_twin_this), stringResource(R.string.ready_twin_this_desc))
        ReadinessIssue.TWIN_OTHER_COPY ->
            Triple(Icons.Rounded.ContentCopy, stringResource(R.string.ready_twin_other), stringResource(R.string.ready_twin_other_desc))
        ReadinessIssue.NOTIFICATIONS_OFF ->
            Triple(Icons.Rounded.NotificationsOff, stringResource(R.string.ready_notifications), stringResource(R.string.ready_notifications_desc))
        ReadinessIssue.ALARM_CHANNEL_MUTED ->
            Triple(Icons.Rounded.NotificationsPaused, stringResource(R.string.ready_channel), stringResource(R.string.ready_channel_desc))
        ReadinessIssue.DND_TOTAL_SILENCE ->
            Triple(Icons.Rounded.DoNotDisturbOn, stringResource(R.string.ready_dnd), stringResource(R.string.ready_dnd_desc))
        ReadinessIssue.ALARM_VOLUME_LOW ->
            Triple(Icons.AutoMirrored.Rounded.VolumeOff, stringResource(R.string.ready_volume), stringResource(R.string.ready_volume_desc))
        ReadinessIssue.BATTERY_RESTRICTED ->
            Triple(Icons.Rounded.BatteryAlert, stringResource(R.string.ready_battery), stringResource(R.string.ready_battery_desc))
        ReadinessIssue.FULL_SCREEN_DENIED ->
            Triple(Icons.Rounded.Fullscreen, stringResource(R.string.ready_full_screen), stringResource(R.string.ready_full_screen_desc))
        ReadinessIssue.LOCK_SCREEN_POPUP_DENIED ->
            Triple(Icons.Rounded.ScreenLockPortrait, stringResource(R.string.ready_lock_popup), stringResource(R.string.ready_lock_popup_desc))
        ReadinessIssue.OVERLAY_DENIED ->
            Triple(Icons.Rounded.Layers, stringResource(R.string.perm_overlay), stringResource(R.string.perm_overlay_desc))
        ReadinessIssue.AUTOSTART_UNCONFIRMED ->
            Triple(Icons.Rounded.RestartAlt, stringResource(R.string.perm_autostart, oem?.label.orEmpty()), oem?.let { stringResource(it.hint) }.orEmpty())
    }
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, null, modifier = Modifier.padding(top = 2.dp).size(22.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(desc, style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                Button(
                    onClick = { fix(issue, context, store) },
                    modifier = Modifier.tvFocusRing(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) {
                    Text(
                        stringResource(
                            when (issue) {
                                ReadinessIssue.TWIN_THIS_COPY -> R.string.ready_twin_remove
                                ReadinessIssue.AUTOSTART_UNCONFIRMED, ReadinessIssue.TWIN_OTHER_COPY -> R.string.action_open_settings
                                else -> R.string.ready_fix
                            }
                        )
                    )
                }
                // Üretici ayarının durumu okunamıyorsa kullanıcı yaptığını onaylar (Xiaomi'de okunur, onay gerekmez).
                if (issue == ReadinessIssue.AUTOSTART_UNCONFIRMED && XiaomiPermissions.autostart(context) == null) {
                    TextButton(onClick = { store.autostartConfirmed = true }, modifier = Modifier.tvFocusRing()) {
                        Text(stringResource(R.string.ready_done), color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
                // İki kopya bilerek de kullanılabilir (her kopyada ayrı eWeLink hesabı): uyarı kalıcı olarak kapatılır.
                if (issue == ReadinessIssue.TWIN_THIS_COPY || issue == ReadinessIssue.TWIN_OTHER_COPY) {
                    TextButton(onClick = { store.twinAcknowledged = true }, modifier = Modifier.tvFocusRing()) {
                        Text(stringResource(R.string.ready_twin_keep), color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
        }
    }
}

private fun fix(issue: ReadinessIssue, context: Context, store: Store) {
    when (issue) {
        ReadinessIssue.TWIN_THIS_COPY -> TwinCopies.openThisCopyInfo(context)
        ReadinessIssue.TWIN_OTHER_COPY -> TwinCopies.openDualAppSettings(context)
        ReadinessIssue.NOTIFICATIONS_OFF -> Actions.openNotificationSettings(context)
        ReadinessIssue.ALARM_CHANNEL_MUTED -> runCatching {
            context.startActivity(
                Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    .putExtra(Settings.EXTRA_CHANNEL_ID, Notifier.CH_ALARM)
            )
        }.onFailure { Actions.openNotificationSettings(context) }
        ReadinessIssue.DND_TOTAL_SILENCE -> runCatching { context.startActivity(Intent("android.settings.ZEN_MODE_SETTINGS")) }
            .onFailure { runCatching { context.startActivity(Intent(Settings.ACTION_SOUND_SETTINGS)) } }
        ReadinessIssue.ALARM_VOLUME_LOW -> store.alarmBoost = true
        ReadinessIssue.BATTERY_RESTRICTED -> Actions.requestBatteryExemption(context)
        ReadinessIssue.FULL_SCREEN_DENIED -> Actions.openFullScreenSettings(context)
        ReadinessIssue.LOCK_SCREEN_POPUP_DENIED -> XiaomiPermissions.openOtherPermissions(context)
        ReadinessIssue.OVERLAY_DENIED -> AlarmActivity.openOverlaySettings(context)
        ReadinessIssue.AUTOSTART_UNCONFIRMED -> OemBackground.current(context)?.open(context)
    }
}
