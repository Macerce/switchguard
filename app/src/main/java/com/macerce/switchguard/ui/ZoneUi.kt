package com.macerce.switchguard.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import com.macerce.switchguard.BuildConfig
import com.macerce.switchguard.R
import com.macerce.switchguard.api.EwelinkClient
import com.macerce.switchguard.api.WeatherClient
import com.macerce.switchguard.core.Zone
import com.macerce.switchguard.data.Store
import com.macerce.switchguard.service.ZoneWatcher
import java.util.Locale
import java.util.UUID
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sinh
import kotlin.math.tan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.Request

// ---------------------------------------------------------------- bölge listesi (Ayarlar → Konum ve bölgeler)

/** Bölgeler: liste, ekle/düzenle/sil ve arka plan konum izni. */
@Composable
fun ZonesSection(store: Store, version: Int) {
    var editing by remember { mutableStateOf<Zone?>(null) }
    var creating by remember { mutableStateOf(false) }
    val zones = remember(version) { store.zones }
    SectionCard {
        if (zones.isEmpty()) {
            Text(
                stringResource(R.string.zone_none),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
        }
        zones.forEach { z ->
            InfoRow(
                Icons.Rounded.Place, z.name,
                stringResource(R.string.zone_summary, "${formatCoord(z.latitude)}, ${formatCoord(z.longitude)}", formatRadius(z.radius)),
                Modifier.clickable { editing = z },
            )
            RowDivider()
        }
        TextButton(modifier = Modifier.padding(8.dp).tvFocusRing(), onClick = { creating = true }) {
            Icon(Icons.Rounded.Add, null)
            Spacer(Modifier.size(8.dp))
            Text(stringResource(R.string.zone_add))
        }
    }
    if (zones.isNotEmpty() || store.automations.any { it.trigger.kind.usesZone }) {
        ZonePermissionCard(Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
    }
    if (creating || editing != null) {
        ZoneEditorDialog(
            initial = editing,
            onSaved = { creating = false; editing = null },
            onDismiss = { creating = false; editing = null },
        )
    }
}

/**
 * Arka planda konum izni: önce açıklama (Play kuralı: izin istenmeden önce neden gerektiği gösterilir), sonra
 * hassas konum, ardından ayrı adımda "Her zaman izin ver". İzin tamamsa kısa bir durum satırı.
 */
@Composable
fun ZonePermissionCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val resumed = lifecycle.isAtLeast(Lifecycle.State.RESUMED)
    var refresh by remember { mutableIntStateOf(0) }
    val ok = remember(resumed, refresh) { ZoneWatcher.hasPermission(context) }
    val needsBackground = remember(resumed, refresh) { ZoneWatcher.needsBackground(context) }
    LaunchedEffect(ok) { if (ok) ZoneWatcher.sync(context) }
    val background = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refresh++ }
    val fine = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { refresh++ }
    val scheme = MaterialTheme.colorScheme
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = if (ok) scheme.secondaryContainer else scheme.tertiaryContainer) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (ok) {
                Text(stringResource(R.string.zone_perm_ok), style = MaterialTheme.typography.bodyMedium, color = scheme.onSecondaryContainer)
                return@Column
            }
            Text(stringResource(R.string.zone_perm_title), style = MaterialTheme.typography.titleSmall, color = scheme.onTertiaryContainer)
            Text(stringResource(R.string.zone_perm_desc), style = MaterialTheme.typography.bodyMedium, color = scheme.onTertiaryContainer)
            if (needsBackground) {
                Text(stringResource(R.string.zone_perm_background), style = MaterialTheme.typography.bodyMedium, color = scheme.onTertiaryContainer)
            }
            Button(modifier = Modifier.tvFocusRing(), onClick = {
                if (needsBackground && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    background.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                } else {
                    fine.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                }
            }) { Text(stringResource(R.string.zone_perm_grant)) }
        }
    }
}

// ---------------------------------------------------------------- bölge düzenleyici

