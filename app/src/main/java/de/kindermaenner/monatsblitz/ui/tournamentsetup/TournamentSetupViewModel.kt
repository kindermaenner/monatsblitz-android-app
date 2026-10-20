package de.kindermaenner.monatsblitz.ui.tournamentsetup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.kindermaenner.monatsblitz.domain.model.GameMode
import de.kindermaenner.monatsblitz.domain.model.Player
import de.kindermaenner.monatsblitz.domain.model.PlayerSortField
import de.kindermaenner.monatsblitz.domain.model.PlayerSortOrder
import de.kindermaenner.monatsblitz.domain.repository.PlayerRepository
import de.kindermaenner.monatsblitz.domain.repository.SettingsRepository
import de.kindermaenner.monatsblitz.domain.usecase.AddPlayerUseCase
import de.kindermaenner.monatsblitz.domain.usecase.CreateTournamentUseCase
import de.kindermaenner.monatsblitz.domain.usecase.SyncPlayersUseCase
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
    private val addPlayerUseCase: AddPlayerUseCase,
    private val syncPlayersUseCase: SyncPlayersUseCase? = null,
    private val settingsRepository: SettingsRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(TournamentSetupUiState(isLoading = true))
    val uiState: StateFlow<TournamentSetupUiState> = _uiState.asStateFlow()

    private val _navigationEffect = MutableSharedFlow<Long>()
    val navigationEffect = _navigationEffect.asSharedFlow()

    init {
        viewModelScope.launch {
            playerRepository.observePlayers().collect { rawPlayers ->
                _uiState.update { state ->
                    state.copy(
                        players = sortPlayers(rawPlayers, state.sortField, state.sortOrder),
                        isLoading = false
                    )
                }
            }
        }
        if (settingsRepository != null) {
            viewModelScope.launch {
                settingsRepository.showDetailedSyncIndicators.collect { showIndicators ->
                    _uiState.update { it.copy(showDetailedSyncIndicators = showIndicators) }
                }
            }
            viewModelScope.launch {
                settingsRepository.playerSortField.collect { field ->
                    _uiState.update { state ->
                        state.copy(
                            sortField = field,
                            players = sortPlayers(state.players, field, state.sortOrder)
                        )
                    }
                }
            }
            viewModelScope.launch {
                settingsRepository.playerSortOrder.collect { order ->
                    _uiState.update { state ->
                        state.copy(
                            sortOrder = order,
                            players = sortPlayers(state.players, state.sortField, order)
                        )
                    }
                }
            }
        }
    }

    private fun sortPlayers(
        players: List<Player>,
        field: PlayerSortField,
        order: PlayerSortOrder
    ): List<Player> {
        val comparator = when (field) {
            PlayerSortField.LAST_NAME -> compareBy<Player>({ it.name.lowercase() }, { it.vorname.lowercase() })
            PlayerSortField.FIRST_NAME -> compareBy<Player>({ it.vorname.lowercase() }, { it.name.lowercase() })
        }
        return if (order == PlayerSortOrder.ASCENDING) {
            players.sortedWith(comparator)
        } else {
            players.sortedWith(comparator.reversed())
        }
    }

    fun toggleSortOrder() {
        val newOrder = if (_uiState.value.sortOrder == PlayerSortOrder.ASCENDING) {
            PlayerSortOrder.DESCENDING
        } else {
            PlayerSortOrder.ASCENDING
        }
        _uiState.update { state ->
            state.copy(
                sortOrder = newOrder,
                players = sortPlayers(state.players, state.sortField, newOrder)
            )
        }
        viewModelScope.launch {
            settingsRepository?.setPlayerSortOrder(newOrder)
        }
    }

    fun toggleSortField() {
        val newField = if (_uiState.value.sortField == PlayerSortField.LAST_NAME) {
            PlayerSortField.FIRST_NAME
        } else {
            PlayerSortField.LAST_NAME
        }
        _uiState.update { state ->
            state.copy(
                sortField = newField,
                players = sortPlayers(state.players, newField, state.sortOrder)
            )
        }
        viewModelScope.launch {
            settingsRepository?.setPlayerSortField(newField)
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

    fun onDateChanged(date: LocalDate) {
        _uiState.update {
            it.copy(selectedDate = date)
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
                date = state.selectedDate,
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
                runCatching { syncPlayersUseCase?.invoke() }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(addPlayerError = e.message ?: "Fehler beim Anlegen des Spielers.")
                }
            }
        }
    }

    fun syncPlayers() {
        val syncUseCase = syncPlayersUseCase ?: return
        if (_uiState.value.isSyncingPlayers) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSyncingPlayers = true) }
            try {
                runCatching { syncUseCase() }
            } finally {
                _uiState.update { it.copy(isSyncingPlayers = false) }
            }
        }
    }
}
