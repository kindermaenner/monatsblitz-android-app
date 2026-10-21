package de.kindermaenner.monatsblitz.ui.root

import de.kindermaenner.monatsblitz.domain.repository.RemoteOpenTournamentInfo
import de.kindermaenner.monatsblitz.domain.repository.SettingsRepository
import de.kindermaenner.monatsblitz.domain.repository.TournamentRepository
import de.kindermaenner.monatsblitz.domain.usecase.SyncPlayersUseCase
import de.kindermaenner.monatsblitz.ui.navigation.AppRoute
import de.kindermaenner.monatsblitz.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class RootViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val tournamentRepository = mockk<TournamentRepository>()
    private val syncPlayersUseCase = mockk<SyncPlayersUseCase>(relaxed = true)
    private val settingsRepository = mockk<SettingsRepository>()

    @Test
    fun `init should determine initial route as Settings when app is not configured`() = runTest {
        coEvery { settingsRepository.isConfigured() } returns false

        val viewModel = RootViewModel(tournamentRepository, syncPlayersUseCase, settingsRepository)

        assertEquals(AppRoute.Settings, viewModel.initialRoute.value)
        coVerify(exactly = 0) { syncPlayersUseCase() }
        coVerify(exactly = 0) { tournamentRepository.getActiveOpenTournamentId() }
    }

    @Test
    fun `init should determine initial route as TournamentSetup when configured and no open tournament is active`() = runTest {
        coEvery { settingsRepository.isConfigured() } returns true
        coEvery { settingsRepository.isServerConfigured() } returns true
        coEvery { settingsRepository.autoAdoptOpenTournament } returns flowOf(false)
        coEvery { tournamentRepository.getActiveOpenTournamentId() } returns null

        val viewModel = RootViewModel(tournamentRepository, syncPlayersUseCase, settingsRepository)

        assertEquals(AppRoute.TournamentSetup, viewModel.initialRoute.value)
        coVerify { syncPlayersUseCase() }
    }

    @Test
    fun `init should skip sync and remote check when server is not configured in offline mode`() = runTest {
        coEvery { settingsRepository.isConfigured() } returns true
        coEvery { settingsRepository.isServerConfigured() } returns false
        coEvery { tournamentRepository.getActiveOpenTournamentId() } returns null

        val viewModel = RootViewModel(tournamentRepository, syncPlayersUseCase, settingsRepository)

        assertEquals(AppRoute.TournamentSetup, viewModel.initialRoute.value)
        coVerify(exactly = 0) { syncPlayersUseCase() }
        coVerify(exactly = 0) { tournamentRepository.getRemoteOpenTournament() }
    }

    @Test
    fun `init should determine initial route as Crosstable when configured and an open tournament is active`() = runTest {
        val activeTournamentId = 123L
        coEvery { settingsRepository.isConfigured() } returns true
        coEvery { settingsRepository.isServerConfigured() } returns true
        coEvery { settingsRepository.autoAdoptOpenTournament } returns flowOf(false)
        coEvery { tournamentRepository.getActiveOpenTournamentId() } returns activeTournamentId

        val viewModel = RootViewModel(tournamentRepository, syncPlayersUseCase, settingsRepository)

        val route = viewModel.initialRoute.value as AppRoute.Crosstable
        assertEquals(activeTournamentId, route.id)
        coVerify { syncPlayersUseCase() }
    }

    @Test
    fun `init should handle sync failure gracefully when configured`() = runTest {
        coEvery { settingsRepository.isConfigured() } returns true
        coEvery { settingsRepository.isServerConfigured() } returns true
        coEvery { settingsRepository.autoAdoptOpenTournament } returns flowOf(false)
        coEvery { syncPlayersUseCase() } throws Exception("Sync failed")
        coEvery { tournamentRepository.getActiveOpenTournamentId() } returns null

        val viewModel = RootViewModel(tournamentRepository, syncPlayersUseCase, settingsRepository)

        assertEquals(AppRoute.TournamentSetup, viewModel.initialRoute.value)
    }

    @Test
    fun `init should auto adopt remote tournament and navigate to Crosstable when autoAdopt is enabled`() = runTest {
        val remoteInfo = RemoteOpenTournamentInfo(id = 55, date = "2026-10-21", mode = "3+2", roundCount = 1, playerCount = 8)
        coEvery { settingsRepository.isConfigured() } returns true
        coEvery { settingsRepository.isServerConfigured() } returns true
        coEvery { settingsRepository.autoAdoptOpenTournament } returns flowOf(true)
        coEvery { tournamentRepository.getRemoteOpenTournament() } returns remoteInfo
        coEvery { tournamentRepository.adoptRemoteTournament(55) } returns 999L

        val viewModel = RootViewModel(tournamentRepository, syncPlayersUseCase, settingsRepository)

        val route = viewModel.initialRoute.value as AppRoute.Crosstable
        assertEquals(999L, route.id)
        coVerify { tournamentRepository.adoptRemoteTournament(55) }
    }
}
