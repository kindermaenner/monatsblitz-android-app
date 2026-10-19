package de.kindermaenner.monatsblitz.domain.usecase

import de.kindermaenner.monatsblitz.domain.model.Tournament

class CalculatePlayerPointsUseCase {
    operator fun invoke(tournament: Tournament, playerId: Long): Double {
        val allGames = tournament.games.values
        val asPlayer1Points = allGames
            .filter { it.player1Id == playerId }
            .sumOf { it.result.points }

        val asPlayer2Points = allGames
            .filter { game ->
                game.player2Id == playerId &&
                    allGames.none { other ->
                        other.player1Id == playerId &&
                            other.player2Id == game.player1Id &&
                            other.leg == game.leg
                    }
            }
            .sumOf { it.result.opposite().points }

        return asPlayer1Points + asPlayer2Points
    }
}
