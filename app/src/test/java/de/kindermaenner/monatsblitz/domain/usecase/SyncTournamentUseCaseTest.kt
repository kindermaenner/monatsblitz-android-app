package de.kindermaenner.monatsblitz.domain.usecase

import de.kindermaenner.monatsblitz.domain.repository.TournamentRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SyncTournamentUseCaseTest {

    private val tournamentRepository = mockk<TournamentRepository>(relaxed = true)
    private val useCase = SyncTournamentUseCase(tournamentRepository)

    @Test
    fun `invoke delegates to tournamentRepository syncDirtyTournaments`() = runTest {
        useCase()

        coVerify(exactly = 1) { tournamentRepository.syncDirtyTournaments() }
    }

    @Test(expected = Exception::class)
    fun `invoke rethrows exception if repository sync fails`() = runTest {
        coEvery { tournamentRepository.syncDirtyTournaments() } throws Exception("Network error")

        useCase()
    }
}

