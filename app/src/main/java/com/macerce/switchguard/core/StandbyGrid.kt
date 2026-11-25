package com.macerce.switchguard.core

/**
 * Bekleme ekranı widget ızgarası (telefon ana ekranı gibi). Her öğe bir widget: saat, hava, durum, güç, son hareketler,
 * yakında, kurallar ve tek tek her cihaz kanalı ile her sensör. Boyut ve konum hücre cinsindendir.
 * Konum serbesttir: kullanıcının koyduğu widget orada kalır, başka bir widget taşınınca/kaldırılınca kaymaz; yalnız
 * boş bir yere bırakılabilir. Konumu olmayan (yeni) ya da dar ekranda sığmayan widget ilk boş yere konur.
 * Kullanıcının kaldırdığı öğeler ayrıca tutulur; yeni eklenen cihaz kendiliğinden ilk boş yere gelir.
 */
object StandbyGrid {
    /** [x]/[y] < 0: henüz yeri yok (ilk boş yere konur). */
    data class Item(val id: String, val w: Int, val h: Int, val x: Int = -1, val y: Int = -1)

    /** Yerleşmiş öğe: sol üst hücre ([x], [y]) ve sütun sayısına göre kırpılmış boyut. */
    data class Placed(val item: Item, val x: Int, val y: Int, val w: Int, val h: Int)

