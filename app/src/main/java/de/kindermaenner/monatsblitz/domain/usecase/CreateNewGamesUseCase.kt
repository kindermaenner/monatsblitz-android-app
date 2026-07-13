package de.kindermaenner.monatsblitz.domain.usecase

import de.kindermaenner.monatsblitz.domain.model.GameResult
import de.kindermaenner.monatsblitz.domain.model.NewGame
import de.kindermaenner.monatsblitz.domain.model.Player

class CreateNewGamesUseCase {
    operator fun invoke(
        players: List<Player>,
        rounds: Int
    ): List<NewGame> {

        val singleRoundGames = players.flatMapIndexed { index, player1 ->
            players.drop(index + 1).map { player2 ->
                NewGame(
                    player1Id = minOf(player1.id, player2.id),
                    player2Id = maxOf(player1.id, player2.id),
                    leg = 1,
                    result = GameResult.Open
                )
            }
        }

        return when (rounds) {
            1 -> singleRoundGames

            2 -> singleRoundGames + singleRoundGames.map {
                it.copy(
                    leg = 2
                )
            }

            else -> throw IllegalArgumentException(
                "Nur 1 oder 2 Runden werden unterstützt"
            )
        }
    }
}