package de.kindermaenner.monatsblitz.infrastructure.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class FinalizeDto(val tournamentId : Int)

@Serializable
data class CreateYearPageDto(val year : Int)

@Serializable
data class RecreatePostsDto(val processed : Int,val succeeded : Int, val failed : Int, val errors : List<String>)
