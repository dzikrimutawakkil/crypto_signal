package com.example.tradingtest.setting

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.tradingtest.R

class SettingActivity : AppCompatActivity() {
    private lateinit var controller: SettingController
    private lateinit var modeSpinner: Spinner
    private lateinit var notifSpinner: Spinner
    private lateinit var customSection: LinearLayout
    private lateinit var rsiSeekBar: SeekBar
    private lateinit var rsiText: TextView
    private lateinit var saveButton: Button

    companion object {
        const val PREF_NAME = "SIGNAL_SETTING"
        const val KEY_MODE = "SIGNAL_MODE"
        const val KEY_RSI_THRESHOLD = "RSI_Threshold_Custom"
        const val KEY_NOTIFICATION_TYPE = "SIGNAL_TYPE"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        controller = SettingController.getInstance() // ✅ ini aman, sudah ada mediator-nya
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)

        modeSpinner = findViewById(R.id.spinnerMode)
        notifSpinner = findViewById(R.id.spinnerNotificationType)
        customSection = findViewById(R.id.customSection)
        rsiSeekBar = findViewById(R.id.seekBarRSI)
        rsiText = findViewById(R.id.textRSIValue)
        saveButton = findViewById(R.id.btnSave)

        val prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        val modes = listOf("Konservatif", "Agresif", "Custom")
        val notifOptions = listOf(
            "Hanya Strong Signal",
            "Tampilkan Semua Sinyal",
            "Prioritas Beli",
            "Prioritas Tambah Posisi"
        )

        val modeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, modes)
        modeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        modeSpinner.adapter = modeAdapter

        val notifAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, notifOptions)
        notifAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        notifSpinner.adapter = notifAdapter

        // Load saved preferences
        val savedMode = prefs.getString(KEY_MODE, "conservative") ?: "conservative"
        val savedNotif = prefs.getString(KEY_NOTIFICATION_TYPE, "strong_only") ?: "strong_only"

        modeSpinner.setSelection(
            when (savedMode) {
                "aggressive" -> modes.indexOf("Agresif")
                "custom" -> modes.indexOf("Custom")
                else -> modes.indexOf("Konservatif")
            }
        )

        notifSpinner.setSelection(
            when (savedNotif) {
                "Hanya Strong" -> notifOptions.indexOf("Hanya Strong Signal")
                "Prioritas Beli" -> notifOptions.indexOf("Prioritas Beli")
                "Prioritas Tambah" -> notifOptions.indexOf("Prioritas Tambah Posisi")
                else -> notifOptions.indexOf("Tampilkan Semua Sinyal")
            }
        )


        val savedThreshold = prefs.getInt(KEY_RSI_THRESHOLD, 20)
        rsiSeekBar.progress = savedThreshold
        rsiText.text = "RSI: $savedThreshold"

        modeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                customSection.visibility = if (modes[position] == "Custom") View.VISIBLE else View.GONE
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        rsiSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                rsiText.text = "RSI: $progress"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        saveButton.setOnClickListener {
            val selectedModeKey = when (modeSpinner.selectedItem.toString()) {
                "Agresif" -> "aggressive"
                "Custom" -> "custom"
                else -> "conservative"
            }
            val selectedNotifKey = when (notifSpinner.selectedItem.toString()) {
                "Hanya Strong Signal" -> "Hanya Strong"
                "Prioritas Beli" -> "Prioritas Beli"
                "Prioritas Tambah Posisi" -> "Prioritas Tambah"
                else -> "Semua"
            }
            prefs.edit()
                .putString(KEY_MODE, selectedModeKey)
                .putString(KEY_NOTIFICATION_TYPE, selectedNotifKey)
                .apply()
            if (selectedModeKey == "custom") {
                prefs.edit().putInt(KEY_RSI_THRESHOLD, rsiSeekBar.progress).apply()
            }
            Toast.makeText(this, "Pengaturan disimpan", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
