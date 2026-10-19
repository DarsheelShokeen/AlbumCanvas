package com.example.albumcanvas.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.albumcanvas.data.db.CachedArtworkDao
import com.example.albumcanvas.data.db.CachedArtworkEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CacheManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cachedArtworkDao: CachedArtworkDao
) {
    private val cacheDir = File(context.cacheDir, "artworks").apply { mkdirs() }

    /**
     * Generate a composite cache key based on available track identifiers.
     */
    fun generateCacheKey(
        mediaId: String?,
        albumId: String?,
        albumName: String,
        artist: String,
        artworkUri: String?
    ): String {
        val uriHash = artworkUri?.let { sha256(it) } ?: ""
        return "mid:${mediaId ?: ""}_aid:${albumId ?: ""}_alb:${albumName}_art:${artist}_uh:${uriHash}"
    }

    /**
     * Retrieve a cached bitmap.
     */
    suspend fun getCachedArtwork(cacheKey: String): Bitmap? = withContext(Dispatchers.IO) {
        val entry = cachedArtworkDao.getCacheByKey(cacheKey) ?: return@withContext null
        val file = File(entry.localPath)
        if (file.exists()) {
            // Update cache access time
            cachedArtworkDao.insertCache(entry.copy(cacheTime = System.currentTimeMillis()))
            BitmapFactory.decodeFile(file.absolutePath)
        } else {
            cachedArtworkDao.deleteCache(cacheKey)
            null
        }
    }

    /**
     * Lookup cache by artist and album (offline fallback)
     */
    suspend fun getCachedArtworkByMetadata(artist: String, albumName: String): Bitmap? = withContext(Dispatchers.IO) {
        if (artist.isBlank() || albumName.isBlank()) return@withContext null
        val entry = cachedArtworkDao.getCacheByArtistAndAlbum(artist, albumName) ?: return@withContext null
        val file = File(entry.localPath)
        if (file.exists()) {
            BitmapFactory.decodeFile(file.absolutePath)
        } else {
            null
        }
    }

    /**
     * Save a bitmap to cache.
     */
    suspend fun cacheArtwork(
        cacheKey: String,
        bitmap: Bitmap,
        mediaId: String?,
        albumId: String?,
        albumName: String,
        artist: String,
        artworkUri: String?
    ): String = withContext(Dispatchers.IO) {
        val filename = "art_${sha256(cacheKey)}.webp"
        val file = File(cacheDir, filename)
        
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.WEBP, 90, out)
        }

        val entity = CachedArtworkEntity(
            cacheKey = cacheKey,
            localPath = file.absolutePath,
            artist = artist,
            albumName = albumName,
            title = "",
            mediaId = mediaId,
            albumId = albumId,
            uriHash = artworkUri?.let { sha256(it) },
            cacheTime = System.currentTimeMillis()
        )
        cachedArtworkDao.insertCache(entity)
        
        // Run auto-cleanup in background
        autoCleanup()
        
        file.absolutePath
    }

    /**
     * Automatically delete oldest cache items if limit is exceeded (e.g. 50 files or 50MB).
     */
    private suspend fun autoCleanup() = withContext(Dispatchers.IO) {
        val allCache = cachedArtworkDao.getAllCacheSorted()
        if (allCache.size > 50) {
            val toRemove = allCache.size - 50
            for (i in 0 until toRemove) {
                val entry = allCache[i]
                val file = File(entry.localPath)
                if (file.exists()) {
                    file.delete()
                }
                cachedArtworkDao.deleteCache(entry.cacheKey)
            }
        }
    }

    /**
     * Clear all cached files and DB metadata.
     */
    suspend fun clearAllCache() = withContext(Dispatchers.IO) {
        cachedArtworkDao.clearCacheMetadata()
        cacheDir.listFiles()?.forEach { file ->
            file.delete()
        }
    }

    /**
     * Retrieve cache storage metrics.
     */
    suspend fun getCacheSizeMb(): Double = withContext(Dispatchers.IO) {
        val bytes = cacheDir.listFiles()?.sumOf { it.length() } ?: 0L
        bytes.toDouble() / (1024.0 * 1024.0)
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
