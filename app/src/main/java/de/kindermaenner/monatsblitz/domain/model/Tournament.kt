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

    fun findGame(player1Id: Long, player2Id: Long, leg : Int): Game? {
        return games.values.firstOrNull { game ->
            ((game.player1Id == player1Id && game.player2Id == player2Id) && game.leg == leg)
        }
    }
}
