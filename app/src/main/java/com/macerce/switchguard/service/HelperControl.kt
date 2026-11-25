package com.macerce.switchguard.service

import android.content.Context
import com.macerce.switchguard.core.Helper
import com.macerce.switchguard.core.Helpers
import com.macerce.switchguard.data.Store

/**
 * Yardımcıların (sanal anahtar, sayaç, zamanlayıcı) tek değiştirme yolu: uygulama, bekleme ekranı, kural ve süre
 * dolumu buradan geçer. Değişiklik kaydedilir, izleme açıksa kurallar sanal cihaz değişimi olarak görür.
 */
object HelperControl {
    private val lock = Any()

    /** [transform] yeni listeyi döndürür; değişmediyse hiçbir şey olmaz. */
    fun update(context: Context, transform: (List<Helper>) -> List<Helper>) {
        val store = Store.get(context)
        val (old, new) = synchronized(lock) {
            val old = store.helpers
            val new = transform(old)
            if (new == old) return
            store.helpers = new
            old to new
        }
        MonitorService.instance?.onHelpersChanged(old, new)
        runCatching { DeviceWidget.updateAll(context) }
    }

    /** Anahtarı/zamanlayıcıyı aç ya da kapat. */
    fun switch(context: Context, deviceId: String, on: Boolean) = update(context) { list ->
        val now = System.currentTimeMillis()
        list.map { if (it.deviceId == deviceId) Helpers.switch(it, on, now) else it }
    }

    fun count(context: Context, deviceId: String, add: Int? = null, set: Int? = null) = update(context) { list ->
        list.map { if (it.deviceId == deviceId) Helpers.count(it, add, set) else it }
    }

    fun save(context: Context, h: Helper) = update(context) { list ->
        if (list.any { it.id == h.id }) list.map { if (it.id == h.id) h else it } else list + h
    }

    fun delete(context: Context, id: String) = update(context) { list -> list.filterNot { it.id == id } }
}
