package de.kindermaenner.monatsblitz.domain.usecase

import de.kindermaenner.monatsblitz.domain.model.GameMode
import de.kindermaenner.monatsblitz.domain.model.Tournament
import de.kindermaenner.monatsblitz.domain.repository.TournamentRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.time.LocalDate

class FinalizeTournamentUseCaseTest {

    private val tournamentRepository = mockk<TournamentRepository>(relaxed = true)
    private val syncTournamentUseCase = mockk<SyncTournamentUseCase>(relaxed = true)
    private val syncGameResultsUseCase = mockk<SyncGameResultsUseCase>(relaxed = true)
    private val syncPlayersUseCase = mockk<SyncPlayersUseCase>(relaxed = true)
    private val createTournamentRankingsUseCase = mockk<CreateTournamentRankingsUseCase>(relaxed = true)

    private val useCase = FinalizeTournamentUseCase(
        tournamentRepository = tournamentRepository,
        syncTournamentUseCase = syncTournamentUseCase,
        syncGameResultsUseCase = syncGameResultsUseCase,
        syncPlayersUseCase = syncPlayersUseCase,
        createTournamentRankingsUseCase = createTournamentRankingsUseCase
    )

    @Test
    fun `constructor with default createTournamentRankingsUseCase initializes properly`() {
        val defaultUseCase = FinalizeTournamentUseCase(
            tournamentRepository = tournamentRepository,
            syncTournamentUseCase = syncTournamentUseCase,
            syncGameResultsUseCase = syncGameResultsUseCase,
            syncPlayersUseCase = syncPlayersUseCase
        )
        assertNotNull(defaultUseCase)
    }

    @Test
    fun `invoke orchestrates sync, rankings calculation, and repository finalization`() = runTest {
        val tournamentId = 1L
        val tournament = Tournament(
            id = tournamentId,
            mode = GameMode.BLITZ_3_2,
            date = LocalDate.now(),
            rounds = 1,
            players = emptyList()
        )
        coEvery { tournamentRepository.getTournamentById(tournamentId) } returns tournament

        useCase(tournamentId)

        coVerifyOrder {
            tournamentRepository.getTournamentById(tournamentId)
            syncPlayersUseCase()
            syncTournamentUseCase()
            syncGameResultsUseCase()
            tournamentRepository.getTournamentById(tournamentId)
            createTournamentRankingsUseCase(tournament)
            tournamentRepository.submitResultsAndFinalize(tournamentId)
        }
    }

    @Test
    fun `invoke throws exception when tournament is not found initially`() = runTest {
        coEvery { tournamentRepository.getTournamentById(999L) } returns null

        try {
            useCase(999L)
            fail("Should throw IllegalStateException")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("nicht gefunden") == true)
        }
        coVerify(exactly = 0) { syncPlayersUseCase() }
    }

    @Test
    fun `invoke throws exception when tournament is not found on second retrieval`() = runTest {
        val tournamentId = 1L
        val tournament = Tournament(
            id = tournamentId,
            mode = GameMode.BLITZ_3_2,
            date = LocalDate.now(),
            rounds = 1,
            players = emptyList()
        )
        coEvery { tournamentRepository.getTournamentById(tournamentId) } returnsMany listOf(tournament, null)

        try {
            useCase(tournamentId)
            fail("Should throw IllegalStateException")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("nicht gefunden") == true)
        }
        coVerify(exactly = 0) { tournamentRepository.submitResultsAndFinalize(any()) }
    }
}

