package com.dailyverse.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.dailyverse.app.data.model.BibleVerse
import com.dailyverse.app.data.model.DailyWallpaper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [BibleVerse::class, DailyWallpaper::class],
    version = 1,
    exportSchema = false
)
abstract class BibleDatabase : RoomDatabase() {

    abstract fun bibleVerseDao(): BibleVerseDao

    companion object {
        const val DATABASE_NAME = "dailyverse_bible.db"

        @Volatile
        private var INSTANCE: BibleDatabase? = null

        fun getInstance(context: Context): BibleDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): BibleDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                BibleDatabase::class.java,
                DATABASE_NAME
            )
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Pre-populate with KJV data will be handled by repository
                    }
                })
                .build()
        }
    }
}
