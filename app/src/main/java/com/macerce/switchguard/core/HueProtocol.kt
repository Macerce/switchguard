package com.macerce.switchguard.core

import org.json.JSONArray
import org.json.JSONObject

/**
 * Philips Hue köprüsü yerel API'si (v1, köprünün HTTP adresinde; bulut gerekmez).
 * - `GET /api/config` (anahtarsız): `bridgeid`, `name`, `modelid`.
 * - Eşleştirme: köprünün düğmesine basıldıktan sonra 30 sn içinde `POST /api {"devicetype":".."}` →
 *   `[{"success":{"username":"<anahtar>"}}]`; basılmadıysa `[{"error":{"type":101}}]`.
 * - `GET /api/<anahtar>/lights` → `{"1":{"name","type","state":{"on","reachable"}}}`;
 *   komut `PUT /api/<anahtar>/lights/<n>/state {"on":true}`.
 * Her lamba/priz SwitchGuard'da ayrı tek kanallı cihazdır ([LocalDevice.extra] = köprü kimliği + lamba numarası).
 */
object HueProtocol {
    const val ERROR_LINK_BUTTON = 101
    const val ERROR_UNAUTHORIZED = 1

    data class Bridge(val id: String, val name: String, val model: String)
    data class Light(val num: String, val name: String, val type: String, val on: Boolean, val reachable: Boolean)

    fun bridgeKey(bridgeId: String) = "hue-bridge-" + bridgeId.lowercase()
    fun lightId(bridgeId: String, num: String) = "hue-" + bridgeId.lowercase() + "-" + num

    fun extraFor(bridgeId: String, num: String): String = JSONObject().put("b", bridgeId.lowercase()).put("l", num).toString()
    fun parseExtra(extra: String): Pair<String, String>? = runCatching {
        val o = JSONObject(extra)
        o.getString("b") to o.getString("l")
    }.getOrNull()

    fun parseConfig(json: String): Bridge? = runCatching {
        val o = JSONObject(json)
        val id = o.optString("bridgeid").takeIf { it.isNotBlank() } ?: return null
        Bridge(id.lowercase(), o.optString("name").ifEmpty { "Hue Bridge" }, o.optString("modelid"))
    }.getOrNull()

    fun pairBody(): String = JSONObject().put("devicetype", "switchguard#android").toString()

    /** Eşleştirme yanıtı: anahtar ya da hata türü (101 = düğmeye basılmadı). */
    fun parsePair(json: String): Pair<String?, Int?> = runCatching {
        val item = JSONArray(json).getJSONObject(0)
        item.optJSONObject("success")?.optString("username")?.takeIf { it.isNotBlank() }?.let { return it to null }
        null to item.optJSONObject("error")?.optInt("type")
    }.getOrDefault(null to null)

    /** Yanıt bir hata dizisi mi (ör. anahtar geçersiz: tür 1). */
    fun errorType(json: String): Int? = runCatching {
        JSONArray(json).optJSONObject(0)?.optJSONObject("error")?.optInt("type")
    }.getOrNull()

    fun parseLights(json: String): List<Light>? = runCatching {
        val o = JSONObject(json)
        o.keys().asSequence().mapNotNull { num ->
            val l = o.optJSONObject(num) ?: return@mapNotNull null
            val st = l.optJSONObject("state") ?: return@mapNotNull null
            if (!st.has("on")) return@mapNotNull null
            Light(num, l.optString("name").ifEmpty { "Hue $num" }, l.optString("type"), st.optBoolean("on"), st.optBoolean("reachable", true))
        }.sortedBy { it.num.toIntOrNull() ?: Int.MAX_VALUE }.toList()
    }.getOrNull()

    fun stateBody(on: Boolean): String = JSONObject().put("on", on).toString()

    fun isStateOk(json: String): Boolean = runCatching {
        val a = JSONArray(json)
        a.length() > 0 && (0 until a.length()).all { a.getJSONObject(it).has("success") }
    }.getOrDefault(false)
}
