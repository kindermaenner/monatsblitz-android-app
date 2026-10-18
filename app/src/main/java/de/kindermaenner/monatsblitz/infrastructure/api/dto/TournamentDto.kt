package de.kindermaenner.monatsblitz.infrastructure.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class TournamentDto(
    val id: Int,
    val date: String,           // ISO-8601 YYYY-MM-DD (per spec)
    val mode: String,
    val round_count: Int,
    val status: String = "open"
)

@Serializable
data class NewTournamentDto(
    val date: String,
    val mode: String,
    val round_count: Int = 1
)
