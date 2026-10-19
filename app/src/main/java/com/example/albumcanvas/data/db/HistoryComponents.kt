package com.example.albumcanvas.data.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "wallpaper_history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long,
    val title: String,
    val artist: String,
    val album: String,
    val playerPackage: String,
    val success: Boolean
)

@Dao
interface HistoryDao {
    @Insert
    suspend fun insertHistory(history: HistoryEntity)

    @Query("SELECT * FROM wallpaper_history ORDER BY timestamp DESC LIMIT 100")
    fun getAllHistoryFlow(): Flow<List<HistoryEntity>>

    @Query("SELECT COUNT(*) FROM wallpaper_history WHERE timestamp >= :startOfDay")
    fun getChangesCountSince(startOfDay: Long): Flow<Int>

    @Query("SELECT * FROM wallpaper_history ORDER BY timestamp DESC LIMIT 1")
    fun getLastUpdate(): Flow<HistoryEntity?>

    @Query("DELETE FROM wallpaper_history")
    suspend fun clearHistory()
}
