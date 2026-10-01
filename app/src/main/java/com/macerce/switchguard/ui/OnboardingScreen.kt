package com.macerce.switchguard.ui

import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material3.RadioButton
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.rounded.Hub
import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeveloperMode
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.currentStateAsState
import com.macerce.switchguard.R
import com.macerce.switchguard.data.Store

private enum class Step { WELCOME, PLATFORM, DEVELOPER, CREDENTIALS, LOGIN, TUYA_GUIDE, TUYA_CONNECT, PERMISSIONS, DONE }

/**
 * İlk açılış sihirbazı: tanıtım → cihazların uygulaması (eWeLink / Tuya) → o bulutun kurulumu → izinler → başlat.
 * eWeLink: geliştirici uygulaması → API bilgileri → giriş. Tuya: Cloud projesi rehberi → bilgileri girip bağlanma.
 * Diğer bulut daha sonra Ayarlar'dan eklenebilir.
 */
@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    val store = remember { Store.get(context) }
    val version by store.version.collectAsStateWithLifecycle()
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val resumed = lifecycle.isAtLeast(Lifecycle.State.RESUMED)

    var tuyaPath by rememberSaveable { mutableStateOf(store.hasTuya && !store.isLoggedIn) }
    val steps = if (tuyaPath) {
        listOf(Step.WELCOME, Step.PLATFORM, Step.TUYA_GUIDE, Step.TUYA_CONNECT, Step.PERMISSIONS, Step.DONE)
    } else {
        listOf(Step.WELCOME, Step.PLATFORM, Step.DEVELOPER, Step.CREDENTIALS, Step.LOGIN, Step.PERMISSIONS, Step.DONE)
    }
    var step by rememberSaveable { mutableIntStateOf(0) }
    BackHandler(enabled = step > 0) { step-- }

    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        LinearProgressIndicator(
            progress = { (step + 1f) / steps.size },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
        )
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                val dir = if (targetState > initialState) 1 else -1
                slideInHorizontally { it * dir } togetherWith slideOutHorizontally { -it * dir }
            },
            modifier = Modifier.weight(1f),
            label = "step",
        ) { s ->
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
                when (steps[s.coerceAtMost(steps.size - 1)]) {
                    Step.WELCOME -> WelcomeStep()
                    Step.PLATFORM -> PlatformStep(tuyaPath) { tuyaPath = it }
                    Step.DEVELOPER -> DeveloperStep()
                    Step.CREDENTIALS -> CredentialsStep(store)
                    Step.LOGIN -> LoginStep(remember(version) { store.isLoggedIn })
                    Step.TUYA_GUIDE -> TuyaGuideStep()
                    Step.TUYA_CONNECT -> TuyaConnectStep(store, version)
                    Step.PERMISSIONS -> PermissionsStep(resumed)
                    Step.DONE -> DoneStep()
                }
            }
        }
        val canContinue = remember(step, version, tuyaPath) {
            when (steps[step]) {
                Step.CREDENTIALS -> store.hasCredentials
                Step.LOGIN -> store.isLoggedIn
                Step.TUYA_CONNECT -> store.hasTuya
                else -> true
            }
        }
        Row(Modifier.fillMaxWidth().padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            if (step > 0) TextButton(onClick = { step-- }) { Text(stringResource(R.string.action_back)) }
            Spacer(Modifier.weight(1f))
            if (step == 0 && store.hasAnyAccount) {
                // Kurulum rehberine sonradan dönenler için.
                TextButton(onClick = onFinished) { Text(stringResource(R.string.action_skip)) }
            }
            Button(
                enabled = canContinue,
                onClick = {
                    if (step < steps.size - 1) step++
                    else {
                        Actions.startMonitoring(context)
                        onFinished()
                    }
                },
            ) {
                Text(stringResource(if (step < steps.size - 1) R.string.action_next else R.string.action_start_monitoring))
            }
        }
    }
}

@Composable
private fun StepHeader(icon: ImageVector, title: String, body: String) {
    Spacer(Modifier.height(24.dp))
    IconBadge(icon, MaterialTheme.colorScheme.onPrimaryContainer, MaterialTheme.colorScheme.primaryContainer, size = 72)
    Spacer(Modifier.height(24.dp))
    Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(12.dp))
    Text(body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(24.dp))
}

