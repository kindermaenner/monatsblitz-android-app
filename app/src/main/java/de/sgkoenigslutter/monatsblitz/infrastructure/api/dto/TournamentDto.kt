package de.sgkoenigslutter.monatsblitz.infrastructure.api.dto

import de.sgkoenigslutter.monatsblitz.data.model.GameMode
import de.sgkoenigslutter.monatsblitz.data.model.Tournament
import java.time.LocalDate

data class TournamentDto(val id: Int, val year : Int, val month : Int, val day: Int, val mode : String, val round_count : Int, val date_formatted:String) {

}

data class NewTournamentDto(val date : String, val mode : String, val round_count : Int = 1) {
}

fun Tournament.toDto() : TournamentDto {
    return TournamentDto(
        id = Id,
        year = Date.year,
        month = Date.monthValue,
        day = Date.dayOfMonth,
        mode = Mode.name,
        round_count = if (doubleRound) 2 else 1,
        date_formatted = Date.toString()
    )
}

fun TournamentDto.toTournament() : Tournament {
    return Tournament(
        Id = id,
        Mode = GameMode.valueOf(mode),
        Date = LocalDate.of(year, month, day),
        players = emptyList(),
        doubleRound = round_count == 2,
        results = mutableMapOf()
    )
}

fun Tournament.toNewDto() : NewTournamentDto {
    return NewTournamentDto(
        date = Date.toString(),
        mode = Mode.name,
        round_count = if (doubleRound) 2 else 1
    )
}
