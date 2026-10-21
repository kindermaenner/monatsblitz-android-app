package de.kindermaenner.monatsblitz.infrastructure.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FinalizeRequestDto(
    @SerialName("tournament_id")
    val tournamentId: Int,
    @SerialName("withdrawn_player_ids")
    val withdrawnPlayerIds: List<Int>? = null
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
data class RecreatePostsDto(val processed : Int,val succeeded : Int, val failed : Int, val errors : List<String>)
