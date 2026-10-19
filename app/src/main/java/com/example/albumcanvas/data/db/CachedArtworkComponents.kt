package com.example.albumcanvas.data.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "cached_artwork")
data class CachedArtworkEntity(
    @PrimaryKey val cacheKey: String, // composite lookup key
    val localPath: String,
    val artist: String,
    val albumName: String,
    val title: String,
    val mediaId: String?,
    val albumId: String?,
    val uriHash: String?,
    val cacheTime: Long
)

@Dao
interface CachedArtworkDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCache(cache: CachedArtworkEntity)

    @Query("SELECT * FROM cached_artwork WHERE cacheKey = :key LIMIT 1")
    suspend fun getCacheByKey(key: String): CachedArtworkEntity?

    @Query("SELECT * FROM cached_artwork WHERE artist = :artist AND albumName = :albumName LIMIT 1")
    suspend fun getCacheByArtistAndAlbum(artist: String, albumName: String): CachedArtworkEntity?

    @Query("SELECT * FROM cached_artwork ORDER BY cacheTime ASC")
    suspend fun getAllCacheSorted(): List<CachedArtworkEntity>

    @Query("SELECT COUNT(*) FROM cached_artwork")
    fun getCacheCountFlow(): Flow<Int>

    @Query("DELETE FROM cached_artwork WHERE cacheKey = :key")
    suspend fun deleteCache(key: String)

    @Query("DELETE FROM cached_artwork")
    suspend fun clearCacheMetadata()
}
