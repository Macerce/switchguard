package com.macerce.switchguard.ui

import android.app.Activity
import android.app.TimePickerDialog
import android.widget.Toast
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.text.format.DateFormat as AndroidDateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Summarize
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
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
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.currentStateAsState
import com.macerce.switchguard.BuildConfig
import com.macerce.switchguard.R
import com.macerce.switchguard.data.Store
import com.macerce.switchguard.service.SummaryScheduler
import kotlinx.coroutines.launch
import com.macerce.switchguard.service.MonitorService
import java.util.Calendar

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
            SectionCard {
                InfoRow(
                    Icons.Rounded.AccountCircle,
                    if (s.isLoggedIn) s.accountEmail.ifEmpty { stringResource(R.string.logged_in) } else stringResource(R.string.not_logged_in),
                    if (s.isLoggedIn) stringResource(R.string.region_x, s.region.uppercase()) else null,
                    Modifier.clickable { LoginActivity.start(context) },
                ) {
                    TextButton(onClick = { LoginActivity.start(context) }) {
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
                        modifier = Modifier.padding(start = 40.dp),
                    )
                }
                Divider()
                InfoRow(Icons.Rounded.Vibration, stringResource(R.string.vibrate)) {
                    Switch(checked = s.vibrate, onCheckedChange = { store.vibrate = it })
                }
                Divider()
                InfoRow(
                    Icons.Rounded.NotificationsActive,
                    stringResource(R.string.test_alarm),
                    stringResource(if (s.monitoringEnabled) R.string.test_alarm_desc else R.string.test_alarm_needs_monitoring),
                    Modifier.clickable(enabled = s.monitoringEnabled) { Actions.testAlarm(context) },
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
                if (Actions.isXiaomi()) {
                    Divider()
                    InfoRow(
                        Icons.Rounded.RestartAlt,
                        stringResource(R.string.perm_autostart),
                        stringResource(R.string.perm_autostart_desc),
                        Modifier.clickable { Actions.openAutostartSettings(context) },
                    )
                }
            }

            // ---------------------------------------------------------------- Hakkında
            SectionTitle(stringResource(R.string.section_about))
            SectionCard {
                InfoRow(
                    Icons.Rounded.School, stringResource(R.string.setup_guide), stringResource(R.string.setup_guide_desc),
                    Modifier.clickable { store.onboardingDone = false },
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

@Composable
private fun graceLabel(sec: Int): String = when {
    sec == 0 -> stringResource(R.string.grace_off)
    sec < 60 -> stringResource(R.string.seconds_n, sec)
    else -> stringResource(R.string.minutes_n, sec / 60)
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
private fun <T> ChoiceDialog(
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
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
fun ConfirmDialog(title: String, body: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = { TextButton(onClick = { onConfirm(); onDismiss() }) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
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
        text = { CredentialFields(appId, { appId = it }, secret, { secret = it }, redirect, { redirect = it }) },
        confirmButton = {
            TextButton(onClick = {
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
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
fun CredentialFields(
    appId: String, onAppId: (String) -> Unit,
    secret: String, onSecret: (String) -> Unit,
    redirect: String, onRedirect: (String) -> Unit,
) {
    Column {
        OutlinedTextField(appId, onAppId, label = { Text("App ID") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(
            secret, onSecret, label = { Text("App Secret") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
        OutlinedTextField(
            redirect, onRedirect, label = { Text("Redirect URL") }, singleLine = true,
            supportingText = { Text(stringResource(R.string.redirect_hint)) },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
    }
}
