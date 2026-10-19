package com.example.albumcanvas.di

import android.content.Context
import androidx.room.Room
import com.example.albumcanvas.data.db.AppDatabase
import com.example.albumcanvas.data.db.CachedArtworkDao
import com.example.albumcanvas.data.db.HistoryDao
import com.example.albumcanvas.data.db.PlayerAppDao
import com.example.albumcanvas.data.pref.SettingsDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "album_canvas_database"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun provideHistoryDao(db: AppDatabase): HistoryDao {
        return db.historyDao()
    }

    @Provides
    @Singleton
    fun providePlayerAppDao(db: AppDatabase): PlayerAppDao {
        return db.playerAppDao()
    }

    @Provides
    @Singleton
    fun provideCachedArtworkDao(db: AppDatabase): CachedArtworkDao {
        return db.cachedArtworkDao()
    }

    @Provides
    @Singleton
    fun provideSettingsDataStore(@ApplicationContext context: Context): SettingsDataStore {
        return SettingsDataStore(context)
    }
}
