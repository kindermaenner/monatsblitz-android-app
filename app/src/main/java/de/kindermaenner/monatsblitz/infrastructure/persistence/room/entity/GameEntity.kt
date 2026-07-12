package de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import de.kindermaenner.monatsblitz.domain.model.GameResult
import de.kindermaenner.monatsblitz.domain.model.Leg

@Entity(
    tableName = "games",
    primaryKeys = [
        "tournamentId",
        "player1Id",
        "player2Id",
        "leg"
    ],
    foreignKeys = [
        ForeignKey(
            entity = TournamentEntity::class,
            parentColumns = ["id"],
            childColumns = ["tournamentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("tournamentId")
    ]
)
data class GameEntity(
    val tournamentId: Long,
    val player1Id: Long,
    val player2Id: Long,
    val leg: Leg,
    val result: GameResult,
    val dirty : Boolean = true
)