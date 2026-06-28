package de.sgkoenigslutter.monatsblitz.ui.viewmodels

import androidx.lifecycle.ViewModel
import de.sgkoenigslutter.monatsblitz.data.model.GameMode
import de.sgkoenigslutter.monatsblitz.data.model.Player
import de.sgkoenigslutter.monatsblitz.data.model.Tournament
import de.sgkoenigslutter.monatsblitz.ui.screens.HomeUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class HomeViewModel(
    private val getPlayers: () -> List<Player>,
    private val createTournament: (List<Int>, GameMode, Boolean) -> Unit
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    init {
        loadPlayers()
    }

    private fun loadPlayers() {
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

    fun startTournament(onStarted: () -> Unit) {
        val state = _uiState.value

        if (state.selectedPlayerIds.size < 2) return

        createTournament(
            state.selectedPlayerIds.toList(),
            state.selectedMode,
            state.doubleRound
        )

        onStarted()
    }
}