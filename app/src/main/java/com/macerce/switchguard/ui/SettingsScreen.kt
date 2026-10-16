package com.macerce.switchguard.ui

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.text.format.DateFormat as AndroidDateFormat
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.ErrorOutline
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
import androidx.compose.material.icons.rounded.ScreenLockPortrait
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Snooze
import androidx.compose.material.icons.rounded.Summarize
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.SyncAlt
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.currentStateAsState
import com.macerce.switchguard.BuildConfig
import com.macerce.switchguard.R
import com.macerce.switchguard.core.AppLanguage
import com.macerce.switchguard.core.Cloud
import com.macerce.switchguard.core.Entitlement
import com.macerce.switchguard.core.PhoneEntry
import com.macerce.switchguard.data.CrashLog
import com.macerce.switchguard.data.Store
import com.macerce.switchguard.service.MonitorService
import com.macerce.switchguard.service.SummaryScheduler
import com.macerce.switchguard.ui.theme.Brand
import com.macerce.switchguard.ui.theme.LocalDarkTheme
import com.macerce.switchguard.ui.theme.StatusColors
import com.macerce.switchguard.ui.theme.tileColor
import java.util.Calendar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Ayarlar alt sayfaları. Ana sayfa yalnızca bunların listesidir; her biri dokununca açılır. */
private enum class SettingsPage(val title: Int) {
    ACCOUNTS(R.string.settings_accounts),
    ALARM(R.string.section_alarm),
    QUIET(R.string.section_quiet_hours),
    SUMMARY(R.string.section_summary),
    MONITORING(R.string.section_monitoring),
    PERMISSIONS(R.string.section_permissions),
    BACKUP(R.string.settings_backup),
    ABOUT(R.string.settings_about),
}

/**
 * Ayarlar: üstte uygulama/plan kartı, altında gruplanmış kategoriler. Kategoriye dokununca kendi sayfası açılır;
 * geri tuşu (ya da sekmeye yeniden dokunmak) listeye döner. Kumandada listeye dönünce odak açılan satıra verilir.
 */
@Composable
fun SettingsScreen(modifier: Modifier, homeRequests: Int = 0) {
    var page by rememberSaveable { mutableStateOf<SettingsPage?>(null) }
    var lastOpened by rememberSaveable { mutableStateOf<SettingsPage?>(null) }
    var dialog by remember { mutableStateOf<(@Composable () -> Unit)?>(null) }
    val showDialog: ((@Composable () -> Unit)?) -> Unit = { dialog = it }
    BackHandler(enabled = page != null) { page = null }
    // Ayarlar sekmesine yeniden dokunulunca listeye dön (ilk değer sayılmaz).
    var seenRequests by rememberSaveable { mutableIntStateOf(homeRequests) }
    LaunchedEffect(homeRequests) {
        if (homeRequests != seenRequests) {
            seenRequests = homeRequests
            page = null
        }
    }

    AnimatedContent(
        targetState = page,
        transitionSpec = {
            // İleri: yeni sayfa sağdan gelir; geri: liste soldan.
            val forward = targetState != null
            (slideInHorizontally { w -> if (forward) w / 4 else -w / 4 } + fadeIn()) togetherWith
                (slideOutHorizontally { w -> if (forward) -w / 4 else w / 4 } + fadeOut())
        },
        modifier = modifier.fillMaxSize(),
        label = "settingsPage",
    ) { target ->
        if (target == null) {
            SettingsHome(
                focusPage = lastOpened,
                onFocusRestored = { lastOpened = null },
                onOpen = { lastOpened = it; page = it },
                showDialog = showDialog,
            )
        } else {
            SettingsSubPage(target, onBack = { page = null }, showDialog = showDialog)
        }
    }

    dialog?.invoke()
}

