package com.macerce.switchguard.core

import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.UUID
import java.util.zip.Deflater
import java.util.zip.Inflater

/**
 * Tek bir kuralı metin (ve QR kod) olarak paylaşma: "SGRULE1:" + sıkıştırılmış JSON (base64url).
 * Kuralın kullandığı cihazların adları da eklenir; içe aktaran cihazda kimlik yoksa cihaz ADINA göre eşlenir
 * (başka hesap/başka ev). Eşlenemeyen başvuru sayılır, kural KAPALI eklenir: kullanıcı gözden geçirip açar.
 * Anahtar, token ya da hesap bilgisi yoktur; yalnız kuralın kendisi ve cihaz adları.
 */
object RuleShare {
    const val PREFIX = "SGRULE1:"

    data class Imported(val rule: Automation, val unresolved: Int, val renamed: Int)

    /** Kuralın başvurduğu bütün cihaz kimlikleri (tetikleyiciler, koşullar, adımlar ve iç içe koşulları). */
    fun deviceRefs(a: Automation): Set<String> = buildSet {
        (listOf(a.trigger) + a.extraTriggers).forEach { add(it.deviceId) }
        a.conditions.forEach { add(it.deviceId) }
        a.flatActions.forEach { x -> add(x.deviceId); x.cond?.items?.forEach { add(it.deviceId) } }
    }.filter { it.isNotEmpty() }.toSet()

    fun encode(a: Automation, names: Map<String, String>): String {
        val rule = JSONArray(Automation.listToJson(listOf(a))).getJSONObject(0)
        val n = JSONObject().apply { deviceRefs(a).forEach { id -> names[id]?.let { put(id, it) } } }
        val json = JSONObject().put("v", 1).put("rule", rule).put("names", n).toString().toByteArray(Charsets.UTF_8)
        val d = Deflater(Deflater.BEST_COMPRESSION).apply { setInput(json); finish() }
        val out = ByteArrayOutputStream()
        val buf = ByteArray(1024)
        while (!d.finished()) out.write(buf, 0, d.deflate(buf))
        d.end()
        return PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(out.toByteArray())
    }

    /** Metnin içinde paylaşılmış kural var mı (mesajın ortasına yapıştırılmış olabilir). */
    fun find(text: String): String? = Regex(Regex.escape(PREFIX) + "[A-Za-z0-9_-]+").find(text)?.value

    /**
     * Paylaşılan metni kurala çevirir. [local]: bu cihazdaki kimlik → ad. Kimlik burada da varsa aynen kalır; yoksa
     * gelen ad ile (büyük/küçük harf ve baştaki/sondaki boşluk yok sayılarak) tek bir yerel cihaz eşleşirse ona
     * çevrilir. Yeni kimlik verilir, kapalı eklenir. Bozuksa null.
     */
    fun decode(text: String, local: Map<String, String>, newId: String = UUID.randomUUID().toString()): Imported? = runCatching {
        val code = find(text) ?: return null
        val raw = Base64.getUrlDecoder().decode(code.removePrefix(PREFIX))
        val inf = Inflater().apply { setInput(raw) }
        val out = ByteArrayOutputStream()
        val buf = ByteArray(1024)
        while (!inf.finished()) {
            val n = inf.inflate(buf)
            if (n == 0 && (inf.needsInput() || inf.needsDictionary())) break
            out.write(buf, 0, n)
            if (out.size() > MAX_BYTES) return null
        }
        inf.end()
        val o = JSONObject(out.toString(Charsets.UTF_8.name()))
        val rule = Automation.listFromJson(JSONArray().put(o.getJSONObject("rule")).toString()).singleOrNull() ?: return null
        val names = o.optJSONObject("names")
        val byName = local.entries.groupBy { norm(it.value) }
        var unresolved = 0
        var renamed = 0
        val map = deviceRefs(rule).associateWith { id ->
            when {
                id in local -> id
                // Cihaz dışı başvurular (sahne, döngü, bölge, yardımcı, kural, kategori) yerel listede yoksa da sayılır.
                else -> names?.optString(id)?.takeIf { it.isNotEmpty() }?.let { byName[norm(it)]?.singleOrNull()?.key }
                    ?.also { renamed++ } ?: id.also { unresolved++ }
            }
        }
        Imported(remap(rule, map).copy(id = newId, enabled = false), unresolved, renamed)
    }.getOrNull()

    private fun norm(s: String) = s.trim().lowercase()

    private fun remap(a: Automation, m: Map<String, String>): Automation {
        fun id(x: String) = m[x] ?: x
        fun t(x: Trigger) = x.copy(deviceId = id(x.deviceId))
        fun c(x: Condition) = x.copy(deviceId = id(x.deviceId))
        fun s(x: AutoAction): AutoAction = x.copy(
            deviceId = id(x.deviceId),
            cond = x.cond?.let { cs -> cs.copy(items = cs.items.map(::c)) },
            then = x.then.map(::s), otherwise = x.otherwise.map(::s),
        )
        return a.copy(
            trigger = t(a.trigger), extraTriggers = a.extraTriggers.map(::t), conditions = a.conditions.map(::c),
            action = s(a.action), extraActions = a.extraActions.map(::s),
        )
    }

    /** Sıkıştırılmış kuralın açılmış hâli bu sınırı aşamaz (bozuk/kötü niyetli metin belleği doldurmasın). */
    private const val MAX_BYTES = 256 * 1024
}
