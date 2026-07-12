package de.kindermaenner.monatsblitz.infrastructure.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class GameDto(val leg: Int, val player1Id: Int, val player2Id: Int, val result: String)

@Serializable
data class CreateGameDto(val tournamentId : Int, val game: GameDto)

@Serializable
data class CreateGamesDto(val tournamentId : Int, val games: List<GameDto>)