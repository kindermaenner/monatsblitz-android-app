package de.kindermaenner.monatsblitz.infrastructure.api.dto

import de.kindermaenner.monatsblitz.domain.model.GameMode
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.ModeEntity
import kotlinx.serialization.Serializable

@Serializable
data class ModeDto(
    val id: Long,
    val name: String
)

@Serializable
data class NewModeDto(
    val name: String
)

@Serializable
data class CreateModeResponseDto(
    val success: Boolean = true,
    val id: Long? = null,
    val name: String? = null,
    val message: String? = null
)

fun ModeDto.toEntity(): ModeEntity = ModeEntity(
    remoteId = id,
    name = name,
    dirty = false
)

fun ModeEntity.toDomain(): GameMode = GameMode(
    id = id,
    remoteId = remoteId,
    name = name,
    dirty = dirty
)

fun GameMode.toEntity(): ModeEntity = ModeEntity(
    id = id,
    remoteId = remoteId,
    name = name,
    dirty = dirty
)
