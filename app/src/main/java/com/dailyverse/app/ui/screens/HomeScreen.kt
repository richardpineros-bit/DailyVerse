package com.dailyverse.app.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dailyverse.app.data.model.AppMode
import com.dailyverse.app.data.model.MemorizationProgress
import com.dailyverse.app.ui.viewmodel.MainViewModel
import com.dailyverse.app.R
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToMemorization: () -> Unit
) {
    val context = LocalContext.current
    val verses by viewModel.currentVerses
    val wallpaper by viewModel.currentWallpaper
    val isLoading by viewModel.isLoading
    val error by viewModel.error
    val progress by viewModel.memorizationProgress
    val settings by viewModel.settings.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "DailyVerse",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    IconButton(onClick = { viewModel.refreshImage() }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (wallpaper != null) {
                FloatingActionButton(
                    onClick = { viewModel.applyWallpaper() },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        Icons.Default.Wallpaper,
                        contentDescription = "Set Wallpaper",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Mode indicator
            ModeIndicatorChip(settings.appMode)

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading && wallpaper == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(9f / 16f)
                        .padding(16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (wallpaper != null) {
                // Wallpaper preview
                WallpaperPreview(
                    wallpaper = wallpaper!!,
                    verses = verses,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
            } else if (verses.isNotEmpty()) {
                // Verse display without wallpaper
                VerseCard(
                    verses = verses,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action buttons
            ActionButtonsRow(
                onGenerate = { viewModel.generateWallpaper() },
                onApply = { viewModel.applyWallpaper() },
                onShare = { /* TODO */ },
                hasWallpaper = wallpaper != null
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Memorization progress
            if (settings.appMode == AppMode.MEMORIZATION && progress != null) {
                MemorizationProgressCard(
                    progress = progress!!,
                    onContinue = { viewModel.advanceMemorization() },
                    onNavigateToConfig = onNavigateToMemorization
                )
            }

            // Settings summary
            SettingsSummaryCard(settings)

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun ModeIndicatorChip(mode: AppMode) {
    val (label, color) = when (mode) {
        AppMode.DAILY_INSPIRATION -> "Daily Inspiration" to MaterialTheme.colorScheme.primary
        AppMode.MEMORIZATION -> "Memorization" to MaterialTheme.colorScheme.secondary
    }

    Box(
        modifier = Modifier
            .padding(top = 16.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = color
        )
    }
}

@Composable
fun WallpaperPreview(
    wallpaper: Bitmap,
    verses: List<com.dailyverse.app.data.model.BibleVerse>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box {
            Image(
                bitmap = wallpaper.asImageBitmap(),
                contentDescription = "Wallpaper preview",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(9f / 16f),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
fun VerseCard(
    verses: List<com.dailyverse.app.data.model.BibleVerse>,
    modifier: Modifier = Modifier
) {
    val verseText = verses.joinToString(" ") { it.text }
    val reference = if (verses.size == 1) {
        com.dailyverse.app.util.BibleBookUtil.formatReference(
            verses[0].book, verses[0].chapter, verses[0].verse
        )
    } else {
        com.dailyverse.app.util.BibleBookUtil.formatReferenceRange(
            verses[0].book, verses[0].chapter,
            verses[0].verse, verses.last().verse
        )
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "\"$verseText\"",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontStyle = FontStyle.Italic
                ),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "\u2014 $reference",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun ActionButtonsRow(
    onGenerate: () -> Unit,
    onApply: () -> Unit,
    onShare: () -> Unit,
    hasWallpaper: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        ActionButton(
            icon = Icons.Default.PhotoLibrary,
            label = "Generate",
            onClick = onGenerate
        )
        ActionButton(
            icon = Icons.Default.Wallpaper,
            label = "Set",
            onClick = onApply,
            enabled = hasWallpaper
        )
        ActionButton(
            icon = Icons.Default.Share,
            label = "Share",
            onClick = onShare,
            enabled = hasWallpaper
        )
        ActionButton(
            icon = Icons.Default.Download,
            label = "Save",
            onClick = onShare, // TODO
            enabled = hasWallpaper
        )
    }
}

@Composable
fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    if (enabled) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant
                )
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (enabled) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (enabled) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}

@Composable
fun MemorizationProgressCard(
    progress: MemorizationProgress,
    onContinue: () -> Unit,
    onNavigateToConfig: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Memorization Progress",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${progress.book} ${progress.chapter}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { progress.percentageComplete },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${(progress.percentageComplete * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "${progress.versesLearned}/${progress.totalVersesInChapter} verses",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (progress.isComplete) {
                Button(
                    onClick = onNavigateToConfig,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Choose Next Chapter")
                }
            } else {
                Button(
                    onClick = onContinue,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Continue (${progress.versesPerDay} verse${if (progress.versesPerDay > 1) "s" else ""}/day)")
                }
            }
        }
    }
}

@Composable
fun SettingsSummaryCard(settings: com.dailyverse.app.data.model.UserSettings) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Current Settings",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            SettingsRow("Bible Version", settings.bibleVersion.displayName)
            SettingsRow("Image Source", settings.imageSource.unsplashCategory?.displayName
                ?: settings.imageSource.pexelsSearchQuery?.let { "Pexels: $it" }
                ?: settings.imageSource.gradientTheme?.displayName
                ?: "4K Nature")
            SettingsRow("Update Time", String.format("%02d:%02d", settings.updateHour, settings.updateMinute))
            SettingsRow("Font Size", settings.fontSize.displayName)
            SettingsRow("Dark Overlay", if (settings.useDarkOverlay) "On" else "Off")
        }
    }
}

@Composable
fun SettingsRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
