package de.kindermaenner.monatsblitz.infrastructure.api.dto

import de.kindermaenner.monatsblitz.data.model.GameMode
import de.kindermaenner.monatsblitz.data.model.GameResult
import de.kindermaenner.monatsblitz.data.model.Player
import de.kindermaenner.monatsblitz.data.model.Tournament
import de.kindermaenner.monatsblitz.infrastructure.api.dto.GameDto
import de.kindermaenner.monatsblitz.infrastructure.api.dto.toDto
import de.kindermaenner.monatsblitz.infrastructure.api.dto.toGame
import de.kindermaenner.monatsblitz.infrastructure.api.dto.toGameResultOrNull
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class MappingUtilsTest {

    @Test
    fun parseResult_variants() {
        assertEquals(GameResult.Win, "1-0".toGameResultOrNull())
        assertEquals(GameResult.Loss, "0-1".toGameResultOrNull())
        assertEquals(GameResult.Remis, "0.5-0.5".toGameResultOrNull())
        assertEquals(GameResult.Remis, "1/2-1/2".toGameResultOrNull())
        assertNull("garbage".toGameResultOrNull())
    }

    @Test
    fun gameDtoToGame_and_back() {
        val p1 = Player(id = 1, Name = "Muster", Vorname = "Erster")
        val p2 = Player(id = 2, Name = "Zweiter", Vorname = "Zwei")
        val tournament = Tournament(
            Id = 42,
            Mode = GameMode.BLITZ_3_2,
            Date = LocalDate.now(),
            players = listOf(p1, p2),
            doubleRound = false
        )

        val dto = GameDto(leg = 1, player1_id = 1, player2_id = 2, result = "1-0")
        val game = dto.toGame(tournament)
        assertNotNull(game)
        assertEquals(GameResult.Win, game!!.result)
        assertEquals(p1.id, game.player1.id)
        assertEquals(p2.id, game.player2.id)

        val dto2 = game.toDto()
        assertEquals(dto.leg, dto2.leg)
        assertEquals(dto.player1_id, dto2.player1_id)
        assertEquals(dto.player2_id, dto2.player2_id)
        assertEquals(dto.result, dto2.result)
    }

    @Test
    fun invalidGame_returnsNull() {
        val p1 = Player(id = 1, Name = "Solo", Vorname = "Only")
        val tournament = Tournament(
            Id = 10,
            Mode = GameMode.BLITZ_3_2,
            Date = LocalDate.now(),
            players = listOf(p1),
            doubleRound = false
        )
        val dto = GameDto(leg = 1, player1_id = 1, player2_id = 99, result = "1-0")
        val game = dto.toGame(tournament)
        assertNull(game)

        val dtoBadResult = GameDto(leg = 1, player1_id = 1, player2_id = 1, result = "unknown")
        val game2 = dtoBadResult.toGame(tournament)
        assertNull(game2)
    }
}
