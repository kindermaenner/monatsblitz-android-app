package de.kindermaenner.monatsblitz.domain.usecase

import de.kindermaenner.monatsblitz.domain.repository.TournamentRepository

class SyncGameResultsUseCase(
    private val tournamentRepository: TournamentRepository
) {
    suspend operator fun invoke() {
        tournamentRepository.syncDirtyGames()
    }
}