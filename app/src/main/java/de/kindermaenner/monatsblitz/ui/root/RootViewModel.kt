package de.kindermaenner.monatsblitz.ui.root

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.kindermaenner.monatsblitz.domain.usecase.SyncPlayersUseCase
import de.kindermaenner.monatsblitz.infrastructure.TournamentStorage
import de.kindermaenner.monatsblitz.ui.navigation.AppRoute
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RootViewModel(
    private val tournamentPreferences: TournamentStorage,
    private val syncPlayersUseCase: SyncPlayersUseCase
) : ViewModel() {

    private val _uiState =
        MutableStateFlow<RootUiState>(RootUiState.Loading)
    val uiState = _uiState.asStateFlow()

    // --- Navigation-Events (Effect) ---
    private val _effect = MutableSharedFlow<RootEffect>()
    val effect = _effect.asSharedFlow()

    init {
        preloadPlayers()
        observeCurrentTournament()
    }

    private fun preloadPlayers() {
        viewModelScope.launch {
            syncPlayersUseCase()
        }
    }

    private fun observeCurrentTournament() {
        viewModelScope.launch {
            tournamentPreferences.getTournamentState().collect { state ->
                val tournamentId = state?.tournamentId

                _uiState.value = RootUiState.Ready(tournamentId)

                if (tournamentId != null) {
                    navigateTo(AppRoute.Tournament(tournamentId))
                }
            }
        }
    }
    fun navigateTo(route: AppRoute) {
        viewModelScope.launch {
            _effect.emit(RootEffect.Navigate(route))
        }
    }

    companion object {
        const val TAG = "RootViewModel"
    }
}
