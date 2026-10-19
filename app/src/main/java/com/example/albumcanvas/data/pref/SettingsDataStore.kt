package com.example.albumcanvas.data.pref

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "album_canvas_settings")

@Singleton
class SettingsDataStore @Inject constructor(
    private val context: Context
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val settingsKey = stringPreferencesKey("settings_json")

    val settingsFlow: Flow<SettingsData> = context.dataStore.data.map { preferences ->
        val jsonStr = preferences[settingsKey]
        if (jsonStr != null) {
            try {
                json.decodeFromString<SettingsData>(jsonStr)
            } catch (e: Exception) {
                SettingsData()
            }
        } else {
            SettingsData()
        }
    }

    suspend fun updateSettings(update: (SettingsData) -> SettingsData) {
        context.dataStore.edit { preferences ->
            val currentSettings = preferences[settingsKey]?.let {
                try {
                    json.decodeFromString<SettingsData>(it)
                } catch (e: Exception) {
                    SettingsData()
                }
            } ?: SettingsData()
            val newSettings = update(currentSettings)
            preferences[settingsKey] = json.encodeToString(SettingsData.serializer(), newSettings)
        }
    }

    suspend fun resetToDefaults() {
        context.dataStore.edit { preferences ->
            preferences.remove(settingsKey)
        }
    }

    suspend fun importSettingsJson(jsonStr: String): Boolean {
        return try {
            val decoded = json.decodeFromString<SettingsData>(jsonStr)
            context.dataStore.edit { preferences ->
                preferences[settingsKey] = json.encodeToString(SettingsData.serializer(), decoded)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun exportSettingsJson(currentSettings: SettingsData): String {
        return json.encodeToString(SettingsData.serializer(), currentSettings)
    }
}
