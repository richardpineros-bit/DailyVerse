package com.dailyverse.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bible_verses")
data class BibleVerse(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val book: String,
    val bookOrder: Int,
    val chapter: Int,
    val verse: Int,
    val text: String,
    val version: String = "KJV",
    val isFavorite: Boolean = false
)

data class VerseLocation(
    val book: String,
    val chapter: Int,
    val verse: Int
)

data class BookInfo(
    val name: String,
    val order: Int,
    val testament: Testament,
    val totalChapters: Int
)

enum class Testament {
    OLD, NEW
}

data class ChapterInfo(
    val book: String,
    val chapter: Int,
    val totalVerses: Int
)

data class MemorizationProgress(
    val book: String,
    val chapter: Int,
    val currentVerse: Int,
    val totalVersesInChapter: Int,
    val versesPerDay: Int,
    val versesLearned: Int,
    val percentageComplete: Float,
    val isComplete: Boolean
)
