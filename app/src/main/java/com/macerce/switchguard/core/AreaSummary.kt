package com.macerce.switchguard.core

/**
 * Bir alanın (oda) kısa özeti, Home Assistant'ın alan kartları gibi: kaç anahtar açık, anlık toplam güç,
 * ortalama sıcaklık/nem, açık kapı ve hareket algılayan sensör sayısı, çevrimdışı cihaz sayısı.
 */
data class AreaSummary(
    val devices: Int,
    val switchesOn: Int,
    val switchesTotal: Int,
    val watts: Double?,
    val temperature: Double?,
    val humidity: Double?,
    val doorsOpen: Int,
    val motion: Int,
    val water: Int,
    val offline: Int,
) {
    val anyOn: Boolean get() = switchesOn > 0

    companion object {
        /** [areaId] alanındaki gerçek cihazlardan (grup sanal cihazları sayılmaz). */
        fun of(areaId: String, layout: DeviceLayout, devices: List<DeviceSnapshot>): AreaSummary =
            of(devices.filter { !DeviceGroup.isGroup(it.id) && layout.categoryOf[it.id] == areaId })

        fun of(inArea: List<DeviceSnapshot>): AreaSummary {
            val online = inArea.filter { it.online }
            fun avg(kind: SensorKind) = online.mapNotNull { it.sensors[kind] }.takeIf { it.isNotEmpty() }?.average()
            fun active(kind: SensorKind) = online.count { it.sensorActive(kind) == true }
            return AreaSummary(
                devices = inArea.size,
                switchesOn = online.sumOf { d -> d.switches.values.count { it } },
                switchesTotal = inArea.sumOf { it.switches.size },
                watts = online.filter { it.hasEnergy }.takeIf { it.isNotEmpty() }?.sumOf { it.power ?: 0.0 },
                temperature = avg(SensorKind.TEMPERATURE),
                humidity = avg(SensorKind.HUMIDITY),
                doorsOpen = active(SensorKind.DOOR),
                motion = active(SensorKind.MOTION),
                water = active(SensorKind.WATER),
                offline = inArea.count { !it.online },
            )
        }

        /** Alan adına göre simge türü (anahtar sözcük, TR/EN); bulunamazsa genel oda. */
        fun iconKind(name: String): AreaIcon {
            val n = name.lowercase()
            fun has(vararg w: String) = w.any { it in n }
            return when {
                has("mutfak", "kitchen") -> AreaIcon.KITCHEN
                has("yatak", "bed") -> AreaIcon.BED
                has("banyo", "tuvalet", "wc", "bath") -> AreaIcon.BATH
                has("bahçe", "bahce", "teras", "balkon", "garden", "yard", "terrace", "balcony", "havuz", "pool") -> AreaIcon.GARDEN
                has("garaj", "otopark", "garage", "parking") -> AreaIcon.GARAGE
                has("ofis", "çalışma", "calisma", "büro", "office", "study") -> AreaIcon.OFFICE
                has("salon", "oturma", "living", "lounge") -> AreaIcon.LIVING
                has("çocuk", "cocuk", "kid", "child") -> AreaIcon.KIDS
                has("depo", "kiler", "storage", "rögar", "rogar", "pompa", "kazan", "boiler") -> AreaIcon.UTILITY
                has("resepsiyon", "lobi", "lobby", "reception", "giriş", "giris", "entrance", "hall", "koridor") -> AreaIcon.ENTRANCE
                else -> AreaIcon.ROOM
            }
        }
    }
}

enum class AreaIcon { ROOM, KITCHEN, BED, BATH, GARDEN, GARAGE, OFFICE, LIVING, KIDS, UTILITY, ENTRANCE }
