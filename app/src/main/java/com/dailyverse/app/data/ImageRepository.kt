package com.dailyverse.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.provider.MediaStore
import coil.imageLoader
import coil.request.ImageRequest
import com.dailyverse.app.data.model.GradientTheme
import com.dailyverse.app.data.model.ImageSource
import com.dailyverse.app.data.model.ImageSourceType
import com.dailyverse.app.data.model.Unsplash4KCategory
import com.dailyverse.app.data.model.UserSettings
import com.dailyverse.app.data.remote.NetworkModule
import com.dailyverse.app.data.remote.PexelsPhoto
import com.dailyverse.app.data.remote.UnsplashPhoto
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val networkModule: NetworkModule
) {
    private val unsplashApi by lazy { networkModule.unsplashApi }
    private val pexelsApi by lazy { networkModule.pexelsApi }

    /**
     * Fetch an image based on the user's image source settings.
     * Returns a Bitmap ready for wallpaper compositing.
     */
    suspend fun fetchImage(imageSource: ImageSource): Result<ImageFetchResult> = withContext(Dispatchers.IO) {
        try {
            when (imageSource.type) {
                ImageSourceType.UNSPLASH_4K -> {
                    val category = imageSource.unsplashCategory ?: Unsplash4KCategory.NATURE_4K
                    val photo = unsplashApi.getRandomPhotoLandscape(category.query)
                    val bitmap = downloadBitmap(photo.urls.regular)
                    Result.success(ImageFetchResult(bitmap, photo, null))
                }
                ImageSourceType.PEXELS_CUSTOM -> {
                    val query = imageSource.pexelsSearchQuery ?: "nature"
                    val response = pexelsApi.searchPhotosLandscape(query = query)
                    if (response.photos.isNotEmpty()) {
                        val photo = response.photos.first()
                        val imageUrl = photo.src.large2x.ifEmpty { photo.src.large }
                        val bitmap = downloadBitmap(imageUrl)
                        Result.success(ImageFetchResult(bitmap, null, photo))
                    } else {
                        throw Exception("No Pexels photos found for query: $query")
                    }
                }
                ImageSourceType.GRADIENT -> {
                    val theme = imageSource.gradientTheme ?: GradientTheme.PURPLE_DUSK
                    val bitmap = createGradientBitmap(theme)
                    Result.success(ImageFetchResult(bitmap, null, null))
                }
                ImageSourceType.SOLID_COLOR -> {
                    val color = imageSource.solidColorHex ?: "#7C6FA7"
                    val bitmap = createSolidColorBitmap(color)
                    Result.success(ImageFetchResult(bitmap, null, null))
                }
                ImageSourceType.USER_GALLERY -> {
                    Result.failure(Exception("Gallery selection handled by UI"))
                }
            }
        } catch (e: Exception) {
            // Fallback: try Unsplash 4K nature, then gradient
            try {
                val photo = unsplashApi.getRandomPhotoLandscape(Unsplash4KCategory.NATURE_4K.query)
                val bitmap = downloadBitmap(photo.urls.regular)
                Result.success(ImageFetchResult(bitmap, photo, null))
            } catch (_: Exception) {
                try {
                    val fallbackBitmap = createGradientBitmap(GradientTheme.PURPLE_DUSK)
                    Result.success(ImageFetchResult(fallbackBitmap, null, null))
                } catch (_: Exception) {
                    Result.failure(e)
                }
            }
        }
    }

    /**
     * Composite verse text onto a background image.
     * Returns a Bitmap ready to be set as wallpaper.
     */
    suspend fun compositeVerseOnImage(
        background: Bitmap,
        verseText: String,
        reference: String,
        settings: UserSettings
    ): Bitmap = withContext(Dispatchers.IO) {
        val width = background.width
        val height = background.height

        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        // Draw background
        canvas.drawBitmap(background, 0f, 0f, null)

        // Calculate average brightness of center area for text color
        val isDark = isImageDark(background)
        val textColor = if (isDark) android.graphics.Color.WHITE else android.graphics.Color.BLACK
        val shadowColor = if (isDark) android.graphics.Color.BLACK else android.graphics.Color.WHITE

        // Apply dark overlay if enabled
        if (settings.useDarkOverlay) {
            val overlayPaint = Paint().apply {
                color = if (isDark) {
                    android.graphics.Color.argb(80, 0, 0, 0)
                } else {
                    android.graphics.Color.argb(60, 255, 255, 255)
                }
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), overlayPaint)
        }

        // Prepare text paint
        val baseTextSize = 72f * settings.fontSize.scale
        val typeface = when (settings.fontStyle) {
            com.dailyverse.app.data.model.FontStyle.SERIF -> Typeface.SERIF
            com.dailyverse.app.data.model.FontStyle.SANS_SERIF -> Typeface.SANS_SERIF
            com.dailyverse.app.data.model.FontStyle.SCRIPT -> Typeface.create("cursive", Typeface.NORMAL)
            com.dailyverse.app.data.model.FontStyle.MODERN -> Typeface.create("sans-serif-light", Typeface.NORMAL)
        }

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            this.textSize = baseTextSize
            this.typeface = typeface
            textAlign = Paint.Align.CENTER
        }

        // Draw verse text with word wrapping
        val margin = width * 0.1f
        val maxWidth = width - (margin * 2)
        val textY = height * 0.35f

        val words = verseText.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            val bounds = Rect()
            textPaint.getTextBounds(testLine, 0, testLine.length, bounds)

            if (bounds.width() > maxWidth && currentLine.isNotEmpty()) {
                lines.add(currentLine)
                currentLine = word
            } else {
                currentLine = testLine
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }

        // Draw shadow first
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = shadowColor
            this.textSize = baseTextSize
            this.typeface = typeface
            textAlign = Paint.Align.CENTER
            alpha = 128
        }

        val lineHeight = baseTextSize * 1.4f
        val totalTextHeight = lines.size * lineHeight
        val startY = textY - (totalTextHeight / 2)

        for (i in lines.indices) {
            val line = lines[i]
            val y = startY + (i * lineHeight) + lineHeight

            // Shadow
            canvas.drawText(line, width / 2f + 2, y + 2, shadowPaint)
            // Text
            canvas.drawText(line, width / 2f, y, textPaint)
        }

        // Draw reference
        if (settings.showReference && reference.isNotEmpty()) {
            val refSize = baseTextSize * 0.6f
            val refPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = textColor
                textSize = refSize
                this.typeface = Typeface.create(typeface, Typeface.ITALIC)
                textAlign = Paint.Align.CENTER
                alpha = 200
            }
            val refShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = shadowColor
                textSize = refSize
                this.typeface = Typeface.create(typeface, Typeface.ITALIC)
                textAlign = Paint.Align.CENTER
                alpha = 100
            }

            val refY = startY + totalTextHeight + refSize * 1.5f
            canvas.drawText(reference, width / 2f + 2, refY + 2, refShadowPaint)
            canvas.drawText(reference, width / 2f, refY, refPaint)
        }

        // Add a subtle app watermark at bottom
        val watermarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            textSize = 24f
            this.typeface = Typeface.SANS_SERIF
            textAlign = Paint.Align.CENTER
            alpha = 60
        }
        canvas.drawText("DailyVerse", width / 2f, height - 40f, watermarkPaint)

        result
    }

    /**
     * Save the final wallpaper bitmap to app storage.
     */
    suspend fun saveWallpaper(bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        val file = File(context.filesDir, "wallpapers")
        if (!file.exists()) file.mkdirs()
        val wallpaperFile = File(file, "current_wallpaper.jpg")
        FileOutputStream(wallpaperFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }
        wallpaperFile.absolutePath
    }

    /**
     * Pick a random image from the user's gallery.
     */
    suspend fun getRandomGalleryImageUri(): Uri? = withContext(Dispatchers.IO) {
        try {
            val projection = arrayOf(MediaStore.Images.Media._ID)
            val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                    val count = cursor.count
                    val randomPosition = (0 until count).random()
                    cursor.moveToPosition(randomPosition)
                    val id = cursor.getLong(idColumn)
                    return@withContext Uri.withAppendedPath(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id.toString()
                    )
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    // ============================================================
    // PRIVATE HELPERS
    // ============================================================

    private suspend fun downloadBitmap(url: String): Bitmap = withContext(Dispatchers.IO) {
        val request = ImageRequest.Builder(context)
            .data(url)
            .allowHardware(false)
            .build()

        val drawable = context.imageLoader.execute(request).drawable
            ?: throw Exception("Failed to load image")

        val bitmap = if (drawable is android.graphics.drawable.BitmapDrawable) {
            drawable.bitmap
        } else {
            val width = drawable.intrinsicWidth.coerceAtLeast(1)
            val height = drawable.intrinsicHeight.coerceAtLeast(1)
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val cvs = Canvas(bmp)
            drawable.setBounds(0, 0, width, height)
            drawable.draw(cvs)
            bmp
        }
        bitmap
    }

    private fun createGradientBitmap(theme: GradientTheme): Bitmap {
        val width = 1080
        val height = 2400
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val colors = theme.colors.map { android.graphics.Color.parseColor(it) }.toIntArray()
        val positions = if (colors.size == 2) {
            floatArrayOf(0f, 1f)
        } else {
            FloatArray(colors.size) { it.toFloat() / (colors.size - 1) }
        }

        val gradient = LinearGradient(
            0f, 0f, width.toFloat(), height.toFloat(),
            colors, positions, Shader.TileMode.CLAMP
        )

        val paint = Paint().apply {
            shader = gradient
        }

        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        return bitmap
    }

    private fun createSolidColorBitmap(hexColor: String): Bitmap {
        val width = 1080
        val height = 2400
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            color = try {
                android.graphics.Color.parseColor(hexColor)
            } catch (_: Exception) {
                android.graphics.Color.parseColor("#7C6FA7")
            }
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        return bitmap
    }

    /**
     * Determine if the center area of an image is dark or light.
     */
    private fun isImageDark(bitmap: Bitmap): Boolean {
        val width = bitmap.width
        val height = bitmap.height
        val centerX = width / 2
        val centerY = height / 2
        val sampleSize = 50

        var totalLuminance = 0.0
        var sampleCount = 0

        val startX = (centerX - sampleSize / 2).coerceIn(0, width - 1)
        val endX = (centerX + sampleSize / 2).coerceIn(0, width - 1)
        val startY = (centerY - sampleSize).coerceIn(0, height - 1)
        val endY = (centerY + sampleSize).coerceIn(0, height - 1)

        for (x in startX until endX step 3) {
            for (y in startY until endY step 3) {
                val pixel = bitmap.getPixel(x, y)
                val r = android.graphics.Color.red(pixel)
                val g = android.graphics.Color.green(pixel)
                val b = android.graphics.Color.blue(pixel)
                val luminance = 0.299 * r + 0.587 * g + 0.114 * b
                totalLuminance += luminance
                sampleCount++
            }
        }

        val avgLuminance = if (sampleCount > 0) totalLuminance / sampleCount else 128.0
        return avgLuminance < 128
    }
}

data class ImageFetchResult(
    val bitmap: Bitmap,
    val unsplashPhoto: UnsplashPhoto?,
    val pexelsPhoto: PexelsPhoto?
)
