package de.kindermaenner.monatsblitz.domain.usecase

import de.kindermaenner.monatsblitz.domain.repository.ModeRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SyncModesUseCaseTest {

    private val modeRepository = mockk<ModeRepository>()
    private val useCase = SyncModesUseCase(modeRepository)

    @Test
    fun `invoke delegates to modeRepository syncModes`() = runTest {
        coEvery { modeRepository.syncModes() } returns Unit

        useCase()

        coVerify(exactly = 1) { modeRepository.syncModes() }
    }
}
