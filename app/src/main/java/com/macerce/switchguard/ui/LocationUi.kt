package com.macerce.switchguard.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.macerce.switchguard.R
import com.macerce.switchguard.data.Store
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume

/**
 * Gün doğumu/batımı ve hava sıcaklığı kuralları için konum: telefonun yaklaşık konumu (izinle, bir kez) ya da elle.
 * Konum yalnızca bu cihazda tutulur; sıcaklık için Open-Meteo'ya yuvarlanmış (≈100 m) olarak gönderilir.
 */
@Composable
fun LocationSetup(store: Store, onChanged: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var manual by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val noLocation = stringResource(R.string.location_unavailable)
    fun fetch() = scope.launch {
        val l = currentLocation(context)
        if (l == null) error = noLocation
        else {
            store.latitude = l.latitude
            store.longitude = l.longitude
            error = null
            onChanged()
        }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) fetch() else manual = true
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val lat = store.latitude
        val lon = store.longitude
        Text(
            if (lat != null && lon != null) stringResource(R.string.location_set, formatCoord(lat), formatCoord(lon))
            else stringResource(R.string.location_needed),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(modifier = Modifier.tvFocusRing(), onClick = {
                if (context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) fetch()
                else permission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
            }) { Text(stringResource(R.string.location_use_phone)) }
            TextButton(modifier = Modifier.tvFocusRing(), onClick = { manual = true }) { Text(stringResource(R.string.location_manual)) }
        }
    }
    if (manual) ManualLocationDialog(store, onDismiss = { manual = false }, onSaved = onChanged)
}

@Composable
private fun ManualLocationDialog(store: Store, onDismiss: () -> Unit, onSaved: () -> Unit) {
    var lat by remember { mutableStateOf(store.latitude?.let(::formatCoord) ?: "") }
    var lon by remember { mutableStateOf(store.longitude?.let(::formatCoord) ?: "") }
    val latV = lat.replace(',', '.').toDoubleOrNull()?.takeIf { it in -90.0..90.0 }
    val lonV = lon.replace(',', '.').toDoubleOrNull()?.takeIf { it in -180.0..180.0 }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.location_manual)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.location_manual_desc))
                val keyboard = KeyboardOptions(keyboardType = KeyboardType.Text)
                OutlinedTextField(lat, { lat = it.take(12) }, label = { Text(stringResource(R.string.location_lat)) }, singleLine = true, keyboardOptions = keyboard, modifier = Modifier.tvTextField())
                OutlinedTextField(lon, { lon = it.take(12) }, label = { Text(stringResource(R.string.location_lon)) }, singleLine = true, keyboardOptions = keyboard, modifier = Modifier.tvTextField())
            }
        },
        confirmButton = {
            TextButton(enabled = latV != null && lonV != null, modifier = Modifier.tvFocusRing(), onClick = {
                store.latitude = latV
                store.longitude = lonV
                onSaved()
                onDismiss()
            }) { Text(stringResource(android.R.string.ok)) }
        },
        dismissButton = { TextButton(modifier = Modifier.tvFocusRing(), onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

fun formatCoord(v: Double): String = String.format(Locale.US, "%.4f", v)

/**
 * Konum: [maxAgeMs]'den yeni son bilinen konum, yoksa Android 11+ anlık konum (en çok ≈30 sn), o da yoksa son bilinen.
 * Hassas konum izni varsa GPS de denenir (bölge seçimi); yalnız yaklaşık izinde ağ konumu.
 */
@SuppressLint("MissingPermission") // Çağırmadan önce izin denetleniyor.
internal suspend fun currentLocation(context: Context, maxAgeMs: Long = 7 * 86_400_000L): Location? {
    val lm = context.getSystemService(LocationManager::class.java) ?: return null
    val fine = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val providers = (if (fine) listOf(LocationManager.FUSED_PROVIDER, LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        else listOf(LocationManager.NETWORK_PROVIDER, LocationManager.FUSED_PROVIDER, LocationManager.GPS_PROVIDER))
        .plus(LocationManager.PASSIVE_PROVIDER)
        .filter { runCatching { lm.isProviderEnabled(it) }.getOrDefault(false) }
    val last = providers.mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }.maxByOrNull { it.time }
    if (last != null && System.currentTimeMillis() - last.time < maxAgeMs) return last
    if (Build.VERSION.SDK_INT >= 30) {
        val provider = providers.firstOrNull { it != LocationManager.PASSIVE_PROVIDER } ?: return last
        return suspendCancellableCoroutine { cont ->
            runCatching {
                lm.getCurrentLocation(provider, null, context.mainExecutor) { cont.resume(it ?: last) }
            }.onFailure { cont.resume(last) }
        }
    }
    return last
}
