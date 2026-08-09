package de.kindermaenner.monatsblitz.infrastructure

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class TournamentState(
    val tournamentId: Long,
    val finalized: Boolean
)

class TournamentStorage(private val dataStore: DataStore<Preferences>) {
    
    private val tournamentIdKey = longPreferencesKey("tournament_id")
    private val finalizedKey = booleanPreferencesKey("tournament_finalized")

    suspend fun resetAll() {
        dataStore.edit { it.clear() }
    }

    suspend fun saveTournamentState(tournamentId : Long, finalized: Boolean) {
        dataStore.edit { preferences ->
            preferences[tournamentIdKey] = tournamentId
            preferences[finalizedKey] = finalized
        }
    }

    fun getTournamentState(): Flow<TournamentState?> {
        return dataStore.data.map { preferences ->
            val id = preferences[tournamentIdKey]
            val finalized = preferences[finalizedKey] ?: false
            if (id != null) TournamentState(id, finalized) else null
        }
    }

    suspend fun finalizeTournament() {
        dataStore.edit { preferences ->
            preferences[finalizedKey] = true
        }
    }
}
