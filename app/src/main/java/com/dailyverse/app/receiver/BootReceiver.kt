package com.dailyverse.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dailyverse.app.worker.DailyWallpaperWorker
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Reschedule the daily wallpaper worker on boot
            // Use default time of 6:00 AM; actual settings will be picked up by the worker
            DailyWallpaperWorker.schedule(context, 6, 0)
        }
    }
}
