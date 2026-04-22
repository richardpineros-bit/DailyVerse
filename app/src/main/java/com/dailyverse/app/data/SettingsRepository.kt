package com.dailyverse.app.data

import com.dailyverse.app.data.local.SettingsDataStore
import com.dailyverse.app.data.model.UserSettings
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) {
    val settingsFlow: Flow<UserSettings> = settingsDataStore.settingsFlow

    suspend fun updateSettings(settings: UserSettings) {
        settingsDataStore.updateSettings(settings)
    }

    suspend fun updateLastShownVerse(verseNumber: Int) {
        settingsDataStore.updateLastShownVerse(verseNumber)
    }

    suspend fun resetMemorizationProgress() {
        settingsDataStore.resetMemorizationProgress()
    }
}
