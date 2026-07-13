package de.kindermaenner.monatsblitz.domain.usecase

import de.kindermaenner.monatsblitz.domain.repository.SyncPlayerRepository

class SyncPlayersUseCase(private val playerRepository: SyncPlayerRepository) {
    suspend operator fun invoke() {
        playerRepository.syncPlayers()
    }
}