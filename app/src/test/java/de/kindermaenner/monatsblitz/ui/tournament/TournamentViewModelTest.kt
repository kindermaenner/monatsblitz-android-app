package de.kindermaenner.monatsblitz.ui.tournament

import android.util.Log
import de.kindermaenner.monatsblitz.domain.model.GameMode
import de.kindermaenner.monatsblitz.domain.model.GameResult
import de.kindermaenner.monatsblitz.domain.model.Player
import de.kindermaenner.monatsblitz.domain.model.Tournament
import de.kindermaenner.monatsblitz.domain.repository.TournamentRepository
import de.kindermaenner.monatsblitz.domain.usecase.SetGameResultUseCase
import de.kindermaenner.monatsblitz.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class TournamentViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<TournamentRepository>()
    private val setGameResultUseCase = mockk<SetGameResultUseCase>(relaxed = true)

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.i(any(), any()) } returns 0
    }

    @Test
    fun `initial state loads tournament from repository`() = runTest {
        val tournament = Tournament(1, GameMode.BLITZ_3_2, LocalDate.now(), 2, listOf(Player(1, "A", "A")))
        coEvery { repository.observeTournament(1) } returns flowOf(tournament)

        val viewModel = TournamentViewModel(repository, setGameResultUseCase, 1)
        
        // Wait for the state to be populated
        val state = viewModel.uiState.first { it.tournament != null }

        assertEquals(tournament, state.tournament)
    }

    @Test
    fun `selectLeg updates leg state if within bounds`() = runTest {
        val tournament = Tournament(1, GameMode.BLITZ_3_2, LocalDate.now(), 2, listOf(Player(1, "A", "A")))
        coEvery { repository.observeTournament(1) } returns flowOf(tournament)
        val viewModel = TournamentViewModel(repository, setGameResultUseCase, 1)
        
        // Ensure VM is active and has loaded the tournament
        backgroundScope.launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        viewModel.selectLeg(2)
        assertEquals(2, viewModel.selectedLeg.value)

        viewModel.selectLeg(3) // Out of bounds
        assertEquals(2, viewModel.selectedLeg.value)
    }

    @Test
    fun `setResult calls usecase`() = runTest {
        val p1 = Player(10, "A", "A")
        val p2 = Player(20, "B", "B")
        val tournament = Tournament(1, GameMode.BLITZ_3_2, LocalDate.now(), 1, listOf(p1, p2))
        coEvery { repository.observeTournament(1) } returns flowOf(tournament)
        
        val viewModel = TournamentViewModel(repository, setGameResultUseCase, 1)
        backgroundScope.launch { viewModel.uiState.collect() }
        advanceUntilIdle()
        
        viewModel.setResult(0, 1, 1, GameResult.Win)

        coVerify { setGameResultUseCase(1, 10, 20, 1, GameResult.Win) }
    }
}
