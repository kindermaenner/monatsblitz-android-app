package de.kindermaenner.monatsblitz.ui.crosstable

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.kindermaenner.monatsblitz.domain.model.GameResult
import de.kindermaenner.monatsblitz.domain.repository.TournamentRepository
import de.kindermaenner.monatsblitz.domain.usecase.CalculatePlayerPointsUseCase
import de.kindermaenner.monatsblitz.domain.usecase.CreateTournamentRankingsUseCase
import de.kindermaenner.monatsblitz.domain.usecase.SetGameResultUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CrosstableViewModel(
    private val repository: TournamentRepository,
    private val setGameResultUseCase: SetGameResultUseCase,
    private val createTournamentRankingsUseCase: CreateTournamentRankingsUseCase,
    private val calculatePlayerPointsUseCase: CalculatePlayerPointsUseCase,
    tournamentId: Long
) : ViewModel() {

    constructor(
        repository: TournamentRepository,
        setGameResultUseCase: SetGameResultUseCase,
        createTournamentRankingsUseCase: CreateTournamentRankingsUseCase,
        tournamentId: Long
    ) : this(
        repository,
        setGameResultUseCase,
        createTournamentRankingsUseCase,
        CalculatePlayerPointsUseCase(),
        tournamentId
    )
    
    private val _selectedLeg = MutableStateFlow(1)
    val selectedLeg = _selectedLeg.asStateFlow()
    
    val uiState =
        combine(
            repository.observeTournament(tournamentId),
            _selectedLeg
        ) { tournament, leg ->
            val pointsMap = tournament?.players?.associate { player ->
                player.id to calculatePlayerPointsUseCase(tournament, player.id)
            } ?: emptyMap()

            CrosstableUiState(
                tournament = tournament,
                leg = leg,
                playerPoints = pointsMap,
                isLoading = false
            )
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            CrosstableUiState()
        )

    fun setResult(rowIndex: Int, columnIndex: Int, round: Int, result: GameResult) {
        val tournament = uiState.value.tournament ?: return
        viewModelScope.launch {
            Log.i("CrosstableViewModel", "setGameResultUseCase: tournament=${tournament.Id}, rowIndex=$rowIndex, columnIndex=$columnIndex, round=$round, result=$result")
            setGameResultUseCase(tournament.Id, tournament.playerIds[rowIndex], tournament.playerIds[columnIndex], round, result)
        }
    }

    fun prepareRankingsAndNavigate(onNavigate: () -> Unit) {
        val tournament = uiState.value.tournament ?: return
        viewModelScope.launch {
            createTournamentRankingsUseCase(tournament)
            onNavigate()
        }
    }

    fun selectLeg(leg: Int) {
        val tournament = uiState.value.tournament ?: return
        if (leg in 1..tournament.rounds) {
            _selectedLeg.value = leg
        }
    }

    fun resetTournament() {
        viewModelScope.launch {
            repository.resetTournament()
        }
    }
}