@Composable
private fun SettingsHome(
    focusPage: SettingsPage?,
    onFocusRestored: () -> Unit,
    onOpen: (SettingsPage) -> Unit,
    showDialog: ((@Composable () -> Unit)?) -> Unit,
) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val tv = LocalIsTv.current
    val store = remember { Store.get(context) }
    val version by store.version.collectAsStateWithLifecycle()
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val resumed = lifecycle.isAtLeast(Lifecycle.State.RESUMED)
    val s = remember(version, resumed) { store }

    // Kumanda: alt sayfadan dönünce odak o sayfanın satırına.
    val requesters = remember { SettingsPage.entries.associateWith { FocusRequester() } }
    LaunchedEffect(focusPage) {
        val target = focusPage ?: return@LaunchedEffect
        if (tv) {
            // Çıkan alt sayfa geçiş bitince kaldırılır; odağı ondan önce verirsek kaldırılırken sıfırlanabiliyor.
            delay(450)
            for (attempt in 1..20) {
                val ok = runCatching { requesters.getValue(target).requestFocus() }.getOrNull()
                if (ok != null && ok != false) break
                delay(50)
            }
        }
        onFocusRestored()
    }
    fun row(p: SettingsPage) = Modifier.focusRequester(requesters.getValue(p))

    Column(Modifier.fillMaxSize()) {
        AppTopBar(stringResource(R.string.tab_settings))
        Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
            PlanCard(s.isPro) { showDialog { ProDialog(onDismiss = { showDialog(null) }) } }

            SectionTitle(stringResource(R.string.settings_group_connections))
            SectionCard {
                MenuRow(
                    Icons.Rounded.Hub, Hue.TEAL, stringResource(R.string.settings_accounts),
                    onClick = { onOpen(SettingsPage.ACCOUNTS) }, modifier = row(SettingsPage.ACCOUNTS),
                    summary = accountsSummary(s),
                    warning = if (s.sessionLost) stringResource(R.string.session_lost_title) else null,
                )
            }

            SectionTitle(stringResource(R.string.settings_group_alerts))
            SectionCard {
                MenuRow(
                    Icons.Rounded.NotificationsActive, Hue.RED, stringResource(R.string.section_alarm),
                    onClick = { onOpen(SettingsPage.ALARM) }, modifier = row(SettingsPage.ALARM),
                    summary = alarmSummary(s),
                )
                Divider(68.dp)
                MenuRow(
                    Icons.Rounded.Bedtime, Hue.INDIGO, stringResource(R.string.section_quiet_hours),
                    onClick = { onOpen(SettingsPage.QUIET) }, modifier = row(SettingsPage.QUIET),
                    summary = s.quietHours.let { q ->
                        if (!q.enabled) stringResource(R.string.state_off_title)
                        else stringResource(R.string.time_range, formatMinute(context, q.startMinute), formatMinute(context, q.endMinute))
                    },
                )
                Divider(68.dp)
                MenuRow(
                    Icons.AutoMirrored.Rounded.Send, Hue.BLUE, stringResource(R.string.tg_title),
                    onClick = { showDialog { TelegramDialog(store) { showDialog(null) } } },
                    summary = escalationSummary(s),
                )
                Divider(68.dp)
                MenuRow(
                    Icons.Rounded.Summarize, Hue.AMBER, stringResource(R.string.section_summary),
                    onClick = { onOpen(SettingsPage.SUMMARY) }, modifier = row(SettingsPage.SUMMARY),
                    summary = if (s.dailySummaryEnabled) stringResource(R.string.daily_at, formatMinute(context, s.dailySummaryMinute))
                    else stringResource(R.string.state_off_title),
                )
            }

            SectionTitle(stringResource(R.string.settings_group_app))
            SectionCard {
                MenuRow(
                    Icons.Rounded.Sync, Hue.GREEN, stringResource(R.string.section_monitoring),
                    onClick = { onOpen(SettingsPage.MONITORING) }, modifier = row(SettingsPage.MONITORING),
                    summary = monitoringSummary(s.offlineGraceSec, resources.getString(R.string.poll_interval_value, s.pollIntervalSec)),
                )
                Divider(68.dp)
                val missing = remember(resumed, version) { missingPermissions(context) }
                MenuRow(
                    Icons.Rounded.VerifiedUser, Hue.ORANGE, stringResource(R.string.section_permissions),
                    onClick = { onOpen(SettingsPage.PERMISSIONS) }, modifier = row(SettingsPage.PERMISSIONS),
                    summary = stringResource(R.string.perm_all_ok),
                    warning = if (missing > 0) pluralStringResource(R.plurals.perm_missing_n, missing, missing) else null,
                )
                Divider(68.dp)
                MenuRow(
                    Icons.Rounded.Backup, Hue.PURPLE, stringResource(R.string.settings_backup),
                    onClick = { onOpen(SettingsPage.BACKUP) }, modifier = row(SettingsPage.BACKUP),
                    summary = stringResource(if (tv) R.string.settings_backup_desc_tv else R.string.settings_backup_desc),
                )
                Divider(68.dp)
                val lang = remember(resumed) { AppLanguage.current(context) }
                MenuRow(
                    Icons.Rounded.Language, Hue.SLATE, stringResource(R.string.language),
                    onClick = {
                        showDialog {
                            ChoiceDialog(
                                stringResource(R.string.language),
                                stringResource(R.string.language_desc),
                                AppLanguage.options, lang, { languageLabel(it) },
                                onPick = { tag -> (context as? Activity)?.let { AppLanguage.set(it, tag) } },
                                onDismiss = { showDialog(null) },
                            )
                        }
                    },
                    summary = languageLabel(lang),
                )
            }

            SectionTitle(stringResource(R.string.settings_group_support))
            SectionCard {
                MenuRow(
                    Icons.AutoMirrored.Rounded.HelpOutline, Hue.TEAL, stringResource(R.string.settings_about),
                    onClick = { onOpen(SettingsPage.ABOUT) }, modifier = row(SettingsPage.ABOUT),
                    summary = stringResource(R.string.settings_about_desc, BuildConfig.VERSION_NAME),
                )
            }
        }
    }
}

