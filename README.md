# DailyVerse

An Android app that displays a Bible verse on your lock screen wallpaper every day. Choose between daily inspiration with random verses, or memorization mode that walks you through a book chapter by chapter.

## Features

- **Two modes:**
  - **Daily Inspiration** - A new random verse every day from across the Bible
  - **Memorization Mode** - Sequential verses (1-3 per day) from a book and chapter you choose, with progress tracking
- **Beautiful lock screen wallpapers** - Bible verse text overlaid on stunning images
- **Two image sources:**
  - **Unsplash 4K** (default) - 10 curated 4K photo categories: Nature, Sunrise, Mountains, Ocean, Forest, Flowers, Night Sky, Abstract, Aurora, Cityscape
  - **Pexels Custom Search** (second option) - Fully customizable: search ANY keyword like "waterfall", "beach", "snow", "minimal", etc.
  - **Gradient Themes** (offline) - 8 beautiful gradients that work without internet
- **Six Bible versions** - KJV, NIV, ESV, NKJV, NLT, and WEB (KJV bundled for offline use)
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
- Unsplash API for 4K images (default)
- Pexels API for custom search images
- Bible API for online translations

## Setup Guide

### Step 1: Get Free API Keys

You need **two free API keys** (takes 2 minutes each):

#### Unsplash API Key (Required - for 4K wallpapers)
1. Go to [https://unsplash.com/developers](https://unsplash.com/developers)
2. Click "Your Apps" → "New Application"
3. Accept the terms, give it a name (e.g., "DailyVerse")
4. Copy your **Access Key**

#### Pexels API Key (Required - for custom search)
1. Go to [https://www.pexels.com/api/](https://www.pexels.com/api/)
2. Click "Join" and create a free account
3. Fill out the form (use "Personal" for project type)
4. Copy your **API Key**

### Step 2: Add API Keys to the Project

Open `app/build.gradle.kts` and replace the placeholder values:

```kotlin
buildConfigField("String", "UNSPLASH_ACCESS_KEY", "\"YOUR_UNSPLASH_ACCESS_KEY\"")
buildConfigField("String", "PEXELS_API_KEY", "\"YOUR_PEXELS_API_KEY\"")
```

### Step 3: Build the APK

You have **three options** to build the APK:

---

### Option A: GitHub Actions (Easiest - No Android Studio needed!)

1. **Create a GitHub repo** (see "Push to GitHub" below)
2. After pushing, go to your repo on GitHub
3. Click **Actions** tab
4. Select **"Build APK"** workflow
5. Click **"Run workflow"**
6. Wait ~5 minutes
7. Download your APK from the **Artifacts** section

### Option B: Android Studio (Recommended for development)

1. Download and install [Android Studio](https://developer.android.com/studio)
2. File → Open → Select the `DailyVerse` folder
3. Wait for Gradle sync (first time takes ~5-10 minutes)
4. Connect your Android device (enable **USB Debugging** in Developer Options)
5. Click the **Run** button (▶) to install and launch
6. To build APK: Build → Build Bundle(s) / APK(s) → Build APK(s)

### Option C: Command Line (Requires Android SDK)

```bash
# 1. Install Android SDK command line tools
# Download from: https://developer.android.com/studio#command-tools

# 2. Set environment variables
export ANDROID_HOME=/path/to/android-sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin

# 3. Accept licenses
sdkmanager --licenses

# 4. Install required SDK components
sdkmanager "platforms;android-34" "build-tools;34.0.0"

# 5. Build debug APK
./gradlew assembleDebug

# 6. The APK will be at:
# app/build/outputs/apk/debug/app-debug.apk
```

### Option D: Docker Build (No local Android SDK needed)

```bash
# Build using a Docker container with Android SDK pre-installed
docker run --rm -v "$(pwd):/project" -w /project \
  mingc/android-build-box \
  bash -c "chmod +x gradlew && ./gradlew assembleDebug"

# APK will be at: app/build/outputs/apk/debug/app-debug.apk
```

---

## Push to GitHub

### Create the repo and push:

```bash
# Option 1: Using GitHub CLI (if installed)
gh repo create richardpineros-bit/DailyVerse --public --source=. --push

# Option 2: Using HTTPS
# First create the empty repo on GitHub (no README, no .gitignore)
# Then:
git remote add origin https://github.com/richardpineros-bit/DailyVerse.git
git branch -M main
git push -u origin main

# Option 3: Using SSH (recommended)
# First create the empty repo on GitHub
git remote add origin git@github.com:richardpineros-bit/DailyVerse.git
git branch -M main
git push -u origin main
```

---

## First Launch

On first launch, the app will:
1. Load 300+ bundled KJV verses into the local database (background)
2. Ask for permissions: notifications, wallpaper setting, gallery access
3. Generate your first verse + wallpaper immediately
4. Schedule daily updates at 6:00 AM

---

## Project Structure

```
DailyVerse/
├── .github/workflows/              # CI/CD - auto builds APK on GitHub
│   ├── build-apk.yml               # Auto debug + release APK
│   └── build-release.yml           # Manual signed release
├── app/src/main/java/com/dailyverse/app/
│   ├── MainActivity.kt             # Main activity with navigation
│   ├── DailyVerseApplication.kt    # Application class + DI
│   ├── data/
│   │   ├── BibleRepository.kt      # Bible verse data (300+ bundled)
│   │   ├── ImageRepository.kt      # Unsplash 4K + Pexels + compositing
│   │   ├── SettingsRepository.kt   # User preferences
│   │   ├── model/                  # Data classes
│   │   ├── local/                  # Room DB + DataStore
│   │   └── remote/                 # Unsplash API, Pexels API, Bible API
│   ├── worker/
│   │   └── DailyWallpaperWorker.kt # Background daily wallpaper update
│   ├── receiver/
│   │   └── BootReceiver.kt         # Reschedule on reboot
│   ├── ui/screens/                 # Compose screens
│   ├── ui/theme/                   # Material3 theme
│   ├── ui/viewmodel/               # ViewModels
│   ├── di/
│   │   └── AppModule.kt            # Hilt module
│   └── util/                       # Bible books helper + notifications
├── app/src/main/res/               # Strings, colors, themes, XML
├── README.md                       # This file
├── build.gradle.kts                # Root build config
├── settings.gradle.kts             # Gradle settings
├── gradlew                         # Gradle wrapper (Unix)
└── gradlew.bat                     # Gradle wrapper (Windows)
```

## Architecture

- **MVVM** pattern with ViewModels managing UI state
- **Repository pattern** for data access
- **Dependency injection** with Hilt
- **Offline-first** - bundled KJV data works without internet
- **Background processing** via WorkManager for reliable daily updates
- **Dual image sources** - Unsplash 4K (default) + Pexels (custom search)

## Permissions Required

| Permission | Why |
|-----------|-----|
| `INTERNET` | Fetch images from Unsplash/Pexels, Bible API |
| `SET_WALLPAPER` | Apply verse wallpaper to lock screen |
| `RECEIVE_BOOT_COMPLETED` | Reschedule daily updates after reboot |
| `POST_NOTIFICATIONS` | Notify when daily verse is ready |
| `READ_MEDIA_IMAGES` | Optional - pick from your gallery |

## Customization

The bundled KJV verses cover the most popular and meaningful verses from every book of the Bible (~300 verses). You can extend this by:

1. Adding more verses to `BibleRepository.kt` → `loadBundledKjvVerses()`
2. Or implementing a CSV/JSON import feature
3. Or connecting to a Bible API for full verse access (NIV, ESV require API keys)

## Notes

- **Unsplash** free tier allows 50 requests/hour - more than enough for personal use
- **Pexels** free tier allows 200 requests/hour - also plenty
- Lock screen wallpaper setting requires the `SET_WALLPAPER` permission
- On some devices (especially Samsung, Xiaomi), you may need to grant additional permissions in Settings > Apps > DailyVerse > Permissions
- The app uses `WorkManager` which respects Doze mode - your wallpaper will still update daily even if you don't open the app

## License

This project is for personal use. Bible text from the King James Version and World English Bible are in the public domain.
