package de.kindermaenner.monatsblitz.infrastructure.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GameDtoData(val leg: Int, val player1Id: Int, val player2Id: Int, val result: String)


@Serializable
data class CreateGameDto(val tournamentId : Int, val leg: Int, val player1Id: Int, val player2Id: Int, val result: String)

@Serializable
data class UpdateGameDto(val id : Int, val tournamentId : Int, val leg: Int, val player1Id: Int, val player2Id: Int, val result: String)

@Serializable
data class GameDto(val id : Int, val tournamentId : Int, val leg: Int, val player1Id: Int, val player2Id: Int, val result: String)



@Serializable
data class CreateGamesDto(val tournamentId : Int, val games: List<GameDtoData>)

@Serializable
data class GameSyncResponseItemDto(
    val success: Boolean,
    @SerialName("game_id")
    val gameId: Int
)

@Serializable
data class CreateGamesResponseDto(val success: Boolean, val count : Int, val items: List<GameSyncResponseItemDto>)
