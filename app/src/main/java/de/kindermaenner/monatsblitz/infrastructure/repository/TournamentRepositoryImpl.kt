package de.kindermaenner.monatsblitz.infrastructure.repository

import de.kindermaenner.monatsblitz.domain.model.Game
import de.kindermaenner.monatsblitz.domain.model.GameResult
import de.kindermaenner.monatsblitz.domain.model.Leg
import de.kindermaenner.monatsblitz.domain.model.MatchKey
import de.kindermaenner.monatsblitz.domain.model.NewTournament
import de.kindermaenner.monatsblitz.domain.model.Tournament
import de.kindermaenner.monatsblitz.domain.repository.TournamentRepository
import de.kindermaenner.monatsblitz.infrastructure.api.MonatsblitzApi
import de.kindermaenner.monatsblitz.infrastructure.api.dto.toDto
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.GameDao
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.PlayerDao
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.TournamentDao
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.TournamentPlayerDao
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.TournamentPlayerCrossRef
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.mapper.GameMapper
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.mapper.PlayerMapper
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.mapper.TournamentMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import androidx.room.withTransaction
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.AppDatabase

class TournamentRepositoryImpl(
    private val tournamentDao: TournamentDao,
    private val tournamentPlayerDao: TournamentPlayerDao,
    private val playerDao: PlayerDao,
    private val gameDao: GameDao,
    private val api: MonatsblitzApi,
    private val database: AppDatabase
) : TournamentRepository {

    override fun observeTournaments(): Flow<List<Tournament>> {
        return tournamentDao.observeTournaments()
            .map { list ->
                list.map {
                    TournamentMapper.toDomain(it)
                }
            }
    }

    override fun observeTournament(id: Long): Flow<Tournament?> {
        val tournamentFlow = tournamentDao.observeTournament(id)
        val playersFlow = playerDao.observePlayers()
        val gamesFlow = gameDao.observeGames(id)

        return combine(
            tournamentFlow,
            playersFlow,
            gamesFlow
        ) { tournament, players, games ->

            if (tournament == null) return@combine null

            val playerIds = tournamentPlayerDao
                .getPlayerIdsForTournament(id)
                .toSet()

            val tournamentPlayers = players
                .filter { it.id in playerIds }

            val gameMap = games.associate { game ->
                MatchKey(
                    tournamentId = game.tournamentId,
                    player1Id = game.player1Id,
                    player2Id = game.player2Id,
                    leg = game.leg
                ) to GameMapper.toDomain(game)
            }

            Tournament(
                Id = tournament.id,
                Mode = tournament.mode,
                Date = tournament.date,
                players = tournamentPlayers.map(PlayerMapper::toDomain),
                games = gameMap.toMutableMap(),
                doubleRound = tournament.doubleRound
            )
        }
    }

    private fun generateGames(request: NewTournament, tournamentId: Long): List<Game> {
        val games = mutableListOf<Game>()
        val playerIds = request.playerIds
        val roundCount = if (request.doubleRound) 2 else 1

        for (round in 1..roundCount) {
            for (i in playerIds.indices) {
                for (j in i + 1 until playerIds.size) {

                    val player1Id = if (round == 1) {
                        playerIds[i]
                    } else {
                        playerIds[j]
                    }

                    val player2Id = if (round == 1) {
                        playerIds[j]
                    } else {
                        playerIds[i]
                    }

                    games.add(
                        Game(
                            tournamentId = tournamentId,
                            player1Id = player1Id,
                            player2Id = player2Id,
                            leg = if (round == 1) Leg.FIRST else Leg.SECOND,
                            result = GameResult.Open
                        )
                    )
                }
            }
        }

        return games
    }

    override suspend fun createTournament(request: NewTournament): Tournament {
        val result = try {
            api.createTournament(request.toDto())
        } catch (e: IOException) {
            null
        }

        val tournamentId = database.withTransaction {

            val entity = TournamentMapper.toEntity(request, result?.tournament_id)
            val id = tournamentDao.insert(entity)

            val refs = request.playerIds.map {
                TournamentPlayerCrossRef(
                    id,
                    it
                )
            }
            tournamentPlayerDao.insertAll(refs)

            val games = generateGames(request, id).map {
                GameMapper.toEntity(it)
            }
            gameDao.insertAll(games)
            id
        }
        return observeTournament(tournamentId).first()!!
    }

}