package de.kindermaenner.monatsblitz.domain.model

import java.time.LocalDate

data class NewTournament( val Mode : GameMode,
                          val Date : LocalDate,
                          val playerIds: List<Int>,
                          val doubleRound: Boolean)
