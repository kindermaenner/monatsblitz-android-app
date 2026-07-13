package de.kindermaenner.monatsblitz.infrastructure.api.dto

import de.kindermaenner.monatsblitz.domain.model.Game
import de.kindermaenner.monatsblitz.domain.model.GameResult
import de.kindermaenner.monatsblitz.domain.model.NewPlayer
import de.kindermaenner.monatsblitz.domain.model.NewTournament
import de.kindermaenner.monatsblitz.domain.model.Player
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.PlayerEntity
import java.util.Locale

fun String.toGameResult(): GameResult {
    val s = trim().replace(',', '.').lowercase(Locale.getDefault())
    return when (s) {
        "1:0" -> GameResult.Win
        "0:1" -> GameResult.Loss
        "0.5:0.5" -> GameResult.Remis
        "+:-" -> GameResult.ForfeitWin
        "-:+" -> GameResult.ForfeitLoss
        else -> GameResult.Open
    }
}

fun GameResult.toResultString(): String = when (this) {
    GameResult.Win -> "1:0"
    GameResult.Loss -> "0:1"
    GameResult.Remis -> "0.5:0.5"
    GameResult.ForfeitWin -> "+:-"
    GameResult.ForfeitLoss -> "-:+"
    GameResult.Open -> "offen"
}

fun NewPlayer.toDto(): NewPlayerDto = NewPlayerDto(
    forename = Vorname,
    surname = Name
)

fun NewTournament.toDto() : NewTournamentDto = NewTournamentDto(
    date = Date.toString(),
    mode = Mode.displayName,
    round_count = rounds
)

fun PlayerEntity.toNewPlayerDto() : NewPlayerDto =
    NewPlayerDto(
        forename = vorname,
        surname = name
    )

fun  PlayerDto.toEntity() : PlayerEntity =
    PlayerEntity(
        remoteId = id.toLong(),
        name = surname,
        vorname = forename,
        dirty = false
    )