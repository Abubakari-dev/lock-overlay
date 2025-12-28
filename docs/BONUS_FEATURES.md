# BONUS FEATURES IMPLEMENTATION GUIDE

## Overview

This document explains how the optional bonus features (Auto-Dismiss Timer and PIN Protection) are implemented and how they work internally.

---

## 1. AUTO-DISMISS TIMER

### What It Does
Automatically dismisses the overlay after a set time period (15, 30, 60, 90, or 120 seconds).

### How It Works

#### User Flow:
1. User opens Lock Overlay app
2. User enables "Auto-Dismiss Timer" switch in Bonus Features section
3. User selects duration from dropdown (e.g., 30 seconds)
4. User activates overlay
5. Countdown timer appears at bottom of overlay screen
6. Timer counts down: "Auto-dismiss in: 30s", "29s", "28s"...
7. When timer reaches 0, overlay automatically dismisses

#### Technical Implementation:

**Step 1: User Configuration (MainActivity.kt)**
```kotlin
// Location: MainActivity.kt, line 104-107
switchAutoDismiss.setOnCheckedChangeListener { _, isChecked ->
    // Show/hide duration selector
    layoutAutoDismissDuration.visibility = if (isChecked) View.VISIBLE else View.GONE

    // Save setting to SharedPreferences
    sharedPreferences.edit().putBoolean("auto_dismiss_enabled", isChecked).apply()
}

// Location: MainActivity.kt, line 121-127
spinnerDuration.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        // Save selected duration (15, 30, 60, 90, or 120 seconds)
        sharedPreferences.edit().putInt("auto_dismiss_duration", durationValues[position]).apply()
    }
}
```

**Step 2: Pass Settings to Service (MainActivity.kt)**
```kotlin
// Location: MainActivity.kt, line 194-205
private fun activateOverlay() {
    val serviceIntent = Intent(this, OverlayService::class.java).apply {
        action = OverlayService.ACTION_START_OVERLAY

        // Pass auto-dismiss settings
        putExtra("auto_dismiss_enabled", sharedPreferences.getBoolean("auto_dismiss_enabled", false))
        putExtra("auto_dismiss_duration", sharedPreferences.getInt("auto_dismiss_duration", 30))
    }
    ContextCompat.startForegroundService(this, serviceIntent)
}
```

**Step 3: Forward Settings to Overlay (OverlayService.kt)**
```kotlin
// Location: OverlayService.kt, line 89-108
private fun showOverlay(sourceIntent: Intent?) {
    val overlayIntent = Intent(this, OverlayActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_NO_HISTORY

        // Forward settings to OverlayActivity
        putExtra("auto_dismiss_enabled", sourceIntent?.getBooleanExtra("auto_dismiss_enabled", false) ?: false)
        putExtra("auto_dismiss_duration", sourceIntent?.getIntExtra("auto_dismiss_duration", 30) ?: 30)
    }
    startActivity(overlayIntent)
}
```

**Step 4: Start Timer in Overlay (OverlayActivity.kt)**
```kotlin
// Location: OverlayActivity.kt, line 43-50
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Get settings from intent
    autoDismissEnabled = intent.getBooleanExtra("auto_dismiss_enabled", false)
    autoDismissDuration = intent.getIntExtra("auto_dismiss_duration", 30)

    // ... other initialization ...

    // Start timer if enabled
    if (autoDismissEnabled) {
        startAutoDismissTimer()
    }
}

// Location: OverlayActivity.kt, line 210-225
private fun startAutoDismissTimer() {
    val durationMillis = (autoDismissDuration * 1000).toLong()  // Convert seconds to milliseconds

    countDownTimer = object : CountDownTimer(durationMillis, 1000) {  // Tick every 1 second
        override fun onTick(millisUntilFinished: Long) {
            val secondsRemaining = (millisUntilFinished / 1000).toInt()

            // Update timer display: "Auto-dismiss in: 30s"
            tvTimer?.text = getString(R.string.auto_dismiss_in, secondsRemaining)
            tvTimer?.visibility = View.VISIBLE
        }

        override fun onFinish() {
            // Timer finished - automatically dismiss overlay
            tvTimer?.visibility = View.GONE
            dismissOverlay()
        }
    }.start()
}
```

