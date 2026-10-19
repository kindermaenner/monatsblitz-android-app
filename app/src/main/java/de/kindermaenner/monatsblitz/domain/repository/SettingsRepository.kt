package de.kindermaenner.monatsblitz.domain.repository

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val showDetailedSyncIndicators: Flow<Boolean>

    suspend fun setShowDetailedSyncIndicators(enabled: Boolean)
}
