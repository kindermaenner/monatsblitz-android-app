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
        val dto = NewTournamentDto(date = "2026-08-09", mode = "3+2", round_count = 2)
        val result = json.encodeToString(dto)

        assertTrue("Should contain date", result.contains("\"date\":\"2026-08-09\""))
        assertTrue("Should contain mode", result.contains("\"mode\":\"3+2\""))
        assertTrue("Should contain round_count", result.contains("\"round_count\":2"))
    }

    @Test
    fun `ResultDto serializes points as double for remis`() {
        val dto = ResultDto(player_id = 1, points = 0.5, rank = 2)
        val result = json.encodeToString(dto)

        assertTrue("Should contain points as 0.5", result.contains("\"points\":0.5"))
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
        assertTrue("First item should have id 301", response.items[0].playerId == 301)
    }
}