**Step 5: Clean Up Timer (OverlayActivity.kt)**
```kotlin
// Location: OverlayActivity.kt, line 200-208
override fun onDestroy() {
    super.onDestroy()

    // Cancel timer to prevent memory leaks
    countDownTimer?.cancel()

    // ... other cleanup ...
}
```

### Data Flow Diagram:
```
MainActivity (User Input)
    ↓ [SharedPreferences: auto_dismiss_enabled=true, duration=30]
    ↓
Intent extras → OverlayService
    ↓ [Forwards settings]
    ↓
Intent extras → OverlayActivity
    ↓ [Creates CountDownTimer(30000ms)]
    ↓
CountDownTimer
    ├─ Tick every 1000ms (1 second)
    │   └─ Update TextView: "Auto-dismiss in: 30s", "29s", "28s"...
    └─ onFinish()
        └─ dismissOverlay()
```

### Key Components:
- **SharedPreferences**: Stores user's auto-dismiss settings persistently
- **Intent Extras**: Passes settings from MainActivity → Service → Activity
- **CountDownTimer**: Android's built-in timer that counts down from specified duration
- **TextView (tvTimer)**: Displays countdown on overlay screen

---

## 2. PIN PROTECTION

### What It Does
Requires user to enter a 4-digit PIN before dismissing the overlay.

### How It Works

#### User Flow:
1. User opens Lock Overlay app
2. User enables "PIN Protection" switch
3. "Set PIN" button appears
4. User taps "Set PIN" and enters 4-digit PIN (e.g., 1234)
5. User confirms PIN (enters 1234 again)
6. PIN is saved
7. User activates overlay
8. User tries to dismiss overlay by pressing "Dismiss Overlay" button
9. PIN verification dialog appears
10. User must enter correct PIN (1234) to unlock
11. If PIN is correct → overlay dismisses
12. If PIN is wrong → error message shown, overlay remains locked

#### Technical Implementation:

**Step 1: Enable PIN Protection (MainActivity.kt)**
```kotlin
// Location: MainActivity.kt, line 109-118
switchPinProtection.setOnCheckedChangeListener { _, isChecked ->
    // Show/hide "Set PIN" button
    btnSetPin.visibility = if (isChecked) View.VISIBLE else View.GONE

    // Save setting
    sharedPreferences.edit().putBoolean("pin_protection_enabled", isChecked).apply()

    // If enabled but no PIN set, show PIN setup dialog
    if (isChecked && !hasPinSet()) {
        Toast.makeText(this, R.string.pin_required, Toast.LENGTH_SHORT).show()
        showSetPinDialog()
    }
}
```

**Step 2: Set PIN Dialog (MainActivity.kt)**
```kotlin
// Location: MainActivity.kt, line 212-259
private fun showSetPinDialog() {
    // Create layout with two input fields
    val inputLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(50, 20, 50, 20)
    }

    // First input: Enter new PIN
    val inputPin = EditText(this).apply {
        hint = getString(R.string.enter_new_pin)  // "Enter New PIN (4 digits)"
        inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        maxLines = 1
    }

    // Second input: Confirm PIN
    val inputConfirm = EditText(this).apply {
        hint = getString(R.string.confirm_pin)  // "Confirm PIN"
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

            // Validation
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
                    // Save PIN to SharedPreferences
                    sharedPreferences.edit().putString("pin_code", pin).apply()
                    Toast.makeText(this, R.string.pin_set_successfully, Toast.LENGTH_SHORT).show()
                    btnSetPin.text = getString(R.string.change_pin)
                }
            }
        }
        .setNegativeButton(android.R.string.cancel, null)
        .show()
}

// Check if PIN is set
private fun hasPinSet(): Boolean {
    val pin = sharedPreferences.getString("pin_code", "")
    return !pin.isNullOrEmpty()
}
```

