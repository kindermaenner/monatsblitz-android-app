package de.kindermaenner.monatsblitz.ui.ranking

import de.kindermaenner.monatsblitz.domain.model.Game
import de.kindermaenner.monatsblitz.domain.model.GameMode
import de.kindermaenner.monatsblitz.domain.model.GameResult
import de.kindermaenner.monatsblitz.domain.model.Player
import de.kindermaenner.monatsblitz.domain.model.SyncStatus
import de.kindermaenner.monatsblitz.domain.model.Tournament
import de.kindermaenner.monatsblitz.domain.model.TournamentRankingItem
import de.kindermaenner.monatsblitz.domain.repository.TournamentRepository
import de.kindermaenner.monatsblitz.domain.sync.TournamentSyncService
import de.kindermaenner.monatsblitz.domain.usecase.FinalizeTournamentUseCase
import de.kindermaenner.monatsblitz.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class RankingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val tournamentRepository = mockk<TournamentRepository>()

    @Before
    fun setup() {
        mockkStatic(android.util.Log::class)
        every { android.util.Log.i(any(), any()) } returns 0
        every { android.util.Log.e(any(), any()) } returns 0
        every { android.util.Log.e(any(), any(), any()) } returns 0
        every { tournamentRepository.observeTournament(any()) } returns flowOf(null)
    }

    @Test
    fun `init should load and map ranking data correctly`() = runTest {
        val tournamentId = 100L
        val rankingItems = listOf(
            TournamentRankingItem(playerId = 1, name = "Alice Meier", points = 2.5, rank = 1),
            TournamentRankingItem(playerId = 2, name = "Bob Mueller", points = 1.0, rank = 2)
        )

        every { tournamentRepository.observeTournamentRanking(tournamentId) } returns flowOf(rankingItems)

        val viewModel = RankingViewModel(tournamentId, tournamentRepository)

        val state = viewModel.uiState.value
        assertTrue(state is RankingUiState.Ready)
        val rows = (state as RankingUiState.Ready).rows
        assertEquals(2, rows.size)

        assertEquals("Alice Meier", rows[0].name)
        assertEquals(2.5, rows[0].points, 0.0)
        assertEquals(1, rows[0].rank)

        assertEquals("Bob Mueller", rows[1].name)
        assertEquals(1.0, rows[1].points, 0.0)
        assertEquals(2, rows[1].rank)
    }

    @Test
    fun `finalizeTournament should update state to isFinalized when successful`() = runTest {
        val tournamentId = 100L
        val finalizeTournamentUseCase = mockk<FinalizeTournamentUseCase>()
        val rankingItems = listOf(TournamentRankingItem(playerId = 1, name = "Alice Meier", points = 2.5, rank = 1))

        every { tournamentRepository.observeTournamentRanking(tournamentId) } returns flowOf(rankingItems)
        coEvery { finalizeTournamentUseCase(tournamentId) } returns Unit

        val viewModel = RankingViewModel(tournamentId, tournamentRepository, finalizeTournamentUseCase)

        viewModel.finalizeTournament()

        val state = viewModel.uiState.value
        assertTrue(state is RankingUiState.Ready)
        val ready = state as RankingUiState.Ready
        assertTrue("isFinalized should be true", ready.isFinalized)
        assertEquals(false, ready.isFinalizing)
        assertEquals(null, ready.finalizeError)
    }

    @Test
    fun `finalizeTournament should update state with error message when failed`() = runTest {
        val tournamentId = 100L
        val finalizeTournamentUseCase = mockk<FinalizeTournamentUseCase>()
        val rankingItems = listOf(TournamentRankingItem(playerId = 1, name = "Alice Meier", points = 2.5, rank = 1))

        every { tournamentRepository.observeTournamentRanking(tournamentId) } returns flowOf(rankingItems)
        coEvery { finalizeTournamentUseCase(tournamentId) } throws RuntimeException("Verbindungsfehler")

        val viewModel = RankingViewModel(tournamentId, tournamentRepository, finalizeTournamentUseCase)

        viewModel.finalizeTournament()

        val state = viewModel.uiState.value
        assertTrue(state is RankingUiState.Ready)
        val ready = state as RankingUiState.Ready
        assertEquals(false, ready.isFinalized)
        assertEquals(false, ready.isFinalizing)
        assertEquals("Verbindungsfehler", ready.finalizeError)

        viewModel.clearFinalizeError()
        val cleared = viewModel.uiState.value as RankingUiState.Ready
        assertEquals(null, cleared.finalizeError)
    }

    @Test
    fun `finalizeTournament should set error when finalizeTournamentUseCase is null`() = runTest {
        val tournamentId = 100L
        val rankingItems = listOf(TournamentRankingItem(playerId = 1, name = "Alice Meier", points = 2.5, rank = 1))

        every { tournamentRepository.observeTournamentRanking(tournamentId) } returns flowOf(rankingItems)

        val viewModel = RankingViewModel(tournamentId, tournamentRepository, finalizeTournamentUseCase = null)

        viewModel.finalizeTournament()

        val state = viewModel.uiState.value as RankingUiState.Ready
        assertEquals("Finalisierung nicht konfiguriert", state.finalizeError)
    }

    @Test
    fun `observeTournament calculates openGamesCount correctly`() = runTest {
        val tournamentId = 100L
        val rankingItems = listOf(TournamentRankingItem(playerId = 1, name = "Alice Meier", points = 1.0, rank = 1))
        val p1 = Player(1, "A", "A")
        val p2 = Player(2, "B", "B")
        val tournament = Tournament(
            id = tournamentId,
            mode = GameMode.BLITZ_5_0,
            date = LocalDate.now(),
            rounds = 1,
            players = listOf(p1, p2),
            games = mapOf(
                1L to Game(1L, 1L, 2L, 1, GameResult.Open)
            )
        )

        every { tournamentRepository.observeTournamentRanking(tournamentId) } returns flowOf(rankingItems)
        every { tournamentRepository.observeTournament(tournamentId) } returns flowOf(tournament)

        val viewModel = RankingViewModel(tournamentId, tournamentRepository)

        val state = viewModel.uiState.value as RankingUiState.Ready
        assertEquals(1, state.openGamesCount)
    }

    @Test
    fun `isReadOnly is true when syncStatus is FINALIZED`() = runTest {
        val tournamentId = 100L
        val rankingItems = listOf(TournamentRankingItem(playerId = 1, name = "Alice Meier", points = 1.0, rank = 1))
        val syncService = mockk<TournamentSyncService>(relaxed = true)

        every { tournamentRepository.observeTournamentRanking(tournamentId) } returns flowOf(rankingItems)
        every { syncService.syncStatus } returns MutableStateFlow(SyncStatus.FINALIZED)

        val viewModel = RankingViewModel(
            tournamentId = tournamentId,
            tournamentRepository = tournamentRepository,
            tournamentSyncService = syncService
        )

        val state = viewModel.uiState.value as RankingUiState.Ready
        assertTrue(state.isReadOnly)
    }

    @Test
    fun `ranking rows append (z) suffix to withdrawn players`() = runTest {
        val tournamentId = 100L
        val rankingItems = listOf(
            TournamentRankingItem(playerId = 1, name = "Alice Meier", points = 1.0, rank = 1),
            TournamentRankingItem(playerId = 2, name = "Bob Mueller", points = 0.5, rank = 2)
        )
        val p1 = Player(1, "Meier", "Alice")
        val p2 = Player(2, "Mueller", "Bob")
        val tournament = Tournament(
            id = tournamentId,
            mode = GameMode.BLITZ_5_0,
            date = LocalDate.now(),
            rounds = 1,
            players = listOf(p1, p2),
            withdrawnPlayerIds = setOf(2L)
        )

        every { tournamentRepository.observeTournamentRanking(tournamentId) } returns flowOf(rankingItems)
        every { tournamentRepository.observeTournament(tournamentId) } returns flowOf(tournament)

        val viewModel = RankingViewModel(tournamentId, tournamentRepository)

        val state = viewModel.uiState.value as RankingUiState.Ready
        assertEquals("Alice", state.rows[0].name)
        assertEquals("Bob (z)", state.rows[1].name)
    }

    @Test
    fun `finalizeTournament should do nothing when openGamesCount is greater than 0`() = runTest {
        val tournamentId = 100L
        val finalizeTournamentUseCase = mockk<FinalizeTournamentUseCase>()
        val rankingItems = listOf(TournamentRankingItem(playerId = 1, name = "Alice Meier", points = 2.5, rank = 1))
        val p1 = Player(1, "A", "A")
        val p2 = Player(2, "B", "B")
        val tournament = Tournament(
            id = tournamentId,
            mode = GameMode.BLITZ_5_0,
            date = LocalDate.now(),
            rounds = 1,
            players = listOf(p1, p2),
            games = mapOf(
                1L to Game(1L, 1L, 2L, 1, GameResult.Open)
            )
        )

        every { tournamentRepository.observeTournamentRanking(tournamentId) } returns flowOf(rankingItems)
        every { tournamentRepository.observeTournament(tournamentId) } returns flowOf(tournament)

        val viewModel = RankingViewModel(tournamentId, tournamentRepository, finalizeTournamentUseCase)

        viewModel.finalizeTournament()

        val state = viewModel.uiState.value as RankingUiState.Ready
        assertEquals(false, state.isFinalized)
        assertEquals(false, state.isFinalizing)
        io.mockk.coVerify(exactly = 0) { finalizeTournamentUseCase(any()) }
    }
}