/** Ayarların başındaki uygulama kartı: logo, plan ve (ücretsizse) Pro'ya geçiş. */
@Composable
private fun PlanCard(isPro: Boolean, onClick: () -> Unit) {
    val dark = LocalDarkTheme.current
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            .tvFocusRing(RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .background(tileColor())
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(56.dp).clip(RoundedCornerShape(18.dp)).background(Brand.guard(dark)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Shield, null, tint = Color.White, modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge)
            Text(
                if (isPro) stringResource(R.string.plan_pro) else pluralStringResource(R.plurals.plan_free, Entitlement.FREE_DEVICES, Entitlement.FREE_DEVICES),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (isPro) {
            Icon(Icons.Rounded.WorkspacePremium, null, tint = MaterialTheme.colorScheme.primary)
        } else {
            Text(
                stringResource(R.string.pro_upgrade),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun accountsSummary(s: Store): String {
    val parts = buildList {
        if (s.isLoggedIn) add("eWeLink")
        if (s.hasTuya) add("Tuya")
        if (s.demoMode) add(stringResource(R.string.demo_short))
    }
    return if (parts.isEmpty()) stringResource(R.string.settings_accounts_none) else parts.joinToString(" · ")
}

@Composable
private fun alarmSummary(s: Store): String {
    val context = LocalContext.current
    val resources = LocalResources.current
    val soundUri = s.alarmSoundUri
    // Yalnızca seçilen ses değişince okunur: Ringtone her oluşturulduğunda bir MediaPlayer hazırlar.
    val name = remember(soundUri) { alarmSoundName(context, soundUri) ?: resources.getString(R.string.default_sound) }
    return name + " · " + stringResource(R.string.volume_pct, (s.alarmVolume * 100).toInt())
}

private fun alarmSoundName(context: android.content.Context, soundUri: String?): String? {
    val uri = soundUri?.let(Uri::parse) ?: RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM)
    return uri?.let { runCatching { RingtoneManager.getRingtone(context, it)?.getTitle(context) }.getOrNull() }
}

/** Alarmın ulaşması için gereken izinlerden kaçı eksik (cihazda geçerli olmayanlar sayılmaz). */
private fun missingPermissions(context: android.content.Context): Int {
    val checks = listOf(
        Actions.notificationsEnabled(context),
        Actions.batteryExempt(context),
        Actions.fullScreenAllowed(context),
        XiaomiPermissions.lockScreenPopup(context),
        if (AlarmActivity.needsOverlayPermission(context)) AlarmActivity.canShowOverApps(context) else null,
    )
    return checks.count { it == false }
}

@Composable
private fun SettingsSubPage(page: SettingsPage, onBack: () -> Unit, showDialog: ((@Composable () -> Unit)?) -> Unit) {
    val tv = LocalIsTv.current
    // Kumanda: sayfa açılınca odak içerikteki ilk öğeye.
    val content = remember { FocusRequester() }
    LaunchedEffect(page) {
        if (!tv) return@LaunchedEffect
        for (attempt in 1..20) {
            val ok = runCatching { content.requestFocus() }.getOrNull()
            if (ok != null && ok != false) break
            delay(50)
        }
    }
    Column(Modifier.fillMaxSize()) {
        AppTopBar(stringResource(page.title), navigationIcon = { BackButton(onBack) }, small = true)
        Column(
            Modifier.focusRequester(content).focusGroup()
                .verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
        ) {
            when (page) {
                SettingsPage.ACCOUNTS -> AccountsPage(showDialog)
                SettingsPage.ALARM -> AlarmPage(showDialog)
                SettingsPage.QUIET -> QuietPage()
                SettingsPage.SUMMARY -> SummaryPage()
                SettingsPage.MONITORING -> MonitoringPage(showDialog)
                SettingsPage.PERMISSIONS -> PermissionsPage()
                SettingsPage.BACKUP -> BackupPage(showDialog)
                SettingsPage.ABOUT -> AboutPage()
            }
        }
    }
}

/** Sayfa başındaki kısa açıklama. */
@Composable
private fun PageIntro(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 4.dp),
    )
}

@Composable
private fun settingsState(): Pair<Store, Int> {
    val context = LocalContext.current
    val store = remember { Store.get(context) }
    val version by store.version.collectAsStateWithLifecycle()
    return store to version
}

@Composable
private fun resumedState(): Boolean {
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    return lifecycle.isAtLeast(Lifecycle.State.RESUMED)
}

// ---------------------------------------------------------------- Hesaplar

@Composable
private fun AccountsPage(showDialog: ((@Composable () -> Unit)?) -> Unit) {
    val context = LocalContext.current
    val (store, version) = settingsState()
    val s = remember(version) { store }
    val close = { showDialog(null) }

    SectionTitle("eWeLink")
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
            Modifier.clickable { showDialog { CredentialsDialog(store, close) } },
        )
        if (s.isLoggedIn) {
            Divider()
            InfoRow(
                Icons.AutoMirrored.Rounded.Logout,
                stringResource(R.string.action_logout),
                modifier = Modifier.clickable {
                    showDialog {
                        ConfirmDialog(
                            title = stringResource(R.string.logout_title),
                            body = stringResource(R.string.logout_body),
                            confirm = stringResource(R.string.action_logout),
                            onConfirm = {
                                Actions.stopMonitoring(context)
                                store.logout()
                            },
                            onDismiss = close,
                        )
                    }
                },
            )
        }
    }

    SectionTitle(stringResource(R.string.tuya_account))
    SectionCard {
        InfoRow(
            Icons.Rounded.Hub,
            stringResource(R.string.tuya_account),
            if (s.hasTuya) s.snapshots.count { it.cloud == Cloud.TUYA }.let { n ->
                pluralStringResource(R.plurals.tuya_connected_x, n, regionLabel(s.tuyaRegion), n)
            } else stringResource(R.string.tuya_not_connected),
            Modifier.clickable { showDialog { TuyaDialog(store, close) } },
        )
    }

    SectionTitle(stringResource(R.string.settings_try))
    SectionCard {
        InfoRow(Icons.Rounded.Science, stringResource(R.string.demo_devices), stringResource(R.string.demo_devices_desc)) {
            Switch(checked = s.demoMode, onCheckedChange = {
                if (it) Actions.enableDemo(context) else Actions.disableDemo(context)
            })
        }
        Divider()
        ProRow(s.isPro) { showDialog { ProDialog(onDismiss = close) } }
    }
}

