package de.kindermaenner.monatsblitz.infrastructure.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateTournamentResponseDto(
    val success: Boolean,
    val tournament_id: Int
)
