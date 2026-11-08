package de.kindermaenner.monatsblitz.domain.usecase

import de.kindermaenner.monatsblitz.domain.repository.SettingsRepository
import de.kindermaenner.monatsblitz.domain.repository.TournamentDumpRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class CreateTournamentDumpUseCaseTest {

    private val dumpRepository = mockk<TournamentDumpRepository>(relaxed = true)
    private val settingsRepository = mockk<SettingsRepository>(relaxed = true)

    @Test
    fun `invoke calls createDump and performs cleanup when autoCleanup is true`() = runTest {
        coEvery { settingsRepository.autoCleanupDumps } returns flowOf(true)
        coEvery { settingsRepository.dumpRetentionMonths } returns flowOf(3)
        coEvery { dumpRepository.createDump(1L) } returns Result.success("/path/to/dump.json")

        val useCase = CreateTournamentDumpUseCase(dumpRepository, settingsRepository)
        val result = useCase(1L)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { dumpRepository.createDump(1L) }
        coVerify(exactly = 1) { dumpRepository.cleanupOldDumps(3) }
    }

    @Test
    fun `invoke skips cleanup when autoCleanup is false`() = runTest {
        coEvery { settingsRepository.autoCleanupDumps } returns flowOf(false)
        coEvery { dumpRepository.createDump(2L) } returns Result.success("/path/to/dump2.json")

        val useCase = CreateTournamentDumpUseCase(dumpRepository, settingsRepository)
        val result = useCase(2L)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { dumpRepository.createDump(2L) }
        coVerify(exactly = 0) { dumpRepository.cleanupOldDumps(any()) }
    }
}
