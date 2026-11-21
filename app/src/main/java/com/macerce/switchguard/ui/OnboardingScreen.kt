package com.macerce.switchguard.ui

import com.macerce.switchguard.ui.theme.LocalDarkTheme
import com.macerce.switchguard.ui.theme.Brand
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.ui.text.style.TextAlign
import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeveloperMode
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.ScreenLockPortrait
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SyncAlt
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.currentStateAsState
import com.macerce.switchguard.R
import com.macerce.switchguard.data.Store

private enum class Step { WELCOME, RECEIVE, DEVELOPER, CREDENTIALS, LOGIN, TUYA_GUIDE, TUYA_CONNECT, PERMISSIONS, DONE }

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

    var platform by rememberSaveable {
        mutableStateOf(if (store.hasTuya && !store.isLoggedIn) Platform.TUYA else Platform.EWELINK)
    }
    val steps = when (platform) {
        Platform.TUYA -> listOf(Step.WELCOME, Step.TUYA_GUIDE, Step.TUYA_CONNECT, Step.PERMISSIONS, Step.DONE)
        Platform.EWELINK -> listOf(Step.WELCOME, Step.DEVELOPER, Step.CREDENTIALS, Step.LOGIN, Step.PERMISSIONS, Step.DONE)
        // Demo: hesap adımları yok; cihazlar başlatınca eklenir.
        Platform.DEMO -> listOf(Step.WELCOME, Step.PERMISSIONS, Step.DONE)
        // Aktarma: Tuya, kurallar ve ayarlar telefondan gelir; eWeLink bu cihazın kendi hesabıyla kurulur.
        Platform.TRANSFER -> listOf(
            Step.WELCOME, Step.RECEIVE,
            // eWeLink: bu cihazın kendi hesabı, geliştirici onayı ve o hesabın anahtarları (aktarılmaz).
            Step.DEVELOPER, Step.CREDENTIALS, Step.LOGIN,
            Step.PERMISSIONS, Step.DONE,
        )
    }
    var step by rememberSaveable { mutableIntStateOf(0) }
    BackHandler(enabled = step > 0) { step-- }

    Column(Modifier.fillMaxSize().safeDrawingPadding().readableWidth(760.dp)) {
        // Karşılamada ilerleme çubuğu yok (videodaki gibi yalnızca logo ve üç düğme).
        if (step > 0) {
            LinearProgressIndicator(
                progress = { step.toFloat() / (steps.size - 1) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            )
        }
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                val dir = if (targetState > initialState) 1 else -1
                slideInHorizontally { it * dir } togetherWith slideOutHorizontally { -it * dir }
            },
            modifier = Modifier.weight(1f),
            label = "step",
        ) { s ->
            if (s == 0) {
                // Karşılama: platform seçimi doğrudan buradaki düğmelerle yapılır.
                WelcomeHero(showSkip = store.hasAnyAccount, onSkip = onFinished) { platform = it; step = 1 }
                return@AnimatedContent
            }
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
                when (steps[s.coerceAtMost(steps.size - 1)]) {
                    Step.WELCOME -> Unit
                    Step.RECEIVE -> ReceiveStep()
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
        val canContinue = remember(step, version, platform) {
            when (steps[step]) {
                // Aktarmada eWeLink isteğe bağlı (yalnızca Tuya kullanan atlayabilir).
                Step.CREDENTIALS -> store.hasCredentials || platform == Platform.TRANSFER
                Step.LOGIN -> store.isLoggedIn || platform == Platform.TRANSFER
                Step.RECEIVE -> store.transferReceived
                Step.TUYA_CONNECT -> store.hasTuya
                else -> true
            }
        }
        if (step > 0) Row(Modifier.fillMaxWidth().padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { step-- }, modifier = Modifier.tvFocusRing()) { Text(stringResource(R.string.action_back)) }
            Spacer(Modifier.weight(1f))
            if (!canContinue && !store.hasAnyAccount) {
                // Hesabı olmadan uygulamaya göz atmak isteyenler (ör. testçiler); hesap sonra Ayarlar'dan eklenir.
                TextButton(onClick = onFinished, modifier = Modifier.tvFocusRing()) { Text(stringResource(R.string.action_skip_for_now)) }
            }
            PillButton(
                stringResource(if (step < steps.size - 1) R.string.action_next else R.string.action_start_monitoring),
                modifier = Modifier.widthIn(min = 132.dp),
                enabled = canContinue,
                onClick = {
                    if (step < steps.size - 1) step++
                    else {
                        if (platform == Platform.DEMO) Actions.enableDemo(context)
                        Actions.startMonitoring(context)
                        onFinished()
                    }
                },
            )
        }
    }
}

