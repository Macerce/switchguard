package com.macerce.switchguard.core

/** Canlı durum listelerinin karşılaştırması (bkz. Store.snapshots: ne zaman hemen diske yazılmalı). */
object SnapshotDiff {
    /**
     * İki liste yalnız ölçümlerde mi farklı: güç, gerilim, akım, kanal gücü, sinyal ve sayısal sensör değerleri.
     * Cihaz listesi, ad, çevrimiçi, anahtarlar ya da ikili sensör (kapı, hareket, su) değiştiyse false.
     */
    fun onlyReadings(old: List<DeviceSnapshot>, new: List<DeviceSnapshot>): Boolean {
        if (old.size != new.size) return false
        for (i in old.indices) if (shape(old[i]) != shape(new[i])) return false
        return true
    }

    private fun shape(d: DeviceSnapshot) = d.copy(
        power = d.power?.let { 0.0 }, voltage = d.voltage?.let { 0.0 }, current = d.current?.let { 0.0 },
        channelPower = d.channelPower.mapValues { 0.0 }, rssi = null,
        sensors = d.sensors.mapValues { (k, v) -> if (k.binary) v else 0.0 },
    )
}
