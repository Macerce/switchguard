package com.macerce.switchguard.ui

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.macerce.switchguard.R
import com.macerce.switchguard.billing.Billing
import com.macerce.switchguard.core.Entitlement

/** Pro'nun ne verdiğini anlatır ve Play satın alma ekranını açar. [reason] pencerenin neden açıldığını söyler. */
@Composable
fun ProDialog(onDismiss: () -> Unit, reason: String? = null) {
    val context = LocalContext.current
    val billing = remember { Billing.get(context) }
    val price by billing.price.collectAsStateWithLifecycle()
    val state by billing.state.collectAsStateWithLifecycle()

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.WorkspacePremium, null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text(stringResource(R.string.pro_title)) },
        text = {
            Column {
                reason?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(12.dp))
                }
                Text(stringResource(R.string.pro_body, Entitlement.FREE_DEVICES))
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(
                        when (state) {
                            Billing.State.PENDING -> R.string.pro_pending
                            Billing.State.UNAVAILABLE -> R.string.pro_unavailable
                            else -> R.string.pro_once
                        }
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = price != null && state == Billing.State.READY,
                onClick = {
                    val activity = context as? Activity
                    if (activity == null || !billing.purchase(activity)) {
                        Toast.makeText(context, R.string.pro_unavailable, Toast.LENGTH_LONG).show()
                    }
                    onDismiss()
                },
            ) {
                Text(
                    price?.let { stringResource(R.string.pro_buy, it) }
                        ?: stringResource(if (state == Billing.State.UNAVAILABLE) R.string.pro_upgrade else R.string.pro_loading)
                )
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_later)) } },
    )
}

/** Ayarlardaki Pro satırı: Pro ise teşekkür, değilse satın alma penceresi. */
@Composable
fun ProRow(isPro: Boolean, onUpgrade: () -> Unit) {
    val context = LocalContext.current
    InfoRow(
        Icons.Rounded.WorkspacePremium,
        stringResource(R.string.pro_title),
        stringResource(if (isPro) R.string.pro_active else R.string.pro_free_status, Entitlement.FREE_DEVICES),
        if (isPro) Modifier else Modifier.clickable(onClick = onUpgrade),
    ) {
        if (!isPro) {
            TextButton(onClick = {
                // Başka telefonda alınmış satın almayı getirir.
                Billing.get(context).refresh()
                Toast.makeText(context, R.string.pro_restoring, Toast.LENGTH_SHORT).show()
            }) { Text(stringResource(R.string.pro_restore)) }
        }
    }
}
