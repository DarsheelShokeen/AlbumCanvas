package com.example.albumcanvas.service

import android.app.WallpaperManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RenderEffect
import android.graphics.RenderNode
import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import com.example.albumcanvas.data.CacheManager
import com.example.albumcanvas.data.MainRepository
import com.example.albumcanvas.data.SongDetails
import com.example.albumcanvas.data.pref.SettingsData
import com.example.albumcanvas.data.pref.SettingsDataStore
import com.example.albumcanvas.utils.ImageProcessor
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class AlbumCanvasWallpaperService : WallpaperService() {

    @Inject lateinit var repository: MainRepository
    @Inject lateinit var settingsDataStore: SettingsDataStore
    @Inject lateinit var cacheManager: CacheManager

    override fun onCreateEngine(): Engine {
        return CanvasEngine()
    }

    private inner class CanvasEngine : Engine() {
        private val engineScope = CoroutineScope(Dispatchers.Main + Job())
        
        private var currentBitmap: Bitmap? = null
        private var previousBitmap: Bitmap? = null
        
        private var animProgress = 1.0f
        private var animStartTime = 0L
        private var isAnimating = false
        private var isVisible = false
        
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        private val handler = Handler(Looper.getMainLooper())
        
        private var screenWidth = 1080
        private var screenHeight = 1920

        private val drawRunnable = Runnable { drawFrame() }

        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)
            
            // Listen to current song changes from Repository
            repository.currentSong
                .onEach { song ->
                    loadNewArtwork(song)
                }
                .launchIn(engineScope)
        }

        override fun onDestroy() {
            super.onDestroy()
            handler.removeCallbacks(drawRunnable)
            engineScope.cancel()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            this.isVisible = visible
            if (visible) {
                if (isAnimating) {
                    scheduleNextFrame()
                } else {
                    drawFrame()
                }
            } else {
                handler.removeCallbacks(drawRunnable)
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            screenWidth = width
            screenHeight = height
            
            // Re-render current frame with new sizes
            engineScope.launch {
                loadNewArtwork(repository.currentSong.value)
            }
        }

        private fun loadNewArtwork(song: SongDetails?) {
            engineScope.launch {
                val settings = settingsDataStore.settingsFlow.first()
                if (!settings.isServiceEnabled) return@launch

                val artwork = song?.artwork
                
                withContext(Dispatchers.Default) {
                    val colors = if (artwork != null) {
                        ImageProcessor.extractPalette(artwork)
                    } else {
                        ImageProcessor.PaletteColors(
                            primary = 0xFF121212.toInt(),
                            accent = 0xFF4CAF50.toInt(),
                            vibrant = 0xFF4CAF50.toInt(),
                            muted = 0xFF121212.toInt(),
                            lightVibrant = 0xFFE8F5E9.toInt(),
                            darkMuted = 0xFF1B5E20.toInt()
                        )
                    }

                    // Process bitmap. In S+ (Android 12+), we skip CPU blur to use hardware RenderEffect
                    val modifiedSettings = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        settings.copy(blurAmount = 0)
                    } else {
                        settings
                    }

                    val rawProcessed = if (artwork != null) {
                        ImageProcessor.processWallpaper(artwork, screenWidth, screenHeight, modifiedSettings, colors)
                    } else {
                        // Create default gradient or black background
                        createDefaultBackground(settings, colors)
                    }

                    previousBitmap = currentBitmap
                    currentBitmap = rawProcessed
                    
                    // Setup animation details
                    if (previousBitmap != null && settings.transitionEffect != "NONE") {
                        animProgress = 0.0f
                        animStartTime = System.currentTimeMillis()
                        isAnimating = true
                    } else {
                        animProgress = 1.0f
                        isAnimating = false
                    }
                }
                
                if (isVisible) {
                    scheduleNextFrame()
                }
                
                // Log successful update in Room
                if (song != null) {
                    repository.logWallpaperChange(
                        song.title,
                        song.artist,
                        song.album,
                        song.packageName,
                        true
                    )
                }
            }
        }

        private fun createDefaultBackground(settings: SettingsData, colors: ImageProcessor.PaletteColors): Bitmap {
            val bmp = Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            
            if (settings.amoledBlack) {
                canvas.drawColor(Color.BLACK)
            } else if (settings.gradientBackground) {
                val gradient = LinearGradient(
                    0f, 0f, screenWidth.toFloat(), screenHeight.toFloat(),
                    intArrayOf(colors.darkMuted, colors.primary, colors.muted),
                    null, Shader.TileMode.CLAMP
                )
                paint.shader = gradient
                canvas.drawRect(0f, 0f, screenWidth.toFloat(), screenHeight.toFloat(), paint)
            } else {
                canvas.drawColor(colors.primary)
            }
            return bmp
        }

        private fun scheduleNextFrame() {
            handler.removeCallbacks(drawRunnable)
            if (isVisible) {
                handler.postDelayed(drawRunnable, 16) // Aim for ~60fps
            }
        }

        private fun drawFrame() {
            val holder = surfaceHolder ?: return
            var canvas: Canvas? = null
            try {
                // Lock hardware canvas on Android 26+ for GPU acceleration
                canvas = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    holder.lockHardwareCanvas()
                } else {
                    holder.lockCanvas()
                }

                if (canvas != null) {
                    // Update animation progress
                    if (isAnimating) {
                        updateAnimationProgress()
                    }

                    renderCanvas(canvas)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                if (canvas != null) {
                    try {
                        holder.unlockCanvasAndPost(canvas)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            if (isAnimating && isVisible) {
                scheduleNextFrame()
            } else {
                isAnimating = false
            }
        }

        private fun updateAnimationProgress() {
            engineScope.launch {
                val settings = settingsDataStore.settingsFlow.first()
                val duration = settings.animationSpeed.coerceIn(100, 3000).toFloat()
                val elapsed = System.currentTimeMillis() - animStartTime
                animProgress = (elapsed / duration).coerceIn(0.0f, 1.0f)
                if (animProgress >= 1.0f) {
                    isAnimating = false
                    previousBitmap = null // Release memory
                }
            }
        }

        private fun renderCanvas(canvas: Canvas) {
            val current = currentBitmap ?: return
            val prev = previousBitmap
            
            val settings = engineScope.launch {
                // Pre-fetch settings asynchronously
            }
            
            // For drawing transitions, we need settings details.
            // Let's block or run synchronously for simple reads.
            // We can read settings from SettingsDataStore flow cache.
            val currentSettings = runBlockingSettings()
            
            // RenderEffect GPU Blur implementation on Android 12+ (API 31)
            val shouldBlur = currentSettings.blurAmount > 0
            val blurRadius = currentSettings.blurAmount.toFloat().coerceIn(1f, 100f)

            if (prev != null && isAnimating && animProgress < 1.0f) {
                // Run Transition rendering
                drawTransitionFrame(canvas, prev, current, currentSettings, shouldBlur, blurRadius)
            } else {
                // Render single current frame (static)
                drawSingleFrame(canvas, current, shouldBlur, blurRadius)
            }
        }

        private fun runBlockingSettings(): SettingsData {
            var data = SettingsData()
            val job = engineScope.launch {
                data = settingsDataStore.settingsFlow.first()
            }
            // To ensure we draw frames synchronously on binder threads,
            // we can block briefly, or settings data store cache is usually instant.
            // A runBlocking or short join is sufficient.
            kotlin.runCatching {
                runBlockingHelper {
                    data = settingsDataStore.settingsFlow.first()
                }
            }
            return data
        }

        private fun drawSingleFrame(canvas: Canvas, bitmap: Bitmap, shouldBlur: Boolean, blurRadius: Float) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && shouldBlur && canvas.isHardwareAccelerated) {
                val renderNode = RenderNode("BlurFrame").apply {
                    setPosition(0, 0, screenWidth, screenHeight)
                    setRenderEffect(RenderEffect.createBlurEffect(blurRadius, blurRadius, Shader.TileMode.CLAMP))
                }
                val nodeCanvas = renderNode.beginRecording()
                nodeCanvas.drawBitmap(bitmap, 0f, 0f, paint)
                renderNode.endRecording()
                canvas.drawRenderNode(renderNode)
            } else {
                canvas.drawBitmap(bitmap, 0f, 0f, paint)
            }
        }

        private fun drawTransitionFrame(
            canvas: Canvas,
            prev: Bitmap,
            current: Bitmap,
            settings: SettingsData,
            shouldBlur: Boolean,
            blurRadius: Float
        ) {
            when (settings.transitionEffect) {
                "CROSSFADE" -> {
                    // Draw old
                    paint.alpha = ((1f - animProgress) * 255).toInt()
                    drawSingleFrame(canvas, prev, shouldBlur, blurRadius)
                    
                    // Draw new
                    paint.alpha = (animProgress * 255).toInt()
                    drawSingleFrame(canvas, current, shouldBlur, blurRadius)
                    paint.alpha = 255
                }
                "ZOOM_IN" -> {
                    // Draw old
                    drawSingleFrame(canvas, prev, shouldBlur, blurRadius)

                    // Draw new scaling in
                    canvas.save()
                    val scale = 0.9f + (0.1f * animProgress)
                    canvas.scale(scale, scale, screenWidth / 2f, screenHeight / 2f)
                    paint.alpha = (animProgress * 255).toInt()
                    drawSingleFrame(canvas, current, shouldBlur, blurRadius)
                    canvas.restore()
                    paint.alpha = 255
                }
                "ZOOM_OUT" -> {
                    // Draw old
                    drawSingleFrame(canvas, prev, shouldBlur, blurRadius)

                    // Draw new scaling out
                    canvas.save()
                    val scale = 1.1f - (0.1f * animProgress)
                    canvas.scale(scale, scale, screenWidth / 2f, screenHeight / 2f)
                    paint.alpha = (animProgress * 255).toInt()
                    drawSingleFrame(canvas, current, shouldBlur, blurRadius)
                    canvas.restore()
                    paint.alpha = 255
                }
                "BLUR_FADE" -> {
                    // Draw transition where blur amount interpolates
                    val dynamicRadius = if (shouldBlur) blurRadius * animProgress else 50f * (1f - animProgress)
                    paint.alpha = ((1f - animProgress) * 255).toInt()
                    drawSingleFrame(canvas, prev, true, 50f * animProgress)
                    
                    paint.alpha = (animProgress * 255).toInt()
                    drawSingleFrame(canvas, current, true, dynamicRadius)
                    paint.alpha = 255
                }
                "SLIDE" -> {
                    // Slide old out left, new in right
                    val offset = screenWidth * animProgress
                    
                    canvas.save()
                    canvas.translate(-offset, 0f)
                    drawSingleFrame(canvas, prev, shouldBlur, blurRadius)
                    canvas.restore()

                    canvas.save()
                    canvas.translate(screenWidth - offset, 0f)
                    drawSingleFrame(canvas, current, shouldBlur, blurRadius)
                    canvas.restore()
                }
                "SCALE" -> {
                    canvas.save()
                    canvas.scale(animProgress, animProgress, screenWidth / 2f, screenHeight / 2f)
                    drawSingleFrame(canvas, current, shouldBlur, blurRadius)
                    canvas.restore()
                }
                "KEN_BURNS" -> {
                    // Slow pan and zoom
                    val scale = 1.0f + (0.05f * animProgress)
                    val dx = (screenWidth * 0.02f) * animProgress
                    canvas.save()
                    canvas.translate(dx, 0f)
                    canvas.scale(scale, scale, screenWidth / 2f, screenHeight / 2f)
                    
                    paint.alpha = ((1f - animProgress) * 255).toInt()
                    drawSingleFrame(canvas, prev, shouldBlur, blurRadius)
                    
                    paint.alpha = (animProgress * 255).toInt()
                    drawSingleFrame(canvas, current, shouldBlur, blurRadius)
                    canvas.restore()
                    paint.alpha = 255
                }
                else -> { // NONE fallback
                    drawSingleFrame(canvas, current, shouldBlur, blurRadius)
                }
            }
        }

        private fun runBlockingHelper(block: suspend () -> Unit) {
            val job = Job()
            CoroutineScope(Dispatchers.Default + job).launch {
                block()
                job.complete()
            }
            // Block thread until complete
            while (!job.isCompleted) {
                Thread.sleep(1)
            }
        }
    }
}