// ---------------------------------------------------------------- İzleme

@Composable
private fun MonitoringPage(showDialog: ((@Composable () -> Unit)?) -> Unit) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val (store, version) = settingsState()
    val s = remember(version) { store }
    val close = { showDialog(null) }
    PageIntro(stringResource(R.string.settings_monitoring_intro))
    SectionCard(Modifier.padding(top = 12.dp)) {
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
                showDialog {
                    ChoiceDialog(
                        stringResource(R.string.offline_grace),
                        stringResource(R.string.offline_grace_desc),
                        graceOptions, s.offlineGraceSec, { graceLabel(it) },
                        onPick = { store.offlineGraceSec = it }, onDismiss = close,
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
                showDialog {
                    ChoiceDialog(
                        stringResource(R.string.blind_alert),
                        stringResource(R.string.blind_alert_desc),
                        blindOptions, s.blindAlertMin, { blindLabel(it) },
                        onPick = { store.blindAlertMin = it }, onDismiss = close,
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
                showDialog {
                    ChoiceDialog(
                        stringResource(R.string.poll_interval),
                        stringResource(R.string.poll_interval_desc),
                        pollOptions, s.pollIntervalSec, { resources.getString(R.string.poll_interval_value, it) },
                        onPick = { store.pollIntervalSec = it }, onDismiss = close,
                    )
                }
            },
        )
    }
}

// ---------------------------------------------------------------- Alarm

@Composable
private fun AlarmPage(showDialog: ((@Composable () -> Unit)?) -> Unit) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val (store, version) = settingsState()
    val s = remember(version) { store }
    val close = { showDialog(null) }
    val ringtonePicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            @Suppress("DEPRECATION")
            val uri: Uri? = result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            store.alarmSoundUri = uri?.toString()
        }
    }
    val soundUri = s.alarmSoundUri
    // Yalnızca seçilen ses değişince okunur: Ringtone her oluşturulduğunda bir MediaPlayer hazırlar.
    val soundName = remember(soundUri) { alarmSoundName(context, soundUri) ?: resources.getString(R.string.default_sound) }

    SectionTitle(stringResource(R.string.settings_sound))
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
        InfoRow(Icons.Rounded.Campaign, stringResource(R.string.alarm_boost), stringResource(R.string.alarm_boost_desc)) {
            Switch(checked = s.alarmBoost, onCheckedChange = { store.alarmBoost = it })
        }
    }

    SectionTitle(stringResource(R.string.settings_when_alarm))
    SectionCard {
        val reminderOptions = listOf(0, 10, 30, 60)
        InfoRow(
            Icons.Rounded.Snooze,
            stringResource(R.string.reminder),
            reminderLabel(s.reminderMin),
            Modifier.clickable {
                showDialog {
                    ChoiceDialog(
                        stringResource(R.string.reminder),
                        stringResource(R.string.reminder_desc),
                        reminderOptions, s.reminderMin, { reminderLabel(it) },
                        onPick = { store.reminderMin = it }, onDismiss = close,
                    )
                }
            },
        )
        Divider()
        InfoRow(
            Icons.AutoMirrored.Rounded.Send,
            stringResource(R.string.tg_title),
            escalationSummary(s),
            Modifier.clickable { showDialog { TelegramDialog(store, close) } },
        )
    }

    SectionTitle(stringResource(R.string.settings_try_it))
    SectionCard {
        InfoRow(
            Icons.Rounded.NotificationsActive,
            stringResource(R.string.test_alarm),
            stringResource(R.string.test_alarm_desc),
            Modifier.clickable { showDialog { TestAlarmDialog(onDismiss = close) } },
        )
    }
}

