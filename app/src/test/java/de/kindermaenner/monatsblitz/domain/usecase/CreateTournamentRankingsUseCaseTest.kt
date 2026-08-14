package de.kindermaenner.monatsblitz.domain.usecase

import de.kindermaenner.monatsblitz.domain.model.Game
import de.kindermaenner.monatsblitz.domain.model.GameMode
import de.kindermaenner.monatsblitz.domain.model.GameResult
import de.kindermaenner.monatsblitz.domain.model.Player
import de.kindermaenner.monatsblitz.domain.model.PlayerRankingEntry
import de.kindermaenner.monatsblitz.domain.model.Tournament
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.TournamentPlayerDao
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.TournamentPlayerCrossRef
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class CreateTournamentRankingsUseCaseTest {

    private val tournamentPlayerDao = mockk<TournamentPlayerDao>(relaxed = true)
    private val useCase = CreateTournamentRankingsUseCase(tournamentPlayerDao)

    // ============ invoke() Tests ============

    @Test
    fun `invoke should save player rankings to dao`() = runTest {
        val players = listOf(
            Player(1, "Mueller", "Anna"),
            Player(2, "Schmidt", "Bob")
        )
        val games = mapOf(
            1L to Game(1, 1, 2, 1, GameResult.Win),      // Player 1: 1.0
            2L to Game(2, 2, 1, 1, GameResult.Loss)      // Player 2: 0.0
        )
        val tournament = Tournament(100, GameMode.BLITZ_3_2, LocalDate.now(), 1, players, games)

        useCase(tournament)

        val capturedRefs = slot<List<TournamentPlayerCrossRef>>()
        coVerify { tournamentPlayerDao.upsertAll(capture(capturedRefs)) }

        assertEquals(2, capturedRefs.captured.size)
        assertEquals(1, capturedRefs.captured[0].playerId)
        assertEquals(2, capturedRefs.captured[1].playerId)
    }

    @Test
    fun `invoke should order players by points descending`() = runTest {
        val players = listOf(
            Player(1, "A", "A"),
            Player(2, "B", "B"),
            Player(3, "C", "C")
        )
        val games = mapOf(
            1L to Game(1, 1, 2, 1, GameResult.Win),      // Player 1: 1.0
            2L to Game(2, 2, 3, 1, GameResult.Remis),    // Player 2: 0.5
            3L to Game(3, 3, 1, 1, GameResult.Loss)      // Player 3: 0.0
        )
        val tournament = Tournament(100, GameMode.BLITZ_3_2, LocalDate.now(), 1, players, games)

        useCase(tournament)

        val capturedRefs = slot<List<TournamentPlayerCrossRef>>()
        coVerify { tournamentPlayerDao.upsertAll(capture(capturedRefs)) }

        assertEquals(3, capturedRefs.captured.size)
        assertEquals(1.0, capturedRefs.captured[0].points ?: 0.0, 0.0)
        assertEquals(0.5, capturedRefs.captured[1].points ?: 0.0, 0.0)
        assertEquals(0.0, capturedRefs.captured[2].points ?: 0.0, 0.0)
    }

    @Test
    fun `invoke should calculate dense ranking correctly with tied points`() = runTest {
        val players = listOf(
            Player(1, "A", "A"),
            Player(2, "B", "B"),
            Player(3, "C", "C"),
            Player(4, "D", "D")
        )
        val games = mapOf(
            1L to Game(1, 1, 2, 1, GameResult.Win),      // Player 1: 1.0
            2L to Game(2, 2, 3, 1, GameResult.Win),      // Player 2: 1.0 (tied)
            3L to Game(3, 3, 4, 1, GameResult.Loss),     // Player 3: 0.0
            4L to Game(4, 4, 1, 1, GameResult.Loss)      // Player 4: 0.0 (tied)
        )
        val tournament = Tournament(100, GameMode.BLITZ_3_2, LocalDate.now(), 1, players, games)

        useCase(tournament)

        val capturedRefs = slot<List<TournamentPlayerCrossRef>>()
        coVerify { tournamentPlayerDao.upsertAll(capture(capturedRefs)) }

        // Tied for 1st -> Rank 1
        assertEquals(1, capturedRefs.captured[0].rank)
        assertEquals(1, capturedRefs.captured[1].rank)
        // Tied for 2nd -> Rank 2 (Dense: no skipping)
        assertEquals(2, capturedRefs.captured[2].rank)
        assertEquals(2, capturedRefs.captured[3].rank)
    }

    // ============ createDenseRanking() Tests ============

    @Test
    fun `createDenseRanking should order descending and not skip ranks`() {
        val rankings = listOf(
            PlayerRankingEntry(1, 100, 10.0),
            PlayerRankingEntry(2, 100, 10.0),
            PlayerRankingEntry(3, 100, 8.0),
            PlayerRankingEntry(4, 100, 5.0)
        )

        val result = useCase.createDenseRanking(rankings)

        assertEquals(1, result[0].rank)
        assertEquals(1, result[1].rank)
        assertEquals(2, result[2].rank) // No gap!
        assertEquals(3, result[3].rank)
    }

    // ============ createCompetitionHandling() Tests ============

    @Test
    fun `createCompetitionHandling should skip ranks for tied players`() {
        val rankings = listOf(
            PlayerRankingEntry(1, 100, 10.0),
            PlayerRankingEntry(2, 100, 10.0),
            PlayerRankingEntry(3, 100, 8.0),
            PlayerRankingEntry(4, 100, 5.0)
        )

        val result = useCase.createCompetitionHandling(rankings)

        assertEquals(1, result[0].rank)
        assertEquals(1, result[1].rank)
        assertEquals(3, result[2].rank) // Gap! (2 skipped)
        assertEquals(4, result[3].rank)
    }

    @Test
    fun `full workflow with real tournament scenario (Dense Ranking)`() = runTest {
        val players = listOf(
            Player(1, "Meier", "Alice"),
            Player(2, "Mueller", "Bob"),
            Player(3, "Schmidt", "Charlie"),
            Player(4, "Weber", "Diana")
        )
        val games = mapOf(
            1L to Game(1, 1, 2, 1, GameResult.Win),      // Alice: 1.0
            2L to Game(2, 1, 3, 1, GameResult.Win),      // Alice: 1.0
            3L to Game(3, 1, 4, 1, GameResult.Remis),    // Alice: 0.5 (total: 2.5)
            4L to Game(4, 2, 3, 1, GameResult.Remis),    // Bob: 0.5
            5L to Game(5, 2, 4, 1, GameResult.Loss),     // Bob: 0.0 (total: 0.5)
            6L to Game(6, 3, 4, 1, GameResult.Win),      // Charlie: 1.0 (total: 1.0)
            7L to Game(7, 4, 1, 1, GameResult.Loss)      // Diana: 0.0 (total: 0.0)
        )
        val tournament = Tournament(100, GameMode.BLITZ_3_2, LocalDate.now(), 1, players, games)

        useCase(tournament)

        val capturedRefs = slot<List<TournamentPlayerCrossRef>>()
        coVerify { tournamentPlayerDao.upsertAll(capture(capturedRefs)) }

        val results = capturedRefs.captured
        assertEquals(1, results[0].rank) // Alice (2.5)
        assertEquals(2, results[1].rank) // Charlie (1.0)
        assertEquals(3, results[2].rank) // Bob (0.5)
        assertEquals(4, results[3].rank) // Diana (0.0)
    }
}
