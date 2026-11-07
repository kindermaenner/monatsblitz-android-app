package de.kindermaenner.monatsblitz.infrastructure.api.dto

import de.kindermaenner.monatsblitz.domain.model.GameMode
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.ModeEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DtoBranchTest {

    @Test
    fun tournamentDto_branchCoverage() {
        val t1 = TournamentDto(
            id = 1,
            date = "2026-11-07",
            mode = "3+2",
            roundCount = 1,
            dailyNumber = 1,
            status = "open",
            countsForRanking = true,
            label = "Turnier A",
            startTime = "18:00",
            playerIds = listOf(1L, 2L),
            withdrawnPlayerIds = listOf(3L)
        )
        val t2 = t1.copy(label = "Turnier B")
        val tDefault = TournamentDto(id = 2, date = "2026-11-08", mode = "5+0", roundCount = 2)

        assertEquals(t1, t1)
        assertNotEquals(t1, t2)
        assertFalse(t1.equals(null))
        assertFalse(t1.equals("other"))
        assertEquals(t1.hashCode(), t1.copy().hashCode())
        assertNotNull(t1.toString())
        assertEquals("open", tDefault.status)
        assertTrue(tDefault.countsForRanking)
        assertEquals("", tDefault.label)

        val newT1 = NewTournamentDto(
            date = "2026-11-07",
            mode = "3+2",
            roundCount = 1,
            dailyNumber = 1,
            label = "L",
            countsForRanking = true,
            confirmParallel = true,
            playerIds = listOf(1L)
        )
        val newT2 = newT1.copy(label = "Other")
        assertEquals(newT1, newT1.copy())
        assertNotEquals(newT1, newT2)
        assertNotNull(newT1.toString())
    }

    @Test
    fun resultDto_branchCoverage() {
        val r1 = ResultDto(playerId = 1, points = 2.5, rank = 1)
        val r2 = r1.copy(points = 3.0)

        assertEquals(r1, r1)
        assertNotEquals(r1, r2)
        assertFalse(r1.equals(null))
        assertEquals(r1.hashCode(), r1.copy().hashCode())
        assertNotNull(r1.toString())

        val createR = CreateResultDto(tournamentId = 1, result = r1)
        assertEquals(createR, createR.copy())
        assertNotNull(createR.toString())

        val createRs = CreateResultsDto(tournamentId = 1, results = listOf(r1))
        assertEquals(createRs, createRs.copy())
        assertNotNull(createRs.toString())

        val batchItem = ResultBatchItemDto(success = true, resultId = 10, playerId = 1, points = 2.0, rank = 1, message = "ok")
        assertEquals(batchItem, batchItem.copy())

        val batchR = ResultBatchResponseDto(success = true, count = 1, items = listOf(batchItem), errors = null)
        assertEquals(batchR, batchR.copy())
    }

    @Test
    fun finalizeDto_branchCoverage() {
        val req1 = FinalizeRequestDto(tournamentId = 5)
        assertEquals(req1, req1.copy())
        assertEquals(req1.hashCode(), req1.copy().hashCode())
        assertNotNull(req1.toString())

        val resp1 = FinalizeResponseDto(success = true, tournamentId = 5, postId = 10, postUpdated = true, published = true)
        val resp2 = FinalizeResponseDto(success = false, tournamentId = null, postId = null, postUpdated = null, published = null)
        assertEquals(resp1, resp1)
        assertNotEquals(resp1, resp2)
        assertNotNull(resp1.toString())
    }

    @Test
    fun modeDto_branchCoverage() {
        val m1 = ModeDto(id = 1L, name = "3+2")
        val m2 = m1.copy(name = "5+0")
        assertEquals(m1, m1)
        assertNotEquals(m1, m2)
        assertNotNull(m1.toString())

        val newM = NewModeDto(name = "Handicap")
        assertEquals(newM, newM.copy())

        val createM = CreateModeResponseDto(success = true, id = 10L, name = "10+0", message = "ok")
        assertEquals(createM, createM.copy())

        val entity = m1.toEntity()
        assertEquals(1L, entity.remoteId)
        assertEquals("3+2", entity.name)
        assertFalse(entity.dirty)

        val domain = entity.toDomain()
        assertEquals("3+2", domain.name)

        val gm = GameMode(id = 2L, remoteId = 3L, name = "Blitz", dirty = true)
        val gmEntity = gm.toEntity()
        assertEquals(2L, gmEntity.id)
        assertEquals(3L, gmEntity.remoteId)
        assertTrue(gmEntity.dirty)
    }

    @Test
    fun playerDto_branchCoverage() {
        val p1 = PlayerDto(id = 1L, surname = "Mustermann", forename = "Max", rufname = "Mäx", lastParticipated = "2026-11-01")
        val p2 = p1.copy(rufname = null)
        assertEquals(p1, p1)
        assertNotEquals(p1, p2)
        assertNotNull(p1.toString())

        val newP = NewPlayerDto(forename = "B", surname = "A", rufname = "C")
        assertEquals(newP, newP.copy())

        val createP = CreatePlayerResponseDto(success = true, playerId = 10L, message = "created")
        assertEquals(createP, createP.copy())

        val batchItem = PlayerBatchItemDto(success = true, playerId = 12L, message = "ok")
        assertEquals(batchItem, batchItem.copy())

        val batchResp = PlayerBatchResponseDto(success = true, count = 1, items = listOf(batchItem), errors = null)
        assertEquals(batchResp, batchResp.copy())
    }

    @Test
    fun gameDto_branchCoverage() {
        val g1 = GameDto(id = 10, gameId = 0, tournamentId = 1, leg = 1, player1Id = 2, player2Id = 3, result = "1:0")
        val g2 = GameDto(id = 0, gameId = 20, tournamentId = 1, leg = 2, player1Id = 3, player2Id = 2, result = "0:1")

        assertEquals(10, g1.resolvedId)
        assertEquals(20, g2.resolvedId)
        assertEquals(g1, g1)
        assertNotEquals(g1, g2)
        assertNotNull(g1.toString())

        val data1 = GameDtoData(gameId = 1, leg = 1, player1Id = 2, player2Id = 3, result = "1:0")
        val data2 = GameDtoData(gameId = null, leg = 1, player1Id = 2, player2Id = 3, result = "1:0")
        assertEquals(data1, data1.copy())
        assertNotEquals(data1, data2)

        val createGame = CreateGameDto(tournamentId = 1, leg = 1, player1Id = 2, player2Id = 3, result = "1:0")
        assertEquals(createGame, createGame.copy())

        val updateGame = UpdateGameDto(id = 5, tournamentId = 1, leg = 1, player1Id = 2, player2Id = 3, result = "0:1")
        assertEquals(updateGame, updateGame.copy())

        val createGames = CreateGamesDto(tournamentId = 1, games = listOf(data1))
        assertEquals(createGames, createGames.copy())

        val syncItem = GameSyncResponseItemDto(success = true, gameId = 42)
        assertEquals(syncItem, syncItem.copy())

        val createResp = CreateGamesResponseDto(success = true, count = 1, items = listOf(syncItem))
        assertEquals(createResp, createResp.copy())
    }

    @Test
    fun apiErrorDto_branchCoverage() {
        val err1 = ApiErrorDto(code = "err", message = "Fehler")
        val err2 = ApiErrorDto(code = "err2", message = "Anderer Fehler")
        assertEquals(err1, err1)
        assertNotEquals(err1, err2)
        assertNotNull(err1.toString())
        assertEquals("Fehler", ApiErrorDto.extractMessage("""{"message":"Fehler"}"""))
        assertEquals(null, ApiErrorDto.extractMessage("invalid json"))
        assertEquals(null, ApiErrorDto.extractMessage(null))

        val exWithMessage = Exception("Custom message")
        assertEquals("Custom message", exWithMessage.toDetailedErrorMessage())

        val exEmpty = RuntimeException("")
        assertEquals("RuntimeException", exEmpty.toDetailedErrorMessage())
    }

    @Test
    fun addTournamentPlayer_and_withdraw_branchCoverage() {
        val add1 = AddTournamentPlayerDto(tournamentId = 1, playerId = 2)
        assertEquals(add1, add1.copy())

        val addResp1 = AddTournamentPlayerResponseDto(success = true, tournamentId = 1, playerId = 2)
        val addResp2 = AddTournamentPlayerResponseDto(success = false, tournamentId = null, playerId = null)
        assertEquals(addResp1, addResp1)
        assertNotEquals(addResp1, addResp2)

        val w1 = WithdrawPlayerDto(tournamentId = 1, playerId = 2, hasLeft = 1)
        assertEquals(w1, w1.copy())

        val wResp1 = WithdrawPlayerResponseDto(success = true, tournamentId = 1, playerId = 2, hasLeft = 1)
        val wResp2 = WithdrawPlayerResponseDto(success = false, tournamentId = 2, playerId = 3, hasLeft = 0)
        assertEquals(wResp1, wResp1)
        assertNotEquals(wResp1, wResp2)
    }

    @Test
    fun responseDtos_branchCoverage() {
        val createT = CreateTournamentResponseDto(success = true, tournamentId = 99)
        assertEquals(createT, createT.copy())

        val batchResp = BatchResponseDto(success = true, count = 2, items = listOf("item1", "item2"), errors = listOf("err"))
        assertEquals(batchResp, batchResp.copy())
    }
}
