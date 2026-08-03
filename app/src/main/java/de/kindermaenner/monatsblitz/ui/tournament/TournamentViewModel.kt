package de.kindermaenner.monatsblitz.ui.tournament

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.kindermaenner.monatsblitz.domain.model.GameResult
import de.kindermaenner.monatsblitz.domain.repository.TournamentRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TournamentViewModel(
    private val repository: TournamentRepository,
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
            val playerId1 = tournament.playerIds[rowIndex]
            val playerId2 = tournament.playerIds[columnIndex]
            repository.updateGameResult(tournament.Id, playerId1, playerId2, uiState.value.leg, result)
            repository.updateGameResult(tournament.Id, playerId2, playerId1, uiState.value.leg, result.opposite())
            Log.i("setResult", "Updated game result for tournament ${tournament.Id}, players $playerId1 vs $playerId2, leg $uiState.value.leg to $result")
        }
    }
}