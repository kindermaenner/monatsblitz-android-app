package de.kindermaenner.monatsblitz.ui.root

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.kindermaenner.monatsblitz.domain.repository.SettingsRepository
import de.kindermaenner.monatsblitz.domain.repository.TournamentRepository
import de.kindermaenner.monatsblitz.domain.usecase.SyncPlayersUseCase
import de.kindermaenner.monatsblitz.ui.navigation.AppRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class RootViewModel(
    private val tournamentRepository: TournamentRepository,
    private val syncPlayersUseCase: SyncPlayersUseCase,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _initialRoute = MutableStateFlow<AppRoute?>(null)
    val initialRoute = _initialRoute.asStateFlow()

    init {
        determineInitialRoute()
    }

    private fun determineInitialRoute() {
        viewModelScope.launch {
            if (!settingsRepository.isConfigured()) {
                _initialRoute.value = AppRoute.Settings
                return@launch
            }

            val isServer = runCatching { settingsRepository.isServerConfigured() }.getOrDefault(true)
            if (isServer) {
                safeSyncPlayers()
                val adoptedId = tryAdoptRemoteOpenTournament()
                if (adoptedId != null) {
                    _initialRoute.value = AppRoute.Crosstable(adoptedId)
                    return@launch
                }
            }

            val activeOpenTournamentId = tournamentRepository.getActiveOpenTournamentId()
            _initialRoute.value = if (activeOpenTournamentId == null) {
                AppRoute.TournamentSetup
            } else {
                AppRoute.Crosstable(activeOpenTournamentId)
            }
        }
    }

    private suspend fun tryAdoptRemoteOpenTournament(): Long? {
        val autoAdopt = try {
            settingsRepository.autoAdoptOpenTournament.first()
        } catch (_: Exception) {
            false
        }
        if (!autoAdopt) return null

        return try {
            val remoteOpen = tournamentRepository.getRemoteOpenTournament()
            if (remoteOpen != null) {
                tournamentRepository.adoptRemoteTournament(remoteOpen.id)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun safeSyncPlayers() {
        try {
            syncPlayersUseCase()
        } catch (_: Exception) {
            // Offline-First: Netzwerkfehler ignorieren
        }
    }
}
