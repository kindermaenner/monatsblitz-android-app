package de.kindermaenner.monatsblitz.infrastructure.api.dto

import kotlinx.serialization.Serializable

// Game(val leg: Leg, val player1: Player, val player2: Player, val result: GameResult)

@Serializable
data class GameDto(val leg: Int, val player1_id: Int, val player2_id: Int, val result: String)

data class CreateGameDto(val tournament_id : Int, val game: GameDto)

data class CreateGamesDto(val tournament_id : Int, val games: List<GameDto>)