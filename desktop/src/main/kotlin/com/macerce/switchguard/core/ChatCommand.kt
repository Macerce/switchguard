package com.macerce.switchguard.core

/**
 * Serbest yazılmış mesajı (Telegram ya da uygulama içi sohbet) komuta çevirir: "salon ve mutfak ışığını kapat",
 * "klima kaç watt", "kaç derece", "bahçe açık mı". Kural tabanlı, internetsiz; Türkçe ve İngilizce. Saf.
 */
object ChatCommand {

    sealed class Intent {
        /** Birden çok hedef: her biri ayrı cihaz/grup adı (sonunda kanal numarası olabilir). */
        data class Switch(val on: Boolean, val targets: List<String>) : Intent()
        /** Boş sorgu = genel durum. */
        data class Status(val query: String) : Intent()
        /** Boş sorgu = güç ölçen bütün cihazlar. */
        data class Power(val query: String) : Intent()
        data object Temperature : Intent()
        data class SetPresence(val away: Boolean) : Intent()
        data class RunScene(val query: String) : Intent()
        data object Help : Intent()
        data object Ack : Intent()
    }

    private val ON = setOf("ac", "acin", "acar", "acsana", "acabilir", "yak", "yakin", "on")
    private val OFF = setOf("kapat", "kapa", "kapatin", "kapatir", "kapatsana", "kapatabilir", "sondur", "off")
    private val ALL = setOf("tum", "tumu", "tumunu", "butun", "hepsi", "hepsini", "all", "every", "everything")
    private val FILLER = setOf(
        "turn", "switch", "lutfen", "please", "the", "misin", "musun", "mi", "mu", "bir", "da", "de", "su", "simdi", "now", "and", "ve",
    )
    private val POWER = setOf("watt", "w", "guc", "gucu", "tuketim", "tuketimi", "harcama", "power", "kwh", "enerji", "ceker", "cekiyor", "harciyor")
    private val TEMP = setOf("sicaklik", "derece", "hava", "temperature", "temp", "weather", "sicak", "soguk")
    private val STATUS = setOf("durum", "durumu", "durumda", "status", "acik", "kapali", "calisiyor", "nasil", "ne", "state", "is")
    private val HOME = setOf("evdeyim", "evde", "geldim", "home")
    private val AWAY = setOf("disaridayim", "disarida", "cikiyorum", "ciktim", "away")
    private val SCENE = setOf("sahne", "sahnesi", "sahnesini", "scene", "calistir", "baslat", "run")

    /** Komut değilse null. "/" ile başlayan Telegram komutları da anlaşılır ("/ac salon, mutfak", "/watt klima"). */
    fun parse(text: String): Intent? {
        val t = text.trim()
        if (t.isEmpty()) return null
        if (t.startsWith("/")) {
            val head = DeviceKinds.words(t.substringBefore(' ').substringBefore('@')).firstOrNull()
            val rest = t.substringAfter(' ', "").trim()
            when (head) {
                "watt", "guc", "power", "enerji" -> return Intent.Power(rest)
                "sicaklik", "derece", "temp", "temperature" -> return Intent.Temperature
            }
            return when (val c = TelegramBot.parseCommand(t) ?: return null) {
                TelegramBot.Command.Status -> Intent.Status(rest)
                TelegramBot.Command.Help -> Intent.Help
                TelegramBot.Command.Ack -> Intent.Ack
                is TelegramBot.Command.Switch -> Intent.Switch(c.on, split(rest).ifEmpty { listOf("") })
                is TelegramBot.Command.SetPresence -> Intent.SetPresence(c.away)
                is TelegramBot.Command.RunScene -> Intent.RunScene(c.query)
            }
        }
        val words = DeviceKinds.words(t)
        if (words.isEmpty()) return null
        if (words.size <= 2 && words.any { it in setOf("yardim", "help", "komutlar", "commands") }) return Intent.Help
        if (words.size <= 2 && words.any { it == "gordum" || it == "ack" }) return Intent.Ack
        if (words.size <= 3 && words.any { it in HOME }) return Intent.SetPresence(false)
        if (words.size <= 3 && words.any { it in AWAY }) return Intent.SetPresence(true)
        val on = words.any { it in ON }
        val off = words.any { it in OFF }
        if (words.any { it in POWER }) return Intent.Power(strip(words, POWER + STATUS + setOf("kac", "kadar", "how", "much", "many", "su", "an")))
        // "is the kitchen on?" bir soru, aç komutu değil.
        if (words.first() == "is" || words.first() == "are") return Intent.Status(strip(words, STATUS + setOf("are", "on", "off")))
        if (words.any { it in TEMP } && !on && !off) return Intent.Temperature
        if (words.any { it in SCENE } && words.any { it.startsWith("sahne") || it == "scene" }) {
            return Intent.RunScene(strip(words, SCENE))
        }
        if (on != off) {
            val targets = split(t).map { strip(DeviceKinds.words(it), ON + OFF) }.filter { it.isNotEmpty() }
            if (targets.isNotEmpty()) return Intent.Switch(on, targets)
        }
        if (words.any { it in STATUS }) return Intent.Status(strip(words, STATUS))
        return null
    }