    /** Kayıtlı düzen: öğeler ve kaldırılanlar. Şu an ekranda olmayan cihazların yeri de saklanır. */
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
        id == CLOCK || id == WEATHER -> 8 to 2
        id == STATUS -> 8 to 1
        id == POWER -> 8 to 3
        id == RECENT || id == UPCOMING || id == RULES -> 8 to 4
        else -> 4 to 1
    }

    fun minSize(id: String): Pair<Int, Int> = when {
        id == CLOCK || id == WEATHER || id == STATUS -> 3 to 1
        id == POWER -> 4 to 2
        id == RECENT || id == UPCOMING || id == RULES -> 4 to 2
        else -> 2 to 1
    }

    const val MAX_H = 16

    /** Satır yüksekliği 56 dp oldu (Home Assistant bölüm ızgarası); eski kare hücreli (46 dp adım) kayıt buna çevrilir. */
    private const val V2 = "v2#"
    private fun oldRows(n: Int) = Math.round(n * 46 / 64.0).toInt()

    fun item(id: String): Item = defaultSize(id).let { (w, h) -> Item(id, w, h) }

    /** Boyutu en küçük boyutun ve [columns]/[MAX_H] sınırının içine çeker. */
    fun clampSize(id: String, w: Int, h: Int, columns: Int): Pair<Int, Int> {
        val (mw, mh) = minSize(id)
        return w.coerceIn(mw, maxOf(mw, columns)) to h.coerceIn(mh, MAX_H)
    }

    /** "clock=4x2@0,0;dev:abc:0=2x1|rules;sen:x" — konum isteğe bağlı; bozuk parçalar atlanır. */
    fun parse(text: String?): State {
        if (text.isNullOrBlank()) return State(emptyList(), emptySet())
        val old = !text.startsWith(V2)
        val (itemsPart, removedPart) = text.removePrefix(V2).split('|', limit = 2).let { it[0] to it.getOrElse(1) { "" } }
        val items = itemsPart.split(';').mapNotNull { part ->
            val eq = part.lastIndexOf('=')
            if (eq <= 0) return@mapNotNull null
            val id = part.substring(0, eq)
            val spec = part.substring(eq + 1).split('@', limit = 2)
            val (w, h0) = spec[0].split('x').mapNotNull { it.toIntOrNull() }.takeIf { it.size == 2 } ?: return@mapNotNull null
            val h = if (old) oldRows(h0).coerceIn(1, MAX_H) else h0
            if (w !in 1..60 || h0 < 1 || h !in 1..MAX_H) return@mapNotNull null
            val pos = spec.getOrNull(1)?.split(',')?.mapNotNull { it.toIntOrNull() }?.takeIf { it.size == 2 && it[0] >= 0 && it[1] >= 0 }
            Item(id, w, h, pos?.get(0) ?: -1, pos?.get(1)?.let { if (old) oldRows(it) else it } ?: -1)
        }.distinctBy { it.id }
        val removed = removedPart.split(';').filter { it.isNotBlank() }.toSet() - items.map { it.id }.toSet()
        return State(items, removed)
    }

    fun format(state: State): String = V2 +
        state.items.joinToString(";") { "${it.id}=${it.w}x${it.h}" + if (it.x >= 0 && it.y >= 0) "@${it.x},${it.y}" else "" } +
            "|" + state.removed.joinToString(";")

    /**
     * Ekranda gösterilecek öğeler: kayıtlı öğelerden şu an var olanlar, ardından henüz yerleştirilmemiş ve kaldırılmamış
     * yeni öğeler ([available] sırasıyla, varsayılan boyutta, yeri yok).
     */
    fun visible(state: State, available: List<String>): List<Item> {
        val have = available.toSet()
        val known = state.items.map { it.id }.toSet()
        return state.items.filter { it.id in have } +
            available.filter { it !in known && it !in state.removed }.map { item(it) }
    }

    /** Ekrandaki öğeleri kaydedilecek düzene çevirir: şu an görünmeyen kayıtlı öğeler sonda korunur. */
    fun commit(state: State, shown: List<Item>, removed: Set<String> = state.removed): State {
        val ids = shown.map { it.id }.toSet()
        return State(shown + state.items.filter { it.id !in ids && it.id !in removed }, removed - ids)
    }

    private class Occupancy(val cols: Int) {
        val rows = mutableListOf<BooleanArray>()
        fun free(x: Int, y: Int, w: Int, h: Int): Boolean {
            if (x < 0 || y < 0 || x + w > cols) return false
            for (r in y until y + h) {
                val row = rows.getOrNull(r) ?: continue
                for (c in x until x + w) if (row[c]) return false
            }
            return true
        }
        fun take(x: Int, y: Int, w: Int, h: Int) {
            while (rows.size < y + h) rows += BooleanArray(cols)
            for (r in y until y + h) for (c in x until x + w) rows[r][c] = true
        }
        fun firstFree(w: Int, h: Int): Pair<Int, Int> {
            var y = 0
            while (true) {
                for (x in 0..cols - w) if (free(x, y, w, h)) return x to y
                y++
            }
        }
    }

    /**
     * Yerleşim: konumu olan ve sığan öğeler kendi yerinde kalır (sırayla; çakışan sonraki öğe yer arar); konumu
     * olmayanlar ya da dar ekranda sığmayanlar sığdıkları ilk boş yere konur.
     */
    fun pack(items: List<Item>, columns: Int): List<Placed> {
        val cols = columns.coerceAtLeast(1)
        val occ = Occupancy(cols)
        val fixed = HashMap<String, Placed>()
        for (it in items) {
            val w = it.w.coerceAtMost(cols)
            if (it.x >= 0 && it.y >= 0 && occ.free(it.x, it.y, w, it.h)) {
                occ.take(it.x, it.y, w, it.h)
                fixed[it.id] = Placed(it, it.x, it.y, w, it.h)
            }
        }
        return items.map { it ->
            fixed[it.id] ?: run {
                val w = it.w.coerceAtMost(cols)
                val (x, y) = occ.firstFree(w, it.h)
                occ.take(x, y, w, it.h)
                Placed(it, x, y, w, it.h)
            }
        }
    }

    /** [id] dışındaki widget'larla çakışmadan ([x], [y], [w]×[h]) alanı boş mu? */
    fun canPlace(placed: List<Placed>, id: String, x: Int, y: Int, w: Int, h: Int, columns: Int): Boolean {
        if (x < 0 || y < 0 || x + w > columns) return false
        return placed.none { p -> p.item.id != id && x < p.x + p.w && p.x < x + w && y < p.y + p.h && p.y < y + h }
    }

    /**
     * Bir widget'ı yeni yere/boyuta koyar; diğer bütün widget'lar ekranda durdukları yerde sabitlenir (sonradan
     * kaymazlar). Alan doluysa düzen değişmez.
     */
    fun place(placed: List<Placed>, id: String, x: Int, y: Int, w: Int, h: Int, columns: Int): List<Item>? {
        if (!canPlace(placed, id, x, y, w, h, columns)) return null
        return placed.map { p ->
            if (p.item.id == id) p.item.copy(w = w, h = h, x = x, y = y) else p.item.copy(w = p.w, x = p.x, y = p.y)
        }
    }

    /**
     * Sürükleyip bırakma (uygulamadaki gibi yer değiştirme): alan boşsa oraya konur; doluysa taşınan widget yine
     * oraya konur, altında kalanlar taşınanın boşalttığı yere geçer (aynı boyuttaysa düz takas). Oraya sığmayan
     * ilk boş yere iner. Geri kalan widget'lar yerinde kalır. Ekran dışıysa null.
     */
    fun move(placed: List<Placed>, id: String, x: Int, y: Int, columns: Int): List<Item>? {
        val me = placed.firstOrNull { it.item.id == id } ?: return null
        if (x < 0 || y < 0 || x + me.w > columns) return null
        if (x == me.x && y == me.y) return null
        place(placed, id, x, y, me.w, me.h, columns)?.let { return it }
        val hit = placed.filter { p -> p.item.id != id && x < p.x + p.w && p.x < x + me.w && y < p.y + p.h && p.y < y + me.h }
        val hitIds = hit.map { it.item.id }.toSet()
        val occ = Occupancy(columns)
        val pos = HashMap<String, Pair<Int, Int>>()
        // Yerinde kalanlar ve taşınan.
        for (p in placed) if (p.item.id != id && p.item.id !in hitIds) { occ.take(p.x, p.y, p.w, p.h); pos[p.item.id] = p.x to p.y }
        occ.take(x, y, me.w, me.h)
        pos[id] = x to y
        // Altında kalanlar: taşınanın eski yerine (aynı göreli konumla), olmazsa ilk boş yere.
        for (p in hit.sortedWith(compareBy({ it.y }, { it.x }))) {
            val tx = me.x + (p.x - x).coerceAtLeast(0)
            val ty = me.y + (p.y - y).coerceAtLeast(0)
            val (nx, ny) = if (occ.free(tx, ty, p.w, p.h)) tx to ty
            else if (occ.free(me.x, me.y, p.w, p.h)) me.x to me.y
            else occ.firstFree(p.w, p.h)
            occ.take(nx, ny, p.w, p.h)
            pos[p.item.id] = nx to ny
        }
        return placed.map { p ->
            val (px, py) = pos.getValue(p.item.id)
            p.item.copy(w = p.w, x = px, y = py)
        }
    }

    /** Ekrandaki yerleri sabitler (kaldırma/ekleme sonrası diğerleri kaymasın). */
    fun freeze(placed: List<Placed>): List<Item> = placed.map { it.item.copy(w = it.w, x = it.x, y = it.y) }

    /** ([cx], [cy]) hücresini kaplayan öğe. */
    fun at(placed: List<Placed>, cx: Int, cy: Int): Placed? =
        placed.firstOrNull { cx >= it.x && cx < it.x + it.w && cy >= it.y && cy < it.y + it.h }
}
