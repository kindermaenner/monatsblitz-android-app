package de.kindermaenner.monatsblitz.infrastructure.api.dto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DtoTest {

    @Test
    fun `TournamentDto data class methods test`() {
        val dto1 = TournamentDto(id = 1, date = "2026-10-18", mode = "3+2", roundCount = 1, status = "open")
        val dto2 = TournamentDto(id = 1, date = "2026-10-18", mode = "3+2", roundCount = 1, status = "open")
        val dto3 = dto1.copy(status = "finalized")

        assertEquals(1, dto1.id)
        assertEquals("2026-10-18", dto1.date)
        assertEquals("3+2", dto1.mode)
        assertEquals(1, dto1.roundCount)
        assertEquals("open", dto1.status)
        assertEquals(dto1, dto2)
        assertEquals(dto1.hashCode(), dto2.hashCode())
        assertNotEquals(dto1, dto3)
        assertFalse(dto1.equals(null))
        assertFalse(dto1.equals("other"))
        assertTrue(dto1.toString().contains("TournamentDto"))
        assertEquals("finalized", dto3.status)
    }

    @Test
    fun `NewTournamentDto data class methods test`() {
        val dto1 = NewTournamentDto(date = "2026-10-18", mode = "5+0", roundCount = 2)
        val dto2 = NewTournamentDto(date = "2026-10-18", mode = "5+0", roundCount = 2)
        val dto3 = dto1.copy(roundCount = 1)

        assertEquals(dto1, dto2)
        assertEquals(dto1.hashCode(), dto2.hashCode())
        assertNotEquals(dto1, dto3)
        assertFalse(dto1.equals(null))
        assertTrue(dto1.toString().contains("NewTournamentDto"))
    }

    @Test
    fun `CreateTournamentResponseDto data class methods test`() {
        val dto1 = CreateTournamentResponseDto(success = true, tournamentId = 10)
        val dto2 = CreateTournamentResponseDto(success = true, tournamentId = 10)
        val dto3 = dto1.copy(success = false)

        assertEquals(dto1, dto2)
        assertEquals(dto1.hashCode(), dto2.hashCode())
        assertNotEquals(dto1, dto3)
        assertFalse(dto1.equals(null))
        assertTrue(dto1.toString().contains("CreateTournamentResponseDto"))
    }

    @Test
    fun `GameDtoData data class methods test`() {
        val dto1 = GameDtoData(gameId = 1, leg = 1, player1Id = 10, player2Id = 20, result = "1:0")
        val dto2 = GameDtoData(gameId = 1, leg = 1, player1Id = 10, player2Id = 20, result = "1:0")
        val dto3 = dto1.copy(gameId = null)

        assertEquals(dto1, dto2)
        assertEquals(dto1.hashCode(), dto2.hashCode())
        assertNotEquals(dto1, dto3)
        assertFalse(dto1.equals(null))
        assertTrue(dto1.toString().contains("GameDtoData"))
    }

    @Test
    fun `CreateGameDto and UpdateGameDto data class methods test`() {
        val c1 = CreateGameDto(tournamentId = 5, leg = 1, player1Id = 10, player2Id = 20, result = "remis")
        val c2 = CreateGameDto(tournamentId = 5, leg = 1, player1Id = 10, player2Id = 20, result = "remis")
        val c3 = c1.copy(result = "1:0")
        assertEquals(c1, c2)
        assertEquals(c1.hashCode(), c2.hashCode())
        assertNotEquals(c1, c3)
        assertFalse(c1.equals(null))
        assertTrue(c1.toString().contains("CreateGameDto"))

        val u1 = UpdateGameDto(id = 1, tournamentId = 5, leg = 1, player1Id = 10, player2Id = 20, result = "remis")
        val u2 = UpdateGameDto(id = 1, tournamentId = 5, leg = 1, player1Id = 10, player2Id = 20, result = "remis")
        val u3 = u1.copy(id = 2)
        assertEquals(u1, u2)
        assertEquals(u1.hashCode(), u2.hashCode())
        assertNotEquals(u1, u3)
        assertFalse(u1.equals(null))
        assertTrue(u1.toString().contains("UpdateGameDto"))
    }

    @Test
    fun `GameDto and CreateGamesDto data class methods test`() {
        val g1 = GameDto(id = 99, tournamentId = 5, leg = 1, player1Id = 10, player2Id = 20, result = "0:1")
        val g2 = GameDto(id = 99, tournamentId = 5, leg = 1, player1Id = 10, player2Id = 20, result = "0:1")
        val g3 = g1.copy(result = "1:0")
        assertEquals(g1, g2)
        assertEquals(g1.hashCode(), g2.hashCode())
        assertNotEquals(g1, g3)
        assertFalse(g1.equals(null))
        assertTrue(g1.toString().contains("GameDto"))

        val cg1 = CreateGamesDto(tournamentId = 5, games = listOf(GameDtoData(leg = 1, player1Id = 1, player2Id = 2, result = "open")))
        val cg2 = cg1.copy()
        assertEquals(cg1, cg2)
        assertEquals(cg1.hashCode(), cg2.hashCode())
        assertTrue(cg1.toString().contains("CreateGamesDto"))
    }

    @Test
    fun `GameSyncResponseItemDto and CreateGamesResponseDto data class methods test`() {
        val item1 = GameSyncResponseItemDto(success = true, gameId = 7)
        val item2 = GameSyncResponseItemDto(success = true, gameId = 7)
        val item3 = item1.copy(success = false)
        assertEquals(item1, item2)
        assertEquals(item1.hashCode(), item2.hashCode())
        assertNotEquals(item1, item3)
        assertFalse(item1.equals(null))
        assertTrue(item1.toString().contains("GameSyncResponseItemDto"))

        val resp1 = CreateGamesResponseDto(success = true, count = 1, items = listOf(item1))
        val resp2 = resp1.copy()
        assertEquals(resp1, resp2)
        assertEquals(resp1.hashCode(), resp2.hashCode())
        assertTrue(resp1.toString().contains("CreateGamesResponseDto"))
    }

    @Test
    fun `ResultDto, CreateResultDto, and CreateResultsDto data class methods test`() {
        val r1 = ResultDto(playerId = 1, points = 3.5, rank = 2)
        val r2 = ResultDto(playerId = 1, points = 3.5, rank = 2)
        val r3 = r1.copy(rank = 3)
        assertEquals(r1, r2)
        assertEquals(r1.hashCode(), r2.hashCode())
        assertNotEquals(r1, r3)
        assertFalse(r1.equals(null))
        assertTrue(r1.toString().contains("ResultDto"))

        val cr1 = CreateResultDto(tournamentId = 1, result = r1)
        val cr2 = cr1.copy()
        assertEquals(cr1, cr2)
        assertEquals(cr1.hashCode(), cr2.hashCode())
        assertTrue(cr1.toString().contains("CreateResultDto"))

        val crs1 = CreateResultsDto(tournamentId = 1, results = listOf(r1))
        val crs2 = crs1.copy()
        assertEquals(crs1, crs2)
        assertEquals(crs1.hashCode(), crs2.hashCode())
        assertTrue(crs1.toString().contains("CreateResultsDto"))

        val rbi1 = ResultBatchItemDto(success = true, resultId = 10, playerId = 1, points = 3.5, rank = 2, message = "ok")
        val rbi2 = rbi1.copy()
        val rbi3 = rbi1.copy(rank = 3)
        assertEquals(rbi1, rbi2)
        assertEquals(rbi1.hashCode(), rbi2.hashCode())
        assertNotEquals(rbi1, rbi3)
        assertTrue(rbi1.toString().contains("ResultBatchItemDto"))
    }

    @Test
    fun `FinalizeRequestDto and FinalizeResponseDto data class methods test`() {
        val req1 = FinalizeRequestDto(tournamentId = 42)
        val req2 = req1.copy()
        assertEquals(42, req1.tournamentId)
        assertEquals(req1, req2)
        assertEquals(req1.hashCode(), req2.hashCode())
        assertTrue(req1.toString().contains("FinalizeRequestDto"))

        val resp1 = FinalizeResponseDto(success = true, tournamentId = 42, postId = 100, postUpdated = false, published = true)
        val resp2 = FinalizeResponseDto(success = true, tournamentId = 42, postId = 100, postUpdated = false, published = true)
        val resp3 = resp1.copy(published = false)
        assertEquals(true, resp1.success)
        assertEquals(42, resp1.tournamentId)
        assertEquals(100, resp1.postId)
        assertEquals(false, resp1.postUpdated)
        assertEquals(true, resp1.published)
        assertEquals(resp1, resp2)
        assertEquals(resp1.hashCode(), resp2.hashCode())
        assertNotEquals(resp1, resp3)
        assertFalse(resp1.equals(null))
        assertTrue(resp1.toString().contains("FinalizeResponseDto"))

        val respEmpty = FinalizeResponseDto(success = false)
        assertNotNull(respEmpty)
    }

    @Test
    fun `CreateYearPageDto and RecreatePostsDto data class methods test`() {
        val yp1 = CreateYearPageDto(year = 2026)
        val yp2 = yp1.copy()
        assertEquals(yp1, yp2)
        assertEquals(yp1.hashCode(), yp2.hashCode())
        assertTrue(yp1.toString().contains("CreateYearPageDto"))

        val rp1 = RecreatePostsDto(processed = 10, succeeded = 8, failed = 2, errors = listOf("E1"))
        val rp2 = rp1.copy()
        assertEquals(rp1, rp2)
        assertEquals(rp1.hashCode(), rp2.hashCode())
        assertTrue(rp1.toString().contains("RecreatePostsDto"))
    }

    @Test
    fun `PlayerDto, NewPlayerDto, and PlayerBatchItemDto data class methods test`() {
        val p1 = PlayerDto(id = 1L, surname = "Mustermann", forename = "Max")
        val p2 = p1.copy()
        assertEquals(p1, p2)
        assertEquals(p1.hashCode(), p2.hashCode())
        assertTrue(p1.toString().contains("PlayerDto"))

        val np1 = NewPlayerDto(forename = "Max", surname = "Mustermann")
        val np2 = np1.copy()
        assertEquals(np1, np2)
        assertEquals(np1.hashCode(), np2.hashCode())
        assertTrue(np1.toString().contains("NewPlayerDto"))

        val bi1 = PlayerBatchItemDto(success = true, playerId = 1L, message = "OK")
        val bi2 = bi1.copy()
        val bi3 = bi1.copy(message = null)
        assertEquals(bi1, bi2)
        assertEquals(bi1.hashCode(), bi2.hashCode())
        assertNotEquals(bi1, bi3)
        assertTrue(bi1.toString().contains("PlayerBatchItemDto"))
    }

    @Test
    fun `BatchResponseDto data class methods test`() {
        val b1 = BatchResponseDto(success = true, count = 1, items = listOf("A"), errors = null)
        val b2 = b1.copy()
        val b3 = b1.copy(errors = listOf("Err"))
        assertEquals(b1, b2)
        assertEquals(b1.hashCode(), b2.hashCode())
        assertNotEquals(b1, b3)
        assertTrue(b1.toString().contains("BatchResponseDto"))
    }
}
