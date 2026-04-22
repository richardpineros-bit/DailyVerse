package com.dailyverse.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dailyverse.app.data.BibleRepository
import com.dailyverse.app.data.SettingsRepository
import com.dailyverse.app.data.model.AppMode
import com.dailyverse.app.data.model.BibleVersion
import com.dailyverse.app.data.model.FontSize
import com.dailyverse.app.data.model.FontStyle
import com.dailyverse.app.data.model.GradientTheme
import com.dailyverse.app.data.model.ImageSource
import com.dailyverse.app.data.model.ImageSourceType
import com.dailyverse.app.data.model.Unsplash4KCategory
import com.dailyverse.app.data.model.UserSettings
import com.dailyverse.app.worker.DailyWallpaperWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    application: Application,
    private val settingsRepository: SettingsRepository,
    private val bibleRepository: BibleRepository
) : AndroidViewModel(application) {

    private val _settings = MutableStateFlow(UserSettings())
    val settings: StateFlow<UserSettings> = _settings

    private val _availableBooks = MutableStateFlow<List<String>>(emptyList())
    val availableBooks: StateFlow<List<String>> = _availableBooks

    private val _availableChapters = MutableStateFlow<List<Int>>(emptyList())
    val availableChapters: StateFlow<List<Int>> = _availableChapters

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess

    init {
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect {
                _settings.value = it
                if (it.memorizationBook.isNotEmpty()) {
                    loadChaptersForBook(it.memorizationBook)
                }
            }
        }

        loadBooks()
    }

    private fun loadBooks() {
        viewModelScope.launch {
            _availableBooks.value = com.dailyverse.app.util.BibleBookUtil.ALL_BOOKS
        }
    }

    fun loadChaptersForBook(book: String) {
        viewModelScope.launch {
            val count = com.dailyverse.app.util.BibleBookUtil.getChapterCount(book)
            _availableChapters.value = (1..count).toList()
        }
    }

    fun updateMode(mode: AppMode) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(appMode = mode))
        }
    }

    fun updateBibleVersion(version: BibleVersion) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(bibleVersion = version))
        }
    }

    fun updateImageSource(source: ImageSource) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(imageSource = source))
        }
    }

    fun updateUnsplash4KCategory(category: Unsplash4KCategory) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(
                current.copy(
                    imageSource = ImageSource(
                        type = ImageSourceType.UNSPLASH_4K,
                        unsplashCategory = category
                    )
                )
            )
        }
    }

    fun updatePexelsSearchQuery(query: String) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(
                current.copy(
                    imageSource = ImageSource(
                        type = ImageSourceType.PEXELS_CUSTOM,
                        pexelsSearchQuery = query.trim()
                    )
                )
            )
        }
    }

    fun updateGradientTheme(theme: GradientTheme) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(
                current.copy(
                    imageSource = ImageSource(
                        type = ImageSourceType.GRADIENT,
                        gradientTheme = theme
                    )
                )
            )
        }
    }

    fun updateUpdateTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(
                current.copy(updateHour = hour, updateMinute = minute)
            )
            if (current.wallpaperEnabled) {
                DailyWallpaperWorker.schedule(getApplication(), hour, minute)
            }
        }
    }

    fun updateVersesPerDay(count: Int) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(versesPerDay = count.coerceIn(1, 3)))
        }
    }

    fun updateFontSize(size: FontSize) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(fontSize = size))
        }
    }

    fun updateFontStyle(style: FontStyle) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(fontStyle = style))
        }
    }

    fun updateShowReference(show: Boolean) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(showReference = show))
        }
    }

    fun updateDarkOverlay(use: Boolean) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(useDarkOverlay = use))
        }
    }

    fun updateNotification(send: Boolean) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(current.copy(sendNotification = send))
        }
    }

    fun updateMemorizationBook(book: String) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(
                current.copy(
                    memorizationBook = book,
                    memorizationChapter = 1,
                    lastShownVerse = 0
                )
            )
            loadChaptersForBook(book)
        }
    }

    fun updateMemorizationChapter(chapter: Int) {
        viewModelScope.launch {
            val current = settings.value
            settingsRepository.updateSettings(
                current.copy(
                    memorizationChapter = chapter,
                    lastShownVerse = 0
                )
            )
        }
    }

    fun resetMemorization() {
        viewModelScope.launch {
            settingsRepository.resetMemorizationProgress()
        }
    }

    fun saveSettings() {
        _saveSuccess.value = true
        viewModelScope.launch {
            kotlinx.coroutines.delay(2000)
            _saveSuccess.value = false
        }
    }
}
