package com.macerce.switchguard.core

/**
 * eWeLink cihazının komut ve durum yolu. Yerel ağ (LAN) ile bulut aynı cihaza aynı anda durum yazarsa biri
 * gecikince cihaz "kapandı → açıldı" gibi görünür ve sahte alarm çalar; bu yüzden her an tek bir yetkili kaynak vardır.
 *
 * - [AUTO]: komut önce yerel ağdan, olmazsa buluttan. Durum bulut anlık bağlantısı canlıyken buluttan,
 *   kopukken (internet/sunucu yok) yerel ağdan.
 * - [LAN]: komut yalnız yerel ağdan (buluta hiç gitmez); anahtar durumu yalnız yerel ağdan. Güç/enerji
 *   ölçümleri yine buluttan gelir.
 * - [CLOUD]: yerel ağ bu cihaz için hiç kullanılmaz (eski davranış).
 */
enum class ControlRoute {
    AUTO, LAN, CLOUD;

    companion object {
        fun from(name: String?): ControlRoute? = entries.firstOrNull { it.name == name }
    }
}

/** Bulut ve yerel ağ durumlarını [ControlRoute] kurallarına göre birleştirir (saf; birim testli). */
object RouteMerge {
    /** Anahtar durumu için yerel ağ mı yetkili. [lanSeen]: cihaz şu an yerel ağda görülüyor mu. */
    fun lanAuthoritative(route: ControlRoute, lanSeen: Boolean, cloudLive: Boolean): Boolean = lanSeen && when (route) {
        ControlRoute.LAN -> true
        ControlRoute.AUTO -> !cloudLive
        ControlRoute.CLOUD -> false
    }

    /**
     * Buluttan gelen cihaz kaydına yerel ağın anahtar durumunu uygular. Yerel ağ yetkiliyse anahtarlar yerel ağdan
     * alınır ve cihaz çevrimiçi sayılır (bulut "çevrimdışı" dese de cihaz ağda yanıt veriyor); diğer alanlar
     * (güç, sensör, sinyal) buluttan kalır.
     */
    fun apply(cloud: DeviceSnapshot, lanSwitches: Map<Int, Boolean>?, route: ControlRoute, cloudLive: Boolean): DeviceSnapshot {
        if (lanSwitches.isNullOrEmpty() || !lanAuthoritative(route, true, cloudLive)) return cloud
        val switches = cloud.switches + lanSwitches.filterKeys { cloud.switches.isEmpty() || it in cloud.switches }
        return cloud.copy(switches = switches, online = true)
    }

    /** Yerel ağ duyurusu bu cihaz için işlenmeli mi. */
    fun acceptLan(route: ControlRoute, cloudLive: Boolean): Boolean = lanAuthoritative(route, true, cloudLive)

    /** Bulutun anlık mesajındaki anahtar değerleri işlenmeli mi (yerel ağ yetkiliyse yok sayılır). */
    fun acceptCloudSwitches(route: ControlRoute, lanSeen: Boolean, cloudLive: Boolean): Boolean =
        !lanAuthoritative(route, lanSeen, cloudLive)
}
