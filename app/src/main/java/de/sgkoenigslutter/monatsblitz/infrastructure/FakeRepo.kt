package de.sgkoenigslutter.monatsblitz.infrastructure

import de.sgkoenigslutter.monatsblitz.data.model.GameMode
import de.sgkoenigslutter.monatsblitz.data.model.Player
import de.sgkoenigslutter.monatsblitz.data.model.Tournament

object FakeRepo {

    private val players = mutableListOf(
        Player(1, "Max", "Müller"),
        Player(2, "Peter", "Meier"),
        Player(3, "Hans", "Schulz") ,
        Player(4, "Klaus", "Bertram"),
        Player(5, "Fiete", "Klose"),
        Player(6, "Bert", "Brot")
    )

    fun getPlayers(): List<Player> = players

    fun createTournament(
        playerIds: List<Int>,
        mode: GameMode,
        doubleRound: Boolean
    ) : Tournament {
       return Tournament(1, "Monatsblitz", players = playerIds.map{id -> players[id]}, doubleRound = doubleRound)
    }
}