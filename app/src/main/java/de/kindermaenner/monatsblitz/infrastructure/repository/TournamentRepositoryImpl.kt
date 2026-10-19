package de.kindermaenner.monatsblitz.infrastructure.repository

import android.util.Log
import androidx.room.withTransaction
import de.kindermaenner.monatsblitz.domain.model.GameResult
import de.kindermaenner.monatsblitz.domain.model.NewTournament
import de.kindermaenner.monatsblitz.domain.model.PlayerRankingEntry
import de.kindermaenner.monatsblitz.domain.model.Tournament
import de.kindermaenner.monatsblitz.domain.model.TournamentRankingItem
import de.kindermaenner.monatsblitz.domain.repository.TournamentRepository
import de.kindermaenner.monatsblitz.infrastructure.TournamentStorage
import de.kindermaenner.monatsblitz.infrastructure.api.MonatsblitzApi
import de.kindermaenner.monatsblitz.infrastructure.api.dto.CreateGamesDto
import de.kindermaenner.monatsblitz.infrastructure.api.dto.CreateResultsDto
import de.kindermaenner.monatsblitz.infrastructure.api.dto.FinalizeRequestDto
import de.kindermaenner.monatsblitz.infrastructure.api.dto.GameDtoData
import de.kindermaenner.monatsblitz.infrastructure.api.dto.NewTournamentDto
import de.kindermaenner.monatsblitz.infrastructure.api.dto.ResultDto
import de.kindermaenner.monatsblitz.infrastructure.api.dto.toResultString
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.AppDatabase
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.GameDao
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.PlayerDao
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.TournamentDao
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.TournamentPlayerDao
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.GameEntity
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.TournamentPlayerCrossRef
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.mapper.toDomain
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.mapper.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import retrofit2.HttpException

