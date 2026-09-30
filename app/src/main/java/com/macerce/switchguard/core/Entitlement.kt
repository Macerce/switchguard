package com.macerce.switchguard.core

/**
 * Ücretsiz sürüm sınırı: Pro değilse yalnızca [FREE_DEVICES] cihaz izlenir.
 * Hangi cihazın izleneceğini kullanıcı seçer ([freeId]); seçilmemişse listedeki ilk izlenmek istenen cihaz.
 */
object Entitlement {
    const val FREE_DEVICES = 1

    /** Ücretsiz sürümde izlenen cihaz; hiçbiri izlenmek istenmiyorsa null. */
    fun freeSlot(orderedIds: List<String>, wantsMonitoring: (String) -> Boolean, freeId: String?): String? =
        freeId?.takeIf { it in orderedIds && wantsMonitoring(it) } ?: orderedIds.firstOrNull(wantsMonitoring)

    /** Gerçekten izlenecek cihazlar: kuralı açık olanlar, ücretsiz sürümde en fazla [FREE_DEVICES] tanesi. */
    fun monitored(orderedIds: List<String>, wantsMonitoring: (String) -> Boolean, pro: Boolean, freeId: String?): Set<String> =
        if (pro) orderedIds.filter(wantsMonitoring).toSet()
        else setOfNotNull(freeSlot(orderedIds, wantsMonitoring, freeId))
}
