package de.kindermaenner.monatsblitz.ui.crosstable

import android.util.Log
import de.kindermaenner.monatsblitz.domain.model.Game
import de.kindermaenner.monatsblitz.domain.model.GameMode
import de.kindermaenner.monatsblitz.domain.model.GameResult
import de.kindermaenner.monatsblitz.domain.model.Player
import de.kindermaenner.monatsblitz.domain.model.Tournament
import de.kindermaenner.monatsblitz.domain.repository.TournamentRepository
import de.kindermaenner.monatsblitz.domain.usecase.CalculatePlayerPointsUseCase
import de.kindermaenner.monatsblitz.domain.usecase.CreateTournamentRankingsUseCase
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
class CrosstableViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<TournamentRepository>()
    private val setGameResultUseCase = mockk<SetGameResultUseCase>(relaxed = true)
    private val createTournamentRankingsUseCase = mockk<CreateTournamentRankingsUseCase>(relaxed = true)
    private val calculatePlayerPointsUseCase = CalculatePlayerPointsUseCase()

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.i(any(), any()) } returns 0
    }

    @Test
    fun `initial state loads tournament from repository`() = runTest {
        val tournament = Tournament(1, GameMode.BLITZ_3_2, LocalDate.now(), 2, listOf(Player(1, "A", "A")))
        coEvery { repository.observeTournament(1) } returns flowOf(tournament)

        val viewModel = CrosstableViewModel(
            repository,
            setGameResultUseCase,
            createTournamentRankingsUseCase,
            calculatePlayerPointsUseCase,
            1
        )
        
        // Wait for the state to be populated
        val state = viewModel.uiState.first { it.tournament != null }

        assertEquals(tournament, state.tournament)
    }

    @Test
    fun `initial state calculates player points correctly`() = runTest {
        val p1 = Player(1, "A", "A")
        val p2 = Player(2, "B", "B")
        val games = mapOf(
            1L to Game(1, 1, 2, 1, GameResult.Win),
            2L to Game(2, 2, 1, 1, GameResult.Loss)
        )
        val tournament = Tournament(1, GameMode.BLITZ_3_2, LocalDate.now(), 1, listOf(p1, p2), games)
        coEvery { repository.observeTournament(1) } returns flowOf(tournament)

        val viewModel = CrosstableViewModel(
            repository,
            setGameResultUseCase,
            createTournamentRankingsUseCase,
            calculatePlayerPointsUseCase,
            1
        )

        val state = viewModel.uiState.first { it.tournament != null }
        assertEquals(1.0, state.playerPoints[1L] ?: 0.0, 0.0)
        assertEquals(0.0, state.playerPoints[2L] ?: 0.0, 0.0)
    }

    @Test
    fun `selectLeg updates leg state if within bounds`() = runTest {
        val tournament = Tournament(1, GameMode.BLITZ_3_2, LocalDate.now(), 2, listOf(Player(1, "A", "A")))
        coEvery { repository.observeTournament(1) } returns flowOf(tournament)
        val viewModel = CrosstableViewModel(
            repository,
            setGameResultUseCase,
            createTournamentRankingsUseCase,
            calculatePlayerPointsUseCase,
            1
        )
        
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
        
        val viewModel = CrosstableViewModel(
            repository,
            setGameResultUseCase,
            createTournamentRankingsUseCase,
            calculatePlayerPointsUseCase,
            1
        )
        backgroundScope.launch { viewModel.uiState.collect() }
        advanceUntilIdle()
        
        viewModel.setResult(0, 1, 1, GameResult.Win)

        coVerify { setGameResultUseCase(1, 10, 20, 1, GameResult.Win) }
    }
}
