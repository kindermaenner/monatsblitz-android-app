package de.kindermaenner.monatsblitz.ui.root

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.kindermaenner.monatsblitz.domain.usecase.SyncPlayersUseCase
import de.kindermaenner.monatsblitz.infrastructure.TournamentStorage
import de.kindermaenner.monatsblitz.infrastructure.api.MonatsblitzApi
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.TournamentDao
import de.kindermaenner.monatsblitz.ui.navigation.AppRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class RootViewModel(
    private val tournamentStorage: TournamentStorage,
    private val syncPlayersUseCase: SyncPlayersUseCase,
    private val tournamentDao: TournamentDao? = null,
    private val monatsblitzApi: MonatsblitzApi? = null
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

            if (state == null || state.finalized) {
                _initialRoute.value = AppRoute.TournamentSetup
                return@launch
            }

            // 3. Wenn lokal offen, gegen Server-Status prüfen (falls remoteId vorhanden)
            var isRemoteFinalized = false
            if (tournamentDao != null && monatsblitzApi != null) {
                try {
                    val tournament = tournamentDao.getTournament(state.tournamentId)
                    val remoteId = tournament?.tournament?.remoteId
                    if (remoteId != null) {
                        val remoteTournament = monatsblitzApi.getTournament(remoteId.toInt())
                        if (remoteTournament.status.equals("finalized", ignoreCase = true)) {
                            isRemoteFinalized = true
                            tournamentStorage.finalizeTournament()
                        }
                    }
                } catch (e: Exception) {
                    // Offline-First: Netzwerkfehler ignorieren, lokales offenes Turnier fortsetzen
                }
            }

            _initialRoute.value = if (isRemoteFinalized) {
                AppRoute.TournamentSetup
            } else {
                AppRoute.Crosstable(state.tournamentId)
            }
        }
    }
}
