package de.kindermaenner.monatsblitz.domain.usecase

import de.kindermaenner.monatsblitz.domain.repository.SettingsRepository
import de.kindermaenner.monatsblitz.domain.repository.TournamentDumpRepository
import kotlinx.coroutines.flow.firstOrNull

class CreateTournamentDumpUseCase(
    private val tournamentDumpRepository: TournamentDumpRepository,
    private val settingsRepository: SettingsRepository? = null
) {
    suspend operator fun invoke(tournamentId: Long): Result<String> {
        val result = tournamentDumpRepository.createDump(tournamentId)

        val autoCleanup = settingsRepository?.autoCleanupDumps?.firstOrNull() ?: true
        if (autoCleanup) {
            val retentionMonths = settingsRepository?.dumpRetentionMonths?.firstOrNull() ?: 3
            tournamentDumpRepository.cleanupOldDumps(retentionMonths)
        }

        return result
    }
}
