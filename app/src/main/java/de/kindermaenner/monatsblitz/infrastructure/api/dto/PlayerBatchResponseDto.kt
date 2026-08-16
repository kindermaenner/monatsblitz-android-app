package de.kindermaenner.monatsblitz.infrastructure.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PlayerBatchItemDto(
    val success: Boolean,
    @SerialName("player_id")
    val playerId: Int? = null,
    val message: String? = null
)

typealias PlayerBatchResponseDto = BatchResponseDto<PlayerBatchItemDto>
