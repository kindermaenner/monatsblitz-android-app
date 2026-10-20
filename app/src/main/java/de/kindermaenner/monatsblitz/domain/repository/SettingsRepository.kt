package de.kindermaenner.monatsblitz.domain.repository

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val showDetailedSyncIndicators: Flow<Boolean>
    val serverUrl: Flow<String>
    val apiKey: Flow<String>

    suspend fun setShowDetailedSyncIndicators(enabled: Boolean)
    suspend fun setServerUrl(url: String)
    suspend fun setApiKey(apiKey: String)
    suspend fun isConfigured(): Boolean
}
