package de.kindermaenner.monatsblitz.domain.model

import java.time.LocalDate

data class Tournament(
    val id: Long,
    val mode: GameMode,
    val date: LocalDate,
    val rounds: Int,
    val players: List<Player>,
    val games: Map<Long, Game> = mapOf()
) {
    val playerIds: List<Long>
        get() = players.map { it.id }

    fun findGame(player1Id: Long, player2Id: Long, leg : Int): Game? {
        val directMatch = games.values.firstOrNull { game ->
            game.player1Id == player1Id && game.player2Id == player2Id && game.leg == leg
        }
        if (directMatch != null) return directMatch

        val reverseMatch = games.values.firstOrNull { game ->
            game.player1Id == player2Id && game.player2Id == player1Id && game.leg == leg
        }
        return reverseMatch?.copy(
            player1Id = player1Id,
            player2Id = player2Id,
            result = reverseMatch.result.opposite()
        )
    }
}
