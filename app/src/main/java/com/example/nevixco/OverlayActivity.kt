package com.example.nevixco

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.text.InputType
import android.view.KeyEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class OverlayActivity : AppCompatActivity() {

    private lateinit var btnDismiss: MaterialButton
    private var tvTimer: TextView? = null
    private var cardTimer: View? = null
    private var tvStatus: TextView? = null

    // Bonus features
    private var autoDismissEnabled = false
    private var autoDismissDuration = 30
    private var pinProtectionEnabled = false
    private var pinCode = ""
    private var countDownTimer: CountDownTimer? = null
    private var pinAttempts = 0
    private val maxPinAttempts = 3

    private val dismissReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == OverlayService.ACTION_DISMISS_OVERLAY) {
                finishAndRemoveTask()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get bonus feature settings from intent
        autoDismissEnabled = intent.getBooleanExtra("auto_dismiss_enabled", false)
        autoDismissDuration = intent.getIntExtra("auto_dismiss_duration", 30)
        pinProtectionEnabled = intent.getBooleanExtra("pin_protection_enabled", false)
        pinCode = intent.getStringExtra("pin_code") ?: ""

        // Make the activity secure and prevent screenshots
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        // Set the activity to show over lock screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        // Keep screen on
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContentView(R.layout.activity_overlay)

        setupFullscreenMode()
        initViews()
        registerDismissReceiver()

        // Disable back button using new API
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Do nothing - back button is disabled
            }
        })

        // Start auto-dismiss timer if enabled
        if (autoDismissEnabled) {
            startAutoDismissTimer()
        }
    }

    private fun setupFullscreenMode() {
        // Make the activity fullscreen and immersive
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // For Android 11 (API 30) and above
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.systemBars())
                controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            // For older versions
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            or View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    )
        }
    }

    private fun initViews() {
        btnDismiss = findViewById(R.id.btnDismiss)
        tvTimer = findViewById(R.id.tvTimer)
        cardTimer = findViewById(R.id.cardTimer)
        tvStatus = findViewById(R.id.tvStatus)

        btnDismiss.setOnClickListener {
            if (pinProtectionEnabled && pinCode.isNotEmpty()) {
                showPinDialog()
            } else {
                dismissOverlay()
            }
        }

        // Update status display
        updateStatusDisplay()
    }

    private fun updateStatusDisplay() {
        tvStatus?.text = "OVERLAY ACTIVE"
        tvStatus?.setTextColor(getColor(R.color.status_active))
    }

    private fun registerDismissReceiver() {
        val filter = IntentFilter(OverlayService.ACTION_DISMISS_OVERLAY)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(dismissReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(dismissReceiver, filter)
        }
    }

    private fun dismissOverlay() {
        // Send broadcast that overlay is being dismissed
        sendBroadcast(Intent(OverlayService.ACTION_OVERLAY_DISMISSED))

        // Stop the service
        val serviceIntent = Intent(this, OverlayService::class.java)
        stopService(serviceIntent)

        // Finish the activity
        finishAndRemoveTask()
    }

    override fun onResume() {
        super.onResume()
        // Re-apply fullscreen mode when resuming
        setupFullscreenMode()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            // Re-apply fullscreen mode when focus is regained
            setupFullscreenMode()
        }
    }

    // Block all key events except volume
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_BACK,
            KeyEvent.KEYCODE_HOME,
            KeyEvent.KEYCODE_APP_SWITCH,
            KeyEvent.KEYCODE_MENU -> {
                // Block these keys
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_BACK,
            KeyEvent.KEYCODE_HOME,
            KeyEvent.KEYCODE_APP_SWITCH,
            KeyEvent.KEYCODE_MENU -> {
                // Block these keys
                true
            }
            else -> super.onKeyUp(keyCode, event)
        }
    }

    // Prevent activity from being paused when possible
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // Re-launch this activity to bring it back to front
        val intent = Intent(this, OverlayActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        }
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
        try {
            unregisterReceiver(dismissReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startAutoDismissTimer() {
        val durationMillis = (autoDismissDuration * 1000).toLong()

        countDownTimer = object : CountDownTimer(durationMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsRemaining = (millisUntilFinished / 1000).toInt()
                tvTimer?.text = getString(R.string.auto_dismiss_in, secondsRemaining)
                cardTimer?.visibility = View.VISIBLE
            }

            override fun onFinish() {
                cardTimer?.visibility = View.GONE
                dismissOverlay()
            }
        }.start()
    }

    private fun showPinDialog() {
        val inputLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 20, 50, 20)
        }

        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            hint = getString(R.string.enter_pin)
            maxLines = 1
        }

        inputLayout.addView(input)

        AlertDialog.Builder(this)
            .setTitle(R.string.verify_pin)
            .setView(inputLayout)
            .setPositiveButton("OK") { dialog, _ ->
                val enteredPin = input.text.toString()
                if (enteredPin == pinCode) {
                    pinAttempts = 0
                    dialog.dismiss()
                    dismissOverlay()
                } else {
                    pinAttempts++
                    if (pinAttempts >= maxPinAttempts) {
                        Toast.makeText(this, "Too many failed attempts", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    } else {
                        Toast.makeText(this, "Incorrect PIN. Attempts remaining: ${maxPinAttempts - pinAttempts}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .setCancelable(false)
            .show()
    }
}
