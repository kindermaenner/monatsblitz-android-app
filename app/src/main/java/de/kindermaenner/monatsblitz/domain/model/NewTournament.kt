package de.kindermaenner.monatsblitz.domain.model

import java.time.LocalDate

data class NewTournament(
    val mode: GameMode,
    val date: LocalDate,
    val rounds: Int,
    val players: List<Player>,
    val games: List<NewGame>,
    val isLocalOnly: Boolean = false
)
