package de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import de.kindermaenner.monatsblitz.domain.model.GameResult

@Entity(
    tableName = "games",
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
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val remoteId : Long? = null,
    val tournamentId: Long,
    val player1Id: Long,
    val player2Id: Long,
    val leg: Int,
    val result: GameResult,
    val dirty : Boolean = true
)