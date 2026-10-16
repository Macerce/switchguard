package com.macerce.switchguard.core

/**
 * Ayar metninden çözülmüş nesnelerin önbelleği: ham metin aynı kaldıkça JSON yeniden çözülmez.
 * SharedPreferences değişmeyen değer için aynı String nesnesini döndürdüğünden karşılaştırma ucuzdur.
 */
class ParseCache {
    private class Entry(val raw: String?, val value: Any?)

    private val entries = HashMap<String, Entry>()

    @Synchronized
    @Suppress("UNCHECKED_CAST")
    fun <T> get(key: String, raw: String?, parse: (String?) -> T): T {
        val e = entries[key]
        if (e != null && e.raw == raw) return e.value as T
        val value = parse(raw)
        entries[key] = Entry(raw, value)
        return value
    }
}
