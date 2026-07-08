package de.kindermaenner.monatsblitz.infrastructure.repository

import de.kindermaenner.monatsblitz.domain.model.Game
import de.kindermaenner.monatsblitz.domain.model.GameResult
import de.kindermaenner.monatsblitz.domain.model.Leg
import de.kindermaenner.monatsblitz.domain.model.NewGame
import de.kindermaenner.monatsblitz.domain.repository.GameRepository
import de.kindermaenner.monatsblitz.infrastructure.api.MonatsblitzApi
import de.kindermaenner.monatsblitz.infrastructure.api.dto.CreateGameDto
import de.kindermaenner.monatsblitz.infrastructure.api.dto.GameDto
import de.kindermaenner.monatsblitz.infrastructure.api.dto.NewPlayerDto
import de.kindermaenner.monatsblitz.infrastructure.api.dto.toGameResult
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.GameDao
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.GameEntity
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.mapper.GameMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GameRepositoryImpl  (
    private val gameDao: GameDao
) : GameRepository {

    override fun observeGames(tournamentId: Int): Flow<List<Game>> =
        gameDao.observeGames(tournamentId)
            .map { list ->
                list.map(GameMapper::toDomain)
            }

    override fun observeGame(
        tournamentId: Int,
        player1Id: Int,
        player2Id: Int,
        leg: Leg
    ): Flow<Game?> =
        gameDao.observeGames(tournamentId)
            .map { list ->
                list
                    .firstOrNull {
                        it.player1Id == player1Id &&
                                it.player2Id == player2Id &&
                                it.leg == leg
                    }
                    ?.let(GameMapper::toDomain)
            }

    override suspend fun createGame(newGame: NewGame): Game {
        val entity = GameEntity(
            tournamentId = newGame.tournamentId,
            player1Id = newGame.player1Id,
            player2Id = newGame.player2Id,
            leg =  if (newGame.leg == Leg.SECOND) Leg.SECOND else Leg.FIRST,
            result =  GameResult.Open
        )

        gameDao.insert(entity)

        return GameMapper.toDomain(entity)
    }

    override suspend fun updateGame(game: Game) {
        gameDao.update(GameMapper.toEntity(game))
    }
}
