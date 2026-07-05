package de.kindermaenner.monatsblitz.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.kindermaenner.monatsblitz.data.model.GameMode
import de.kindermaenner.monatsblitz.data.model.Player
import de.kindermaenner.monatsblitz.infrastructure.TournamentStorage
import de.kindermaenner.monatsblitz.ui.screens.HomeUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    private val getPlayers: suspend () -> List<Player>,
    private val createTournament: suspend (List<Int>, GameMode, Boolean) -> Int,
    private val tournamentStorage: TournamentStorage,
    private val onTournamentCreated: (Int) -> Unit = {}
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState
    
    private val _currentTournamentId = MutableStateFlow<Int?>(null)
    val currentTournamentId: StateFlow<Int?> = _currentTournamentId
    
    init {
        // Load saved tournament state on app startup
        viewModelScope.launch {
            //tournamentStorage.resetAll()
            //_currentTournamentId.value = null
            tournamentStorage.getTournamentState().collect { state ->
                if (state != null && !state.finalized) {
                    Log.i("HomeViewModel", "Resuming tournament with ID: ${state.tournamentId}")
                    _currentTournamentId.value = state.tournamentId
                    onTournamentCreated(state.tournamentId)
                }
            }
        }
    }

    suspend fun loadPlayers() {
        _uiState.value = _uiState.value.copy(
            players = getPlayers()
        )
    }

    fun togglePlayer(id: Int) {
        val current = _uiState.value.selectedPlayerIds.toMutableSet()

        if (current.contains(id)) current.remove(id)
        else current.add(id)

        _uiState.value = _uiState.value.copy(
            selectedPlayerIds = current
        )
    }

    fun setMode(mode: GameMode) {
        _uiState.value = _uiState.value.copy(selectedMode = mode)
    }

    fun setDoubleRound(value: Boolean) {
        _uiState.value = _uiState.value.copy(doubleRound = value)
    }

    fun startTournament() {
        val state = _uiState.value

        if (state.selectedPlayerIds.size < 2) return

        viewModelScope.launch {
            val tournamentId = createTournament(
                state.selectedPlayerIds.toList(),
                state.selectedMode,
                state.doubleRound
            )
            tournamentStorage.saveTournamentState(
                tournamentId,
                state.selectedPlayerIds.toList(),
                finalized = false)
            _currentTournamentId.value = tournamentId
            onTournamentCreated(tournamentId)
        }
    }
    
    fun finalizeTournament() {
        viewModelScope.launch {
            _currentTournamentId.value?.let { id ->
                tournamentStorage.finalizeTournament(id)
            }
            _currentTournamentId.value = null
        }
    }
}