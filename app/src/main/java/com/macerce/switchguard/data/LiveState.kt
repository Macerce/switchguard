package com.macerce.switchguard.data

import com.macerce.switchguard.core.ExpectedChanges
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class ConnState {
    /** İzleme kapalı. */
    STOPPED,
    CONNECTING,
    /** WebSocket açık, değişiklikler anında geliyor. */
    LIVE,
    /** WebSocket kullanılamıyor; periyodik sorgulama ile izleniyor. */
    POLLING,
    /** Telefonun interneti yok ya da eWeLink'e ulaşılamıyor. */
    NO_INTERNET,
    /** Oturum geçersiz, yeniden giriş gerekli. */
    AUTH_ERROR,
    ERROR,
}

data class Connection(
    val state: ConnState = ConnState.STOPPED,
    val detail: String = "",
    /** Son başarılı senkron (ms), yoksa 0. */
    val lastSync: Long = 0,
)

/** Servis ile arayüz arasında paylaşılan, kalıcı olmayan canlı durum (aynı süreç). */
object LiveState {
    private val _connection = MutableStateFlow(Connection())
    val connection: StateFlow<Connection> = _connection

    /** Uygulamadan yapılan aç/kapa işlemleri; alarm çaldırmamaları için. */
    val expected = ExpectedChanges()

    fun setConnection(state: ConnState, detail: String = "", synced: Boolean = false) {
        val prev = _connection.value
        _connection.value = Connection(state, detail, if (synced) System.currentTimeMillis() else prev.lastSync)
    }
}
