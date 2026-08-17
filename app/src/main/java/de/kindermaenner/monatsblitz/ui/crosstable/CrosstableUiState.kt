package de.kindermaenner.monatsblitz.ui.crosstable

import de.kindermaenner.monatsblitz.domain.model.Tournament

data class CrosstableUiState(
    val tournament: Tournament? = null,
    val leg: Int = 1,
    val playerPoints: Map<Long, Double> = emptyMap(),
    val isLoading: Boolean = true,
    val error: String? = null
)
