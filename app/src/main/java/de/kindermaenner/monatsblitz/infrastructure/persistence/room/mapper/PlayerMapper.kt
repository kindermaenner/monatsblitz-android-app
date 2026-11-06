package de.kindermaenner.monatsblitz.infrastructure.persistence.room.mapper

import de.kindermaenner.monatsblitz.domain.model.NewPlayer
import de.kindermaenner.monatsblitz.domain.model.Player
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.PlayerEntity

fun PlayerEntity.toDomain(): Player =
    Player(
        id = id,
        name = name,
        vorname = vorname,
        rufname = rufname.takeIf { it.isNotBlank() },
        dirty = dirty,
        isLocal = isLocal,
        lastParticipated = lastParticipated
    )

fun Player.toEntity(): PlayerEntity =
    PlayerEntity(
        id = id,
        name = name,
        vorname = vorname,
        rufname = rufname ?: "",
        remoteId = null,
        dirty = !isLocal,
        isLocal = isLocal,
        lastParticipated = lastParticipated
    )

fun NewPlayer.toEntity(): PlayerEntity =
    PlayerEntity(
        name = name,
        vorname = vorname,
        rufname = rufname ?: "",
        remoteId = null,
        dirty = !isLocal,
        isLocal = isLocal,
        lastParticipated = null
    )
