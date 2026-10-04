package com.macerce.switchguard.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.macerce.switchguard.R
import com.macerce.switchguard.core.Transfer
import com.macerce.switchguard.core.TransferDiscovery
import com.macerce.switchguard.core.TransferPeer
import com.macerce.switchguard.core.TransferReceiver
import com.macerce.switchguard.core.TransferSender
import com.macerce.switchguard.data.Store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Alıcı: kodu gösterir ve aynı ağdaki telefondan gelecek kurulumu bekler.
 * [onReceived] veri uygulandıktan sonra çağrılır.
 */
@Composable
fun ReceiveTransferPanel(onReceived: () -> Unit) {
    val context = LocalContext.current
    val store = remember { Store.get(context) }
    var code by remember { mutableStateOf("") }
    var done by remember { mutableStateOf(false) }
    var source by remember { mutableStateOf("") }
    DisposableEffect(Unit) {
        val receiver = TransferReceiver(
            context,
            onNewCode = { code = it },
            onReceived = { data ->
                runCatching { Transfer.import(store, data) }.onSuccess {
                    source = it
                    done = true
                    onReceived()
                }
            },
        )
        receiver.start()
        onDispose { receiver.stop() }
    }

    if (done) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(10.dp))
            Text(stringResource(R.string.transfer_received), style = MaterialTheme.typography.titleMedium)
        }
        if (store.transferSource.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            SameAccountWarning(store)
        }
        return
    }
    Text(stringResource(R.string.transfer_receive_steps), style = MaterialTheme.typography.bodyLarge)
    Spacer(Modifier.height(20.dp))
    Text(stringResource(R.string.transfer_code_label), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(
        code.chunked(3).joinToString(" "),
        fontSize = 56.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace,
        color = MaterialTheme.colorScheme.primary, letterSpacing = 4.sp,
    )
    Spacer(Modifier.height(12.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
        Spacer(Modifier.width(10.dp))
        Text(stringResource(R.string.transfer_waiting), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * Bu cihaza kurulum telefondan aktarıldıysa: o telefondaki eWeLink hesabıyla giriş yapılmamalı
 * (eWeLink hesap başına tek oturum tutar, telefondaki oturum kapanır). Aynı hesapla girildiyse kırmızı uyarı.
 */
@Composable
fun SameAccountWarning(store: Store, sender: Boolean = false) {
    val source = store.transferSource
    val known = source.isNotEmpty() && source != Transfer.UNKNOWN_ACCOUNT
    val same = known && store.isLoggedIn && store.accountEmail.equals(source, ignoreCase = true)
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (same) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.tertiaryContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp)) {
            Icon(Icons.Rounded.Warning, null)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    stringResource(if (same) R.string.transfer_same_account_title else R.string.transfer_other_account_title),
                    style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    when {
                        same -> stringResource(R.string.transfer_same_account_body, Transfer.maskEmail(source))
                        sender -> stringResource(R.string.transfer_send_warning_body)
                        known -> stringResource(R.string.transfer_other_account_body, Transfer.maskEmail(source))
                        else -> stringResource(R.string.transfer_other_account_body_generic)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (LocalIsTv.current) {
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.transfer_tv_qr_hint), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

/** Gönderen: ağdaki SwitchGuard'ları bulur, seçilene kodla kurulumu gönderir. */
@Composable
fun SendTransferDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val store = remember { Store.get(context) }
    val scope = rememberCoroutineScope()
    var peers by remember { mutableStateOf(emptyList<TransferPeer>()) }
    var target by remember { mutableStateOf<TransferPeer?>(null) }
    var code by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<TransferSender.Result?>(null) }
    DisposableEffect(Unit) {
        val d = TransferDiscovery(context) { peers = it }
        d.start()
        onDispose { d.stop() }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.transfer_send_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                when (val r = result) {
                    is TransferSender.Result.Ok -> {
                        Text(stringResource(R.string.transfer_sent_ok, r.device.ifEmpty { target?.name.orEmpty() }))
                        if (store.isLoggedIn) Text(
                            stringResource(R.string.transfer_sent_ewelink_note),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        return@Column
                    }
                    else -> Unit
                }
                val t = target
                if (t == null) {
                    SameAccountWarning(store, sender = true)
                    Text(stringResource(R.string.transfer_send_body), style = MaterialTheme.typography.bodyMedium)
                    if (peers.isEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(10.dp))
                            Text(stringResource(R.string.transfer_searching), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    peers.forEach { p ->
                        InfoRow(
                            Icons.Rounded.Devices, p.name, p.host,
                            Modifier.clickable { target = p; result = null },
                        )
                    }
                } else {
                    Text(stringResource(R.string.transfer_enter_code, t.name), style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(
                        code, { v -> code = v.filter(Char::isDigit).take(6) },
                        label = { Text(stringResource(R.string.transfer_code_label)) },
                        singleLine = true,
                        keyboardOptions = tvKeyboardOptions(KeyboardOptions(keyboardType = KeyboardType.NumberPassword)),
                        modifier = Modifier.fillMaxWidth().tvTextField(),
                    )
                    when (val r = result) {
                        TransferSender.Result.WrongCode -> Text(stringResource(R.string.transfer_wrong_code), color = MaterialTheme.colorScheme.error)
                        is TransferSender.Result.Failed -> Text(stringResource(R.string.transfer_failed), color = MaterialTheme.colorScheme.error)
                        else -> Unit
                    }
                }
            }
        },
        confirmButton = {
            val t = target
            if (result is TransferSender.Result.Ok) {
                TextButton(onClick = onDismiss, modifier = Modifier.tvFocusRing()) { Text(stringResource(R.string.action_done)) }
            } else if (t != null) {
                TextButton(
                    enabled = code.length == 6 && !sending,
                    modifier = Modifier.tvFocusRing(),
                    onClick = {
                        sending = true
                        scope.launch {
                            result = withContext(Dispatchers.IO) { TransferSender.send(t, code, Transfer.export(store)) }
                            sending = false
                        }
                    },
                ) { Text(stringResource(if (sending) R.string.transfer_sending else R.string.transfer_send_action)) }
            }
        },
        dismissButton = {
            if (result !is TransferSender.Result.Ok) {
                TextButton(onClick = { if (target != null) { target = null; code = ""; result = null } else onDismiss() }, modifier = Modifier.tvFocusRing()) {
                    Text(stringResource(if (target != null) R.string.action_back else R.string.action_cancel))
                }
            }
        },
    )
}
