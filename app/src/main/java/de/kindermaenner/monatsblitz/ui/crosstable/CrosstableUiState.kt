package de.kindermaenner.monatsblitz.ui.crosstable

import de.kindermaenner.monatsblitz.domain.model.SyncStatus
import de.kindermaenner.monatsblitz.domain.model.Tournament
import de.kindermaenner.monatsblitz.ui.crosstable.components.CrosstableRowData

data class CrosstableUiState(
    val tournament: Tournament? = null,
    val leg: Int = 1,
    val playerPoints: Map<Long, Double> = emptyMap(),
    val rows: List<CrosstableRowData> = emptyList(),
    val isLoading: Boolean = true,
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val showDetailedSyncIndicators: Boolean = true,
    val error: String? = null
)
