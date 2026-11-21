package com.macerce.switchguard.ui

import android.text.format.DateFormat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.macerce.switchguard.R
import com.macerce.switchguard.core.Sun
import com.macerce.switchguard.data.RuntimeState
import com.macerce.switchguard.data.Store
import com.macerce.switchguard.ui.theme.Brand
import com.macerce.switchguard.ui.theme.tileColor
import java.util.Date
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Konum sayfasının üstü (tanıtım videosundaki görünüm): bölge dairesiyle harita, gün doğumu/batımı yayı, dış sıcaklık
 * ve "konum telefonda kalır" notu. Konum ayarlı değilse yalnızca not görünür.
 */
@Composable
fun LocationHeader(store: Store, version: Int, showMap: Boolean) {
    val context = LocalContext.current
    val lat = store.latitude
    val lon = store.longitude
    val zones = remember(version) { store.zones }
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (lat != null && lon != null) {
            if (showMap) {
                // Bölge varsa ilk bölgenin dairesi, yoksa ev konumu (daire çizilmez).
                val center = zones.firstOrNull()
                val state = remember(center, lat, lon) {
                    MapState(center?.latitude ?: lat, center?.longitude ?: lon, 15.0)
                }
                TileMap(state, center?.radius ?: 0, Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(20.dp)))
            }
            val now = System.currentTimeMillis()
            val rise = remember(lat, lon) { Sun.time(now, lat, lon, sunrise = true) }
            val set = remember(lat, lon) { Sun.time(now, lat, lon, sunrise = false) }
            val temp = remember(version) { RuntimeState.get(context).outdoorTemp }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SunCard(rise, set, now, Modifier.weight(1.4f))
                if (temp != null) {
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(20.dp)).background(tileColor()).padding(16.dp),
                    ) {
                        Icon(Icons.Rounded.Thermostat, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(6.dp))
                        Text(formatNumber(Math.round(temp * 10) / 10.0) + " °C", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.location_outside), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        Row(Modifier.padding(horizontal = 8.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Lock, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.location_private), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Gün yayı: doğuştan batışa yarım daire, güneş şu anki yerinde; altında doğuş ve batış saatleri. */
@Composable
private fun SunCard(rise: Long?, set: Long?, now: Long, modifier: Modifier) {
    val context = LocalContext.current
    val track = MaterialTheme.colorScheme.outlineVariant
    val sun = Brand.amber
    Column(modifier.clip(RoundedCornerShape(20.dp)).background(tileColor()).padding(16.dp)) {
        Canvas(Modifier.fillMaxWidth().height(64.dp)) {
            val stroke = 3.dp.toPx()
            val r = minOf(size.width / 2, size.height) - stroke
            val c = Offset(size.width / 2, size.height)
            drawArc(track, 180f, 180f, false, Offset(c.x - r, c.y - r), Size(r * 2, r * 2), style = Stroke(stroke, cap = StrokeCap.Round))
            if (rise != null && set != null && set > rise) {
                val p = ((now - rise).toFloat() / (set - rise)).coerceIn(0f, 1f)
                val a = PI * (1 - p)
                drawArc(sun, 180f, 180f * p, false, Offset(c.x - r, c.y - r), Size(r * 2, r * 2), style = Stroke(stroke, cap = StrokeCap.Round))
                drawCircle(sun, 7.dp.toPx(), Offset(c.x + (r * cos(a)).toFloat(), c.y - (r * sin(a)).toFloat()))
            }
        }
        Spacer(Modifier.height(8.dp))
        val fmt = DateFormat.getTimeFormat(context)
        if (rise != null) Text(stringResource(R.string.evt_sunrise) + "  " + fmt.format(Date(rise)), style = MaterialTheme.typography.labelMedium)
        if (set != null) Text(stringResource(R.string.evt_sunset) + "  " + fmt.format(Date(set)), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
    }
}
