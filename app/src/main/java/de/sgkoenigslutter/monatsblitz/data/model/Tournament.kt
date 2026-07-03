package de.sgkoenigslutter.monatsblitz.data.model

import java.time.LocalDate

data class Tournament(val Id: Int,
                      val Mode : GameMode,
                      val Date : LocalDate,
                      val players: List<Player>,
                      val doubleRound: Boolean,
                      val games: MutableMap<MatchKey, Game> =  mutableMapOf(),
) {
    fun getPlayer(playerId: Int): Player? {
        return players.find { it.id == playerId }
    }

    fun addGame(game: Game) {
        games[MatchKey(game.player1, game.player2, game.leg)] = game
    }

    fun getGame(player1: Player, player2: Player, leg: Leg = Leg.FIRST): Game? =
        games[MatchKey(player1, player2, leg)]

    fun getResult(player1: Player, player2: Player, leg: Leg = Leg.FIRST): GameResult? =
        getGame(player1, player2, leg)?.result
}

