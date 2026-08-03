package de.kindermaenner.monatsblitz.ui.tournament

import de.kindermaenner.monatsblitz.domain.model.Tournament

data class TournamentUiState(

    val tournament: Tournament? = null,

    val leg : Int = 1,

    val isLoading: Boolean = true,

    val error: String? = null
)