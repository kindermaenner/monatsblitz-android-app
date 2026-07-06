package de.kindermaenner.monatsblitz.infrastructure.api.dto

import de.kindermaenner.monatsblitz.domain.model.Game
import de.kindermaenner.monatsblitz.domain.model.GameResult
import de.kindermaenner.monatsblitz.domain.model.Leg
import de.kindermaenner.monatsblitz.domain.model.Tournament
import java.util.Locale

fun String.toGameResult(): GameResult {
    val s = trim().replace(',', '.').lowercase(Locale.getDefault())
    return when (s) {
        "1-0", "1:0" -> GameResult.Win
        "0-1", "0:1" -> GameResult.Loss
        "0.5-0.5", "1/2-1/2", "0.5:0.5" -> GameResult.Remis
        "+:-" -> GameResult.ForfeitWin
        "-:+" -> GameResult.ForfeitLoss
        else -> GameResult.Open
    }
}

fun GameResult.toResultString(): String = when (this) {
    GameResult.Win -> "1-0"
    GameResult.Loss -> "0-1"
    GameResult.Remis -> "0.5-0.5"
    GameResult.ForfeitWin -> "+:-"
    GameResult.ForfeitLoss -> "-:+"
    GameResult.Open -> " "
}

// Domain <-> DTO for Game (requires tournament context to resolve players)
fun GameDto.toGame(tournament: Tournament): Game? {
    val legEnum = if (leg == 2) Leg.SECOND else Leg.FIRST
    return Game(tournament.Id, leg = legEnum,  player1Id = player1_id, player2Id  = player2_id, result = result.toGameResult())
}

fun Game.toDto(): GameDto =
    GameDto(
        leg = if (leg == Leg.SECOND) 2 else 1,
        player1_id = player1Id,
        player2_id = player2Id,
        result = result.toResultString()
    )
