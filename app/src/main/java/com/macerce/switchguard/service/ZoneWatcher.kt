package com.macerce.switchguard.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.macerce.switchguard.data.Store

/**
 * Bölge otomasyonları: açık bir giriş/çıkış kuralı olan bölgeleri sisteme (Google Play hizmetleri) kaydeder;
 * telefon sınırı geçince [ZoneReceiver] uyanır. Konumu uygulama izlemez, sistem pil dostu biçimde izler.
 * Kayıtlar telefon yeniden başlayınca silinir: açılışta ve izleme başlarken [sync] yeniden kurar.
 */
object ZoneWatcher {
    private const val TAG = "ZoneWatcher"

    /** Kayıt için gereken izinler: hassas konum + (Android 10+) arka planda konum. */
    fun hasPermission(context: Context): Boolean {
        fun granted(p: String) = context.checkSelfPermission(p) == PackageManager.PERMISSION_GRANTED
        return granted(Manifest.permission.ACCESS_FINE_LOCATION) &&
            (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || granted(Manifest.permission.ACCESS_BACKGROUND_LOCATION))
    }

    /** Hassas konum izni var, yalnızca arka plan eksik (Android 11+'da Ayarlar'dan "Her zaman izin ver"). */
    fun needsBackground(context: Context): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
            context.checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED

    /** Açık kuralların kullandığı bölgeler. */
    fun activeZones(store: Store) = store.zones.filter { z ->
        store.automations.any { it.enabled && it.trigger.kind.usesZone && it.trigger.deviceId == z.id }
    }

    /** Son kaydedilen bölge kümesinin imzası (izin dahil); değişmediyse yeniden kayıt yapılmaz. */
    @Volatile private var lastSignature: String? = null

    private fun signature(context: Context): String =
        activeZones(Store.get(context)).joinToString(";") { "${it.id},${it.latitude},${it.longitude},${it.radius}" } + "|" + hasPermission(context)

    /** Ayarlar değişince: yalnızca bölgeler ya da izin değiştiyse [sync]. */
    fun syncIfChanged(context: Context) {
        val sig = signature(context)
        if (sig == lastSignature) return
        sync(context)
    }

    /** Kayıtları güncel bölgelere eşitler; izin yoksa ya da bölge kuralı yoksa hepsini kaldırır. */
    @SuppressLint("MissingPermission")
    fun sync(context: Context) {
        val app = context.applicationContext
        val client = runCatching { LocationServices.getGeofencingClient(app) }.getOrNull() ?: return
        val pending = pendingIntent(app)
        val zones = activeZones(Store.get(app))
        lastSignature = signature(app)
        client.removeGeofences(pending).addOnCompleteListener {
            if (zones.isEmpty() || !hasPermission(app)) return@addOnCompleteListener
            val fences = zones.map { z ->
                Geofence.Builder()
                    .setRequestId(z.id)
                    .setCircularRegion(z.latitude, z.longitude, z.radius.toFloat())
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
                    // Bildirim gecikmesi: pil için sistem toplu iletebilir (en çok 2 dk).
                    .setNotificationResponsiveness(120_000)
                    .build()
            }
            // Başlangıç tetiklemesi yok: kural kurulurken "zaten içindesin" diye cihazlar oynamasın.
            val request = GeofencingRequest.Builder().setInitialTrigger(0).addGeofences(fences).build()
            runCatching { client.addGeofences(request, pending) }
                .onSuccess { task -> task.addOnFailureListener { Log.w(TAG, "addGeofences failed: ${it.message}") } }
                .onFailure { Log.w(TAG, "addGeofences threw", it) }
        }
    }

    private fun pendingIntent(context: Context): PendingIntent {
        // Play hizmetleri olay ayrıntılarını niyetin içine yazar: değiştirilebilir olmalı (Android 12+).
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0)
        return PendingIntent.getBroadcast(context, 0, Intent(context, ZoneReceiver::class.java), flags)
    }
}

/** Bölgeye giriş/çıkış: kuralları izleme servisi çalıştırır (izleme kapalıysa hiçbir otomasyon çalışmaz). */
class ZoneReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) {
            Log.w("ZoneReceiver", "geofence error ${event.errorCode}")
            return
        }
        val entered = when (event.geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> true
            Geofence.GEOFENCE_TRANSITION_EXIT -> false
            else -> return
        }
        event.triggeringGeofences.orEmpty().forEach { MonitorService.zone(context, it.requestId, entered) }
    }
}
