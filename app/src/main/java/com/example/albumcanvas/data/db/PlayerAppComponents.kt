package com.example.albumcanvas.data.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "media_player_config")
data class PlayerAppEntity(
    @PrimaryKey val packageName: String,
    val appName: String,
    val isEnabled: Boolean = true,
    val detectedTime: Long
)

@Dao
interface PlayerAppDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlayer(player: PlayerAppEntity)

    @Query("UPDATE media_player_config SET isEnabled = :enabled WHERE packageName = :packageName")
    suspend fun updatePlayerState(packageName: String, enabled: Boolean)

    @Query("SELECT * FROM media_player_config ORDER BY appName ASC")
    fun getAllPlayersFlow(): Flow<List<PlayerAppEntity>>

    @Query("SELECT * FROM media_player_config WHERE packageName = :packageName LIMIT 1")
    suspend fun getPlayer(packageName: String): PlayerAppEntity?

    @Query("DELETE FROM media_player_config")
    suspend fun clearPlayers()
}
