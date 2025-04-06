package com.example.tradingtest

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.ta4j.core.Bar

class MarketScannerService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private var isScanning = false
    private var selectedSymbol: String = "BTCUSDT"
    private var selectedInterval: String = "5m"

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        selectedSymbol = intent?.getStringExtra("symbol") ?: "BTCUSDT"
        selectedInterval = intent?.getStringExtra("interval") ?: "5m"
//        startForegroundWithNotification("Market Scanner Aktif", "Mendeteksi kondisi pasar...")

        isScanning = true
        startMarketCheckLoop()

        return START_STICKY
    }

    private fun startMarketCheckLoop() {
        handler.post(object : Runnable {
            override fun run() {
                CoroutineScope(Dispatchers.IO).launch {
                    val data = CryptoDataFetcher.fetchData(selectedSymbol, selectedInterval)

                    if (data.isNotEmpty()) {
                        val signal = IndicatorAnalyzer.checkMarketActiveSignal(data)

                        if (signal != null && signal.signal == "MARKET ACTIVE") {
                            showNotification("Pasar Aktif", signal.explanation)
                        }
                    }
                }
                handler.postDelayed(this, 300_000)
            }
        })
    }

    private fun startForegroundWithNotification(title: String, content: String) {
        val notification = createForegroundNotification(title, content)
        startForeground(1, notification)
    }

    private fun createForegroundNotification(title: String, content: String): Notification {
        val channelId = "market_scanner_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Market Scanner",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun showNotification(title: String, content: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Intent untuk membuka MainActivity saat notifikasi diklik
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, "market_scanner_channel")
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setContentIntent(pendingIntent) // 👈 di sini kuncinya
            .setAutoCancel(true) // notifikasi hilang saat diklik
            .build()

        manager.notify(1001, notification)
    }


    override fun onDestroy() {
        isScanning = false
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
