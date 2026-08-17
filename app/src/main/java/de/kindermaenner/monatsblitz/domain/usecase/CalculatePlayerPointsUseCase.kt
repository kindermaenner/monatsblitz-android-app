package de.kindermaenner.monatsblitz.domain.usecase

import de.kindermaenner.monatsblitz.domain.model.Tournament

class CalculatePlayerPointsUseCase {
    operator fun invoke(tournament: Tournament, playerId: Long): Double {
        return tournament.games.values
            .filter { it.player1Id == playerId }
            .sumOf { it.result.points }
    }
}
