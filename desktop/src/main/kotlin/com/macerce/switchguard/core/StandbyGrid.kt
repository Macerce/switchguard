package com.macerce.switchguard.core

/**
 * Bekleme ekranı widget ızgarası (telefon ana ekranı gibi). Her öğe bir widget: saat, hava, durum, güç, son hareketler,
 * yakında, kurallar ve tek tek her cihaz kanalı ile her sensör. Boyut hücre cinsinden (genişlik × yükseklik).
 * Sıra bir listedir; yerleşim sütun sayısına göre otomatik yapılır (her öğe sığdığı ilk boş yere): ekran dönünce ya da
 * pencere büyüyünce bozulmaz. Kullanıcının kaldırdığı öğeler ayrıca tutulur; yeni eklenen cihaz kendiliğinden sona gelir.
 */
object StandbyGrid {
    data class Item(val id: String, val w: Int, val h: Int)

    /** Yerleşmiş öğe: sol üst hücre ([x], [y]) ve sütun sayısına göre kırpılmış boyut. */
    data class Placed(val item: Item, val x: Int, val y: Int, val w: Int, val h: Int)

    /** Kayıtlı düzen: sıralı öğeler ve kaldırılanlar. Şu an ekranda olmayan cihazların yeri de saklanır. */
    data class State(val items: List<Item>, val removed: Set<String>)

    const val CLOCK = "clock"
    const val WEATHER = "weather"
    const val STATUS = "status"
    const val POWER = "power"
    const val RECENT = "recent"
    const val UPCOMING = "upcoming"
    const val RULES = "rules"

    fun device(id: String, channel: Int) = "dev:$id:$channel"
    fun sensor(id: String) = "sen:$id"

    /** "dev:<id>:<kanal>" → (id, kanal); cihaz kimliğinde ':' olabilir, kanal sondadır. */
    fun parseDevice(widget: String): Pair<String, Int>? {
        if (!widget.startsWith("dev:")) return null
        val rest = widget.removePrefix("dev:")
        val i = rest.lastIndexOf(':')
        if (i <= 0) return null
        return rest.substring(0, i) to (rest.substring(i + 1).toIntOrNull() ?: return null)
    }

    fun parseSensor(widget: String): String? = if (widget.startsWith("sen:")) widget.removePrefix("sen:") else null

    /**
     * Varsayılan ve en küçük boyut (hücre ~40 dp, telefon dikeyde 8 sütun). Köşe tutamağıyla en küçükten sütun
     * sayısına kadar serbestçe büyütülür.
     */
    fun defaultSize(id: String): Pair<Int, Int> = when {
        id == CLOCK -> 8 to 4
        id == WEATHER || id == STATUS -> 8 to 2
        id == POWER -> 8 to 4
        id == RECENT || id == UPCOMING || id == RULES -> 8 to 5
        else -> 4 to 2
    }

    fun minSize(id: String): Pair<Int, Int> = when {
        id == CLOCK || id == WEATHER || id == STATUS -> 3 to 2
        id == POWER -> 4 to 3
        id == RECENT || id == UPCOMING || id == RULES -> 4 to 3
        else -> 2 to 2
    }

    const val MAX_H = 24

    fun item(id: String): Item = defaultSize(id).let { (w, h) -> Item(id, w, h) }

    /** Yeni boyut; en küçük boyutun ve [columns]/[MAX_H] sınırının içinde kalır. */
    fun resize(items: List<Item>, id: String, w: Int, h: Int, columns: Int): List<Item> {
        val (mw, mh) = minSize(id)
        return items.map { if (it.id == id) it.copy(w = w.coerceIn(mw, maxOf(mw, columns)), h = h.coerceIn(mh, MAX_H)) else it }
    }

