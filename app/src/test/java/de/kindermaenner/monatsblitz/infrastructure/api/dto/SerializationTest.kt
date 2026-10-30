package de.kindermaenner.monatsblitz.infrastructure.api.dto

import kotlinx.serialization.json.Json
import org.junit.Assert.assertTrue
import org.junit.Test

class SerializationTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `CreateGamesDto serializes to snake_case for WordPress`() {
        val game = GameDtoData(leg = 1, player1Id = 10, player2Id = 20, result = "1:0")
        val dto = CreateGamesDto(tournamentId = 5, games = listOf(game))

        val result = json.encodeToString(dto)

        assertTrue("Should contain tournament_id", result.contains("\"tournament_id\":5"))
        assertTrue("Should contain leg_type", result.contains("\"leg_type\":1"))
        assertTrue("Should contain player1_id", result.contains("\"player1_id\":10"))
        assertTrue("Should contain player2_id", result.contains("\"player2_id\":20"))
    }

    @Test
    fun `NewTournamentDto serializes to snake_case for WordPress`() {
        val dto = NewTournamentDto(date = "2026-08-09", mode = "3+2", roundCount = 2)
        val result = json.encodeToString(dto)

        assertTrue("Should contain date", result.contains("\"date\":\"2026-08-09\""))
        assertTrue("Should contain mode", result.contains("\"mode\":\"3+2\""))
        assertTrue("Should contain round_count", result.contains("\"round_count\":2"))
    }

    @Test
    fun `ResultDto serializes points as double for remis`() {
        val dto = ResultDto(playerId = 1, points = 0.5, rank = 2)
        val result = json.encodeToString(dto)

        assertTrue("Should contain points as 0.5", result.contains("\"points\":0.5"))
        assertTrue("Should contain player_id", result.contains("\"player_id\":1"))
    }

    @Test
    fun `PlayerBatchResponseDto deserializes correctly from snake_case`() {
        val jsonString = """
            {
              "success": true,
              "count": 1,
              "items": [
                { "success": true, "player_id": 301 }
              ]
            }
        """.trimIndent()

        val response = json.decodeFromString<PlayerBatchResponseDto>(jsonString)

        assertTrue("Success should be true", response.success)
        assertTrue("Count should be 1", response.count == 1)
        assertTrue("First item should have id 301", response.items[0].playerId == 301L)
    }

    @Test
    fun `FinalizeRequestDto serializes to snake_case for WordPress`() {
        val dto = FinalizeRequestDto(tournamentId = 42)
        val result = json.encodeToString(dto)

        assertTrue("Should contain tournament_id", result.contains("\"tournament_id\":42"))
    }

    @Test
    fun `FinalizeResponseDto deserializes correctly from snake_case`() {
        val jsonString = """
            {
              "success": true,
              "tournament_id": 42,
              "post_id": 105,
              "post_updated": false,
              "published": true
            }
        """.trimIndent()

        val response = json.decodeFromString<FinalizeResponseDto>(jsonString)

        assertTrue("Success should be true", response.success)
        assertTrue("tournamentId should be 42", response.tournamentId == 42)
        assertTrue("postId should be 105", response.postId == 105)
        assertTrue("postUpdated should be false", response.postUpdated == false)
        assertTrue("published should be true", response.published == true)
    }

    @Test
    fun `TournamentDto deserializes with default open status or given status`() {
        val jsonDefault = """{"id": 1, "date": "2026-10-18", "mode": "3+2", "round_count": 1}"""
        val dtoDefault = json.decodeFromString<TournamentDto>(jsonDefault)
        assertTrue("Default status should be open", dtoDefault.status == "open")

        val jsonFinalized = """{"id": 2, "date": "2026-10-18", "mode": "3+2", "round_count": 1, "status": "finalized"}"""
        val dtoFinalized = json.decodeFromString<TournamentDto>(jsonFinalized)
        assertTrue("Status should be finalized", dtoFinalized.status == "finalized")
    }

    @Test
    fun `BatchResponseDto deserializes correctly with items and errors`() {
        val jsonString = """
            {
              "success": false,
              "count": 0,
              "items": [],
              "errors": ["Fehler beim Speichern"]
            }
        """.trimIndent()

        val response = json.decodeFromString<BatchResponseDto<String>>(jsonString)
        org.junit.Assert.assertFalse("Success should be false", response.success)
        org.junit.Assert.assertEquals(0, response.count)
        org.junit.Assert.assertTrue("Items should be empty", response.items.isEmpty())
        org.junit.Assert.assertEquals(listOf("Fehler beim Speichern"), response.errors)
    }

    @Test
    fun `GameDto, CreateGameDto, and UpdateGameDto serialize and deserialize correctly`() {
        val createGame = CreateGameDto(tournamentId = 1, leg = 1, player1Id = 10, player2Id = 20, result = "1:0")
        val createJson = json.encodeToString(createGame)
        val decodedCreate = json.decodeFromString<CreateGameDto>(createJson)
        org.junit.Assert.assertEquals(1, decodedCreate.tournamentId)
        org.junit.Assert.assertEquals("1:0", decodedCreate.result)

        val updateGame = UpdateGameDto(id = 5, tournamentId = 1, leg = 2, player1Id = 20, player2Id = 10, result = "0:1")
        val updateJson = json.encodeToString(updateGame)
        val decodedUpdate = json.decodeFromString<UpdateGameDto>(updateJson)
        org.junit.Assert.assertEquals(5, decodedUpdate.id)
        org.junit.Assert.assertEquals("0:1", decodedUpdate.result)

        val game = GameDto(id = 100, tournamentId = 2, leg = 1, player1Id = 3, player2Id = 4, result = "remis")
        val gameJson = json.encodeToString(game)
        val decodedGame = json.decodeFromString<GameDto>(gameJson)
        org.junit.Assert.assertEquals(100, decodedGame.id)
        org.junit.Assert.assertEquals(100, decodedGame.resolvedId)
        org.junit.Assert.assertEquals(3, decodedGame.player1Id)

        val wpJsonWithIdOnly = """{"id":42,"tournament_id":2,"leg_type":1,"player1_id":3,"player2_id":4,"result":"1:0"}"""
        val decodedFromWp = json.decodeFromString<GameDto>(wpJsonWithIdOnly)
        org.junit.Assert.assertEquals(42, decodedFromWp.resolvedId)

        val legacyJsonWithGameId = """{"game_id":88,"tournament_id":2,"leg_type":1,"player1_id":3,"player2_id":4,"result":"0:1"}"""
        val decodedFromLegacy = json.decodeFromString<GameDto>(legacyJsonWithGameId)
        org.junit.Assert.assertEquals(88, decodedFromLegacy.resolvedId)
    }

    @Test
    fun `CreateResultDto and CreateResultsDto serialize and deserialize correctly`() {
        val resultDto = ResultDto(playerId = 15, points = 2.5, rank = 3)
        val singleResult = CreateResultDto(tournamentId = 8, result = resultDto)
        val singleJson = json.encodeToString(singleResult)
        val decodedSingle = json.decodeFromString<CreateResultDto>(singleJson)
        org.junit.Assert.assertEquals(8, decodedSingle.tournamentId)
        org.junit.Assert.assertEquals(15, decodedSingle.result.playerId)

        val multiResult = CreateResultsDto(tournamentId = 8, results = listOf(resultDto))
        val multiJson = json.encodeToString(multiResult)
        val decodedMulti = json.decodeFromString<CreateResultsDto>(multiJson)
        org.junit.Assert.assertEquals(8, decodedMulti.tournamentId)
        org.junit.Assert.assertEquals(1, decodedMulti.results.size)
    }

    @Test
    fun `ResultBatchResponseDto deserializes correctly with minimal and enriched items`() {
        // Minimal item as previously returned by WP plugin
        val jsonMinimal = """
            {
              "success": true,
              "count": 1,
              "items": [
                { "success": true, "result_id": 42 }
              ]
            }
        """.trimIndent()
        val decodedMinimal = json.decodeFromString<ResultBatchResponseDto>(jsonMinimal)
        org.junit.Assert.assertTrue(decodedMinimal.success)
        org.junit.Assert.assertEquals(1, decodedMinimal.count)
        org.junit.Assert.assertEquals(42, decodedMinimal.items[0].resultId)
        org.junit.Assert.assertNull(decodedMinimal.items[0].playerId)
        org.junit.Assert.assertNull(decodedMinimal.items[0].points)
        org.junit.Assert.assertNull(decodedMinimal.items[0].rank)

        // Enriched item as now returned by WP plugin
        val jsonEnriched = """
            {
              "success": true,
              "count": 2,
              "items": [
                { "success": true, "result_id": 101, "player_id": 5, "points": 4.5, "rank": 1 },
                { "success": true, "result_id": 102, "player_id": 8, "points": 3.0, "rank": 2, "message": "Ergebnis aktualisiert" }
              ]
            }
        """.trimIndent()
        val decodedEnriched = json.decodeFromString<ResultBatchResponseDto>(jsonEnriched)
        org.junit.Assert.assertTrue(decodedEnriched.success)
        org.junit.Assert.assertEquals(2, decodedEnriched.count)
        org.junit.Assert.assertEquals(101, decodedEnriched.items[0].resultId)
        org.junit.Assert.assertEquals(5, decodedEnriched.items[0].playerId)
        org.junit.Assert.assertEquals(4.5, decodedEnriched.items[0].points)
        org.junit.Assert.assertEquals(1, decodedEnriched.items[0].rank)
        org.junit.Assert.assertEquals("Ergebnis aktualisiert", decodedEnriched.items[1].message)
    }

    @Test
    fun `CreateYearPageDto and RecreatePostsDto serialize and deserialize correctly`() {
        val yearPage = CreateYearPageDto(year = 2026)
        val yearJson = json.encodeToString(yearPage)
        val decodedYear = json.decodeFromString<CreateYearPageDto>(yearJson)
        org.junit.Assert.assertEquals(2026, decodedYear.year)

        val recreate = RecreatePostsDto(processed = 10, succeeded = 8, failed = 2, errors = listOf("Post 3 failed"))
        val recreateJson = json.encodeToString(recreate)
        val decodedRecreate = json.decodeFromString<RecreatePostsDto>(recreateJson)
        org.junit.Assert.assertEquals(10, decodedRecreate.processed)
        org.junit.Assert.assertEquals(8, decodedRecreate.succeeded)
        org.junit.Assert.assertEquals(2, decodedRecreate.failed)
        org.junit.Assert.assertEquals(listOf("Post 3 failed"), decodedRecreate.errors)
    }

    @Test
    fun `PlayerDto and NewPlayerDto serialize and deserialize correctly`() {
        val player = PlayerDto(id = 77L, surname = "Kasparov", forename = "Garry")
        val playerJson = json.encodeToString(player)
        val decodedPlayer = json.decodeFromString<PlayerDto>(playerJson)
        org.junit.Assert.assertEquals(77L, decodedPlayer.id)
        org.junit.Assert.assertEquals("Garry", decodedPlayer.forename)

        val newPlayer = NewPlayerDto(surname = "Karpov", forename = "Anatoly")
        val newPlayerJson = json.encodeToString(newPlayer)
        val decodedNewPlayer = json.decodeFromString<NewPlayerDto>(newPlayerJson)
        org.junit.Assert.assertEquals("Karpov", decodedNewPlayer.surname)
        org.junit.Assert.assertEquals("Anatoly", decodedNewPlayer.forename)
    }

    @Test
    fun `PlayerBatchItemDto handles various field states`() {
        val item1 = PlayerBatchItemDto(success = true, playerId = 99L, message = null)
        val json1 = json.encodeToString(item1)
        val decoded1 = json.decodeFromString<PlayerBatchItemDto>(json1)
        org.junit.Assert.assertTrue(decoded1.success)
        org.junit.Assert.assertEquals(99L, decoded1.playerId)
        org.junit.Assert.assertNull(decoded1.message)

        val item2 = PlayerBatchItemDto(success = false, playerId = null, message = "Duplikat")
        val json2 = json.encodeToString(item2)
        val decoded2 = json.decodeFromString<PlayerBatchItemDto>(json2)
        org.junit.Assert.assertFalse(decoded2.success)
        org.junit.Assert.assertNull(decoded2.playerId)
        org.junit.Assert.assertEquals("Duplikat", decoded2.message)
    }

    @Test
    fun `FinalizeResponseDto handles null optional fields`() {
        val jsonString = """{"success": false}"""
        val response = json.decodeFromString<FinalizeResponseDto>(jsonString)
        org.junit.Assert.assertFalse(response.success)
        org.junit.Assert.assertNull(response.tournamentId)
        org.junit.Assert.assertNull(response.postId)
        org.junit.Assert.assertNull(response.postUpdated)
        org.junit.Assert.assertNull(response.published)
    }

    @Test
    fun `CreatePlayerResponseDto deserializes correctly from WordPress single player response`() {
        val jsonString = """{"success": true, "player_id": 42}"""
        val response = json.decodeFromString<CreatePlayerResponseDto>(jsonString)
        org.junit.Assert.assertTrue(response.success)
        org.junit.Assert.assertEquals(42L, response.playerId)
    }

    @Test
    fun `WithdrawPlayerDto and WithdrawPlayerResponseDto serialize and deserialize correctly`() {
        val request = WithdrawPlayerDto(tournamentId = 5, playerId = 12, hasLeft = 1)
        val reqJson = json.encodeToString(request)
        val decodedReq = json.decodeFromString<WithdrawPlayerDto>(reqJson)
        org.junit.Assert.assertEquals(5, decodedReq.tournamentId)
        org.junit.Assert.assertEquals(12, decodedReq.playerId)
        org.junit.Assert.assertEquals(1, decodedReq.hasLeft)

        val responseJson = """{"success": true, "tournament_id": 5, "player_id": 12, "has_left": 1}"""
        val decodedRes = json.decodeFromString<WithdrawPlayerResponseDto>(responseJson)
        org.junit.Assert.assertTrue(decodedRes.success)
        org.junit.Assert.assertEquals(5, decodedRes.tournamentId)
        org.junit.Assert.assertEquals(12, decodedRes.playerId)
        org.junit.Assert.assertEquals(1, decodedRes.hasLeft)
    }
}
