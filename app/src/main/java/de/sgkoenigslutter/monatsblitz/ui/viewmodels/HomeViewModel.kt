package de.sgkoenigslutter.monatsblitz.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.sgkoenigslutter.monatsblitz.data.model.GameMode
import de.sgkoenigslutter.monatsblitz.data.model.Player
import de.sgkoenigslutter.monatsblitz.ui.screens.HomeUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    private val getPlayers: suspend () -> List<Player>,
    private val createTournament: suspend (List<Int>, GameMode, Boolean) -> Int,
    private val onTournamentCreated: (Int) -> Unit = {}
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

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
            onTournamentCreated(tournamentId)
        }
    }
}