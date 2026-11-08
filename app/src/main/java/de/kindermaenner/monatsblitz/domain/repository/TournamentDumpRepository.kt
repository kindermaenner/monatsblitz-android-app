package de.kindermaenner.monatsblitz.domain.repository

import de.kindermaenner.monatsblitz.domain.model.TournamentDumpFileInfo
import java.io.File

interface TournamentDumpRepository {
    suspend fun createDump(tournamentId: Long): Result<String>
    suspend fun cleanupOldDumps(retentionMonths: Int): Result<Int>
    suspend fun getDumpCount(): Int
    suspend fun getDumpFiles(): List<TournamentDumpFileInfo>
    suspend fun getDumpFile(fileName: String): File?
    suspend fun restoreDump(jsonContent: String): Result<Long>
}
