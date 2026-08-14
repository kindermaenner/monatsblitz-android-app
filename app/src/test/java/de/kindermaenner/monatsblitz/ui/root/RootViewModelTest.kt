package de.kindermaenner.monatsblitz.ui.root

import de.kindermaenner.monatsblitz.domain.usecase.SyncPlayersUseCase
import de.kindermaenner.monatsblitz.infrastructure.TournamentState
import de.kindermaenner.monatsblitz.infrastructure.TournamentStorage
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

    private val tournamentStorage = mockk<TournamentStorage>()
    private val syncPlayersUseCase = mockk<SyncPlayersUseCase>(relaxed = true)

    @Test
    fun `init should determine initial route as TournamentSetup when no tournament is active`() = runTest {
        coEvery { tournamentStorage.getTournamentState() } returns flowOf(null)

        val viewModel = RootViewModel(tournamentStorage, syncPlayersUseCase)

        assertEquals(AppRoute.TournamentSetup, viewModel.initialRoute.value)
        coVerify { syncPlayersUseCase() }
    }

    @Test
    fun `init should determine initial route as Crosstable when tournament is active`() = runTest {
        val activeTournamentId = 123L
        coEvery { tournamentStorage.getTournamentState() } returns flowOf(TournamentState(activeTournamentId, false))

        val viewModel = RootViewModel(tournamentStorage, syncPlayersUseCase)

        val route = viewModel.initialRoute.value as AppRoute.Crosstable
        assertEquals(activeTournamentId, route.id)
        coVerify { syncPlayersUseCase() }
    }

    @Test
    fun `init should handle sync failure gracefully`() = runTest {
        coEvery { syncPlayersUseCase() } throws Exception("Sync failed")
        coEvery { tournamentStorage.getTournamentState() } returns flowOf(null)

        val viewModel = RootViewModel(tournamentStorage, syncPlayersUseCase)

        assertEquals(AppRoute.TournamentSetup, viewModel.initialRoute.value)
    }
}
