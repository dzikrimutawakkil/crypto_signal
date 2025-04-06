package com.example.tradingtest

import android.util.Log
import org.ta4j.core.Bar
import org.ta4j.core.BaseBarSeries
import org.ta4j.core.indicators.MACDIndicator
import org.ta4j.core.indicators.RSIIndicator
import org.ta4j.core.indicators.helpers.ClosePriceIndicator

data class SignalResult(
    val signal: String,
    val explanation: String,
    val action: String,
    val rsi: Double? = null,
    val macd: Double? = null,
    val volumeAnomaly: Double? = null,
    val marketRange: String? = null
)

object IndicatorAnalyzer {

    private const val VOLUME_ANOMALY_STRONG = 1.05
    private const val VOLUME_ANOMALY_WEAK = 0.9
    private const val VOLUME_MIN_FOR_MOMENTUM = 0.7

    fun analyze(bars: List<Bar>): SignalResult {
        val series = BaseBarSeries("simple_series", bars)
        val closePrice = ClosePriceIndicator(series)
        val rsi = RSIIndicator(closePrice, 7)
        val macd = MACDIndicator(closePrice, 12, 26)

        val lastIndex = series.endIndex
        if (lastIndex < 26) {
            return SignalResult(
                signal = "NEUTRAL",
                explanation = "Data belum cukup untuk analisis (butuh setidaknya 26 bar).",
                action = "HOLD",
                rsi = null,
                macd = null,
                volumeAnomaly = null,
                marketRange = null
            )
        }

        val marketSignal = checkMarketActiveSignal(bars)

        val rsiNow = rsi.getValue(lastIndex).doubleValue()
        val macdNow = macd.getValue(lastIndex).doubleValue()

        val volumeAnomaly = if (bars.size >= 6) {
            val recentVolumes = bars.subList(lastIndex - 5, lastIndex)
            val avgVolume = recentVolumes.map { it.volume.doubleValue() }.average()
            if (avgVolume == 0.0) 0.0 else bars[lastIndex].volume.doubleValue() / avgVolume
        } else 1.0

        if (marketSignal.signal == "SIDEWAYS") {
            return marketSignal.copy(
                rsi = rsiNow,
                macd = macdNow,
                volumeAnomaly = volumeAnomaly,
                marketRange = "%.2f".format(calculateRange(bars))
            )
        }

        Log.d("Signal", "RSI = $rsiNow")
        Log.d("Signal", "MACD = $macdNow")
        Log.d("Signal", "Volume Anomaly = $volumeAnomaly")

        val isBuyMomentum = rsiNow > 70 && macdNow > 0 && volumeAnomaly >= VOLUME_MIN_FOR_MOMENTUM
        val isSellMomentum = rsiNow < 30 && macdNow < 0 && volumeAnomaly >= VOLUME_MIN_FOR_MOMENTUM

        val baseSignal = when {
            macdNow > 0 && rsiNow in 55.0..70.0 && volumeAnomaly > VOLUME_ANOMALY_STRONG ->
                "BUY STRONG"

            macdNow < 0 && rsiNow in 30.0..45.0 && volumeAnomaly > VOLUME_ANOMALY_STRONG ->
                "SELL STRONG"

            macdNow > 0 && rsiNow > 50.0 && volumeAnomaly > VOLUME_ANOMALY_WEAK ->
                "BUY WEAK"

            macdNow < 0 && rsiNow < 50.0 && volumeAnomaly > VOLUME_ANOMALY_WEAK ->
                "SELL WEAK"

            macdNow > 0 && rsiNow in 45.0..55.0 && volumeAnomaly < VOLUME_ANOMALY_WEAK ->
                "BUY EARLY"

            else -> "NEUTRAL"
        }

        val finalSignal = when {
            baseSignal.startsWith("BUY") && isBuyMomentum -> "$baseSignal + MOMENTUM"
            baseSignal.startsWith("SELL") && isSellMomentum -> "$baseSignal + MOMENTUM"
            else -> baseSignal
        }

        val explanation = when (finalSignal) {
            "BUY STRONG + MOMENTUM" ->
                "Sinyal beli kuat diperkuat oleh momentum: RSI tinggi ($rsiNow), MACD positif ($macdNow), dan volume tinggi ($volumeAnomaly)."

            "SELL STRONG + MOMENTUM" ->
                "Sinyal jual kuat diperkuat oleh momentum: RSI rendah ($rsiNow), MACD negatif ($macdNow), dan volume tinggi ($volumeAnomaly)."

            "BUY WEAK + MOMENTUM", "BUY WEAK" ->
                "Potensi beli: MACD naik, RSI menguat ($rsiNow), volume lumayan ($volumeAnomaly)."

            "SELL WEAK + MOMENTUM", "SELL WEAK" ->
                "Potensi jual: MACD turun, RSI melemah ($rsiNow), volume lumayan ($volumeAnomaly)."

            "BUY EARLY" ->
                "Awal potensi tren naik: MACD positif, RSI moderat ($rsiNow), volume belum meningkat ($volumeAnomaly)."

            "BUY STRONG" ->
                "Sinyal beli kuat: MACD positif, RSI sehat ($rsiNow), volume tinggi ($volumeAnomaly)."

            "SELL STRONG" ->
                "Sinyal jual kuat: MACD negatif, RSI rendah ($rsiNow), volume tinggi ($volumeAnomaly)."

            "NEUTRAL" ->
                "Belum ada sinyal jelas. Tunggu momentum atau indikator tambahan."

            else -> "Sinyal terdeteksi: $finalSignal"
        }

        val action = when {
            finalSignal.startsWith("BUY") && !finalSignal.contains("MOMENTUM") -> "BUY"
            finalSignal.startsWith("BUY") && finalSignal.contains("MOMENTUM") -> "HOLD"
            finalSignal.startsWith("SELL") -> "SELL"
            else -> "HOLD"
        }

        Log.d("Signal", "Final Signal: $finalSignal")
        return SignalResult(
            signal = finalSignal,
            explanation = explanation,
            action = action,
            rsi = rsiNow,
            macd = macdNow,
            volumeAnomaly = volumeAnomaly,
            marketRange = "%.2f".format(calculateRange(bars))
        )
    }

