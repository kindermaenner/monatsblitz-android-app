package de.kindermaenner.monatsblitz.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.kindermaenner.monatsblitz.domain.repository.PlayerRepository
import de.kindermaenner.monatsblitz.domain.repository.SyncPlayerRepository
import de.kindermaenner.monatsblitz.domain.usecase.SyncPlayersUseCase
import de.kindermaenner.monatsblitz.infrastructure.TournamentStorage
import de.kindermaenner.monatsblitz.ui.screens.RootUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class RootViewModel(
    private val tournamentPreferences: TournamentStorage,
    private val syncPlayersUseCase: SyncPlayersUseCase
) : ViewModel() {

    private val _uiState =
        MutableStateFlow<RootUiState>(RootUiState.Loading)

    val uiState = _uiState.asStateFlow()

    init {
        preloadPlayers()
        observeCurrentTournament()
    }

    private fun preloadPlayers() {
        viewModelScope.launch {
            Log.i("RootViewModel", "sync players")
            syncPlayersUseCase()
        }
    }

    private fun observeCurrentTournament() {
        viewModelScope.launch {
            val id = tournamentPreferences.getTournamentState().firstOrNull()?.tournamentId
            _uiState.value =
                if (id == null) {
                    RootUiState.ReadyWithoutTournament
                } else {
                    RootUiState.ReadyWithTournament(id)
                }
        }
    }
}
