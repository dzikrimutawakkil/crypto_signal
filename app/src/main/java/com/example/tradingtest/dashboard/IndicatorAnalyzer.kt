package com.example.tradingtest.dashboard

import android.content.Context
import android.preference.PreferenceManager
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
    val marketRange: String? = null,
    val shouldNotify: Boolean = true
)

data class VolumeThresholds(
    val strong: Double,
    val weak: Double,
    val momentum: Double
)

data class RSIThresholds(
    val strongBuyMin: Double,
    val strongBuyMax: Double,
    val strongSellMin: Double,
    val strongSellMax: Double,
    val buyMomentum: Double,
    val sellMomentum: Double
)

data class MACDThresholds(
    val positive: Double = 0.0,
    val negative: Double = 0.0
)

private fun getVolumeThresholds(context: Context): VolumeThresholds {
    val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    val mode = prefs.getString("signal_mode", "conservative") ?: "conservative"

    return when (mode) {
        "conservative" -> VolumeThresholds(1.10, 0.95, 0.8)
        "aggressive" -> VolumeThresholds(0.95, 0.75, 0.6) // ✅ update di sini
        "custom" -> {
            val strong = prefs.getString("custom_volume_strong", "1.05")?.toDoubleOrNull() ?: 1.05
            val weak = prefs.getString("custom_volume_weak", "0.9")?.toDoubleOrNull() ?: 0.9
            val momentum = prefs.getString("custom_volume_momentum", "0.7")?.toDoubleOrNull() ?: 0.7
            VolumeThresholds(strong, weak, momentum)
        }
        else -> VolumeThresholds(1.05, 0.9, 0.7)
    }
}

private fun getRSIThresholds(context: Context): RSIThresholds {
    val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    return when (prefs.getString("signal_mode", "conservative")) {
        "conservative" -> RSIThresholds(55.0, 70.0, 30.0, 45.0, 70.0, 30.0)
        "aggressive" -> RSIThresholds(50.0, 65.0, 35.0, 50.0, 65.0, 35.0)
        "custom" -> {
            val buyMin = prefs.getString("custom_rsi_buy_min", "55.0")?.toDoubleOrNull() ?: 55.0
            val buyMax = prefs.getString("custom_rsi_buy_max", "70.0")?.toDoubleOrNull() ?: 70.0
            val sellMin = prefs.getString("custom_rsi_sell_min", "30.0")?.toDoubleOrNull() ?: 30.0
            val sellMax = prefs.getString("custom_rsi_sell_max", "45.0")?.toDoubleOrNull() ?: 45.0
            val buyMom = prefs.getString("custom_rsi_buy_momentum", "70.0")?.toDoubleOrNull() ?: 70.0
            val sellMom = prefs.getString("custom_rsi_sell_momentum", "30.0")?.toDoubleOrNull() ?: 30.0
            RSIThresholds(buyMin, buyMax, sellMin, sellMax, buyMom, sellMom)
        }
        else -> RSIThresholds(55.0, 70.0, 30.0, 45.0, 70.0, 30.0)
    }
}

private fun getMACDThresholds(context: Context): MACDThresholds {
    return MACDThresholds() // Placeholder if you want to expand this later
}

object IndicatorAnalyzer {

