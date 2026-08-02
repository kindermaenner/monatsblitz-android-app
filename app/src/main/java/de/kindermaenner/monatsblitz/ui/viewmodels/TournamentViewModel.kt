package de.kindermaenner.monatsblitz.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.kindermaenner.monatsblitz.domain.model.Game
import de.kindermaenner.monatsblitz.domain.model.GameResult
import de.kindermaenner.monatsblitz.domain.model.NewGame
import de.kindermaenner.monatsblitz.domain.repository.TournamentRepository
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.GameDao
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.mapper.toEntity
import de.kindermaenner.monatsblitz.ui.screens.TournamentUiState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TournamentViewModel(
    private val repository: TournamentRepository,
    private val gameDao : GameDao,
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
        var leg = 1
        if ((tournament.rounds == 2) && (rowIndex < columnIndex)) {
            leg = 2
        }
        viewModelScope.launch {
            val playerId1 = tournament.playerIds[rowIndex]
            val playerId2 = tournament.playerIds[columnIndex]
            repository.updateGameResult(tournament.Id, playerId1, playerId2, leg, result)
            Log.i("setResult", "Updated game result for tournament ${tournament.Id}, players $playerId1 vs $playerId2, leg $leg to $result")
        }
    }
}