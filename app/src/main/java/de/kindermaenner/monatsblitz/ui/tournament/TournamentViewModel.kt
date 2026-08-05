package de.kindermaenner.monatsblitz.ui.tournament

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.kindermaenner.monatsblitz.domain.model.GameResult
import de.kindermaenner.monatsblitz.domain.repository.TournamentRepository
import de.kindermaenner.monatsblitz.domain.usecase.SetGameResultUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TournamentViewModel(
    private val repository: TournamentRepository,
    private val setGameResultUseCase: SetGameResultUseCase,
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
        val tournament = uiState.value.tournament ?: return
        viewModelScope.launch {
            setGameResultUseCase(tournament.Id, tournament.playerIds[rowIndex], tournament.playerIds[columnIndex], uiState.value.leg, result)
        }
    }
}