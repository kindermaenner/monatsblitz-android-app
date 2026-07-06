package de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity

import androidx.room.Entity

@Entity(
    tableName = "tournament_players",
    primaryKeys = ["tournamentId", "playerId"]
)
data class TournamentPlayerCrossRef(
    val tournamentId: Int,

    val playerId: Int
)
