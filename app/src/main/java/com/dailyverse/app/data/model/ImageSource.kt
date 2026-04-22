package com.dailyverse.app.data.model

enum class ImageSourceType {
    UNSPLASH_4K,
    PEXELS_CUSTOM,
    GRADIENT,
    SOLID_COLOR,
    USER_GALLERY
}

enum class Unsplash4KCategory(val query: String, val displayName: String) {
    NATURE_4K("4k nature landscape scenic wallpaper", "Nature 4K"),
    SUNRISE_4K("4k sunrise sunset golden hour wallpaper", "Sunrise 4K"),
    MOUNTAINS_4K("4k mountain peak alpine wallpaper", "Mountains 4K"),
    OCEAN_4K("4k ocean sea water waves wallpaper", "Ocean 4K"),
    FOREST_4K("4k forest trees woods wallpaper", "Forest 4K"),
    FLOWERS_4K("4k flowers garden bloom wallpaper", "Flowers 4K"),
    STARS_4K("4k night sky stars milky way galaxy wallpaper", "Night Sky 4K"),
    ABSTRACT_4K("4k abstract texture pattern minimal wallpaper", "Abstract 4K"),
    AURORA_4K("4k aurora borealis northern lights wallpaper", "Aurora 4K"),
    CITYSCAPE_4K("4k cityscape skyline night lights wallpaper", "Cityscape 4K");

    companion object {
        fun fromDisplayName(name: String): Unsplash4KCategory {
            return entries.find { it.displayName == name } ?: NATURE_4K
        }
    }
}

enum class GradientTheme(val colors: List<String>, val displayName: String) {
    PURPLE_DUSK(listOf("#667eea", "#764ba2"), "Purple Dusk"),
    OCEAN_BLUE(listOf("#2193b0", "#6dd5ed"), "Ocean Blue"),
    SUNSET_WARM(listOf("#f83600", "#f9d423"), "Sunset Warm"),
    FOREST_GREEN(listOf("#11998e", "#38ef7d"), "Forest Green"),
    ROSE_PINK(listOf("#e65c00", "#F9D423"), "Golden Hour"),
    MIDNIGHT(listOf("#232526", "#414345"), "Midnight"),
    PASTEL_DREAM(listOf("#a8edea", "#fed6e3"), "Pastel Dream"),
    EARTH_TONE(listOf("#3E5151", "#DECBA4"), "Earth Tone");

    companion object {
        fun fromDisplayName(name: String): GradientTheme {
            return entries.find { it.displayName == name } ?: PURPLE_DUSK
        }
    }
}

data class ImageSource(
    val type: ImageSourceType,
    val unsplashCategory: Unsplash4KCategory? = null,
    val gradientTheme: GradientTheme? = null,
    val pexelsSearchQuery: String? = null,
    val solidColorHex: String? = null
)
