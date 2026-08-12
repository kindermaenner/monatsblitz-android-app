package de.kindermaenner.monatsblitz.ui.root

sealed interface RootUiState {

    data object Loading : RootUiState

    data class Ready(val tournamentId: Long?) : RootUiState

    data class Error(val message: String) : RootUiState
}