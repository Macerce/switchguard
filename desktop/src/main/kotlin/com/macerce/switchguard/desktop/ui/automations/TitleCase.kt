package com.macerce.switchguard.desktop.ui.automations

import java.util.Locale

/**
 * Seçim listelerinde gösterim için her kelimenin baş harfini büyütür ("şu süre açık kalınca:" → "Şu Süre Açık
 * Kalınca"); dilin kuralıyla (Türkçede i → İ). Sondaki iki nokta atılır: metin cümle içinde ("… kalınca: 30 dk")
 * kullanılmak için yazılmıştır, listede tek başına durur.
 */
internal fun titleCase(text: String, language: String): String {
    val locale = Locale.forLanguageTag(language)
    return text.trim().trimEnd(':').trim().split(' ').joinToString(" ") { word ->
        if (word.isEmpty()) word else word.substring(0, 1).uppercase(locale) + word.substring(1)
    }
}
