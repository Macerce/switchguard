package com.macerce.switchguard.desktop.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.ui.platform.testTag
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Key
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.macerce.switchguard.api.EwelinkClient
import com.macerce.switchguard.desktop.AppGraph
import com.macerce.switchguard.desktop.accounts.EwelinkLogin
import com.macerce.switchguard.desktop.accounts.EwelinkRedirect
import com.macerce.switchguard.desktop.accounts.LoginResult
import com.macerce.switchguard.desktop.accounts.Parsed
import com.macerce.switchguard.desktop.i18n.Strings
import com.macerce.switchguard.desktop.platform.ClipboardWatcher
import com.macerce.switchguard.desktop.platform.Links
import com.macerce.switchguard.desktop.ui.InfoRow
import com.macerce.switchguard.desktop.ui.RowDivider
import com.macerce.switchguard.desktop.ui.SectionCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Ayarlar → Hesaplar → eWeLink kartı (telefondaki gibi): hesap (giriş / yeniden giriş), API bilgileri, çıkış ve hesabı
 * kaldırma satırları; altında bu bilgisayarın ayrı hesap gerektirdiği notu.
 */
@Composable
fun EwelinkRows(graph: AppGraph, strings: Strings, version: Int) {
    val store = graph.store
    val hasKeys = remember(version) { store.hasCredentials }
    val loggedIn = remember(version) { store.isLoggedIn }
    val email = remember(version) { store.accountEmail }
    val region = remember(version) { store.region }
    val appId = remember(version) { store.appId }
    var dialog by remember { mutableStateOf<String?>(null) } // "keys", "login", "logout", "remove"

    SectionCard {
        InfoRow(
            Icons.Rounded.AccountCircle,
            when {
                loggedIn -> EwelinkLogin.maskEmail(email).ifEmpty { strings["logged_in"] }
                else -> strings["not_logged_in"]
            },
            when {
                loggedIn -> strings.format("region_x", region.uppercase())
                !hasKeys -> strings["desktop_ewelink_keys_missing"]
                else -> null
            },
            Modifier.clickable { dialog = if (hasKeys) "login" else "keys" },
        ) {
            if (hasKeys) {
                TextButton(onClick = { dialog = "login" }) { Text(strings[if (loggedIn) "action_relogin" else "action_login"]) }
            }
        }
        RowDivider()
        InfoRow(
            Icons.Rounded.Key,
            strings["api_credentials"],
            if (hasKeys) strings.format("app_id_x", appId.take(6) + "…") else strings["not_set"],
            Modifier.clickable { dialog = "keys" },
        )
        if (loggedIn) {
            RowDivider()
            InfoRow(Icons.AutoMirrored.Rounded.Logout, strings["action_logout"], modifier = Modifier.clickable { dialog = "logout" })
        }
        if (hasKeys) {
            RowDivider()
            InfoRow(Icons.Rounded.DeleteOutline, strings["desktop_ewelink_remove"], modifier = Modifier.clickable { dialog = "remove" })
        }
    }
    PageIntro(strings["desktop_ewelink_pc_notice"], Modifier.padding(top = 8.dp))

    /** Hesap değişti: izlenecek bir şey kalmadıysa izleme durur, yoksa (açıksa) yeniden bağlanır. */
    fun afterAccountChange() {
        if (!store.hasAnyAccount) graph.engine.stop() else applyEngineSettings(graph)
    }

    when (dialog) {
        "keys" -> ApiCredentialsDialog(graph, strings) { dialog = null }
        "login" -> EwelinkLoginDialog(graph, strings) { dialog = null }
        "logout" -> Confirm(strings, strings["logout_title"] + "\n\n" + strings["logout_body"], strings["action_logout"], onDismiss = { dialog = null }) {
            store.logout()
            afterAccountChange()
        }
        "remove" -> Confirm(strings, strings["desktop_ewelink_remove_confirm"], strings["desktop_remove"], onDismiss = { dialog = null }) {
            EwelinkLogin(store).removeAccount()
            afterAccountChange()
        }
    }
}

/**
 * App ID, App Secret ve Redirect URL. Kayıtlı Secret telefondaki gibi alana gelir, noktalarla gizli; göz düğmesiyle
 * görülebilir (önceden boş gelirdi ve kaydedilmemiş sanılıyordu). App ID değişirse eski oturum o uygulamaya ait
 * olduğundan oturum kapatılır.
 */
