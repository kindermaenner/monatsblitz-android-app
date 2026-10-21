package de.kindermaenner.monatsblitz.ui.crosstable

import android.util.Log
import de.kindermaenner.monatsblitz.domain.model.Game
import de.kindermaenner.monatsblitz.domain.model.GameMode
import de.kindermaenner.monatsblitz.domain.model.GameResult
import de.kindermaenner.monatsblitz.domain.model.Player
import de.kindermaenner.monatsblitz.domain.model.PlayerWithdrawRule
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
            1L
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
            1L
        )

        val state = viewModel.uiState.first { it.tournament != null }
        assertEquals(1.0, state.playerPoints[1L] ?: 0.0, 0.0)
        assertEquals(0.0, state.playerPoints[2L] ?: 0.0, 0.0)
        assertEquals(2, state.rows.size)
        assertEquals(listOf("x", "1"), state.rows[0].results)
        assertEquals(listOf("0", "x"), state.rows[1].results)
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
            1L
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
    fun `setResult calls usecase and triggers auto sync`() = runTest {
        val p1 = Player(10, "A", "A")
        val p2 = Player(20, "B", "B")
        val tournament = Tournament(1, GameMode.BLITZ_3_2, LocalDate.now(), 1, listOf(p1, p2))
        coEvery { repository.observeTournament(1) } returns flowOf(tournament)
        val syncService = mockk<de.kindermaenner.monatsblitz.domain.sync.TournamentSyncService>(relaxed = true)
        every { syncService.syncStatus } returns kotlinx.coroutines.flow.MutableStateFlow(de.kindermaenner.monatsblitz.domain.model.SyncStatus.SYNCED)
        
        val viewModel = CrosstableViewModel(
            repository,
            setGameResultUseCase,
            createTournamentRankingsUseCase,
            calculatePlayerPointsUseCase,
            1L,
            syncService
        )
        backgroundScope.launch { viewModel.uiState.collect() }
        advanceUntilIdle()
        
        viewModel.setResult(0, 1, 1, GameResult.Win)
        advanceUntilIdle()

        coVerify { setGameResultUseCase(1, 10, 20, 1, GameResult.Win) }
        coVerify { syncService.triggerSync(1L) }
    }

    @Test
    fun `triggerManualSync triggers syncService`() = runTest {
        val tournament = Tournament(1, GameMode.BLITZ_3_2, LocalDate.now(), 1, listOf(Player(1, "A", "A")))
        coEvery { repository.observeTournament(1) } returns flowOf(tournament)
        val syncService = mockk<de.kindermaenner.monatsblitz.domain.sync.TournamentSyncService>(relaxed = true)
        every { syncService.syncStatus } returns kotlinx.coroutines.flow.MutableStateFlow(de.kindermaenner.monatsblitz.domain.model.SyncStatus.FAILED)

        val viewModel = CrosstableViewModel(
            repository,
            setGameResultUseCase,
            createTournamentRankingsUseCase,
            calculatePlayerPointsUseCase,
            1L,
            syncService
        )
        backgroundScope.launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        viewModel.triggerManualSync()
        advanceUntilIdle()

        coVerify { syncService.triggerSync(1L) }
    }

    @Test
    fun `triggerManualSync does nothing when status is SYNCING or FINALIZED`() = runTest {
        val tournament = Tournament(1, GameMode.BLITZ_3_2, LocalDate.now(), 1, listOf(Player(1, "A", "A")))
        coEvery { repository.observeTournament(1) } returns flowOf(tournament)
        val syncService = mockk<de.kindermaenner.monatsblitz.domain.sync.TournamentSyncService>(relaxed = true)
        val statusFlow = kotlinx.coroutines.flow.MutableStateFlow(de.kindermaenner.monatsblitz.domain.model.SyncStatus.SYNCING)
        every { syncService.syncStatus } returns statusFlow

        val viewModel = CrosstableViewModel(
            repository,
            setGameResultUseCase,
            createTournamentRankingsUseCase,
            calculatePlayerPointsUseCase,
            1L,
            syncService
        )
        backgroundScope.launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        viewModel.triggerManualSync()
        advanceUntilIdle()
        coVerify(exactly = 0) { syncService.triggerSync(any()) }

        statusFlow.value = de.kindermaenner.monatsblitz.domain.model.SyncStatus.FINALIZED
        advanceUntilIdle()

        viewModel.triggerManualSync()
        advanceUntilIdle()
        coVerify(exactly = 0) { syncService.triggerSync(any()) }
    }

    @Test
    fun `setResult does not call usecase when syncStatus is FINALIZED`() = runTest {
        val p1 = Player(10, "A", "A")
        val p2 = Player(20, "B", "B")
        val tournament = Tournament(1, GameMode.BLITZ_3_2, LocalDate.now(), 1, listOf(p1, p2))
        coEvery { repository.observeTournament(1) } returns flowOf(tournament)
        val syncService = mockk<de.kindermaenner.monatsblitz.domain.sync.TournamentSyncService>(relaxed = true)
        every { syncService.syncStatus } returns kotlinx.coroutines.flow.MutableStateFlow(de.kindermaenner.monatsblitz.domain.model.SyncStatus.FINALIZED)

        val viewModel = CrosstableViewModel(
            repository,
            setGameResultUseCase,
            createTournamentRankingsUseCase,
            calculatePlayerPointsUseCase,
            1L,
            syncService
        )
        backgroundScope.launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        viewModel.setResult(0, 1, 1, GameResult.Win)
        advanceUntilIdle()

        coVerify(exactly = 0) { setGameResultUseCase(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `prepareRankingsAndNavigate invokes usecase and callback`() = runTest {
        val tournament = Tournament(1, GameMode.BLITZ_3_2, LocalDate.now(), 1, listOf(Player(1, "A", "A")))
        coEvery { repository.observeTournament(1) } returns flowOf(tournament)

        val viewModel = CrosstableViewModel(
            repository,
            setGameResultUseCase,
            createTournamentRankingsUseCase,
            calculatePlayerPointsUseCase,
            1L
        )
        backgroundScope.launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        var navigated = false
        viewModel.prepareRankingsAndNavigate { navigated = true }
        advanceUntilIdle()

        coVerify { createTournamentRankingsUseCase(tournament) }
        org.junit.Assert.assertTrue("Callback should be invoked", navigated)
    }

    @Test
    fun `resetTournament calls repository reset`() = runTest {
        val tournament = Tournament(1, GameMode.BLITZ_3_2, LocalDate.now(), 1, listOf(Player(1, "A", "A")))
        coEvery { repository.observeTournament(1) } returns flowOf(tournament)
        coEvery { repository.resetTournament() } returns Unit

        val viewModel = CrosstableViewModel(
            repository,
            setGameResultUseCase,
            createTournamentRankingsUseCase,
            calculatePlayerPointsUseCase,
            1L
        )

        viewModel.resetTournament()
        advanceUntilIdle()

        coVerify { repository.resetTournament() }
    }

    @Test
    fun `triggerManualSync emits syncMessageEvent on failure with error reason`() = runTest {
        val tournament = Tournament(1, GameMode.BLITZ_3_2, LocalDate.now(), 1, listOf(Player(1, "A", "A")))
        coEvery { repository.observeTournament(1) } returns flowOf(tournament)
        val syncService = mockk<de.kindermaenner.monatsblitz.domain.sync.TournamentSyncService>()
        val statusFlow = kotlinx.coroutines.flow.MutableStateFlow(de.kindermaenner.monatsblitz.domain.model.SyncStatus.FAILED)
        val errorFlow = kotlinx.coroutines.flow.MutableStateFlow<String?>("Server nicht erreichbar")
        coEvery { syncService.checkInitialStatus(any()) } returns Unit
        coEvery { syncService.triggerSync(any()) } returns Unit
        every { syncService.syncStatus } returns statusFlow
        every { syncService.lastSyncError } returns errorFlow

        val viewModel = CrosstableViewModel(
            repository,
            setGameResultUseCase,
            createTournamentRankingsUseCase,
            calculatePlayerPointsUseCase,
            1L,
            syncService
        )
        val messages = mutableListOf<String>()
        backgroundScope.launch(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) {
            viewModel.syncMessageEvent.collect { messages.add(it) }
        }
        viewModel.uiState.first { it.tournament != null }

        viewModel.triggerManualSync()
        advanceUntilIdle()

        org.junit.Assert.assertEquals(listOf("Server nicht erreichbar"), messages)
    }

    @Test
    fun `uiState sorts players alphabetically in crosstable`() = runTest {
        val p1 = Player(1, "Schmidt", "Bob")
        val p2 = Player(2, "Meier", "Alice")
        val p3 = Player(3, "Adams", "Charlie")
        val tournament = Tournament(1, GameMode.BLITZ_3_2, LocalDate.now(), 1, listOf(p1, p2, p3))
        coEvery { repository.observeTournament(1) } returns flowOf(tournament)

        val viewModel = CrosstableViewModel(
            repository,
            setGameResultUseCase,
            createTournamentRankingsUseCase,
            calculatePlayerPointsUseCase,
            1L
        )

        val state = viewModel.uiState.first { it.tournament != null }
        assertEquals(listOf("Alice", "Bob", "Charlie"), state.rows.map { it.playerName })
        assertEquals(listOf(2L, 1L, 3L), state.tournament?.playerIds)
    }

    @Test
    fun `uiState sorts players alphabetically with umlauts and case insensitivity`() = runTest {
        val p1 = Player(1, "Zimmermann", "Zoe")
        val p2 = Player(2, "Arzt", "Änne")
        val p3 = Player(3, "Bauer", "Berta")
        val tournament = Tournament(1, GameMode.BLITZ_3_2, LocalDate.now(), 1, listOf(p1, p2, p3))
        coEvery { repository.observeTournament(1) } returns flowOf(tournament)

        val viewModel = CrosstableViewModel(
            repository,
            setGameResultUseCase,
            createTournamentRankingsUseCase,
            calculatePlayerPointsUseCase,
            1L
        )

        val state = viewModel.uiState.first { it.tournament != null }
        assertEquals(listOf("Änne", "Berta", "Zoe"), state.rows.map { it.playerName })
        assertEquals(listOf(2L, 3L, 1L), state.tournament?.playerIds)
    }

    @Test
    fun `uiState sets isWithdrawn on row data correctly`() = runTest {
        val p1 = Player(1, "Schmidt", "Bob")
        val p2 = Player(2, "Meier", "Alice")
        val tournament = Tournament(
            id = 1,
            mode = GameMode.BLITZ_3_2,
            date = LocalDate.now(),
            rounds = 1,
            players = listOf(p1, p2),
            withdrawnPlayerIds = setOf(1L)
        )
        coEvery { repository.observeTournament(1) } returns flowOf(tournament)

        val viewModel = CrosstableViewModel(
            repository,
            setGameResultUseCase,
            createTournamentRankingsUseCase,
            calculatePlayerPointsUseCase,
            1L
        )

        val state = viewModel.uiState.first { it.tournament != null }
        val aliceRow = state.rows.first { it.playerId == 2L }
        val bobRow = state.rows.first { it.playerId == 1L }

        org.junit.Assert.assertFalse(aliceRow.isWithdrawn)
        org.junit.Assert.assertTrue(bobRow.isWithdrawn)
    }

    @Test
    fun `withdrawPlayer delegates to repository`() = runTest {
        val p1 = Player(1, "Schmidt", "Bob")
        val tournament = Tournament(1, GameMode.BLITZ_3_2, LocalDate.now(), 1, listOf(p1))
        coEvery { repository.observeTournament(1) } returns flowOf(tournament)
        coEvery { repository.withdrawPlayer(1L, 1L, PlayerWithdrawRule.YOUTH_DRAW) } returns Unit

        val viewModel = CrosstableViewModel(
            repository,
            setGameResultUseCase,
            createTournamentRankingsUseCase,
            calculatePlayerPointsUseCase,
            1L
        )

        viewModel.uiState.first { it.tournament != null }
        viewModel.withdrawPlayer(1L, PlayerWithdrawRule.YOUTH_DRAW)
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.withdrawPlayer(1L, 1L, PlayerWithdrawRule.YOUTH_DRAW) }
    }
}