// ---------------------------------------------------------------- Sessiz saatler

@Composable
private fun QuietPage() {
    val context = LocalContext.current
    val time = rememberTimePicker()
    val (store, version) = settingsState()
    val q = remember(version) { store.quietHours }
    PageIntro(stringResource(R.string.quiet_hours_desc))
    SectionCard(Modifier.padding(top = 12.dp)) {
        InfoRow(Icons.Rounded.Bedtime, stringResource(R.string.quiet_hours)) {
            Switch(checked = q.enabled, onCheckedChange = { store.quietHours = q.copy(enabled = it) })
        }
        if (q.enabled) {
            Divider()
            val startTitle = stringResource(R.string.quiet_start)
            InfoRow(
                Icons.Rounded.Schedule, startTitle, formatMinute(context, q.startMinute),
                Modifier.clickable { time.pick(startTitle, q.startMinute) { store.quietHours = store.quietHours.copy(startMinute = it) } },
            )
            Divider()
            val endTitle = stringResource(R.string.quiet_end)
            InfoRow(
                Icons.Rounded.Schedule, endTitle, formatMinute(context, q.endMinute),
                Modifier.clickable { time.pick(endTitle, q.endMinute) { store.quietHours = store.quietHours.copy(endMinute = it) } },
            )
        }
    }
    PageIntro(stringResource(R.string.settings_quiet_critical))
}