/** Bölge ekleme/düzenleme: tam ekran harita, ortadaki iğne bölgenin merkezi; ad, yarıçap ve yer arama. */
@Composable
fun ZoneEditorDialog(initial: Zone?, onSaved: (Zone) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val store = remember { Store.get(context) }
    val scope = rememberCoroutineScope()
    val map = remember {
        val lat = initial?.latitude ?: store.latitude
        val lon = initial?.longitude ?: store.longitude
        // Konum bilinmiyorsa Türkiye'nin tamamı görünür.
        if (lat != null && lon != null) MapState(lat, lon, if (initial != null) 16.0 else 15.0) else MapState(39.0, 35.0, 5.0)
    }
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var radiusIndex by remember {
        mutableIntStateOf(Zone.RADII.indexOf(initial?.radius ?: Zone.DEFAULT_RADIUS).coerceAtLeast(0))
    }
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<WeatherClient.Place>?>(null) }
    var locating by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    val noLocation = stringResource(R.string.location_unavailable)
    val usedBy = remember(initial) { initial?.let { z -> store.automations.count { it.trigger.deviceId == z.id } } ?: 0 }

    fun locate() = scope.launch {
        locating = true
        val l = currentLocation(context, maxAgeMs = 2 * 60_000L)
        locating = false
        if (l == null) error = noLocation
        else {
            error = null
            map.moveTo(l.latitude, l.longitude, 17.0)
        }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { r ->
        if (r.values.any { it }) locate() else error = noLocation
    }
    fun search() {
        val q = query.trim()
        if (q.length < 2) return
        scope.launch {
            results = runCatching {
                withContext(Dispatchers.IO) { WeatherClient.search(q, Locale.getDefault().language) }
            }.getOrElse { emptyList() }
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().imePadding()) {
                AppTopBar(
                    stringResource(if (initial == null) R.string.zone_add else R.string.zone_edit),
                    small = true,
                    navigationIcon = {
                        IconButton(modifier = Modifier.tvFocusRing(), onClick = onDismiss) {
                            Icon(Icons.Rounded.Close, stringResource(R.string.action_cancel))
                        }
                    },
                    actions = {
                        if (initial != null) IconButton(modifier = Modifier.tvFocusRing(), onClick = { confirmDelete = true }) {
                            Icon(Icons.Rounded.Delete, stringResource(R.string.action_delete))
                        }
                    },
                )
                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        name, { name = it.take(40) },
                        label = { Text(stringResource(R.string.zone_name)) },
                        placeholder = { Text(stringResource(R.string.zone_name_hint)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.Sentences),
                        modifier = Modifier.fillMaxWidth().tvTextField(),
                    )
                    OutlinedTextField(
                        query, { query = it; if (it.isBlank()) results = null },
                        label = { Text(stringResource(R.string.zone_search)) },
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = ::search) { Icon(Icons.Rounded.Search, stringResource(R.string.zone_search)) }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { search() }),
                        modifier = Modifier.fillMaxWidth().tvTextField(),
                    )
                }
                Box(Modifier.weight(1f).fillMaxWidth().padding(16.dp)) {
                    TileMap(map, Zone.RADII[radiusIndex], Modifier.fillMaxSize())
                    Column(Modifier.align(Alignment.TopEnd).padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        MapButton(Icons.Rounded.Add, stringResource(R.string.zone_zoom_in)) { map.zoomBy(1.0) }
                        MapButton(Icons.Rounded.Remove, stringResource(R.string.zone_zoom_out)) { map.zoomBy(-1.0) }
                        MapButton(Icons.Rounded.MyLocation, stringResource(R.string.zone_here), enabled = !locating) {
                            val granted = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                                context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                            if (granted) locate()
                            else permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                        }
                    }
                    Text(
                        stringResource(R.string.map_attribution),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.align(Alignment.BottomEnd)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f), RoundedCornerShape(topStart = 8.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                    // Arama sonuçları haritanın üstünde açılır; birine dokununca harita oraya gider.
                    results?.let { list ->
                        Surface(
                            Modifier.align(Alignment.TopStart).fillMaxWidth(0.85f).padding(8.dp),
                            shape = RoundedCornerShape(16.dp), tonalElevation = 6.dp, shadowElevation = 6.dp,
                        ) {
                            Column(Modifier.padding(vertical = 4.dp)) {
                                if (list.isEmpty()) {
                                    Text(stringResource(R.string.zone_search_none), Modifier.padding(16.dp))
                                }
                                list.forEach { p ->
                                    Text(
                                        p.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.fillMaxWidth().tvFocusRing(RoundedCornerShape(8.dp), inside = true)
                                            .clickable { map.moveTo(p.latitude, p.longitude, 14.0); results = null }
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                    )
                                }
                            }
                        }
                    }
                }
                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        stringResource(R.string.zone_map_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
                    Text(
                        stringResource(R.string.zone_radius) + ": " + formatRadius(Zone.RADII[radiusIndex]),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Slider(
                        value = radiusIndex.toFloat(),
                        onValueChange = { radiusIndex = it.roundToInt().coerceIn(0, Zone.RADII.lastIndex) },
                        valueRange = 0f..Zone.RADII.lastIndex.toFloat(),
                        steps = Zone.RADII.size - 2,
                        modifier = Modifier.tvFocusRing(RoundedCornerShape(12.dp)),
                    )
                    Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                        TextButton(modifier = Modifier.tvFocusRing(), onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
                        Button(
                            modifier = Modifier.tvFocusRing(),
                            enabled = name.isNotBlank(),
                            onClick = {
                                val zone = Zone(
                                    initial?.id ?: (Zone.PREFIX + UUID.randomUUID()),
                                    name.trim(), map.latitude, map.longitude, Zone.RADII[radiusIndex],
                                )
                                val list = store.zones
                                store.zones = if (list.any { it.id == zone.id }) list.map { if (it.id == zone.id) zone else it } else list + zone
                                onSaved(zone)
                            },
                        ) { Text(stringResource(R.string.action_save)) }
                    }
                }
            }
        }
    }

    if (confirmDelete && initial != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            text = {
                Text(if (usedBy > 0) stringResource(R.string.zone_in_use, usedBy) else stringResource(R.string.zone_delete_confirm, initial.name))
            },
            confirmButton = {
                if (usedBy == 0) TextButton(modifier = Modifier.tvFocusRing(), onClick = {
                    store.zones = store.zones.filterNot { it.id == initial.id }
                    confirmDelete = false
                    onDismiss()
                }) { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(modifier = Modifier.tvFocusRing(), onClick = { confirmDelete = false }) {
                    Text(stringResource(if (usedBy > 0) android.R.string.ok else R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun MapButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Surface(shape = CircleShape, tonalElevation = 4.dp, shadowElevation = 4.dp) {
        IconButton(modifier = Modifier.tvFocusRing(), enabled = enabled, onClick = onClick) { Icon(icon, label) }
    }
}

/** "200 m", "1 km", "1,5 km". */
fun formatRadius(meters: Int): String =
    if (meters < 1000) "$meters m"
    else String.format(Locale.getDefault(), if (meters % 1000 == 0) "%.0f km" else "%.1f km", meters / 1000.0)

// ---------------------------------------------------------------- harita

/** Haritanın merkezi ve yakınlığı (OSM yakınlık düzeyi, kesirli: iki parmakla yumuşak yakınlaşma). */
class MapState(latitude: Double, longitude: Double, zoom: Double) {
    var latitude by mutableDoubleStateOf(latitude)
        private set
    var longitude by mutableDoubleStateOf(longitude)
        private set
    var zoom by mutableDoubleStateOf(zoom.coerceIn(MIN_ZOOM, MAX_ZOOM))
        private set

    fun moveTo(lat: Double, lon: Double, zoom: Double = this.zoom) {
        latitude = lat.coerceIn(-MAX_LAT, MAX_LAT)
        longitude = ((lon + 540) % 360) - 180
        this.zoom = zoom.coerceIn(MIN_ZOOM, MAX_ZOOM)
    }

    fun zoomBy(delta: Double) { zoom = (zoom + delta).coerceIn(MIN_ZOOM, MAX_ZOOM) }

    /** Ekranda (dx, dy) piksel kaydırma; [tilePx] bir karonun ekrandaki boyu (yakınlık tam sayıyken). */
    fun panBy(dx: Float, dy: Float, tilePx: Float) {
        val scale = tilePx * 2.0.pow(zoom)
        val x = lonToX(longitude) * scale - dx
        val y = latToY(latitude) * scale - dy
        moveTo(yToLat(y / scale), xToLon(x / scale))
    }

    companion object {
        const val MIN_ZOOM = 3.0
        const val MAX_ZOOM = 19.0
        const val MAX_LAT = 85.0
        /** Boylam → [0,1) dünya koordinatı (Web Mercator). */
        fun lonToX(lon: Double) = (lon + 180) / 360
        fun latToY(lat: Double): Double {
            val r = Math.toRadians(lat)
            return (1 - ln(tan(r) + 1 / cos(r)) / PI) / 2
        }
        fun xToLon(x: Double) = x * 360 - 180
        fun yToLat(y: Double) = Math.toDegrees(atan(sinh(PI * (1 - 2 * y))))
    }
}

/** Karoların önbelleği: dialog kapanıp açılınca yeniden indirilmesin (en çok ~200 karo). */
private val tileCache = object : LinkedHashMap<String, ImageBitmap>(64, 0.75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ImageBitmap>?) = size > 200
}
private val tileLimit = Semaphore(4)

/**
 * OpenStreetMap karolarıyla basit harita: sürükleyerek kaydırma, iki parmakla yakınlaştırma, kumandada yön tuşları.
 * Ortada iğne ve [radius] metrelik daire: bölgenin merkezi ve büyüklüğü.
 */
@Composable
fun TileMap(state: MapState, radius: Int, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val tilePx = with(density) { 192.dp.toPx() }
    var size by remember { mutableStateOf(IntSize.Zero) }
    val loaded = remember { mutableStateMapOf<String, ImageBitmap>() }
    // İndirmeler haritayı kaydırınca iptal edilmesin diye efektin değil haritanın kapsamında.
    val scope = rememberCoroutineScope()
    val inFlight = remember { mutableSetOf<String>() }
    val scheme = MaterialTheme.colorScheme

    val z = floor(state.zoom).toInt()
    val drawn = tilePx * 2.0.pow(state.zoom - z).toFloat()
    val n = 1 shl z
    val cx = MapState.lonToX(state.longitude) * n
    val cy = MapState.latToY(state.latitude) * n
    // Ekrana giren karolar.
    val needed = remember(z, cx, cy, size, drawn) {
        if (size == IntSize.Zero) emptyList() else {
            val halfW = size.width / 2f / drawn
            val halfH = size.height / 2f / drawn
            val xs = floor(cx - halfW).toInt()..floor(cx + halfW).toInt()
            val ys = (floor(cy - halfH).toInt()..floor(cy + halfH).toInt()).filter { it in 0 until n }
            xs.flatMap { x -> ys.map { y -> Triple(z, x, y) } }
        }
    }
    LaunchedEffect(needed) {
        for ((tz, tx, ty) in needed) {
            val wx = ((tx % n) + n) % n
            val key = "$tz/$wx/$ty"
            if (key in loaded || key in inFlight) continue
            synchronized(tileCache) { tileCache[key] }?.let { loaded[key] = it; continue }
            inFlight += key
            scope.launch {
                val bmp = withContext(Dispatchers.IO) { tileLimit.withPermit { runCatching { fetchTile(key) }.getOrNull() } }
                inFlight -= key
                if (bmp == null) return@launch
                synchronized(tileCache) { tileCache[key] = bmp }
                loaded[key] = bmp
            }
        }
    }

    Box(
        modifier
            .background(scheme.surfaceVariant, RoundedCornerShape(20.dp))
            .onSizeChanged { size = it }
            .pointerInput(tilePx) {
                detectTransformGestures { _, pan, zoomChange, _ ->
                    state.panBy(pan.x, pan.y, tilePx)
                    if (zoomChange != 1f) state.zoomBy(ln(zoomChange.toDouble()) / ln(2.0))
                }
            }
            .onKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown) return@onKeyEvent false
                val step = tilePx / 2
                when (e.key) {
                    Key.DirectionLeft -> state.panBy(step, 0f, tilePx)
                    Key.DirectionRight -> state.panBy(-step, 0f, tilePx)
                    Key.DirectionUp -> state.panBy(0f, step, tilePx)
                    Key.DirectionDown -> state.panBy(0f, -step, tilePx)
                    else -> return@onKeyEvent false
                }
                true
            }
            .tvFocusRing(RoundedCornerShape(20.dp))
            .focusable(),
    ) {
        Canvas(Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp))) {
            val w = this.size.width
            val h = this.size.height
            for ((tz, tx, ty) in needed) {
                val wx = ((tx % n) + n) % n
                val img = loaded["$tz/$wx/$ty"] ?: continue
                val left = (w / 2f + (tx - cx) * drawn).toFloat()
                val top = (h / 2f + (ty - cy) * drawn).toFloat()
                drawImage(
                    img,
                    dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
                    dstSize = androidx.compose.ui.unit.IntSize(drawn.roundToInt() + 1, drawn.roundToInt() + 1),
                )
            }
            // Daire: yarıçap metreden piksele (Web Mercator ölçeği enleme göre değişir).
            val metersPerPx = 156_543.03392 * cos(Math.toRadians(state.latitude)) / 2.0.pow(state.zoom) * 256 / tilePx
            val r = (radius / metersPerPx).toFloat()
            val center = Offset(w / 2f, h / 2f)
            drawCircle(scheme.primary.copy(alpha = 0.18f), r, center)
            drawCircle(scheme.primary, r, center, style = Stroke(width = 2.dp.toPx()))
            drawCircle(scheme.surface, 7.dp.toPx(), center)
            drawCircle(scheme.primary, 5.dp.toPx(), center)
        }
    }
}

private fun fetchTile(key: String): ImageBitmap? {
    // OSM karo kullanım kuralı: uygulamayı tanıtan User-Agent zorunlu.
    val request = Request.Builder()
        .url("https://tile.openstreetmap.org/$key.png")
        .header("User-Agent", "SwitchGuard/${BuildConfig.VERSION_NAME} (Android; com.macerce.switchguard)")
        .build()
    EwelinkClient.http.newCall(request).execute().use { r ->
        if (!r.isSuccessful) return null
        val bytes = r.body?.bytes() ?: return null
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
    }
}
