# IMPLEMENTATION DETAILS

GitHub Repository: https://github.com/Abubakari-dev/lock-overlay

## ARCHITECTURE

### Three-Component System
1. MainActivity - User control interface
2. OverlayService - Background service manager
3. OverlayActivity - Full-screen overlay

## COMPONENTS

### MainActivity (MainActivity.kt)
Main control screen

**Location**: app/src/main/java/com/example/nevixco/MainActivity.kt

**Functionality**:
- Displays overlay status (ACTIVE/DISMISSED)
- Two buttons: Activate and Deactivate
- Checks and requests overlay permission
- Starts/stops OverlayService
- Receives status updates via BroadcastReceiver

**Key Methods**:
- checkOverlayPermission() - Verifies SYSTEM_ALERT_WINDOW permission
- requestOverlayPermission() - Opens system settings for permission
- activateOverlay() - Starts foreground service
- deactivateOverlay() - Stops service and overlay
- updateStatus() - Updates UI based on overlay state

### OverlayService (OverlayService.kt)
Background service managing overlay lifecycle

**Location**: app/src/main/java/com/example/nevixco/OverlayService.kt

**Functionality**:
- Runs as foreground service with notification
- Launches OverlayActivity when activated
- Manages overlay state
- Broadcasts status changes
- Persists until explicitly stopped

**Key Methods**:
- onStartCommand() - Handles start/stop actions
- showOverlay() - Launches OverlayActivity
- hideOverlay() - Dismisses overlay and broadcasts status
- createNotification() - Creates persistent notification

**Broadcasts**:
- ACTION_OVERLAY_SHOWN - Sent when overlay appears
- ACTION_OVERLAY_DISMISSED - Sent when overlay closes
- ACTION_DISMISS_OVERLAY - Sent to close overlay

### OverlayActivity (OverlayActivity.kt)
Full-screen lock overlay

**Location**: app/src/main/java/com/example/nevixco/OverlayActivity.kt

**Functionality**:
- Displays full-screen immersive overlay
- Blocks all system navigation
- Single "Dismiss Overlay" button
- Prevents screenshots
- Shows over lock screen
- Keeps screen on
- Re-launches itself if minimized

**Key Methods**:
- setupFullscreenMode() - Hides system bars and makes immersive
- onBackPressed() - Blocked (empty implementation)
- onKeyDown/onKeyUp() - Blocks home, back, recent apps buttons
- onUserLeaveHint() - Re-launches activity to prevent minimizing
- onWindowFocusChanged() - Re-applies fullscreen mode
- dismissOverlay() - Stops service and closes overlay

**Window Flags**:
- FLAG_SECURE - Prevents screenshots
- FLAG_SHOW_WHEN_LOCKED - Shows over lock screen
- FLAG_TURN_SCREEN_ON - Wakes screen
- FLAG_KEEP_SCREEN_ON - Prevents screen timeout

### AndroidManifest.xml
App configuration

**Location**: app/src/main/AndroidManifest.xml

**Configuration**:
- Declares required permissions
- Registers MainActivity as launcher
- Configures OverlayActivity as singleInstance
- Registers OverlayService as foreground service
- Sets foreground service type to specialUse

## BLOCKING MECHANISMS

### Navigation Blocking
- Back button: onBackPressed() does nothing
- Home/Recent: onKeyDown/onKeyUp block key events
- Swipe gestures: Immersive sticky mode re-hides bars
- Minimize: onUserLeaveHint() re-launches activity

### Fullscreen Mode
- Android 11+: WindowInsetsController hides system bars
- Android 10-: SYSTEM_UI_FLAG_IMMERSIVE_STICKY flag
- BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE - bars auto-hide

## COMMUNICATION

### Broadcast System
Components communicate via BroadcastReceivers

**Flow**:
1. MainActivity sends ACTION_START_OVERLAY to service
2. Service shows overlay and broadcasts ACTION_OVERLAY_SHOWN
3. MainActivity receives broadcast and updates status
4. User presses dismiss button in overlay
5. Overlay broadcasts ACTION_OVERLAY_DISMISSED
6. MainActivity receives broadcast and updates status

## PERMISSIONS

### SYSTEM_ALERT_WINDOW
Required for overlay display
Requested at runtime on Android 6.0+
User must enable in system settings

### FOREGROUND_SERVICE
Required for background service
Automatically granted

### FOREGROUND_SERVICE_SPECIAL_USE
Required on Android 14+
Declared in manifest

## LAYOUTS

### activity_main.xml
Main screen layout with status text and control buttons

### activity_overlay.xml
Full-screen overlay with centered dismiss button
