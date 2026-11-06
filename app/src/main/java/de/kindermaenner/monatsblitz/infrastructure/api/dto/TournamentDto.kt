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
    val status: String = "open",
    @SerialName("daily_number")
    val dailyNumber: Int = 1,
    val label: String = "",
    @SerialName("counts_for_ranking")
    val countsForRanking: Boolean = true,
    @SerialName("start_time")
    val startTime: String? = null,
    @SerialName("player_ids")
    val playerIds: List<Long> = emptyList(),
    @SerialName("withdrawn_player_ids")
    val withdrawnPlayerIds: List<Long> = emptyList()
)

@Serializable
data class NewTournamentDto(
    val date: String,
    val mode: String,
    @SerialName("round_count")
    val roundCount: Int = 1,
    @SerialName("daily_number")
    val dailyNumber: Int? = null,
    val label: String = "",
    @SerialName("counts_for_ranking")
    val countsForRanking: Boolean = true,
    @SerialName("confirm_parallel")
    val confirmParallel: Boolean = false,
    @SerialName("player_ids")
    val playerIds: List<Long> = emptyList()
)
