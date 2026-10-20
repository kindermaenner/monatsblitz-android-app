package de.kindermaenner.monatsblitz.infrastructure.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import de.kindermaenner.monatsblitz.domain.model.PlayerSortField
import de.kindermaenner.monatsblitz.domain.model.PlayerSortOrder
import de.kindermaenner.monatsblitz.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

class AppSettingsStorage(private val dataStore: DataStore<Preferences>) : SettingsRepository {

    private val showDetailedSyncIndicatorsKey = booleanPreferencesKey("show_detailed_sync_indicators")
    private val serverUrlKey = stringPreferencesKey("server_url")
    private val apiKeyKey = stringPreferencesKey("api_key")
    private val playerSortFieldKey = stringPreferencesKey("player_sort_field")
    private val playerSortOrderKey = stringPreferencesKey("player_sort_order")
    private val useShortPlayerNamesKey = booleanPreferencesKey("use_short_player_names")

    @Volatile
    private var cachedServerUrl: String? = null

    @Volatile
    private var cachedApiKey: String? = null

    override val showDetailedSyncIndicators: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[showDetailedSyncIndicatorsKey] ?: true
    }

    override val serverUrl: Flow<String> = dataStore.data.map { preferences ->
        val url = preferences[serverUrlKey] ?: ""
        cachedServerUrl = url
        url
    }

    override val apiKey: Flow<String> = dataStore.data.map { preferences ->
        val key = preferences[apiKeyKey] ?: ""
        cachedApiKey = key
        key
    }

    override val playerSortField: Flow<PlayerSortField> = dataStore.data.map { preferences ->
        val raw = preferences[playerSortFieldKey]
        raw?.let { runCatching { PlayerSortField.valueOf(it) }.getOrNull() } ?: PlayerSortField.LAST_NAME
    }

    override val playerSortOrder: Flow<PlayerSortOrder> = dataStore.data.map { preferences ->
        val raw = preferences[playerSortOrderKey]
        raw?.let { runCatching { PlayerSortOrder.valueOf(it) }.getOrNull() } ?: PlayerSortOrder.ASCENDING
    }

    override val useShortPlayerNames: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[useShortPlayerNamesKey] ?: true
    }

    override suspend fun setShowDetailedSyncIndicators(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[showDetailedSyncIndicatorsKey] = enabled
        }
    }

    override suspend fun setServerUrl(url: String) {
        cachedServerUrl = url
        dataStore.edit { preferences ->
            preferences[serverUrlKey] = url
        }
    }

    override suspend fun setApiKey(apiKey: String) {
        cachedApiKey = apiKey
        dataStore.edit { preferences ->
            preferences[apiKeyKey] = apiKey
        }
    }

    override suspend fun setPlayerSortField(field: PlayerSortField) {
        dataStore.edit { preferences ->
            preferences[playerSortFieldKey] = field.name
        }
    }

    override suspend fun setPlayerSortOrder(order: PlayerSortOrder) {
        dataStore.edit { preferences ->
            preferences[playerSortOrderKey] = order.name
        }
    }

    override suspend fun setUseShortPlayerNames(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[useShortPlayerNamesKey] = enabled
        }
    }

    override suspend fun isConfigured(): Boolean {
        val prefs = dataStore.data.first()
        val url = prefs[serverUrlKey]?.trim().orEmpty()
        val key = prefs[apiKeyKey]?.trim().orEmpty()
        return url.isNotEmpty() && key.isNotEmpty()
    }

    fun getServerUrlSync(): String {
        cachedServerUrl?.let { return it }
        return runBlocking {
            val url = dataStore.data.first()[serverUrlKey].orEmpty()
            cachedServerUrl = url
            url
        }
    }

    fun getApiKeySync(): String {
        cachedApiKey?.let { return it }
        return runBlocking {
            val key = dataStore.data.first()[apiKeyKey].orEmpty()
            cachedApiKey = key
            key
        }
    }
}
