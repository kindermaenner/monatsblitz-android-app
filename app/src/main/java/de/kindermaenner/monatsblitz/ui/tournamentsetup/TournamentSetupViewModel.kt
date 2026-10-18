package de.kindermaenner.monatsblitz.ui.tournamentsetup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.kindermaenner.monatsblitz.domain.model.GameMode
import de.kindermaenner.monatsblitz.domain.repository.PlayerRepository
import de.kindermaenner.monatsblitz.domain.usecase.AddPlayerUseCase
import de.kindermaenner.monatsblitz.domain.usecase.CreateTournamentUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

class TournamentSetupViewModel(
    private val playerRepository: PlayerRepository,
    private val createTournamentUseCase: CreateTournamentUseCase,
    private val addPlayerUseCase: AddPlayerUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TournamentSetupUiState(isLoading = true))
    val uiState: StateFlow<TournamentSetupUiState> = _uiState.asStateFlow()

    private val _navigationEffect = MutableSharedFlow<Long>()
    val navigationEffect = _navigationEffect.asSharedFlow()

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

    fun togglePlayer(playerId: Long) {
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

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onShowOnlySelectedChanged(showOnly: Boolean) {
        _uiState.update { it.copy(showOnlySelected = showOnly) }
    }

    fun clearSelectedPlayers() {
        _uiState.update { it.copy(selectedPlayerIds = emptySet()) }
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
            _navigationEffect.emit(tournament.id)
        }
    }

    fun onShowAddPlayerDialog(show: Boolean) {
        _uiState.update { it.copy(showAddPlayerDialog = show, addPlayerError = null) }
    }

    fun onNewPlayerNameChanged(vorname: String, name: String) {
        _uiState.update { it.copy(newPlayerVorname = vorname, newPlayerName = name, addPlayerError = null) }
    }

    fun addNewPlayer() {
        val state = uiState.value
        val vorname = state.newPlayerVorname.trim()
        val name = state.newPlayerName.trim()
        if (vorname.isBlank() || name.isBlank()) return

        viewModelScope.launch {
            try {
                val newPlayer = addPlayerUseCase(vorname, name)
                // Automatisches Auswählen des neuen Spielers
                onPlayerChecked(newPlayer.id, true)
                onShowAddPlayerDialog(false)
                onNewPlayerNameChanged("", "")
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(addPlayerError = e.message ?: "Fehler beim Anlegen des Spielers.")
                }
            }
        }
    }
}
