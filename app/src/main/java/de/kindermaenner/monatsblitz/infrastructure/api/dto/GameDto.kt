package de.kindermaenner.monatsblitz.infrastructure.api.dto

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