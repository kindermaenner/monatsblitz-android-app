package de.kindermaenner.monatsblitz.ui.root

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.kindermaenner.monatsblitz.domain.usecase.SyncPlayersUseCase
import de.kindermaenner.monatsblitz.infrastructure.TournamentStorage
import de.kindermaenner.monatsblitz.ui.navigation.AppRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class RootViewModel(
    private val tournamentStorage: TournamentStorage,
    private val syncPlayersUseCase: SyncPlayersUseCase
) : ViewModel() {

    private val _initialRoute = MutableStateFlow<AppRoute?>(null)
    val initialRoute = _initialRoute.asStateFlow()

    init {
        determineInitialRoute()
    }

    private fun determineInitialRoute() {
        viewModelScope.launch {
            // 1. Spieler syncen
            try {
                syncPlayersUseCase()
            } catch (e: Exception) {
                // ignore
            }

            // 2. Aktuelles Turnier prüfen
            val state = tournamentStorage.getTournamentState().first()
            
            _initialRoute.value = if (state?.tournamentId != null) {
                AppRoute.Crosstable(state.tournamentId)
            } else {
                AppRoute.TournamentSetup
            }
        }
    }
}
