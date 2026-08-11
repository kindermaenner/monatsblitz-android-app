package de.kindermaenner.monatsblitz.domain.model

data class PlayerRankingEntry(val playerId: Long, val tournamentId: Long, val points : Double, val rank : Int = 0)
