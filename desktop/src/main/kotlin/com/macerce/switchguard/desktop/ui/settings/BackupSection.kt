package com.macerce.switchguard.desktop.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.macerce.switchguard.desktop.AppGraph
import com.macerce.switchguard.desktop.backup.BackupException
import com.macerce.switchguard.desktop.backup.BackupService
import com.macerce.switchguard.desktop.i18n.Strings
import com.macerce.switchguard.desktop.platform.Autostart
import com.macerce.switchguard.desktop.syncAutostart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.awt.Frame
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDate

private const val MIN_PASSWORD = 8

/** Yedekleme akışının adımı. */
private sealed interface Step {
    data class NewPassword(val target: Path) : Step
    data class Password(val source: Path) : Step
    data class Confirm(val loaded: BackupService.Loaded) : Step
    data object Working : Step
    data class Message(val text: String) : Step
}

/**
 * Ayarlar → Yedekleme: "Dosyaya yedekle…" (dosya seç → şifre ×2 → yaz) ve "Dosyadan geri yükle…" (dosya seç → şifre →
 * özet ve uyarı → izlemeyi durdur → geri yükle → Windows ile başlat ayarını uygula → gerekirse izlemeyi başlat).
 * Dosya işleri arka planda; [chooseTarget]/[chooseSource] testlerde değiştirilir (Windows dosya penceresi açılmasın).
 */
@Composable
fun BackupSection(
    graph: AppGraph,
    strings: Strings,
    chooseTarget: (String) -> Path? = { chooseBackupTarget(it, strings["desktop_backup_create"]) },
    chooseSource: () -> Path? = { chooseBackupSource(strings["desktop_backup_restore"]) },
) {
    val service = remember { BackupService(graph.store, graph.soundsDir, graph.dataDir.resolve("settings.json"), appVersion()) }
    var step by remember { mutableStateOf<Step?>(null) }
    val scope = rememberCoroutineScope()

    SettingRow(strings["desktop_backup_create"], strings["desktop_backup_create_desc"], onClick = {
        chooseTarget(BackupService.defaultFileName(LocalDate.now()))?.let { step = Step.NewPassword(withExtension(it)) }
    })
    SettingRow(strings["desktop_backup_restore"], strings["desktop_backup_restore_desc"], onClick = {
        chooseSource()?.let { step = Step.Password(it) }
    })

    fun errorText(e: BackupException, failedKey: String) = when (e.kind) {
        BackupException.Kind.WRONG_PASSWORD_OR_DAMAGED -> strings["desktop_restore_wrong_password"]
        BackupException.Kind.NOT_BACKUP -> strings["desktop_restore_not_backup"]
        BackupException.Kind.TOO_NEW -> strings["desktop_restore_too_new"]
        BackupException.Kind.TOO_BIG -> strings["desktop_restore_too_big"]
        BackupException.Kind.IO -> strings.format(failedKey, e.cause?.javaClass?.simpleName ?: e.kind.name)
    }

    when (val s = step) {
        null -> Unit
        is Step.NewPassword -> PasswordDialog(strings, confirm = true, onDismiss = { step = null }) { password ->
            step = Step.Working
            scope.launch {
                step = try {
                    withContext(Dispatchers.IO) { service.write(s.target, password) }
                    Step.Message(strings.format("desktop_backup_saved_x", s.target.toString()))
                } catch (e: BackupException) {
                    Step.Message(errorText(e, "desktop_backup_failed_x"))
                } finally {
                    password.fill('\u0000')
                }
            }
        }
        is Step.Password -> PasswordDialog(strings, confirm = false, onDismiss = { step = null }) { password ->
            step = Step.Working
            scope.launch {
                step = try {
                    Step.Confirm(withContext(Dispatchers.IO) { service.read(s.source, password) })
                } catch (e: BackupException) {
                    Step.Message(errorText(e, "desktop_restore_failed_x"))
                } finally {
                    password.fill('\u0000')
                }
            }
        }
        is Step.Confirm -> {
            val sum = s.loaded.summary
            val yesNo = { b: Boolean -> strings[if (b) "desktop_yes" else "desktop_no"] }
            AlertDialog(
                onDismissRequest = { step = null },
                title = { Text(strings["desktop_backup_restore"]) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(strings.format("desktop_restore_summary", sum.created, sum.appVersion, yesNo(sum.tuya), yesNo(sum.ewelinkKeys), sum.rules, sum.automations, sum.sounds))
                        Text(strings["desktop_restore_warning"], color = MaterialTheme.colorScheme.error)
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        step = Step.Working
                        graph.engine.stop()
                        scope.launch {
                            step = try {
                                withContext(Dispatchers.IO) { service.apply(s.loaded) }
                                val store = graph.store
                                syncAutostart(Autostart(), store.autostart)
                                if (store.monitoringEnabled && store.hasAnyAccount) graph.engine.restart()
                                Step.Message(strings[if (store.hasCredentials) "desktop_restore_done_login" else "desktop_restore_done"])
                            } catch (e: BackupException) {
                                Step.Message(errorText(e, "desktop_restore_failed_x"))
                            }
                        }
                    }) { Text(strings["desktop_restore_confirm"]) }
                },
                dismissButton = { TextButton(onClick = { step = null }) { Text(strings["action_cancel"]) } },
            )
        }
        Step.Working -> AlertDialog(
            onDismissRequest = {},
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(strings["desktop_backup_working"])
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            },
            confirmButton = {},
        )
        is Step.Message -> AlertDialog(
            onDismissRequest = { step = null },
            text = { Text(s.text) },
            confirmButton = { Button(onClick = { step = null }) { Text(strings["desktop_ok"]) } },
        )
    }
}

