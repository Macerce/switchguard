package com.macerce.switchguard.core

import org.json.JSONArray

/**
 * GitHub'daki yayınlardan bu uygulamanın daha yeni sürümünü bulur. Telefon ve masaüstü aynı depoda ayrı etiketlerle
 * yayınlanır: telefon `v2.16.0`, masaüstü `desktop-v0.7.0`. Taslak ve ön sürümler sayılmaz.
 */
object UpdateCheck {
    const val RELEASES_URL = "https://api.github.com/repos/Macerce/switchguard/releases?per_page=30"
    const val PHONE_TAG = "v"
    const val DESKTOP_TAG = "desktop-v"

    /** [assetUrl]: adı [assetSuffix] ile biten ilk dosyanın indirme adresi (yoksa null). */
    data class Release(val version: String, val pageUrl: String, val assetUrl: String?)

    /** [tagPrefix] ile etiketlenmiş, [current]'tan yeni en yüksek sürüm; yoksa null. */
    fun newer(releasesJson: String, tagPrefix: String, current: String, assetSuffix: String = ""): Release? {
        val list = JSONArray(releasesJson)
        var best: Release? = null
        for (i in 0 until list.length()) {
            val r = list.optJSONObject(i) ?: continue
            if (r.optBoolean("draft") || r.optBoolean("prerelease")) continue
            val tag = r.optString("tag_name")
            if (!tag.startsWith(tagPrefix)) continue
            val version = tag.removePrefix(tagPrefix)
            if (parse(version) == null) continue
            if (compare(version, current) <= 0 || (best != null && compare(version, best.version) <= 0)) continue
            val assets = r.optJSONArray("assets") ?: JSONArray()
            val asset = (0 until assets.length()).mapNotNull { assets.optJSONObject(it) }
                .firstOrNull { assetSuffix.isNotEmpty() && it.optString("name").endsWith(assetSuffix, ignoreCase = true) }
            best = Release(version, r.optString("html_url"), asset?.optString("browser_download_url"))
        }
        return best
    }

    /** "2.16.0" → [2, 16, 0]; sürüm değilse null. "2.16.0-kisisel" gibi ekler yok sayılır. */
    fun parse(version: String): List<Int>? =
        version.substringBefore('-').split('.').map { it.toIntOrNull() ?: return null }.takeIf { it.isNotEmpty() }

    /** Sayısal karşılaştırma (2.10.0 > 2.9.9); eksik kısımlar 0 sayılır, okunamayan sürüm en eski kabul edilir. */
    fun compare(a: String, b: String): Int {
        val x = parse(a) ?: return if (parse(b) == null) 0 else -1
        val y = parse(b) ?: return 1
        for (i in 0 until maxOf(x.size, y.size)) {
            val c = x.getOrElse(i) { 0 }.compareTo(y.getOrElse(i) { 0 })
            if (c != 0) return c
        }
        return 0
    }
}
