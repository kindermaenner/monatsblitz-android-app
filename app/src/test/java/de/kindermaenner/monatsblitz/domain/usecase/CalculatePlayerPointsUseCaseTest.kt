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
}