@Composable
private fun Feature(icon: ImageVector, text: String) {
    Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(16.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun WelcomeStep() {
    StepHeader(Icons.Rounded.Shield, stringResource(R.string.onb_welcome_title), stringResource(R.string.onb_welcome_body))
    Feature(Icons.Rounded.NotificationsActive, stringResource(R.string.onb_feature_alarm))
    Feature(Icons.Rounded.PowerSettingsNew, stringResource(R.string.onb_feature_control))
    Feature(Icons.Rounded.Tune, stringResource(R.string.onb_feature_rules))
    Feature(Icons.Rounded.History, stringResource(R.string.onb_feature_history))
    Spacer(Modifier.height(16.dp))
    Text(stringResource(R.string.disclaimer), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
}

@Composable
private fun NumberedStep(n: Int, text: String) {
    Row(Modifier.padding(vertical = 6.dp)) {
        Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.size(28.dp)) {
            Text("$n", Modifier.padding(top = 4.dp), style = MaterialTheme.typography.labelLarge, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
        Spacer(Modifier.width(14.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 3.dp))
    }
}

@Composable
private fun PlatformStep(tuya: Boolean, onPick: (Boolean) -> Unit) {
    StepHeader(Icons.Rounded.Hub, stringResource(R.string.onb_platform_title), stringResource(R.string.onb_platform_body))
    PlatformOption(!tuya, stringResource(R.string.onb_platform_ewelink), stringResource(R.string.onb_platform_ewelink_desc)) { onPick(false) }
    Spacer(Modifier.height(12.dp))
    PlatformOption(tuya, stringResource(R.string.onb_platform_tuya), stringResource(R.string.onb_platform_tuya_desc)) { onPick(true) }
    Spacer(Modifier.height(12.dp))
    Text(stringResource(R.string.onb_platform_both), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun PlatformOption(selected: Boolean, title: String, desc: String, onClick: () -> Unit) {
    SectionCard(horizontalPadding = 0) {
        Row(
            Modifier.fillMaxWidth().selectable(selected = selected, onClick = onClick).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = selected, onClick = null)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(desc, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun TuyaGuideStep() {
    val context = LocalContext.current
    StepHeader(Icons.Rounded.DeveloperMode, stringResource(R.string.onb_tuya_title), stringResource(R.string.onb_tuya_body))
    NumberedStep(1, stringResource(R.string.onb_tuya_step1))
    NumberedStep(2, stringResource(R.string.onb_tuya_step2))
    NumberedStep(3, stringResource(R.string.onb_tuya_step3))
    NumberedStep(4, stringResource(R.string.onb_tuya_step4))
    Spacer(Modifier.height(16.dp))
    Button(onClick = { Actions.openUrl(context, Actions.TUYA_PLATFORM_URL) }, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.AutoMirrored.Rounded.OpenInNew, null)
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.onb_open_tuya))
    }
    OutlinedButton(
        onClick = { Actions.openUrl(context, Actions.guideUrl(context, "tuya")) },
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    ) {
        Icon(Icons.AutoMirrored.Rounded.MenuBook, null)
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.onb_open_guide))
    }
    Spacer(Modifier.height(12.dp))
    Text(stringResource(R.string.onb_tuya_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun TuyaConnectStep(store: Store, version: Int) {
    var dialog by remember { mutableStateOf(false) }
    StepHeader(Icons.Rounded.Key, stringResource(R.string.onb_tuya_connect_title), stringResource(R.string.onb_tuya_connect_body))
    val connected = remember(version) { store.hasTuya }
    if (connected) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(R.string.tuya_connected_x, regionLabel(store.tuyaRegion), remember(version) {
                    store.snapshots.count { it.cloud == com.macerce.switchguard.core.Cloud.TUYA }
                }),
                style = MaterialTheme.typography.titleMedium,
            )
        }
        TextButton(onClick = { dialog = true }) { Text(stringResource(R.string.action_change)) }
    } else {
        Button(onClick = { dialog = true }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.action_enter_keys)) }
    }
    Spacer(Modifier.height(12.dp))
    Text(stringResource(R.string.onb_tuya_privacy), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    if (dialog) TuyaDialog(store) { dialog = false }
}

@Composable
private fun DeveloperStep() {
    val context = LocalContext.current
    val clipboard = remember { context.getSystemService(ClipboardManager::class.java) }
    StepHeader(Icons.Rounded.DeveloperMode, stringResource(R.string.onb_dev_title), stringResource(R.string.onb_dev_body))
    NumberedStep(1, stringResource(R.string.onb_dev_step1))
    NumberedStep(2, stringResource(R.string.onb_dev_step2))
    NumberedStep(3, stringResource(R.string.onb_dev_step3, Store.DEFAULT_REDIRECT))
    NumberedStep(4, stringResource(R.string.onb_dev_step4))
    Spacer(Modifier.height(16.dp))
    Button(onClick = { Actions.openUrl(context, Actions.DEV_PORTAL_URL) }, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.AutoMirrored.Rounded.OpenInNew, null)
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.onb_open_portal))
    }
    OutlinedButton(
        onClick = { Actions.openUrl(context, Actions.guideUrl(context, "ewelink")) },
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    ) {
        Icon(Icons.AutoMirrored.Rounded.MenuBook, null)
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.onb_open_guide))
    }
    OutlinedButton(
        onClick = { clipboard.setPrimaryClip(ClipData.newPlainText("Redirect URL", Store.DEFAULT_REDIRECT)) },
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    ) {
        Icon(Icons.Rounded.ContentCopy, null)
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.onb_copy_redirect))
    }
    Spacer(Modifier.height(12.dp))
    Text(stringResource(R.string.onb_dev_brand_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun CredentialsStep(store: Store) {
    var appId by remember { mutableStateOf(store.appId) }
    var secret by remember { mutableStateOf(store.appSecret) }
    var redirect by remember { mutableStateOf(store.redirectUrl) }
    StepHeader(Icons.Rounded.Key, stringResource(R.string.onb_cred_title), stringResource(R.string.onb_cred_body))
    // Her tuşta kaydet ki "İleri" düğmesi anında etkinleşsin.
    CredentialFields(
        appId, { appId = it; store.appId = it },
        secret, { secret = it; store.appSecret = it },
        redirect, { redirect = it; store.redirectUrl = it },
    )
    Spacer(Modifier.height(12.dp))
    Text(stringResource(R.string.onb_cred_privacy), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun LoginStep(loggedIn: Boolean) {
    val context = LocalContext.current
    StepHeader(Icons.Rounded.Shield, stringResource(R.string.onb_login_title), stringResource(R.string.onb_login_body))
    if (loggedIn) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.logged_in), style = MaterialTheme.typography.titleMedium)
        }
        TextButton(onClick = { LoginActivity.start(context) }) { Text(stringResource(R.string.action_relogin)) }
    } else {
        Button(onClick = { LoginActivity.start(context) }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.action_login))
        }
    }
}

