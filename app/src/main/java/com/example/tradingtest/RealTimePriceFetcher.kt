package com.example.tradingtest

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request

object RealtimePriceFetcher {
    private val client = OkHttpClient()

    fun fetch(symbol: String): Double? {
        val url = "https://api.binance.com/api/v3/ticker/price?symbol=$symbol"
        val request = Request.Builder().url(url).build()

        return try {
            val response = client.newCall(request).execute()
            val json = org.json.JSONObject(response.body?.string())
            json.getDouble("price")
        } catch (e: Exception) {
            Log.e("RealtimeFetcher", "Error fetching price: ${e.message}")
            null
        }
    }
}
