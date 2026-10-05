package com.macerce.switchguard.core

import org.json.JSONArray
import org.json.JSONObject

/** Kullanıcının oluşturduğu cihaz grubu (ör. "Rögarlar", "Aydınlatma"). */
data class Category(val id: String, val name: String)

/** Cihazlar ekranındaki bir bölüm: başlığı ve sıralı cihazları. */
data class DeviceSection(val kind: Kind, val category: Category?, val devices: List<DeviceSnapshot>) {
    enum class Kind { PRIORITY, CATEGORY, OTHER, ALL }
    /** Katlama durumunun saklandığı anahtar. */
    val key: String get() = category?.id ?: kind.name
}

/**
 * Cihazlar ekranının kullanıcı düzeni: elle sıralama, öncelikli (en üste sabit) cihazlar,
 * küçük kartlar ve kategoriler. Cihaz kimlikleriyle tutulur; listede olmayan (yeni) cihazlar
 * sona, ada göre eklenir; artık olmayan cihazların kayıtları zararsızca yok sayılır.
 */
data class DeviceLayout(
    val order: List<String> = emptyList(),
    val priority: Set<String> = emptySet(),
    val compact: Set<String> = emptySet(),
    val categories: List<Category> = emptyList(),
    val categoryOf: Map<String, String> = emptyMap(),
    /** Katlanmış bölümlerin anahtarları ([DeviceSection.key]). */
    val collapsed: Set<String> = emptySet(),
) {
    /** Tüm cihazlar, kullanıcının sırasıyla. */
    fun sorted(devices: List<DeviceSnapshot>): List<DeviceSnapshot> {
        val index = order.withIndex().associate { it.value to it.index }
        return devices.sortedWith(compareBy<DeviceSnapshot> { index[it.id] ?: Int.MAX_VALUE }.thenBy { it.name.lowercase() })
    }

    /**
     * Ekranda gösterilecek bölümler: önce öncelikliler, sonra kategoriler (kendi sırasıyla),
     * en son kategorisizler ("Diğer"). Hiç kategori ve öncelikli yoksa liste başlıksız tek bölümdür.
     * Boş bölümler gösterilmez; kategori boşsa da başlığı kalır ki cihaz atanabilsin.
     */
    fun sections(devices: List<DeviceSnapshot>): List<DeviceSection> {
        val all = sorted(devices)
        val out = mutableListOf<DeviceSection>()
        val pri = all.filter { it.id in priority }
        if (pri.isNotEmpty()) out += DeviceSection(DeviceSection.Kind.PRIORITY, null, pri)
        val rest = all.filter { it.id !in priority }
        if (categories.isEmpty()) {
            // Öncelikliler varsa geri kalanlar "Diğer" başlığıyla ayrılır.
            val kind = if (pri.isEmpty()) DeviceSection.Kind.ALL else DeviceSection.Kind.OTHER
            if (rest.isNotEmpty()) out += DeviceSection(kind, null, rest)
            return out
        }
        val known = categories.map { it.id }.toSet()
        for (c in categories) out += DeviceSection(DeviceSection.Kind.CATEGORY, c, rest.filter { categoryOf[it.id] == c.id })
        val other = rest.filter { categoryOf[it.id] !in known }
        if (other.isNotEmpty()) out += DeviceSection(DeviceSection.Kind.OTHER, null, other)
        return out
    }

    /**
     * Cihazı bulunduğu bölüm içinde bir yukarı ([delta] = -1) ya da aşağı (+1) taşır.
     * Sıra tüm cihazlar için tek listede tutulur; komşusuyla yer değiştirir.
     */
    fun move(id: String, delta: Int, devices: List<DeviceSnapshot>): DeviceLayout {
        val section = sections(devices).firstOrNull { s -> s.devices.any { it.id == id } } ?: return this
        val ids = section.devices.map { it.id }
        val i = ids.indexOf(id)
        val j = i + delta
        if (j !in ids.indices) return this
        val full = sorted(devices).map { it.id }.toMutableList()
        val a = full.indexOf(id)
        val b = full.indexOf(ids[j])
        full[a] = ids[j]
        full[b] = id
        return copy(order = full)
    }

    fun togglePriority(id: String) = copy(priority = if (id in priority) priority - id else priority + id)
    fun toggleCompact(id: String) = copy(compact = if (id in compact) compact - id else compact + id)
    fun toggleCollapsed(key: String) = copy(collapsed = if (key in collapsed) collapsed - key else collapsed + key)

    /** [categoryId] null ise cihaz kategorisiz olur. */
    fun assign(id: String, categoryId: String?) =
        copy(categoryOf = if (categoryId == null) categoryOf - id else categoryOf + (id to categoryId))

    fun addCategory(c: Category) = copy(categories = categories + c)
    fun renameCategory(id: String, name: String) = copy(categories = categories.map { if (it.id == id) it.copy(name = name) else it })

    /** Kategori silinince cihazları kategorisiz kalır. */
    fun deleteCategory(id: String) = copy(
        categories = categories.filterNot { it.id == id },
        categoryOf = categoryOf.filterValues { it != id },
        collapsed = collapsed - id,
    )

    fun moveCategory(id: String, delta: Int): DeviceLayout {
        val i = categories.indexOfFirst { it.id == id }
        val j = i + delta
        if (i < 0 || j !in categories.indices) return this
        val list = categories.toMutableList()
        list[i] = list[j].also { list[j] = list[i] }
        return copy(categories = list)
    }

    fun toJson(): String = JSONObject()
        .put("order", JSONArray(order))
        .put("priority", JSONArray(priority.toList()))
        .put("compact", JSONArray(compact.toList()))
        .put("categories", JSONArray().apply { categories.forEach { put(JSONObject().put("id", it.id).put("name", it.name)) } })
        .put("categoryOf", JSONObject(categoryOf))
        .put("collapsed", JSONArray(collapsed.toList()))
        .toString()

    companion object {
        fun fromJson(json: String?): DeviceLayout {
            if (json.isNullOrEmpty()) return DeviceLayout()
            return runCatching {
                val o = JSONObject(json)
                fun strings(key: String) = o.optJSONArray(key)?.let { a -> (0 until a.length()).map { a.getString(it) } }.orEmpty()
                val cats = o.optJSONArray("categories")?.let { a ->
                    (0 until a.length()).map { a.getJSONObject(it).let { c -> Category(c.getString("id"), c.getString("name")) } }
                }.orEmpty()
                val of = o.optJSONObject("categoryOf")?.let { m -> m.keys().asSequence().associateWith { m.getString(it) } }.orEmpty()
                DeviceLayout(strings("order"), strings("priority").toSet(), strings("compact").toSet(), cats, of, strings("collapsed").toSet())
            }.getOrDefault(DeviceLayout())
        }
    }
}
