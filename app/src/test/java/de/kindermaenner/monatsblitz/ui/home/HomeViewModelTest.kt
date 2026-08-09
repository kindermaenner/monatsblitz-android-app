package de.kindermaenner.monatsblitz.ui.home

import de.kindermaenner.monatsblitz.domain.model.GameMode
import de.kindermaenner.monatsblitz.domain.model.Player
import de.kindermaenner.monatsblitz.domain.repository.PlayerRepository
import de.kindermaenner.monatsblitz.domain.usecase.CreateTournamentUseCase
import de.kindermaenner.monatsblitz.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val playerRepository = mockk<PlayerRepository>()
    private val createTournamentUseCase = mockk<CreateTournamentUseCase>()

    @Test
    fun `initial state loads players from repository`() = runTest {
        val players = listOf(Player(1, "N", "V"))
        coEvery { playerRepository.observePlayers() } returns flowOf(players)

        val viewModel = HomeViewModel(playerRepository, createTournamentUseCase)

        assertEquals(players, viewModel.uiState.value.players)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `togglePlayer updates selected ids`() = runTest {
        coEvery { playerRepository.observePlayers() } returns flowOf(emptyList())
        val viewModel = HomeViewModel(playerRepository, createTournamentUseCase)

        viewModel.togglePlayer(1L)
        assertTrue(viewModel.uiState.value.selectedPlayerIds.contains(1L))

        viewModel.togglePlayer(1L)
        assertFalse(viewModel.uiState.value.selectedPlayerIds.contains(1L))
    }

    @Test
    fun `createTournament calls usecase with selected players`() = runTest {
        val p1 = Player(1, "A", "A")
        val p2 = Player(2, "B", "B")
        coEvery { playerRepository.observePlayers() } returns flowOf(listOf(p1, p2))
        coEvery { createTournamentUseCase(any(), any(), any(), any()) } returns mockk()

        val viewModel = HomeViewModel(playerRepository, createTournamentUseCase)
        viewModel.togglePlayer(1L)
        viewModel.onModeChanged(GameMode.BLITZ_5_0)
        
        viewModel.createTournament()

        coVerify { createTournamentUseCase(
            players = listOf(p1),
            mode = GameMode.BLITZ_5_0,
            date = any(),
            rounds = 1
        ) }
    }
}
