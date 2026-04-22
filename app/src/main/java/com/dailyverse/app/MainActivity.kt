package com.dailyverse.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dailyverse.app.ui.screens.HomeScreen
import com.dailyverse.app.ui.screens.ImageSourceScreen
import com.dailyverse.app.ui.screens.MemorizationConfigScreen
import com.dailyverse.app.ui.screens.SettingsScreen
import com.dailyverse.app.ui.theme.DailyVerseTheme
import com.dailyverse.app.ui.viewmodel.MainViewModel
import com.dailyverse.app.ui.viewmodel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DailyVerseTheme {
                DailyVerseApp()
            }
        }
    }
}

sealed class Screen(val route: String, val title: Int, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Home : Screen("home", R.string.nav_home, Icons.Default.Home)
    object Settings : Screen("settings", R.string.nav_settings, Icons.Default.Settings)
}

@Composable
fun DailyVerseApp() {
    val navController = rememberNavController()
    var selectedScreen by rememberSaveable { mutableIntStateOf(0) }
    val screens = listOf(Screen.Home, Screen.Settings)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                screens.forEachIndexed { index, screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = stringResource(screen.title)) },
                        label = { Text(stringResource(screen.title)) },
                        selected = selectedScreen == index,
                        onClick = {
                            selectedScreen = index
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Home.route) {
                val mainViewModel: MainViewModel = hiltViewModel()
                HomeScreen(
                    viewModel = mainViewModel,
                    onNavigateToMemorization = {
                        navController.navigate("memorization")
                    }
                )
            }
            composable(Screen.Settings.route) {
                val settingsViewModel: SettingsViewModel = hiltViewModel()
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onNavigateToMemorization = {
                        navController.navigate("memorization")
                    },
                    onNavigateToImageSource = {
                        navController.navigate("image_source")
                    }
                )
            }
            composable("memorization") {
                val settingsViewModel: SettingsViewModel = hiltViewModel()
                MemorizationConfigScreen(
                    viewModel = settingsViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("image_source") {
                val settingsViewModel: SettingsViewModel = hiltViewModel()
                ImageSourceScreen(
                    viewModel = settingsViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
