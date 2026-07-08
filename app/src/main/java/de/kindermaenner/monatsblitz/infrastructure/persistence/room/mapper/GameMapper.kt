package de.kindermaenner.monatsblitz.infrastructure.persistence.room.mapper

import de.kindermaenner.monatsblitz.domain.model.Game
import de.kindermaenner.monatsblitz.domain.model.Leg
import de.kindermaenner.monatsblitz.infrastructure.api.dto.GameDto
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.GameEntity

object GameMapper {

    fun toDomain(entity: GameEntity): Game =
        Game(
            tournamentId = entity.tournamentId,
            player1Id = entity.player1Id,
            player2Id = entity.player2Id,
            leg = entity.leg,
            result = entity.result
        )

    fun toEntity(domain: Game): GameEntity =
        GameEntity(
            tournamentId = domain.tournamentId,
            player1Id = domain.player1Id,
            player2Id = domain.player2Id,
            leg = domain.leg,
            result = domain.result
        )

    fun toDto(domain: Game): GameDto =
        GameDto(
            player1_id = domain.player1Id,
            player2_id = domain.player2Id,
            leg = if (domain.leg == Leg.SECOND) 2 else 1,
            result = domain.result.displayName
        )
}