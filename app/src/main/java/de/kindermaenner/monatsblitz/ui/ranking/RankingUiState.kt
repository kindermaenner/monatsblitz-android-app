package de.kindermaenner.monatsblitz.ui.ranking

import de.kindermaenner.monatsblitz.domain.model.Tournament

sealed interface RankingUiState {
    data object Loading : RankingUiState
    data class Ready(
        val rows: List<RankingRowData>,
        val openGamesCount: Int = 0,
        val isReadOnly: Boolean = false,
        val isFinalizing: Boolean = false,
        val isFinalized: Boolean = false,
        val finalizeError: String? = null,
        val tournament: Tournament? = null
    ) : RankingUiState
}