package de.kindermaenner.monatsblitz.infrastructure.repository

import de.kindermaenner.monatsblitz.domain.model.Game
import de.kindermaenner.monatsblitz.domain.model.GameMode
import de.kindermaenner.monatsblitz.domain.model.GameResult
import de.kindermaenner.monatsblitz.domain.model.Leg
import de.kindermaenner.monatsblitz.domain.model.MatchKey
import de.kindermaenner.monatsblitz.domain.model.NewTournament
import de.kindermaenner.monatsblitz.domain.model.Tournament
import de.kindermaenner.monatsblitz.domain.repository.TournamentRepository
import de.kindermaenner.monatsblitz.infrastructure.TournamentStorage
import de.kindermaenner.monatsblitz.infrastructure.api.MonatsblitzApi
import de.kindermaenner.monatsblitz.infrastructure.api.dto.CreateGameDto
import de.kindermaenner.monatsblitz.infrastructure.api.dto.NewTournamentDto
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.GameDao
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.PlayerDao
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.TournamentDao
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.TournamentPlayerDao
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.TournamentEntity
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.TournamentPlayerCrossRef
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.mapper.GameMapper
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.mapper.PlayerMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class TournamentRepositoryImpl(
    private val tournamentDao: TournamentDao,
    private val tournamentPlayerDao: TournamentPlayerDao,
    private val playerDao: PlayerDao,
    private val gameDao: GameDao,
    private val api: MonatsblitzApi
) : TournamentRepository {

    override fun observeTournaments(): Flow<List<Tournament>> {
       return tournamentDao.observeTournaments()
           .map { list ->
               list.map {
                   Tournament(
                       Id = it.id,
                       Mode = it.mode,
                       Date = it.date,
                       players = emptyList(),
                       games = mutableMapOf(),
                       doubleRound = it.doubleRound
                   )
               }
           }
    }

    override fun observeTournament(id: Int): Flow<Tournament?> {
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

    private fun generateGames(request: NewTournament): List<Game> {
        val games = mutableListOf<Game>()
        val playerIds = request.playerIds
        val roundCount = if (request.doubleRound) 2 else 1

        for (round in 1..roundCount) {
            for (i in playerIds.indices) {
                for (j in i + 1 until playerIds.size) {
                    val player1Id = playerIds[i]
                    val player2Id = playerIds[j]
                    games.add(
                        Game(
                            tournamentId = -1, // Placeholder, will be set after tournament creation
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
        val newTournamentDto = NewTournamentDto(
            date = request.Date.toString(),
            mode = request.Mode.displayName,
            round_count = if (request.doubleRound) 2 else 1
        )
        val result = api.createTournament(newTournamentDto)
        tournamentDao.insert(
            TournamentEntity(
                remoteId = if (result.success) result.tournament_id else null,
                mode = request.Mode,
                date = request.Date,
                doubleRound = request.doubleRound,
                dirty = !result.success
            )
        )
        val refs = request.playerIds.map {
            TournamentPlayerCrossRef(result.tournament_id, it)
        }
        tournamentPlayerDao.insertAll(refs)

        val games = generateGames(request).map {
            GameMapper.toEntity(it)
        }
        gameDao.insertAll(games)

        return observeTournament(result.tournament_id).first()!!
    }

    override suspend fun refreshTournament(id: Int) {
        tournamentDao.getTournament(id)?.remoteId?.let { it ->
            api.getTournament(it).let { tournamentDto ->
                val tournamentEntity = TournamentEntity(
                    id = id,
                    remoteId = tournamentDto.id,
                    mode = GameMode.fromDisplayName(tournamentDto.mode) ?: GameMode.BLITZ_3_2,
                    date = LocalDate.parse(tournamentDto.date),
                    doubleRound = tournamentDto.round_count == 2
                )
                tournamentDao.insert(tournamentEntity)
            }
        }
    }

    override suspend fun syncTournament(id: Int) {
        val tournament = observeTournament(id).first() ?: throw IllegalArgumentException("Tournament not found")
        val games = gameDao.observeGames(id).first()
        gameDao.observeGames(id).first().forEach { gameEntity ->
            val gameDto = GameMapper.toDto(GameMapper.toDomain(gameEntity))
            api.createGame(CreateGameDto(id, gameDto)) // TODO: dirty update
            gameDao.markGameAsSynced(gameEntity);
        }
        api.createTournament(NewTournamentDto(tournament.Date.toString(), tournament.Mode.displayName, if (tournament.doubleRound)2 else 1))
        tournamentDao.markTournamentAsClean(id);
    }


}