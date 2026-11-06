package com.macerce.switchguard.api

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.Locale

/** Dış sıcaklık: Open-Meteo (anahtarsız, ücretsiz). Engelleyici; arka planda çağrılmalı. */
object WeatherClient {
    private const val URL = "https://api.open-meteo.com/v1/forecast"

    /** Konumdaki şu anki sıcaklık (°C). */
    fun temperature(latitude: Double, longitude: Double): Double {
        val url = URL.toHttpUrl().newBuilder()
            .addQueryParameter("latitude", String.format(Locale.US, "%.3f", latitude))
            .addQueryParameter("longitude", String.format(Locale.US, "%.3f", longitude))
            .addQueryParameter("current", "temperature_2m")
            .build()
        EwelinkClient.http.newCall(Request.Builder().url(url).build()).execute().use { r ->
            if (!r.isSuccessful) throw IOException("HTTP ${r.code}")
            val json = JSONObject(r.body?.string().orEmpty())
            return json.getJSONObject("current").getDouble("temperature_2m")
        }
    }

    /** Bugünün havası: şu anki sıcaklık, WMO hava kodu, gündüz mü, bugünkü en düşük/en yüksek ve yağış olasılığı. */
    data class Today(
        val temp: Double,
        val code: Int,
        val isDay: Boolean,
        val min: Double?,
        val max: Double?,
        val rainChance: Int?,
    )

    fun today(latitude: Double, longitude: Double): Today {
        val url = URL.toHttpUrl().newBuilder()
            .addQueryParameter("latitude", String.format(Locale.US, "%.3f", latitude))
            .addQueryParameter("longitude", String.format(Locale.US, "%.3f", longitude))
            .addQueryParameter("current", "temperature_2m,weather_code,is_day")
            .addQueryParameter("daily", "temperature_2m_min,temperature_2m_max,precipitation_probability_max")
            .addQueryParameter("forecast_days", "1")
            .addQueryParameter("timezone", "auto")
            .build()
        EwelinkClient.http.newCall(Request.Builder().url(url).build()).execute().use { r ->
            if (!r.isSuccessful) throw IOException("HTTP ${r.code}")
            val json = JSONObject(r.body?.string().orEmpty())
            val cur = json.getJSONObject("current")
            val daily = json.optJSONObject("daily")
            fun first(key: String): Double? = daily?.optJSONArray(key)?.takeIf { it.length() > 0 && !it.isNull(0) }?.getDouble(0)
            return Today(
                temp = cur.getDouble("temperature_2m"),
                code = cur.optInt("weather_code", 0),
                isDay = cur.optInt("is_day", 1) == 1,
                min = first("temperature_2m_min"),
                max = first("temperature_2m_max"),
                rainChance = first("precipitation_probability_max")?.toInt(),
            )
        }
    }

    /** Arama sonucu: "Kadıköy, İstanbul, Türkiye". */
    data class Place(val name: String, val latitude: Double, val longitude: Double)

    /** Şehir/yer adına göre en çok 5 sonuç (Open-Meteo geocoding; adres değil, yerleşim düzeyinde). */
    fun search(query: String, language: String): List<Place> {
        val url = "https://geocoding-api.open-meteo.com/v1/search".toHttpUrl().newBuilder()
            .addQueryParameter("name", query.trim())
            .addQueryParameter("count", "5")
            .addQueryParameter("language", language)
            .build()
        EwelinkClient.http.newCall(Request.Builder().url(url).build()).execute().use { r ->
            if (!r.isSuccessful) throw IOException("HTTP ${r.code}")
            val results = JSONObject(r.body?.string().orEmpty()).optJSONArray("results") ?: return emptyList()
            return (0 until results.length()).map { i ->
                val o = results.getJSONObject(i)
                val label = listOf(o.optString("name"), o.optString("admin1"), o.optString("country"))
                    .filter { it.isNotBlank() }.distinct().joinToString(", ")
                Place(label, o.getDouble("latitude"), o.getDouble("longitude"))
            }
        }
    }
}
