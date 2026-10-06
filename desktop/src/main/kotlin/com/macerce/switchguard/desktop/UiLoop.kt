package com.macerce.switchguard.desktop

/**
 * Arayüzün kaç kez yeniden kurulabileceği: [windowMs] içinde en çok [max] kez. Sürekli aynı hatayı veren bir arayüz
 * sonsuz döngüye girmesin; arada uzun süre sorunsuz çalıştıysa hak yenilenir.
 */
internal class UiRestarts(private val max: Int = 3, private val windowMs: Long = 10 * 60_000L) {
    private val recent = ArrayDeque<Long>()

    /** [now]'da bir yeniden başlatma yapılabilir mi; yapılabilirse sayılır. */
    fun allow(now: Long): Boolean {
        while (recent.isNotEmpty() && now - recent.first() >= windowMs) recent.removeFirst()
        if (recent.size >= max) return false
        recent.addLast(now)
        return true
    }
}

/**
 * [ui]'yi (Compose `application`, `exitProcessOnExit = false`) çalıştırır. Hatayla biterse ve [restarts] izin veriyorsa
 * aynı süreçte yeniden kurar: izleme motoru arayüzden bağımsız çalıştığı için izleme ve alarm bu sırada kesilmez.
 * Arayüz normal biterse (çıkış) true, yeniden başlatma hakkı bitince false döner. Her hata günlüğe yazılır.
 */
internal fun runUi(restarts: UiRestarts, clock: () -> Long = System::currentTimeMillis, ui: () -> Unit): Boolean {
    while (true) {
        try {
            ui()
            return true
        } catch (t: Throwable) {
            if (!restarts.allow(clock())) {
                Log.w(TAG, "the user interface keeps failing; giving up", t)
                return false
            }
            Log.w(TAG, "the user interface stopped with an error; starting it again (monitoring continues)", t)
        }
    }
}

private const val TAG = "Main"
