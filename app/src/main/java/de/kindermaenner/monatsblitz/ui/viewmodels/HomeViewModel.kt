package de.kindermaenner.monatsblitz.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.kindermaenner.monatsblitz.domain.model.GameMode
import de.kindermaenner.monatsblitz.domain.model.NewTournament
import de.kindermaenner.monatsblitz.domain.repository.PlayerRepository
import de.kindermaenner.monatsblitz.domain.repository.TournamentRepository
import de.kindermaenner.monatsblitz.domain.usecase.CreateNewGamesUseCase
import de.kindermaenner.monatsblitz.domain.usecase.CreateTournamentUseCase
import de.kindermaenner.monatsblitz.infrastructure.TournamentStorage
import de.kindermaenner.monatsblitz.ui.screens.HomeUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

class HomeViewModel(
    private val playerRepository: PlayerRepository,
    private val createTournamentUseCase: CreateTournamentUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            playerRepository.observePlayers().collect { players ->
                _uiState.update {
                    it.copy(
                        players = players,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun togglePlayer(playerId:Long) {
        _uiState.update { state ->
            val selected = state.selectedPlayerIds.toMutableSet()
            if (selected.contains(playerId)) {
                selected -= playerId
            } else {
                selected += playerId
            }
            state.copy(selectedPlayerIds = selected)
        }
    }

    fun onPlayerChecked(playerId: Long, checked: Boolean) {
        _uiState.update { state ->
            val selected = state.selectedPlayerIds.toMutableSet()

            if (checked)
                selected += playerId
            else
                selected -= playerId

            state.copy(selectedPlayerIds = selected)
        }
    }

    fun onModeChanged(mode: GameMode) {
        _uiState.update {
            it.copy(selectedMode = mode)
        }
    }

    fun onDoubleRoundChanged(enabled: Boolean) {
        _uiState.update {
            it.copy(doubleRound = enabled)
        }
    }

    fun createTournament() {
        viewModelScope.launch {
            val state = uiState.value

            val players = state.players.filter {
                it.id in state.selectedPlayerIds
            }
            val tournament = createTournamentUseCase.invoke(
                players = players,
                mode = state.selectedMode,
                date = LocalDate.now(),
                rounds = if (state.doubleRound) 2 else 1
            )
            Log.i("HomeViewModel", "Created: $tournament")
        }
    }
}
