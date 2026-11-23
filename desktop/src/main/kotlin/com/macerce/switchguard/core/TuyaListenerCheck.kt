package com.macerce.switchguard.core

/**
 * Tuya mesaj servisi "Failover" aboneliğidir: aynı proje anahtarlarıyla bağlanan cihazlardan yalnızca biri mesaj alır,
 * diğerleri bağlı ama sessiz yedekte bekler (bağlantı açılır, hiçbir şey gelmez). Wi-Fi aktarımıyla anahtarlar TV'ye ya
 * da tablete kopyalanınca böyle olur; yedekteki cihaz "Canlı" görünüp değişiklikleri ancak senkronda fark ederdi.
 *
 * Bu sınıf, mesaj servisi açıkken yapılan senkronlardan cihazın sessiz yedek olup olmadığını çıkarır: senkron, soketten
 * hiç gelmemiş bir değişiklik (aç/kapa ya da bağlantı) bulursa bu bir "kaçırma"dır; art arda [threshold] kaçırma
 * yedek sayılır. Soketten gerçek bir olay gelince (bu cihaz mesajları alıyor) sayaç sıfırlanır. Değişiklik bulmayan
 * senkron bir şey söylemez, sayaca dokunmaz.
 */
class TuyaListenerCheck(private val threshold: Int = 2) {
    var misses = 0
        private set

    /** Bu cihaz sessiz yedek tüketici mi (mesajlar başka bir cihaza gidiyor). */
    val standby: Boolean get() = misses >= threshold

    /**
     * Soket senkrondan önceki bütün aralıkta açıktı ve senkron bitti. [missed]: soketten gelmemiş bir değişiklik
     * bulundu mu. Dönüş: bu senkronla yedek durumuna yeni geçildi (kullanıcıya bir kez haber vermek için).
     */
    fun onSync(missed: Boolean): Boolean {
        if (!missed) return false
        val was = standby
        misses++
        return !was && standby
    }

    /** Soketten bir olay geldi: mesajlar bu cihaza geliyor. */
    fun onEvent() {
        misses = 0
    }

    fun reset() {
        misses = 0
    }

    companion object {
        /**
         * Senkronda okunan durum ([fetched]), bilinen durumda ([known]) olmayan bir aç/kapa ya da bağlantı değişikliği
         * içeriyor mu. Güç, sensör gibi sürekli değişen değerler sayılmaz; yeni ya da silinmiş cihazlar da sayılmaz.
         */
        fun missed(known: List<DeviceSnapshot>, fetched: List<DeviceSnapshot>): Boolean {
            val old = known.associateBy { it.id }
            return fetched.any { d ->
                val o = old[d.id] ?: return@any false
                o.online != d.online || (d.online && o.switches != d.switches)
            }
        }
    }
}
