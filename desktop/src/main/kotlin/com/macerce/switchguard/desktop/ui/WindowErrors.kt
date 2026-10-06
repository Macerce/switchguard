package com.macerce.switchguard.desktop.ui

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.WindowExceptionHandler
import androidx.compose.ui.window.WindowExceptionHandlerFactory
import com.macerce.switchguard.desktop.Log
import java.util.concurrent.atomic.AtomicInteger

private const val TAG = "WindowErrors"

/** Aynı bozuk pencere her karede yeniden hata verebilir: bu kadarı tam yazılır, sonrası yalnızca sayılır. */
private const val MAX_LOGGED = 5

/**
 * Bir pencerenin içindeki hata (çizim, yerleşim, girdi, pencerenin kendi yeniden oluşturması). Compose'un
 * varsayılanı hata penceresi gösterip pencereyi kapatır ve hatayı yeniden fırlatır; burada hata günlüğe yazılır,
 * [onError] çağrılır (pencereyi kurtarma kararı, ör. ana pencerede hatayı yerinde gösterme) ve **asla yeniden
 * fırlatılmaz**: bir ekrandaki hata alarm penceresini ve izlemeyi düşürmesin. [onError]'ın kendi hatası da yutulur.
 */
@OptIn(ExperimentalComposeUiApi::class)
internal fun windowErrorHandler(name: String, onError: (Throwable) -> Unit): WindowExceptionHandler {
    val count = AtomicInteger()
    return object : WindowExceptionHandler {
        override fun onException(throwable: Throwable) {
            val n = count.incrementAndGet()
            if (n <= MAX_LOGGED) {
                Log.w(TAG, "$name window failed (#$n)", throwable)
            } else if (n % 1000 == 0) {
                Log.w(TAG, "$name window failed $n times so far (${throwable.javaClass.name}: ${throwable.message})")
            }
            try {
                onError(throwable)
            } catch (t: Throwable) {
                Log.w(TAG, "$name window recovery failed", t)
            }
        }
    }
}

/** `LocalWindowExceptionHandlerFactory` için: o kapsamda açılan her pencere [windowErrorHandler] kullanır. */
@OptIn(ExperimentalComposeUiApi::class)
internal fun windowErrorFactory(name: String, onError: (Throwable) -> Unit): WindowExceptionHandlerFactory =
    object : WindowExceptionHandlerFactory {
        override fun exceptionHandler(window: java.awt.Window): WindowExceptionHandler = windowErrorHandler(name, onError)
    }
