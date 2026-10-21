package de.kindermaenner.monatsblitz.infrastructure.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import de.kindermaenner.monatsblitz.domain.model.ModePreselectionType
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
    private val autoAdoptOpenTournamentKey = booleanPreferencesKey("auto_adopt_open_tournament")

    private val modePreselectionTypeKey = stringPreferencesKey("mode_preselection_type")
    private val fixedModeKey = stringPreferencesKey("fixed_mode")
    private val rotationModesKey = stringPreferencesKey("rotation_modes")
    private val standaloneOfflineModeKey = booleanPreferencesKey("standalone_offline_mode")

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

    override val autoAdoptOpenTournament: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[autoAdoptOpenTournamentKey] ?: false
    }

    override val modePreselectionType: Flow<ModePreselectionType> = dataStore.data.map { preferences ->
        val raw = preferences[modePreselectionTypeKey]
        raw?.let { runCatching { ModePreselectionType.valueOf(it) }.getOrNull() } ?: ModePreselectionType.NONE
    }

    override val fixedMode: Flow<String> = dataStore.data.map { preferences ->
        preferences[fixedModeKey] ?: ""
    }

    override val rotationModes: Flow<List<String>> = dataStore.data.map { preferences ->
        val raw = preferences[rotationModesKey]
        if (raw.isNullOrBlank()) {
            emptyList()
        } else {
            raw.split(";").map { it.trim() }.filter { it.isNotEmpty() }
        }
    }

    override val standaloneOfflineMode: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[standaloneOfflineModeKey] ?: false
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

    override suspend fun setAutoAdoptOpenTournament(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[autoAdoptOpenTournamentKey] = enabled
        }
    }

    override suspend fun setModePreselectionType(type: ModePreselectionType) {
        dataStore.edit { preferences ->
            preferences[modePreselectionTypeKey] = type.name
        }
    }

    override suspend fun setFixedMode(mode: String) {
        dataStore.edit { preferences ->
            preferences[fixedModeKey] = mode
        }
    }

    override suspend fun setRotationModes(modes: List<String>) {
        dataStore.edit { preferences ->
            preferences[rotationModesKey] = modes.joinToString(";")
        }
    }

    override suspend fun setStandaloneOfflineMode(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[standaloneOfflineModeKey] = enabled
        }
    }

    override suspend fun isConfigured(): Boolean {
        val prefs = dataStore.data.first()
        val url = runCatching { prefs[serverUrlKey]?.trim().orEmpty() }.getOrDefault("")
        val key = runCatching { prefs[apiKeyKey]?.trim().orEmpty() }.getOrDefault("")
        if (url.isNotEmpty() && key.isNotEmpty()) return true
        val offline = runCatching { prefs[standaloneOfflineModeKey] }.getOrNull() ?: false
        return offline
    }

    override suspend fun isServerConfigured(): Boolean {
        val prefs = dataStore.data.first()
        val url = runCatching { prefs[serverUrlKey]?.trim().orEmpty() }.getOrDefault("")
        val key = runCatching { prefs[apiKeyKey]?.trim().orEmpty() }.getOrDefault("")
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
