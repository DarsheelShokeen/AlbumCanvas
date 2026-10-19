package com.example.albumcanvas.data

import android.graphics.Bitmap
import com.example.albumcanvas.data.db.HistoryDao
import com.example.albumcanvas.data.db.HistoryEntity
import com.example.albumcanvas.data.db.PlayerAppDao
import com.example.albumcanvas.data.db.PlayerAppEntity
import com.example.albumcanvas.utils.ImageProcessor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class SongDetails(
    val title: String,
    val artist: String,
    val album: String,
    val packageName: String,
    val appName: String,
    val artwork: Bitmap?,
    val mediaId: String?,
    val albumId: String?,
    val artworkUri: String?,
    val timestamp: Long = System.currentTimeMillis()
)

@Singleton
class MainRepository @Inject constructor(
    private val historyDao: HistoryDao,
    private val playerAppDao: PlayerAppDao
) {
    private val _currentSong = MutableStateFlow<SongDetails?>(null)
    val currentSong: StateFlow<SongDetails?> = _currentSong.asStateFlow()

    private val _status = MutableStateFlow("Idle")
    val status: StateFlow<String> = _status.asStateFlow()

    private val _paletteColors = MutableStateFlow<ImageProcessor.PaletteColors?>(null)
    val paletteColors: StateFlow<ImageProcessor.PaletteColors?> = _paletteColors.asStateFlow()

    private val _lastWallpaperUpdate = MutableStateFlow<HistoryEntity?>(null)
    val lastWallpaperUpdate: StateFlow<HistoryEntity?> = _lastWallpaperUpdate.asStateFlow()

    fun updateCurrentSong(song: SongDetails?) {
        _currentSong.value = song
        if (song?.artwork != null) {
            val extractedColors = ImageProcessor.extractPalette(song.artwork)
            _paletteColors.value = extractedColors
        }
    }

    fun updateStatus(newStatus: String) {
        _status.value = newStatus
    }

    suspend fun logWallpaperChange(
        title: String,
        artist: String,
        album: String,
        playerPackage: String,
        success: Boolean
    ) {
        val entry = HistoryEntity(
            timestamp = System.currentTimeMillis(),
            title = title,
            artist = artist,
            album = album,
            playerPackage = playerPackage,
            success = success
        )
        historyDao.insertHistory(entry)
        _lastWallpaperUpdate.value = entry
    }

    suspend fun registerDetectedPlayer(packageName: String, appName: String) {
        val entity = PlayerAppEntity(
            packageName = packageName,
            appName = appName,
            detectedTime = System.currentTimeMillis()
        )
        playerAppDao.insertPlayer(entity)
    }
}