    fun checkMarketActiveSignal(bars: List<Bar>): SignalResult {
        if (bars.size < 10) {
            return SignalResult(
                signal = "SIDEWAYS",
                explanation = "Data tidak cukup untuk analisis.",
                action = "HOLD",
                rsi = null,
                macd = null,
                volumeAnomaly = null,
                marketRange = "%.2f".format(calculateRange(bars))
            )
        }

        val highs = bars.takeLast(10).map { it.highPrice.doubleValue() }
        val lows = bars.takeLast(10).map { it.lowPrice.doubleValue() }

        val maxHigh = highs.maxOrNull() ?: return SignalResult(
            signal = "SIDEWAYS",
            explanation = "Gagal membaca harga tertinggi. Asumsikan pasar sideways.",
            action = "HOLD",
            rsi = null,
            macd = null,
            volumeAnomaly = null,
            marketRange = "%.2f".format(calculateRange(bars))
        )
        val minLow = lows.minOrNull() ?: return SignalResult(
            signal = "SIDEWAYS",
            explanation = "Gagal membaca harga terendah. Asumsikan pasar sideways.",
            action = "HOLD",
            rsi = null,
            macd = null,
            volumeAnomaly = null,
            marketRange = "%.2f".format(calculateRange(bars))
        )

        val rangePercent = ((maxHigh - minLow) / minLow) * 100

        return if (rangePercent < 0.3) {
            Log.d("Signal", "Kondisi Pasar : SideWays (range: %.2f%%)".format(rangePercent))
            SignalResult(
                signal = "SIDEWAYS",
                explanation = "Pasar sedang datar (range: %.2f%%). Belum ada sinyal signifikan.".format(rangePercent),
                action = "HOLD",
                rsi = null,
                macd = null,
                volumeAnomaly = null,
                marketRange = "%.2f".format(rangePercent)
            )
        } else {
            Log.d("Signal", "Kondisi Pasar : Active (range: %.2f%%)".format(rangePercent))
            SignalResult(
                signal = "MARKET ACTIVE",
                explanation = "Pasar mulai aktif (range: %.2f%%) dan tidak sideways.".format(rangePercent),
                action = "WATCH",
                rsi = null,
                macd = null,
                volumeAnomaly = null,
                marketRange = "%.2f".format(rangePercent)
            )
        }
    }

    private fun calculateRange(bars: List<Bar>): Double {
        val highs = bars.takeLast(10).map { it.highPrice.doubleValue() }
        val lows = bars.takeLast(10).map { it.lowPrice.doubleValue() }

        val maxHigh = highs.maxOrNull() ?: return 0.0
        val minLow = lows.minOrNull() ?: return 0.0

        return ((maxHigh - minLow) / minLow) * 100
    }
}
