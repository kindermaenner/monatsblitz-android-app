package de.kindermaenner.monatsblitz.domain.usecase

import de.kindermaenner.monatsblitz.domain.repository.PlayerRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SyncPlayersUseCaseTest {

    private val playerRepository = mockk<PlayerRepository>(relaxed = true)
    private val useCase = SyncPlayersUseCase(playerRepository)

    @Test
    fun `invoke delegates to playerRepository syncPlayers`() = runTest {
        useCase()

        coVerify(exactly = 1) { playerRepository.syncPlayers() }
    }

    @Test(expected = Exception::class)
    fun `invoke rethrows exception if repository sync fails`() = runTest {
        coEvery { playerRepository.syncPlayers() } throws Exception("Network error")

        useCase()
    }
}

