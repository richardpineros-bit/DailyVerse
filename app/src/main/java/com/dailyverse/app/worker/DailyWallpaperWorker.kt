package com.dailyverse.app.worker

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.dailyverse.app.data.BibleRepository
import com.dailyverse.app.data.ImageRepository
import com.dailyverse.app.data.SettingsRepository
import com.dailyverse.app.data.local.BibleDatabase
import com.dailyverse.app.data.model.AppMode
import com.dailyverse.app.data.model.BibleVersion
import com.dailyverse.app.util.BibleBookUtil
import com.dailyverse.app.util.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.io.File
import java.util.Calendar
import java.util.concurrent.TimeUnit

@HiltWorker
class DailyWallpaperWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val bibleRepository: BibleRepository,
    private val imageRepository: ImageRepository,
    private val settingsRepository: SettingsRepository,
    private val notificationHelper: NotificationHelper,
    private val bibleDatabase: BibleDatabase
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "daily_wallpaper_work"
        const val KEY_RESULT = "result"
        const val KEY_VERSE = "verse_text"
        const val KEY_REFERENCE = "verse_reference"

        fun schedule(context: Context, hour: Int, minute: Int) {
            val workManager = WorkManager.getInstance(context)

            // Calculate delay until next occurrence
            val now = Calendar.getInstance()
            val scheduled = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)

                if (before(now)) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            val initialDelay = scheduled.timeInMillis - now.timeInMillis

            val workRequest = OneTimeWorkRequestBuilder<DailyWallpaperWorker>()
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .addTag(WORK_NAME)
                .build()

            workManager.enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }

        fun scheduleNext(context: Context, hour: Int, minute: Int) {
            // Schedule the next day's work after completing current one
            val workManager = WorkManager.getInstance(context)
            val now = Calendar.getInstance()
            val nextDay = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val delay = nextDay.timeInMillis - now.timeInMillis

            val workRequest = OneTimeWorkRequestBuilder<DailyWallpaperWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .addTag(WORK_NAME)
                .build()

            workManager.enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
        }
    }

    override suspend fun doWork(): Result {
        return try {
            val settings = settingsRepository.settingsFlow.first()

            if (!settings.wallpaperEnabled) {
                return Result.success()
            }

            // Ensure data is loaded
            if (!bibleRepository.isDataLoaded()) {
                bibleRepository.loadBundledKjvData()
            }

            // Get verse based on mode
            val verses = when (settings.appMode) {
                AppMode.DAILY_INSPIRATION -> {
                    val verse = bibleRepository.getRandomVerse(settings.bibleVersion)
                    verse?.let { listOf(it) } ?: emptyList()
                }
                AppMode.MEMORIZATION -> {
                    if (settings.memorizationBook.isEmpty()) {
                        bibleRepository.getRandomVerse(settings.bibleVersion)?.let { listOf(it) } ?: emptyList()
                    } else {
                        val lastVerse = settings.lastShownVerse
                        val chapterVerses = bibleRepository.getChapterVerses(
                            settings.memorizationBook,
                            settings.memorizationChapter,
                            settings.bibleVersion
                        )

                        if (chapterVerses.isEmpty()) {
                            // Fallback to bundled verses for this book/chapter
                            getBundledVersesForMemorization(
                                settings.memorizationBook,
                                settings.memorizationChapter,
                                lastVerse + 1,
                                settings.versesPerDay
                            )
                        } else {
                            val startIdx = lastVerse.coerceIn(0, chapterVerses.size - 1)
                            val endIdx = (startIdx + settings.versesPerDay - 1).coerceAtMost(chapterVerses.size - 1)
                            chapterVerses.subList(startIdx, endIdx + 1)
                        }
                    }
                }
            }

            if (verses.isEmpty()) {
                return Result.retry()
            }

            // Update memorization progress
            if (settings.appMode == AppMode.MEMORIZATION && settings.memorizationBook.isNotEmpty()) {
                val newLastVerse = verses.last().verse
                settingsRepository.updateLastShownVerse(newLastVerse)
            }

            // Build verse text and reference
            val verseText = verses.joinToString(" ") { it.text }
            val reference = if (verses.size == 1) {
                BibleBookUtil.formatReference(verses[0].book, verses[0].chapter, verses[0].verse)
            } else {
                BibleBookUtil.formatReferenceRange(
                    verses[0].book, verses[0].chapter,
                    verses[0].verse, verses.last().verse
                )
            }

            // Fetch and composite image
            val imageResult = imageRepository.fetchImage(settings.imageSource)
            if (imageResult.isSuccess) {
                val imageFetch = imageResult.getOrThrow()
                val wallpaper = imageRepository.compositeVerseOnImage(
                    imageFetch.bitmap,
                    verseText,
                    reference,
                    settings
                )

                // Save wallpaper
                val savedPath = imageRepository.saveWallpaper(wallpaper)

                // Set as lock screen wallpaper
                setWallpaper(wallpaper)

                // Show notification
                if (settings.sendNotification) {
                    notificationHelper.showVerseReadyNotification(verseText, reference)
                }

                // Schedule next
                scheduleNext(applicationContext, settings.updateHour, settings.updateMinute)

                // Save daily record
                val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                    .format(java.util.Date())
                val imageUrl = imageFetch.unsplashPhoto?.urls?.regular
                    ?: imageFetch.pexelsPhoto?.src?.large2x
                val photographerName = imageFetch.unsplashPhoto?.user?.name
                    ?: imageFetch.pexelsPhoto?.photographer
                bibleDatabase.bibleVerseDao().insertDailyWallpaper(
                    com.dailyverse.app.data.model.DailyWallpaper(
                        date = today,
                        verseId = verses[0].id,
                        verseText = verseText,
                        verseReference = reference,
                        imageUrl = imageUrl,
                        photographerName = photographerName
                    )
                )

                Result.success(
                    Data.Builder()
                        .putString(KEY_VERSE, verseText)
                        .putString(KEY_REFERENCE, reference)
                        .putString("image_path", savedPath)
                        .build()
                )
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun setWallpaper(bitmap: Bitmap) {
        try {
            val wallpaperManager = WallpaperManager.getInstance(applicationContext)
            wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
        } catch (e: Exception) {
            // Fallback: try setting as both
            try {
                val wallpaperManager = WallpaperManager.getInstance(applicationContext)
                wallpaperManager.setBitmap(bitmap)
            } catch (_: Exception) {
            }
        }
    }

    private suspend fun getBundledVersesForMemorization(
        book: String,
        chapter: Int,
        startVerse: Int,
        count: Int
    ): List<com.dailyverse.app.data.model.BibleVerse> {
        return bibleRepository.getVersesForMemorization(
            book, chapter, startVerse, count, BibleVersion.KJV
        )
    }
}
