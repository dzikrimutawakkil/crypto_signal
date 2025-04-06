package com.example.tradingtest

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.ta4j.core.Bar
import org.ta4j.core.BaseBar
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

object CryptoDataFetcher {
    fun fetchData(symbol: String, interval: String): List<Bar> {
        val url = "https://api.binance.com/api/v1/klines?symbol=$symbol&interval=$interval&limit=100"
        val request = Request.Builder().url(url).build()
        val client = OkHttpClient()

        return try {
            val response = client.newCall(request).execute()
            val json = JSONArray(response.body?.string())
            val bars = mutableListOf<Bar>()

            for (i in 0 until json.length()) {
                val candle = json.getJSONArray(i)
                val openTime = candle.getLong(0) // ✅ Pakai waktu open
                val open = candle.getDouble(1)
                val high = candle.getDouble(2)
                val low = candle.getDouble(3)
                val close = candle.getDouble(4)
                val volume = candle.getDouble(5)

                bars.add(
                    BaseBar(
                        Duration.ofMinutes(5),
                        ZonedDateTime.ofInstant(Instant.ofEpochMilli(openTime), ZoneId.systemDefault()),
                        open, high, low, close, volume
                    )
                )
            }

            bars
        } catch (e: Exception) {
            Log.e("Fetcher", "Error: ${e.message}")
            emptyList()
        }
    }
}

