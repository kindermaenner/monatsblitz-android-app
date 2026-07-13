package de.kindermaenner.monatsblitz.infrastructure.api

import de.kindermaenner.monatsblitz.infrastructure.api.dto.NewPlayerDto
import de.kindermaenner.monatsblitz.infrastructure.api.dto.PlayerDto

class PlayerRemoteDataSource (private val api : MonatsblitzApi) {
    suspend fun getPlayers(): List<PlayerDto> {
        return api.getPlayers()
    }
    suspend fun createPlayer (playerDto : NewPlayerDto) : PlayerDto {
        return api.createPlayer(playerDto)
    }
}