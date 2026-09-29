package com.macerce.switchguard.ui

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

private const val STEPS = 6

/** İlk açılış sihirbazı: tanıtım → geliştirici uygulaması → API bilgileri → giriş → izinler → başlat. */
@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    val store = remember { Store.get(context) }
    val version by store.version.collectAsStateWithLifecycle()
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val resumed = lifecycle.isAtLeast(Lifecycle.State.RESUMED)

    var step by rememberSaveable { mutableIntStateOf(0) }
    BackHandler(enabled = step > 0) { step-- }

    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        LinearProgressIndicator(
            progress = { (step + 1f) / STEPS },
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
                when (s) {
                    0 -> WelcomeStep()
                    1 -> DeveloperStep()
                    2 -> CredentialsStep(store)
                    3 -> LoginStep(remember(version) { store.isLoggedIn })
                    4 -> PermissionsStep(resumed)
                    else -> DoneStep()
                }
            }
        }
        val canContinue = remember(step, version) {
            when (step) {
                2 -> store.hasCredentials
                3 -> store.isLoggedIn
                else -> true
            }
        }
        Row(Modifier.fillMaxWidth().padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            if (step > 0) TextButton(onClick = { step-- }) { Text(stringResource(R.string.action_back)) }
            Spacer(Modifier.weight(1f))
            if (step == 0 && store.isLoggedIn) {
                // Kurulum rehberine sonradan dönenler için.
                TextButton(onClick = onFinished) { Text(stringResource(R.string.action_skip)) }
            }
            Button(
                enabled = canContinue,
                onClick = {
                    if (step < STEPS - 1) step++
                    else {
                        Actions.startMonitoring(context)
                        onFinished()
                    }
                },
            ) {
                Text(stringResource(if (step < STEPS - 1) R.string.action_next else R.string.action_start_monitoring))
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
    SectionCard(Modifier.padding(horizontal = 0.dp)) {
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
