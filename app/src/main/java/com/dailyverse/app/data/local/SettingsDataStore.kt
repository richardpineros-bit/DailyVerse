package com.dailyverse.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dailyverse.app.data.model.AppMode
import com.dailyverse.app.data.model.BibleVersion
import com.dailyverse.app.data.model.FontSize
import com.dailyverse.app.data.model.FontStyle
import com.dailyverse.app.data.model.GradientTheme
import com.dailyverse.app.data.model.ImageSource
import com.dailyverse.app.data.model.ImageSourceType
import com.dailyverse.app.data.model.Unsplash4KCategory
import com.dailyverse.app.data.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "dailyverse_settings")

@Singleton
class SettingsDataStore @Inject constructor(
    private val context: Context
) {
    private object PreferencesKeys {
        val APP_MODE = stringPreferencesKey("app_mode")
        val BIBLE_VERSION = stringPreferencesKey("bible_version")
        val IMAGE_SOURCE_TYPE = stringPreferencesKey("image_source_type")
        val UNSPLASH_4K_CATEGORY = stringPreferencesKey("unsplash_4k_category")
        val IMAGE_GRADIENT = stringPreferencesKey("image_gradient")
        val PEXELS_SEARCH_QUERY = stringPreferencesKey("pexels_search_query")
        val UPDATE_HOUR = intPreferencesKey("update_hour")
        val UPDATE_MINUTE = intPreferencesKey("update_minute")
        val VERSES_PER_DAY = intPreferencesKey("verses_per_day")
        val FONT_SIZE = stringPreferencesKey("font_size")
        val FONT_STYLE = stringPreferencesKey("font_style")
        val SHOW_REFERENCE = booleanPreferencesKey("show_reference")
        val USE_DARK_OVERLAY = booleanPreferencesKey("use_dark_overlay")
        val SEND_NOTIFICATION = booleanPreferencesKey("send_notification")
        val MEMORIZATION_BOOK = stringPreferencesKey("memorization_book")
        val MEMORIZATION_CHAPTER = intPreferencesKey("memorization_chapter")
        val LAST_SHOWN_VERSE = intPreferencesKey("last_shown_verse")
        val WALLPAPER_ENABLED = booleanPreferencesKey("wallpaper_enabled")
    }

    val settingsFlow: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        UserSettings(
            appMode = AppMode.valueOf(
                prefs[PreferencesKeys.APP_MODE] ?: AppMode.DAILY_INSPIRATION.name
            ),
            bibleVersion = BibleVersion.valueOf(
                prefs[PreferencesKeys.BIBLE_VERSION] ?: BibleVersion.KJV.name
            ),
            imageSource = parseImageSource(prefs),
            updateHour = prefs[PreferencesKeys.UPDATE_HOUR] ?: 6,
            updateMinute = prefs[PreferencesKeys.UPDATE_MINUTE] ?: 0,
            versesPerDay = prefs[PreferencesKeys.VERSES_PER_DAY] ?: 1,
            fontSize = FontSize.valueOf(
                prefs[PreferencesKeys.FONT_SIZE] ?: FontSize.MEDIUM.name
            ),
            fontStyle = FontStyle.valueOf(
                prefs[PreferencesKeys.FONT_STYLE] ?: FontStyle.SERIF.name
            ),
            showReference = prefs[PreferencesKeys.SHOW_REFERENCE] ?: true,
            useDarkOverlay = prefs[PreferencesKeys.USE_DARK_OVERLAY] ?: true,
            sendNotification = prefs[PreferencesKeys.SEND_NOTIFICATION] ?: true,
            memorizationBook = prefs[PreferencesKeys.MEMORIZATION_BOOK] ?: "",
            memorizationChapter = prefs[PreferencesKeys.MEMORIZATION_CHAPTER] ?: 1,
            lastShownVerse = prefs[PreferencesKeys.LAST_SHOWN_VERSE] ?: 0,
            wallpaperEnabled = prefs[PreferencesKeys.WALLPAPER_ENABLED] ?: true
        )
    }

    suspend fun updateSettings(settings: UserSettings) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.APP_MODE] = settings.appMode.name
            prefs[PreferencesKeys.BIBLE_VERSION] = settings.bibleVersion.name
            prefs[PreferencesKeys.IMAGE_SOURCE_TYPE] = settings.imageSource.type.name
            prefs[PreferencesKeys.UNSPLASH_4K_CATEGORY] = settings.imageSource.unsplashCategory?.name
                ?: Unsplash4KCategory.NATURE_4K.name
            prefs[PreferencesKeys.IMAGE_GRADIENT] = settings.imageSource.gradientTheme?.name
                ?: GradientTheme.PURPLE_DUSK.name
            prefs[PreferencesKeys.PEXELS_SEARCH_QUERY] = settings.imageSource.pexelsSearchQuery ?: "nature"
            prefs[PreferencesKeys.UPDATE_HOUR] = settings.updateHour
            prefs[PreferencesKeys.UPDATE_MINUTE] = settings.updateMinute
            prefs[PreferencesKeys.VERSES_PER_DAY] = settings.versesPerDay
            prefs[PreferencesKeys.FONT_SIZE] = settings.fontSize.name
            prefs[PreferencesKeys.FONT_STYLE] = settings.fontStyle.name
            prefs[PreferencesKeys.SHOW_REFERENCE] = settings.showReference
            prefs[PreferencesKeys.USE_DARK_OVERLAY] = settings.useDarkOverlay
            prefs[PreferencesKeys.SEND_NOTIFICATION] = settings.sendNotification
            prefs[PreferencesKeys.MEMORIZATION_BOOK] = settings.memorizationBook
            prefs[PreferencesKeys.MEMORIZATION_CHAPTER] = settings.memorizationChapter
            prefs[PreferencesKeys.LAST_SHOWN_VERSE] = settings.lastShownVerse
            prefs[PreferencesKeys.WALLPAPER_ENABLED] = settings.wallpaperEnabled
        }
    }

    suspend fun updateLastShownVerse(verseNumber: Int) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.LAST_SHOWN_VERSE] = verseNumber
        }
    }

    suspend fun resetMemorizationProgress() {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.LAST_SHOWN_VERSE] = 0
        }
    }

    private fun parseImageSource(prefs: Preferences): ImageSource {
        val type = ImageSourceType.valueOf(
            prefs[PreferencesKeys.IMAGE_SOURCE_TYPE] ?: ImageSourceType.UNSPLASH_4K.name
        )
        return when (type) {
            ImageSourceType.UNSPLASH_4K -> ImageSource(
                type = type,
                unsplashCategory = Unsplash4KCategory.valueOf(
                    prefs[PreferencesKeys.UNSPLASH_4K_CATEGORY] ?: Unsplash4KCategory.NATURE_4K.name
                )
            )
            ImageSourceType.PEXELS_CUSTOM -> ImageSource(
                type = type,
                pexelsSearchQuery = prefs[PreferencesKeys.PEXELS_SEARCH_QUERY] ?: "nature"
            )
            ImageSourceType.GRADIENT -> ImageSource(
                type = type,
                gradientTheme = GradientTheme.valueOf(
                    prefs[PreferencesKeys.IMAGE_GRADIENT] ?: GradientTheme.PURPLE_DUSK.name
                )
            )
            ImageSourceType.SOLID_COLOR -> ImageSource(type = type)
            ImageSourceType.USER_GALLERY -> ImageSource(type = type)
        }
    }
}
