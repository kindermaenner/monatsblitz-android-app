package de.sgkoenigslutter.monatsblitz.infrastructure.api.dto

import de.sgkoenigslutter.monatsblitz.data.model.GameMode
import de.sgkoenigslutter.monatsblitz.data.model.Tournament
import de.sgkoenigslutter.monatsblitz.data.model.Player
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class TournamentDto(
    val id: Int,
    val date: String,           // ISO-8601 YYYY-MM-DD (per spec)
    val mode: String,
    val round_count: Int,
    val date_formatted: String? = null
)

@Serializable
data class NewTournamentDto(
    val date: String,
    val mode: String,
    val round_count: Int = 1
)

fun Tournament.toDto(): TournamentDto =
    TournamentDto(
        id = Id,
        date = Date.toString(),
        mode = Mode.name,
        round_count = if (doubleRound) 2 else 1,
        date_formatted = Date.toString()
    )

fun TournamentDto.toTournament(players: List<Player> = emptyList()): Tournament =
    Tournament(
        Id = id,
        Mode = GameMode.valueOf(mode),
        Date = LocalDate.parse(date),
        players = players,
        doubleRound = round_count == 2,
        games = mutableMapOf()
    )

