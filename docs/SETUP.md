# SETUP AND INSTALLATION

## PREREQUISITES
- Android Studio Arctic Fox or newer
- Android SDK 24 (Minimum) to 34 (Target)
- JDK 11 or higher
- Git (for cloning repository)
- Android device or emulator running Android 6.0 or higher

## INSTALLATION STEPS

### 1. Clone Project from GitHub
```bash
git clone https://github.com/Abubakari-dev/lock-overlay.git
cd lock-overlay
```

Or download ZIP from GitHub:
```
https://github.com/Abubakari-dev/lock-overlay
Click "Code" > "Download ZIP"
Extract to desired location
```

### 2. Open in Android Studio
```
File > Open > Select 'lock-overlay' folder
```

### 3. Sync Gradle
```
Android Studio will automatically sync Gradle dependencies
If not, click "Sync Project with Gradle Files" in toolbar
```

### 4. Build Project
```
Build > Make Project
or
Ctrl+F9 (Windows/Linux) / Cmd+F9 (Mac)
```

### 5. Generate APK
```
Build > Build Bundle(s) / APK(s) > Build APK(s)
APK location: app/build/outputs/apk/debug/app-debug.apk
```

### 6. Install APK
```
Transfer APK to Android device
Enable "Install from Unknown Sources" in device settings
Install APK
```

## RUNNING THE APP

### From Android Studio
```
1. Connect Android device via USB or start emulator
2. Click Run button (green triangle) or Shift+F10
3. Select target device
```

### From Device
```
1. Open "Lock Overlay" app
2. Tap "Activate Overlay"
3. Grant overlay permission when prompted
4. Overlay will appear full-screen
5. Press "Dismiss Overlay" button inside overlay to close
```

## PERMISSIONS
App requires overlay permission on first run:
- Tap "Grant Permission" button
- System settings will open
- Enable "Display over other apps"
- Return to app and tap "Activate Overlay" again

## TESTING
1. Activate overlay
2. Try pressing back button (blocked)
3. Try pressing home button (overlay persists)
4. Try opening notification panel (blocked)
5. Only "Dismiss Overlay" button should work
