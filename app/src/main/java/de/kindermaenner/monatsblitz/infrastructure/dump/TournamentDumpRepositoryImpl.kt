package de.kindermaenner.monatsblitz.infrastructure.dump

import android.content.Context
import android.util.Log
import de.kindermaenner.monatsblitz.domain.model.GameMode
import de.kindermaenner.monatsblitz.domain.model.GameResult
import de.kindermaenner.monatsblitz.domain.model.TournamentDumpFileInfo
import de.kindermaenner.monatsblitz.domain.repository.TournamentDumpRepository
import de.kindermaenner.monatsblitz.infrastructure.dump.dto.DumpGameDto
import de.kindermaenner.monatsblitz.infrastructure.dump.dto.DumpPlayerDto
import de.kindermaenner.monatsblitz.infrastructure.dump.dto.TournamentDumpDto
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.AppDatabase
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.GameEntity
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.PlayerEntity
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.TournamentEntity
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.TournamentPlayerCrossRef
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class TournamentDumpRepositoryImpl(
    private val baseDirProvider: () -> File,
    private val database: AppDatabase,
    private val json: Json = Json { prettyPrint = true; ignoreUnknownKeys = true }
) : TournamentDumpRepository {

    constructor(context: Context, database: AppDatabase) : this(
        baseDirProvider = { File(context.filesDir, "tournament_dumps") },
        database = database
    )

    override suspend fun createDump(tournamentId: Long): Result<String> {
        return runCatching {
            val tournamentWithDetails = database.tournamentDao().getTournament(tournamentId)
                ?: throw IllegalArgumentException("Turnier mit ID $tournamentId nicht gefunden")

            val playerRefs = tournamentWithDetails.playerRefs.associateBy { it.playerId }
            val players = database.playerDao().getPlayersByIds(playerRefs.keys.toList())

            val dumpPlayers = players.map { player ->
                val ref = playerRefs[player.id]
                DumpPlayerDto(
                    localId = player.id,
                    remoteId = player.remoteId,
                    vorname = player.vorname,
                    name = player.name,
                    rufname = player.rufname,
                    hasLeft = ref?.hasLeft ?: false,
                    points = ref?.points,
                    rank = ref?.rank
                )
            }

            val dumpGames = tournamentWithDetails.games.map { game ->
                DumpGameDto(
                    localGameId = game.id,
                    player1Id = game.player1Id,
                    player2Id = game.player2Id,
                    leg = game.leg,
                    result = game.result.name,
                    remoteGameId = game.remoteId,
                    dirty = game.dirty
                )
            }

            val dump = TournamentDumpDto(
                dumpedAt = Instant.now().toString(),
                tournamentId = tournamentWithDetails.tournament.id,
                mode = tournamentWithDetails.tournament.mode.name,
                date = tournamentWithDetails.tournament.date.toString(),
                rounds = tournamentWithDetails.tournament.rounds,
                remoteId = tournamentWithDetails.tournament.remoteId,
                isLocalOnly = tournamentWithDetails.tournament.isLocalOnly,
                countsForRanking = tournamentWithDetails.tournament.countsForRanking,
                label = tournamentWithDetails.tournament.label,
                players = dumpPlayers,
                games = dumpGames
            )

            val dir = baseDirProvider().apply { if (!exists()) mkdirs() }
            val timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now())
            val file = File(dir, "tournament_${tournamentId}_${timestamp}.json")
            file.writeText(json.encodeToString(dump))
            Log.i(TAG, "Turnier-Dump erfolgreich gespeichert: ${file.absolutePath}")
            file.absolutePath
        }
    }

    override suspend fun cleanupOldDumps(retentionMonths: Int): Result<Int> {
        return runCatching {
            val dir = baseDirProvider()
            if (!dir.exists() || !dir.isDirectory) return@runCatching 0

            val cutoffMillis = System.currentTimeMillis() - (retentionMonths * 30L * 24L * 60L * 60L * 1000L)
            var deletedCount = 0

            dir.listFiles { file -> file.isFile && file.name.endsWith(".json") }?.forEach { file ->
                if (file.lastModified() < cutoffMillis && file.delete()) {
                    deletedCount++
                    Log.i(TAG, "Alter Turnier-Dump gelöscht: ${file.name}")
                }
            }
            deletedCount
        }
    }

    override suspend fun getDumpCount(): Int {
        val dir = baseDirProvider()
        if (!dir.exists() || !dir.isDirectory) return 0
        return dir.listFiles { file -> file.isFile && file.name.endsWith(".json") }?.size ?: 0
    }

    override suspend fun getDumpFiles(): List<TournamentDumpFileInfo> {
        val dir = baseDirProvider()
        if (!dir.exists() || !dir.isDirectory) return emptyList()

        return dir.listFiles { file -> file.isFile && file.name.endsWith(".json") }
            ?.sortedByDescending { it.lastModified() }
            ?.mapNotNull { file ->
                val match = Regex("tournament_(\\d+)_(.+)\\.json").find(file.name)
                val tId = match?.groupValues?.getOrNull(1)?.toLongOrNull() ?: 0L
                val ts = match?.groupValues?.getOrNull(2) ?: ""
                TournamentDumpFileInfo(
                    fileName = file.name,
                    filePath = file.absolutePath,
                    tournamentId = tId,
                    timestamp = ts,
                    sizeBytes = file.length()
                )
            } ?: emptyList()
    }

    override suspend fun getDumpFile(fileName: String): File? {
        val dir = baseDirProvider()
        val file = File(dir, fileName)
        return if (file.exists() && file.isFile) file else null
    }

    override suspend fun restoreDump(jsonContent: String): Result<Long> {
        return runCatching {
            val dump = json.decodeFromString<TournamentDumpDto>(jsonContent)
            val tournamentId = dump.tournamentId

            val localPlayerMap = restorePlayers(dump.players)
            restoreTournamentEntity(dump)
            restoreTournamentPlayers(tournamentId, dump.players, localPlayerMap)
            val gameCount = restoreGames(tournamentId, dump.games, localPlayerMap)

            Log.i(TAG, "Turnier $tournamentId erfolgreich aus Dump wiederhergestellt ($gameCount Partien)")
            tournamentId
        }
    }

    private suspend fun restorePlayers(dumpPlayers: List<DumpPlayerDto>): Map<Long, Long> {
        val localPlayerMap = mutableMapOf<Long, Long>()
        dumpPlayers.forEach { dumpPlayer ->
            val existing = database.playerDao().findPlayersByName(dumpPlayer.vorname, dumpPlayer.name).firstOrNull()
            val actualId = if (existing != null) {
                if (existing.remoteId == null && dumpPlayer.remoteId != null) {
                    database.playerDao().update(existing.copy(remoteId = dumpPlayer.remoteId))
                }
                existing.id
            } else {
                database.playerDao().insert(
                    PlayerEntity(
                        remoteId = dumpPlayer.remoteId,
                        vorname = dumpPlayer.vorname,
                        name = dumpPlayer.name,
                        rufname = dumpPlayer.rufname ?: ""
                    )
                )
            }
            localPlayerMap[dumpPlayer.localId] = actualId
        }
        return localPlayerMap
    }

    private suspend fun restoreTournamentEntity(dump: TournamentDumpDto) {
        val tournamentId = dump.tournamentId
        val existingTournament = database.tournamentDao().getTournament(tournamentId)
        val tournamentMode = GameMode.fromString(dump.mode)
        val tournamentDate = runCatching { LocalDate.parse(dump.date) }.getOrDefault(LocalDate.now())
        val tournamentEntity = TournamentEntity(
            id = tournamentId,
            remoteId = dump.remoteId,
            date = tournamentDate,
            rounds = dump.rounds,
            mode = tournamentMode,
            isLocalOnly = dump.isLocalOnly,
            countsForRanking = dump.countsForRanking,
            label = dump.label,
            dirty = true
        )
        if (existingTournament != null) {
            database.tournamentDao().update(tournamentEntity)
        } else {
            database.tournamentDao().insert(tournamentEntity)
        }
    }

    private suspend fun restoreTournamentPlayers(
        tournamentId: Long,
        dumpPlayers: List<DumpPlayerDto>,
        localPlayerMap: Map<Long, Long>
    ) {
        val crossRefs = dumpPlayers.mapNotNull { dumpPlayer ->
            val actualPlayerId = localPlayerMap[dumpPlayer.localId] ?: return@mapNotNull null
            TournamentPlayerCrossRef(
                tournamentId = tournamentId,
                playerId = actualPlayerId,
                hasLeft = dumpPlayer.hasLeft,
                points = dumpPlayer.points,
                rank = dumpPlayer.rank
            )
        }
        if (crossRefs.isNotEmpty()) {
            database.tournamentPlayerDao().upsertAll(crossRefs)
        }
    }

    private suspend fun restoreGames(
        tournamentId: Long,
        dumpGames: List<DumpGameDto>,
        localPlayerMap: Map<Long, Long>
    ): Int {
        val gameEntities = dumpGames.mapNotNull { dumpGame ->
            val p1 = localPlayerMap[dumpGame.player1Id] ?: return@mapNotNull null
            val p2 = localPlayerMap[dumpGame.player2Id] ?: return@mapNotNull null
            val result = runCatching { GameResult.valueOf(dumpGame.result) }.getOrDefault(GameResult.Open)
            GameEntity(
                id = dumpGame.localGameId,
                remoteId = dumpGame.remoteGameId,
                tournamentId = tournamentId,
                player1Id = p1,
                player2Id = p2,
                leg = dumpGame.leg,
                result = result,
                dirty = true
            )
        }
        if (gameEntities.isNotEmpty()) {
            database.gameDao().upsertAll(gameEntities)
        }
        return gameEntities.size
    }

    companion object {
        private const val TAG = "TournamentDump"
    }
}
