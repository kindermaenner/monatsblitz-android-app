package de.kindermaenner.monatsblitz.infrastructure.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GameDtoData(
    @SerialName("game_id")
    val gameId: Int? = null,
    @SerialName("leg_type")
    val leg: Int,
    @SerialName("player1_id")
    val player1Id: Int,
    @SerialName("player2_id")
    val player2Id: Int,
    val result: String
)

@Serializable
data class CreateGameDto(
    @SerialName("tournament_id")
    val tournamentId: Int,
    @SerialName("leg_type")
    val leg: Int,
    @SerialName("player1_id")
    val player1Id: Int,
    @SerialName("player2_id")
    val player2Id: Int,
    val result: String
)

@Serializable
data class UpdateGameDto(
    @SerialName("game_id")
    val id: Int,
    @SerialName("tournament_id")
    val tournamentId: Int,
    @SerialName("leg_type")
    val leg: Int,
    @SerialName("player1_id")
    val player1Id: Int,
    @SerialName("player2_id")
    val player2Id: Int,
    val result: String
)

@Serializable
data class GameDto(
    val id: Int = 0,
    @SerialName("game_id")
    val gameId: Int = 0,
    @SerialName("tournament_id")
    val tournamentId: Int = 0,
    @SerialName("leg_type")
    val leg: Int = 1,
    @SerialName("player1_id")
    val player1Id: Int,
    @SerialName("player2_id")
    val player2Id: Int,
    val result: String
) {
    val resolvedId: Int
        get() = if (id != 0) id else gameId
}

@Serializable
data class CreateGamesDto(
    @SerialName("tournament_id")
    val tournamentId: Int,
    val games: List<GameDtoData>
)

@Serializable
data class GameSyncResponseItemDto(
    val success: Boolean,
    @SerialName("game_id")
    val gameId: Int
)

@Serializable
data class CreateGamesResponseDto(
    val success: Boolean,
    val count: Int,
    val items: List<GameSyncResponseItemDto>
)
