package de.kindermaenner.monatsblitz.infrastructure

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

data class TournamentState(
    val tournamentId: Int,
    val playerIds: List<Int>,
    val finalized: Boolean
)

class TournamentStorage(private val context: Context) {
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "tournament_state")
    private val dataStore get() = context.dataStore
    
    private val tournamentIdKey = intPreferencesKey("tournament_id")
    private val playerIdsKey = stringPreferencesKey("player_ids")
    private val finalizedKey = booleanPreferencesKey("tournament_finalized")

    suspend fun resetAll() {
        dataStore.edit { it.clear() }
    }

    suspend fun saveTournamentState(tournamentId : Int, playerIds: List<Int>, finalized: Boolean) {
        val playerIdsJson = Json.encodeToString(playerIds)
        dataStore.edit { preferences ->
            preferences[tournamentIdKey] = tournamentId
            preferences[playerIdsKey] = playerIdsJson
            preferences[finalizedKey] = finalized
        }
    }

    fun getTournamentState(): Flow<TournamentState?> {
        return dataStore.data.map { preferences ->
            val id = preferences[tournamentIdKey]
            val finalized = preferences[finalizedKey] ?: false
            val playerIdsJson = preferences[playerIdsKey] ?: "[]"
            val playerIds = Json.decodeFromString<List<Int>>(playerIdsJson)
            if (id != null) TournamentState(id, playerIds, finalized) else null
        }
    }

    suspend fun finalizeTournament() {
        dataStore.edit { preferences ->
            preferences[finalizedKey] = true
        }
    }
}
