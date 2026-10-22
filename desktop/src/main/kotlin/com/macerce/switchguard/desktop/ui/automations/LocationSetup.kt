package com.macerce.switchguard.desktop.ui.automations

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.macerce.switchguard.data.Store
import com.macerce.switchguard.desktop.i18n.Strings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Gün doğumu/batımı ve dış sıcaklık kuralları için konum (telefondaki `LocationSetup`). Bilgisayarın konum servisi
 * yerine şehir adıyla arama (Open-Meteo, anahtarsız) ya da elle enlem/boylam. Konum yalnızca bu bilgisayarda tutulur.
 */
@Composable
fun LocationSetup(store: Store, strings: Strings, onChanged: () -> Unit, modifier: Modifier = Modifier) {
    var manual by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf(false) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val lat = store.latitude
        val lon = store.longitude
        Text(
            if (lat != null && lon != null) strings.format("location_set", formatCoord(lat), formatCoord(lon)) else strings["location_needed"],
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = { search = true }, modifier = Modifier.testTag("location-search")) { Text(strings["desktop_location_search"]) }
            TextButton(onClick = { manual = true }) { Text(strings["location_manual"]) }
        }
    }
    if (search) CitySearchDialog(strings, onDismiss = { search = false }) { la, lo ->
        store.latitude = la
        store.longitude = lo
        onChanged()
    }
    if (manual) ManualLocationDialog(store, strings, onDismiss = { manual = false }, onSaved = onChanged)
}

private data class Place(val name: String, val detail: String, val lat: Double, val lon: Double)

private val http by lazy { OkHttpClient.Builder().callTimeout(15, TimeUnit.SECONDS).build() }

/** Open-Meteo yer adı araması: en çok 6 sonuç. */
private fun searchPlaces(query: String, language: String): List<Place> {
    val url = "https://geocoding-api.open-meteo.com/v1/search".toHttpUrl().newBuilder()
        .addQueryParameter("name", query).addQueryParameter("count", "6").addQueryParameter("language", language).build()
    http.newCall(Request.Builder().url(url).build()).execute().use { r ->
        if (!r.isSuccessful) throw java.io.IOException("HTTP ${r.code}")
        val arr = JSONObject(r.body?.string().orEmpty()).optJSONArray("results") ?: return emptyList()
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            Place(
                o.optString("name"),
                listOf(o.optString("admin1"), o.optString("country")).filter { it.isNotBlank() }.joinToString(", "),
                o.getDouble("latitude"), o.getDouble("longitude"),
            )
        }
    }
}

@Composable
private fun CitySearchDialog(strings: Strings, onDismiss: () -> Unit, onPick: (Double, Double) -> Unit) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Place>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    fun run() {
        if (query.isBlank() || busy) return
        busy = true
        error = null
        scope.launch {
            val r = withContext(Dispatchers.IO) { runCatching { searchPlaces(query.trim(), strings.language) } }
            busy = false
            r.onSuccess { results = it }.onFailure { error = strings["state_no_internet"] }
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings["desktop_location_search"]) },
        text = {
            Column(Modifier.width(420.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(strings["desktop_location_search_desc"], style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        query, { query = it.take(60) }, singleLine = true,
                        label = { Text(strings["desktop_location_city"]) },
                        modifier = Modifier.weight(1f).testTag("city-query"),
                    )
                    FilledTonalButton(enabled = !busy && query.isNotBlank(), onClick = { run() }, modifier = Modifier.padding(top = 8.dp)) {
                        Text(strings["desktop_location_find"])
                    }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                results?.let { list ->
                    if (list.isEmpty()) Text(strings["desktop_location_none"], style = MaterialTheme.typography.bodySmall)
                    list.forEach { p ->
                        Column(
                            Modifier.fillMaxWidth().clickable { onPick(p.lat, p.lon); onDismiss() }.padding(vertical = 6.dp, horizontal = 4.dp),
                        ) {
                            Text(p.name, style = MaterialTheme.typography.bodyLarge)
                            Text("${p.detail} · ${formatCoord(p.lat)}, ${formatCoord(p.lon)}", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(strings["action_cancel"]) } },
    )
}

@Composable
private fun ManualLocationDialog(store: Store, strings: Strings, onDismiss: () -> Unit, onSaved: () -> Unit) {
    var lat by remember { mutableStateOf(store.latitude?.let(::formatCoord) ?: "") }
    var lon by remember { mutableStateOf(store.longitude?.let(::formatCoord) ?: "") }
    val latV = lat.replace(',', '.').toDoubleOrNull()?.takeIf { it in -90.0..90.0 }
    val lonV = lon.replace(',', '.').toDoubleOrNull()?.takeIf { it in -180.0..180.0 }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings["location_manual"]) },
        text = {
            Column(Modifier.width(420.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(strings["location_manual_desc"])
                OutlinedTextField(lat, { lat = it.take(12) }, label = { Text(strings["location_lat"]) }, singleLine = true)
                OutlinedTextField(lon, { lon = it.take(12) }, label = { Text(strings["location_lon"]) }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(enabled = latV != null && lonV != null, onClick = {
                store.latitude = latV
                store.longitude = lonV
                onSaved()
                onDismiss()
            }) { Text(strings["action_save"]) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings["action_cancel"]) } },
    )
}

fun formatCoord(v: Double): String = String.format(Locale.US, "%.4f", v)
