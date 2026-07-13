package de.kindermaenner.monatsblitz.domain.model

data class NewGame(val player1Id: Long, val player2Id: Long, val leg: Int, val result: GameResult = GameResult.Open)
