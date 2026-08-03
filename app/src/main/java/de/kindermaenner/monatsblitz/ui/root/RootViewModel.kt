package de.kindermaenner.monatsblitz.ui.root

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.kindermaenner.monatsblitz.domain.usecase.SyncPlayersUseCase
import de.kindermaenner.monatsblitz.infrastructure.TournamentStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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
            tournamentPreferences.getTournamentState().collect { state ->
                Log.i(TAG, "setting uiState: id=${state?.tournamentId}")

                _uiState.value =
                    if (state?.tournamentId == null) {
                        RootUiState.ReadyWithoutTournament
                    } else {
                        RootUiState.ReadyWithTournament(state.tournamentId)
                    }
            }
        }
    }

    companion object {
        const val TAG = "RootViewModel"
    }
}
