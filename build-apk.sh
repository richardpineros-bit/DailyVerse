#!/bin/bash
# DailyVerse APK Builder Script
# Usage: ./build-apk.sh [debug|release]
# Requires: JDK 17+, Android SDK

set -e

BUILD_TYPE=${1:-"debug"}

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}=== DailyVerse APK Builder ===${NC}"
echo ""

# Check Java
echo -e "${YELLOW}Checking Java...${NC}"
if ! command -v java &> /dev/null; then
    echo -e "${RED}Error: Java not found. Install JDK 17+${NC}"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | cut -d'.' -f1-2)
echo "  Java version: $JAVA_VERSION"

# Check Android SDK
echo -e "${YELLOW}Checking Android SDK...${NC}"
if [ -z "$ANDROID_HOME" ] && [ -z "$ANDROID_SDK_ROOT" ]; then
    echo -e "${YELLOW}  Warning: ANDROID_HOME not set${NC}"
    echo "  Trying to find Android SDK..."
    
    # Common locations
    COMMON_SDK_PATHS=(
        "$HOME/Android/Sdk"
        "$HOME/Library/Android/sdk"
        "/usr/local/android-sdk"
        "/opt/android-sdk"
        "/mnt/agents/output/android-sdk"
    )
    
    for SDK_PATH in "${COMMON_SDK_PATHS[@]}"; do
        if [ -d "$SDK_PATH" ]; then
            export ANDROID_HOME="$SDK_PATH"
            echo "  Found: $ANDROID_HOME"
            break
        fi
    done
    
    if [ -z "$ANDROID_HOME" ]; then
        echo -e "${RED}Error: Android SDK not found.${NC}"
        echo ""
        echo "Options:"
        echo "  1. Install Android Studio (recommended)"
        echo "  2. Download command line tools from:"
        echo "     https://developer.android.com/studio#command-tools"
        echo "  3. Use Docker: docker run --rm -v \"\$(pwd):/project\" mingc/android-build-box bash -c \"cd /project && ./gradlew assembleDebug\""
        echo "  4. Use GitHub Actions (see README.md)"
        exit 1
    fi
fi

export ANDROID_SDK_ROOT="${ANDROID_HOME:-$ANDROID_SDK_ROOT}"
echo "  ANDROID_HOME: $ANDROID_HOME"

# Check for required SDK components
echo -e "${YELLOW}Checking SDK components...${NC}"
if [ ! -d "$ANDROID_HOME/platforms/android-34" ]; then
    echo "  Android API 34 not found. Installing..."
    if command -v sdkmanager &> /dev/null; then
        sdkmanager "platforms;android-34" "build-tools;34.0.0"
    else
        echo -e "${RED}  Error: sdkmanager not found. Cannot auto-install.${NC}"
        echo "  Please install via Android Studio or sdkmanager."
        exit 1
    fi
fi
echo "  SDK components OK"

# Make gradlew executable
echo -e "${YELLOW}Setting up Gradle...${NC}"
chmod +x gradlew

# Check API keys
echo -e "${YELLOW}Checking API keys...${NC}"
if grep -q "YOUR_UNSPLASH_ACCESS_KEY" app/build.gradle.kts; then
    echo -e "${RED}  Warning: Unsplash API key is still the placeholder!${NC}"
    echo "  Edit app/build.gradle.kts and replace YOUR_UNSPLASH_ACCESS_KEY"
fi
if grep -q "YOUR_PEXELS_API_KEY" app/build.gradle.kts; then
    echo -e "${RED}  Warning: Pexels API key is still the placeholder!${NC}"
    echo "  Edit app/build.gradle.kts and replace YOUR_PEXELS_API_KEY"
fi

# Build
echo ""
echo -e "${GREEN}Building ${BUILD_TYPE} APK...${NC}"
echo ""

if [ "$BUILD_TYPE" = "release" ]; then
    ./gradlew assembleRelease
    APK_PATH="app/build/outputs/apk/release/app-release-unsigned.apk"
else
    ./gradlew assembleDebug
    APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
fi

# Check if build succeeded
if [ -f "$APK_PATH" ]; then
    echo ""
    echo -e "${GREEN}Build successful!${NC}"
    echo -e "${GREEN}APK location: $(pwd)/$APK_PATH${NC}"
    ls -lh "$APK_PATH"
    echo ""
    echo "Install on your device:"
    echo "  adb install -r $(pwd)/$APK_PATH"
else
    echo -e "${RED}Build failed. Check the error messages above.${NC}"
    exit 1
fi
