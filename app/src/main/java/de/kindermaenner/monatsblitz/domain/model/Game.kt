package de.kindermaenner.monatsblitz.domain.model

data class Game(val id : Long, val player1Id: Long, val player2Id: Long, val leg: Int, val result: GameResult)
