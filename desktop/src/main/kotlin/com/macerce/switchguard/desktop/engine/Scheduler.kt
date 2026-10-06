package com.macerce.switchguard.desktop.engine

import com.macerce.switchguard.desktop.Log
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/** Motorun saatleri; testlerde sahtesi verilir. */
interface Clock {
    /** Duvar saati (Unix ms): olay zamanları, sessiz saatler, otomasyon süreleri, uyku algılama. */
    fun wall(): Long

    /** Monoton geçen süre (ms; telefondaki `SystemClock.elapsedRealtime()`): senkron ve yeniden bağlanma aralıkları. */
    fun elapsed(): Long
}

object SystemClock : Clock {
    override fun wall() = System.currentTimeMillis()

    /** HotSpot'ta (Windows QPC, Linux CLOCK_MONOTONIC) açılıştan beri sayan pozitif değer. */
    override fun elapsed() = System.nanoTime() / 1_000_000
}

/**
 * Tek iş parçacıklı iş kuyruğu (telefondaki `Handler` + `HandlerThread`): işler sırayla ve tek tek çalışır;
 * bu yüzden motorun durumu kilitsiz tutulabilir.
 */
interface Scheduler {
    fun post(task: () -> Unit)

    fun postDelayed(delayMs: Long, task: () -> Unit)

    /** Kuyruktaki tüm işleri atar (`Handler.removeCallbacksAndMessages(null)`); o an çalışan iş tamamlanır. */
    fun cancelAll()

    /** Kuyruğu kapatır (`Looper.quitSafely`): zamanı gelmiş işler çalışır, ileri tarihliler atılır; sonraki işler yok sayılır. */
    fun shutdown()
}

/** [Scheduler]'ın gerçeği: tek iş parçacıklı `ScheduledThreadPoolExecutor`, daemon iş parçacığı [name]. */
class ExecutorScheduler(name: String) : Scheduler {
    @Volatile private var thread: Thread? = null

    private val executor = ScheduledThreadPoolExecutor(1) { r ->
        Thread(r, name).apply { isDaemon = true }.also { thread = it }
    }.apply {
        // quitSafely: kapatılınca zamanı gelmiş işler çalışır, ileri tarihliler (tick vb.) atılır.
        executeExistingDelayedTasksAfterShutdownPolicy = false
    }

    /** [cancelAll] her çağrıldığında artar; daha önce kuyruğa girmiş işler sırası gelince çalışmadan geçilir. */
    private val generation = AtomicLong()

    override fun post(task: () -> Unit) = postDelayed(0, task)

    override fun postDelayed(delayMs: Long, task: () -> Unit) {
        val queuedIn = generation.get()
        val runnable = Runnable {
            if (queuedIn != generation.get()) return@Runnable
            // Yakalanmayan hata sessizce kaybolmasın (telefonda süreç çöker ve servis yeniden başlar).
            try {
                task()
            } catch (t: Throwable) {
                Log.w(TAG, "monitor task failed", t)
            }
        }
        try {
            executor.schedule(runnable, delayMs.coerceAtLeast(0), TimeUnit.MILLISECONDS)
        } catch (_: RejectedExecutionException) {
            // Kapatıldıktan sonra gelen işler atılır (ölü iş parçacığındaki Handler gibi).
        }
    }

    override fun cancelAll() {
        generation.incrementAndGet()
    }

    /** Başka bir iş parçacığından çağrılırsa son işlerin (bağlantıları kapatma vb.) bitmesini en fazla 2 sn bekler. */
    override fun shutdown() {
        executor.shutdown()
        if (Thread.currentThread() === thread) return
        try {
            executor.awaitTermination(2, TimeUnit.SECONDS)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }

    private companion object {
        const val TAG = "SwitchGuard"
    }
}
