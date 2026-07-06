package de.kindermaenner.monatsblitz.domain.model

data class Game(val tournamentId : Int, val leg: Leg, val player1Id: Int, val player2Id: Int, val result: GameResult) {
}
