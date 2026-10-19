package de.kindermaenner.monatsblitz.domain.usecase

import de.kindermaenner.monatsblitz.domain.repository.PlayerRepository

class SyncPlayersUseCase(
    private val playerRepository: PlayerRepository
) {
    suspend operator fun invoke() {
        playerRepository.syncPlayers()
    }
}