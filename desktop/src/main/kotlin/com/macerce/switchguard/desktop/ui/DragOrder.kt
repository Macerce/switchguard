package com.macerce.switchguard.desktop.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Cihazlar ekranında sürükle-bırak sırası (`spec-2-ayarlar.md` §7). Sürükleme yokken ekran sırayı doğrudan ayarlardan
 * alır ([displayed]); sürüklerken yalnızca bu bellekteki liste değişir ([move]) ve bırakınca yeni sıra **bir kez**
 * döner ([end]) — ayar dosyası her adımda yazılmaz. Sürükleme sırasında motor listeyi değiştirirse ([show]) yerel
 * sıra korunur: bilinen cihazlar yerinde kalır, yeniler sona eklenir, gidenler çıkar (kart zıplamaz).
 * Durum Compose durumudur: değişince ekran yeniden çizilir.
 */
class DragOrder {
    var ids: List<String> by mutableStateOf(emptyList())
        private set
    var dragging: Boolean by mutableStateOf(false)
        private set
    private var startIds: List<String> = emptyList()

    /** Sürükleme başladı; [current] o an ekrandaki sıra. */
    fun begin(current: List<String>) {
        startIds = current
        ids = current
        dragging = true
    }

    /** Sürüklenen [fromId], [toId]'nin yerine geçer (öbürleri kayar). Bilinmeyen kimlikler yok sayılır. */
    fun move(fromId: String, toId: String) {
        val from = ids.indexOf(fromId)
        val to = ids.indexOf(toId)
        if (from < 0 || to < 0 || from == to) return
        ids = ids.toMutableList().apply { add(to, removeAt(from)) }
    }

    /** Ayarlardaki (motorun güncellediği) sıra; yalnızca sürüklerken yerel sırayla birleştirilir. */
    fun show(sorted: List<String>) {
        if (!dragging) return
        val incoming = sorted.toSet()
        val kept = ids.filter { it in incoming }
        ids = kept + sorted.filter { it !in kept.toSet() }
    }

    /** Sürükleme bitti: sıra değiştiyse yeni sıra (kaydedilmeli), değişmediyse ya da sürükleme yoksa null. */
    fun end(): List<String>? {
        if (!dragging) return null
        dragging = false
        return ids.takeIf { it != startIds }
    }

    /** Ekranda gösterilecek sıra. */
    fun displayed(sorted: List<String>): List<String> = if (dragging) ids else sorted
}
