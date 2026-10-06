package com.macerce.switchguard.desktop

/**
 * Yakalanmamış hatalar günlüğe yazılır. Uygulama `javaw` ile, konsolsuz çalışır: stderr'e yazılan yığın izi
 * kaybolur. [install] her iş parçacığının (arayüz/AWT iş parçacığı dahil) yakalanmamış hatasını [Log]'a yönlendirir;
 * [Log.init]'ten hemen sonra çağrılır.
 */
object CrashLog {
    private const val TAG = "Crash"

    fun install() {
        Thread.setDefaultUncaughtExceptionHandler { thread, t ->
            Log.w(TAG, "uncaught exception in thread '${thread.name}'", t)
        }
    }
}
