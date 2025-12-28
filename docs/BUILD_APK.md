# BUILD APK GUIDE

## GET THE PROJECT

### Clone from GitHub
```bash
git clone https://github.com/Abubakari-dev/lock-overlay.git
cd lock-overlay
```

## BUILD FROM ANDROID STUDIO

### Step 1: Open Project
```
1. Launch Android Studio
2. File > Open
3. Select: lock-overlay folder
4. Wait for Gradle sync to complete
```

### Step 2: Build APK
```
1. Build > Build Bundle(s) / APK(s) > Build APK(s)
2. Wait for build to complete
3. Click "locate" link in notification
```

### Step 3: Find APK
```
Location: app/build/outputs/apk/debug/app-debug.apk
```

## BUILD FROM COMMAND LINE

### Windows
```bash
cd lock-overlay
.\gradlew.bat assembleDebug
```

### Linux/Mac
```bash
cd lock-overlay
./gradlew assembleDebug
```

### APK Output Location
```
app/build/outputs/apk/debug/app-debug.apk
```

## TRANSFER TO DEVICE

### Method 1: USB Cable
```
1. Connect device via USB
2. Copy app-debug.apk to device Downloads folder
3. Open Files app on device
4. Navigate to Downloads
5. Tap app-debug.apk
6. Allow installation from unknown sources
7. Install
```

### Method 2: ADB
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Method 3: Android Studio
```
1. Connect device via USB
2. Enable USB debugging on device
3. Click Run button in Android Studio
4. Select your device
```

## VERIFY INSTALLATION

```
1. Open app drawer on device
2. Look for "Lock Overlay" app
3. Launch app
4. Grant overlay permission when prompted
5. Test activate/dismiss functionality
```
