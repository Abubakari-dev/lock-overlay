package com.lockoverlay

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

    // Bonus features
    private lateinit var switchAutoDismiss: SwitchMaterial
    private lateinit var switchPinProtection: SwitchMaterial
    private lateinit var layoutAutoDismissDuration: LinearLayout
    private lateinit var spinnerDuration: Spinner
    private lateinit var btnSetPin: MaterialButton
    private lateinit var sharedPreferences: SharedPreferences

    private val durationValues = intArrayOf(15, 30, 60, 90, 120)

    // Activity result launcher for overlay permission
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
                OverlayService.ACTION_OVERLAY_SHOWN -> {
                    updateStatus(true)
                }
                OverlayService.ACTION_OVERLAY_DISMISSED -> {
                    updateStatus(false)
                }
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

        // Bonus features views
        switchAutoDismiss = findViewById(R.id.switchAutoDismiss)
        switchPinProtection = findViewById(R.id.switchPinProtection)
        layoutAutoDismissDuration = findViewById(R.id.layoutAutoDismissDuration)
        spinnerDuration = findViewById(R.id.spinnerDuration)
        btnSetPin = findViewById(R.id.btnSetPin)

        // Setup spinner
        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.auto_dismiss_durations,
            android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
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

        // Auto-dismiss switch
        switchAutoDismiss.setOnCheckedChangeListener { _, isChecked ->
            layoutAutoDismissDuration.visibility = if (isChecked) View.VISIBLE else View.GONE
            sharedPreferences.edit().putBoolean("auto_dismiss_enabled", isChecked).apply()
        }

        // PIN protection switch
        switchPinProtection.setOnCheckedChangeListener { _, isChecked ->
            btnSetPin.visibility = if (isChecked) View.VISIBLE else View.GONE
            sharedPreferences.edit().putBoolean("pin_protection_enabled", isChecked).apply()

            if (isChecked && !hasPinSet()) {
                Toast.makeText(this, R.string.pin_required, Toast.LENGTH_SHORT).show()
                showSetPinDialog()
            }
        }

        // Duration spinner
        spinnerDuration.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                sharedPreferences.edit().putInt("auto_dismiss_duration", durationValues[position]).apply()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Set PIN button
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

        // Set spinner selection
        val position = durationValues.indexOf(duration)
        if (position >= 0) {
            spinnerDuration.setSelection(position)
        }

        // Update button text if PIN is set
        if (hasPinSet()) {
            btnSetPin.text = getString(R.string.change_pin)
        }
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
                .setTitle(R.string.permission_required)
                .setMessage(R.string.permission_message)
                .setPositiveButton(R.string.grant_permission) { _, _ ->
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                    overlayPermissionLauncher.launch(intent)
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }

    private fun activateOverlay() {
        val serviceIntent = Intent(this, OverlayService::class.java).apply {
            action = OverlayService.ACTION_START_OVERLAY
            // Pass bonus feature settings
            putExtra("auto_dismiss_enabled", sharedPreferences.getBoolean("auto_dismiss_enabled", false))
            putExtra("auto_dismiss_duration", sharedPreferences.getInt("auto_dismiss_duration", 30))
            putExtra("pin_protection_enabled", sharedPreferences.getBoolean("pin_protection_enabled", false))
            putExtra("pin_code", sharedPreferences.getString("pin_code", ""))
        }
        ContextCompat.startForegroundService(this, serviceIntent)
        Toast.makeText(this, "Activating overlay...", Toast.LENGTH_SHORT).show()
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
            hint = getString(R.string.enter_new_pin)
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            maxLines = 1
        }

        val inputConfirm = EditText(this).apply {
            hint = getString(R.string.confirm_pin)
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            maxLines = 1
        }

        inputLayout.addView(inputPin)
        inputLayout.addView(inputConfirm)

        AlertDialog.Builder(this)
            .setTitle(if (hasPinSet()) R.string.change_pin else R.string.set_pin)
            .setView(inputLayout)
            .setPositiveButton("OK") { _, _ ->
                val pin = inputPin.text.toString()
                val confirm = inputConfirm.text.toString()

                when {
                    pin.isEmpty() || confirm.isEmpty() -> {
                        Toast.makeText(this, "PIN cannot be empty", Toast.LENGTH_SHORT).show()
                    }
                    pin.length != 4 -> {
                        Toast.makeText(this, "PIN must be 4 digits", Toast.LENGTH_SHORT).show()
                    }
                    pin != confirm -> {
                        Toast.makeText(this, R.string.pin_mismatch, Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        sharedPreferences.edit().putString("pin_code", pin).apply()
                        Toast.makeText(this, R.string.pin_set_successfully, Toast.LENGTH_SHORT).show()
                        btnSetPin.text = getString(R.string.change_pin)
                    }
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun deactivateOverlay() {
        val serviceIntent = Intent(this, OverlayService::class.java).apply {
            action = OverlayService.ACTION_STOP_OVERLAY
        }
        startService(serviceIntent)
        updateStatus(false)
        Toast.makeText(this, "Deactivating overlay...", Toast.LENGTH_SHORT).show()
    }

    private fun updateStatus(isActive: Boolean) {
        runOnUiThread {
            if (isActive) {
                tvStatus.text = getString(R.string.overlay_active)
                tvStatus.setTextColor(ContextCompat.getColor(this, R.color.status_active))
                tvStatus.setBackgroundResource(R.drawable.bg_status_active)
            } else {
                tvStatus.text = getString(R.string.overlay_dismissed)
                tvStatus.setTextColor(ContextCompat.getColor(this, R.color.status_inactive))
                tvStatus.setBackgroundResource(R.drawable.bg_status_inactive)
            }
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