// ---------------------------------------------------------------- Günlük özet

@Composable
private fun SummaryPage() {
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val time = rememberTimePicker()
    val (store, version) = settingsState()
    val s = remember(version) { store }
    PageIntro(stringResource(R.string.daily_summary_desc))
    SectionCard(Modifier.padding(top = 12.dp)) {
        InfoRow(Icons.Rounded.Summarize, stringResource(R.string.daily_summary)) {
            Switch(checked = s.dailySummaryEnabled, onCheckedChange = {
                store.dailySummaryEnabled = it
                SummaryScheduler.schedule(context)
            })
        }
        if (s.dailySummaryEnabled) {
            Divider()
            val timeTitle = stringResource(R.string.daily_summary_time)
            InfoRow(
                Icons.Rounded.Schedule, timeTitle, formatMinute(context, s.dailySummaryMinute),
                Modifier.clickable {
                    time.pick(timeTitle, s.dailySummaryMinute) {
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
}

// ---------------------------------------------------------------- İzinler

@Composable
private fun PermissionsPage() {
    val context = LocalContext.current
    val resumed = resumedState()
    PageIntro(stringResource(R.string.settings_permissions_intro))
    SectionCard(Modifier.padding(top = 12.dp)) {
        val notif = remember(resumed) { Actions.notificationsEnabled(context) }
        val battery = remember(resumed) { Actions.batteryExempt(context) }
        InfoRow(
            if (notif) Icons.Rounded.NotificationsActive else Icons.Rounded.NotificationsOff,
            stringResource(R.string.perm_notifications),
            stringResource(if (notif) R.string.perm_granted else R.string.perm_missing),
            Modifier.clickable { Actions.openNotificationSettings(context) },
        ) { PermState(notif) }
        Divider()
        InfoRow(
            if (battery) Icons.Rounded.BatteryChargingFull else Icons.Rounded.BatteryAlert,
            stringResource(R.string.perm_battery),
            stringResource(if (battery) R.string.perm_battery_ok else R.string.perm_battery_missing),
            Modifier.clickable(enabled = !battery) { Actions.requestBatteryExemption(context) },
        ) { PermState(battery) }
        val fullScreen = remember(resumed) { Actions.fullScreenAllowed(context) }
        if (fullScreen != null) {
            Divider()
            InfoRow(
                Icons.Rounded.Fullscreen,
                stringResource(R.string.perm_full_screen),
                stringResource(if (fullScreen) R.string.perm_granted else R.string.perm_full_screen_missing),
                Modifier.clickable(enabled = !fullScreen) { Actions.openFullScreenSettings(context) },
            ) { PermState(fullScreen) }
        }
        val lockPopup = remember(resumed) { XiaomiPermissions.lockScreenPopup(context) }
        if (lockPopup != null) {
            Divider()
            InfoRow(
                Icons.Rounded.ScreenLockPortrait,
                stringResource(R.string.perm_lock_popup),
                stringResource(if (lockPopup) R.string.perm_granted else R.string.perm_lock_popup_missing),
                Modifier.clickable(enabled = !lockPopup) { XiaomiPermissions.openOtherPermissions(context) },
            ) { PermState(lockPopup) }
        }
        if (AlarmActivity.needsOverlayPermission(context)) {
            val overlay = remember(resumed) { AlarmActivity.canShowOverApps(context) }
            Divider()
            InfoRow(
                Icons.Rounded.Layers,
                stringResource(R.string.perm_overlay),
                stringResource(if (overlay) R.string.perm_granted else R.string.perm_missing),
                Modifier.clickable(enabled = !overlay) { AlarmActivity.openOverlaySettings(context) },
            ) { PermState(overlay) }
        }
        OemBackground.current(context)?.let { oem ->
            val autostart = remember(resumed) { XiaomiPermissions.autostart(context) }
            Divider()
            InfoRow(
                Icons.Rounded.RestartAlt,
                stringResource(R.string.perm_autostart, oem.label),
                stringResource(if (autostart == true) R.string.perm_granted else oem.hint),
                Modifier.clickable { oem.open(context) },
            ) { if (autostart != null) PermState(autostart) }
        }
    }
}

/** İzin satırının sağındaki durum: yeşil onay ya da turuncu uyarı. */
@Composable
private fun PermState(ok: Boolean) {
    Icon(
        if (ok) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline,
        contentDescription = stringResource(if (ok) R.string.perm_granted else R.string.perm_missing),
        tint = if (ok) StatusColors.online else StatusColors.warning,
    )
}

// ---------------------------------------------------------------- Yedekleme ve aktarım

@Composable
private fun BackupPage(showDialog: ((@Composable () -> Unit)?) -> Unit) {
    val (store, _) = settingsState()
    val close = { showDialog(null) }
    SectionTitle(stringResource(R.string.settings_other_devices))
    SectionCard {
        InfoRow(
            Icons.Rounded.SyncAlt,
            stringResource(R.string.transfer_send_row),
            stringResource(R.string.transfer_send_row_desc),
            Modifier.clickable { showDialog { SendTransferDialog(close) } },
        )
        Divider()
        InfoRow(
            Icons.Rounded.Download,
            stringResource(R.string.transfer_receive_row),
            stringResource(R.string.transfer_receive_row_desc),
            Modifier.clickable {
                showDialog {
                    AlertDialog(
                        onDismissRequest = close,
                        title = { Text(stringResource(R.string.onb_transfer_title)) },
                        text = {
                            Column {
                                ReceiveTransferPanel(onReceived = {
                                    // Gelen anahtarlar görünsün/onaylansın; ardından bu cihazın kendi hesabıyla giriş.
                                    if (store.hasCredentials) showDialog { CredentialsDialog(store, close) }
                                })
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = close, modifier = Modifier.tvFocusRing()) {
                                Text(stringResource(R.string.action_done))
                            }
                        },
                    )
                }
            },
        )
    }
    // TV'de dosya seçme/kaydetme ekranı (DocumentsUI) yok; kurulum oraya telefondan aktarılır.
    if (!LocalIsTv.current) BackupSection()
}

// ---------------------------------------------------------------- Yardım ve hakkında

@Composable
private fun AboutPage() {
    val context = LocalContext.current
    val resources = LocalResources.current
    val (store, _) = settingsState()
    val resumed = resumedState()
    SectionTitle(stringResource(R.string.settings_help))
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
    }

    SectionTitle(stringResource(R.string.settings_contact))
    SectionCard {
        var crashes by remember(resumed) { mutableIntStateOf(CrashLog.count(context)) }
        InfoRow(
            Icons.Rounded.BugReport,
            stringResource(R.string.crash_report),
            if (crashes > 0) pluralStringResource(R.plurals.crash_report_desc_some, crashes, crashes)
            else stringResource(R.string.crash_report_desc_none),
            Modifier.clickable {
                if (!Actions.sendProblemReport(context)) {
                    Toast.makeText(context, resources.getString(R.string.crash_report_no_mail), Toast.LENGTH_LONG).show()
                }
            },
        ) {
            if (crashes > 0) {
                IconButton(modifier = Modifier.tvFocusRing(), onClick = { CrashLog.clear(context); crashes = 0 }) {
                    Icon(Icons.Rounded.DeleteOutline, stringResource(R.string.crash_report_clear), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
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
    }

    SectionTitle(stringResource(R.string.section_about))
    SectionCard {
        InfoRow(
            Icons.Rounded.Info,
            stringResource(R.string.version_x, BuildConfig.VERSION_NAME),
            stringResource(R.string.disclaimer),
        )
    }
}

@Composable
private fun Divider(inset: Dp = 56.dp) = RowDivider(inset)

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
private fun monitoringSummary(graceSec: Int, poll: String): String =
    if (graceSec == 0) stringResource(R.string.monitoring_summary_now, poll)
    else stringResource(R.string.monitoring_summary_grace, graceLabel(graceSec), poll)

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

/**
 * Test alarmı: sesli ya da sessiz (gece başkasını uyandırmadan yalnızca ekran ve bildirim), istenirse 10 saniye sonra.
 * Gecikmede telefon kilitlenir ve alarm ekranının kilit ekranında açıldığı görülür.
 */
@Composable
private fun TestAlarmDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val resources = LocalResources.current
    var silent by remember { mutableStateOf(false) }
    var delayed by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.test_alarm)) },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                listOf(false to R.string.test_alarm_sound, true to R.string.test_alarm_silent).forEach { (value, label) ->
                    Row(
                        Modifier.fillMaxWidth().selectable(selected = silent == value, onClick = { silent = value })
                            .tvFocusRing(RoundedCornerShape(12.dp), inside = true).padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = silent == value, onClick = null)
                        Column(Modifier.padding(start = 12.dp)) {
                            Text(stringResource(label), style = MaterialTheme.typography.bodyLarge)
                            Text(
                                stringResource(if (value) R.string.test_alarm_silent_desc else R.string.test_alarm_sound_desc),
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Row(
                    Modifier.fillMaxWidth().clickable { delayed = !delayed }
                        .tvFocusRing(RoundedCornerShape(12.dp), inside = true).padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.test_alarm_delay, TEST_DELAY_SEC), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            stringResource(if (LocalIsTv.current) R.string.test_alarm_delay_desc_tv else R.string.test_alarm_delay_desc),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = delayed, onCheckedChange = null, modifier = Modifier.padding(start = 12.dp))
                }
            }
        },
        confirmButton = {
            TextButton(modifier = Modifier.tvFocusRing(), onClick = {
                Actions.testAlarm(context, silent, if (delayed) TEST_DELAY_SEC * 1000L else 0)
                if (delayed) {
                    Toast.makeText(context, resources.getString(R.string.test_alarm_scheduled, TEST_DELAY_SEC), Toast.LENGTH_LONG).show()
                }
                onDismiss()
            }) { Text(stringResource(R.string.test_alarm_start)) }
        },
        dismissButton = { TextButton(modifier = Modifier.tvFocusRing(), onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

private const val TEST_DELAY_SEC = 10

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
        PhoneEntryButton(
            stringResource(R.string.api_credentials),
            listOf(PhoneEntry.Field("appId", "App ID", value = appId), PhoneEntry.Field("appSecret", "App Secret", secret = true)),
            onReceived = { v ->
                v["appId"]?.takeIf { it.isNotEmpty() }?.let(onAppId)
                v["appSecret"]?.takeIf { it.isNotEmpty() }?.let(onSecret)
            },
            modifier = Modifier.padding(bottom = 8.dp),
        )
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
