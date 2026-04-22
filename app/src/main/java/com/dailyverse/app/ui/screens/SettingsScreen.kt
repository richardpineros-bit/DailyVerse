package com.dailyverse.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.dailyverse.app.data.model.AppMode
import com.dailyverse.app.data.model.BibleVersion
import com.dailyverse.app.data.model.FontSize
import com.dailyverse.app.data.model.FontStyle
import com.dailyverse.app.ui.viewmodel.SettingsViewModel
import com.dailyverse.app.worker.DailyWallpaperWorker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToMemorization: () -> Unit,
    onNavigateToImageSource: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    val context = LocalContext.current
    var showTimePicker by remember { mutableStateOf(false) }
    val saveSuccess by viewModel.saveSuccess.collectAsState()

    if (saveSuccess) {
        Toast.makeText(context, "Settings saved!", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // App Mode Section
            SettingsSection(title = "App Mode") {
                Column {
                    ModeOption(
                        title = "Daily Inspiration",
                        subtitle = "A new random verse every day",
                        selected = settings.appMode == AppMode.DAILY_INSPIRATION,
                        onSelect = { viewModel.updateMode(AppMode.DAILY_INSPIRATION) }
                    )
                    ModeOption(
                        title = "Memorization Mode",
                        subtitle = "Sequential verses from a chosen book",
                        selected = settings.appMode == AppMode.MEMORIZATION,
                        onSelect = { viewModel.updateMode(AppMode.MEMORIZATION) }
                    )
                }
            }

            // Bible Version Section
            SettingsSection(title = "Bible Version") {
                Column {
                    BibleVersion.entries.forEach { version ->
                        VersionOption(
                            version = version,
                            selected = settings.bibleVersion == version,
                            onSelect = { viewModel.updateBibleVersion(version) }
                        )
                    }
                }
            }

            // Image Source
            SettingsClickableItem(
                icon = Icons.Default.Image,
                title = "Image Source",
                subtitle = settings.imageSource.category?.displayName
                    ?: settings.imageSource.gradientTheme?.displayName
                    ?: "Nature & Landscapes",
                onClick = onNavigateToImageSource
            )

            // Update Time
            SettingsClickableItem(
                icon = Icons.Default.AccessTime,
                title = "Daily Update Time",
                subtitle = String.format("%02d:%02d", settings.updateHour, settings.updateMinute),
                onClick = { showTimePicker = true }
            )

            if (showTimePicker) {
                TimePickerDialog(
                    initialHour = settings.updateHour,
                    initialMinute = settings.updateMinute,
                    onDismiss = { showTimePicker = false },
                    onConfirm = { hour, minute ->
                        viewModel.updateUpdateTime(hour, minute)
                        DailyWallpaperWorker.schedule(context, hour, minute)
                        showTimePicker = false
                    }
                )
            }

            // Verses Per Day (for memorization)
            if (settings.appMode == AppMode.MEMORIZATION) {
                SettingsSection(title = "Memorization") {
                    SettingsClickableItem(
                        icon = Icons.Default.Book,
                        title = "Book & Chapter",
                        subtitle = if (settings.memorizationBook.isEmpty()) {
                            "Not configured"
                        } else {
                            "${settings.memorizationBook} ${settings.memorizationChapter}"
                        },
                        onClick = onNavigateToMemorization
                    )
                    VersesPerDaySlider(
                        value = settings.versesPerDay,
                        onValueChange = { viewModel.updateVersesPerDay(it) }
                    )
                }
            }

            // Display Settings
            SettingsSection(title = "Display") {
                FontSizeSelector(
                    selected = settings.fontSize,
                    onSelect = { viewModel.updateFontSize(it) }
                )
                FontStyleSelector(
                    selected = settings.fontStyle,
                    onSelect = { viewModel.updateFontStyle(it) }
                )
                ToggleSetting(
                    icon = Icons.Default.DarkMode,
                    title = "Dark Overlay",
                    subtitle = "Improve text readability on images",
                    checked = settings.useDarkOverlay,
                    onCheckedChange = { viewModel.updateDarkOverlay(it) }
                )
            }

            // Notifications
            ToggleSetting(
                icon = Icons.Default.Notifications,
                title = "Daily Notification",
                subtitle = "Get notified when your verse is ready",
                checked = settings.sendNotification,
                onCheckedChange = { viewModel.updateNotification(it) }
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            content()
        }
    }
}

@Composable
fun ModeOption(
    title: String,
    subtitle: String,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelect
        )
        Column(
            modifier = Modifier.padding(start = 8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun VersionOption(
    version: BibleVersion,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelect
        )
        Column(
            modifier = Modifier.padding(start = 8.dp)
        ) {
            Text(
                text = version.displayName,
                style = MaterialTheme.typography.bodyMedium
            )
            if (!version.isBundled) {
                Text(
                    text = "Requires internet",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
fun SettingsClickableItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        leadingContent = {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
fun ToggleSetting(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        modifier = Modifier.fillMaxWidth(),
        leadingContent = {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        },
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
fun VersesPerDaySlider(
    value: Int,
    onValueChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Verses per day: $value",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = 1f..3f,
            steps = 1,
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("1", style = MaterialTheme.typography.labelSmall)
            Text("2", style = MaterialTheme.typography.labelSmall)
            Text("3", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun FontSizeSelector(
    selected: FontSize,
    onSelect: (FontSize) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Font Size: ${selected.displayName}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            FontSize.entries.forEach { size ->
                OutlinedCard(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                        .clickable { onSelect(size) },
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = if (selected == size)
                            MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .padding(vertical = 12.dp)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "A",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = MaterialTheme.typography.bodyLarge.fontSize * size.scale
                            ),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FontStyleSelector(
    selected: FontStyle,
    onSelect: (FontStyle) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Font Style",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            FontStyle.entries.forEach { style ->
                OutlinedCard(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                        .clickable { onSelect(style) },
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = if (selected == style)
                            MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .padding(vertical = 12.dp)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = style.displayName,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Select Update Time",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                TimePicker(state = timePickerState)
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    TextButton(
                        onClick = {
                            onConfirm(timePickerState.hour, timePickerState.minute)
                        }
                    ) {
                        Text("OK")
                    }
                }
            }
        }
    }
}