    fun analyze(context: Context, bars: List<Bar>): SignalResult {
        val series = BaseBarSeries("simple_series", bars)
        val closePrice = ClosePriceIndicator(series)
        val rsi = RSIIndicator(closePrice, 7)
        val macd = MACDIndicator(closePrice, 12, 26)
        val lastIndex = series.endIndex

        if (lastIndex < 26) {
            return SignalResult(
                signal = "NEUTRAL",
                explanation = "Data belum cukup untuk analisis.",
                action = "HOLD"
            )
        }

        val rsiNow = rsi.getValue(lastIndex).doubleValue()
        val macdNow = macd.getValue(lastIndex).doubleValue()
        val volumeAnomaly = calculateVolumeAnomaly(bars, lastIndex)
        val marketSignal = checkMarketActiveSignal(bars)

        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val signalMode = prefs.getString("signal_mode", "conservative") ?: "conservative"
        val signalType = prefs.getString("signal_type", "strong_only") ?: "strong_only"
        val volumeThresholds = getVolumeThresholds(context)
        val rsiThresholds = getRSIThresholds(context)

        if (marketSignal.signal == "SIDEWAYS") {
            return marketSignal.copy(
                rsi = rsiNow,
                macd = macdNow,
                volumeAnomaly = volumeAnomaly,
                marketRange = "%.2f".format(calculateRange(bars)),
                shouldNotify = false
            )
        }

        val isBuyMomentum = rsiNow > rsiThresholds.buyMomentum && macdNow > 0 && volumeAnomaly >= volumeThresholds.momentum
        val isSellMomentum = rsiNow < rsiThresholds.sellMomentum && macdNow < 0 && volumeAnomaly >= volumeThresholds.momentum

        val baseSignal = when {
            signalMode == "aggressive" && macdNow > 0 && rsiNow in rsiThresholds.strongBuyMin..rsiThresholds.strongBuyMax && volumeAnomaly >= volumeThresholds.strong -> "BUY STRONG"
            signalMode == "aggressive" && macdNow < 0 && rsiNow in rsiThresholds.strongSellMin..rsiThresholds.strongSellMax && volumeAnomaly >= volumeThresholds.strong -> "SELL STRONG"
            macdNow > 0 && rsiNow in rsiThresholds.strongBuyMin..rsiThresholds.strongBuyMax && volumeAnomaly >= volumeThresholds.strong -> "BUY STRONG"
            macdNow < 0 && rsiNow in rsiThresholds.strongSellMin..rsiThresholds.strongSellMax && volumeAnomaly >= volumeThresholds.strong -> "SELL STRONG"
            macdNow > 0 && rsiNow > rsiThresholds.strongBuyMin && volumeAnomaly >= volumeThresholds.weak -> "BUY WEAK"
            macdNow < 0 && rsiNow < rsiThresholds.strongSellMax && volumeAnomaly >= volumeThresholds.weak -> "SELL WEAK"
            macdNow > 0 && rsiNow in rsiThresholds.strongSellMax..rsiThresholds.strongBuyMin && volumeAnomaly < volumeThresholds.weak -> "BUY EARLY"
            else -> "NEUTRAL"
        }

        val finalSignal = when {
            baseSignal.startsWith("BUY") && isBuyMomentum -> "$baseSignal + MOMENTUM"
            baseSignal.startsWith("SELL") && isSellMomentum -> "$baseSignal + MOMENTUM"
            else -> baseSignal
        }

        val shouldNotify = shouldShowSignal(finalSignal, signalMode, signalType)

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
            finalSignal.startsWith("BUY") -> "HOLD"
            finalSignal.startsWith("SELL") -> "SELL"
            else -> "HOLD"
        }

        return SignalResult(
            signal = finalSignal,
            explanation = explanation,
            action = action,
            rsi = rsiNow,
            macd = macdNow,
            volumeAnomaly = volumeAnomaly,
            marketRange = "%.2f".format(calculateRange(bars)),
            shouldNotify = shouldNotify
        )
    }

    private fun calculateVolumeAnomaly(bars: List<Bar>, lastIndex: Int): Double {
        return if (bars.size >= 6) {
            val recentVolumes = bars.subList(lastIndex - 5, lastIndex)
            val avgVolume = recentVolumes.map { it.volume.doubleValue() }.average()
            if (avgVolume == 0.0) 0.0 else bars[lastIndex].volume.doubleValue() / avgVolume
        } else 1.0
    }

    private fun shouldShowSignal(signal: String, mode: String, type: String): Boolean {
        if (mode == "conservative" && !signal.contains("STRONG") && !signal.contains("MOMENTUM")) return false
        return when (type) {
            "strong_only" -> signal.contains("STRONG")
            "buy_priority" -> signal.contains("BUY")
            "add_priority" -> signal.contains("BUY") || signal.contains("EARLY")
            "all" -> true
            else -> true
        }
    }

    fun checkMarketActiveSignal(bars: List<Bar>): SignalResult {
        if (bars.size < 10) return SignalResult("SIDEWAYS", "Data tidak cukup.", "HOLD")

        val highs = bars.takeLast(10).map { it.highPrice.doubleValue() }
        val lows = bars.takeLast(10).map { it.lowPrice.doubleValue() }
        val maxHigh = highs.maxOrNull() ?: return SignalResult("SIDEWAYS", "Gagal baca high", "HOLD")
        val minLow = lows.minOrNull() ?: return SignalResult("SIDEWAYS", "Gagal baca low", "HOLD")

        val rangePercent = ((maxHigh - minLow) / minLow) * 100
        return if (rangePercent < 0.3)
            SignalResult("SIDEWAYS", "Pasar datar (range: %.2f%%)".format(rangePercent), "HOLD")
        else
            SignalResult("MARKET ACTIVE", "Pasar aktif (range: %.2f%%)".format(rangePercent), "WATCH")
    }

    private fun calculateRange(bars: List<Bar>): Double {
        val highs = bars.takeLast(10).map { it.highPrice.doubleValue() }
        val lows = bars.takeLast(10).map { it.lowPrice.doubleValue() }
        val maxHigh = highs.maxOrNull() ?: return 0.0
        val minLow = lows.minOrNull() ?: return 0.0
        return ((maxHigh - minLow) / minLow) * 100
    }
}
