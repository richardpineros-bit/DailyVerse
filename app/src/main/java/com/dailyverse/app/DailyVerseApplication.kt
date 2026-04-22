package com.dailyverse.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.dailyverse.app.data.BibleRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class DailyVerseApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var bibleRepository: BibleRepository

    override fun onCreate() {
        super.onCreate()

        // Pre-load bundled Bible data on first launch
        CoroutineScope(Dispatchers.IO).launch {
            if (!bibleRepository.isDataLoaded()) {
                bibleRepository.loadBundledKjvData()
            }
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
}
