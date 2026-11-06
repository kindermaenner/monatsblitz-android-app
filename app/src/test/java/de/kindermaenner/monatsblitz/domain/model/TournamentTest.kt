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

    @Test
    fun `findGame inverts forfeit results when viewed from opposite player perspective`() {
        val gameWin = Game(id = 1, player1Id = 1, player2Id = 2, leg = 1, result = GameResult.ForfeitWin)
        val gameLoss = Game(id = 2, player1Id = 1, player2Id = 3, leg = 1, result = GameResult.ForfeitLoss)
        val gameDraw = Game(id = 3, player1Id = 2, player2Id = 3, leg = 1, result = GameResult.ForfeitDraw)

        val tournament = Tournament(
            id = 5,
            mode = GameMode.BLITZ_5_0,
            date = LocalDate.now(),
            rounds = 1,
            players = players + Player(3, "Name3", "Vorname3"),
            games = mapOf(1L to gameWin, 2L to gameLoss, 3L to gameDraw)
        )

        // ForfeitWin (1 wins against 2) -> viewed by 2 against 1 is ForfeitLoss
        val fromP2 = tournament.findGame(2, 1, 1)
        assertEquals(GameResult.ForfeitLoss, fromP2?.result)
        assertEquals(2L, fromP2?.player1Id)
        assertEquals(1L, fromP2?.player2Id)

        // ForfeitLoss (1 loses against 3) -> viewed by 3 against 1 is ForfeitWin
        val fromP3 = tournament.findGame(3, 1, 1)
        assertEquals(GameResult.ForfeitWin, fromP3?.result)
        assertEquals(3L, fromP3?.player1Id)
        assertEquals(1L, fromP3?.player2Id)

        // ForfeitDraw (=) -> viewed from other side remains ForfeitDraw
        val drawFromP3 = tournament.findGame(3, 2, 1)
        assertEquals(GameResult.ForfeitDraw, drawFromP3?.result)
    }

    @Test
    fun `findGame distinguishes games in different legs for the same pairing`() {
        val leg1Game = Game(id = 101, player1Id = 1, player2Id = 2, leg = 1, result = GameResult.Win)
        val leg2Game = Game(id = 102, player1Id = 1, player2Id = 2, leg = 2, result = GameResult.Loss)

        val tournament = Tournament(
            id = 1,
            mode = GameMode.BLITZ_3_2,
            date = LocalDate.now(),
            rounds = 2,
            players = players,
            games = mapOf(101L to leg1Game, 102L to leg2Game)
        )

        assertEquals(GameResult.Win, tournament.findGame(1, 2, leg = 1)?.result)
        assertEquals(GameResult.Loss, tournament.findGame(1, 2, leg = 2)?.result)
        assertEquals(GameResult.Loss, tournament.findGame(2, 1, leg = 1)?.result)
        assertEquals(GameResult.Win, tournament.findGame(2, 1, leg = 2)?.result)
    }

    @Test
    fun `tournament domain model retains withdrawn player ids, ranking flag, and custom label`() {
        val tournament = Tournament(
            id = 42,
            mode = GameMode.HANDICAP,
            date = LocalDate.of(2026, 11, 6),
            rounds = 2,
            players = players,
            withdrawnPlayerIds = setOf(2L),
            isLocalOnly = true,
            countsForRanking = false,
            label = "Gruppe B"
        )

        assertEquals(setOf(2L), tournament.withdrawnPlayerIds)
        assertEquals(false, tournament.countsForRanking)
        assertEquals("Gruppe B", tournament.label)
        assertEquals(true, tournament.isLocalOnly)
        assertEquals(listOf(1L, 2L), tournament.playerIds)
    }
}
