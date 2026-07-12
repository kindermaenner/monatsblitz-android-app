package de.kindermaenner.monatsblitz.infrastructure.repository

import de.kindermaenner.monatsblitz.domain.model.Game
import de.kindermaenner.monatsblitz.domain.model.GameResult
import de.kindermaenner.monatsblitz.domain.model.Leg
import de.kindermaenner.monatsblitz.domain.model.NewGame
import de.kindermaenner.monatsblitz.domain.model.Tournament
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

    override fun observeGames(tournamentId: Long): Flow<List<Game>> =
        gameDao.observeGames(tournamentId)
            .map { list ->
                list.map(GameMapper::toDomain)
            }

    override fun observeGame(
        tournamentId: Long,
        player1Id: Long,
        player2Id: Long,
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

        val entity = GameMapper.toEntity(newGame)

        gameDao.insert(entity)

        return GameMapper.toDomain(entity)
    }

    override suspend fun updateGame(game: Game) {
        gameDao.update(GameMapper.toEntity(game))
    }

}
