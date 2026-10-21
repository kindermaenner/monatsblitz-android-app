package de.kindermaenner.monatsblitz.infrastructure.api

import de.kindermaenner.monatsblitz.infrastructure.api.dto.CreateModeResponseDto
import de.kindermaenner.monatsblitz.infrastructure.api.dto.ModeDto
import de.kindermaenner.monatsblitz.infrastructure.api.dto.NewModeDto

class ModeRemoteDataSource(private val api: MonatsblitzApi) {
    suspend fun getModes(): List<ModeDto> = api.getModes()

    suspend fun createMode(modeDto: NewModeDto): CreateModeResponseDto = api.createMode(modeDto)
}
