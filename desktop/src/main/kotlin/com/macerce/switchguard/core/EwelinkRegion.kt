package com.macerce.switchguard.core

/**
 * eWeLink veri merkezleri. Bölge sunucu adresinin içine yazılır ("https://<bölge>-apia.coolkit.cc"); bu yüzden
 * giriş dönüşünden ya da yedek dosyasından gelen değer olduğu gibi kullanılmaz: "evil.com/#" gibi bir değer
 * token'ları başka bir sunucuya gönderirdi. Yalnızca bilinen bölgeler kabul edilir.
 */
object EwelinkRegion {
    const val DEFAULT = "eu"
    val ALL = setOf("eu", "us", "as", "cn")

    /** Bilinen bir bölgeyse küçük harfli hali, değilse null. */
    fun parse(value: String?): String? = value?.trim()?.lowercase()?.takeIf { it in ALL }
}
