package com.macerce.switchguard.desktop.i18n

import com.macerce.switchguard.core.ChangeLabels
import com.macerce.switchguard.core.Demo
import com.macerce.switchguard.core.DurationStrings
import com.macerce.switchguard.core.DurationText
import com.macerce.switchguard.core.EventStrings
import com.macerce.switchguard.desktop.Log
import java.io.InputStream
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.Date
import java.util.IllegalFormatException
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

/**
 * Seçili dilin metin tablosu. Telefonun `strings.xml` dosyaları (EN/TR) ile masaüstüne özel `desktop.xml`
 * dosyaları classpath'ten okunur ve dil başına birleştirilir. Anahtar seçili dilde yoksa İngilizce,
 * orada da yoksa anahtarın kendisi döner. Çoğul metinler (`<plurals>`) [plural] ile okunur.
 */
class Strings internal constructor(
    /** "tr" ya da "en". */
    val language: String,
    private val table: Map<String, String>,
    private val english: Map<String, String>,
    /** Anahtar → (miktar → şablon); miktarlar Android'deki gibi "one" / "other". */
    private val plurals: Map<String, Map<String, String>> = emptyMap(),
    private val englishPlurals: Map<String, Map<String, String>> = emptyMap(),
) {
    private val warned: MutableSet<String> = ConcurrentHashMap.newKeySet()

    /** Seçili dilin yerel ayarı (saat, tarih ve sayı biçimleri için). */
    val locale: Locale get() = Locale.forLanguageTag(language)

    operator fun get(key: String): String =
        table[key] ?: english[key] ?: key.also { if (warned.add(key)) Log.w(TAG, "missing string '$key' ($language)") }

    /**
     * Şablonu biçimler; asla fırlatmaz: alarm/olay metni bir çeviri hatası yüzünden düşmesin. Seçili dildeki şablon
     * argümanlara uymazsa İngilizce şablon denenir; o da uymazsa şablon olduğu gibi, argümanlar köşeli parantezde döner.
     */
    fun format(key: String, vararg args: Any): String {
        val template = get(key)
        tryFormat(language, template, args)?.let { return it }
        english[key]?.takeIf { it != template }?.let { en -> tryFormat("en", en, args)?.let { return it } }
        return "$template [${args.joinToString(", ")}]"
    }

    /** Telefondaki `stringArrayResource`: `<string-array>` öğeleri. */
    fun array(key: String): List<String> = generateSequence(0) { it + 1 }.map { table["$key[$it]"] ?: english["$key[$it]"] }.takeWhile { it != null }.map { it!! }.toList()

    /** Telefondaki `getString(id, ...)`: argüman yoksa şablon olduğu gibi ([get]), varsa [format]. */
    fun str(key: String, vararg args: Any): String = if (args.isEmpty()) get(key) else format(key, *args)

    /**
     * Çoğul metin (telefondaki `getQuantityString`): [count] 1 ise "one", değilse "other" şablonu [args] ile biçimlenir.
     * İngilizce ve Türkçe yalnızca bu iki biçimi kullanır. [format] gibi asla fırlatmaz.
     */
    fun plural(key: String, count: Int, vararg args: Any): String {
        val quantity = if (count == 1) "one" else "other"
        fun pick(from: Map<String, Map<String, String>>) = from[key]?.let { it[quantity] ?: it["other"] }
        val template = pick(plurals) ?: pick(englishPlurals)
            ?: return key.also { if (warned.add(key)) Log.w(TAG, "missing plural '$key' ($language)") }
        tryFormat(language, template, args)?.let { return it }
        pick(englishPlurals)?.takeIf { it != template }?.let { en -> tryFormat("en", en, args)?.let { return it } }
        return "$template [${args.joinToString(", ")}]"
    }

    private fun tryFormat(lang: String, template: String, args: Array<out Any>): String? = try {
        String.format(Locale.forLanguageTag(lang), template, *args)
    } catch (e: IllegalFormatException) {
        Log.w(TAG, "bad format for '$template' ($lang): $e")
        null
    }

    /** Telefondaki `EventLog.durations(context)`: "2 dk 10 sn" gibi süre kalıpları. */
    fun durations() = DurationStrings(
        seconds = { format("dur_seconds", it) },
        minSec = { m, sec -> format("dur_min_sec", m, sec) },
        minutes = { format("dur_minutes", it) },
        hourMin = { h, m -> format("dur_hour_min", h, m) },
        dayHour = { d, h -> format("dur_day_hour", d, h) },
    )

    /** Kısa saat ("14:02" / "2:02 PM"), seçili dilde. */
    fun time(ms: Long): String = DateFormat.getTimeInstance(DateFormat.SHORT, locale).format(Date(ms))

    /** "14:02 – 14:37"; farklı günlerdeyse tarihle birlikte (telefondaki `DateUtils.formatDateRange` karşılığı). */
    fun timeRange(from: Long, to: Long): String {
        fun day(t: Long) = Instant.ofEpochMilli(t).atZone(ZoneId.systemDefault()).toLocalDate()
        if (day(from) == day(to)) return "${time(from)} – ${time(to)}"
        val date = SimpleDateFormat("d MMM", locale)
        return "${date.format(Date(from))} ${time(from)} – ${date.format(Date(to))} ${time(to)}"
    }

    /** Telefondaki `EventLog.strings(context)` ile aynı anahtarlar. */
    fun eventStrings() = EventStrings(
        labels = ChangeLabels(
            on = get("label_on"),
            off = get("label_off"),
            online = get("label_online"),
            offline = get("label_offline"),
        ) { name, n -> format("label_channel", name, n) },
        shortDrop = { format("event_short_drop", it) },
        test = get("test_alarm_line"),
        automationFired = { format("automation_fired", it) },
        deviceMissing = get("device_missing"),
        automationFailed = { t, e -> format("automation_failed", t, e) },
        powerStale = { format("power_data_stale", it) },
        monitorGap = { from, to -> format("event_monitor_gap", timeRange(from, to), DurationText.format(to - from, durations())) },
        monitorBlind = { format("event_monitor_blind", time(it)) },
        monitorRestored = { from, to -> format("event_monitor_restored", DurationText.format(to - from, durations())) },
        alarmDismissed = { n, ms -> plural("event_alarm_dismissed", n, n, DurationText.format(ms, durations())) },
        reminder = { label, state, ms -> format("event_reminder", label, state, DurationText.format(ms, durations())) },
        escalation = { n, min, err ->
            if (err == null) plural("event_escalated", n, n, min) else format("event_escalation_failed", min, err)
        },
        outage = { n -> plural("event_outage", n, n) },
        outageEnded = { from, to -> format("event_outage_ended", DurationText.format(to - from, durations()), timeRange(from, to)) },
        energyAnomaly = { name, high, kwh, avg ->
            val pct = kotlin.math.abs(((kwh / avg - 1) * 100).toInt())
            format(if (high) "event_energy_high" else "event_energy_low", name, pct, formatKwh(kwh), formatKwh(avg))
        },
        budget = { cost, budget -> format("event_budget", money(cost), money(budget)) },
        telegramAck = { who, ms -> format("event_tg_ack", who, DurationText.format(ms, durations())) },
        telegramSwitch = { who, label, on, err ->
            val text = format("event_tg_switch", who, label, get(if (on) "label_on" else "label_off"))
            if (err != null) format("automation_failed", text, err) else text
        },
        charger = { lost, ms ->
            if (lost) get("desktop_event_ac_lost") else format("desktop_event_ac_back", DurationText.format(ms, durations()))
        },
        scene = { name, ok, failed, by ->
            val base = plural("event_scene", ok, name, ok)
            val fail = if (failed > 0) plural("event_scene_failed", failed, failed) else ""
            base + fail + (by?.let { " · $it" } ?: "")
        },
        presence = { away, by, ended ->
            if (ended) get("event_vacation_ended")
            else get(if (away) "event_presence_away" else "event_presence_home") + (by?.let { " · $it" } ?: "")
        },
        watchdog = { name, lost, ms ->
            format(if (lost) "event_watchdog_lost" else "event_watchdog_back", name, DurationText.format(ms, durations()))
        },
    )

    /** "1,25 kWh" / "350 Wh" (telefondaki `DailySummary.formatKwh`). */
    fun formatKwh(kwh: Double): String =
        if (kwh < 1) String.format(locale, "%.0f Wh", kwh * 1000) else String.format(locale, "%.2f kWh", kwh)

    /** Para tutarı, sistemin para birimiyle (telefondaki `EnergyFormat.money`). */
    fun money(amount: Double): String = com.macerce.switchguard.desktop.ui.energy.EnergyFormat.money(amount, locale)

    /** Telefondaki `Actions.enableDemo` ile aynı anahtarlar. */
    fun demoNames() = Demo.Names(
        freezer = get("demo_freezer"),
        pumps = get("demo_pumps"),
        light = get("demo_light"),
        stalledPump = get("demo_auto_pump"),
        lightLeftOn = get("demo_auto_light"),
    )

    companion object {
        private const val TAG = "Strings"
        /**
         * Telefonun metinleri + masaüstüne özgüler. Masaüstü metinleri ekran grubuna göre ayrı dosyalarda (birlikte
         * çalışılırken aynı dosyaya yazılıp çakışmasın); hiçbiri telefon anahtarını gölgeleyemez (StringsTest).
         */
        internal val FILES = listOf("strings.xml", "desktop.xml", "desktop-devices.xml", "desktop-settings.xml", "desktop-alerts.xml", "desktop-v06.xml")

        /** [language] "tr" ya da "en"; boşsa sistem dili Türkçeyse "tr", değilse "en"; bilinmeyen dil "en" olur. */
        fun load(language: String): Strings {
            val lang = when (language.ifEmpty { Locale.getDefault().language }) {
                "tr" -> "tr"
                else -> "en"
            }
            val english = readTable("values")
            val turkish = if (lang == "en") null else readTable("values-tr")
            return Strings(
                lang,
                if (turkish == null) english.strings else english.strings + turkish.strings,
                english.strings,
                if (turkish == null) english.plurals else english.plurals + turkish.plurals,
                english.plurals,
            )
        }

        private class Table(val strings: Map<String, String>, val plurals: Map<String, Map<String, String>>)

        private fun readTable(dir: String): Table =
            FILES.fold(Table(emptyMap(), emptyMap())) { acc, file ->
                val path = "/strings/$dir/$file"
                val bytes = (Strings::class.java.getResourceAsStream(path) ?: error("missing resource $path")).use { it.readBytes() }
                Table(acc.strings + parse(bytes.inputStream()), acc.plurals + parsePlurals(bytes.inputStream()))
            }

        /** Android `<string name="...">` öğelerini ([unescape] uygulanmış) okur. */
        internal fun parse(xml: InputStream): Map<String, String> {
            val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xml)
            val nodes = doc.getElementsByTagName("string")
            val out = LinkedHashMap<String, String>()
            for (i in 0 until nodes.length) {
                val e = nodes.item(i) as Element
                out[e.getAttribute("name")] = unescape(e.textContent)
            }
            // <string-array>: öğeler "ad[0]", "ad[1]" … anahtarlarıyla ([array]).
            val arrays = doc.getElementsByTagName("string-array")
            for (i in 0 until arrays.length) {
                val e = arrays.item(i) as Element
                val items = e.getElementsByTagName("item")
                for (j in 0 until items.length) out[e.getAttribute("name") + "[$j]"] = unescape(items.item(j).textContent)
            }
            return out
        }

        /** Android `<plurals name="..."><item quantity="...">` öğelerini ([unescape] uygulanmış) okur. */
        internal fun parsePlurals(xml: InputStream): Map<String, Map<String, String>> {
            val nodes = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xml).getElementsByTagName("plurals")
            val out = LinkedHashMap<String, Map<String, String>>()
            for (i in 0 until nodes.length) {
                val e = nodes.item(i) as Element
                val items = e.getElementsByTagName("item")
                out[e.getAttribute("name")] = (0 until items.length).associate { j ->
                    val item = items.item(j) as Element
                    item.getAttribute("quantity") to unescape(item.textContent)
                }
            }
            return out
        }

        /** Android kaçışları: dıştaki çift tırnaklar atılır; `\'` `\"` `\n` `\t` `\\`; bilinmeyen `\x` → `x`. */
        internal fun unescape(raw: String): String {
            val s = if (raw.length >= 2 && raw.startsWith('"') && raw.endsWith('"')) raw.substring(1, raw.length - 1) else raw
            val sb = StringBuilder(s.length)
            var i = 0
            while (i < s.length) {
                val c = s[i]
                if (c == '\\' && i + 1 < s.length) {
                    i++
                    sb.append(
                        when (val n = s[i]) {
                            'n' -> '\n'
                            't' -> '\t'
                            else -> n
                        }
                    )
                } else {
                    sb.append(c)
                }
                i++
            }
            return sb.toString()
        }
    }
}
