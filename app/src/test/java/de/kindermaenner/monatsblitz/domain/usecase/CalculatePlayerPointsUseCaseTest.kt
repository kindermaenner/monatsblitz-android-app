package de.kindermaenner.monatsblitz.domain.usecase

import de.kindermaenner.monatsblitz.domain.model.Game
import de.kindermaenner.monatsblitz.domain.model.GameMode
import de.kindermaenner.monatsblitz.domain.model.GameResult
import de.kindermaenner.monatsblitz.domain.model.Player
import de.kindermaenner.monatsblitz.domain.model.Tournament
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class CalculatePlayerPointsUseCaseTest {

    private val useCase = CalculatePlayerPointsUseCase()

    @Test
    fun `invoke should correctly calculate total points for player`() {
        val players = listOf(
            Player(1, "A", "A"),
            Player(2, "B", "B"),
            Player(3, "C", "C")
        )
        val games = mapOf(
            1L to Game(1, 1, 2, 1, GameResult.Win),       // 1.0
            2L to Game(2, 1, 3, 1, GameResult.Remis),     // 0.5
            3L to Game(3, 2, 1, 1, GameResult.Loss),      // 0.0
            4L to Game(4, 3, 1, 1, GameResult.Remis)      // 0.5
        )
        val tournament = Tournament(100, GameMode.BLITZ_3_2, LocalDate.now(), 1, players, games)

        val player1Points = useCase(tournament, 1L)
        val player2Points = useCase(tournament, 2L)
        val player3Points = useCase(tournament, 3L)

        assertEquals(1.5, player1Points, 0.0)
        assertEquals(0.0, player2Points, 0.0)
        assertEquals(0.5, player3Points, 0.0)
    }

    @Test
    fun `invoke should return 0 points when player has no games`() {
        val tournament = Tournament(100, GameMode.BLITZ_3_2, LocalDate.now(), 1, listOf(Player(1, "A", "A")), emptyMap())
        val points = useCase(tournament, 1L)
        assertEquals(0.0, points, 0.0)
    }

    @Test
    fun `invoke should correctly calculate points from canonical single-row games`() {
        val players = listOf(
            Player(1, "A", "A"),
            Player(2, "B", "B"),
            Player(3, "C", "C")
        )
        val canonicalGames = mapOf(
            1L to Game(1, 1, 2, 1, GameResult.Win),   // P1: 1.0, P2: 0.0
            2L to Game(2, 1, 3, 1, GameResult.Remis), // P1: 0.5, P3: 0.5
            3L to Game(3, 2, 3, 1, GameResult.Loss)   // P2: 0.0, P3: 1.0
        )
        val tournament = Tournament(100, GameMode.BLITZ_3_2, LocalDate.now(), 1, players, canonicalGames)

        assertEquals(1.5, useCase(tournament, 1L), 0.0)
        assertEquals(0.0, useCase(tournament, 2L), 0.0)
        assertEquals(1.5, useCase(tournament, 3L), 0.0)
    }

    @Test
    fun `invoke should calculate points for forfeit draw and forfeit win and loss`() {
        val players = listOf(
            Player(1, "A", "A"),
            Player(2, "B", "B"),
            Player(3, "C", "C")
        )
        val canonicalGames = mapOf(
            1L to Game(1, 1, 2, 1, GameResult.ForfeitDraw), // P1: 0.5, P2: 0.5
            2L to Game(2, 1, 3, 1, GameResult.ForfeitWin),  // P1: 1.0, P3: 0.0
            3L to Game(3, 2, 3, 1, GameResult.ForfeitLoss)  // P2: 0.0, P3: 1.0
        )
        val tournament = Tournament(100, GameMode.BLITZ_3_2, LocalDate.now(), 1, players, canonicalGames)

        assertEquals(1.5, useCase(tournament, 1L), 0.0)
        assertEquals(0.5, useCase(tournament, 2L), 0.0)
        assertEquals(1.0, useCase(tournament, 3L), 0.0)
    }

    @Test
    fun `invoke should aggregate points across both legs in 2-round tournament`() {
        val players = listOf(
            Player(1, "A", "A"),
            Player(2, "B", "B")
        )
        val games = mapOf(
            1L to Game(id = 1, player1Id = 1, player2Id = 2, leg = 1, result = GameResult.Win),    // P1: +1.0, P2: 0.0
            2L to Game(id = 2, player1Id = 1, player2Id = 2, leg = 2, result = GameResult.Remis)   // P1: +0.5, P2: +0.5
        )
        val tournament = Tournament(100, GameMode.BLITZ_3_2, LocalDate.now(), 2, players, games)

        assertEquals(1.5, useCase(tournament, 1L), 0.0)
        assertEquals(0.5, useCase(tournament, 2L), 0.0)
    }

    @Test
    fun `invoke should award zero points for open unplayed games`() {
        val players = listOf(
            Player(1, "A", "A"),
            Player(2, "B", "B")
        )
        val games = mapOf(
            1L to Game(id = 1, player1Id = 1, player2Id = 2, leg = 1, result = GameResult.Open)
        )
        val tournament = Tournament(100, GameMode.BLITZ_3_2, LocalDate.now(), 1, players, games)

        assertEquals(0.0, useCase(tournament, 1L), 0.0)
        assertEquals(0.0, useCase(tournament, 2L), 0.0)
    }

    @Test
    fun `invoke calculates points correctly when player is player2 in stored canonical pairing`() {
        val players = listOf(
            Player(1, "A", "A"),
            Player(2, "B", "B")
        )
        // P1 loses to P2 (stored canonically as 1 vs 2, Result.Loss) -> P2 should get 1.0 pt
        val games = mapOf(
            1L to Game(id = 1, player1Id = 1, player2Id = 2, leg = 1, result = GameResult.Loss)
        )
        val tournament = Tournament(100, GameMode.BLITZ_3_2, LocalDate.now(), 1, players, games)

        assertEquals(0.0, useCase(tournament, 1L), 0.0)
        assertEquals(1.0, useCase(tournament, 2L), 0.0)
    }

    @Test
    fun `invoke ignores mirrored duplicate game in same leg`() {
        val players = listOf(Player(1, "A", "A"), Player(2, "B", "B"))
        // Stored both 1 vs 2 (Win) and 2 vs 1 (Loss) for leg 1
        val games = mapOf(
            1L to Game(id = 1, player1Id = 1, player2Id = 2, leg = 1, result = GameResult.Win),
            2L to Game(id = 2, player1Id = 2, player2Id = 1, leg = 1, result = GameResult.Loss)
        )
        val tournament = Tournament(100, GameMode.BLITZ_3_2, LocalDate.now(), 1, players, games)

        // For player 2: asPlayer1 is Loss (0.0), and asPlayer2 is ignored because leg 1 already has game where P2 is player 1
        assertEquals(1.0, useCase(tournament, 1L), 0.0)
        assertEquals(0.0, useCase(tournament, 2L), 0.0)
    }

    @Test
    fun `invoke calculates points correctly with mixed regular and forfeit results across multiple opponents`() {
        val players = (1L..6L).map { Player(it, "P$it", "V$it") }
        val games = mapOf(
            1L to Game(id = 1, player1Id = 1, player2Id = 2, leg = 1, result = GameResult.Win),         // +1.0
            2L to Game(id = 2, player1Id = 1, player2Id = 3, leg = 1, result = GameResult.ForfeitLoss), // +0.0
            3L to Game(id = 3, player1Id = 1, player2Id = 4, leg = 1, result = GameResult.ForfeitDraw), // +0.5
            4L to Game(id = 4, player1Id = 1, player2Id = 5, leg = 1, result = GameResult.Remis),       // +0.5
            5L to Game(id = 5, player1Id = 1, player2Id = 6, leg = 1, result = GameResult.ForfeitWin)   // +1.0
        )
        val tournament = Tournament(100, GameMode.BLITZ_3_2, LocalDate.now(), 1, players, games)

        assertEquals(3.0, useCase(tournament, 1L), 0.0)
    }

    @Test
    fun `invoke returns 0 for non-participating unknown player ID`() {
        val players = listOf(Player(1, "A", "A"), Player(2, "B", "B"))
        val games = mapOf(1L to Game(id = 1, player1Id = 1, player2Id = 2, leg = 1, result = GameResult.Win))
        val tournament = Tournament(100, GameMode.BLITZ_3_2, LocalDate.now(), 1, players, games)

        assertEquals(0.0, useCase(tournament, 999L), 0.0)
    }

    @Test
    fun `invoke handles player with games in only one leg of double round tournament`() {
        val players = listOf(Player(1, "A", "A"), Player(2, "B", "B"))
        // Only round 1 played, round 2 still open
        val games = mapOf(
            1L to Game(id = 1, player1Id = 1, player2Id = 2, leg = 1, result = GameResult.Win),
            2L to Game(id = 2, player1Id = 1, player2Id = 2, leg = 2, result = GameResult.Open)
        )
        val tournament = Tournament(100, GameMode.BLITZ_3_2, LocalDate.now(), 2, players, games)

        assertEquals(1.0, useCase(tournament, 1L), 0.0)
        assertEquals(0.0, useCase(tournament, 2L), 0.0)
    }
}
