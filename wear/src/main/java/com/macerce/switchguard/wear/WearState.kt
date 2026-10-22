package com.macerce.switchguard.wear

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject

/** Telefondan gelen durum: izleme özeti, çalan alarmın satırları ve cihazlar. */
data class PhoneStatus(
    val kind: String = "",
    val state: String = "",
    val alarm: List<String> = emptyList(),
    /** Çalan alarmın başlangıcı: yeni alarm mı, aynı alarma eklenen satır mı ayırt etmek için. */
    val alarmId: Long = 0,
    val devices: List<WearDevice> = emptyList(),
    val away: Boolean = false,
    val vacation: Boolean = false,
    /** Sahneler: kimlik → ad. */
    val scenes: List<Pair<String, String>> = emptyList(),
) {
    val alarming get() = alarm.isNotEmpty()

    companion object {
        fun parse(json: String?): PhoneStatus? = runCatching {
            val o = JSONObject(json ?: return null)
            val alarm = o.optJSONArray("alarm")
            val devices = o.optJSONArray("devices")
            PhoneStatus(
                kind = o.optString("kind"),
                state = o.optString("state"),
                alarm = (0 until (alarm?.length() ?: 0)).map { alarm!!.getString(it) },
                alarmId = o.optLong("alarmId"),
                away = o.optBoolean("away"),
                vacation = o.optBoolean("vacation"),
                scenes = o.optJSONArray("scenes")?.let { a -> (0 until a.length()).map { a.getJSONObject(it).let { s -> s.getString("id") to s.optString("name") } } }.orEmpty(),
                devices = (0 until (devices?.length() ?: 0)).map { i ->
                    val d = devices!!.getJSONObject(i)
                    val sw = d.optJSONObject("sw")
                    WearDevice(
                        id = d.getString("id"),
                        name = d.optString("name"),
                        online = d.optBoolean("online"),
                        switches = sw?.keys()?.asSequence()?.associate { it.toInt() to sw.getBoolean(it) }?.toSortedMap() ?: emptyMap(),
                        watts = if (d.has("w")) d.getDouble("w") else null,
                    )
                },
            )
        }.getOrNull()
    }
}

data class WearDevice(val id: String, val name: String, val online: Boolean, val switches: Map<Int, Boolean>, val watts: Double?)

/** Saatteki son durum; uygulama kapalıyken de son bilinen hal gösterilsin diye diske yazılır. */
object WearState {
    private val _status = MutableStateFlow(PhoneStatus())
    val status: StateFlow<PhoneStatus> = _status

    /** Durum hiç gelmedi mi (telefon bağlı değil ya da uygulama yok). */
    @Volatile var received = false
        private set

    fun load(context: Context) {
        if (received) return
        PhoneStatus.parse(prefs(context).getString("json", null))?.let { _status.value = it; received = true }
    }

    fun update(context: Context, json: String): PhoneStatus? {
        val s = PhoneStatus.parse(json) ?: return null
        prefs(context).edit().putString("json", json).apply()
        _status.value = s
        received = true
        return s
    }

    private fun prefs(context: Context) = context.getSharedPreferences("wear_state", Context.MODE_PRIVATE)
}
