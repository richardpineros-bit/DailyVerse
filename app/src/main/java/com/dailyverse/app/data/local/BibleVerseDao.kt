package com.dailyverse.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.dailyverse.app.data.model.BibleVerse
import com.dailyverse.app.data.model.DailyWallpaper
import kotlinx.coroutines.flow.Flow

@Dao
interface BibleVerseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVerses(verses: List<BibleVerse>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVerse(verse: BibleVerse)

    @Query("SELECT * FROM bible_verses WHERE book = :book AND chapter = :chapter AND verse = :verseNum AND version = :version LIMIT 1")
    suspend fun getVerse(book: String, chapter: Int, verseNum: Int, version: String): BibleVerse?

    @Query("SELECT * FROM bible_verses WHERE book = :book AND chapter = :chapter AND verse BETWEEN :startVerse AND :endVerse AND version = :version ORDER BY verse")
    suspend fun getVersesInRange(book: String, chapter: Int, startVerse: Int, endVerse: Int, version: String): List<BibleVerse>

    @Query("SELECT * FROM bible_verses WHERE book = :book AND chapter = :chapter AND version = :version ORDER BY verse")
    suspend fun getChapterVerses(book: String, chapter: Int, version: String): List<BibleVerse>

    @Query("SELECT * FROM bible_verses WHERE version = :version ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomVerse(version: String): BibleVerse?

    @Query("SELECT DISTINCT book, bookOrder FROM bible_verses WHERE version = :version ORDER BY bookOrder")
    suspend fun getAllBooks(version: String): List<BookOrderTuple>

    @Query("SELECT COUNT(*) FROM bible_verses WHERE book = :book AND chapter = :chapter AND version = :version")
    suspend fun getVerseCount(book: String, chapter: Int, version: String): Int

    @Query("SELECT DISTINCT chapter FROM bible_verses WHERE book = :book AND version = :version ORDER BY chapter")
    suspend fun getChaptersForBook(book: String, version: String): List<Int>

    @Query("SELECT * FROM bible_verses WHERE isFavorite = 1 ORDER BY book, chapter, verse")
    fun getFavoriteVerses(): Flow<List<BibleVerse>>

    @Update
    suspend fun updateVerse(verse: BibleVerse)

    @Query("UPDATE bible_verses SET isFavorite = :isFavorite WHERE id = :verseId")
    suspend fun setFavorite(verseId: Int, isFavorite: Boolean)

    @Query("SELECT COUNT(*) FROM bible_verses WHERE version = :version")
    suspend fun getVerseCountForVersion(version: String): Int

    // Daily Wallpaper
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyWallpaper(wallpaper: DailyWallpaper)

    @Query("SELECT * FROM daily_wallpapers WHERE date = :date LIMIT 1")
    suspend fun getDailyWallpaper(date: String): DailyWallpaper?

    @Query("SELECT * FROM daily_wallpapers ORDER BY date DESC LIMIT :limit")
    fun getWallpaperHistory(limit: Int): Flow<List<DailyWallpaper>>

    @Query("UPDATE daily_wallpapers SET isFavorite = :isFavorite WHERE date = :date")
    suspend fun setWallpaperFavorite(date: String, isFavorite: Boolean)
}

data class BookOrderTuple(
    val book: String,
    val bookOrder: Int
)
