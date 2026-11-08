package de.kindermaenner.monatsblitz.domain.repository

interface TournamentDumpRepository {
    suspend fun createDump(tournamentId: Long): Result<String>
    suspend fun cleanupOldDumps(retentionMonths: Int): Result<Int>
    suspend fun getDumpCount(): Int
}
