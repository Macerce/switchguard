package com.macerce.switchguard.core

import java.util.Calendar
import org.json.JSONArray
import org.json.JSONObject

/**
 * Sohbetteki hazır mesaj: dokununca [text] gönderilir. İsteğe bağlı olarak kendiliğinden de çalışır: her gün belli
 * saatte ([atMinute], [days]) ya da otomasyon tetikleyicisiyle ([trigger]; cihaz açılınca, güç, sıcaklık, gün batımı...).
 */
data class ChatTemplate(
    val id: String,
    val name: String,
    val text: String,
    val enabled: Boolean = true,
    /** Günün dakikası (0..1439); null = saatle çalışmaz. */
    val atMinute: Int? = null,
    /** [atMinute] hangi günler (Pazartesi = bit 0, bkz. [ActiveWindow]). */
    val days: Int = ActiveWindow.ALL_DAYS,
    /** null = tetikleyiciyle çalışmaz. */
    val trigger: Trigger? = null,
) {
    val automatic: Boolean get() = atMinute != null || trigger != null

    companion object {
        /** Otomasyon motorunda şablon kurallarının kimlik öneki; eylemi [ActionKind.RUN_SCENE] + bu önekli kimlik. */
        const val PREFIX = "tpl:"

        fun isTemplate(id: String) = id.startsWith(PREFIX)

        /** Tetikleyicili şablonlar motorun anladığı kurallara çevrilir (cihaz açılınca, güç, sıcaklık, gün doğumu...). */
        fun automations(list: List<ChatTemplate>): List<Automation> = list.mapNotNull { t ->
            val trigger = t.trigger ?: return@mapNotNull null
            Automation(PREFIX + t.id, t.name, t.enabled, trigger, AutoAction(ActionKind.RUN_SCENE, PREFIX + t.id))
        }

        /** Bugünkü çalışma anı (gün seçili değilse null). */
        fun timeToday(t: ChatTemplate, now: Long): Long? {
            val minute = t.atMinute ?: return null
            val cal = Calendar.getInstance().apply { timeInMillis = now }
            if (t.days and (1 shl ActiveWindow.dayIndex(cal)) == 0) return null
            cal.set(Calendar.HOUR_OF_DAY, minute / 60); cal.set(Calendar.MINUTE, minute % 60)
            cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

        /**
         * Saati gelmiş, bugün henüz çalışmamış şablonlar. Vakit [LATE_MS]'den fazla geçtiyse (telefon kapalıydı)
         * o günkü atlanır ama çalışmış sayılır: "19:00'da ışıkları aç" gece yarısı çalışmasın.
         * Dönen ikinci değer güncel son çalışma kayıtlarıdır.
         */
        fun due(list: List<ChatTemplate>, lastRun: Map<String, Long>, now: Long): Pair<List<ChatTemplate>, Map<String, Long>> {
            val runs = lastRun.toMutableMap()
            val out = mutableListOf<ChatTemplate>()
            for (t in list) {
                if (!t.enabled) continue
                val at = timeToday(t, now) ?: continue
                if (now < at || (runs[t.id] ?: 0) >= at) continue
                runs[t.id] = now
                if (now - at <= LATE_MS) out += t
            }
            return out to runs
        }

        /** Bir sonraki saatli çalışma (bugün ya da yarın); yoksa null. */
        fun nextAt(list: List<ChatTemplate>, lastRun: Map<String, Long>, now: Long): Long? =
            list.filter { it.enabled }.mapNotNull { t ->
                timeToday(t, now)?.takeIf { it > now || (lastRun[t.id] ?: 0) < it }
                    ?: timeToday(t, now + 86_400_000L)
            }.minOrNull()

        const val LATE_MS = 2 * 3_600_000L

        fun listToJson(list: List<ChatTemplate>): String = JSONArray().apply {
            list.forEach { t ->
                put(JSONObject().put("id", t.id).put("name", t.name).put("text", t.text).put("enabled", t.enabled).put("days", t.days).apply {
                    t.atMinute?.let { put("at", it) }
                    t.trigger?.let { tr ->
                        put("trigger", JSONObject().put("kind", tr.kind.name).put("device", tr.deviceId).put("channel", tr.channel)
                            .put("minutes", tr.minutes).put("watts", tr.watts))
                    }
                })
            }
        }.toString()

        /** Bozuk/tanınmayan kayıtlar atlanır. */
        fun listFromJson(json: String?): List<ChatTemplate>? {
            if (json == null) return null
            val arr = runCatching { JSONArray(json) }.getOrNull() ?: return emptyList()
            return (0 until arr.length()).mapNotNull { i ->
                runCatching {
                    val o = arr.getJSONObject(i)
                    ChatTemplate(
                        id = o.getString("id"),
                        name = o.optString("name"),
                        text = o.optString("text"),
                        enabled = o.optBoolean("enabled", true),
                        atMinute = if (o.has("at")) o.getInt("at") else null,
                        days = o.optInt("days", ActiveWindow.ALL_DAYS),
                        trigger = o.optJSONObject("trigger")?.let { tr ->
                            Trigger(TriggerKind.valueOf(tr.getString("kind")), tr.optString("device"), tr.optInt("channel"),
                                tr.optInt("minutes"), tr.optDouble("watts", 0.0))
                        },
                    )
                }.getOrNull()
            }
        }
    }
}
