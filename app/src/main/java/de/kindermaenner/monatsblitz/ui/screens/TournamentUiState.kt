package de.kindermaenner.monatsblitz.ui.screens

import de.kindermaenner.monatsblitz.domain.model.Tournament

data class TournamentUiState(

    val tournament: Tournament? = null,

    val isLoading: Boolean = true,

    val error: String? = null
)
