package com.macerce.switchguard.core

/**
 * Serbest yazılmış mesajı (Telegram ya da uygulama içi sohbet) komuta çevirir: "salon ve mutfak ışığını kapat",
 * "klima kaç watt", "kaç derece", "bahçe açık mı". Kural tabanlı, internetsiz; Türkçe ve İngilizce. Saf.
 */
object ChatCommand {

    sealed class Intent {
        /** Birden çok hedef: her biri ayrı cihaz/grup adı (sonunda kanal numarası olabilir). */
        data class Switch(val on: Boolean, val targets: List<String>) : Intent()
        /** Tek cümlede hem aç hem kapat: "mutfağı aç, salonu kapat". Adımlar söyleniş sırasıyla. */
        data class Batch(val steps: List<Switch>) : Intent()
        /**
         * Süreli komut. [hold] false: [seconds] sonra aç/kapat ("10 dakika sonra kapat").
         * [hold] true: şimdi aç/kapat, [seconds] sonra tersine çevir ("20 dakikalığına aç").
         */
        data class Timed(val on: Boolean, val targets: List<String>, val seconds: Int, val hold: Boolean) : Intent()
        /** "Hepsini kapat": bütün cihazlar; yürütülmeden önce onay istenir. */
        data class SwitchAll(val on: Boolean) : Intent()
        /** Onay bekleyen işleme "evet" / "hayır". */
        data class Confirm(val yes: Boolean) : Intent()
        /** "Sulama döngüsünü başlat / durdur". */
        data class RunCycle(val start: Boolean, val query: String) : Intent()
        /** "Neler açık?" */
        data object WhatsOn : Intent()
        /** Şu anki durumun sözlü özeti: hava, açık cihazlar, yakında çalışacak işler. */
        data object Briefing : Intent()
        /** Günün özeti. */
        data object Summary : Intent()
        /** Boş sorgu = genel durum. */
        data class Status(val query: String) : Intent()
        /** Boş sorgu = güç ölçen bütün cihazlar. */
        data class Power(val query: String) : Intent()
        data object Temperature : Intent()
        /**
         * Sensör okumaları (masaüstü; telefonda yok): "sensörler", "salonun nemi", "kapılar açık mı", "pil durumu".
         * [kinds] boşsa her tür; [query] boşsa bütün sensörlü cihazlar.
         */
        data class Sensors(val query: String, val kinds: Set<SensorKind>) : Intent()
        data class SetPresence(val away: Boolean) : Intent()
        data class RunScene(val query: String) : Intent()
        data object Help : Intent()
        data object Ack : Intent()
    }

