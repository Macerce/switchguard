package com.macerce.switchguard.ui

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.SettingsBackupRestore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.macerce.switchguard.BuildConfig
import com.macerce.switchguard.R
import com.macerce.switchguard.core.BackupException
import com.macerce.switchguard.data.Backup
import com.macerce.switchguard.data.Store
import com.macerce.switchguard.service.MonitorService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import java.util.Locale

private const val MIN_PASSWORD = 6

/**
 * Ayarlar → Yedekleme: kurallar, otomasyonlar ve bağlantı anahtarları şifreli bir dosyaya kaydedilir ya da
 * dosyadan geri yüklenir (yeni telefona geçiş). Şifre hiçbir yerde saklanmaz.
 */
@Composable
fun BackupSection() {
    val context = LocalContext.current
    val resources = LocalResources.current
    val store = remember { Store.get(context) }
    val scope = rememberCoroutineScope()
    var askCreate by remember { mutableStateOf(false) }
    var createPassword by remember { mutableStateOf<CharArray?>(null) }
    var openUri by remember { mutableStateOf<Uri?>(null) }
    var openError by remember { mutableStateOf<String?>(null) }
    var pending by remember { mutableStateOf<Pair<Backup.Summary, Map<String, Any>>?>(null) }
    var busy by remember { mutableStateOf(false) }
    val toast = { id: Int -> Toast.makeText(context, resources.getString(id), Toast.LENGTH_LONG).show() }

    val saver = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val password = createPassword
        createPassword = null
        if (uri == null || password == null) return@rememberLauncherForActivityResult
        busy = true
        scope.launch {
            val ok = runCatching {
                withContext(Dispatchers.IO) {
                    // Anahtar türetme (PBKDF2, 600 bin tur) birkaç saniye sürebilir.
                    val bytes = Backup.create(store, BuildConfig.VERSION_NAME, password)
                    (context.contentResolver.openOutputStream(uri, "wt") ?: error("no output")).use { it.write(bytes) }
                }
            }.isSuccess
            password.fill(' ')
            busy = false
            toast(if (ok) R.string.backup_saved else R.string.backup_failed)
        }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            openError = null
            openUri = uri
        }
    }

    SectionTitle(stringResource(R.string.section_backup))
    SectionCard {
        InfoRow(
            Icons.Rounded.Backup, stringResource(R.string.backup_create), stringResource(R.string.backup_create_desc),
            Modifier.clickable { askCreate = true },
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.surface)
        InfoRow(
            Icons.Rounded.SettingsBackupRestore, stringResource(R.string.backup_restore), stringResource(R.string.backup_restore_desc),
            Modifier.clickable { runCatching { picker.launch(arrayOf("*/*")) }.onFailure { toast(R.string.backup_failed) } },
        )
    }

    if (askCreate) {
        PasswordDialog(
            title = stringResource(R.string.backup_password_title_create),
            hint = stringResource(R.string.backup_password_hint_create),
            confirm = true, busy = false, error = null,
            onOk = { password ->
                askCreate = false
                createPassword = password
                runCatching { saver.launch(fileName(context)) }.onFailure { toast(R.string.backup_failed) }
            },
            onDismiss = { askCreate = false },
        )
    }

    openUri?.let { uri ->
        PasswordDialog(
            title = stringResource(R.string.backup_password_title_open),
            hint = null, confirm = false, busy = busy, error = openError,
            onOk = { password ->
                busy = true
                scope.launch {
                    val result = runCatching { withContext(Dispatchers.IO) { Backup.read(readLimited(context, uri), password) } }
                    password.fill(' ')
                    busy = false
                    result.onSuccess {
                        pending = it
                        openUri = null
                    }.onFailure { e ->
                        openError = resources.getString(
                            when ((e as? BackupException)?.kind) {
                                BackupException.Kind.WRONG_PASSWORD_OR_DAMAGED -> R.string.backup_wrong_password
                                BackupException.Kind.TOO_NEW -> R.string.backup_too_new
                                BackupException.Kind.OTHER_PLATFORM -> R.string.backup_other_platform
                                BackupException.Kind.NOT_BACKUP, BackupException.Kind.TOO_BIG -> R.string.backup_not_backup
                                else -> R.string.backup_failed
                            }
                        )
                    }
                }
            },
            onDismiss = { if (!busy) openUri = null },
        )
    }

    pending?.let { (summary, settings) ->
        ConfirmDialog(
            title = stringResource(R.string.backup_restore_title),
            body = summaryText(context, summary),
            confirm = stringResource(R.string.action_restore),
            onConfirm = {
                Backup.apply(store, settings)
                if (store.monitoringEnabled) MonitorService.send(context, MonitorService.ACTION_RESTART)
                toast(R.string.backup_restored)
            },
            onDismiss = { pending = null },
        )
    }

    // Yedek hazırlanırken (şifre penceresi kapandıktan sonra) kısa bir bekleme göstergesi.
    if (busy && openUri == null) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(24.dp))
                    Spacer(Modifier.width(16.dp))
                    Text(stringResource(R.string.backup_working))
                }
            },
        )
    }
}

