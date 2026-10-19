package de.kindermaenner.monatsblitz.ui.root

import de.kindermaenner.monatsblitz.domain.repository.TournamentRepository
import de.kindermaenner.monatsblitz.domain.usecase.SyncPlayersUseCase
import de.kindermaenner.monatsblitz.ui.navigation.AppRoute
import de.kindermaenner.monatsblitz.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class RootViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val tournamentRepository = mockk<TournamentRepository>()
    private val syncPlayersUseCase = mockk<SyncPlayersUseCase>(relaxed = true)

    @Test
    fun `init should determine initial route as TournamentSetup when no open tournament is active`() = runTest {
        coEvery { tournamentRepository.getActiveOpenTournamentId() } returns null

        val viewModel = RootViewModel(tournamentRepository, syncPlayersUseCase)

        assertEquals(AppRoute.TournamentSetup, viewModel.initialRoute.value)
        coVerify { syncPlayersUseCase() }
    }

    @Test
    fun `init should determine initial route as Crosstable when an open tournament is active`() = runTest {
        val activeTournamentId = 123L
        coEvery { tournamentRepository.getActiveOpenTournamentId() } returns activeTournamentId

        val viewModel = RootViewModel(tournamentRepository, syncPlayersUseCase)

        val route = viewModel.initialRoute.value as AppRoute.Crosstable
        assertEquals(activeTournamentId, route.id)
        coVerify { syncPlayersUseCase() }
    }

    @Test
    fun `init should handle sync failure gracefully`() = runTest {
        coEvery { syncPlayersUseCase() } throws Exception("Sync failed")
        coEvery { tournamentRepository.getActiveOpenTournamentId() } returns null

        val viewModel = RootViewModel(tournamentRepository, syncPlayersUseCase)

        assertEquals(AppRoute.TournamentSetup, viewModel.initialRoute.value)
    }
}

