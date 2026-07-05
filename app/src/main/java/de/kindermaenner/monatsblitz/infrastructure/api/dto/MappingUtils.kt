package de.kindermaenner.monatsblitz.infrastructure.api.dto

import de.kindermaenner.monatsblitz.data.model.Game
import de.kindermaenner.monatsblitz.data.model.GameResult
import de.kindermaenner.monatsblitz.data.model.Leg
import de.kindermaenner.monatsblitz.data.model.Tournament
import java.util.Locale

fun String.toGameResultOrNull(): GameResult? {
    val s = trim().replace(',', '.').lowercase(Locale.getDefault())
    return when (s) {
        "1-0", "1:0" -> GameResult.Win
        "0-1", "0:1" -> GameResult.Loss
        "0.5-0.5", "1/2-1/2", "0.5:0.5" -> GameResult.Remis
        else -> null
    }
}

fun GameResult.toResultString(): String = when (this) {
    GameResult.Win -> "1-0"
    GameResult.Loss -> "0-1"
    GameResult.Remis -> "0.5-0.5"
}

// Domain <-> DTO for Game (requires tournament context to resolve players)
fun GameDto.toGame(tournament: Tournament): Game? {
    val p1 = tournament.getPlayer(player1_id) ?: return null
    val p2 = tournament.getPlayer(player2_id) ?: return null
    val legEnum = if (leg == 2) Leg.SECOND else Leg.FIRST
    val r = result.toGameResultOrNull() ?: return null
    return Game(leg = legEnum, player1 = p1, player2 = p2, result = r)
}

fun Game.toDto(): GameDto =
    GameDto(
        leg = if (leg == Leg.SECOND) 2 else 1,
        player1_id = player1.id,
        player2_id = player2.id,
        result = result.toResultString()
    )
