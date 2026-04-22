package com.dailyverse.app.data.remote

import com.dailyverse.app.BuildConfig
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface BibleApi {

    /**
     * Get a specific verse from the Bible API (scripture.api.bible)
     * Using API.Bible service
     */
    @GET("bibles/{bibleId}/verses/{verseId}")
    suspend fun getVerse(
        @Path("bibleId") bibleId: String,
        @Path("verseId") verseId: String,
        @Query("api-key") apiKey: String = BuildConfig.BIBLE_API_KEY,
        @Query("content-type") contentType: String = "text",
        @Query("include-notes") includeNotes: Boolean = false,
        @Query("include-titles") includeTitles: Boolean = true
    ): BibleApiResponse

    /**
     * Get a chapter
     */
    @GET("bibles/{bibleId}/chapters/{chapterId}")
    suspend fun getChapter(
        @Path("bibleId") bibleId: String,
        @Path("chapterId") chapterId: String,
        @Query("api-key") apiKey: String = BuildConfig.BIBLE_API_KEY
    ): ChapterResponse

    /**
     * Search for verses
     */
    @GET("bibles/{bibleId}/search")
    suspend fun searchVerses(
        @Path("bibleId") bibleId: String,
        @Query("query") query: String,
        @Query("api-key") apiKey: String = BuildConfig.BIBLE_API_KEY,
        @Query("limit") limit: Int = 1
    ): SearchResponse

    companion object {
        const val BASE_URL = "https://api.scripture.api.bible/v1/"

        // Bible IDs for different versions
        const val BIBLE_ID_KJV = "de4e12af7f28f599-01"
        const val BIBLE_ID_NIV = "71c6eab17ae5b550-01"
        const val BIBLE_ID_ESV = "f421fe261da7624f-01"
        const val BIBLE_ID_NLT = "5e6cf49d38ee17e0-01"
        const val BIBLE_ID_NKJV = "0034e02e963d960d-01"
        const val BIBLE_ID_WEB = "9879dbb7cfe39e4d-01"
    }
}

data class BibleApiResponse(
    val data: VerseData
)

data class VerseData(
    val id: String,
    val orgId: String,
    val bookId: String,
    val bibleId: String,
    val chapterId: String,
    val content: String,
    val reference: String,
    val verseCount: Int,
    val copyright: String
)

data class ChapterResponse(
    val data: ChapterData
)

data class ChapterData(
    val id: String,
    val bibleId: String,
    val number: String,
    val bookId: String,
    val content: String,
    val reference: String,
    val verseCount: Int,
    val copyright: String
)

data class SearchResponse(
    val data: SearchData
)

data class SearchData(
    val verses: List<SearchVerse>,
    val versesCount: Int
)

data class SearchVerse(
    val id: String,
    val orgId: String,
    val bookId: String,
    val bibleId: String,
    val chapterId: String,
    val text: String,
    val reference: String
)
