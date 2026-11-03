package de.kindermaenner.monatsblitz.infrastructure.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AddTournamentPlayerDto(
    @SerialName("tournament_id")
    val tournamentId: Int,
    @SerialName("player_id")
    val playerId: Int
)

@Serializable
data class AddTournamentPlayerResponseDto(
    val success: Boolean,
    @SerialName("tournament_id")
    val tournamentId: Int? = null,
    @SerialName("player_id")
    val playerId: Int? = null
)
