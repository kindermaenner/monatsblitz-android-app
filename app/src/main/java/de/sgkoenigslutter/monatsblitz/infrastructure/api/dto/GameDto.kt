package de.sgkoenigslutter.monatsblitz.infrastructure.api.dto

import de.sgkoenigslutter.monatsblitz.data.model.Game
import de.sgkoenigslutter.monatsblitz.data.model.GameResult
import de.sgkoenigslutter.monatsblitz.data.model.Player
import de.sgkoenigslutter.monatsblitz.data.model.Tournament
import kotlinx.serialization.Serializable
import kotlin.Int

// Game(val leg: Leg, val player1: Player, val player2: Player, val result: GameResult)

@Serializable
data class GameDto(val leg: Int, val player1_id: Int, val player2_id: Int, val result: String)

data class CreateGameDto(val tournament_id : Int, val game: GameDto)

data class CreateGamesDto(val tournament_id : Int, val games: List<GameDto>)