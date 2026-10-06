package com.macerce.switchguard.desktop.platform

import androidx.compose.ui.window.Notification
import com.macerce.switchguard.desktop.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay

/** Sıraya alınmış tepsi bildirimi; [onDelivered] bildirim tepsiye teslim edilince bir kez çağrılır. */
class QueuedNotification(val notification: Notification, val onDelivered: (() -> Unit)? = null)

/**
 * Tepsi bildirimlerinin sınırsız kuyruğu. Compose'un `TrayState.sendNotification`'ı tamponsuz bir kanala `trySend`
 * yapar: bildirim ancak `Tray` toplayıcısı o an alıcıda bekliyorsa iletilir, aksi halde sessizce düşer. Motor tek
 * bir eşitlemede birçok değişiklik için art arda bildirim verdiğinden ve `application {}` başlamadan önce de
 * bildirim üretebildiğinden doğrudan çağrı bildirim kaybettirir. Üreticiler (motor iş parçacığı, arayüz) yalnızca
 * [enqueue] eder; arayüzde çalışan tek bir [pump] bunları aralıklı olarak tepsiye iletir.
 */
class NotificationQueue {
    private val channel = Channel<QueuedNotification>(Channel.UNLIMITED)

    /** Her iş parçacığından çağrılabilir, hiç bloklamaz. */
    fun enqueue(notification: Notification, onDelivered: (() -> Unit)? = null) {
        channel.trySend(QueuedNotification(notification, onDelivered))
    }

    /**
     * Kuyruktakileri sırayla [deliver]'a verir; iptal edilene dek sürer. İlk bildirimden önce [initialDelayMs]
     * (Tray toplayıcısı hazır olsun), her bildirimden sonra [spacingMs] beklenir (toplayıcı bir sonrakini almadan
     * tekrar alıcıda olsun). [deliver] teslim edildiyse true döner, o zaman [QueuedNotification.onDelivered]
     * çağrılır; false (ör. tepsi yok) ya da istisna bildirimi düşürür, pompa yine de sürer.
     */
    suspend fun pump(initialDelayMs: Long, spacingMs: Long, deliver: (Notification) -> Boolean) {
        delay(initialDelayMs)
        for (item in channel) {
            val delivered = try {
                deliver(item.notification)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "notification delivery failed", e)
                false
            }
            if (delivered) {
                try {
                    item.onDelivered?.invoke()
                } catch (e: Exception) {
                    Log.w(TAG, "notification callback failed", e)
                }
            }
            delay(spacingMs)
        }
    }

    companion object {
        private const val TAG = "Notifications"

        /** Tepsi bildirimi için varsayılan başlangıç gecikmesi ve aralık (ms). */
        const val INITIAL_DELAY_MS = 500L
        const val SPACING_MS = 250L
    }
}
