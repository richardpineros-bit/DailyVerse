package com.dailyverse.app.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface UnsplashApi {

    @GET("photos/random")
    suspend fun getRandomPhoto(
        @Query("query") query: String,
        @Query("orientation") orientation: String = "portrait",
        @Query("content_filter") contentFilter: String = "high"
    ): UnsplashPhoto

    @GET("photos/random")
    suspend fun getRandomPhotoLandscape(
        @Query("query") query: String,
        @Query("orientation") orientation: String = "landscape",
        @Query("content_filter") contentFilter: String = "high"
    ): UnsplashPhoto
}

data class UnsplashPhoto(
    val id: String,
    val urls: UnsplashUrls,
    val user: UnsplashUser,
    val links: UnsplashLinks,
    val description: String?,
    val alt_description: String?
)

data class UnsplashUrls(
    val raw: String,
    val full: String,
    val regular: String,
    val small: String,
    val thumb: String
)

data class UnsplashUser(
    val id: String,
    val username: String,
    val name: String,
    val portfolio_url: String?,
    val links: UnsplashUserLinks
)

data class UnsplashUserLinks(
    val html: String
)

data class UnsplashLinks(
    val html: String,
    val download_location: String
)
