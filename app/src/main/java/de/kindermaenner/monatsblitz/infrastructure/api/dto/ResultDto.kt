package de.kindermaenner.monatsblitz.infrastructure.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ResultDto(
    @SerialName("player_id") val playerId: Int,
    val points: Double,
    val rank: Int
)

@Serializable
data class CreateResultDto(
    @SerialName("tournament_id") val tournamentId: Int,
    val result: ResultDto
)

@Serializable
data class CreateResultsDto(
    @SerialName("tournament_id") val tournamentId: Int,
    val results: List<ResultDto>
)

@Serializable
data class ResultBatchItemDto(
    val success: Boolean = true,
    @SerialName("result_id") val resultId: Int? = null,
    @SerialName("player_id") val playerId: Int? = null,
    val points: Double? = null,
    val rank: Int? = null,
    val message: String? = null
)

typealias ResultBatchResponseDto = BatchResponseDto<ResultBatchItemDto>
