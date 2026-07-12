package de.kindermaenner.monatsblitz.infrastructure.api.dto

import de.kindermaenner.monatsblitz.domain.model.Game
import de.kindermaenner.monatsblitz.domain.model.GameMode
import de.kindermaenner.monatsblitz.domain.model.GameResult
import de.kindermaenner.monatsblitz.domain.model.Leg
import de.kindermaenner.monatsblitz.domain.model.NewPlayer
import de.kindermaenner.monatsblitz.domain.model.NewTournament
import de.kindermaenner.monatsblitz.domain.model.Player
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
fun GameDto.toGame(tournamentId : Long, player1Id : Long, player2Id: Long): Game {
    val legEnum = if (leg == 2) Leg.SECOND else Leg.FIRST
    return Game(tournamentId, leg = legEnum,  player1Id = player1Id, player2Id  = player2Id, result = result.toGameResult())
}

fun Game.toDto(player1Id : Int, player2Id : Int): GameDto =
    GameDto(
        leg = if (leg == Leg.SECOND) 2 else 1,
        player1Id = player1Id,
        player2Id = player2Id,
        result = result.toResultString()
    )

fun NewPlayer.toDto(): NewPlayerDto = NewPlayerDto(
    forename = Vorname,
    surname = Name
)

fun PlayerDto.toDomain(): Player = Player(id = 0, Name = surname, Vorname = forename)

fun NewTournament.toDto() : NewTournamentDto = NewTournamentDto(
    date = Date.toString(),
    mode = Mode.displayName,
    round_count = if (doubleRound) 2 else 1
)
