package de.kindermaenner.monatsblitz.domain.usecase

import android.util.Log
import de.kindermaenner.monatsblitz.domain.model.GameResult
import de.kindermaenner.monatsblitz.domain.repository.TournamentRepository
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class SetGameResultUseCaseTest {

    private val repository = mockk<TournamentRepository>(relaxed = true)
    private val useCase = SetGameResultUseCase(repository)

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.i(any(), any()) } returns 0
    }

    @Test
    fun `invoke updates both directions of a game result`() = runTest {
        val tournamentId = 1L
        val p1 = 10L
        val p2 = 20L
        val leg = 1
        val result = GameResult.Win

        useCase(tournamentId, p1, p2, leg, result)

        coVerify { repository.updateGameResult(tournamentId, p1, p2, leg, GameResult.Win) }
        coVerify { repository.updateGameResult(tournamentId, p2, p1, leg, GameResult.Loss) }
    }
}
