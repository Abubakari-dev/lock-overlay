package com.example.nevixco

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import android.widget.TextView

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var btnActivate: MaterialButton
    private lateinit var btnDeactivate: MaterialButton
    private lateinit var switchAutoDismiss: SwitchMaterial
    private lateinit var switchPinProtection: SwitchMaterial
    private lateinit var layoutAutoDismissDuration: LinearLayout
    private lateinit var spinnerDuration: Spinner
    private lateinit var btnSetPin: MaterialButton
    private lateinit var sharedPreferences: SharedPreferences

    private val durationValues = intArrayOf(15, 30, 60, 90, 120)

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (checkOverlayPermission()) {
            Toast.makeText(this, "Permission granted!", Toast.LENGTH_SHORT).show()
            activateOverlay()
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private val overlayStatusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                OverlayService.ACTION_OVERLAY_SHOWN -> updateStatus(true)
                OverlayService.ACTION_OVERLAY_DISMISSED -> updateStatus(false)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sharedPreferences = getSharedPreferences("LockOverlayPrefs", Context.MODE_PRIVATE)

        initViews()
        setupListeners()
        loadSettings()
        registerOverlayReceiver()
    }

    private fun initViews() {
        tvStatus = findViewById(R.id.tvStatus)
        btnActivate = findViewById(R.id.btnActivate)
        btnDeactivate = findViewById(R.id.btnDeactivate)
        switchAutoDismiss = findViewById(R.id.switchAutoDismiss)
        switchPinProtection = findViewById(R.id.switchPinProtection)
        layoutAutoDismissDuration = findViewById(R.id.layoutAutoDismissDuration)
        spinnerDuration = findViewById(R.id.spinnerDuration)
        btnSetPin = findViewById(R.id.btnSetPin)

        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.auto_dismiss_durations,
            R.layout.spinner_item
        )
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spinnerDuration.adapter = adapter
    }

    private fun setupListeners() {
        btnActivate.setOnClickListener {
            if (checkOverlayPermission()) {
                activateOverlay()
            } else {
                requestOverlayPermission()
            }
        }

        btnDeactivate.setOnClickListener {
            deactivateOverlay()
        }

        switchAutoDismiss.setOnCheckedChangeListener { _, isChecked ->
            layoutAutoDismissDuration.visibility = if (isChecked) View.VISIBLE else View.GONE
            sharedPreferences.edit().putBoolean("auto_dismiss_enabled", isChecked).apply()
        }

        switchPinProtection.setOnCheckedChangeListener { _, isChecked ->
            btnSetPin.visibility = if (isChecked) View.VISIBLE else View.GONE
            sharedPreferences.edit().putBoolean("pin_protection_enabled", isChecked).apply()

            if (isChecked && !hasPinSet()) {
                Toast.makeText(this, "Please set a PIN first", Toast.LENGTH_SHORT).show()
                showSetPinDialog()
            }
        }

        spinnerDuration.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                sharedPreferences.edit().putInt("auto_dismiss_duration", durationValues[position]).apply()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        btnSetPin.setOnClickListener {
            showSetPinDialog()
        }
    }

    private fun loadSettings() {
        val autoDismissEnabled = sharedPreferences.getBoolean("auto_dismiss_enabled", false)
        val pinProtectionEnabled = sharedPreferences.getBoolean("pin_protection_enabled", false)
        val duration = sharedPreferences.getInt("auto_dismiss_duration", 30)

        switchAutoDismiss.isChecked = autoDismissEnabled
        switchPinProtection.isChecked = pinProtectionEnabled
        layoutAutoDismissDuration.visibility = if (autoDismissEnabled) View.VISIBLE else View.GONE
        btnSetPin.visibility = if (pinProtectionEnabled) View.VISIBLE else View.GONE

        val position = durationValues.indexOf(duration)
        if (position >= 0) {
            spinnerDuration.setSelection(position)
        }

        if (hasPinSet()) {
            btnSetPin.text = "Change PIN"
        }
    }

    private fun updateUIBasedOnStatus(isActive: Boolean) {
        // All buttons remain visible always
        // This method can be extended for other UI changes based on status
    }

    private fun registerOverlayReceiver() {
        val filter = IntentFilter().apply {
            addAction(OverlayService.ACTION_OVERLAY_SHOWN)
            addAction(OverlayService.ACTION_OVERLAY_DISMISSED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(overlayStatusReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(overlayStatusReceiver, filter)
        }
    }

    private fun checkOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            AlertDialog.Builder(this)
                .setTitle("Permission Required")
                .setMessage("This app needs overlay permission to display the lock screen")
                .setPositiveButton("Grant") { _, _ ->
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                    overlayPermissionLauncher.launch(intent)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun activateOverlay() {
        val serviceIntent = Intent(this, OverlayService::class.java).apply {
            action = OverlayService.ACTION_START_OVERLAY
            putExtra("auto_dismiss_enabled", sharedPreferences.getBoolean("auto_dismiss_enabled", false))
            putExtra("auto_dismiss_duration", sharedPreferences.getInt("auto_dismiss_duration", 30))
            putExtra("pin_protection_enabled", sharedPreferences.getBoolean("pin_protection_enabled", false))
            putExtra("pin_code", sharedPreferences.getString("pin_code", ""))
        }
        ContextCompat.startForegroundService(this, serviceIntent)
        Toast.makeText(this, "Overlay activated", Toast.LENGTH_SHORT).show()
    }

    private fun hasPinSet(): Boolean {
        val pin = sharedPreferences.getString("pin_code", "")
        return !pin.isNullOrEmpty()
    }

    private fun showSetPinDialog() {
        val inputLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 20, 50, 20)
        }

        val inputPin = EditText(this).apply {
            hint = "Enter PIN (4 digits)"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            maxLines = 1
        }

        val inputConfirm = EditText(this).apply {
            hint = "Confirm PIN"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            maxLines = 1
        }

        // Show/Hide PIN toggle
        val showPinCheckbox = android.widget.CheckBox(this).apply {
            text = "Show PIN"
            setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    inputPin.inputType = InputType.TYPE_CLASS_NUMBER
                    inputConfirm.inputType = InputType.TYPE_CLASS_NUMBER
                } else {
                    inputPin.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
                    inputConfirm.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
                }
                inputPin.setSelection(inputPin.text.length)
                inputConfirm.setSelection(inputConfirm.text.length)
            }
        }

        inputLayout.addView(inputPin)
        inputLayout.addView(inputConfirm)
        inputLayout.addView(showPinCheckbox)

        AlertDialog.Builder(this)
            .setTitle(if (hasPinSet()) "Change PIN" else "Set PIN")
            .setView(inputLayout)
            .setPositiveButton("OK") { _, _ ->
                val pin = inputPin.text.toString()
                val confirm = inputConfirm.text.toString()

                when {
                    pin.isEmpty() || confirm.isEmpty() -> {
                        Toast.makeText(this, "PIN cannot be empty", Toast.LENGTH_SHORT).show()
                    }
                    pin.length != 4 -> {
                        Toast.makeText(this, "PIN must be exactly 4 digits", Toast.LENGTH_SHORT).show()
                    }
                    !pin.all { it.isDigit() } -> {
                        Toast.makeText(this, "PIN must contain only numbers", Toast.LENGTH_SHORT).show()
                    }
                    pin != confirm -> {
                        Toast.makeText(this, "PINs do not match", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        sharedPreferences.edit().putString("pin_code", pin).apply()
                        Toast.makeText(this, "PIN set successfully", Toast.LENGTH_SHORT).show()
                        btnSetPin.text = "Change PIN"
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deactivateOverlay() {
        val serviceIntent = Intent(this, OverlayService::class.java).apply {
            action = OverlayService.ACTION_STOP_OVERLAY
        }
        startService(serviceIntent)
        updateStatus(false)
        Toast.makeText(this, "Overlay deactivated", Toast.LENGTH_SHORT).show()
    }

    private fun updateStatus(isActive: Boolean) {
        runOnUiThread {
            if (isActive) {
                tvStatus.text = "● OVERLAY ACTIVE"
                tvStatus.setTextColor(ContextCompat.getColor(this, R.color.status_active))
            } else {
                tvStatus.text = "● OVERLAY INACTIVE"
                tvStatus.setTextColor(ContextCompat.getColor(this, R.color.status_inactive))
            }
            updateUIBasedOnStatus(isActive)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(overlayStatusReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
