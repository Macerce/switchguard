package com.macerce.switchguard.desktop.ui

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Cihaz kartının görünümü. Genel boyut Cihazlar ekranından seçilir (`Store.cardSize`); bir cihaz telefondaki gibi
 * "küçük kart" işaretliyse (`DeviceLayout.compact`) genel boyuttan bağımsız olarak küçük görünür.
 */
enum class CardStyle {
    /** Ad, durum, güç, kanallar (bugünkü kart). */
    LARGE,

    /** Daha dar ve sık: aynı içerik, küçük yazı ve boşluk. */
    MEDIUM,

    /** Tek satır: durum noktası, ad, anahtar (çok kanallıda açık kanal sayısı). */
    SMALL;

    companion object {
        fun parse(size: String): CardStyle = when (size) {
            "medium" -> MEDIUM
            "small" -> SMALL
            else -> LARGE
        }

        fun of(size: String, compact: Boolean): CardStyle = if (compact) SMALL else parse(size)

        /** Izgarada bir kartın en küçük genişliği: küçüldükçe bir satıra daha çok kart sığar. */
        fun gridMinWidth(size: String): Dp = when (parse(size)) {
            LARGE -> 340.dp
            MEDIUM -> 260.dp
            SMALL -> 200.dp
        }
    }
}