    /** "clock=4x2;dev:abc:0=2x1|rules;sen:x" — bozuk parçalar atlanır. */
    fun parse(text: String?): State {
        if (text.isNullOrBlank()) return State(emptyList(), emptySet())
        val (itemsPart, removedPart) = text.split('|', limit = 2).let { it[0] to it.getOrElse(1) { "" } }
        val items = itemsPart.split(';').mapNotNull { part ->
            val eq = part.lastIndexOf('=')
            if (eq <= 0) return@mapNotNull null
            val id = part.substring(0, eq)
            val (w, h) = part.substring(eq + 1).split('x').mapNotNull { it.toIntOrNull() }.takeIf { it.size == 2 } ?: return@mapNotNull null
            if (w !in 1..60 || h !in 1..MAX_H) null else Item(id, w, h)
        }.distinctBy { it.id }
        val removed = removedPart.split(';').filter { it.isNotBlank() }.toSet() - items.map { it.id }.toSet()
        return State(items, removed)
    }

    fun format(state: State): String =
        state.items.joinToString(";") { "${it.id}=${it.w}x${it.h}" } + "|" + state.removed.joinToString(";")

    /**
     * Ekranda gösterilecek sıra: kayıtlı öğelerden şu an var olanlar, ardından henüz yerleştirilmemiş ve kaldırılmamış
     * yeni öğeler ([available] sırasıyla, varsayılan boyutta).
     */
    fun visible(state: State, available: List<String>): List<Item> {
        val have = available.toSet()
        val known = state.items.map { it.id }.toSet()
        return state.items.filter { it.id in have } +
            available.filter { it !in known && it !in state.removed }.map { item(it) }
    }

    /** Ekrandaki yeni sırayı kaydedilecek düzene çevirir: şu an görünmeyen kayıtlı öğeler sonda korunur. */
    fun commit(state: State, shown: List<Item>, removed: Set<String> = state.removed): State {
        val ids = shown.map { it.id }.toSet()
        return State(shown + state.items.filter { it.id !in ids && it.id !in removed }, removed - ids)
    }

    /** Her öğe sırasıyla sığdığı ilk boş yere (satır satır, soldan sağa) konur. */
    fun pack(items: List<Item>, columns: Int): List<Placed> {
        val cols = columns.coerceAtLeast(1)
        val taken = mutableListOf<BooleanArray>()
        fun free(x: Int, y: Int, w: Int, h: Int): Boolean {
            for (r in y until y + h) {
                val row = taken.getOrNull(r) ?: continue
                for (c in x until x + w) if (row[c]) return false
            }
            return true
        }
        val out = ArrayList<Placed>(items.size)
        for (it in items) {
            val w = it.w.coerceAtMost(cols)
            val h = it.h
            var y = 0
            var spot: Pair<Int, Int>? = null
            while (spot == null) {
                for (x in 0..cols - w) if (free(x, y, w, h)) { spot = x to y; break }
                if (spot == null) y++
            }
            val (px, py) = spot
            while (taken.size < py + h) taken += BooleanArray(cols)
            for (r in py until py + h) for (c in px until px + w) taken[r][c] = true
            out += Placed(it, px, py, w, h)
        }
        return out
    }

    /** [id]'yi listede [targetId]'nin yerine taşır (sürükle-bırak). */
    fun move(items: List<Item>, id: String, targetId: String): List<Item> {
        if (id == targetId) return items
        val me = items.firstOrNull { it.id == id } ?: return items
        val to = items.indexOfFirst { it.id == targetId }
        if (to < 0) return items
        return items.filter { it.id != id }.toMutableList().apply { add(to, me) }
    }

    /** Sürüklenen öğe boş bir yere bırakılırsa sona geçer. */
    fun moveToEnd(items: List<Item>, id: String): List<Item> {
        val me = items.firstOrNull { it.id == id } ?: return items
        return items.filter { it.id != id } + me
    }

    /** ([cx], [cy]) hücresini kaplayan öğe. */
    fun at(placed: List<Placed>, cx: Int, cy: Int): Placed? =
        placed.firstOrNull { cx >= it.x && cx < it.x + it.w && cy >= it.y && cy < it.y + it.h }
}
