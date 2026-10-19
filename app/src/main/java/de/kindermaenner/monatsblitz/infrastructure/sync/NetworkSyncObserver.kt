package de.kindermaenner.monatsblitz.infrastructure.sync

import android.net.ConnectivityManager
import android.net.Network
import de.kindermaenner.monatsblitz.domain.sync.TournamentSyncService
import de.kindermaenner.monatsblitz.infrastructure.TournamentStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class NetworkSyncObserver(
    private val connectivityManager: ConnectivityManager?,
    private val tournamentStorage: TournamentStorage,
    private val tournamentSyncService: TournamentSyncService,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {
    private val networkCallback by lazy {
        object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                onNetworkAvailable()
            }
        }
    }

    fun start() {
        try {
            connectivityManager?.registerDefaultNetworkCallback(networkCallback)
        } catch (_: Exception) {
            // Ignoriert, falls ConnectivityManager in Testumgebungen nicht verfügbar ist
        }
    }

    fun stop() {
        try {
            connectivityManager?.unregisterNetworkCallback(networkCallback)
        } catch (_: Exception) {
            // Bereits abgemeldet oder nicht registriert
        }
    }

    internal fun onNetworkAvailable(): Job {
        return scope.launch {
            val state = tournamentStorage.getTournamentState().first()
            if (state != null && !state.finalized) {
                tournamentSyncService.checkInitialStatus(state.tournamentId)
            }
        }
    }
}
