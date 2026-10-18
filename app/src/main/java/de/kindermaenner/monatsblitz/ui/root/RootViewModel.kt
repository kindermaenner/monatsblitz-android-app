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
            safeSyncPlayers()

            val state = tournamentStorage.getTournamentState().first()
            if (state == null || state.finalized) {
                _initialRoute.value = AppRoute.TournamentSetup
                return@launch
            }

            val isRemoteFinalized = isRemoteTournamentFinalized(state.tournamentId)
            _initialRoute.value = if (isRemoteFinalized) {
                AppRoute.TournamentSetup
            } else {
                AppRoute.Crosstable(state.tournamentId)
            }
        }
    }

    private suspend fun safeSyncPlayers() {
        try {
            syncPlayersUseCase()
        } catch (_: Exception) {
            // Offline-First: Netzwerkfehler ignorieren
        }
    }

    private suspend fun isRemoteTournamentFinalized(tournamentId: Long): Boolean {
        if (tournamentDao == null || monatsblitzApi == null) return false
        return try {
            val tournament = tournamentDao.getTournament(tournamentId)
            val remoteId = tournament?.tournament?.remoteId ?: return false
            val remoteTournament = monatsblitzApi.getTournament(remoteId.toInt())
            val finalized = remoteTournament.status.equals("finalized", ignoreCase = true)
            if (finalized) {
                tournamentStorage.finalizeTournament()
            }
            finalized
        } catch (_: Exception) {
            // Offline-First: Netzwerkfehler ignorieren, lokales offenes Turnier fortsetzen
            false
        }
    }
}
