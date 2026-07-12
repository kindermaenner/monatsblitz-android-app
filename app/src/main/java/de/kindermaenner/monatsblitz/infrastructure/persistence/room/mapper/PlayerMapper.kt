package de.kindermaenner.monatsblitz.infrastructure.persistence.room.mapper

import de.kindermaenner.monatsblitz.domain.model.NewPlayer
import de.kindermaenner.monatsblitz.domain.model.Player
import de.kindermaenner.monatsblitz.infrastructure.api.dto.NewPlayerDto
import de.kindermaenner.monatsblitz.infrastructure.api.dto.PlayerDto
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.PlayerEntity

object PlayerMapper {

    fun toDomain(entity: PlayerEntity): Player =
        Player(
            id = entity.id,
            Name = entity.name,
            Vorname = entity.vorname
        )

    fun toEntity(domain: Player, remoteId : Int? = null): PlayerEntity =
        PlayerEntity(
            id = domain.id,
            name = domain.Name,
            vorname = domain.Vorname,
            remoteId = remoteId,
            dirty = remoteId != null
        )
    fun toEntity(domain: NewPlayer, remoteId : Int? = null): PlayerEntity =
        PlayerEntity(
            name = domain.Name,
            vorname = domain.Vorname,
            remoteId = remoteId,
            dirty = remoteId != null
        )

    fun toEntity(domain: PlayerDto, remoteId : Int? = null, localId : Long = 0): PlayerEntity =
        PlayerEntity(
            id = localId,
            name = domain.surname,
            vorname = domain.forename,
            remoteId = remoteId,
            dirty = remoteId != null
        )

    fun toDto(entity : PlayerEntity) : NewPlayerDto =
        NewPlayerDto(
            surname = entity.name,
            forename = entity.vorname
        )

}