package de.kindermaenner.monatsblitz.ui.ranking

sealed interface RankingUiState {
    data object Loading : RankingUiState
    data class Ready(
        val rows: List<RankingRowData>,
        val openGamesCount: Int = 0,
        val isReadOnly: Boolean = false,
        val isFinalizing: Boolean = false,
        val isFinalized: Boolean = false,
        val finalizeError: String? = null
    ) : RankingUiState
}