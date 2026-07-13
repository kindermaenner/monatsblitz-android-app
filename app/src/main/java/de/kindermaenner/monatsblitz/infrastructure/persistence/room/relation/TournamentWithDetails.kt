package de.kindermaenner.monatsblitz.infrastructure.persistence.room.relation

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.GameEntity
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.PlayerEntity
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.TournamentEntity
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.TournamentPlayerCrossRef

data class TournamentWithDetails(

    @Embedded
    val tournament: TournamentEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "tournamentId"
    )
    val games: List<GameEntity>,

    @Relation(
        entity = PlayerEntity::class,
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = TournamentPlayerCrossRef::class,
            parentColumn = "tournamentId",
            entityColumn = "playerId"
        )
    )
    val players: List<PlayerEntity>
)