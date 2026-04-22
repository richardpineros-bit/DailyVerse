package com.dailyverse.app.data.remote

import com.dailyverse.app.BuildConfig
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface PexelsApi {

    @GET("v1/search")
    suspend fun searchPhotos(
        @Header("Authorization") apiKey: String = BuildConfig.PEXELS_API_KEY,
        @Query("query") query: String,
        @Query("per_page") perPage: Int = 1,
        @Query("orientation") orientation: String = "portrait"
    ): PexelsSearchResponse

    @GET("v1/search")
    suspend fun searchPhotosLandscape(
        @Header("Authorization") apiKey: String = BuildConfig.PEXELS_API_KEY,
        @Query("query") query: String,
        @Query("per_page") perPage: Int = 1,
        @Query("orientation") orientation: String = "landscape"
    ): PexelsSearchResponse

    @GET("v1/curated")
    suspend fun getCurated(
        @Header("Authorization") apiKey: String = BuildConfig.PEXELS_API_KEY,
        @Query("per_page") perPage: Int = 1
    ): PexelsSearchResponse
}

data class PexelsSearchResponse(
    val total_results: Int,
    val page: Int,
    val per_page: Int,
    val photos: List<PexelsPhoto>
)

data class PexelsPhoto(
    val id: Long,
    val width: Int,
    val height: Int,
    val url: String,
    val photographer: String,
    val photographer_url: String,
    val src: PexelsPhotoSrc,
    val alt: String?
)

data class PexelsPhotoSrc(
    val original: String,
    val large2x: String,
    val large: String,
    val medium: String,
    val small: String,
    val portrait: String,
    val landscape: String,
    val tiny: String
)
