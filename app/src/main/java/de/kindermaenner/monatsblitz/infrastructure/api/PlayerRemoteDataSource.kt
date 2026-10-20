package de.kindermaenner.monatsblitz.infrastructure.api

import de.kindermaenner.monatsblitz.infrastructure.api.dto.NewPlayerDto
import de.kindermaenner.monatsblitz.infrastructure.api.dto.PlayerBatchItemDto
import de.kindermaenner.monatsblitz.infrastructure.api.dto.PlayerDto

class PlayerRemoteDataSource (private val api : MonatsblitzApi) {
    suspend fun getPlayers(): List<PlayerDto> {
        return api.getPlayers()
    }
    suspend fun createPlayer (playerDto : NewPlayerDto) : PlayerBatchItemDto {
        return api.createPlayer(playerDto)
    }
}