@Composable
private fun StepHeader(icon: ImageVector, title: String, body: String) {
    Spacer(Modifier.height(16.dp))
    // Halkalı marka dairesi (karşılama logosuyla aynı dil).
    HaloIcon(icon, size = 88.dp)
    Spacer(Modifier.height(20.dp))
    Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(12.dp))
    Text(body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(24.dp))
}

@Composable
private fun Feature(icon: ImageVector, text: String, hue: Hue = Hue.TEAL) {
    Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        HueIcon(icon, hue, size = 36.dp)
        Spacer(Modifier.width(16.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

/**
 * Karşılama (tanıtım videosundaki ekran): halkalı kalkan, ad, slogan ve üç büyük düğme.
 * eWeLink / Tuya / Demo seçimi doğrudan buradan yapılır; aktarma altta bağlantı olarak durur.
 */
@Composable
private fun WelcomeHero(showSkip: Boolean, onSkip: () -> Unit, onPick: (Platform) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val minH = maxHeight
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).heightIn(min = minH).padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(Modifier.fillMaxWidth().height(56.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                // Kurulum rehberine sonradan dönenler için.
                if (showSkip) TextButton(onClick = onSkip, modifier = Modifier.tvFocusRing()) { Text(stringResource(R.string.action_skip)) }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                HaloIcon(Icons.Rounded.Shield, size = 156.dp)
                Spacer(Modifier.height(28.dp))
                Text(
                    stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    stringResource(R.string.onb_hero_tagline),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = 300.dp),
                )
            }
            Column(Modifier.widthIn(max = 420.dp).padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                PillButton(
                    stringResource(R.string.action_login),
                    onClick = { onPick(Platform.EWELINK) },
                    icon = Icons.AutoMirrored.Rounded.Login,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                PillButton(
                    stringResource(R.string.onb_hero_tuya),
                    onClick = { onPick(Platform.TUYA) },
                    style = PillStyle.OUTLINED,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(22.dp))
                PillButton(
                    stringResource(R.string.onb_hero_demo),
                    onClick = { onPick(Platform.DEMO) },
                    style = PillStyle.TONAL,
                    icon = Icons.Rounded.PlayArrow,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(6.dp))
                TextButton(onClick = { onPick(Platform.TRANSFER) }, modifier = Modifier.tvFocusRing()) {
                    Icon(Icons.Rounded.SyncAlt, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.onb_platform_transfer))
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    stringResource(R.string.disclaimer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
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

private enum class Platform { EWELINK, TUYA, DEMO, TRANSFER }

@Composable
private fun TuyaGuideStep() {
    val context = LocalContext.current
    StepHeader(Icons.Rounded.DeveloperMode, stringResource(R.string.onb_tuya_title), stringResource(R.string.onb_tuya_body))
    NumberedStep(1, stringResource(R.string.onb_tuya_step1))
    NumberedStep(2, stringResource(R.string.onb_tuya_step2))
    NumberedStep(3, stringResource(R.string.onb_tuya_step3))
    NumberedStep(4, stringResource(R.string.onb_tuya_step4))
    Spacer(Modifier.height(16.dp))
    PillButton(
        stringResource(R.string.onb_open_tuya),
        onClick = { Actions.openUrl(context, Actions.TUYA_PLATFORM_URL) },
        icon = Icons.AutoMirrored.Rounded.OpenInNew,
        modifier = Modifier.fillMaxWidth(),
    )
    PillButton(
        stringResource(R.string.onb_open_guide),
        onClick = { Actions.openUrl(context, Actions.guideUrl(context, "tuya")) },
        style = PillStyle.OUTLINED,
        icon = Icons.AutoMirrored.Rounded.MenuBook,
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
    )
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
                remember(version) { store.snapshots.count { it.cloud == com.macerce.switchguard.core.Cloud.TUYA } }.let { n ->
                    pluralStringResource(R.plurals.tuya_connected_x, n, regionLabel(store.tuyaRegion), n)
                },
                style = MaterialTheme.typography.titleMedium,
            )
        }
        TextButton(onClick = { dialog = true }, modifier = Modifier.tvFocusRing()) { Text(stringResource(R.string.action_change)) }
    } else {
        PillButton(stringResource(R.string.action_enter_keys), onClick = { dialog = true }, icon = Icons.Rounded.Key, modifier = Modifier.fillMaxWidth())
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
    PillButton(
        stringResource(R.string.onb_open_portal),
        onClick = { Actions.openUrl(context, Actions.DEV_PORTAL_URL) },
        icon = Icons.AutoMirrored.Rounded.OpenInNew,
        modifier = Modifier.fillMaxWidth(),
    )
    PillButton(
        stringResource(R.string.onb_open_guide),
        onClick = { Actions.openUrl(context, Actions.guideUrl(context, "ewelink")) },
        style = PillStyle.OUTLINED,
        icon = Icons.AutoMirrored.Rounded.MenuBook,
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
    )
    PillButton(
        stringResource(R.string.onb_copy_redirect),
        onClick = { clipboard.setPrimaryClip(ClipData.newPlainText("Redirect URL", Store.DEFAULT_REDIRECT)) },
        style = PillStyle.TONAL,
        icon = Icons.Rounded.ContentCopy,
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
    )
    Spacer(Modifier.height(12.dp))
    Text(stringResource(R.string.onb_dev_brand_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun CredentialsStep(store: Store) {
    var appId by remember { mutableStateOf(store.appId) }
    var secret by remember { mutableStateOf(store.appSecret) }
    var redirect by remember { mutableStateOf(store.redirectUrl) }
    // Anahtarlar bu cihazda giriş yapılacak hesaba ait olmalı: aktarmadan geldiyse uyarı en üstte.
    if (store.transferSource.isNotEmpty()) {
        Spacer(Modifier.height(16.dp))
        SameAccountWarning(store)
    }
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
    // Her cihazda ayrı eWeLink hesabı uyarısı en üstte (aktarma olsun olmasın).
    Spacer(Modifier.height(16.dp))
    SameAccountWarning(Store.get(LocalContext.current))
    val context = LocalContext.current
    StepHeader(Icons.Rounded.Shield, stringResource(R.string.onb_login_title), stringResource(R.string.onb_login_body))
    if (loggedIn) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.logged_in), style = MaterialTheme.typography.titleMedium)
        }
        TextButton(onClick = { LoginActivity.start(context) }, modifier = Modifier.tvFocusRing()) { Text(stringResource(R.string.action_relogin)) }
    } else {
        PillButton(
            stringResource(R.string.action_login),
            onClick = { LoginActivity.start(context) },
            icon = Icons.AutoMirrored.Rounded.Login,
            modifier = Modifier.fillMaxWidth(),
        )
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
    remember(resumed) { Actions.fullScreenAllowed(context) }?.let { allowed ->
        Spacer(Modifier.height(12.dp))
        PermissionCard(
            Icons.Rounded.Fullscreen,
            stringResource(R.string.perm_full_screen),
            stringResource(R.string.onb_perm_full_screen_desc),
            granted = allowed,
        ) { Actions.openFullScreenSettings(context) }
    }
    remember(resumed) { XiaomiPermissions.lockScreenPopup(context) }?.let { allowed ->
        Spacer(Modifier.height(12.dp))
        PermissionCard(
            Icons.Rounded.ScreenLockPortrait,
            stringResource(R.string.perm_lock_popup),
            stringResource(R.string.onb_perm_lock_popup_desc),
            granted = allowed,
        ) { XiaomiPermissions.openOtherPermissions(context) }
    }
    if (AlarmActivity.needsOverlayPermission(context)) {
        Spacer(Modifier.height(12.dp))
        PermissionCard(
            Icons.Rounded.Layers,
            stringResource(R.string.perm_overlay),
            stringResource(R.string.perm_overlay_desc),
            granted = remember(resumed) { AlarmActivity.canShowOverApps(context) },
        ) { AlarmActivity.openOverlaySettings(context) }
    }
    OemBackground.current(context)?.let { oem ->
        Spacer(Modifier.height(12.dp))
        // Xiaomi'de durum okunur; diğer markalarda okunamaz, kart her zaman "aç" düğmesiyle gösterilir.
        PermissionCard(
            Icons.Rounded.RestartAlt,
            stringResource(R.string.perm_autostart, oem.label),
            stringResource(oem.hint),
            granted = remember(resumed) { XiaomiPermissions.autostart(context) } == true,
            actionLabel = stringResource(R.string.action_open_settings),
        ) { oem.open(context) }
    }
}

@Composable
private fun PermissionCard(
    icon: ImageVector,
    title: String,
    desc: String,
    granted: Boolean,
    actionLabel: String = stringResource(R.string.action_allow),
    onGrant: () -> Unit,
) {
    SectionCard(horizontalPadding = 0) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (granted) CheckDot()
            }
            Spacer(Modifier.height(8.dp))
            Text(desc, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (!granted) {
                Spacer(Modifier.height(8.dp))
                PillButton(actionLabel, onClick = onGrant, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun DoneStep() {
    StepHeader(Icons.Rounded.CheckCircle, stringResource(R.string.onb_done_title), stringResource(R.string.onb_done_body))
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Feature(Icons.Rounded.Tune, stringResource(R.string.onb_done_tip_rules), Hue.INDIGO)
        Feature(Icons.Rounded.NotificationsActive, stringResource(R.string.onb_done_tip_test), Hue.RED)
    }
}

@Composable
private fun ReceiveStep() {
    StepHeader(Icons.Rounded.SyncAlt, stringResource(R.string.onb_transfer_title), stringResource(R.string.onb_transfer_body))
    ReceiveTransferPanel(onReceived = {})
}
