package de.kindermaenner.monatsblitz.domain.model

import java.time.LocalDate

data class Tournament(val Id: Long,
                      val Mode : GameMode,
                      val Date : LocalDate,
                      val rounds: Int,
                      val players : List<Player>,
                      val games: Map<Long, Game> = mapOf()
)  {
    val playerIds: List<Long>
        get() = players.map { it.id }
}