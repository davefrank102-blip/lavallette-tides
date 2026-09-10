package com.lavallette.tides.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "lavallette_cache")

class CacheStore(private val context: Context) {
    private val tideKey = stringPreferencesKey("tide_json")
    private val weatherKey = stringPreferencesKey("weather_json")
    private val savedAtKey = longPreferencesKey("saved_at")

    suspend fun save(tideJson: String, weatherJson: String, savedAt: Long) {
        context.dataStore.edit { prefs ->
            prefs[tideKey] = tideJson
            prefs[weatherKey] = weatherJson
            prefs[savedAtKey] = savedAt
        }
    }

    suspend fun load(): Triple<String, String, Long>? {
        val prefs = context.dataStore.data.map { it }.first()
        val tide = prefs[tideKey] ?: return null
        val weather = prefs[weatherKey] ?: return null
        val saved = prefs[savedAtKey] ?: 0L
        return Triple(tide, weather, saved)
    }
}