/**
 * Şifre penceresi. [confirm] ise iki kez sorulur ve en az [MIN_PASSWORD] karakter istenir (yedek alırken);
 * değilse tek alan (geri yüklerken). Şifre [onOk]'a karakter dizisi olarak verilir, iş bitince sıfırlanır.
 */
@Composable
private fun PasswordDialog(strings: Strings, confirm: Boolean, onDismiss: () -> Unit, onOk: (CharArray) -> Unit) {
    var first by remember { mutableStateOf("") }
    var second by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings[if (confirm) "desktop_backup_create" else "desktop_backup_restore"]) },
        text = {
            Column(Modifier.widthIn(min = 420.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (confirm) Text(strings["desktop_backup_password_rules"], style = MaterialTheme.typography.bodyMedium)
                PasswordField(first, { first = it }, strings["desktop_backup_password"])
                if (confirm) PasswordField(second, { second = it }, strings["desktop_backup_password_again"])
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(enabled = first.isNotEmpty(), onClick = {
                error = when {
                    confirm && first.length < MIN_PASSWORD -> strings["desktop_backup_password_short"]
                    confirm && first != second -> strings["desktop_backup_password_mismatch"]
                    else -> null
                }
                if (error == null) onOk(first.toCharArray())
            }) { Text(strings["desktop_ok"]) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings["action_cancel"]) } },
    )
}

@Composable
private fun PasswordField(value: String, onChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value, onChange, Modifier.fillMaxWidth(),
        label = { Text(label) }, singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
    )
}

private fun withExtension(path: Path): Path =
    if (path.fileName.toString().endsWith(".${BackupService.EXTENSION}", ignoreCase = true)) path
    else path.resolveSibling("${path.fileName}.${BackupService.EXTENSION}")

/** Windows "Farklı kaydet" penceresi; vazgeçilirse null. */
fun chooseBackupTarget(defaultName: String, title: String): Path? {
    val dialog = FileDialog(null as Frame?, title, FileDialog.SAVE).apply { file = defaultName }
    dialog.isVisible = true
    return Paths.get(dialog.directory ?: return null, dialog.file ?: return null)
}

/** Windows "Aç" penceresi (`*.sgbackup`); vazgeçilirse null. */
fun chooseBackupSource(title: String): Path? {
    val dialog = FileDialog(null as Frame?, title, FileDialog.LOAD).apply { file = "*.${BackupService.EXTENSION}" }
    dialog.isVisible = true
    return Paths.get(dialog.directory ?: return null, dialog.file ?: return null)
}