    private val ON = setOf(
        "ac", "acin", "acar", "acsana", "acabilir", "acalim", "aciver", "acilsin", "yak", "yakin", "yakar", "yansin",
        "calistir", "baslat", "on", "start",
    )
    private val OFF = setOf(
        "kapat", "kapa", "kapatin", "kapatir", "kapatsana", "kapatabilir", "kapatalim", "kapativer", "kapansin",
        "sondur", "sondurun", "sonsun", "durdur", "off", "stop",
    )
    /** Cihaz adında geçmeyebilen genel sözler: "mutfağın ışığını" → adı yalnızca "Mutfak" olan cihaz. */
    private val GENERIC = listOf("isik", "lamba", "light", "lamp")
    private val NUMBERS = mapOf(
        "bir" to "1", "birinci" to "1", "iki" to "2", "ikinci" to "2", "uc" to "3", "ucuncu" to "3", "dort" to "4", "dorduncu" to "4",
        "one" to "1", "first" to "1", "two" to "2", "second" to "2", "three" to "3", "third" to "3", "four" to "4", "fourth" to "4",
    )
    private val CHANNEL = listOf("kanal", "channel", "numara", "number", "nolu")
    private val YES = setOf("evet", "onayla", "onay", "onayliyorum", "tamam", "yes", "confirm", "ok", "okay", "sure")
    private val NO = setOf("hayir", "iptal", "vazgec", "vazgectim", "no", "cancel", "nope")
    /** "her şeyi", "tüm cihazları", "everything": [ALL] ile birlikte hedefin tamamıysa bütün cihazlar. */
    private val EVERYTHING = setOf("her", "sey", "seyi", "cihaz", "cihazi", "cihazlar", "cihazlari", "device", "devices", "of", "them")
    private val CYCLE = listOf("dongu", "zamanlayici", "cycle", "timer")
    private val AMOUNTS = mapOf("yarim" to 0.5, "half" to 0.5, "bir" to 1.0, "iki" to 2.0, "uc" to 3.0, "dort" to 4.0, "bes" to 5.0,
        "a" to 1.0, "an" to 1.0, "one" to 1.0, "two" to 2.0, "three" to 3.0, "four" to 4.0, "five" to 5.0, "ten" to 10.0)
    private val ALL = setOf("tum", "tumu", "tumunu", "butun", "hepsi", "hepsini", "all", "every", "everything")
    private val FILLER = setOf(
        "turn", "switch", "lutfen", "please", "the", "misin", "musun", "mi", "mu", "bir", "da", "de", "su", "simdi", "now", "and", "ve",
    )
    private val POWER = setOf("watt", "w", "guc", "gucu", "tuketim", "tuketimi", "harcama", "power", "kwh", "enerji", "ceker", "cekiyor", "harciyor")
    private val TEMP = setOf("sicaklik", "derece", "hava", "temperature", "temp", "weather", "sicak", "soguk")
    private val STATUS = setOf("durum", "durumu", "durumda", "status", "acik", "kapali", "calisiyor", "nasil", "ne", "state", "is")
    /** Sensör türü sözleri (kök başı); "sensor" türsüzdür. */
    private val SENSOR_WORDS = listOf(
        "nem" to SensorKind.HUMIDITY, "humid" to SensorKind.HUMIDITY,
        "pil" to SensorKind.BATTERY, "batar" to SensorKind.BATTERY, "battery" to SensorKind.BATTERY,
        "kapi" to SensorKind.DOOR, "pencere" to SensorKind.DOOR, "door" to SensorKind.DOOR, "window" to SensorKind.DOOR,
        "hareket" to SensorKind.MOTION, "motion" to SensorKind.MOTION, "varlik" to SensorKind.MOTION,
        "kacak" to SensorKind.WATER, "kacag" to SensorKind.WATER, "leak" to SensorKind.WATER, "sizinti" to SensorKind.WATER,
        "aydinlik" to SensorKind.ILLUMINANCE, "lux" to SensorKind.ILLUMINANCE, "illumin" to SensorKind.ILLUMINANCE,
    )
    private fun sensorWord(w: String): Boolean = w.startsWith("sensor") || SENSOR_WORDS.any { (k, _) -> w.startsWith(k) }
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
        if (words.size <= 2 && words.all { it in YES }) return Intent.Confirm(true)
        if (words.size <= 2 && words.all { it in NO }) return Intent.Confirm(false)
        if (words.any { it.startsWith("brif") || it.startsWith("brief") } || ("neler" in words && "oluyor" in words) ||
            ("going" in words && "on" in words)) return Intent.Briefing
        if (words.any { it.startsWith("ozet") || it == "summary" }) {
            return if (words.any { it.startsWith("durum") || it == "status" }) Intent.Briefing else Intent.Summary
        }
        if ("bugun" in words && words.any { it == "oldu" || it == "olmus" }) return Intent.Summary
        if (words.any { it in setOf("neler", "hangi", "hangileri", "hangisi", "what", "whats", "which") } && words.any { it == "acik" || it == "on" }) {
            return Intent.WhatsOn
        }
        if ("acik" in words && words.any { it.startsWith("olan") }) return Intent.WhatsOn
        if (words.size <= 3 && words.any { it in HOME }) return Intent.SetPresence(false)
        if (words.size <= 3 && words.any { it in AWAY }) return Intent.SetPresence(true)
        val on = words.any { it in ON }
        val off = words.any { it in OFF }
        if (words.any { it in POWER }) return Intent.Power(strip(words, POWER + STATUS + setOf("kac", "kadar", "how", "much", "many", "su", "an")))
        if (!on && !off && words.any(::sensorWord)) {
            val kinds = words.flatMap { w -> SENSOR_WORDS.filter { w.startsWith(it.first) }.map { it.second } }.toSet()
            val rest = words.filterNot { w -> sensorWord(w) || w in STATUS || w in FILLER || w in ALL ||
                w in setOf("kac", "ne", "nedir", "durumu", "seviyesi", "orani", "var", "mi", "mu", "how", "what", "are", "any", "level", "su", "open", "closed", "kapali", "yok") }
            return Intent.Sensors(rest.joinToString(" "), kinds)
        }
        // "is the kitchen on?" bir soru, aç komutu değil.
        if (words.first() == "is" || words.first() == "are") return Intent.Status(strip(words, STATUS + setOf("are", "on", "off")))
        if (words.any { it in TEMP } && !on && !off) return Intent.Temperature
        if (words.any { it in SCENE } && words.any { it.startsWith("sahne") || it == "scene" }) {
            return Intent.RunScene(strip(words, SCENE))
        }
        if ((on || off) && words.any { w -> CYCLE.any { w.startsWith(it) } }) {
            if (on == off) return null
            return Intent.RunCycle(on, words.filterNot { w -> w in ON || w in OFF || w in FILLER || CYCLE.any { w.startsWith(it) } }.joinToString(" "))
        }
        if (on || off) {
            val timed = duration(t)
            val steps = clauses(timed?.rest ?: t)
            val everything = steps.size == 1 && steps[0].targets.size == 1 &&
                DeviceKinds.words(steps[0].targets[0]).let { w -> w.any { it in ALL || it == "sey" || it == "seyi" } && w.all { it in ALL || it in EVERYTHING } }
            if (timed != null) {
                // Süreli komut tek yönlü olmalı; karışık ya da "hepsini" ile süre birlikte anlaşılmaz sayılır.
                return if (steps.size == 1 && !everything) Intent.Timed(steps[0].on, steps[0].targets, timed.seconds, timed.hold) else null
            }
            if (everything) return Intent.SwitchAll(steps[0].on)
            if (steps.size == 1) return steps[0]
            if (steps.size > 1) return Intent.Batch(steps)
        }
        if (words.any { it in STATUS }) return Intent.Status(strip(words, STATUS))
        return null
    }

    /**
     * Cümleyi aç/kapat sözlerinden böler: "mutfağı aç salonu kapat" → iki adım. Türkçede söz hedeften sonra gelir
     * ("salonu aç"), İngilizcede çoğu kez önce ("turn on kitchen"); cümle sözle başlıyorsa her söz ardındakine uygulanır.
     * Art arda aynı yöndeki adımlar birleşir.
     */
    private fun clauses(text: String): List<Intent.Switch> {
        val tokens = text.trim().split(Regex("""\s+"""))
        fun verb(token: String): Boolean? = DeviceKinds.words(token).singleOrNull()?.let { if (it in ON) true else if (it in OFF) false else null }
        val firstReal = tokens.firstOrNull { tok -> DeviceKinds.words(tok).any { it !in FILLER } }
        val leading = firstReal != null && verb(firstReal) != null
        val parts = mutableListOf<Pair<Boolean, String>>()
        val pending = StringBuilder()
        var current: Boolean? = null
        for (tok in tokens) {
            val v = verb(tok)
            if (v == null) {
                pending.append(tok).append(' ')
                continue
            }
            if (leading) current?.let { parts += it to pending.toString() } else parts += v to pending.toString()
            pending.clear()
            current = v
        }
        // Son sözden sonra kalanlar: baştaki düzende onun hedefi, sondaki düzende son adıma eklenir ("salonu kapat, mutfağı da").
        current?.let { parts += it to pending.toString() }
        val steps = mutableListOf<Intent.Switch>()
        for ((on, chunk) in parts) {
            val targets = split(chunk).map { target(DeviceKinds.words(it)) }.filter { it.isNotEmpty() }
            if (targets.isEmpty()) continue
            val last = steps.lastOrNull()
            if (last != null && last.on == on) steps[steps.size - 1] = last.copy(targets = last.targets + targets) else steps += Intent.Switch(on, targets)
        }
        return steps
    }

    private class Duration(val seconds: Int, val hold: Boolean, val rest: String)

    private fun unit(w: String): Int = when {
        w.startsWith("saniye") || w in setOf("sn", "sec", "secs", "second", "seconds") -> 1
        w.startsWith("dakika") || w in setOf("dk", "min", "mins", "minute", "minutes") -> 60
        w.startsWith("saat") || w in setOf("hour", "hours", "hr", "hrs") -> 3600
        else -> 0
    }

    /**
     * Cümledeki süreyi bulur ve çıkarır: "10 dakika sonra", "yarım saat sonra", "in 10 minutes" (gecikme);
     * "20 dakikalığına", "1 saat boyunca", "for 20 minutes" (o süre boyunca). Süre yoksa ya da yönü belli değilse null.
     */
    private fun duration(text: String): Duration? {
        val tokens = text.trim().split(Regex("""\s+"""))
        val norm = tokens.map { DeviceKinds.words(it).joinToString("") }
        for (i in 1 until tokens.size) {
            val u = unit(norm[i])
            if (u == 0) continue
            var first = i - 1
            var amount = norm[first].toDoubleOrNull() ?: AMOUNTS[norm[first]] ?: continue
            // "half an hour"
            if (norm[first] in setOf("a", "an") && first > 0 && norm[first - 1] == "half") { first--; amount = 0.5 }
            var last = i
            val before = norm.getOrNull(first - 1)
            val after = norm.getOrNull(i + 1)
            val hold: Boolean
            when {
                norm[i].endsWith("ligina") || norm[i].endsWith("ligine") -> hold = true
                after in setOf("boyunca", "sureyle", "sureligine") -> { hold = true; last = i + 1 }
                after == "sonra" || after == "later" -> { hold = false; last = i + 1 }
                before == "for" -> { hold = true; first-- }
                before == "in" || before == "after" -> { hold = false; first-- }
                else -> continue
            }
            val seconds = (amount * u).toLong().coerceIn(Cycle.MIN_STEP_SECONDS.toLong(), Cycle.MAX_STEP_SECONDS.toLong()).toInt()
            return Duration(seconds, hold, tokens.filterIndexed { j, _ -> j < first || j > last }.joinToString(" "))
        }
        return null
    }

    /** Hedef sözleri; sondaki "bir" kanal numarası olabilir ("pompa bir"), başka yerde dolgu ("şunu bir aç"). */
    private fun target(words: List<String>): String {
        val kept = words.filterIndexed { i, w -> w !in ON && w !in OFF && (w !in FILLER || (w == "bir" && i == words.lastIndex && i > 0)) }
        return if (kept == listOf("bir")) "" else kept.joinToString(" ")
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
        // Konuşma dili: "pompanın ikinci kanalı" → "pompanin 2".
        val spoken = tidy(query)
        if (spoken.isEmpty()) return Target.NotFound
        if (!all && spoken != query) {
            val m = TelegramBot.match(spoken.joinToString(" "), devices)
            if (m is TelegramBot.Match.Found) return Target.One(m.device, m.channel)
        }
        val number = spoken.last().toIntOrNull()
        val nameWords = if (number != null && spoken.size > 1) spoken.dropLast(1) else spoken
        fun search(wanted: List<String>) = devices.filter { d ->
            val name = DeviceKinds.words(d.name)
            wanted.all { q -> name.any { n -> similar(q, n) } }
        }
        var found = search(nameWords)
        if (found.isEmpty()) {
            // "mutfağın ışığını": cihazın adı yalnızca "Mutfak" olabilir.
            val specific = nameWords.filterNot { q -> GENERIC.any { similar(q, it) } }
            if (specific.isNotEmpty() && specific.size < nameWords.size) found = search(specific)
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

    /** Sayı sözlerini rakama çevirir, "kanal/numara" sözlerini atar, kanal numarasını sona alır. */
    private fun tidy(query: List<String>): List<String> {
        val words = query.filterNot { w -> CHANNEL.any { w.startsWith(it) } }.mapIndexed { i, w -> if (i > 0) NUMBERS[w] ?: w else w }
        val i = words.indexOfLast { it.toIntOrNull() != null }
        return if (i > 0 && i < words.lastIndex) words.filterIndexed { j, _ -> j != i } + words[i] else words
    }

    /**
     * Ekli/yumuşamış kelime aynı kök mü: "isigini"~"isik", "mutfagin"~"mutfak", "salonu"~"salon", "pompayi"~"pompa".
     * Yalnız son ünsüz yumuşaması (k→ğ, p→b, t→d) sayılır; "isiticiyi" ile "isik" aynı kök değildir.
     */
    fun similar(q: String, n: String): Boolean {
        if (q == n) return true
        if (q.toIntOrNull() != null || n.toIntOrNull() != null) return false
        if (q.length >= 3 && n.startsWith(q)) return true
        if (n.length < 3) return false
        if (q.startsWith(n)) return true
        val soft = when (n.last()) {
            'k' -> 'g'
            'p' -> 'b'
            't' -> 'd'
            else -> return false
        }
        return q.length > n.length && q.startsWith(n.dropLast(1) + soft)
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
