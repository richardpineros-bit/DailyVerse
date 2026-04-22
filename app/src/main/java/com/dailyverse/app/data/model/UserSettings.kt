package com.dailyverse.app.data.model

data class UserSettings(
    val appMode: AppMode = AppMode.DAILY_INSPIRATION,
    val bibleVersion: BibleVersion = BibleVersion.KJV,
    val imageSource: ImageSource = ImageSource(
        type = ImageSourceType.UNSPLASH_4K,
        unsplashCategory = Unsplash4KCategory.NATURE_4K
    ),
    val updateHour: Int = 6,
    val updateMinute: Int = 0,
    val versesPerDay: Int = 1,
    val fontSize: FontSize = FontSize.MEDIUM,
    val fontStyle: FontStyle = FontStyle.SERIF,
    val showReference: Boolean = true,
    val useDarkOverlay: Boolean = true,
    val sendNotification: Boolean = true,
    val memorizationBook: String = "",
    val memorizationChapter: Int = 1,
    val lastShownVerse: Int = 0,
    val wallpaperEnabled: Boolean = true
)

enum class FontSize(val displayName: String, val scale: Float) {
    SMALL("Small", 0.8f),
    MEDIUM("Medium", 1.0f),
    LARGE("Large", 1.2f),
    EXTRA_LARGE("Extra Large", 1.5f);

    companion object {
        fun fromDisplayName(name: String): FontSize {
            return entries.find { it.displayName == name } ?: MEDIUM
        }
    }
}

enum class FontStyle(val displayName: String, val fontName: String) {
    SERIF("Serif", "serif"),
    SANS_SERIF("Sans Serif", "sans-serif"),
    SCRIPT("Script", "cursive"),
    MODERN("Modern", "sans-serif-light");

    companion object {
        fun fromDisplayName(name: String): FontStyle {
            return entries.find { it.displayName == name } ?: SERIF
        }
    }
}
