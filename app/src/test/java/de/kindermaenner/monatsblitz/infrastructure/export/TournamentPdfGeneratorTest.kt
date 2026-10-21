package de.kindermaenner.monatsblitz.infrastructure.export

import de.kindermaenner.monatsblitz.domain.model.Game
import de.kindermaenner.monatsblitz.domain.model.GameMode
import de.kindermaenner.monatsblitz.domain.model.GameResult
import de.kindermaenner.monatsblitz.domain.model.Player
import de.kindermaenner.monatsblitz.domain.model.Tournament
import de.kindermaenner.monatsblitz.ui.ranking.RankingRowData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class TournamentPdfGeneratorTest {

    @Test
    fun `buildCrosstableRows formats crosstable with results and diagonal marks`() {
        val p1 = Player(1L, "Carlsen", "Magnus")
        val p2 = Player(2L, "Nakamura", "Hikaru")
        val p3 = Player(3L, "Ding", "Liren")
        val players = listOf(p1, p2, p3)

        val gamesList = listOf(
            Game(id = 101, player1Id = 1L, player2Id = 2L, leg = 1, result = GameResult.Win),
            Game(id = 102, player1Id = 2L, player2Id = 3L, leg = 1, result = GameResult.Remis),
            Game(id = 103, player1Id = 3L, player2Id = 1L, leg = 1, result = GameResult.ForfeitDraw)
        )
        val gamesMap = gamesList.associateBy { it.id }

        val tournament = Tournament(
            id = 1L,
            mode = GameMode.BLITZ_3_2,
            date = LocalDate.of(2026, 10, 21),
            rounds = 1,
            players = players,
            games = gamesMap,
            withdrawnPlayerIds = setOf(3L),
            isLocalOnly = true
        )

        val rankingRows = listOf(
            RankingRowData(rank = 1, name = "Magnus Carlsen", points = 1.5),
            RankingRowData(rank = 2, name = "Hikaru Nakamura", points = 0.5),
            RankingRowData(rank = 3, name = "Liren Ding", points = 0.5)
        )

        val rows = TournamentPdfGenerator.buildCrosstableRows(tournament, rankingRows)

        assertEquals(3, rows.size)

        // Row 0 (Carlsen): vs p1 = "x", vs p2 = "1", vs p3 = "="
        val r0 = rows[0]
        assertEquals(1L, r0.playerId)
        assertEquals("Magnus Carlsen", r0.playerName)
        assertEquals(1.5, r0.totalPoints, 0.001)
        assertEquals(listOf("x", "1", "="), r0.results)
        assertEquals(false, r0.isWithdrawn)

        // Row 1 (Nakamura): vs p1 = "0", vs p2 = "x", vs p3 = "½"
        val r1 = rows[1]
        assertEquals(2L, r1.playerId)
        assertEquals(0.5, r1.totalPoints, 0.001)
        assertEquals(listOf("0", "x", "½"), r1.results)
        assertEquals(false, r1.isWithdrawn)

        // Row 2 (Ding): vs p1 = "=", vs p2 = "½", vs p3 = "x"
        val r2 = rows[2]
        assertEquals(3L, r2.playerId)
        assertEquals(0.5, r2.totalPoints, 0.001)
        assertEquals(listOf("=", "½", "x"), r2.results)
        assertTrue(r2.isWithdrawn)
    }
}
