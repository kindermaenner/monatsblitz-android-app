package de.kindermaenner.monatsblitz.ui.ranking

sealed interface RankingUiState {
    data object Loading : RankingUiState
    data class Ready(
        val rows: List<RankingRowData>,
        val isFinalizing: Boolean = false,
        val isFinalized: Boolean = false,
        val finalizeError: String? = null
    ) : RankingUiState
}