package de.kindermaenner.monatsblitz.infrastructure.dump

import android.content.Context
import android.util.Log
import de.kindermaenner.monatsblitz.domain.repository.TournamentDumpRepository
import de.kindermaenner.monatsblitz.infrastructure.dump.dto.DumpGameDto
import de.kindermaenner.monatsblitz.infrastructure.dump.dto.DumpPlayerDto
import de.kindermaenner.monatsblitz.infrastructure.dump.dto.TournamentDumpDto
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.AppDatabase
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.time.Instant
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
                if (file.lastModified() < cutoffMillis) {
                    if (file.delete()) {
                        deletedCount++
                        Log.i(TAG, "Alter Turnier-Dump gelöscht: ${file.name}")
                    }
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

    companion object {
        private const val TAG = "TournamentDump"
    }
}
