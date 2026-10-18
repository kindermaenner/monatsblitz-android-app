package de.kindermaenner.monatsblitz.ui.crosstable

import de.kindermaenner.monatsblitz.domain.model.Tournament
import de.kindermaenner.monatsblitz.infrastructure.sync.SyncStatus

data class CrosstableUiState(
    val tournament: Tournament? = null,
    val leg: Int = 1,
    val playerPoints: Map<Long, Double> = emptyMap(),
    val isLoading: Boolean = true,
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val showDetailedSyncIndicators: Boolean = true,
    val error: String? = null
)
