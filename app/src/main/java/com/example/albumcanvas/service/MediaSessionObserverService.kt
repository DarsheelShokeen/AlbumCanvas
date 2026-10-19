package com.example.albumcanvas.service

import android.app.Notification
import android.app.WallpaperManager
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.BatteryManager
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import coil.ImageLoader
import coil.request.ImageRequest
import com.example.albumcanvas.AlbumCanvasApp
import com.example.albumcanvas.R
import com.example.albumcanvas.data.CacheManager
import com.example.albumcanvas.data.MainRepository
import com.example.albumcanvas.data.SongDetails
import com.example.albumcanvas.data.db.PlayerAppDao
import com.example.albumcanvas.data.db.PlayerAppEntity
import com.example.albumcanvas.data.pref.SettingsData
import com.example.albumcanvas.data.pref.SettingsDataStore
import com.example.albumcanvas.utils.ImageProcessor
import com.example.albumcanvas.utils.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class MediaSessionObserverService : NotificationListenerService() {

    @Inject lateinit var repository: MainRepository
    @Inject lateinit var settingsDataStore: SettingsDataStore
    @Inject lateinit var playerAppDao: PlayerAppDao
    @Inject lateinit var cacheManager: CacheManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var mediaSessionManager: MediaSessionManager
    
    private val activeCallbacks = mutableMapOf<MediaController, MediaController.Callback>()
    private val notificationIcons = mutableMapOf<String, Bitmap>() // packageName to Large Icon Bitmap fallback
    
    private var updateJob: Job? = null
    private var isPowerConnected = false

    private val powerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            isPowerConnected = intent?.action == Intent.ACTION_POWER_CONNECTED
            serviceScope.launch {
                val settings = settingsDataStore.settingsFlow.first()
                // If service needs to react, trigger re-evaluation
                evaluateActiveSessions()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        mediaSessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        registerReceiver(powerReceiver, filter)
        
        // Query initial charging status
        val batteryStatus: Intent? = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        isPowerConnected = status == BatteryManager.BATTERY_STATUS_CHARGING || 
                           status == BatteryManager.BATTERY_STATUS_FULL

        startForegroundServiceNotification()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(powerReceiver)
        unregisterAllCallbacks()
        serviceScope.cancel()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        repository.updateStatus("Active")
        registerSessionListener()
        evaluateActiveSessions()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        repository.updateStatus("Disconnected")
        unregisterAllCallbacks()
    }

    private fun registerSessionListener() {
        try {
            val componentName = ComponentName(this, MediaSessionObserverService::class.java)
            mediaSessionManager.addOnActiveSessionsChangedListener(
                { controllers ->
                    serviceScope.launch {
                        updateControllerCallbacks(controllers)
                    }
                },
                componentName
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun evaluateActiveSessions() {
        try {
            val componentName = ComponentName(this, MediaSessionObserverService::class.java)
            val controllers = mediaSessionManager.getActiveSessions(componentName)
            updateControllerCallbacks(controllers)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateControllerCallbacks(controllers: List<MediaController>?) {
        if (controllers == null) return
        
        // Remove callbacks for controllers no longer active
        val iterator = activeCallbacks.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (!controllers.contains(entry.key)) {
                entry.key.unregisterCallback(entry.value)
                iterator.remove()
            }
        }

        // Add callbacks for new controllers
        for (controller in controllers) {
            if (!activeCallbacks.containsKey(controller)) {
                val callback = object : MediaController.Callback() {
                    override fun onMetadataChanged(metadata: MediaMetadata?) {
                        scheduleWallpaperUpdate()
                    }

                    override fun onPlaybackStateChanged(state: PlaybackState?) {
                        scheduleWallpaperUpdate()
                    }
                }
                controller.registerCallback(callback)
                activeCallbacks[controller] = callback
            }
        }
        
        scheduleWallpaperUpdate()
    }

    private fun unregisterAllCallbacks() {
        for ((controller, callback) in activeCallbacks) {
            controller.unregisterCallback(callback)
        }
        activeCallbacks.clear()
    }

    /**
     * Intercepts new notifications to extract high-quality Large Icons as artwork fallbacks.
     */
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        val sbnPackage = sbn?.packageName ?: return
        val notification = sbn.notification ?: return
        
        // Capture large icon as a potential fallback
        val largeIcon = notification.getLargeIcon()?.loadDrawable(this)?.let { drawable ->
            (drawable as? BitmapDrawable)?.bitmap
        }
        if (largeIcon != null) {
            notificationIcons[sbnPackage] = largeIcon
            // Re-evaluate if this notification is from the playing app and we are missing artwork
            scheduleWallpaperUpdate()
        }
    }

    private fun scheduleWallpaperUpdate() {
        updateJob?.cancel()
        updateJob = serviceScope.launch {
            val settings = settingsDataStore.settingsFlow.first()
            if (!settings.isServiceEnabled) {
                repository.updateStatus("Disabled")
                return@launch
            }
            
            // 500 ms (or configured) Queue Stability Delay
            delay(settings.delayMs.toLong())
            
            processActiveMedia()
        }
    }

    private suspend fun processActiveMedia() {
        val componentName = ComponentName(this, MediaSessionObserverService::class.java)
        val controllers = mediaSessionManager.getActiveSessions(componentName) ?: return
        
        // Find the currently playing controller
        val activeController = controllers.firstOrNull {
            it.playbackState?.state == PlaybackState.STATE_PLAYING
        } ?: controllers.firstOrNull() // fallback to the first active session

        if (activeController == null) {
            repository.updateStatus("No Active Media")
            return
        }

        val metadata = activeController.metadata
        val playbackState = activeController.playbackState
        val packageName = activeController.packageName
        
        // Get app details
        val appName = getAppName(packageName)
        repository.registerDetectedPlayer(packageName, appName)

        // Check if player is enabled
        val playerConfig = playerAppDao.getPlayer(packageName)
        if (playerConfig != null && !playerConfig.isEnabled) {
            repository.updateStatus("Player Disabled")
            return
        }

        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: ""
        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: ""
        val album = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM) ?: ""
        
        if (title.isBlank() && artist.isBlank()) {
            repository.updateStatus("Listening...")
            return
        }

        val settings = settingsDataStore.settingsFlow.first()

        // Ignore Ads Check
        if (settings.ignoreAds && isAd(title, artist, album)) {
            repository.updateStatus("Ignoring Ad")
            return
        }

        repository.updateStatus("Playing")

        // Retrieve Artwork using priority order
        val fallbackIcon = notificationIcons[packageName]
        val artwork = retrieveArtwork(activeController, metadata, fallbackIcon)

        val mediaId = metadata?.getString(MediaMetadata.METADATA_KEY_MEDIA_ID)
        val albumId = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI) // Use artwork URI as proxy for album identity
        val artworkUri = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI) 
                         ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI)

        val cacheKey = cacheManager.generateCacheKey(mediaId, albumId, album, artist, artworkUri)

        // Prevent duplicate updates
        val currentSongState = repository.currentSong.value
        if (currentSongState != null && 
            currentSongState.title == title && 
            currentSongState.artist == artist && 
            currentSongState.packageName == packageName &&
            (currentSongState.artwork != null && artwork != null && currentSongState.artwork.sameAs(artwork))
        ) {
            return // Skip duplicate
        }

        // Cache the artwork if it's new and valid
        if (artwork != null) {
            cacheManager.cacheArtwork(cacheKey, artwork, mediaId, albumId, album, artist, artworkUri)
        }

        val songDetails = SongDetails(
            title = title,
            artist = artist,
            album = album,
            packageName = packageName,
            appName = appName,
            artwork = artwork ?: cacheManager.getCachedArtworkByMetadata(artist, album),
            mediaId = mediaId,
            albumId = albumId,
            artworkUri = artworkUri
        )

        repository.updateCurrentSong(songDetails)
        updateForegroundNotification(songDetails)

        // Update home screen widgets
        val widgetIntent = Intent(this@MediaSessionObserverService, com.example.albumcanvas.widget.AlbumCanvasWidget::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            val ids = AppWidgetManager.getInstance(this@MediaSessionObserverService).getAppWidgetIds(
                ComponentName(this@MediaSessionObserverService, com.example.albumcanvas.widget.AlbumCanvasWidget::class.java)
            )
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        }
        sendBroadcast(widgetIntent)

        // Set Static Wallpaper if Live Wallpaper is NOT active
        if (!isLiveWallpaperActive() && artwork != null) {
            applyStaticWallpaper(artwork, settings)
        }
    }

    private suspend fun retrieveArtwork(
        controller: MediaController,
        metadata: MediaMetadata?,
        fallbackIcon: Bitmap?
    ): Bitmap? = withContext(Dispatchers.IO) {
        if (metadata == null) return@withContext fallbackIcon

        // Priority 1: METADATA_KEY_ALBUM_ART_URI
        metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI)?.let { uri ->
            loadBitmapFromUri(uri)?.let { return@withContext it }
        }

        // Priority 2: METADATA_KEY_DISPLAY_ICON_URI
        metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI)?.let { uri ->
            loadBitmapFromUri(uri)?.let { return@withContext it }
        }

        // Priority 3: METADATA_KEY_ALBUM_ART
        metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)?.let { return@withContext it }

        // Priority 4: METADATA_KEY_ART
        metadata.getBitmap(MediaMetadata.METADATA_KEY_ART)?.let { return@withContext it }

        // Priority 5: Notification Large Icon
        return@withContext fallbackIcon
    }

    private suspend fun loadBitmapFromUri(uriStr: String): Bitmap? {
        if (uriStr.isBlank()) return null
        return try {
            val imageLoader = ImageLoader(this)
            val request = ImageRequest.Builder(this)
                .data(uriStr)
                .allowHardware(false) // Must be software bitmap for pixel processing
                .build()
            val result = imageLoader.execute(request)
            (result.drawable as? BitmapDrawable)?.bitmap
        } catch (e: Exception) {
            null
        }
    }

    private fun isAd(title: String, artist: String, album: String): Boolean {
        val adKeywords = listOf("advertisement", "spotify ad", "sponsored", "ad", "audio ad")
        val t = title.lowercase()
        val ar = artist.lowercase()
        val al = album.lowercase()
        return adKeywords.any { t.contains(it) || ar.contains(it) || al.contains(it) }
    }

    private fun getAppName(packageName: String): String {
        val pm = packageManager
        return try {
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName.substringAfterLast('.')
        }
    }

    private fun isLiveWallpaperActive(): Boolean {
        val wallpaperManager = WallpaperManager.getInstance(this)
        val info = wallpaperManager.wallpaperInfo
        return info?.packageName == packageName
    }

    private suspend fun applyStaticWallpaper(artwork: Bitmap, settings: SettingsData) = withContext(Dispatchers.IO) {
        val wallpaperManager = WallpaperManager.getInstance(this@MediaSessionObserverService)
        try {
            val metrics = resources.displayMetrics
            val screenWidth = metrics.widthPixels
            val screenHeight = metrics.heightPixels

            val colors = ImageProcessor.extractPalette(artwork)
            val processed = ImageProcessor.processWallpaper(artwork, screenWidth, screenHeight, settings, colors)

            val flags = when (settings.wallpaperTarget) {
                "HOME" -> WallpaperManager.FLAG_SYSTEM
                "LOCK" -> WallpaperManager.FLAG_LOCK
                else -> WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
            }

            wallpaperManager.setBitmap(processed, null, true, flags)
            
            val current = repository.currentSong.value
            if (current != null) {
                repository.logWallpaperChange(
                    current.title,
                    current.artist,
                    current.album,
                    current.packageName,
                    true
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            val current = repository.currentSong.value
            if (current != null) {
                repository.logWallpaperChange(
                    current.title,
                    current.artist,
                    current.album,
                    current.packageName,
                    false
                )
            }
        }
    }

    private fun startForegroundServiceNotification() {
        val notification = NotificationHelper.createNotification(
            context = this,
            appName = "Waiting for media...",
            songTitle = "AlbumCanvas active",
            isPlaying = false
        )
        startForeground(FOREGROUND_NOTIFICATION_ID, notification)
    }

    private fun updateForegroundNotification(song: SongDetails) {
        val notification = NotificationHelper.createNotification(
            context = this,
            appName = song.appName,
            songTitle = "${song.title} - ${song.artist}",
            isPlaying = true
        )
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.notify(FOREGROUND_NOTIFICATION_ID, notification)
    }

    companion object {
        const val FOREGROUND_NOTIFICATION_ID = 1001
    }
}
