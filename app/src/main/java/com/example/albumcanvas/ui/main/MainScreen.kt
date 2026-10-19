package com.example.albumcanvas.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.example.albumcanvas.theme.AlbumCanvasTheme
import com.example.albumcanvas.ui.screens.CustomizationScreen
import com.example.albumcanvas.ui.screens.HomeScreen
import com.example.albumcanvas.ui.screens.PlayersScreen
import com.example.albumcanvas.ui.screens.SettingsScreen
import com.example.albumcanvas.ui.screens.StatisticsScreen
import com.example.albumcanvas.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onItemClick: (NavKey) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = hiltViewModel()
) {
    val song by viewModel.currentSong.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    val paletteColors by viewModel.paletteColors.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val changesCountToday by viewModel.changesCountToday.collectAsStateWithLifecycle()
    val lastUpdate by viewModel.lastUpdate.collectAsStateWithLifecycle()
    val cachedCount by viewModel.cachedCount.collectAsStateWithLifecycle()
    val cacheSizeMb by viewModel.cacheSizeMb.collectAsStateWithLifecycle()
    val memoryUsageMb by viewModel.memoryUsageMb.collectAsStateWithLifecycle()
    val players by viewModel.mediaPlayers.collectAsStateWithLifecycle()

    var selectedTab by rememberSaveable { mutableStateOf(0) }

    val formatter = remember { SimpleDateFormat("hh:mm:ss a", Locale.getDefault()) }
    val lastUpdateStr = remember(lastUpdate) {
        lastUpdate?.let { formatter.format(Date(it.timestamp)) } ?: "Never"
    }

    AlbumCanvasTheme(
        darkTheme = settings.darkMode,
        dynamicColor = false, // Prioritize dynamic album art coloring
        artworkColors = if (settings.dynamicMaterialYou) paletteColors else null
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "AlbumCanvas",
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.Brush, contentDescription = "Canvas") },
                        label = { Text("Canvas") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Default.QueueMusic, contentDescription = "Players") },
                        label = { Text("Players") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        icon = { Icon(Icons.Default.BarChart, contentDescription = "Stats") },
                        label = { Text("Stats") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 4,
                        onClick = { selectedTab = 4 },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Settings") }
                    )
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (selectedTab) {
                    0 -> HomeScreen(
                        song = song,
                        status = status,
                        colors = paletteColors,
                        settings = settings,
                        lastUpdateStr = lastUpdateStr
                    )
                    1 -> CustomizationScreen(
                        settings = settings,
                        onSettingsChanged = { newSettings -> viewModel.updateSettings { newSettings } }
                    )
                    2 -> PlayersScreen(
                        players = players,
                        onPlayerToggled = { pkg, state -> viewModel.togglePlayer(pkg, state) }
                    )
                    3 -> StatisticsScreen(
                        changesCountToday = changesCountToday,
                        cachedCount = cachedCount,
                        cacheSizeMb = cacheSizeMb,
                        memoryUsageMb = memoryUsageMb,
                        lastUpdate = lastUpdate,
                        onClearStats = { viewModel.clearHistory() },
                        onForceRefresh = { viewModel.updateMemoryUsage(); viewModel.updateCacheSize() }
                    )
                    4 -> SettingsScreen(
                        settings = settings,
                        onSettingsChanged = { newSettings -> viewModel.updateSettings { newSettings } },
                        onClearCache = { viewModel.clearCache() },
                        onResetDefaults = { viewModel.resetToDefaults() },
                        onExportSettings = { onExport -> viewModel.exportSettings(onExport) },
                        onImportSettings = { json -> viewModel.importSettings(json) { viewModel.updateMemoryUsage() } }
                    )
                }
            }
        }
    }
}
