package de.kindermaenner.monatsblitz.infrastructure.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AppSettingsStorage(private val dataStore: DataStore<Preferences>) {

    private val showDetailedSyncIndicatorsKey = booleanPreferencesKey("show_detailed_sync_indicators")

    val showDetailedSyncIndicators: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[showDetailedSyncIndicatorsKey] ?: true
    }

    suspend fun setShowDetailedSyncIndicators(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[showDetailedSyncIndicatorsKey] = enabled
        }
    }
}
