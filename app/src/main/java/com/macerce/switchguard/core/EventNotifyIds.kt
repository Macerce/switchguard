package com.macerce.switchguard.core

/**
 * Olay bildirimlerinin kimlikleri ve sayı sınırı (saf mantık).
 *
 * Android uygulama başına etkin bildirim sayısını sınırlar (cihaza göre 25–50) ve sınırı aşan **yeni** bildirimleri
 * sessizce reddeder. Her olay ayrı bildirim olursa gidip gelen bir cihaz sınırı doldurur ve sonra çalan alarmın
 * bildirimi (kilit ekranındaki tam ekran alarm dahil) gösterilmez. Bu yüzden:
 * - Aynı cihazın aynı olayı ([key], ör. "cihaz/WENT_OFFLINE") hep aynı kimliği kullanır: yenisi eskisinin yerine geçer.
 * - Anahtarsız olaylar dönen bir aralıktan kimlik alır.
 * - Göndermeden önce olay bildirimleri [MAX_ACTIVE] veya daha fazlaysa en eskiler kaldırılır ([toCancel]).
 * Kimlikler sabit bildirimlerin (durum, alarm, uyarılar: 1–99) aralığına hiç girmez; temizlik onlara dokunamaz.
 */
object EventNotifyIds {
    /** Aynı anda en çok bu kadar olay bildirimi kalır (Android'in en düşük sınırı 25'in altında). */
    const val MAX_ACTIVE = 20

    private const val KEYED_BASE = 100_000
    private const val KEYED_SIZE = 100_000
    private const val ROTATING_BASE = 200_000
    private const val ROTATING_SIZE = 100_000

    /** Anahtarlı olayın kararlı kimliği: aynı anahtar her zaman aynı kimlik. */
    fun keyed(key: String): Int = KEYED_BASE + Math.floorMod(key.hashCode(), KEYED_SIZE)

    /** Anahtarsız olayın kimliği: zamandan türetilir, aralığın içinde döner. */
    fun rotating(now: Long): Int = ROTATING_BASE + Math.floorMod(now, ROTATING_SIZE.toLong()).toInt()

    /** Kimlik bir olay bildirimine mi ait (yalnız bunlar temizlenebilir). */
    fun isEventId(id: Int): Boolean = id in KEYED_BASE until ROTATING_BASE + ROTATING_SIZE

    /**
     * Yeni bir olay bildirimi göndermeden önce kaldırılacak bildirimler: etkin olaylar ([active]: kimlik → gönderilme
     * zamanı) [max] veya daha fazlaysa en eskiler, yenisiyle birlikte en çok [max] kalacak şekilde. [incomingId] zaten
     * etkinse (aynı anahtarın yenisi) onun yerine geçeceği için sayılmaz. Olay olmayan kimlikler hiç seçilmez.
     */
    fun toCancel(active: List<Pair<Int, Long>>, incomingId: Int, max: Int = MAX_ACTIVE): List<Int> {
        val events = active.filter { (id, _) -> isEventId(id) && id != incomingId }
        val over = events.size - (max - 1)
        if (over <= 0) return emptyList()
        return events.sortedBy { it.second }.take(over).map { it.first }
    }
}
