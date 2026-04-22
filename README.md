# DailyVerse

An Android app that displays a Bible verse on your lock screen wallpaper every day. Choose between daily inspiration with random verses, or memorization mode that walks you through a book chapter by chapter.

## Features

- **Two modes:**
  - **Daily Inspiration** - A new random verse every day from across the Bible
  - **Memorization Mode** - Sequential verses (1-3 per day) from a book and chapter you choose, with progress tracking
- **Beautiful lock screen wallpapers** - Bible verse text overlaid on stunning images
- **Multiple image sources** - Nature, sunsets, mountains, ocean, forests, flowers, night sky, abstract, or solid color gradients
- **Six Bible versions** - KJV, NIV, ESV, NKJV, NLT, and WEB (KJV and WEB bundled for offline use)
- **Customizable** - Font size, font style, dark overlay toggle, verse reference display
- **Daily scheduling** - Set what time your wallpaper updates each day
- **Notifications** - Optional notification when your daily verse is ready
- **Works offline** - Bundled with 300+ essential KJV verses; online versions require internet

## Tech Stack

- Kotlin
- Jetpack Compose for UI
- Room Database for offline Bible storage
- DataStore for user preferences
- WorkManager for daily scheduling
- Hilt for dependency injection
- Retrofit + OkHttp for API calls
- Coil for image loading
- Unsplash API for images
- Bible API for online translations

## Getting Started

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17 or higher
- Android SDK with API 34
- An Android device running Android 8.0+ (API 26+)

### Setup

1. **Open in Android Studio**
   - File → Open → Select the `DailyVerse` folder
   - Let Android Studio sync and download dependencies (this may take a few minutes)

2. **Add your Unsplash API key**
   - Go to [Unsplash Developers](https://unsplash.com/developers) and create a free app
   - Copy your Access Key
   - Open `app/build.gradle.kts`
   - Replace `YOUR_UNSPLASH_ACCESS_KEY` with your actual key:
     ```kotlin
     buildConfigField("String", "UNSPLASH_ACCESS_KEY", "\"your_actual_key_here\"")
     ```

3. **Build and run**
   - Connect your Android device via USB (enable Developer Options and USB Debugging)
   - Click the Run button (▶) in Android Studio
   - The app will install and launch on your device

### First Launch

On first launch, the app will:
1. Load 300+ bundled KJV verses into the local database (this happens in the background)
2. Ask for permissions: notifications, wallpaper setting, and optionally gallery access
3. Generate your first verse + wallpaper immediately

## Usage

### Home Screen
- View today's verse and preview the wallpaper
- Tap **Generate** to create a new wallpaper
- Tap **Set** (or the floating action button) to apply to your lock screen
- Tap the refresh icon for a new image with the same verse

### Settings
- **App Mode** - Switch between Daily Inspiration and Memorization Mode
- **Bible Version** - Choose your preferred translation
- **Image Source** - Pick photo categories or gradient themes
- **Update Time** - Set when your daily wallpaper updates (default: 6:00 AM)
- **Verses Per Day** - For memorization: 1, 2, or 3 verses daily
- **Display** - Font size, font style, dark overlay toggle
- **Notifications** - Enable/disable daily verse notifications

### Memorization Mode Setup
1. Go to Settings → tap on "Memorization Mode"
2. Tap "Book & Chapter" to configure
3. Select your book from the dropdown
4. Select your chapter
5. Choose how many verses per day (1-3)
6. The app will drip verses sequentially until you finish the chapter

## Project Structure

```
DailyVerse/
├── app/src/main/java/com/dailyverse/app/
│   ├── MainActivity.kt              # Main activity with navigation
│   ├── DailyVerseApplication.kt     # Application class + DI
│   ├── data/
│   │   ├── BibleRepository.kt       # Bible verse data management
│   │   ├── ImageRepository.kt       # Image fetching + text compositing
│   │   ├── SettingsRepository.kt    # User preferences
│   │   ├── model/                   # Data classes
│   │   ├── local/                   # Room DB + DataStore
│   │   └── remote/                  # API interfaces
│   ├── worker/
│   │   └── DailyWallpaperWorker.kt  # Background daily update
│   ├── receiver/
│   │   └── BootReceiver.kt          # Reschedule on reboot
│   ├── ui/
│   │   ├── screens/                 # Compose screens
│   │   ├── theme/                   # Material3 theme
│   │   └── viewmodel/               # ViewModels
│   ├── di/
│   │   └── AppModule.kt             # Hilt module
│   └── util/                        # Helpers
├── app/src/main/res/                # Resources
└── build.gradle.kts                 # Build config
```

## Architecture

- **MVVM** pattern with ViewModels managing UI state
- **Repository pattern** for data access
- **Dependency injection** with Hilt
- **Offline-first** - bundled KJV data works without internet
- **Background processing** via WorkManager for reliable daily updates

## Customization

The bundled KJV verses cover the most popular and meaningful verses from every book of the Bible (~300 verses). You can extend this by:

1. Adding more verses to `BibleRepository.kt` → `loadBundledKjvVerses()`
2. Or implementing a CSV/JSON import feature
3. Or connecting to a Bible API for full verse access (NIV, ESV require API keys)

## Notes

- **Unsplash** free tier allows 50 requests/hour - more than enough for personal use
- Lock screen wallpaper setting requires the `SET_WALLPAPER` permission
- On some devices (especially Samsung, Xiaomi), you may need to grant additional permissions in Settings > Apps > DailyVerse > Permissions
- The app uses `WorkManager` which respects Doze mode and battery optimizations - your wallpaper will still update daily even if you don't open the app

## License

This project is for personal use. Bible text from the King James Version and World English Bible are in the public domain.
