package de.kindermaenner.monatsblitz.infrastructure.sync

import android.net.ConnectivityManager
import de.kindermaenner.monatsblitz.domain.sync.TournamentSyncService
import de.kindermaenner.monatsblitz.infrastructure.TournamentState
import de.kindermaenner.monatsblitz.infrastructure.TournamentStorage
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NetworkSyncObserverTest {

    private val connectivityManager = mockk<ConnectivityManager>(relaxed = true)
    private val tournamentStorage = mockk<TournamentStorage>(relaxed = true)
    private val tournamentSyncService = mockk<TournamentSyncService>(relaxed = true)

    @Test
    fun `start and stop register and unregister network callback`() {
        val observer = NetworkSyncObserver(
            connectivityManager = connectivityManager,
            tournamentStorage = tournamentStorage,
            tournamentSyncService = tournamentSyncService
        )

        observer.start()
        verify { connectivityManager.registerDefaultNetworkCallback(any()) }

        observer.stop()
        verify { connectivityManager.unregisterNetworkCallback(any<ConnectivityManager.NetworkCallback>()) }
    }

    @Test
    fun `onNetworkAvailable triggers checkInitialStatus when open tournament exists`() = runTest {
        coEvery { tournamentStorage.getTournamentState() } returns flowOf(TournamentState(tournamentId = 42L, finalized = false))
        val observer = NetworkSyncObserver(
            connectivityManager = connectivityManager,
            tournamentStorage = tournamentStorage,
            tournamentSyncService = tournamentSyncService
        )

        observer.onNetworkAvailable().join()

        coVerify { tournamentSyncService.checkInitialStatus(42L) }
    }

    @Test
    fun `onNetworkAvailable does nothing when tournament is finalized or null`() = runTest {
        coEvery { tournamentStorage.getTournamentState() } returns flowOf(TournamentState(tournamentId = 42L, finalized = true))
        val observer = NetworkSyncObserver(
            connectivityManager = connectivityManager,
            tournamentStorage = tournamentStorage,
            tournamentSyncService = tournamentSyncService
        )

        observer.onNetworkAvailable().join()

        coVerify(exactly = 0) { tournamentSyncService.checkInitialStatus(any()) }
    }
}
