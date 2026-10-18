package de.kindermaenner.monatsblitz.infrastructure.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TournamentDto(
    val id: Int,
    val date: String,           // ISO-8601 YYYY-MM-DD (per spec)
    val mode: String,
    @SerialName("round_count")
    val roundCount: Int,
    val status: String = "open"
)

@Serializable
data class NewTournamentDto(
    val date: String,
    val mode: String,
    @SerialName("round_count")
    val roundCount: Int = 1
)
