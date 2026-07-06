package de.kindermaenner.monatsblitz.infrastructure.api.dto

import kotlinx.serialization.Serializable



@Serializable
data class GameDto(val leg: Int, val player1_id: Int, val player2_id: Int, val result: String)

@Serializable
data class CreateGameDto(val tournamentId : Int, val game: GameDto)

@Serializable
data class CreateGamesDto(val tournamentId : Int, val games: List<GameDto>)