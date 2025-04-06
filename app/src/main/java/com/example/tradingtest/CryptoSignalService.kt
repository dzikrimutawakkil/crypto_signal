package com.example.tradingtest

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.*
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import org.ta4j.core.Bar

class CryptoSignalService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val intervalMillis: Long = 60_000
    private var isRunning = false
    private lateinit var signalRunnable: Runnable

    private var selectedSymbol: String = "BTCUSDT"
    private var selectedInterval: String = "5m"

    private var isSimulating = false
    private var buyPrice: Double = 0.0
    private var buyTimestamp: Long = 0L

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        selectedSymbol = intent?.getStringExtra("symbol") ?: "BTCUSDT"
        selectedInterval = intent?.getStringExtra("interval") ?: "5m"

        when (intent?.action) {
            "SIMULATED_BUY" -> {
                simulateBuy()
                return START_STICKY
            }
            "SIMULATED_SELL" -> {
                simulateSell()
                return START_STICKY
            }
        }

        if (!isRunning) {
            isRunning = true
            startForeground(1, createNotification())
            startSignalLoop()
        }

        return START_STICKY
    }

    private fun simulateBuy() {
        CoroutineScope(Dispatchers.IO).launch {
            val latestPrice = RealtimePriceFetcher.fetch(selectedSymbol)
            if (latestPrice != null) {
                buyPrice = latestPrice
                buyTimestamp = System.currentTimeMillis()
                isSimulating = true
                Log.d("Simulasi", "Beli (Realtime) di harga $buyPrice")
            }
        }
    }


    private fun simulateSell() {
        CoroutineScope(Dispatchers.IO).launch {
            if (!isSimulating) return@launch
            val currentPrice = RealtimePriceFetcher.fetch(selectedSymbol) ?: return@launch
            val changePercent = ((currentPrice - buyPrice) / buyPrice) * 100
            isSimulating = false
            Log.d("Simulasi", "Jual (Realtime) di harga $currentPrice. Hasil: ${"%.2f".format(changePercent)}%")
        }
    }


    private fun startSignalLoop() {
        signalRunnable = object : Runnable {
            @RequiresApi(Build.VERSION_CODES.O)
            override fun run() {
                CoroutineScope(Dispatchers.IO).launch {
                    val data = checkForSignal()
                    checkReminders(data)
                }
                handler.postDelayed(this, intervalMillis)
            }
        }
        handler.post(signalRunnable)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun checkForSignal(): List<Bar> {
        val data = CryptoDataFetcher.fetchData(selectedSymbol, selectedInterval)
        if (data.isNotEmpty()) {
            val signal = IndicatorAnalyzer.analyze(data)

            showSignalNotification(signal)

            var changePercent: Double? = null
            if (buyPrice != 0.0) {
                val currentPrice = data.lastOrNull()?.closePrice?.doubleValue() ?: 0.0
                changePercent = ((currentPrice - buyPrice) / buyPrice) * 100
            }

            broadcastIndicators(signal, changePercent)

            Log.d("Signal", "Signal Detected: $signal")
        }
        return data
    }


    private suspend fun checkReminders(data: List<Bar>) {
        if (!isSimulating) return

        val currentPrice = RealtimePriceFetcher.fetch(selectedSymbol) ?: return
        val changePercent = ((currentPrice - buyPrice) / buyPrice) * 100
        val elapsed = System.currentTimeMillis() - buyTimestamp

        val channelId = "reminder_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Reminder Channel",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val message = when {
            changePercent >= 1.5 -> "🎯 Target profit tercapai! +${"%.2f".format(changePercent)}%"
            changePercent <= -1.0 -> "⚠️ Stop loss! Kerugian -${"%.2f".format(changePercent)}%"
            elapsed >= 10 * 60 * 1000 -> "⏰ Sudah hold lebih dari 10 menit. Pertimbangkan jual."
            else -> null
        }

        message?.let {
            val notification = NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Simulasi Beli: $selectedSymbol")
                .setContentText(it)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()
            notificationManager.notify(2002, notification)
        }
    }


    private fun showSignalNotification(signal: SignalResult) {
        val channelId = "crypto_signal_channel"
        val notificationId = 1001

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Crypto Signals",
                NotificationManager.IMPORTANCE_HIGH
            )
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }

        val description = getSignalDescription(signal)

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Signal: ${signal.signal} (${selectedSymbol}/${selectedInterval})")
            .setContentText(description)
            .setStyle(NotificationCompat.BigTextStyle().bigText(description))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)
    }

    private fun createNotification(): Notification {
        val channelId = "crypto_service_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Crypto Service",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Crypto Signal Service Running")
            .setContentText("Monitoring $selectedSymbol ($selectedInterval)...")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun getSignalDescription(result: SignalResult): String {
        val signal = result.signal
        val explanation = result.explanation
        val action = result.action
        return "🔔 Aksi: $action - $explanation"
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        handler.removeCallbacks(signalRunnable)
        Log.d("Service", "CryptoSignalService stopped.")
    }

    private fun broadcastIndicators(signal: SignalResult, changePercent: Double? = null) {
        val intent = Intent("com.example.tradingtest.INDICATOR_UPDATE").apply {
            putExtra("rsi", signal.rsi?.toString() ?: "-")
            putExtra("action", signal.action)
            putExtra("detail", signal.explanation)
            putExtra("macd", signal.macd?.toString() ?: "-")
            putExtra("volumeAnomaly", signal.volumeAnomaly?.toString() ?: "-")
            putExtra("marketRange", signal.marketRange ?: "-")
            putExtra("simulationResult", changePercent?.let { "${"%.2f".format(it)}%" } ?: "-")
            setPackage("com.example.tradingtest")
        }
        sendBroadcast(intent) // Native Android broadcast
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
