package com.example.albumcanvas.ui.viewmodel

import android.app.ActivityManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.albumcanvas.data.CacheManager
import com.example.albumcanvas.data.MainRepository
import com.example.albumcanvas.data.SongDetails
import com.example.albumcanvas.data.db.CachedArtworkDao
import com.example.albumcanvas.data.db.HistoryDao
import com.example.albumcanvas.data.db.HistoryEntity
import com.example.albumcanvas.data.db.PlayerAppDao
import com.example.albumcanvas.data.db.PlayerAppEntity
import com.example.albumcanvas.data.pref.SettingsData
import com.example.albumcanvas.data.pref.SettingsDataStore
import com.example.albumcanvas.utils.ImageProcessor
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: MainRepository,
    private val settingsDataStore: SettingsDataStore,
    private val historyDao: HistoryDao,
    private val playerAppDao: PlayerAppDao,
    private val cachedArtworkDao: CachedArtworkDao,
    private val cacheManager: CacheManager
) : ViewModel() {

    val currentSong: StateFlow<SongDetails?> = repository.currentSong
    val status: StateFlow<String> = repository.status
    val paletteColors: StateFlow<ImageProcessor.PaletteColors?> = repository.paletteColors
    val settings: StateFlow<SettingsData> = settingsDataStore.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsData()
    )

    // Statistics
    val changesCountToday: StateFlow<Int> = historyDao.getChangesCountSince(getStartOfToday()).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val lastUpdate: StateFlow<HistoryEntity?> = repository.lastWallpaperUpdate

    val cachedCount: StateFlow<Int> = cachedArtworkDao.getCacheCountFlow().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    private val _cacheSizeMb = MutableStateFlow(0.0)
    val cacheSizeMb: StateFlow<Double> = _cacheSizeMb.asStateFlow()

    private val _memoryUsageMb = MutableStateFlow(0.0)
    val memoryUsageMb: StateFlow<Double> = _memoryUsageMb.asStateFlow()

    // Detected Players
    val mediaPlayers: StateFlow<List<PlayerAppEntity>> = playerAppDao.getAllPlayersFlow().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        updateCacheSize()
        updateMemoryUsage()
    }

    fun updateSettings(updater: (SettingsData) -> SettingsData) {
        viewModelScope.launch {
            settingsDataStore.updateSettings(updater)
        }
    }

    fun togglePlayer(packageName: String, enabled: Boolean) {
        viewModelScope.launch {
            playerAppDao.updatePlayerState(packageName, enabled)
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            cacheManager.clearAllCache()
            updateCacheSize()
        }
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            settingsDataStore.resetToDefaults()
        }
    }

    fun exportSettings(onExported: (String) -> Unit) {
        viewModelScope.launch {
            val currentSettings = settings.value
            val jsonStr = settingsDataStore.exportSettingsJson(currentSettings)
            onExported(jsonStr)
        }
    }

    fun importSettings(jsonStr: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = settingsDataStore.importSettingsJson(jsonStr)
            onResult(success)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            historyDao.clearHistory()
        }
    }

    fun updateCacheSize() {
        viewModelScope.launch {
            _cacheSizeMb.value = cacheManager.getCacheSizeMb()
        }
    }

    fun updateMemoryUsage() {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)
        // Return used memory in MB
        _memoryUsageMb.value = (memInfo.totalMem - memInfo.availMem).toDouble() / (1024.0 * 1024.0)
    }

    private fun getStartOfToday(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
