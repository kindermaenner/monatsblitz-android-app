package de.kindermaenner.monatsblitz.infrastructure.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FinalizeRequestDto(
    @SerialName("tournament_id")
    val tournamentId: Int,
    @SerialName("withdrawn_player_ids")
    val withdrawnPlayerIds: List<Int>? = null,
    @SerialName("round_count")
    val roundCount: Int? = null
)

typealias FinalizeDto = FinalizeRequestDto

@Serializable
data class FinalizeResponseDto(
    val success: Boolean,
    @SerialName("tournament_id")
    val tournamentId: Int? = null,
    @SerialName("post_id")
    val postId: Int? = null,
    @SerialName("post_updated")
    val postUpdated: Boolean? = null,
    val published: Boolean? = null
)

@Serializable
data class CreateYearPageDto(val year : Int)

@Serializable
data class RecreatePostsDto(
    val processed: Int = 0,
    val succeeded: Int = 0,
    val failed: Int = 0,
    val errors: List<String> = emptyList(),
    val success: Boolean = true
)

@Serializable
data class ReopenTournamentRequestDto(
    @SerialName("tournament_id")
    val tournamentId: Long
)

@Serializable
data class ReopenTournamentResponseDto(
    val success: Boolean = false,
    @SerialName("tournament_id")
    val tournamentId: Long = 0,
    val status: String = "",
    @SerialName("already_open")
    val alreadyOpen: Boolean = false,
    val message: String = ""
)

@Serializable
data class VerifyAuthResponseDto(
    val valid: Boolean = false,
    val role: String = "",
    @SerialName("api_version")
    val apiVersion: Int = 0,
    val capabilities: List<String> = emptyList()
)

