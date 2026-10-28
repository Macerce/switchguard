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
