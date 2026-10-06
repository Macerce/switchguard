package com.macerce.switchguard.desktop

/** Tepsi menüsünün öğeleri; `Main.kt`'deki Tray bunları sırayla çizer (çıkıştan önce ayraç). */
internal enum class TrayAction { DISMISS_ALARM, OPEN, MONITORING, TEST_ALARM, EXIT }

/**
 * Kapatılmamış alarm varken ilk öğe "Alarmı kapat"tır: alarmı susturmanın alarm penceresinden bağımsız ikinci yolu
 * (pencere bozulur ve yeniden kurma hakkı biterse tek yol bu olur).
 */
internal fun trayActions(alarmPending: Boolean): List<TrayAction> = buildList {
    if (alarmPending) add(TrayAction.DISMISS_ALARM)
    addAll(listOf(TrayAction.OPEN, TrayAction.MONITORING, TrayAction.TEST_ALARM, TrayAction.EXIT))
}
