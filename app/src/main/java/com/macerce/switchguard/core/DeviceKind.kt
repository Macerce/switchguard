package com.macerce.switchguard.core

/**
 * Cihazın ekranda hangi simgeyle gösterileceği. Bulutlar cihaz türünü güvenilir bildirmediğinden
 * (eWeLink uiid'i röle kartını, Tuya kategorisi prizi söyler; ikisi de "ne bağlı" demez) önce kullanıcının
 * verdiği addan tahmin edilir, ad bir şey söylemiyorsa cihazın özelliklerine bakılır.
 */
enum class DeviceKind { LIGHT, WATER, COLD, AIR, HEAT, DOOR, PLUG, RELAYS, SWITCH,
    /** Sanal cihaz: [DeviceGroup]. */
    GROUP,
    /** Otomasyon tetikleyicisindeki sanal cihaz: [DeviceCategory]. */
    CATEGORY }

object DeviceKinds {
    /** Ad içindeki kelimenin başına bakılır: Türkçe ekler ("rögarı", "pompası") eşleşmeyi bozmasın. */
    private val prefixes: List<Pair<DeviceKind, List<String>>> = listOf(
        DeviceKind.LIGHT to listOf("aydinlat", "isik", "isig", "lamba", "avize", "spot", "projektor", "light", "lamp", "bulb", "led"),
        DeviceKind.WATER to listOf("rogar", "flator", "pompa", "hidrofor", "depo", "tank", "havuz", "sulama", "seviye",
            "water", "pump", "pool", "sewer", "manhole", "level", "irrigat", "sump"),
        DeviceKind.COLD to listOf("dondurucu", "buzdolab", "sogutucu", "sogutma", "freezer", "fridge", "cooler", "chiller"),
        DeviceKind.AIR to listOf("klima", "vantilator", "aspirator", "havalandir", "fan", "hvac", "aircon"),
        DeviceKind.HEAT to listOf("isitici", "kombi", "kazan", "boyler", "termosifon", "heater", "boiler"),
        DeviceKind.DOOR to listOf("kapi", "garaj", "bariyer", "door", "gate", "garage"),
        DeviceKind.PLUG to listOf("priz", "plug", "socket", "outlet"),
    )

    /** Tek başına anlamlı kısa kelimeler: önek olarak aranırsa başka kelimelere de uyardı ("su" → "sunucu"). */
    private val exact: Map<String, DeviceKind> = mapOf(
        "su" to DeviceKind.WATER, "suyu" to DeviceKind.WATER, "ac" to DeviceKind.AIR,
    )

    fun of(device: DeviceSnapshot): DeviceKind {
        if (DeviceGroup.isGroup(device.id)) return DeviceKind.GROUP
        if (DeviceCategory.isCategory(device.id)) return DeviceKind.CATEGORY
        for (word in words(device.name)) {
            exact[word]?.let { return it }
            for ((kind, list) in prefixes) if (list.any { word.startsWith(it) }) return kind
        }
        return when {
            device.isMultiChannel -> DeviceKind.RELAYS
            // Enerji ölçen tek kanallı cihazların çoğu akıllı prizdir (POW, S40, Tuya prizler).
            device.hasEnergy || device.cloud == Cloud.TUYA -> DeviceKind.PLUG
            else -> DeviceKind.SWITCH
        }
    }

    /** Küçük harf, Türkçe harfler sadeleştirilmiş kelimeler ("Rögar Işığı" → rogar, isigi). */
    fun words(name: String): List<String> {
        val folded = buildString {
            for (c in name) {
                append(
                    when (c) {
                        'ı', 'I', 'İ', 'i' -> 'i'
                        'ğ', 'Ğ' -> 'g'
                        'ü', 'Ü' -> 'u'
                        'ş', 'Ş' -> 's'
                        'ö', 'Ö' -> 'o'
                        'ç', 'Ç' -> 'c'
                        else -> c.lowercaseChar()
                    }
                )
            }
        }
        return folded.split(Regex("[^\\p{L}\\p{Nd}]+")).filter { it.isNotEmpty() }
    }
}
