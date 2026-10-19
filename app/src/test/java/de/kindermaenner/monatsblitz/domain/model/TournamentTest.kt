package de.kindermaenner.monatsblitz.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class TournamentTest {

    private val player1 = Player(1, "Name1", "Vorname1")
    private val player2 = Player(2, "Name2", "Vorname2")
    private val players = listOf(player1, player2)

    @Test
    fun `playerIds returns list of all player ids`() {
        val tournament = Tournament(
            id = 1,
            mode = GameMode.BLITZ_3_2,
            date = LocalDate.now(),
            rounds = 1,
            players = players
        )
        assertEquals(listOf(1L, 2L), tournament.playerIds)
    }

    @Test
    fun `findGame returns correct game by player ids and leg including inverted perspective`() {
        val game = Game(id = 10, player1Id = 1, player2Id = 2, leg = 1, result = GameResult.Win, dirty = true)
        val tournament = Tournament(
            id = 1,
            mode = GameMode.BLITZ_3_2,
            date = LocalDate.now(),
            rounds = 1,
            players = players,
            games = mapOf(10L to game)
        )

        assertEquals(game, tournament.findGame(1, 2, 1))
        assertEquals(
            Game(id = 10, player1Id = 2, player2Id = 1, leg = 1, result = GameResult.Loss, dirty = true),
            tournament.findGame(2, 1, 1)
        )
        assertNull(tournament.findGame(1, 2, 2))
        assertNull(tournament.findGame(1, 3, 1))
    }
}