@Composable
private fun PasswordDialog(
    title: String,
    hint: String?,
    confirm: Boolean,
    busy: Boolean,
    error: String?,
    onOk: (CharArray) -> Unit,
    onDismiss: () -> Unit,
) {
    var first by remember { mutableStateOf("") }
    var second by remember { mutableStateOf("") }
    val tooShort = confirm && first.length < MIN_PASSWORD
    val mismatch = confirm && second.isNotEmpty() && first != second
    val ready = first.isNotEmpty() && !tooShort && (!confirm || first == second) && !busy
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                hint?.let { Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 12.dp)) }
                OutlinedTextField(
                    value = first, onValueChange = { first = it }, singleLine = true, enabled = !busy,
                    label = { Text(stringResource(R.string.backup_password)) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    supportingText = if (confirm && first.isNotEmpty() && tooShort) {
                        { Text(stringResource(R.string.backup_password_short)) }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (confirm) {
                    OutlinedTextField(
                        value = second, onValueChange = { second = it }, singleLine = true,
                        label = { Text(stringResource(R.string.backup_password_repeat)) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        isError = mismatch,
                        supportingText = if (mismatch) {
                            { Text(stringResource(R.string.backup_password_mismatch)) }
                        } else null,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                }
                if (busy) {
                    Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(R.string.backup_working))
                    }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp)) }
            }
        },
        confirmButton = {
            TextButton(modifier = Modifier.tvFocusRing(), enabled = ready, onClick = { onOk(first.toCharArray()) }) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(modifier = Modifier.tvFocusRing(), enabled = !busy, onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

private fun fileName(context: Context): String =
    "switchguard-" + context.getString(R.string.backup_file_word) + "-" +
        SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date()) + ".sgbackup"

/** Dosyayı en fazla [Backup.MAX_BYTES] + 1 bayt okur (büyükse [Backup.read] reddeder). */
private fun readLimited(context: Context, uri: Uri): ByteArray {
    val input = context.contentResolver.openInputStream(uri) ?: error("no input")
    return input.use {
        val out = ByteArrayOutputStream()
        val buf = ByteArray(8192)
        while (out.size() <= Backup.MAX_BYTES) {
            val n = it.read(buf)
            if (n < 0) break
            out.write(buf, 0, n)
        }
        out.toByteArray()
    }
}

private fun summaryText(context: Context, s: Backup.Summary): String {
    val locale = context.resources.configuration.locales[0]
    val created = runCatching {
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale).format(Date(Instant.parse(s.created).toEpochMilli()))
    }.getOrDefault(s.created)
    val res = context.resources
    val items = buildList {
        if (s.ewelinkKeys) add(context.getString(R.string.backup_item_ewelink))
        if (s.tuya) add(context.getString(R.string.backup_item_tuya))
        if (s.rules > 0) add(res.getQuantityString(R.plurals.backup_item_rules, s.rules, s.rules))
        if (s.automations > 0) add(res.getQuantityString(R.plurals.backup_item_automations, s.automations, s.automations))
        add(context.getString(R.string.backup_item_settings))
    }
    return context.getString(R.string.backup_restore_body, created, s.appVersion, items.joinToString("\n") { "• $it" })
}