@Composable
fun ApiCredentialsDialog(graph: AppGraph, strings: Strings, onDismiss: () -> Unit) {
    val store = graph.store
    var appId by remember { mutableStateOf(store.appId) }
    var secret by remember { mutableStateOf(store.appSecret) }
    var showSecret by remember { mutableStateOf(false) }
    var redirect by remember { mutableStateOf(store.redirectUrl) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings["api_credentials"]) },
        text = {
            Column(
                Modifier.widthIn(min = 480.dp).heightIn(max = 560.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(strings["desktop_ewelink_pc_notice"], style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(appId, { appId = it }, Modifier.fillMaxWidth(), label = { Text("App ID") }, singleLine = true)
                OutlinedTextField(
                    secret, { secret = it }, Modifier.fillMaxWidth().testTag("app-secret"),
                    label = { Text("App Secret") }, singleLine = true,
                    visualTransformation = if (showSecret) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { showSecret = !showSecret }) {
                            Icon(
                                if (showSecret) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                contentDescription = strings[if (showSecret) "desktop_secret_hide" else "desktop_secret_show"],
                            )
                        }
                    },
                )
                OutlinedTextField(
                    redirect, { redirect = it }, Modifier.fillMaxWidth(),
                    label = { Text("Redirect URL") }, singleLine = true,
                    supportingText = { Text(strings["redirect_hint"]) },
                )
                SetupSteps(strings["desktop_how_to_setup"], Setup.ewelinkSteps(strings), Setup.ewelinkActions(strings, store), inDialog = true)
            }
        },
        confirmButton = {
            Button(enabled = appId.isNotBlank() && secret.isNotBlank(), onClick = {
                if (appId.trim() != store.appId) store.logout()
                store.appId = appId
                if (secret.trim() != store.appSecret) store.appSecret = secret
                store.redirectUrl = redirect
                onDismiss()
            }) { Text(strings["action_save"]) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings["action_cancel"]) } },
    )
}

/**
 * Tarayıcıyla giriş (yedek yol, `spec-2-ayarlar.md` §4.2): pencere açılınca giriş sayfası tarayıcıda açılır; pano
 * yarım saniyede bir okunur ve bu girişe ait dönüş adresi kopyalanınca kendiliğinden kullanılır; elle yapıştırma da
 * var. Başarıda motor yeniden başlar ve pencere kapanır.
 */
@Composable
fun EwelinkLoginDialog(graph: AppGraph, strings: Strings, onDismiss: () -> Unit) {
    val store = graph.store
    val state = remember { EwelinkRedirect.newState() }
    val url = remember { EwelinkClient(store).buildLoginUrl(state) }
    var pasted by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun use(text: String, fromClipboard: Boolean) {
        if (busy) return
        when (val parsed = EwelinkRedirect.parse(text, store.redirectUrl, state)) {
            Parsed.NotRedirect -> if (!fromClipboard) message = strings["desktop_ewelink_wrong_address"]
            Parsed.WrongState -> message = strings["desktop_ewelink_wrong_address"]
            is Parsed.Code -> {
                busy = true
                message = null
                scope.launch {
                    val result = withContext(Dispatchers.IO) { EwelinkLogin(store).complete(parsed.code, parsed.region) }
                    busy = false
                    when (result) {
                        LoginResult.Ok -> {
                            graph.engine.restart()
                            onDismiss()
                        }
                        is LoginResult.Rejected -> message = strings.format("desktop_ewelink_login_failed", result.message)
                        LoginResult.NoInternet -> message = strings["desktop_no_internet"]
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!Links.open(url)) message = strings["desktop_link_copied"]
        val watcher = ClipboardWatcher { use(it, fromClipboard = true) }
        while (true) {
            watcher.poll()
            delay(500)
        }
    }

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(strings["desktop_ewelink_login_title"]) },
        text = {
            Column(Modifier.widthIn(min = 480.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(strings["desktop_ewelink_login_steps"], style = MaterialTheme.typography.bodyMedium)
                Text(
                    strings[if (busy) "desktop_ewelink_signing_in" else "desktop_ewelink_waiting"],
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                LinearProgressIndicator(Modifier.fillMaxWidth())
                OutlinedTextField(
                    pasted, { pasted = it }, Modifier.fillMaxWidth(),
                    label = { Text(strings["desktop_ewelink_paste_hint"]) }, singleLine = true, enabled = !busy,
                )
                message?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error) }
                TextButton(onClick = { if (!Links.open(url)) message = strings["desktop_link_copied"] }) {
                    Text(strings["desktop_ewelink_open_again"])
                }
            }
        },
        confirmButton = {
            Button(enabled = !busy && pasted.isNotBlank(), onClick = { use(pasted, fromClipboard = false) }) {
                Text(strings["desktop_connect"])
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text(strings["action_cancel"]) } },
    )
}
