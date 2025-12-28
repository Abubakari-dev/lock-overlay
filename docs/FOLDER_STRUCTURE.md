# FOLDER STRUCTURE

GitHub Repository: https://github.com/Abubakari-dev/lock-overlay

## PROJECT LAYOUT

```
lock-overlay/
├── app/
│   ├── build.gradle                    # App configuration
│   ├── proguard-rules.pro              # Code obfuscation rules
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml     # App configuration and permissions
│           ├── java/com/example/nevixco/ # Source code directory
│           │   ├── MainActivity.kt     # Main control screen
│           │   ├── OverlayActivity.kt  # Full-screen overlay screen
│           │   └── OverlayService.kt   # Background service
│           └── res/                    # Resources
│               ├── drawable/           # Background shapes and gradients
│               ├── layout/             # Screen layouts
│               │   ├── activity_main.xml       # Main screen layout
│               │   └── activity_overlay.xml    # Overlay screen layout
│               ├── values/             # Colors, strings, themes
│               └── mipmap-*/           # App icons
├── build.gradle                        # Project-level build configuration
├── settings.gradle                     # Project settings
└── docs/                               # Documentation files
    ├── REQUIREMENTS.md
    ├── FOLDER_STRUCTURE.md
    ├── SETUP.md
    ├── IMPLEMENTATION.md
    └── BUILD_APK.md
```

## KEY DIRECTORIES

### app/src/main/java/com/example/nevixco/
Contains all Kotlin source files

### app/src/main/res/
Contains all UI resources (layouts, colors, strings, drawables)

### docs/
Project documentation
