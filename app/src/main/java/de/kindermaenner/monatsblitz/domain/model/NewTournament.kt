package de.kindermaenner.monatsblitz.domain.model

import java.time.LocalDate

data class NewTournament( val Mode : GameMode,
                          val Date : LocalDate,
                          val rounds: Int,
                          val players : List<Player>,
                          val games: List<NewGame>)
