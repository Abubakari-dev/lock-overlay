# PROJECT REQUIREMENTS

GitHub Repository: https://github.com/Abubakari-dev/lock-overlay

## OBJECTIVE
Create Android mobile application with full-screen overlay that cannot be dismissed except by internal button.

## FUNCTIONAL REQUIREMENTS

### Background Service
- App runs in background
- Overlay appears even when other apps are open
- Service persists until manually stopped

### Full-Screen Overlay
Overlay must:
- Cover entire screen
- Block all user interactions outside overlay
- Prevent dismissal using:
  - Navigation buttons
  - Gesture navigation
  - Swipe down (notification panel)
  - Swipe up
  - Voice commands
  - Back button
  - Home button
  - Recent apps button
- Remain active until internal button pressed

### Internal Control Button
- Only one button inside overlay can remove overlay
- No other method should exit or minimize overlay

### Status Indicator
Simple text indicating:
- OVERLAY ACTIVE
- OVERLAY DISMISSED

### UI
- Single overlay screen
- Clean, simple, minimal design
- No multi-page navigation

## SCOPE

### Included
- Local mobile application
- Foreground service
- System overlay permission
- Broadcast communication
- Status updates

### Not Required
- No database
- No internet/network access
- No user authentication
- No system shutdown, reboot, or power control
- No multi-screen UI

## DELIVERABLES

1. Source code
2. APK file
3. README.md with:
   - Installation instructions
   - How to run app
   - Explanation of background service and overlay logic

## IMPLEMENTATION STATUS

All requirements met:
- Background service implemented as OverlayService
- Full-screen overlay implemented as OverlayActivity
- Navigation blocking via key interception and window flags
- Gesture blocking via immersive mode
- Single dismiss button in overlay
- Status indicator in MainActivity
- Clean minimal UI with Material Design
