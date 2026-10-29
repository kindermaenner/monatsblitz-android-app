package de.kindermaenner.monatsblitz.infrastructure.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PlayerDto(
    val id: Long,
    val surname: String,
    val forename: String,
    @SerialName("last_participated")
    val lastParticipated: String? = null
)

@Serializable
data class NewPlayerDto(val forename: String, val surname: String)

@Serializable
data class CreatePlayerResponseDto(
    val success: Boolean,
    @SerialName("player_id")
    val playerId: Long? = null,
    val message: String? = null
)
