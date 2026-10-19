package de.kindermaenner.monatsblitz.ui.ranking

import androidx.lifecycle.ViewModel
import de.kindermaenner.monatsblitz.domain.repository.TournamentRepository
import de.kindermaenner.monatsblitz.domain.usecase.FinalizeTournamentUseCase
import de.kindermaenner.monatsblitz.util.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class RankingViewModelFactoryTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val tournamentRepository = mockk<TournamentRepository>(relaxed = true)
    private val finalizeTournamentUseCase = mockk<FinalizeTournamentUseCase>(relaxed = true)

    @Before
    fun setUp() {
        every { tournamentRepository.observeTournamentRanking(any()) } returns flowOf(emptyList())
    }

    @Test
    fun `create should return RankingViewModel when modelClass matches`() {
        val factory = RankingViewModelFactory(
            tournamentId = 42L,
            tournamentRepository = tournamentRepository,
            finalizeTournamentUseCase = finalizeTournamentUseCase
        )

        val viewModel = factory.create(RankingViewModel::class.java)
        assertNotNull(viewModel)
    }

    @Test
    fun `create should throw IllegalArgumentException for unknown ViewModel class`() {
        val factory = RankingViewModelFactory(
            tournamentId = 42L,
            tournamentRepository = tournamentRepository
        )

        class UnknownViewModel : ViewModel()

        assertThrows(IllegalArgumentException::class.java) {
            factory.create(UnknownViewModel::class.java)
        }
    }
}

