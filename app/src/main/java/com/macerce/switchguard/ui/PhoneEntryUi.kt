package com.macerce.switchguard.ui

import android.content.Context
import android.net.ConnectivityManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.macerce.switchguard.R
import com.macerce.switchguard.core.PhoneEntry
import com.macerce.switchguard.core.PhoneEntryServer
import java.net.Inet4Address

/**
 * TV'de uzun anahtarları (App Secret, bot anahtarı…) kumandayla yazmak yerine telefondan girme düğmesi.
 * Telefonda ve tablette görünmez: orada alanlara doğrudan yapıştırılır.
 */
@Composable
fun PhoneEntryButton(title: String, fields: List<PhoneEntry.Field>, onReceived: (Map<String, String>) -> Unit, modifier: Modifier = Modifier) {
    if (!LocalIsTv.current) return
    var open by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { open = true }, modifier = modifier.tvFocusRing()) {
        Icon(Icons.Rounded.QrCode2, null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.phone_entry_button))
    }
    if (open) {
        PhoneEntryDialog(
            title, fields,
            onReceived = { values ->
                open = false
                onReceived(values)
            },
            onDismiss = { open = false },
        )
    }
}

/** QR kodu ve adresi gösterir; pencere açık kaldıkça ev ağında sayfa sunulur, kapanınca sunucu durur. */
@Composable
private fun PhoneEntryDialog(title: String, fields: List<PhoneEntry.Field>, onReceived: (Map<String, String>) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val resources = LocalResources.current
    // Sunucu bu pencereye ait: açılınca başlar, kapanınca (ya da gönderim gelince) durur.
    val session = remember {
        val host = lanAddress(context) ?: return@remember null
        val key = PhoneEntry.newKey()
        val token = PhoneEntry.newToken()
        val strings = mapOf(
            "lang" to resources.getString(R.string.phone_entry_lang),
            "lead" to resources.getString(R.string.phone_entry_page_lead),
            "badLink" to resources.getString(R.string.phone_entry_page_bad_link),
            "paste" to resources.getString(R.string.phone_entry_page_paste),
            "send" to resources.getString(R.string.phone_entry_page_send),
            "sending" to resources.getString(R.string.phone_entry_page_sending),
            "sent" to resources.getString(R.string.phone_entry_page_sent),
            "sentBody" to resources.getString(R.string.phone_entry_page_sent_body),
            "failed" to resources.getString(R.string.phone_entry_page_failed),
            "empty" to resources.getString(R.string.phone_entry_page_empty),
            "note" to resources.getString(R.string.phone_entry_page_note),
        )
        val template = context.assets.open("phone_entry.html").bufferedReader().use { it.readText() }
        val page = PhoneEntry.page(template, PhoneEntry.config(title, fields, strings))
        val server = PhoneEntryServer(page, token, key, onReceived)
        val port = server.start() ?: return@remember null
        server to PhoneEntry.url(host, port, token, key)
    }
    DisposableEffect(Unit) { onDispose { session?.first?.stop() } }

    AlertDialog(
        onDismissRequest = onDismiss,
        // TV'nin varsayılan pencere genişliği (≈320 dp) QR ile yanındaki adımları sığdırmaz.
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.width(600.dp),
        title = { Text(stringResource(R.string.phone_entry_title)) },
        text = {
            if (session == null) Text(stringResource(R.string.phone_entry_no_network))
            else Column {
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    QrCode(session.second, Modifier.size(220.dp))
                    // Sabit genişlik: pencere genişliğini içerik belirler; ağırlık verilirse yazıya yer kalmaz.
                    Column(Modifier.width(260.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.phone_entry_step1), style = MaterialTheme.typography.bodyMedium)
                        Text(stringResource(R.string.phone_entry_step2), style = MaterialTheme.typography.bodyMedium)
                        Text(
                            stringResource(R.string.phone_entry_step3),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(10.dp))
                            Text(stringResource(R.string.phone_entry_waiting), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                Text(
                    stringResource(R.string.phone_entry_manual, session.second),
                    // QR + yazı sütunu kadar geniş: uzun adres pencereyi ekran boyu genişletmesin.
                    modifier = Modifier.width(500.dp).padding(top = 16.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.tvFocusRing()) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

/** Siyah-beyaz QR kodu; okunabilsin diye her temada beyaz zemin ve kenar boşluğuyla. */
@Composable
fun QrCode(text: String, modifier: Modifier = Modifier) {
    val matrix = remember(text) {
        runCatching {
            QRCodeWriter().encode(
                text, BarcodeFormat.QR_CODE, 0, 0,
                mapOf(EncodeHintType.MARGIN to 2, EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M),
            )
        }.getOrNull()
    } ?: return
    Canvas(modifier.aspectRatio(1f).clip(RoundedCornerShape(12.dp)).background(Color.White)) {
        val cell = size.minDimension / matrix.width
        for (y in 0 until matrix.height) for (x in 0 until matrix.width) {
            // Hücreler biraz taşar: kayan nokta yuvarlamasında modüller arasında ince beyaz çizgi kalmasın.
            if (matrix.get(x, y)) drawRect(Color.Black, topLeft = Offset(x * cell, y * cell), size = Size(cell + 0.6f, cell + 0.6f))
        }
    }
}

/** Bu cihazın ev ağındaki IPv4 adresi (Wi-Fi ya da kablo); ağ yoksa null. */
private fun lanAddress(context: Context): String? {
    val cm = context.getSystemService(ConnectivityManager::class.java)
    val addresses = cm.getLinkProperties(cm.activeNetwork)?.linkAddresses?.map { it.address }.orEmpty()
        .filterIsInstance<Inet4Address>().filter { !it.isLoopbackAddress }
    return (addresses.firstOrNull { it.isSiteLocalAddress } ?: addresses.firstOrNull())?.hostAddress
}
