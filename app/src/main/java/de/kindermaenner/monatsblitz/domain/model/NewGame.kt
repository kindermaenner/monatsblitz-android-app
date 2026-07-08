package de.kindermaenner.monatsblitz.domain.model

data class NewGame(val tournamentId : Int, val leg: Leg, val player1Id: Int, val player2Id: Int)
