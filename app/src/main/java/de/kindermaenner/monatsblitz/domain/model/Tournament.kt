package de.kindermaenner.monatsblitz.domain.model

import java.time.LocalDate

data class Tournament(val Id: Long,
                      val Mode : GameMode,
                      val Date : LocalDate,
                      val players: List<Player>,
                      val doubleRound: Boolean,
                      val games: MutableMap<MatchKey, Game> =  mutableMapOf()
) {
    fun getPlayer(playerId: Long): Player? {
        return players.find { it.id == playerId }
    }

    fun addGame(game: Game) {
        games[MatchKey(Id, game.player1Id, game.player2Id, leg = game.leg)] = game
    }

    fun getGame(player1: Player, player2: Player, leg: Leg): Game? =
        games[MatchKey(Id,player1.id, player2.id, leg)]

    fun getGame(player1Id: Long, player2Id: Long, leg: Leg): Game? =
        games[MatchKey(Id, player1Id, player2Id, leg)]

    fun getResult(player1Id: Long, player2Id: Long, leg: Leg): GameResult? =
        getGame(player1Id, player2Id, leg)?.result

    fun getResult(player1: Player, player2: Player, leg: Leg): GameResult? =
        getGame(player1, player2, leg)?.result
}

