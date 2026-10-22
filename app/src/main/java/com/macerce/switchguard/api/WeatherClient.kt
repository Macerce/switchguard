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
}
