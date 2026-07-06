package de.kindermaenner.monatsblitz.domain.model

data class MatchKey(val tournamentId : Int, val player1Id: Int, val player2Id: Int, val leg: Leg)
