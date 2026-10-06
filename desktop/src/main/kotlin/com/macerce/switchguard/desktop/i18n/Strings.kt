package com.macerce.switchguard.desktop.i18n

import com.macerce.switchguard.core.ChangeLabels
import com.macerce.switchguard.core.Demo
import com.macerce.switchguard.core.EventStrings
import com.macerce.switchguard.desktop.Log
import java.io.InputStream
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

/**
 * Seçili dilin metin tablosu. Telefonun `strings.xml` dosyaları (EN/TR) ile masaüstüne özel `desktop.xml`
 * dosyaları classpath'ten okunur ve dil başına birleştirilir. Anahtar seçili dilde yoksa İngilizce,
 * orada da yoksa anahtarın kendisi döner.
 */
class Strings private constructor(
    /** "tr" ya da "en". */
    val language: String,
    private val table: Map<String, String>,
    private val english: Map<String, String>,
) {
    private val warned: MutableSet<String> = ConcurrentHashMap.newKeySet()

    operator fun get(key: String): String =
        table[key] ?: english[key] ?: key.also { if (warned.add(key)) Log.w(TAG, "missing string '$key' ($language)") }

    fun format(key: String, vararg args: Any): String = String.format(Locale.forLanguageTag(language), get(key), *args)

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
    )

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
        private val FILES = listOf("strings.xml", "desktop.xml")

        /** [language] "tr" ya da "en"; boşsa sistem dili Türkçeyse "tr", değilse "en"; bilinmeyen dil "en" olur. */
        fun load(language: String): Strings {
            val lang = when (language.ifEmpty { Locale.getDefault().language }) {
                "tr" -> "tr"
                else -> "en"
            }
            val english = readTable("values")
            return Strings(lang, if (lang == "en") english else english + readTable("values-tr"), english)
        }

        private fun readTable(dir: String): Map<String, String> =
            FILES.fold(emptyMap()) { acc, file ->
                val path = "/strings/$dir/$file"
                val stream = Strings::class.java.getResourceAsStream(path) ?: error("missing resource $path")
                acc + stream.use(::parse)
            }

        /** Android `<string name="...">` öğelerini ([unescape] uygulanmış) okur. */
        internal fun parse(xml: InputStream): Map<String, String> {
            val nodes = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xml).getElementsByTagName("string")
            val out = LinkedHashMap<String, String>()
            for (i in 0 until nodes.length) {
                val e = nodes.item(i) as Element
                out[e.getAttribute("name")] = unescape(e.textContent)
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
