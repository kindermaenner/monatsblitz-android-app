package de.kindermaenner.monatsblitz.infrastructure.persistence.room.mapper

import de.kindermaenner.monatsblitz.domain.model.NewPlayer
import de.kindermaenner.monatsblitz.domain.model.Player
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.PlayerEntity

fun PlayerEntity.toDomain(): Player =
    Player(
        id = id,
        Name = name,
        Vorname = vorname,
        displaySuffix = displaySuffix
    )

fun Player.toEntity(): PlayerEntity =
    PlayerEntity(
        id = id,
        name = Name,
        vorname = Vorname,
        displaySuffix = displaySuffix,
        remoteId = null,
        dirty = true
    )

fun NewPlayer.toEntity() :PlayerEntity =
    PlayerEntity(
        name = Name,
        vorname = Vorname,
        displaySuffix = displaySuffix,
        remoteId = null,
        dirty = true
    )
