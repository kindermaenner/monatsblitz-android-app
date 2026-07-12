package de.kindermaenner.monatsblitz.infrastructure.api.dto

import de.kindermaenner.monatsblitz.domain.model.GameMode
import de.kindermaenner.monatsblitz.domain.model.Player
import de.kindermaenner.monatsblitz.domain.model.Tournament
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class TournamentDto(
    val id: Int,
    val date: String,           // ISO-8601 YYYY-MM-DD (per spec)
    val mode: String,
    val round_count: Int,
    val date_formatted: String? = null
)

@Serializable
data class NewTournamentDto(
    val date: String,
    val mode: String,
    val round_count: Int = 1
)
