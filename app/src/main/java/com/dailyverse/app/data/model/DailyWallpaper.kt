package com.dailyverse.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_wallpapers")
data class DailyWallpaper(
    @PrimaryKey
    val date: String,
    val verseId: Int,
    val verseText: String,
    val verseReference: String,
    val imageUrl: String? = null,
    val localImagePath: String? = null,
    val photographerName: String? = null,
    val photographerUrl: String? = null,
    val isFavorite: Boolean = false
)