class TournamentRepositoryImpl(
    private val tournamentDao: TournamentDao,
    private val tournamentPlayerDao: TournamentPlayerDao,
    private val gameDao: GameDao,
    private val database: AppDatabase,
    private val tournamentStorage: TournamentStorage,
    private val playerDao: PlayerDao? = null,
    private val monatsblitzApi: MonatsblitzApi? = null,
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

    override suspend fun getTournamentById(id: Long): Tournament? {
        val entity = tournamentDao.getTournament(id)
        return entity?.toDomain()
    }

    override suspend fun createTournament(request: NewTournament): Tournament {
        return database.withTransaction {
            val tournamentEntity = request.toEntity()
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
        val minPlayerId = minOf(playerId1, playerId2)
        val maxPlayerId = maxOf(playerId1, playerId2)
        val canonicalResult = if (playerId1 <= playerId2) result else result.opposite()

        Log.i(TAG, "updateGameResult called with tournamentId=$tournamentId, playerId1=$minPlayerId, playerId2=$maxPlayerId, leg=$leg, result=$canonicalResult")
        val game = gameDao.getGameByPlayers(tournamentId, minPlayerId, maxPlayerId, leg)

        if (game != null) {
            val shouldBeDirty = canonicalResult != GameResult.Open || game.remoteId != null
            Log.i(TAG, "updateGameResult: Updating existing game: $game")
            gameDao.update(game.copy(result = canonicalResult, dirty = shouldBeDirty))
        } else {
            val newGame = GameEntity(
                tournamentId = tournamentId,
                player1Id = minPlayerId,
                player2Id = maxPlayerId,
                leg = leg,
                result = canonicalResult,
                dirty = canonicalResult != GameResult.Open
            )
            Log.i(TAG, "updateGameResult: Inserting new game: $newGame")
            gameDao.insert(newGame)
        }
    }

    override suspend fun resetTournament() {
        val currentState = tournamentStorage.getTournamentState().first()
        if (currentState != null && !currentState.finalized) {
            database.withTransaction {
                tournamentPlayerDao.deleteForTournament(currentState.tournamentId)
                tournamentDao.deleteById(currentState.tournamentId)
            }
        }
        tournamentStorage.resetAll()
    }

    override suspend fun saveRankings(rankings: List<PlayerRankingEntry>) {
        val entities = rankings.map { it.toEntity() }
        tournamentPlayerDao.upsertAll(entities)
    }

    override fun observeTournamentRanking(tournamentId: Long): Flow<List<TournamentRankingItem>> {
        val resolvedPlayerDao = playerDao
            ?: throw IllegalStateException("PlayerDao ist nicht konfiguriert")

        return tournamentPlayerDao.observeRankingForTournament(tournamentId)
            .map { entries ->
                val players = resolvedPlayerDao.getAllPlayers().associateBy { it.id }
                entries.filter { it.rank != null }.map { entry ->
                    val domainEntry = entry.toDomain()
                    val player = players[entry.playerId]
                    TournamentRankingItem(
                        playerId = entry.playerId,
                        name = if (player != null) "${player.vorname} ${player.name}" else "Unbekannt",
                        points = domainEntry.points,
                        rank = domainEntry.rank
                    )
                }
            }
    }

    override suspend fun syncDirtyTournaments() {
        val api = monatsblitzApi
            ?: throw IllegalStateException("MonatsblitzApi ist nicht konfiguriert")

        tournamentDao.getDirtyTournaments().forEach {
            val dto = NewTournamentDto(
                date = it.tournament.date.toString(),
                mode = it.tournament.mode.displayName,
                roundCount = it.tournament.rounds,
                playerIds = it.players.mapNotNull { player -> player.remoteId }
            )
            try {
                val response = api.createTournament(dto)
                if (response.success) {
                    tournamentDao.updateTournamentRemoteId(it.tournament.id, response.tournamentId)
                }
            } catch (e: HttpException) {
                if (e.code() == 409) {
                    val existingOpenTournament = api.getTournaments().firstOrNull { remote ->
                        remote.date == it.tournament.date.toString() &&
                            remote.status.equals("open", ignoreCase = true)
                    }
                    if (existingOpenTournament != null) {
                        tournamentDao.updateTournamentRemoteId(it.tournament.id, existingOpenTournament.id)
                    } else {
                        throw e
                    }
                } else {
                    throw e
                }
            }
        }
    }

    override suspend fun syncDirtyGames() {
        val api = monatsblitzApi
            ?: throw IllegalStateException("MonatsblitzApi ist nicht konfiguriert")

        val gamesByTournament = gameDao.getGamesForSync()
            .groupBy { it.tournamentRemoteId }
        gamesByTournament.forEach { (tournamentRemoteId, games) ->
            val data = games.map { syncData ->
                GameDtoData(
                    gameId = syncData.remoteGameId?.toInt(),
                    player1Id = syncData.player1Id.toInt(),
                    player2Id = syncData.player2Id.toInt(),
                    leg = syncData.leg,
                    result = syncData.result.toResultString()
                )
            }

            val response = api.createGames(
                CreateGamesDto(
                    tournamentId = tournamentRemoteId,
                    games = data
                )
            )
            if (response.success) {
                games.forEachIndexed { index, game ->
                    gameDao.markGameAsSynced(game.localGameId, response.items[index].gameId)
                }
            }
        }
    }

    override suspend fun submitResultsAndFinalize(tournamentId: Long) {
        val api = monatsblitzApi
            ?: throw IllegalStateException("MonatsblitzApi ist nicht konfiguriert")
        val resolvedPlayerDao = playerDao
            ?: throw IllegalStateException("PlayerDao ist nicht konfiguriert")

        val tournamentWithDetails = tournamentDao.getTournament(tournamentId)
            ?: throw IllegalStateException("Turnier mit ID $tournamentId nicht gefunden")

        val remoteId = tournamentWithDetails.tournament.remoteId
            ?: throw IllegalStateException("Turnier konnte nicht mit dem Server synchronisiert werden")

        val rankings = tournamentPlayerDao.getRankingForTournament(tournamentId)
        val playerEntities = resolvedPlayerDao.getPlayersByIds(rankings.map { it.playerId }).associateBy { it.id }

        val resultDtos = rankings.map { entry ->
            val player = playerEntities[entry.playerId]
                ?: throw IllegalStateException("Spieler mit ID ${entry.playerId} nicht gefunden")
            val playerRemoteId = player.remoteId
                ?: throw IllegalStateException("Spieler ${player.vorname} ${player.name} hat keine Remote-ID")

            ResultDto(
                playerId = playerRemoteId.toInt(),
                points = entry.points ?: 0.0,
                rank = entry.rank ?: 0
            )
        }

        val createResultsResponse = api.createResults(
            CreateResultsDto(
                tournamentId = remoteId.toInt(),
                results = resultDtos
            )
        )
        if (!createResultsResponse.success) {
            val errorMsg = createResultsResponse.errors?.joinToString() ?: "Ergebnisse konnten nicht übertragen werden"
            throw IllegalStateException(errorMsg)
        }

        val finalizeResponse = api.finalize(
            FinalizeRequestDto(tournamentId = remoteId.toInt())
        )
        if (!finalizeResponse.success) {
            throw IllegalStateException("Finalisierung auf dem Server fehlgeschlagen")
        }

        tournamentStorage.finalizeTournament()
    }

    override suspend fun getActiveOpenTournamentId(): Long? {
        val state = tournamentStorage.getTournamentState().first()
        if (state == null || state.finalized) {
            return null
        }

        val api = monatsblitzApi ?: return state.tournamentId
        val isRemoteFinalized = try {
            val tournament = tournamentDao.getTournament(state.tournamentId)
            val remoteId = tournament?.tournament?.remoteId ?: return state.tournamentId
            val remoteTournament = api.getTournament(remoteId.toInt())
            val finalized = remoteTournament.status.equals("finalized", ignoreCase = true)
            if (finalized) {
                tournamentStorage.finalizeTournament()
            }
            finalized
        } catch (_: Exception) {
            false
        }

        return if (isRemoteFinalized) null else state.tournamentId
    }

    companion object {
        const val TAG = "TournamentRepository"
    }

}