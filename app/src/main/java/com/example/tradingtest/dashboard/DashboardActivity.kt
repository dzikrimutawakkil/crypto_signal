package com.example.tradingtest.dashboard

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.tradingtest.AppNavigationMediator
import com.example.tradingtest.IAppNavigationMediator
import com.example.tradingtest.R

class DashboardActivity : AppCompatActivity() {
    private val signalReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val signal = intent?.getStringExtra("signal") ?: return
            Log.d("Broadcast", "Signal diterima: $signal")
            // Update UI sesuai kebutuhan
        }
    }

    private lateinit var spinnerKoin: Spinner
    private lateinit var spinnerInterval: Spinner
    private lateinit var btnSimulasiBeli: Button
    private lateinit var btnHentikanSimulasi: Button
    private lateinit var switchMarketScanner: Switch
    private lateinit var switchSignalService: Switch
    private lateinit var controller: DashBoardController

    private val symbols = listOf("BTCUSDT", "ETHUSDT", "BNBUSDT", "ADAUSDT", "XRPUSDT", "LTCUSDT", "SOLUSDT", "TKOUSDT")
    private val intervals = listOf("1m", "3m", "5m", "15m")

    private lateinit var indicatorReceiver: BroadcastReceiver

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        controller = DashBoardController.getInstance()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }

        // Inisialisasi view dari XML
        spinnerKoin = findViewById(R.id.spinnerKoin)
        spinnerInterval = findViewById(R.id.spinnerInterval)
        btnSimulasiBeli = findViewById(R.id.btnSimulasiBeli)
        btnHentikanSimulasi = findViewById(R.id.btnHentikanSimulasi)
        switchMarketScanner = findViewById(R.id.switchMarketScanner)
        switchSignalService = findViewById(R.id.switchSignalService)

        // Setup spinner
        spinnerKoin.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, symbols)
        spinnerInterval.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, intervals)

        spinnerKoin.setSelection(symbols.indexOf("BTCUSDT"))
        spinnerInterval.setSelection(intervals.indexOf("5m"))

        // Set status awal switch
        switchMarketScanner.isChecked = isServiceRunning(MarketScannerService::class.java)
        switchSignalService.isChecked = isServiceRunning(CryptoSignalService::class.java)

        val btnSettings = findViewById<ImageButton>(R.id.btnSettings)
        btnSettings.setOnClickListener {
            controller.goToSetting(this)
        }

        // Market Scanner Switch
        switchMarketScanner.setOnCheckedChangeListener { _, isChecked ->
            val intent = Intent(this, MarketScannerService::class.java).apply {
                putExtra("symbol", spinnerKoin.selectedItem.toString())
                putExtra("interval", spinnerInterval.selectedItem.toString())
            }

            if (isChecked) startService(intent)
            else stopService(intent)
        }

        // Signal Service Switch
        switchSignalService.setOnCheckedChangeListener { _, isChecked ->
            val intent = Intent(this, CryptoSignalService::class.java).apply {
                putExtra("symbol", spinnerKoin.selectedItem.toString())
                putExtra("interval", spinnerInterval.selectedItem.toString())
            }

            if (isChecked) startService(intent)
            else stopService(intent)
        }

        // Simulasi Beli
        btnSimulasiBeli.setOnClickListener {
            val intent = Intent(this, CryptoSignalService::class.java).apply {
                action = "SIMULATED_BUY"
            }
            startService(intent)
        }

        // Hentikan Simulasi
        btnHentikanSimulasi.setOnClickListener {
            val intent = Intent(this, CryptoSignalService::class.java).apply {
                action = "SIMULATED_SELL"
            }
            startService(intent)
        }

        // Broadcast receiver untuk indikator
        indicatorReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val rsi = intent?.getStringExtra("rsi") ?: "-"
                val macd = intent?.getStringExtra("macd") ?: "-"
                val volume = intent?.getStringExtra("volumeAnomaly") ?: "-"
                val market = intent?.getStringExtra("marketRange") ?: "-"
                val result = intent?.getStringExtra("simulationResult") ?: "-"
                val action = intent?.getStringExtra("action") ?: "-"
                val description = intent?.getStringExtra("detail") ?: "-"

                findViewById<TextView>(R.id.tvRSI).text = "📈 RSI: $rsi"
                findViewById<TextView>(R.id.tvMACD).text = "📉 MACD: $macd"
                findViewById<TextView>(R.id.tvVolumeAnomaly).text = "📦 Volume Anomali: $volume"
                findViewById<TextView>(R.id.tvMarketRange).text = "🌐 Market Growth: $market%"
                findViewById<TextView>(R.id.tvSimulationResult).text = "💰 Simulasi Profit: $result"
                findViewById<TextView>(R.id.action).text = "Rekomendasi : $action"
                findViewById<TextView>(R.id.description).text = "$description"
            }
        }

        val indicatorFilter = IntentFilter("com.example.tradingtest.INDICATOR_UPDATE")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(indicatorReceiver, indicatorFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(indicatorReceiver, indicatorFilter)
        }
        loadLastSignalIfNeeded()
    }

    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return manager.getRunningServices(Int.MAX_VALUE)
            .any { it.service.className == serviceClass.name }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notifikasi diizinkan", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Izin notifikasi ditolak", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onResume() {
        super.onResume()
        loadLastSignalIfNeeded()
        switchMarketScanner.isChecked = isServiceRunning(MarketScannerService::class.java)
        switchSignalService.isChecked = isServiceRunning(CryptoSignalService::class.java)
        val filter = IntentFilter("com.example.tradingtest.SIGNAL_UPDATE")
        registerReceiver(signalReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(signalReceiver)
    }

    private fun loadLastSignalIfNeeded() {
        val prefs = getSharedPreferences("INDICATOR_DATA", Context.MODE_PRIVATE)

        val action = prefs.getString("action", null) ?: return
        val detail = prefs.getString("detail", "-")
        val rsi = prefs.getString("rsi", "-")
        val macd = prefs.getString("macd", "-")
        val volumeAnomaly = prefs.getString("volumeAnomaly", "-")
        val marketRange = prefs.getString("marketRange", "-")
        val simulationResult = prefs.getString("simulationResult", "-")

        // Update UI di sini dengan nilai-nilai di atas
        findViewById<TextView>(R.id.tvRSI).text = "📈 RSI: $rsi"
        findViewById<TextView>(R.id.tvMACD).text = "📉 MACD: $macd"
        findViewById<TextView>(R.id.tvVolumeAnomaly).text = "📦 Volume Anomali: $volumeAnomaly"
        findViewById<TextView>(R.id.tvMarketRange).text = "🌐 Market Growth: $marketRange%"
        findViewById<TextView>(R.id.tvSimulationResult).text = "💰 Simulasi Profit: $simulationResult"
        findViewById<TextView>(R.id.action).text = "Rekomendasi : $action"
        findViewById<TextView>(R.id.description).text = "$detail"
    }

}
