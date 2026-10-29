package de.kindermaenner.monatsblitz.domain.repository

import de.kindermaenner.monatsblitz.domain.model.ModePreselectionType
import de.kindermaenner.monatsblitz.domain.model.PlayerSortField
import de.kindermaenner.monatsblitz.domain.model.PlayerSortOrder
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val showDetailedSyncIndicators: Flow<Boolean>
    val serverUrl: Flow<String>
    val apiKey: Flow<String>
    val playerSortField: Flow<PlayerSortField>
    val playerSortOrder: Flow<PlayerSortOrder>
    val useShortPlayerNames: Flow<Boolean>
    val autoAdoptOpenTournament: Flow<Boolean>
    val activePlayersCutoffMonths: Flow<Int>

    val modePreselectionType: Flow<ModePreselectionType>
    val fixedMode: Flow<String>
    val rotationModes: Flow<List<String>>

    val standaloneOfflineMode: Flow<Boolean>
    val isServerConfigured: Flow<Boolean>
    val isConfigured: Flow<Boolean>

    suspend fun setShowDetailedSyncIndicators(enabled: Boolean)
    suspend fun setServerUrl(url: String)
    suspend fun setApiKey(apiKey: String)
    suspend fun setPlayerSortField(field: PlayerSortField)
    suspend fun setPlayerSortOrder(order: PlayerSortOrder)
    suspend fun setUseShortPlayerNames(enabled: Boolean)
    suspend fun setAutoAdoptOpenTournament(enabled: Boolean)
    suspend fun setActivePlayersCutoffMonths(months: Int)

    suspend fun setModePreselectionType(type: ModePreselectionType)
    suspend fun setFixedMode(mode: String)
    suspend fun setRotationModes(modes: List<String>)
    suspend fun setStandaloneOfflineMode(enabled: Boolean)

    suspend fun isConfigured(): Boolean
    suspend fun isServerConfigured(): Boolean
}
