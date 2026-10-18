package de.kindermaenner.monatsblitz.infrastructure.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateTournamentResponseDto(
    val success: Boolean,
    @SerialName("tournament_id")
    val tournamentId: Int
)