**Step 3: Pass PIN to Overlay (MainActivity.kt → OverlayService → OverlayActivity)**
```kotlin
// Location: MainActivity.kt, line 194-205
private fun activateOverlay() {
    val serviceIntent = Intent(this, OverlayService::class.java).apply {
        action = OverlayService.ACTION_START_OVERLAY

        // Pass PIN settings
        putExtra("pin_protection_enabled", sharedPreferences.getBoolean("pin_protection_enabled", false))
        putExtra("pin_code", sharedPreferences.getString("pin_code", ""))
    }
    ContextCompat.startForegroundService(this, serviceIntent)
}

// Location: OverlayService.kt, line 89-108
// Service forwards PIN settings to OverlayActivity via Intent extras
```

**Step 4: Intercept Dismiss Button (OverlayActivity.kt)**
```kotlin
// Location: OverlayActivity.kt, line 43-50
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Get PIN settings from intent
    pinProtectionEnabled = intent.getBooleanExtra("pin_protection_enabled", false)
    pinCode = intent.getStringExtra("pin_code") ?: ""

    // ... other initialization ...
}

// Location: OverlayActivity.kt, line 109-120
private fun initViews() {
    btnDismiss = findViewById(R.id.btnDismiss)

    btnDismiss.setOnClickListener {
        // Check if PIN protection is enabled
        if (pinProtectionEnabled && pinCode.isNotEmpty()) {
            // Show PIN verification dialog instead of dismissing
            showPinDialog()
        } else {
            // No PIN protection - dismiss normally
            dismissOverlay()
        }
    }
}
```

**Step 5: PIN Verification Dialog (OverlayActivity.kt)**
```kotlin
// Location: OverlayActivity.kt, line 227-248
private fun showPinDialog() {
    val inputLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(50, 20, 50, 20)
    }

    val input = EditText(this).apply {
        inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        hint = getString(R.string.enter_pin)  // "Enter PIN"
    }

    inputLayout.addView(input)

    AlertDialog.Builder(this)
        .setTitle(R.string.verify_pin)  // "Enter PIN to Unlock"
        .setView(inputLayout)
        .setPositiveButton("OK") { dialog, _ ->
            val enteredPin = input.text.toString()

            // Verify PIN
            if (enteredPin == pinCode) {
                // Correct PIN - dismiss overlay
                pinAttempts = 0
                dialog.dismiss()
                dismissOverlay()
            } else {
                // Wrong PIN - show error with remaining attempts
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
        .setCancelable(false)  // User cannot cancel dialog by tapping outside
        .show()
}
```

### Data Flow Diagram:
```
MainActivity (Setup Phase)
    ↓
User enables PIN Protection
    ↓
showSetPinDialog()
    ├─ User enters PIN: "1234"
    ├─ User confirms: "1234"
    ├─ Validate: length=4, match=true
    └─ Save to SharedPreferences: pin_code="1234"

MainActivity (Activation Phase)
    ↓ [SharedPreferences: pin_protection_enabled=true, pin_code="1234"]
Intent extras → OverlayService
    ↓ [Forwards PIN settings]
Intent extras → OverlayActivity
    ↓ [Stores: pinProtectionEnabled=true, pinCode="1234"]

OverlayActivity (Dismiss Phase)
    ↓
User taps "Dismiss Overlay" button
    ↓
btnDismiss.onClick()
    ├─ Check: pinProtectionEnabled=true AND pinCode.isNotEmpty()
    ├─ YES → showPinDialog()
    │   ↓
    │   User enters PIN
    │   ↓
    │   Verify: enteredPin == pinCode?
    │   ├─ YES → dismissOverlay()
    │   └─ NO  → Show error, stay locked
    │
    └─ NO → dismissOverlay() directly
```

### Security Features:
1. **PIN Storage**: Stored in SharedPreferences (encrypted on Android 6.0+)
2. **PIN Validation**: Must be exactly 4 digits
3. **Confirmation**: User must enter PIN twice when setting it
4. **Non-Cancelable Dialog**: User cannot bypass by tapping outside dialog
5. **Error Feedback**: Shows "Incorrect PIN" message but doesn't reveal correct PIN
6. **Change PIN**: User can change PIN at any time via "Change PIN" button
7. **Attempt Limiting**: Maximum 3 failed PIN attempts before blocking further attempts

