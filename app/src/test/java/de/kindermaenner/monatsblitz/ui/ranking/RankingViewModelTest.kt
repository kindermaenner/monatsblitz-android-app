package de.kindermaenner.monatsblitz.ui.ranking

import de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.PlayerDao
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.TournamentPlayerDao
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.PlayerEntity
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.TournamentPlayerCrossRef
import de.kindermaenner.monatsblitz.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class RankingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val tournamentPlayerDao = mockk<TournamentPlayerDao>()
    private val playerDao = mockk<PlayerDao>()

    @Before
    fun setup() {
        mockkStatic(android.util.Log::class)
        every { android.util.Log.i(any(), any()) } returns 0
        every { android.util.Log.e(any(), any()) } returns 0
        every { android.util.Log.e(any(), any(), any()) } returns 0
    }

    @Test
    fun `init should load and map ranking data correctly`() = runTest {
        val tournamentId = 100L
        val players = listOf(
            PlayerEntity(id = 1, name = "Meier", vorname = "Alice", dirty = false),
            PlayerEntity(id = 2, name = "Mueller", vorname = "Bob", dirty = false)
        )
        val rankingRefs = listOf(
            TournamentPlayerCrossRef(tournamentId, 1, 2.5, 1),
            TournamentPlayerCrossRef(tournamentId, 2, 1.0, 2)
        )

        coEvery { tournamentPlayerDao.observeRankingForTournament(tournamentId) } returns flowOf(rankingRefs)
        coEvery { playerDao.getAllPlayers() } returns players

        val viewModel = RankingViewModel(tournamentId, tournamentPlayerDao, playerDao)

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
    fun `init should handle missing player names gracefully`() = runTest {
        val tournamentId = 100L
        val rankingRefs = listOf(
            TournamentPlayerCrossRef(tournamentId, 999, 1.0, 1)
        )

        coEvery { tournamentPlayerDao.observeRankingForTournament(tournamentId) } returns flowOf(rankingRefs)
        coEvery { playerDao.getAllPlayers() } returns emptyList()

        val viewModel = RankingViewModel(tournamentId, tournamentPlayerDao, playerDao)

        val state = viewModel.uiState.value
        assertTrue(state is RankingUiState.Ready)
        val rows = (state as RankingUiState.Ready).rows
        assertEquals("Unbekannt", rows[0].name)
    }

    @Test
    fun `finalizeTournament should update state to isFinalized when successful`() = runTest {
        val tournamentId = 100L
        val finalizeTournamentUseCase = mockk<de.kindermaenner.monatsblitz.domain.usecase.FinalizeTournamentUseCase>()
        val rankingRefs = listOf(TournamentPlayerCrossRef(tournamentId, 1, 2.5, 1))

        coEvery { tournamentPlayerDao.observeRankingForTournament(tournamentId) } returns flowOf(rankingRefs)
        coEvery { playerDao.getAllPlayers() } returns listOf(PlayerEntity(id = 1, name = "Meier", vorname = "Alice", dirty = false))
        coEvery { finalizeTournamentUseCase(tournamentId) } returns de.kindermaenner.monatsblitz.infrastructure.api.dto.FinalizeResponseDto(success = true)

        val viewModel = RankingViewModel(tournamentId, tournamentPlayerDao, playerDao, finalizeTournamentUseCase)

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
        val finalizeTournamentUseCase = mockk<de.kindermaenner.monatsblitz.domain.usecase.FinalizeTournamentUseCase>()
        val rankingRefs = listOf(TournamentPlayerCrossRef(tournamentId, 1, 2.5, 1))

        coEvery { tournamentPlayerDao.observeRankingForTournament(tournamentId) } returns flowOf(rankingRefs)
        coEvery { playerDao.getAllPlayers() } returns listOf(PlayerEntity(id = 1, name = "Meier", vorname = "Alice", dirty = false))
        coEvery { finalizeTournamentUseCase(tournamentId) } throws RuntimeException("Verbindungsfehler")

        val viewModel = RankingViewModel(tournamentId, tournamentPlayerDao, playerDao, finalizeTournamentUseCase)

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
}
