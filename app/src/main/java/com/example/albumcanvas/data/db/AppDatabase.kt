package com.example.albumcanvas.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        HistoryEntity::class,
        PlayerAppEntity::class,
        CachedArtworkEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
    abstract fun playerAppDao(): PlayerAppDao
    abstract fun cachedArtworkDao(): CachedArtworkDao
}
