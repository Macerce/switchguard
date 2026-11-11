package com.macerce.switchguard.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.macerce.switchguard.R

/**
 * Konum izni istenmeden hemen önce gösterilen belirgin açıklama (Google Play "Prominent Disclosure and Consent").
 * Sistem izin penceresi yalnızca "Kabul ediyorum"a dokunulunca açılır; pencereyi kapatmak ya da
 * "Hayır, teşekkürler" demek izin istemez. Her konum izni isteği buradan geçmelidir.
 */
@Composable
fun LocationConsentDialog(text: String, onAgree: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.LocationOn, null) },
        title = { Text(stringResource(R.string.loc_consent_title)) },
        text = { Text(text) },
        confirmButton = {
            Button(modifier = Modifier.tvFocusRing(), onClick = { onDismiss(); onAgree() }) {
                Text(stringResource(R.string.loc_consent_agree))
            }
        },
        dismissButton = {
            TextButton(modifier = Modifier.tvFocusRing(), onClick = onDismiss) {
                Text(stringResource(R.string.loc_consent_deny))
            }
        },
    )
}
