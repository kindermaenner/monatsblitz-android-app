package de.kindermaenner.monatsblitz.domain.model

data class Game(val tournamentId : Long, val leg: Leg, val player1Id: Long, val player2Id: Long, val result: GameResult) {
}
