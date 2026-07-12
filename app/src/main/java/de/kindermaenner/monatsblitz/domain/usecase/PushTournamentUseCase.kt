package de.kindermaenner.monatsblitz.domain.usecase

import de.kindermaenner.monatsblitz.domain.repository.GameRepository
import de.kindermaenner.monatsblitz.domain.repository.PlayerRepository
import de.kindermaenner.monatsblitz.domain.repository.TournamentRepository

class PushTournamentUseCase(
    private val tournamentRepository: TournamentRepository,
    private val playerRepository: PlayerRepository,
    private val gameRepository: GameRepository
) {
    suspend operator fun invoke(localTournamentId: Long) {

    }
}