@Composable
private fun PermissionsStep(resumed: Boolean) {
    val context = LocalContext.current
    var notifGranted by remember(resumed) { mutableStateOf(Actions.notificationsEnabled(context)) }
    val battery = remember(resumed) { Actions.batteryExempt(context) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        notifGranted = Actions.notificationsEnabled(context)
    }

    StepHeader(Icons.Rounded.NotificationsActive, stringResource(R.string.onb_perm_title), stringResource(R.string.onb_perm_body))
    PermissionCard(
        Icons.Rounded.NotificationsActive,
        stringResource(R.string.perm_notifications),
        stringResource(R.string.onb_perm_notif_desc),
        granted = notifGranted,
    ) {
        if (Build.VERSION.SDK_INT >= 33) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        else Actions.openNotificationSettings(context)
    }
    Spacer(Modifier.height(12.dp))
    PermissionCard(
        Icons.Rounded.BatteryChargingFull,
        stringResource(R.string.perm_battery),
        stringResource(R.string.onb_perm_battery_desc),
        granted = battery,
    ) { Actions.requestBatteryExemption(context) }
}

@Composable
private fun PermissionCard(icon: ImageVector, title: String, desc: String, granted: Boolean, onGrant: () -> Unit) {
    SectionCard(horizontalPadding = 0) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (granted) Icon(Icons.Rounded.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(8.dp))
            Text(desc, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (!granted) {
                Spacer(Modifier.height(8.dp))
                Button(onClick = onGrant) { Text(stringResource(R.string.action_allow)) }
            }
        }
    }
}

@Composable
private fun DoneStep() {
    StepHeader(Icons.Rounded.CheckCircle, stringResource(R.string.onb_done_title), stringResource(R.string.onb_done_body))
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Feature(Icons.Rounded.Tune, stringResource(R.string.onb_done_tip_rules))
        Feature(Icons.Rounded.NotificationsActive, stringResource(R.string.onb_done_tip_test))
    }
}
