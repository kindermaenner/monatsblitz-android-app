package de.kindermaenner.monatsblitz.infrastructure.api.dto

data class FinalizeDto(val tournamentId : Int)

data class CreateYearPageDto(val year : Int)

data class RecreatePostsDto(val processed : Int,val succeeded : Int, val failed : Int, val errors : List<String>)
