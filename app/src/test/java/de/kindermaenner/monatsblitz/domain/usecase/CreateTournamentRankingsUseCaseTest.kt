package de.kindermaenner.monatsblitz.domain.usecase

import de.kindermaenner.monatsblitz.domain.model.Game
import de.kindermaenner.monatsblitz.domain.model.GameMode
import de.kindermaenner.monatsblitz.domain.model.GameResult
import de.kindermaenner.monatsblitz.domain.model.Player
import de.kindermaenner.monatsblitz.domain.model.PlayerRankingEntry
import de.kindermaenner.monatsblitz.domain.model.Tournament
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.TournamentPlayerDao
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.TournamentPlayerCrossRef
import io.mockk.coEvery
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
        assertEquals(1, capturedRefs.captured[0].playerId)  // Anna
        assertEquals(2, capturedRefs.captured[1].playerId)  // Bob
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
        // First player should have most points
        assertEquals(1.0, capturedRefs.captured[0].points ?: 0.0, 0.0)
        // Second player should have fewer points
        assertEquals(0.5, capturedRefs.captured[1].points ?: 0.0, 0.0)
        // Third player should have least
        assertEquals(0.0, capturedRefs.captured[2].points ?: 0.0, 0.0)
    }

    @Test
    fun `invoke with empty player list should call dao with empty list`() = runTest {
        val tournament = Tournament(100, GameMode.BLITZ_3_2, LocalDate.now(), 1, emptyList(), emptyMap())

        useCase(tournament)

        val capturedRefs = slot<List<TournamentPlayerCrossRef>>()
        coVerify { tournamentPlayerDao.upsertAll(capture(capturedRefs)) }

        assertEquals(0, capturedRefs.captured.size)
    }

    @Test
    fun `invoke with single player should create ranking with rank 1`() = runTest {
        val players = listOf(Player(1, "Solo", "Player"))
        val games = mapOf(
            1L to Game(1, 1, 1, 1, GameResult.Win)
        )
        val tournament = Tournament(100, GameMode.BLITZ_3_2, LocalDate.now(), 1, players, games)

        useCase(tournament)

        val capturedRefs = slot<List<TournamentPlayerCrossRef>>()
        coVerify { tournamentPlayerDao.upsertAll(capture(capturedRefs)) }

        assertEquals(1, capturedRefs.captured.size)
        assertEquals(1, capturedRefs.captured[0].rank)
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

        // Both with 1.0 points should have rank 1 (dense ranking)
        assertEquals(1, capturedRefs.captured[0].rank)
        assertEquals(1, capturedRefs.captured[1].rank)
        // Both with 0.0 points should have rank 3 (skipping 2)
        assertEquals(3, capturedRefs.captured[2].rank)
        assertEquals(3, capturedRefs.captured[3].rank)
    }

    // ============ getPointsForPlayer() Tests ============
    // Note: getPointsForPlayer is private, so we test it indirectly via invoke()

    @Test
    fun `points calculation should sum all player1 game results`() = runTest {
        val players = listOf(
            Player(1, "A", "A"),
            Player(2, "B", "B"),
            Player(3, "C", "C")
        )
        val games = mapOf(
            1L to Game(1, 1, 2, 1, GameResult.Win),      // Player 1 vs 2: 1.0
            2L to Game(2, 1, 3, 1, GameResult.Remis),    // Player 1 vs 3: 0.5
            3L to Game(3, 1, 2, 2, GameResult.Win),      // Player 1 vs 2 (leg 2): 1.0
            4L to Game(4, 2, 1, 1, GameResult.Loss)      // Player 2 vs 1 (this doesn't count for player 1)
        )
        val tournament = Tournament(100, GameMode.BLITZ_3_2, LocalDate.now(), 2, players, games)

        useCase(tournament)

        val capturedRefs = slot<List<TournamentPlayerCrossRef>>()
        coVerify { tournamentPlayerDao.upsertAll(capture(capturedRefs)) }

        // Find player 1 in results
        val player1Result = capturedRefs.captured.find { it.playerId == 1L }!!
        assertEquals(2.5, player1Result.points ?: 0.0, 0.0)  // 1.0 + 0.5 + 1.0
    }

    @Test
    fun `points calculation should only count player1Id games`() = runTest {
        val players = listOf(
            Player(1, "A", "A"),
            Player(2, "B", "B")
        )
        val games = mapOf(
            1L to Game(1, 1, 2, 1, GameResult.Win),      // Player 1 as player1: 1.0
            2L to Game(2, 2, 1, 1, GameResult.Win)       // Player 1 as player2: NOT COUNTED
        )
        val tournament = Tournament(100, GameMode.BLITZ_3_2, LocalDate.now(), 1, players, games)

        useCase(tournament)

        val capturedRefs = slot<List<TournamentPlayerCrossRef>>()
        coVerify { tournamentPlayerDao.upsertAll(capture(capturedRefs)) }

        val player1Result = capturedRefs.captured.find { it.playerId == 1L }!!
        assertEquals(1.0, player1Result.points ?: 0.0, 0.0)
    }

    @Test
    fun `player without games should have 0 points`() = runTest {
        val players = listOf(
            Player(1, "A", "A"),
            Player(2, "B", "B")
        )
        val games = mapOf(
            1L to Game(1, 1, 1, 1, GameResult.Win)
        )
        val tournament = Tournament(100, GameMode.BLITZ_3_2, LocalDate.now(), 1, players, games)

        useCase(tournament)

        val capturedRefs = slot<List<TournamentPlayerCrossRef>>()
        coVerify { tournamentPlayerDao.upsertAll(capture(capturedRefs)) }

        val player2Result = capturedRefs.captured.find { it.playerId == 2L }!!
        assertEquals(0.0, player2Result.points ?: 0.0, 0.0)
    }

    @Test
    fun `points calculation should handle all game result types`() = runTest {
        val players = listOf(
            Player(1, "A", "A"),
            Player(2, "B", "B"),
            Player(3, "C", "C"),
            Player(4, "D", "D"),
            Player(5, "E", "E")
        )
        val games = mapOf(
            1L to Game(1, 1, 2, 1, GameResult.Win),          // 1.0
            2L to Game(2, 1, 3, 1, GameResult.Loss),         // 0.0
            3L to Game(3, 1, 4, 1, GameResult.Remis),        // 0.5
            4L to Game(4, 1, 5, 1, GameResult.ForfeitWin)    // 1.0
        )
        val tournament = Tournament(100, GameMode.BLITZ_3_2, LocalDate.now(), 1, players, games)

        useCase(tournament)

        val capturedRefs = slot<List<TournamentPlayerCrossRef>>()
        coVerify { tournamentPlayerDao.upsertAll(capture(capturedRefs)) }

        val player1Result = capturedRefs.captured.find { it.playerId == 1L }!!
        assertEquals(2.5, player1Result.points ?: 0.0, 0.0)  // 1.0 + 0.0 + 0.5 + 1.0
    }

    // ============ createDenseRanking() Tests ============

    @Test
    fun `createDenseRanking should order players by points descending`() {
        val rankings = listOf(
            PlayerRankingEntry(1, 100, 8.5),
            PlayerRankingEntry(2, 100, 5.0),
            PlayerRankingEntry(3, 100, 10.0)
        )

        val result = useCase.competitionRanking(rankings)

        assertEquals(3, result.size)
        assertEquals(10.0, result[0].points, 0.0)
        assertEquals(8.5, result[1].points, 0.0)
        assertEquals(5.0, result[2].points, 0.0)
    }

    @Test
    fun `createDenseRanking should assign same rank for equal points`() {
        val rankings = listOf(
            PlayerRankingEntry(1, 100, 10.0),
            PlayerRankingEntry(2, 100, 10.0),
            PlayerRankingEntry(3, 100, 8.0)
        )

        val result = useCase.competitionRanking(rankings)

        // Both with 10.0 should have rank 1
        assertEquals(1, result[0].rank)
        assertEquals(1, result[1].rank)
        // Third should be rank 3 (dense ranking)
        assertEquals(3, result[2].rank)
    }

    @Test
    fun `createDenseRanking should skip ranks for tied players`() {
        val rankings = listOf(
            PlayerRankingEntry(1, 100, 10.0),
            PlayerRankingEntry(2, 100, 10.0),
            PlayerRankingEntry(3, 100, 10.0),
            PlayerRankingEntry(4, 100, 7.0)
        )

        val result = useCase.competitionRanking(rankings)

        // Three tied for first should all be rank 1
        assertEquals(1, result[0].rank)
        assertEquals(1, result[1].rank)
        assertEquals(1, result[2].rank)
        // Next rank should be 4 (skipping 2 and 3)
        assertEquals(4, result[3].rank)
    }

    @Test
    fun `createDenseRanking should preserve all player data`() {
        val rankings = listOf(
            PlayerRankingEntry(1, 100, 10.0),
            PlayerRankingEntry(2, 100, 8.0),
            PlayerRankingEntry(3, 100, 5.0)
        )

        val result = useCase.competitionRanking(rankings)

        assertEquals(3, result.size)
        assertEquals(1, result[0].playerId)
        assertEquals(2, result[1].playerId)
        assertEquals(3, result[2].playerId)
        assertEquals(100, result[0].tournamentId)
        assertEquals(100, result[1].tournamentId)
        assertEquals(100, result[2].tournamentId)
    }

    // ============ competitionRanking() Tests ============

    @Test
    fun `competitionRanking should create gaps for tied players`() {
        val rankings = listOf(
            PlayerRankingEntry(1, 100, 10.0),
            PlayerRankingEntry(2, 100, 10.0),
            PlayerRankingEntry(3, 100, 8.0),
            PlayerRankingEntry(4, 100, 8.0),
            PlayerRankingEntry(5, 100, 5.0)
        )

        val result = useCase.competitionRanking(rankings)

        // Both 10.0 ranked 1
        assertEquals(1, result[0].rank)
        assertEquals(1, result[1].rank)
        // Both 8.0 ranked 3 (not 2, skipping positions)
        assertEquals(3, result[2].rank)
        assertEquals(3, result[3].rank)
        // 5.0 ranked 5
        assertEquals(5, result[4].rank)
    }

    @Test
    fun `competitionRanking should order descending`() {
        val rankings = listOf(
            PlayerRankingEntry(1, 100, 5.0),
            PlayerRankingEntry(2, 100, 10.0),
            PlayerRankingEntry(3, 100, 7.5)
        )

        val result = useCase.competitionRanking(rankings)

        assertEquals(10.0, result[0].points, 0.0)
        assertEquals(7.5, result[1].points, 0.0)
        assertEquals(5.0, result[2].points, 0.0)
    }

    @Test
    fun `competitionRanking with single player should give rank 1`() {
        val rankings = listOf(
            PlayerRankingEntry(1, 100, 5.0)
        )

        val result = useCase.competitionRanking(rankings)

        assertEquals(1, result[0].rank)
    }

    @Test
    fun `competitionRanking should handle empty list`() {
        val rankings = emptyList<PlayerRankingEntry>()

        val result = useCase.competitionRanking(rankings)

        assertEquals(0, result.size)
    }

    @Test
    fun `competitionRanking should correctly count itemsProcessed`() {
        val rankings = listOf(
            PlayerRankingEntry(1, 100, 10.0),
            PlayerRankingEntry(2, 100, 8.0),
            PlayerRankingEntry(3, 100, 8.0),
            PlayerRankingEntry(4, 100, 5.0)
        )

        val result = useCase.competitionRanking(rankings)

        // itemsProcessed: 1, 2, 3, 4
        // Ranks should be: 1, 2, 2, 4
        assertEquals(1, result[0].rank)
        assertEquals(2, result[1].rank)
        assertEquals(2, result[2].rank)
        assertEquals(4, result[3].rank)
    }

    // ============ Integration Tests ============

    @Test
    fun `full workflow with real tournament scenario`() = runTest {
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
        assertEquals(4, results.size)

        // Verify order and points
        assertEquals(2.5, results[0].points ?: 0.0, 0.0)  // Alice
        assertEquals(1.0, results[1].points ?: 0.0, 0.0)  // Charlie
        assertEquals(0.5, results[2].points ?: 0.0, 0.0)  // Bob
        assertEquals(0.0, results[3].points ?: 0.0, 0.0)  // Diana

        // Verify ranks (dense ranking)
        assertEquals(1, results[0].rank)  // Alice: rank 1
        assertEquals(2, results[1].rank)  // Charlie: rank 2
        assertEquals(3, results[2].rank)  // Bob: rank 3
        assertEquals(4, results[3].rank)  // Diana: rank 4
    }
}
