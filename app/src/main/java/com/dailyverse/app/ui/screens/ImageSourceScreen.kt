package com.dailyverse.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dailyverse.app.data.model.GradientTheme
import com.dailyverse.app.data.model.ImageSourceType
import com.dailyverse.app.data.model.Unsplash4KCategory
import com.dailyverse.app.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageSourceScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    val currentType = settings.imageSource.type
    val currentUnsplashCategory = settings.imageSource.unsplashCategory
    val currentGradient = settings.imageSource.gradientTheme
    val currentPexelsQuery = settings.imageSource.pexelsSearchQuery ?: "nature"
    var pexelsSearchInput by remember { mutableStateOf(currentPexelsQuery) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Image Source",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {

            // ====== UNSPLASH 4K SECTION ======
            SectionTitle("Unsplash 4K (Default)")
            SectionSubtitle("High-quality 4K wallpapers from Unsplash")
            Spacer(modifier = Modifier.height(12.dp))

            val categories = Unsplash4KCategory.entries.toList()
            categories.chunked(2).forEach { rowCategories ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    rowCategories.forEach { category ->
                        CategoryCard(
                            category = category,
                            selected = currentType == ImageSourceType.UNSPLASH_4K &&
                                    currentUnsplashCategory == category,
                            onClick = { viewModel.updateUnsplash4KCategory(category) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (rowCategories.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ====== PEXELS CUSTOM SEARCH SECTION ======
            SectionTitle("Pexels (Custom Search)")
            SectionSubtitle("Search any keyword on Pexels for custom wallpapers")
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        viewModel.updatePexelsSearchQuery(pexelsSearchInput)
                    },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (currentType == ImageSourceType.PEXELS_CUSTOM)
                        MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surface
                ),
                border = if (currentType == ImageSourceType.PEXELS_CUSTOM) {
                    androidx.compose.foundation.border.BorderStroke(
                        2.dp,
                        MaterialTheme.colorScheme.primary
                    )
                } else null
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = pexelsSearchInput,
                            onValueChange = { pexelsSearchInput = it },
                            label = { Text("Search keyword (e.g., nature, mountains)") },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null)
                            },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            if (pexelsSearchInput.isNotBlank()) {
                                viewModel.updatePexelsSearchQuery(pexelsSearchInput)
                            }
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Use Pexels Search")
                    }
                    if (currentType == ImageSourceType.PEXELS_CUSTOM) {
                        Row(
                            modifier = Modifier.padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.size(4.dp))
                            Text(
                                text = "Active: searching \"$currentPexelsQuery\" on Pexels",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Quick search chips
            Spacer(modifier = Modifier.height(12.dp))
            val popularSearches = listOf(
                "nature", "mountains", "ocean", "forest", "flowers",
                "sunset", "aurora", "galaxy", "cityscape", "abstract",
                "minimal", "waterfall", "beach", "snow", "autumn"
            )
            Text(
                text = "Popular searches:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            androidx.compose.foundation.layout.FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                popularSearches.forEach { search ->
                    androidx.compose.material3.InputChip(
                        selected = pexelsSearchInput == search,
                        onClick = {
                            pexelsSearchInput = search
                            viewModel.updatePexelsSearchQuery(search)
                        },
                        label = { Text(search) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ====== GRADIENT THEMES SECTION ======
            SectionTitle("Gradient Themes")
            SectionSubtitle("Beautiful gradients that work offline")
            Spacer(modifier = Modifier.height(12.dp))

            val gradients = GradientTheme.entries.toList()
            gradients.chunked(2).forEach { rowGradients ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    rowGradients.forEach { theme ->
                        GradientCard(
                            theme = theme,
                            selected = currentType == ImageSourceType.GRADIENT &&
                                    currentGradient == theme,
                            onClick = { viewModel.updateGradientTheme(theme) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (rowGradients.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Info card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "About Image Sources",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Unsplash 4K provides stunning high-resolution wallpapers. " +
                                "Pexels lets you search any keyword for custom images. " +
                                "Gradient themes work completely offline. " +
                                "All photos are free to use.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
fun SectionSubtitle(subtitle: String) {
    Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 2.dp)
    )
}

@Composable
fun CategoryCard(
    category: Unsplash4KCategory,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .padding(4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected)
                MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        ),
        border = if (selected) {
            androidx.compose.foundation.border.BorderStroke(
                2.dp,
                MaterialTheme.colorScheme.primary
            )
        } else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val categoryColors = getCategoryColors(category)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.5f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.linearGradient(
                            colors = categoryColors,
                            start = androidx.compose.ui.geometry.Offset(0f, 0f),
                            end = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                        )
                    )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = category.displayName,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center
            )
            if (selected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun GradientCard(
    theme: GradientTheme,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = theme.colors.map { Color(android.graphics.Color.parseColor(it)) }

    Card(
        modifier = modifier
            .padding(4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = if (selected) {
            androidx.compose.foundation.border.BorderStroke(
                2.dp,
                MaterialTheme.colorScheme.primary
            )
        } else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.5f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.linearGradient(
                            colors = colors,
                            start = androidx.compose.ui.geometry.Offset(0f, 0f),
                            end = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                        )
                    )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = theme.displayName,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center
            )
            if (selected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

private fun getCategoryColors(category: Unsplash4KCategory): List<Color> {
    return when (category) {
        Unsplash4KCategory.NATURE_4K -> listOf(Color(0xFF2E7D32), Color(0xFF81C784), Color(0xFF4FC3F7))
        Unsplash4KCategory.SUNRISE_4K -> listOf(Color(0xFFFF6F00), Color(0xFFFFB74D), Color(0xFF81D4FA))
        Unsplash4KCategory.MOUNTAINS_4K -> listOf(Color(0xFF37474F), Color(0xFF90A4AE), Color(0xFFCFD8DC))
        Unsplash4KCategory.OCEAN_4K -> listOf(Color(0xFF01579B), Color(0xFF4FC3F7), Color(0xFFB3E5FC))
        Unsplash4KCategory.FOREST_4K -> listOf(Color(0xFF1B5E20), Color(0xFF388E3C), Color(0xFF8BC34A))
        Unsplash4KCategory.FLOWERS_4K -> listOf(Color(0xFFAD1457), Color(0xFFF06292), Color(0xFFF8BBD0))
        Unsplash4KCategory.STARS_4K -> listOf(Color(0xFF0D47A1), Color(0xFF1A237E), Color(0xFF311B92))
        Unsplash4KCategory.ABSTRACT_4K -> listOf(Color(0xFF4A148C), Color(0xFF7B1FA2), Color(0xFFCE93D8))
        Unsplash4KCategory.AURORA_4K -> listOf(Color(0xFF004D40), Color(0xFF00E676), Color(0xFF69F0AE))
        Unsplash4KCategory.CITYSCAPE_4K -> listOf(Color(0xFF212121), Color(0xFF424242), Color(0xFFFFE082))
    }
}
