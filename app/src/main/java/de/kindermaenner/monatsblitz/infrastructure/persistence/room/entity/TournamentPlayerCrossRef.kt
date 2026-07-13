package de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "tournament_player_cross_ref",
    primaryKeys = [
        "tournamentId",
        "playerId"
    ],
    foreignKeys = [
        ForeignKey(
            entity = TournamentEntity::class,
            parentColumns = ["id"],
            childColumns = ["tournamentId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PlayerEntity::class,
            parentColumns = ["id"],
            childColumns = ["playerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("playerId")
    ]
)
data class TournamentPlayerCrossRef(
    val tournamentId: Long,
    val playerId: Long
)
