package de.kindermaenner.monatsblitz.domain.usecase

import de.kindermaenner.monatsblitz.domain.repository.TournamentRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SyncGameResultsUseCaseTest {

    private val tournamentRepository = mockk<TournamentRepository>(relaxed = true)
    private val useCase = SyncGameResultsUseCase(tournamentRepository)

    @Test
    fun `invoke delegates to tournamentRepository syncDirtyGames`() = runTest {
        useCase()

        coVerify(exactly = 1) { tournamentRepository.syncDirtyGames() }
    }

    @Test(expected = Exception::class)
    fun `invoke rethrows exception if repository sync fails`() = runTest {
        coEvery { tournamentRepository.syncDirtyGames() } throws Exception("Network error")

        useCase()
    }
}

