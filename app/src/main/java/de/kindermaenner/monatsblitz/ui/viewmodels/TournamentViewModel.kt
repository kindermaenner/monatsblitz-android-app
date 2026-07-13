package de.kindermaenner.monatsblitz.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.kindermaenner.monatsblitz.domain.model.GameResult
import de.kindermaenner.monatsblitz.domain.repository.TournamentRepository
import de.kindermaenner.monatsblitz.ui.screens.TournamentUiState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class TournamentViewModel(
    repository: TournamentRepository,
    tournamentId: Long) : ViewModel() {
    val uiState =
        repository
            .observeTournament(tournamentId)
            .map {
                TournamentUiState(
                    tournament = it,
                    isLoading = false
                )
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                TournamentUiState()
            )

    fun setResult(rowIndex : Int, columnIndex : Int, result : GameResult) {

    }
}