    /** "salon, mutfak ve bahçe" → üç hedef. */
    private fun split(text: String): List<String> =
        text.split(Regex("""\s*(?:,|;|&|\+|\s(?:ve|and|ile|ayrica)\s)\s*""", RegexOption.IGNORE_CASE)).map { it.trim() }.filter { it.isNotEmpty() }

    private fun strip(words: List<String>, drop: Set<String>): String = words.filterNot { it in drop || it in FILLER }.joinToString(" ")

    sealed class Target {
        data class One(val device: DeviceSnapshot, val channel: Int) : Target()
        /** "tüm ışıklar": eşleşen bütün cihazlar. */
        data class Many(val devices: List<DeviceSnapshot>) : Target()
        data class Ambiguous(val names: List<String>) : Target()
        data class NeedChannel(val device: DeviceSnapshot) : Target()
        data object NotFound : Target()
    }

    /**
     * Hedefi cihaza çevirir. Önce [TelegramBot.match] (tam ad, baştan, içinde); bulunamazsa Türkçe eklere dayanıklı
     * kelime eşleşmesi: "salon ışığını" → "Salon Işık", "pompayı" → "Pompa". "tüm/bütün" ile bütün eşleşenler.
     */
    fun resolve(target: String, devices: List<DeviceSnapshot>): Target {
        val words = DeviceKinds.words(target)
        val all = words.any { it in ALL }
        val query = words.filterNot { it in ALL }
        if (query.isEmpty()) return Target.NotFound
        if (!all) {
            when (val m = TelegramBot.match(query.joinToString(" "), devices)) {
                is TelegramBot.Match.Found -> return Target.One(m.device, m.channel)
                is TelegramBot.Match.NeedChannel -> return Target.NeedChannel(m.device)
                is TelegramBot.Match.Ambiguous -> return Target.Ambiguous(m.names)
                TelegramBot.Match.NotFound -> Unit
            }
        }
        val number = query.last().toIntOrNull()
        val nameWords = if (number != null && query.size > 1) query.dropLast(1) else query
        val found = devices.filter { d ->
            val name = DeviceKinds.words(d.name)
            nameWords.all { q -> name.any { n -> similar(q, n) } }
        }
        return when {
            found.isEmpty() -> Target.NotFound
            all -> Target.Many(found)
            found.size > 1 -> Target.Ambiguous(found.map { it.name })
            else -> {
                val d = found[0]
                val ch = number?.minus(1)
                when {
                    !d.isMultiChannel -> Target.One(d, d.switches.keys.minOrNull() ?: 0)
                    ch != null && ch in d.switches -> Target.One(d, ch)
                    else -> Target.NeedChannel(d)
                }
            }
        }
    }

    /** Ekli/yumuşamış kelime aynı kök mü: "isigini"~"isik", "salonu"~"salon", "pompayi"~"pompa". */
    fun similar(q: String, n: String): Boolean {
        if (q == n) return true
        if (q.toIntOrNull() != null || n.toIntOrNull() != null) return false
        if (q.length >= 3 && n.startsWith(q)) return true
        if (n.length >= 3 && q.startsWith(n)) return true
        val common = q.zip(n).takeWhile { it.first == it.second }.size
        return common >= maxOf(3, n.length - 1)
    }

    /**
     * Cihazın çalışırken çektiği olağan güç (W): son günlerin ölçümlerinde bekleme sınırını aşanların ortancası.
     * Yeterli ölçüm yoksa null ("henüz öğreniliyor").
     */
    fun typicalWatts(samples: List<PowerSample>, minSamples: Int = 5): Double? {
        val working = samples.map { it.watts }.filter { it >= 3.0 }.sorted()
        if (working.size < minSamples) return null
        val mid = working.size / 2
        return if (working.size % 2 == 1) working[mid] else (working[mid - 1] + working[mid]) / 2
    }
}
