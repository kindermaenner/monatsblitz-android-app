package de.kindermaenner.monatsblitz.infrastructure.api.dto
import kotlinx.serialization.Serializable

@Serializable
data class ResultDto(val player_id : Int, val points : Int, val rank : Int) {
    companion object {
    }
}

@Serializable
data class CreateResultDto(val tournament_id : Int, val result: ResultDto)

@Serializable
data class CreateResultsDto(val tournament_id : Int, val results: List<ResultDto>)