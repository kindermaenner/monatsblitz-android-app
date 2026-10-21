package de.kindermaenner.monatsblitz.infrastructure.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WithdrawPlayerDto(
    @SerialName("tournament_id")
    val tournamentId: Int,
    @SerialName("player_id")
    val playerId: Int,
    @SerialName("has_left")
    val hasLeft: Int = 1
)

@Serializable
data class WithdrawPlayerResponseDto(
    val success: Boolean,
    @SerialName("tournament_id")
    val tournamentId: Int,
    @SerialName("player_id")
    val playerId: Int,
    @SerialName("has_left")
    val hasLeft: Int
)
