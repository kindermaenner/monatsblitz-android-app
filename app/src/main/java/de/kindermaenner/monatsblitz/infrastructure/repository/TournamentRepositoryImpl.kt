package de.kindermaenner.monatsblitz.infrastructure.repository

import de.kindermaenner.monatsblitz.domain.model.NewTournament
import de.kindermaenner.monatsblitz.domain.model.Tournament
import de.kindermaenner.monatsblitz.domain.repository.TournamentRepository
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.GameDao
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.TournamentDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import androidx.room.withTransaction
import de.kindermaenner.monatsblitz.domain.model.GameResult
import de.kindermaenner.monatsblitz.infrastructure.TournamentStorage
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.AppDatabase
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.PlayerDao
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.TournamentPlayerDao
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.TournamentPlayerCrossRef
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.GameEntity
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.mapper.toDomain
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.mapper.toEntity

class TournamentRepositoryImpl(
    private val tournamentDao: TournamentDao,
    private val tournamentPlayerDao : TournamentPlayerDao,
    private val playerDao : PlayerDao,
    private val gameDao: GameDao,
    private val database: AppDatabase,
    private val tournamentStorage : TournamentStorage,
) : TournamentRepository {

    override fun observeTournaments(): Flow<List<Tournament>> {
        return tournamentDao.observeTournaments()
            .map { entities ->
                entities.map { it.toDomain() }
            }
    }

    override fun observeTournament(id: Long): Flow<Tournament?> {
        return tournamentDao.observeTournament(id)
            .map { it ->
                it?.toDomain()
            }
    }

    override suspend fun createTournament(request: NewTournament): Tournament {
        return database.withTransaction {
            val tournamentEntity = request.toEntity();
            val tournamentId = tournamentDao.insert(
                tournamentEntity
            )

            val gameEntities = request.games.map {
                it.toEntity(
                    tournamentId = tournamentId
                )
            }

            val gameIds = gameDao.insertAll(gameEntities)

            val savedGameEntities = gameEntities.mapIndexed { index, entity ->
                entity.copy(id = gameIds[index])
            }

            val playerRefs = request.players.map {
                TournamentPlayerCrossRef(
                    tournamentId = tournamentId,
                    playerId = it.id
                )
            }
            tournamentPlayerDao.insertAll(playerRefs)

            tournamentStorage.saveTournamentState(tournamentId, false)
            tournamentEntity
                .copy(id = tournamentId)
                .toDomain(savedGameEntities, players = request.players)
        }
    }

    override suspend fun updateGameResult(
        tournamentId: Long,
        playerId1: Long,
        playerId2: Long,
        leg: Int,
        result: GameResult
    ) {
        val game = gameDao.getGameByPlayers(tournamentId, playerId1, playerId2)
        
        if (game != null) {
            gameDao.update(game.copy(result = result, dirty = true))
        } else {
            val newGame = GameEntity(
                tournamentId = tournamentId,
                player1Id = playerId1,
                player2Id = playerId2,
                leg = leg,
                result = result,
                dirty = true
            )
            gameDao.insert(newGame)
        }
    }

    companion object {
        const val TAG = "TournamentRepository"
    }

}