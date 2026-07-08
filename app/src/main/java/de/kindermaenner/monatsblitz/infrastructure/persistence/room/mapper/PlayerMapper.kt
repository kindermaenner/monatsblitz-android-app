package de.kindermaenner.monatsblitz.infrastructure.persistence.room.mapper

import de.kindermaenner.monatsblitz.domain.model.Player
import de.kindermaenner.monatsblitz.infrastructure.api.dto.PlayerDto
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.PlayerEntity

object PlayerMapper {

    fun toDomain(entity: PlayerEntity): Player =
        Player(
            id = entity.id,
            Name = entity.name,
            Vorname = entity.vorname
        )

    fun toEntity(dto: PlayerDto): PlayerEntity =
        PlayerEntity(
            id = dto.id,
            name = dto.surname,
            vorname = dto.forename
        )

    fun toEntity(domain: Player): PlayerEntity =
        PlayerEntity(
            id = domain.id,
            name = domain.Name,
            vorname = domain.Vorname
        )
}