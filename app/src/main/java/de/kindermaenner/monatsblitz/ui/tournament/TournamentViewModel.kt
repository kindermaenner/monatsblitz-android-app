package de.kindermaenner.monatsblitz.ui.tournament

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.kindermaenner.monatsblitz.domain.model.GameResult
import de.kindermaenner.monatsblitz.domain.repository.TournamentRepository
import de.kindermaenner.monatsblitz.domain.usecase.CreateTournamentRankingsUseCase
import de.kindermaenner.monatsblitz.domain.usecase.SetGameResultUseCase
import de.kindermaenner.monatsblitz.ui.navigation.AppRoute
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TournamentViewModel(
    private val repository: TournamentRepository,
    private val setGameResultUseCase: SetGameResultUseCase,
    private val updateRankingUseCase: CreateTournamentRankingsUseCase,
    tournamentId: Long) : ViewModel() {
    
    private val _selectedLeg = MutableStateFlow(1)
    val selectedLeg = _selectedLeg.asStateFlow()
    
    val uiState =
        combine(
            repository.observeTournament(tournamentId),
            _selectedLeg
        ) { tournament, leg ->
            TournamentUiState(
                tournament = tournament,
                leg = leg,
                isLoading = false
            )
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            TournamentUiState()
        )

    fun setResult(rowIndex : Int, columnIndex : Int, round : Int, result : GameResult) {
        val tournament = uiState.value.tournament ?: return
        viewModelScope.launch {
            Log.i("TournamentViewModel", "setGameResultUseCase: tournament=${tournament.Id}, rowIndex=$rowIndex, columnIndex=$columnIndex, round=$round, result=$result")
            setGameResultUseCase(tournament.Id, tournament.playerIds[rowIndex], tournament.playerIds[columnIndex], round, result)
        }
    }

    fun selectLeg(leg: Int) {
        val tournament = uiState.value.tournament ?: return
        if (leg in 1..tournament.rounds) {
            _selectedLeg.value = leg
        }
    }

    fun updateRanking() {
        val tournament = uiState.value.tournament ?: return
        viewModelScope.launch {
            updateRankingUseCase(tournament)
        }
    }
}