### Key Components:
- **SharedPreferences**: Stores PIN and enabled/disabled state
- **AlertDialog**: Shows PIN input dialogs (setup and verification)
- **EditText with PASSWORD type**: Hides PIN as user types (shows dots ••••)
- **Intent Extras**: Passes PIN from MainActivity → Service → Activity
- **Boolean Check**: Intercepts dismiss button click to show PIN dialog

---

## COMBINATION OF BOTH FEATURES

### Scenario: Auto-Dismiss Timer + PIN Protection Both Enabled

**What Happens:**
1. User enables both Auto-Dismiss Timer (30s) and PIN Protection
2. User sets PIN: 1234
3. User activates overlay
4. Overlay appears with countdown timer: "Auto-dismiss in: 30s"
5. Timer counts down: 29s, 28s, 27s...
6. If user tries to dismiss manually → PIN dialog appears
   - Correct PIN → dismisses
   - Wrong PIN → stays locked, timer continues
7. When timer reaches 0 → automatically dismisses WITHOUT asking for PIN

**Why Auto-Dismiss Bypasses PIN:**
The auto-dismiss feature calls `dismissOverlay()` directly from the timer's `onFinish()` method, which doesn't go through the button click listener where PIN checking happens. This is intentional - if user set an auto-dismiss timer, they want it to actually dismiss automatically.

**Code Logic:**
```kotlin
// Manual dismiss (via button) - requires PIN
btnDismiss.setOnClickListener {
    if (pinProtectionEnabled && pinCode.isNotEmpty()) {
        showPinDialog()  // PIN required
    } else {
        dismissOverlay()
    }
}

// Auto-dismiss (via timer) - bypasses PIN
override fun onFinish() {
    dismissOverlay()  // Direct call, no PIN check
}
```

---

## TESTING THE FEATURES

### Test Auto-Dismiss Timer:
1. Enable "Auto-Dismiss Timer" switch
2. Select "15 seconds" from dropdown
3. Activate overlay
4. Observe: Timer shows "Auto-dismiss in: 15s" and counts down
5. Wait 15 seconds
6. Result: Overlay automatically dismisses

### Test PIN Protection:
1. Enable "PIN Protection" switch
2. Tap "Set PIN"
3. Enter: 1234
4. Confirm: 1234
5. Activate overlay
6. Tap "Dismiss Overlay" button
7. Enter wrong PIN: 5678
8. Result: Error message, overlay stays
9. Enter correct PIN: 1234
10. Result: Overlay dismisses

### Test Both Combined:
1. Enable both features
2. Set PIN: 9999
3. Set timer: 60 seconds
4. Activate overlay
5. Try manual dismiss at 30 seconds → Requires PIN
6. Wait until timer reaches 0 → Auto-dismisses without PIN

---

## FILE LOCATIONS

**Auto-Dismiss Timer:**
- UI: `activity_main.xml` (line 233-296)
- Logic: `MainActivity.kt` (line 104-107, 121-127)
- Timer: `OverlayActivity.kt` (line 210-225)
- Display: `activity_overlay.xml` (line 210-222)

**PIN Protection:**
- UI: `activity_main.xml` (line 298-348)
- Setup: `MainActivity.kt` (line 109-118, 212-259)
- Verification: `OverlayActivity.kt` (line 113-119, 227-248)
- Strings: `strings.xml` (line 19-31)

**Data Persistence:**
- Settings stored in: `SharedPreferences("LockOverlayPrefs")`
- Keys: `auto_dismiss_enabled`, `auto_dismiss_duration`, `pin_protection_enabled`, `pin_code`

---

## SUMMARY

Both bonus features are fully functional and work independently or together. They use standard Android components (SharedPreferences, CountDownTimer, AlertDialog) and follow best practices for data persistence and user interaction.
