package com.dailyverse.app.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dailyverse.app.data.BibleRepository
import com.dailyverse.app.data.ImageRepository
import com.dailyverse.app.data.SettingsRepository
import com.dailyverse.app.data.model.AppMode
import com.dailyverse.app.data.model.BibleVerse
import com.dailyverse.app.data.model.MemorizationProgress
import com.dailyverse.app.data.model.UserSettings
import com.dailyverse.app.worker.DailyWallpaperWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application,
    private val bibleRepository: BibleRepository,
    private val imageRepository: ImageRepository,
    private val settingsRepository: SettingsRepository
) : AndroidViewModel(application) {

    private val _currentVerses = mutableStateOf<List<BibleVerse>>(emptyList())
    val currentVerses: State<List<BibleVerse>> = _currentVerses

    private val _currentWallpaper = mutableStateOf<Bitmap?>(null)
    val currentWallpaper: State<Bitmap?> = _currentWallpaper

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _error = mutableStateOf<String?>(null)
    val error: State<String?> = _error

    private val _memorizationProgress = mutableStateOf<MemorizationProgress?>(null)
    val memorizationProgress: State<MemorizationProgress?> = _memorizationProgress

    private val _settings = MutableStateFlow(UserSettings())
    val settings: StateFlow<UserSettings> = _settings

    init {
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect {
                _settings.value = it
            }
        }
        loadDailyVerse()
    }

    fun loadDailyVerse() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val currentSettings = settings.value

                if (!bibleRepository.isDataLoaded()) {
                    bibleRepository.loadBundledKjvData()
                }

                val verses = when (currentSettings.appMode) {
                    AppMode.DAILY_INSPIRATION -> {
                        val verse = bibleRepository.getRandomVerse(currentSettings.bibleVersion)
                        verse?.let { listOf(it) } ?: emptyList()
                    }
                    AppMode.MEMORIZATION -> {
                        if (currentSettings.memorizationBook.isEmpty()) {
                            bibleRepository.getRandomVerse(currentSettings.bibleVersion)?.let { listOf(it) } ?: emptyList()
                        } else {
                            val chapterVerses = bibleRepository.getChapterVerses(
                                currentSettings.memorizationBook,
                                currentSettings.memorizationChapter,
                                currentSettings.bibleVersion
                            )

                            if (chapterVerses.isEmpty()) {
                                bibleRepository.getVersesForMemorization(
                                    currentSettings.memorizationBook,
                                    currentSettings.memorizationChapter,
                                    currentSettings.lastShownVerse + 1,
                                    currentSettings.versesPerDay,
                                    currentSettings.bibleVersion
                                )
                            } else {
                                val startIdx = currentSettings.lastShownVerse.coerceIn(0, chapterVerses.size - 1)
                                val endIdx = (startIdx + currentSettings.versesPerDay - 1).coerceAtMost(chapterVerses.size - 1)
                                chapterVerses.subList(startIdx, endIdx + 1)
                            }
                        }
                    }
                }

                _currentVerses.value = verses

                // Update progress
                if (currentSettings.appMode == AppMode.MEMORIZATION &&
                    currentSettings.memorizationBook.isNotEmpty()
                ) {
                    loadMemorizationProgress()
                }

            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun generateWallpaper() {
        viewModelScope.launch {
            if (_currentVerses.value.isEmpty()) return@launch

            _isLoading.value = true
            try {
                val currentSettings = settings.value
                val verses = _currentVerses.value
                val verseText = verses.joinToString(" ") { it.text }
                val reference = if (verses.size == 1) {
                    com.dailyverse.app.util.BibleBookUtil.formatReference(
                        verses[0].book, verses[0].chapter, verses[0].verse
                    )
                } else {
                    com.dailyverse.app.util.BibleBookUtil.formatReferenceRange(
                        verses[0].book, verses[0].chapter,
                        verses[0].verse, verses.last().verse
                    )
                }

                val imageResult = imageRepository.fetchImage(currentSettings.imageSource)
                if (imageResult.isSuccess) {
                    val fetchResult = imageResult.getOrThrow()
                    val wallpaper = imageRepository.compositeVerseOnImage(
                        fetchResult.bitmap,
                        verseText,
                        reference,
                        currentSettings
                    )
                    _currentWallpaper.value = wallpaper
                    imageRepository.saveWallpaper(wallpaper)
                } else {
                    _error.value = "Failed to fetch image"
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun applyWallpaper() {
        viewModelScope.launch {
            _currentWallpaper.value?.let { bitmap ->
                try {
                    val wallpaperManager = android.app.WallpaperManager.getInstance(getApplication())
                    wallpaperManager.setBitmap(bitmap, null, true, android.app.WallpaperManager.FLAG_LOCK)
                } catch (e: Exception) {
                    try {
                        val wallpaperManager = android.app.WallpaperManager.getInstance(getApplication())
                        wallpaperManager.setBitmap(bitmap)
                    } catch (e2: Exception) {
                        _error.value = "Failed to set wallpaper: ${e2.message}"
                    }
                }
            }
        }
    }

    fun loadMemorizationProgress() {
        viewModelScope.launch {
            val currentSettings = settings.value
            if (currentSettings.memorizationBook.isEmpty()) return@launch

            val progress = bibleRepository.getMemorizationProgress(
                currentSettings.memorizationBook,
                currentSettings.memorizationChapter,
                currentSettings.lastShownVerse,
                currentSettings.versesPerDay
            )
            _memorizationProgress.value = progress
        }
    }

    fun advanceMemorization() {
        viewModelScope.launch {
            val currentSettings = settings.value
            if (currentSettings.memorizationBook.isEmpty()) return@launch

            val currentLast = currentSettings.lastShownVerse
            val chapterCount = com.dailyverse.app.util.BibleBookUtil.getChapterCount(
                currentSettings.memorizationBook
            )
            val totalVerses = bibleRepository.getVerseCount(
                currentSettings.memorizationBook,
                currentSettings.memorizationChapter
            )

            val newLastVerse = currentLast + currentSettings.versesPerDay

            if (newLastVerse >= totalVerses) {
                // Chapter complete - move to next chapter or mark complete
                if (currentSettings.memorizationChapter < chapterCount) {
                    settingsRepository.updateSettings(
                        currentSettings.copy(
                            memorizationChapter = currentSettings.memorizationChapter + 1,
                            lastShownVerse = 0
                        )
                    )
                } else {
                    settingsRepository.updateSettings(
                        currentSettings.copy(lastShownVerse = totalVerses)
                    )
                }
            } else {
                settingsRepository.updateLastShownVerse(newLastVerse)
            }

            loadDailyVerse()
        }
    }

    fun refreshImage() {
        generateWallpaper()
    }

    fun setWallpaperEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val currentSettings = settings.value
            settingsRepository.updateSettings(currentSettings.copy(wallpaperEnabled = enabled))

            if (enabled) {
                DailyWallpaperWorker.schedule(
                    getApplication(),
                    currentSettings.updateHour,
                    currentSettings.updateMinute
                )
            } else {
                DailyWallpaperWorker.cancel(getApplication())
            }
        }
    }
}
