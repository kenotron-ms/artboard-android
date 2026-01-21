# Quick Start Guide

Get Artboard running on your Android tablet in 5 minutes!

## Option 1: Build in Android Studio (Recommended)

1. **Install Android Studio**
   - Download from https://developer.android.com/studio
   - Install with default settings

2. **Open Project**
   ```
   File → Open → Select the 'artboard' folder
   ```

3. **Wait for Gradle Sync**
   - First time will download dependencies (5-10 minutes)
   - Watch the progress bar at bottom of Android Studio

4. **Connect Your Tablet**
   - Enable Developer Options on tablet:
     - Settings → About Tablet → Tap "Build Number" 7 times
   - Enable USB Debugging:
     - Settings → Developer Options → USB Debugging ON
   - Connect via USB cable

5. **Run the App**
   - Click the green ▶️ Play button
   - Select your device
   - App installs and launches!

## Option 2: Build from Command Line

```bash
# Navigate to project
cd artboard

# Build debug APK
./gradlew assembleDebug

# Install on connected device
adb install app/build/outputs/apk/debug/app-debug.apk
```

## First Time Setup

### If you don't have the Gradle wrapper:

Download it manually:
```bash
cd artboard
gradle wrapper --gradle-version 8.2
```

Or use Android Studio which will set it up automatically.

### If Gradle sync fails:

1. Check your internet connection (needs to download dependencies)
2. File → Invalidate Caches / Restart
3. Try again

## Testing Without a Device

Use Android Studio's emulator:

1. Tools → Device Manager
2. Create Device → Select tablet (e.g., "Pixel Tablet")
3. Download system image (Android 10+)
4. Run the emulator
5. Click Play to install app

**Note**: Stylus pressure won't work in emulator, but you can test with mouse.

## Minimum Requirements

- **Tablet**: Android 10 or higher
- **Stylus**: Any pressure-sensitive stylus (optional but recommended)
- **RAM**: 2GB+ (4GB+ recommended)
- **Storage**: 100MB free space

## What to Expect

First launch:
- Black toolbar at top
- White canvas filling screen
- Touch or use stylus to draw
- Tap toolbar icons to change brushes and colors

Performance:
- Should feel instant and smooth
- If laggy, try reducing canvas size in code (default is 2048x2048)

## Customizing Canvas Size

Edit `MainActivity.kt` or `CanvasViewModel.kt`:

```kotlin
// Change these values
Project.create("My Drawing", 1024, 1024)  // Smaller = faster
// or
Project.create("My Drawing", 4096, 4096)  // Larger = more detail
```

## Next Steps

- Read the full README.md for features
- Check ARCHITECTURE.md for how it works
- Start drawing! 🎨

## Getting Help

If something doesn't work:
1. Check Android Studio's "Build" panel for errors
2. Try File → Sync Project with Gradle Files
3. Clean build: Build → Clean Project, then Build → Rebuild Project
4. Check you have JDK 17+ installed

**Most common issue**: Gradle sync failure → Wait for internet download, then retry.
