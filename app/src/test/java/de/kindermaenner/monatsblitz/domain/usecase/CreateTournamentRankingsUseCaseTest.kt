package de.kindermaenner.monatsblitz.domain.usecase

import de.kindermaenner.monatsblitz.domain.model.Game
import de.kindermaenner.monatsblitz.domain.model.GameMode
import de.kindermaenner.monatsblitz.domain.model.GameResult
import de.kindermaenner.monatsblitz.domain.model.Player
import de.kindermaenner.monatsblitz.domain.model.PlayerRankingEntry
import de.kindermaenner.monatsblitz.domain.model.Tournament
import de.kindermaenner.monatsblitz.domain.repository.TournamentRepository
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class CreateTournamentRankingsUseCaseTest {

    private val tournamentRepository = mockk<TournamentRepository>(relaxed = true)
    private val useCase = CreateTournamentRankingsUseCase(tournamentRepository)

    // ============ invoke() Tests ============

    @Test
    fun `invoke should save player rankings to repository`() = runTest {
        val players = listOf(
            Player(1, "Mueller", "Anna"),
            Player(2, "Schmidt", "Bob")
        )
        val games = mapOf(
            1L to Game(1, 1, 2, 1, GameResult.Win)       // Player 1: 1.0, Player 2: 0.0
        )
        val tournament = Tournament(100, GameMode.BLITZ_3_2, LocalDate.now(), 1, players, games)

        useCase(tournament)

        val capturedEntries = slot<List<PlayerRankingEntry>>()
        coVerify { tournamentRepository.saveRankings(capture(capturedEntries)) }

        assertEquals(2, capturedEntries.captured.size)
        assertEquals(1, capturedEntries.captured[0].playerId)
        assertEquals(2, capturedEntries.captured[1].playerId)
    }

    @Test
    fun `invoke should order players by points descending`() = runTest {
        val players = listOf(
            Player(1, "A", "A"),
            Player(2, "B", "B"),
            Player(3, "C", "C")
        )
        val games = mapOf(
            1L to Game(1, 1, 2, 1, GameResult.Win),      // P1: 1.0, P2: 0.0
            2L to Game(2, 1, 3, 1, GameResult.Win),      // P1: +1.0 (2.0), P3: 0.0
            3L to Game(3, 2, 3, 1, GameResult.Win)       // P2: +1.0 (1.0), P3: 0.0
        )
        val tournament = Tournament(100, GameMode.BLITZ_3_2, LocalDate.now(), 1, players, games)

        useCase(tournament)

        val capturedEntries = slot<List<PlayerRankingEntry>>()
        coVerify { tournamentRepository.saveRankings(capture(capturedEntries)) }

        assertEquals(3, capturedEntries.captured.size)
        assertEquals(2.0, capturedEntries.captured[0].points, 0.0)
        assertEquals(1.0, capturedEntries.captured[1].points, 0.0)
        assertEquals(0.0, capturedEntries.captured[2].points, 0.0)
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
            1L to Game(1, 1, 3, 1, GameResult.Win),      // Player 1: 1.0, Player 3: 0.0
            2L to Game(2, 2, 4, 1, GameResult.Win)       // Player 2: 1.0, Player 4: 0.0
        )
        val tournament = Tournament(100, GameMode.BLITZ_3_2, LocalDate.now(), 1, players, games)

        useCase(tournament)

        val capturedEntries = slot<List<PlayerRankingEntry>>()
        coVerify { tournamentRepository.saveRankings(capture(capturedEntries)) }

        // Tied for 1st -> Rank 1
        assertEquals(1, capturedEntries.captured[0].rank)
        assertEquals(1, capturedEntries.captured[1].rank)
        // Tied for 2nd -> Rank 2 (Dense: no skipping)
        assertEquals(2, capturedEntries.captured[2].rank)
        assertEquals(2, capturedEntries.captured[3].rank)
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
            1L to Game(1, 1, 2, 1, GameResult.Win),      // Alice: 1.0, Bob: 0.0
            2L to Game(2, 1, 3, 1, GameResult.Win),      // Alice: +1.0 (2.0), Charlie: 0.0
            3L to Game(3, 1, 4, 1, GameResult.Remis),    // Alice: +0.5 (2.5), Diana: 0.5
            4L to Game(4, 2, 3, 1, GameResult.Loss),     // Bob: +0.0, Charlie: +1.0 (1.0)
            5L to Game(5, 2, 4, 1, GameResult.Win),      // Bob: +1.0 (1.0), Diana: +0.0
            6L to Game(6, 3, 4, 1, GameResult.Win)       // Charlie: +1.0 (2.0), Diana: +0.0 (0.5)
        )
        val tournament = Tournament(100, GameMode.BLITZ_3_2, LocalDate.now(), 1, players, games)

        useCase(tournament)

        val capturedEntries = slot<List<PlayerRankingEntry>>()
        coVerify { tournamentRepository.saveRankings(capture(capturedEntries)) }

        val results = capturedEntries.captured
        assertEquals(1, results[0].rank) // Alice (2.5)
        assertEquals(2, results[1].rank) // Charlie (2.0)
        assertEquals(3, results[2].rank) // Bob (1.0)
        assertEquals(4, results[3].rank) // Diana (0.5)
    }
}

