package de.kindermaenner.monatsblitz.infrastructure.repository

import android.util.Log
import androidx.room.withTransaction
import de.kindermaenner.monatsblitz.domain.model.GameResult
import de.kindermaenner.monatsblitz.domain.model.PlayerWithdrawRule
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
import de.kindermaenner.monatsblitz.infrastructure.api.dto.WithdrawPlayerDto
import de.kindermaenner.monatsblitz.domain.model.GameMode
import de.kindermaenner.monatsblitz.domain.repository.RemoteOpenTournamentInfo
import de.kindermaenner.monatsblitz.domain.usecase.CreateNewGamesUseCase
import de.kindermaenner.monatsblitz.infrastructure.api.dto.GameDto
import de.kindermaenner.monatsblitz.infrastructure.api.dto.toGameResult
import de.kindermaenner.monatsblitz.infrastructure.api.dto.toResultString
import java.time.LocalDate
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.AppDatabase
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.GameDao
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.PlayerDao
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.TournamentDao
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.TournamentPlayerDao
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.GameEntity
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.PlayerEntity
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
    private val createNewGamesUseCase: CreateNewGamesUseCase = CreateNewGamesUseCase(),
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
        return tournamentPlayerDao.observeRankingWithPlayers(tournamentId)
            .map { entries ->
                entries.map { entry ->
                    val fullName = if (entry.vorname != null && entry.name != null) {
                        "${entry.vorname} ${entry.name}"
                    } else {
                        "Unbekannt"
                    }
                    val displayName = if (entry.hasLeft) "$fullName (z)" else fullName
                    TournamentRankingItem(
                        playerId = entry.playerId,
                        name = displayName,
                        points = entry.points ?: 0.0,
                        rank = entry.rank
                    )
                }
            }
    }

    override suspend fun withdrawPlayer(
        tournamentId: Long,
        playerId: Long,
        rule: PlayerWithdrawRule
    ) {
        tournamentPlayerDao.setPlayerWithdrawn(tournamentId, playerId, true)

        val games = gameDao.getGamesForTournament(tournamentId)
        val openGamesToUpdate = games.filter { game ->
            (game.player1Id == playerId || game.player2Id == playerId) && game.result == GameResult.Open
        }

        openGamesToUpdate.forEach { game ->
            val newResult = when (rule) {
                PlayerWithdrawRule.YOUTH_DRAW -> GameResult.ForfeitDraw
                PlayerWithdrawRule.REGULAR_LOSS -> {
                    if (game.player1Id == playerId) {
                        GameResult.ForfeitLoss
                    } else {
                        GameResult.ForfeitWin
                    }
                }
            }
            gameDao.update(game.copy(result = newResult, dirty = true))
        }

        val tournament = tournamentDao.getTournament(tournamentId)
        val player = playerDao?.getPlayerById(playerId)
        val remoteTournamentId = tournament?.tournament?.remoteId
        val remotePlayerId = player?.remoteId
        if (remoteTournamentId != null && remotePlayerId != null && monatsblitzApi != null) {
            try {
                monatsblitzApi.withdrawPlayer(
                    WithdrawPlayerDto(
                        tournamentId = remoteTournamentId.toInt(),
                        playerId = remotePlayerId.toInt(),
                        hasLeft = 1
                    )
                )
            } catch (e: Exception) {
                Log.w(TAG, "withdrawPlayer: Failed to notify remote backend", e)
            }
        }
    }

    override suspend fun syncDirtyTournaments() {
        val api = requireApi()

        val dirtyTournaments = tournamentDao.getDirtyTournaments()
        Log.i(TAG, "syncDirtyTournaments: found ${dirtyTournaments.size} dirty tournaments")
        dirtyTournaments.forEach {
            val dto = NewTournamentDto(
                date = it.tournament.date.toString(),
                mode = it.tournament.mode.displayName,
                roundCount = it.tournament.rounds,
                playerIds = it.players.mapNotNull { player -> player.remoteId }
            )
            try {
                Log.i(TAG, "syncDirtyTournaments: Sending tournament to API: $dto")
                val response = api.createTournament(dto)
                Log.i(TAG, "syncDirtyTournaments: Response from API: $response")
                if (response.success) {
                    tournamentDao.updateTournamentRemoteId(it.tournament.id, response.tournamentId)
                }
            } catch (e: HttpException) {
                Log.e(TAG, "syncDirtyTournaments: HTTP error ${e.code()}", e)
                handleCreateTournamentConflict(api, it.tournament.id, it.tournament.date.toString(), e)
            }
        }
    }

    private suspend fun handleCreateTournamentConflict(
        api: MonatsblitzApi,
        localTournamentId: Long,
        tournamentDate: String,
        exception: HttpException
    ) {
        if (exception.code() != 409) {
            throw exception
        }
        val existingOpenTournament = api.getTournaments().firstOrNull { remote ->
            remote.date == tournamentDate && remote.status.equals("open", ignoreCase = true)
        } ?: throw exception

        tournamentDao.updateTournamentRemoteId(localTournamentId, existingOpenTournament.id)
    }

    override suspend fun syncDirtyGames() {
        val api = requireApi()

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
        val api = requireApi()
        val resolvedPlayerDao = requirePlayerDao()

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

        val withdrawnPlayerRefs = tournamentPlayerDao.getRankingForTournament(tournamentId).filter { it.hasLeft }
        val withdrawnRemoteIds = withdrawnPlayerRefs.mapNotNull { ref ->
            playerEntities[ref.playerId]?.remoteId?.toInt()
        }

        val finalizeResponse = api.finalize(
            FinalizeRequestDto(
                tournamentId = remoteId.toInt(),
                withdrawnPlayerIds = withdrawnRemoteIds.ifEmpty { null }
            )
        )
        if (!finalizeResponse.success) {
            throw IllegalStateException("Finalisierung auf dem Server fehlgeschlagen")
        }

        tournamentStorage.finalizeTournament()
    }

    override suspend fun finalizeLocalTournament(tournamentId: Long) {
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

    override suspend fun getRemoteOpenTournament(): RemoteOpenTournamentInfo? {
        val api = monatsblitzApi ?: return null
        return try {
            val remoteList = api.getTournaments()
            val openTournament = remoteList.firstOrNull { it.status.equals("open", ignoreCase = true) } ?: return null

            val currentState = tournamentStorage.getTournamentState().first()
            if (currentState != null && !currentState.finalized) {
                val localTournament = tournamentDao.getTournament(currentState.tournamentId)
                if (localTournament?.tournament?.remoteId?.toInt() == openTournament.id) {
                    return null
                }
            }

            RemoteOpenTournamentInfo(
                id = openTournament.id,
                date = openTournament.date,
                mode = openTournament.mode,
                roundCount = openTournament.roundCount,
                playerCount = openTournament.playerIds.size
            )
        } catch (e: Exception) {
            Log.e(TAG, "getRemoteOpenTournament failed: ${e.message}", e)
            null
        }
    }

    override suspend fun adoptRemoteTournament(remoteTournamentId: Int): Long {
        val api = requireApi()
        val resolvedPlayerDao = requirePlayerDao()

        val remoteTournament = api.getTournament(remoteTournamentId)
        val remoteGames = try {
            api.getGames(remoteTournamentId)
        } catch (e: Exception) {
            Log.w(TAG, "adoptRemoteTournament: could not fetch games: ${e.message}")
            emptyList()
        }

        val remotePlayerIds = remoteTournament.playerIds
        val localPlayers = remotePlayerIds.mapNotNull { remoteId ->
            resolvedPlayerDao.getPlayerByRemoteId(remoteId)
        }

        val domainPlayers = localPlayers.map { it.toDomain() }
        val mode = GameMode.fromString(remoteTournament.mode)
        val rounds = maxOf(1, remoteTournament.roundCount)
        val parsedDate = runCatching { LocalDate.parse(remoteTournament.date) }.getOrElse { LocalDate.now() }

        val localTournamentId = database.withTransaction {
            val tournamentId = upsertTournamentEntity(remoteTournamentId, mode, rounds, parsedDate)

            val withdrawnRemoteIds = remoteTournament.withdrawnPlayerIds.toSet()
            val playerRefs = localPlayers.map {
                val hasLeft = it.remoteId != null && withdrawnRemoteIds.contains(it.remoteId)
                TournamentPlayerCrossRef(
                    tournamentId = tournamentId,
                    playerId = it.id,
                    hasLeft = hasLeft
                )
            }
            tournamentPlayerDao.deleteForTournament(tournamentId)
            tournamentPlayerDao.insertAll(playerRefs)

            val baseGames = createNewGamesUseCase(domainPlayers, rounds)
            val gameEntities = reconcileGames(tournamentId, baseGames, remoteGames, resolvedPlayerDao)

            gameDao.deleteForTournament(tournamentId)
            gameDao.insertAll(gameEntities)

            saveTournamentRankings(tournamentId, localPlayers, gameEntities)

            tournamentStorage.saveTournamentState(tournamentId, false)
            tournamentId
        }

        return localTournamentId
    }

    private suspend fun upsertTournamentEntity(
        remoteTournamentId: Int,
        mode: GameMode,
        rounds: Int,
        parsedDate: LocalDate
    ): Long {
        val existingTournamentWithDetails = tournamentDao.getTournamentByRemoteId(remoteTournamentId)
        return if (existingTournamentWithDetails != null) {
            val tournamentId = existingTournamentWithDetails.tournament.id
            tournamentDao.update(
                existingTournamentWithDetails.tournament.copy(
                    mode = mode,
                    date = parsedDate,
                    rounds = rounds,
                    dirty = false
                )
            )
            tournamentId
        } else {
            val newEntity = de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.TournamentEntity(
                mode = mode,
                date = parsedDate,
                rounds = rounds,
                remoteId = remoteTournamentId.toLong(),
                dirty = false
            )
            tournamentDao.insert(newEntity)
        }
    }

    private suspend fun reconcileGames(
        tournamentId: Long,
        baseGames: List<de.kindermaenner.monatsblitz.domain.model.Game>,
        remoteGames: List<GameDto>,
        resolvedPlayerDao: PlayerDao
    ): List<GameEntity> {
        val remoteGameMap = mutableMapOf<Triple<Long, Long, Int>, GameDto>()
        remoteGames.forEach { rg ->
            val lp1 = resolvedPlayerDao.getPlayerByRemoteId(rg.player1Id.toLong())?.id
            val lp2 = resolvedPlayerDao.getPlayerByRemoteId(rg.player2Id.toLong())?.id
            if (lp1 != null && lp2 != null) {
                remoteGameMap[Triple(minOf(lp1, lp2), maxOf(lp1, lp2), rg.leg)] = rg
            }
        }

        return baseGames.map { bg ->
            val pMin = minOf(bg.player1Id, bg.player2Id)
            val pMax = maxOf(bg.player1Id, bg.player2Id)
            val remoteGame = remoteGameMap[Triple(pMin, pMax, bg.leg)]

            val (result, remoteGameId) = if (remoteGame != null) {
                val lp1 = resolvedPlayerDao.getPlayerByRemoteId(remoteGame.player1Id.toLong())?.id ?: pMin
                val rawResult = remoteGame.result.toGameResult()
                val canonicalResult = if (lp1 == pMin) rawResult else rawResult.opposite()
                Pair(canonicalResult, remoteGame.id.toLong())
            } else {
                Pair(bg.result, null)
            }

            GameEntity(
                tournamentId = tournamentId,
                player1Id = pMin,
                player2Id = pMax,
                leg = bg.leg,
                result = result,
                remoteId = remoteGameId,
                dirty = false
            )
        }
    }

    private suspend fun saveTournamentRankings(
        tournamentId: Long,
        localPlayers: List<PlayerEntity>,
        gameEntities: List<GameEntity>
    ) {
        val playerPoints = mutableMapOf<Long, Double>()
        localPlayers.forEach { playerPoints[it.id] = 0.0 }

        gameEntities.forEach { game ->
            when (game.result) {
                GameResult.Win,
                GameResult.ForfeitWin -> {
                    playerPoints[game.player1Id] = (playerPoints[game.player1Id] ?: 0.0) + 1.0
                }
                GameResult.Loss,
                GameResult.ForfeitLoss -> {
                    playerPoints[game.player2Id] = (playerPoints[game.player2Id] ?: 0.0) + 1.0
                }
                GameResult.Remis,
                GameResult.ForfeitDraw -> {
                    playerPoints[game.player1Id] = (playerPoints[game.player1Id] ?: 0.0) + 0.5
                    playerPoints[game.player2Id] = (playerPoints[game.player2Id] ?: 0.0) + 0.5
                }
                GameResult.Open -> { /* 0.0 */ }
            }
        }

        val sorted = playerPoints.entries.sortedByDescending { it.value }
        var lastPoints: Double? = null
        var lastRank = 0
        var itemsProcessed = 0
        val updatedRefs = sorted.map { (playerId, points) ->
            itemsProcessed++
            val rank = if (lastPoints != null && points == lastPoints) lastRank else itemsProcessed
            lastPoints = points
            lastRank = rank
            TournamentPlayerCrossRef(
                tournamentId = tournamentId,
                playerId = playerId,
                points = points,
                rank = rank
            )
        }
        tournamentPlayerDao.upsertAll(updatedRefs)
    }

    override suspend fun getRecentTournamentModes(): List<String> {
        val api = monatsblitzApi
        if (api != null) {
            try {
                val remoteTournaments = api.getTournaments()
                if (remoteTournaments.isNotEmpty()) {
                    return remoteTournaments.map { it.mode }
                }
            } catch (e: Exception) {
                Log.w(TAG, "getRecentTournamentModes: failed to fetch remote tournaments: ${e.message}")
            }
        }
        return try {
            val local = tournamentDao.observeTournaments().first()
            local.sortedByDescending { it.tournament.date }.map { it.tournament.mode.name }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun requireApi(): MonatsblitzApi =
        monatsblitzApi ?: throw IllegalStateException(ERROR_API_NOT_CONFIGURED)

    private fun requirePlayerDao(): PlayerDao =
        playerDao ?: throw IllegalStateException(ERROR_PLAYER_DAO_NOT_CONFIGURED)

    companion object {
        const val TAG = "TournamentRepository"
        private const val ERROR_API_NOT_CONFIGURED = "MonatsblitzApi ist nicht konfiguriert"
        private const val ERROR_PLAYER_DAO_NOT_CONFIGURED = "PlayerDao ist nicht konfiguriert"
    }

}
