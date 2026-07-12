package de.kindermaenner.monatsblitz.ui.screens

sealed interface RootUiState {

    data object Loading : RootUiState

    data object ReadyWithoutTournament : RootUiState

    data class ReadyWithTournament(
        val tournamentId: Long
    ) : RootUiState

    data class Error(
        val message: String
    ) : RootUiState
}

