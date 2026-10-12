package com.macerce.switchguard.ui

import android.app.Activity
import android.app.TimePickerDialog
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.text.format.DateFormat as AndroidDateFormat
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Snooze
import androidx.compose.material.icons.rounded.Summarize
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.SyncAlt
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.currentStateAsState
import com.macerce.switchguard.BuildConfig
import com.macerce.switchguard.R
import com.macerce.switchguard.core.AppLanguage
import com.macerce.switchguard.core.Cloud
import com.macerce.switchguard.data.Store
import com.macerce.switchguard.service.MonitorService
import com.macerce.switchguard.service.SummaryScheduler
import java.util.Calendar
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(modifier: Modifier) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val store = remember { Store.get(context) }
    val version by store.version.collectAsStateWithLifecycle()
    // İzin durumları uygulamaya dönünce yeniden okunsun.
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val resumed = lifecycle.isAtLeast(Lifecycle.State.RESUMED)

    var dialog by remember { mutableStateOf<(@Composable () -> Unit)?>(null) }

    val ringtonePicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            @Suppress("DEPRECATION")
            val uri: Uri? = result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            store.alarmSoundUri = uri?.toString()
        }
    }

    // version okunarak ayarlar her değişimde tazelenir.
    val s = remember(version, resumed) { store }
    val scope = rememberCoroutineScope()
    val soundName = remember(version) {
        val uri = s.alarmSoundUri?.let(Uri::parse)
            ?: RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM)
        uri?.let { runCatching { RingtoneManager.getRingtone(context, it)?.getTitle(context) }.getOrNull() }
            ?: resources.getString(R.string.default_sound)
    }

    Column(modifier.fillMaxSize()) {
        TopAppBar(title = { Text(stringResource(R.string.tab_settings), fontWeight = FontWeight.SemiBold) })
        Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {

            // ---------------------------------------------------------------- Hesap
            SectionTitle(stringResource(R.string.section_account))
            if (s.transferSource.isNotEmpty() && s.hasCredentials) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) { SameAccountWarning(store) }
            }
            SectionCard {
                InfoRow(
                    Icons.Rounded.AccountCircle,
                    if (s.isLoggedIn) s.accountEmail.ifEmpty { stringResource(R.string.logged_in) } else stringResource(R.string.not_logged_in),
                    if (s.isLoggedIn) stringResource(R.string.region_x, s.region.uppercase()) else null,
                    Modifier.clickable { LoginActivity.start(context) },
                ) {
                    TextButton(modifier = Modifier.tvFocusRing(), onClick = { LoginActivity.start(context) }) {
                        Text(stringResource(if (s.isLoggedIn) R.string.action_relogin else R.string.action_login))
                    }
                }
                Divider()
                InfoRow(
                    Icons.Rounded.Key,
                    stringResource(R.string.api_credentials),
                    if (s.hasCredentials) stringResource(R.string.app_id_x, s.appId.take(6) + "…") else stringResource(R.string.not_set),
                    Modifier.clickable { dialog = { CredentialsDialog(store) { dialog = null } } },
                )
                if (s.isLoggedIn) {
                    Divider()
                    InfoRow(
                        Icons.AutoMirrored.Rounded.Logout,
                        stringResource(R.string.action_logout),
                        modifier = Modifier.clickable {
                            dialog = {
                                ConfirmDialog(
                                    title = stringResource(R.string.logout_title),
                                    body = stringResource(R.string.logout_body),
                                    confirm = stringResource(R.string.action_logout),
                                    onConfirm = {
                                        Actions.stopMonitoring(context)
                                        store.logout()
                                    },
                                    onDismiss = { dialog = null },
                                )
                            }
                        },
                    )
                }
                Divider()
                InfoRow(
                    Icons.Rounded.Hub,
                    stringResource(R.string.tuya_account),
                    if (s.hasTuya) s.snapshots.count { it.cloud == Cloud.TUYA }.let { n ->
                        pluralStringResource(R.plurals.tuya_connected_x, n, regionLabel(s.tuyaRegion), n)
                    } else stringResource(R.string.tuya_not_connected),
                    Modifier.clickable { dialog = { TuyaDialog(store) { dialog = null } } },
                )
                Divider()
                InfoRow(
                    Icons.Rounded.SyncAlt,
                    stringResource(R.string.transfer_send_row),
                    stringResource(R.string.transfer_send_row_desc),
                    Modifier.clickable { dialog = { SendTransferDialog { dialog = null } } },
                )
                Divider()
                InfoRow(
                    Icons.Rounded.Download,
                    stringResource(R.string.transfer_receive_row),
                    stringResource(R.string.transfer_receive_row_desc),
                    Modifier.clickable {
                        dialog = {
                            AlertDialog(
                                onDismissRequest = { dialog = null },
                                title = { Text(stringResource(R.string.onb_transfer_title)) },
                                text = {
                                    Column {
                                        ReceiveTransferPanel(onReceived = {
                                            // Gelen anahtarlar görünsün/onaylansın; ardından bu cihazın kendi hesabıyla giriş.
                                            if (store.hasCredentials) dialog = { CredentialsDialog(store) { dialog = null } }
                                        })
                                    }
                                },
                                confirmButton = {
                                    TextButton(onClick = { dialog = null }, modifier = Modifier.tvFocusRing()) {
                                        Text(stringResource(R.string.action_done))
                                    }
                                },
                            )
                        }
                    },
                )
                Divider()
                InfoRow(Icons.Rounded.Science, stringResource(R.string.demo_devices), stringResource(R.string.demo_devices_desc)) {
                    Switch(checked = s.demoMode, onCheckedChange = {
                        if (it) Actions.enableDemo(context) else Actions.disableDemo(context)
                    })
                }
                Divider()
                ProRow(s.isPro) { dialog = { ProDialog(onDismiss = { dialog = null }) } }
            }

            // ---------------------------------------------------------------- İzleme
            SectionTitle(stringResource(R.string.section_monitoring))
            SectionCard {
                InfoRow(Icons.Rounded.Bolt, stringResource(R.string.always_awake), stringResource(R.string.always_awake_desc)) {
                    Switch(checked = s.alwaysAwake, onCheckedChange = {
                        store.alwaysAwake = it
                        if (store.monitoringEnabled) Actions.startMonitoring(context)
                    })
                }
                Divider()
                val graceOptions = listOf(0, 30, 60, 120, 300)
                InfoRow(
                    Icons.Rounded.Timer,
                    stringResource(R.string.offline_grace),
                    graceLabel(s.offlineGraceSec),
                    Modifier.clickable {
                        dialog = {
                            ChoiceDialog(
                                stringResource(R.string.offline_grace),
                                stringResource(R.string.offline_grace_desc),
                                graceOptions, s.offlineGraceSec, { graceLabel(it) },
                                onPick = { store.offlineGraceSec = it }, onDismiss = { dialog = null },
                            )
                        }
                    },
                )
                Divider()
                val blindOptions = listOf(0, 5, 10, 15, 30, 60)
                InfoRow(
                    Icons.Rounded.WifiOff,
                    stringResource(R.string.blind_alert),
                    blindLabel(s.blindAlertMin),
                    Modifier.clickable {
                        dialog = {
                            ChoiceDialog(
                                stringResource(R.string.blind_alert),
                                stringResource(R.string.blind_alert_desc),
                                blindOptions, s.blindAlertMin, { blindLabel(it) },
                                onPick = { store.blindAlertMin = it }, onDismiss = { dialog = null },
                            )
                        }
                    },
                )
                Divider()
                val pollOptions = listOf(15, 30, 60, 120, 300)
                InfoRow(
                    Icons.Rounded.Sync,
                    stringResource(R.string.poll_interval),
                    stringResource(R.string.poll_interval_value, s.pollIntervalSec),
                    Modifier.clickable {
                        dialog = {
                            ChoiceDialog(
                                stringResource(R.string.poll_interval),
                                stringResource(R.string.poll_interval_desc),
                                pollOptions, s.pollIntervalSec, { resources.getString(R.string.poll_interval_value, it) },
                                onPick = { store.pollIntervalSec = it }, onDismiss = { dialog = null },
                            )
                        }
                    },
                )
            }

            // ---------------------------------------------------------------- Alarm
            SectionTitle(stringResource(R.string.section_alarm))
            SectionCard {
                InfoRow(
                    Icons.Rounded.MusicNote,
                    stringResource(R.string.alarm_sound),
                    soundName,
                    Modifier.clickable {
                        ringtonePicker.launch(
                            Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                                .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM or RingtoneManager.TYPE_RINGTONE)
                                .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                                .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                .putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, s.alarmSoundUri?.let(Uri::parse))
                        )
                    },
                )
                Divider()
                var volume by remember(version) { mutableFloatStateOf(s.alarmVolume) }
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Rounded.VolumeUp, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            stringResource(R.string.alarm_volume, (volume * 100).toInt()),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(start = 16.dp),
                        )
                    }
                    Slider(
                        value = volume,
                        onValueChange = { volume = it },
                        onValueChangeFinished = { store.alarmVolume = volume },
                        valueRange = 0.05f..1f,
                        modifier = Modifier.padding(start = 40.dp).tvSliderKeys()
                            .tvFocusRing(RoundedCornerShape(12.dp), inside = true),
                    )
                }
                Divider()
                InfoRow(Icons.Rounded.Vibration, stringResource(R.string.vibrate)) {
                    Switch(checked = s.vibrate, onCheckedChange = { store.vibrate = it })
                }
                Divider()
                val reminderOptions = listOf(0, 10, 30, 60)
                InfoRow(
                    Icons.Rounded.Snooze,
                    stringResource(R.string.reminder),
                    reminderLabel(s.reminderMin),
                    Modifier.clickable {
                        dialog = {
                            ChoiceDialog(
                                stringResource(R.string.reminder),
                                stringResource(R.string.reminder_desc),
                                reminderOptions, s.reminderMin, { reminderLabel(it) },
                                onPick = { store.reminderMin = it }, onDismiss = { dialog = null },
                            )
                        }
                    },
                )
                Divider()
                InfoRow(Icons.Rounded.Campaign, stringResource(R.string.alarm_boost), stringResource(R.string.alarm_boost_desc)) {
                    Switch(checked = s.alarmBoost, onCheckedChange = { store.alarmBoost = it })
                }
                Divider()
                InfoRow(
                    Icons.Rounded.NotificationsActive,
                    stringResource(R.string.test_alarm),
                    stringResource(R.string.test_alarm_desc),
                    Modifier.clickable { Actions.testAlarm(context) },
                )
            }

            // ---------------------------------------------------------------- Sessiz saatler
            SectionTitle(stringResource(R.string.section_quiet_hours))
            SectionCard {
                val q = s.quietHours
                InfoRow(Icons.Rounded.Bedtime, stringResource(R.string.quiet_hours), stringResource(R.string.quiet_hours_desc)) {
                    Switch(checked = q.enabled, onCheckedChange = { store.quietHours = q.copy(enabled = it) })
                }
                if (q.enabled) {
                    Divider()
                    InfoRow(
                        Icons.Rounded.Schedule, stringResource(R.string.quiet_start), formatMinute(context, q.startMinute),
                        Modifier.clickable { pickTime(context, q.startMinute) { store.quietHours = store.quietHours.copy(startMinute = it) } },
                    )
                    Divider()
                    InfoRow(
                        Icons.Rounded.Schedule, stringResource(R.string.quiet_end), formatMinute(context, q.endMinute),
                        Modifier.clickable { pickTime(context, q.endMinute) { store.quietHours = store.quietHours.copy(endMinute = it) } },
                    )
                }
            }

            // ---------------------------------------------------------------- Günlük özet
            SectionTitle(stringResource(R.string.section_summary))
            SectionCard {
                InfoRow(Icons.Rounded.Summarize, stringResource(R.string.daily_summary), stringResource(R.string.daily_summary_desc)) {
                    Switch(checked = s.dailySummaryEnabled, onCheckedChange = {
                        store.dailySummaryEnabled = it
                        SummaryScheduler.schedule(context)
                    })
                }
                if (s.dailySummaryEnabled) {
                    Divider()
                    InfoRow(
                        Icons.Rounded.Schedule, stringResource(R.string.daily_summary_time), formatMinute(context, s.dailySummaryMinute),
                        Modifier.clickable {
                            pickTime(context, s.dailySummaryMinute) {
                                store.dailySummaryMinute = it
                                SummaryScheduler.schedule(context)
                            }
                        },
                    )
                    Divider()
                    InfoRow(
                        Icons.Rounded.Visibility, stringResource(R.string.daily_summary_preview), null,
                        Modifier.clickable {
                            scope.launch {
                                if (!Actions.previewSummary(context)) {
                                    Toast.makeText(context, resources.getString(R.string.summary_no_data), Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                    )
                }
            }

            // ---------------------------------------------------------------- İzinler
            SectionTitle(stringResource(R.string.section_permissions))
            SectionCard {
                val notif = remember(resumed) { Actions.notificationsEnabled(context) }
                val battery = remember(resumed) { Actions.batteryExempt(context) }
                InfoRow(
                    if (notif) Icons.Rounded.NotificationsActive else Icons.Rounded.NotificationsOff,
                    stringResource(R.string.perm_notifications),
                    stringResource(if (notif) R.string.perm_granted else R.string.perm_missing),
                    Modifier.clickable { Actions.openNotificationSettings(context) },
                )
                Divider()
                InfoRow(
                    if (battery) Icons.Rounded.BatteryChargingFull else Icons.Rounded.BatteryAlert,
                    stringResource(R.string.perm_battery),
                    stringResource(if (battery) R.string.perm_battery_ok else R.string.perm_battery_missing),
                    Modifier.clickable(enabled = !battery) { Actions.requestBatteryExemption(context) },
                )
                val fullScreen = remember(resumed) { Actions.fullScreenAllowed(context) }
                if (fullScreen != null) {
                    Divider()
                    InfoRow(
                        Icons.Rounded.Fullscreen,
                        stringResource(R.string.perm_full_screen),
                        stringResource(if (fullScreen) R.string.perm_granted else R.string.perm_full_screen_missing),
                        Modifier.clickable(enabled = !fullScreen) { Actions.openFullScreenSettings(context) },
                    )
                }
                if (AlarmActivity.needsOverlayPermission(context)) {
                    val overlay = remember(resumed) { AlarmActivity.canShowOverApps(context) }
                    Divider()
                    InfoRow(
                        Icons.Rounded.Layers,
                        stringResource(R.string.perm_overlay),
                        stringResource(if (overlay) R.string.perm_granted else R.string.perm_missing),
                        Modifier.clickable(enabled = !overlay) { AlarmActivity.openOverlaySettings(context) },
                    )
                }
                OemBackground.current(context)?.let { oem ->
                    Divider()
                    InfoRow(
                        Icons.Rounded.RestartAlt,
                        stringResource(R.string.perm_autostart, oem.label),
                        stringResource(oem.hint),
                        Modifier.clickable { oem.open(context) },
                    )
                }
            }

            // ---------------------------------------------------------------- Genel
            SectionTitle(stringResource(R.string.section_general))
            SectionCard {
                val lang = remember(resumed) { AppLanguage.current(context) }
                InfoRow(
                    Icons.Rounded.Language,
                    stringResource(R.string.language),
                    languageLabel(lang),
                    Modifier.clickable {
                        dialog = {
                            ChoiceDialog(
                                stringResource(R.string.language),
                                stringResource(R.string.language_desc),
                                AppLanguage.options, lang, { languageLabel(it) },
                                onPick = { tag -> (context as? Activity)?.let { AppLanguage.set(it, tag) } },
                                onDismiss = { dialog = null },
                            )
                        }
                    },
                )
            }

            // ---------------------------------------------------------------- Yedekleme
            // TV'de dosya seçme/kaydetme ekranı (DocumentsUI) yok; kurulum oraya telefondan aktarılır.
            if (!LocalIsTv.current) BackupSection()

            // ---------------------------------------------------------------- Hakkında
            SectionTitle(stringResource(R.string.section_about))
            SectionCard {
                InfoRow(
                    Icons.Rounded.School, stringResource(R.string.setup_guide), stringResource(R.string.setup_guide_desc),
                    Modifier.clickable { store.onboardingDone = false },
                )
                Divider()
                InfoRow(
                    Icons.AutoMirrored.Rounded.MenuBook, stringResource(R.string.guide_ewelink), stringResource(R.string.guide_desc),
                    Modifier.clickable { Actions.openUrl(context, Actions.guideUrl(context, "ewelink")) },
                ) { Icon(Icons.AutoMirrored.Rounded.OpenInNew, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                Divider()
                InfoRow(
                    Icons.AutoMirrored.Rounded.MenuBook, stringResource(R.string.guide_tuya), stringResource(R.string.guide_desc),
                    Modifier.clickable { Actions.openUrl(context, Actions.guideUrl(context, "tuya")) },
                ) { Icon(Icons.AutoMirrored.Rounded.OpenInNew, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                Divider()
                InfoRow(
                    Icons.Rounded.Email, stringResource(R.string.contact), Actions.CONTACT_EMAIL,
                    Modifier.clickable {
                        if (!Actions.emailSupport(context)) {
                            Toast.makeText(context, Actions.CONTACT_EMAIL, Toast.LENGTH_LONG).show()
                        }
                    },
                )
                Divider()
                InfoRow(
                    Icons.Rounded.PrivacyTip, stringResource(R.string.privacy_policy), null,
                    Modifier.clickable { Actions.openUrl(context, Actions.PRIVACY_URL) },
                ) { Icon(Icons.AutoMirrored.Rounded.OpenInNew, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                Divider()
                InfoRow(
                    Icons.Rounded.Info,
                    stringResource(R.string.version_x, BuildConfig.VERSION_NAME),
                    stringResource(R.string.disclaimer),
                )
            }
        }
    }

    dialog?.invoke()
}

@Composable
private fun Divider() = HorizontalDivider(color = MaterialTheme.colorScheme.surface)

/** Dil adları her zaman kendi dilinde yazılır: yanlış dili seçen de geri dönebilsin. */
@Composable
private fun languageLabel(tag: String): String = when (tag) {
    "en" -> "English"
    "tr" -> "Türkçe"
    else -> stringResource(R.string.language_system)
}

@Composable
private fun reminderLabel(min: Int): String =
    if (min == 0) stringResource(R.string.blind_alert_off) else stringResource(R.string.reminder_every, min)

@Composable
private fun blindLabel(min: Int): String =
    if (min == 0) stringResource(R.string.blind_alert_off) else stringResource(R.string.dur_minutes, min)

@Composable
private fun graceLabel(sec: Int): String = when {
    sec == 0 -> stringResource(R.string.grace_off)
    sec < 60 -> pluralStringResource(R.plurals.seconds_n, sec, sec)
    else -> pluralStringResource(R.plurals.minutes_n, sec / 60, sec / 60)
}

fun formatMinute(context: android.content.Context, minute: Int): String {
    val cal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, minute / 60); set(Calendar.MINUTE, minute % 60)
    }
    return AndroidDateFormat.getTimeFormat(context).format(cal.time)
}

fun pickTime(context: android.content.Context, minute: Int, onPick: (Int) -> Unit) {
    TimePickerDialog(
        context, { _, h, m -> onPick(h * 60 + m) },
        minute / 60, minute % 60, AndroidDateFormat.is24HourFormat(context),
    ).show()
}

@Composable
fun <T> ChoiceDialog(
    title: String,
    description: String,
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
            Column {
                Text(description, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 12.dp))
                options.forEach { o ->
                    Row(
                        Modifier.fillMaxWidth()
                            .selectable(selected = o == selected, onClick = { onPick(o); onDismiss() })
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = o == selected, onClick = null)
                        Text(label(o), modifier = Modifier.padding(start = 12.dp))
                    }
                }
            }
        },
        confirmButton = { TextButton(modifier = Modifier.tvFocusRing(), onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
fun ConfirmDialog(title: String, body: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = { TextButton(modifier = Modifier.tvFocusRing(), onClick = { onConfirm(); onDismiss() }) { Text(confirm) } },
        dismissButton = { TextButton(modifier = Modifier.tvFocusRing(), onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
fun CredentialsDialog(store: Store, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var appId by remember { mutableStateOf(store.appId) }
    var secret by remember { mutableStateOf(store.appSecret) }
    var redirect by remember { mutableStateOf(store.redirectUrl) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.api_credentials)) },
        text = {
            // Sabit azami yükseklik: kaydırma alanı pencerenin ölçümüne bağlı kalırsa odaklı öğeyi görünür tutma
            // ile pencere ölçümü 1 px'lik yuvarlamayla birbirini tetikler ve içerik sürekli titrer.
            Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                CredentialFields(appId, { appId = it }, secret, { secret = it }, redirect, { redirect = it })
            }
        },
        confirmButton = {
            TextButton(modifier = Modifier.tvFocusRing(), onClick = {
                val changed = appId.trim() != store.appId || secret.trim() != store.appSecret
                store.appId = appId
                store.appSecret = secret
                store.redirectUrl = redirect
                // Başka bir App ID'nin token'ı yeni App ID ile çalışmaz.
                if (changed && store.isLoggedIn) {
                    store.logout()
                    if (store.monitoringEnabled) MonitorService.send(context, MonitorService.ACTION_RESTART)
                }
                onDismiss()
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(modifier = Modifier.tvFocusRing(), onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
fun CredentialFields(
    appId: String, onAppId: (String) -> Unit,
    secret: String, onSecret: (String) -> Unit,
    redirect: String, onRedirect: (String) -> Unit,
) {
    Column {
        TvKeyboardHint()
        OutlinedTextField(appId, onAppId, label = { Text("App ID") }, singleLine = true, keyboardOptions = tvKeyboardOptions(), modifier = Modifier.fillMaxWidth().tvTextField())
        OutlinedTextField(
            secret, onSecret, label = { Text("App Secret") }, singleLine = true, keyboardOptions = tvKeyboardOptions(),
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp).tvTextField(),
        )
        OutlinedTextField(
            redirect, onRedirect, label = { Text("Redirect URL") }, singleLine = true, keyboardOptions = tvKeyboardOptions(),
            supportingText = { Text(stringResource(R.string.redirect_hint)) },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp).tvTextField(),
        )
        // Alanlar önce (kumanda görünür alanlara gider), her cihazda ayrı eWeLink hesabı uyarısı hemen altında.
        Spacer(Modifier.height(12.dp))
        SameAccountWarning(Store.get(LocalContext.current))
    }
}
