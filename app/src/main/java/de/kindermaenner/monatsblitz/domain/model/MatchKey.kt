package de.kindermaenner.monatsblitz.domain.model

data class MatchKey(val tournamentId : Long, val player1Id: Long, val player2Id: Long, val leg: Leg)
