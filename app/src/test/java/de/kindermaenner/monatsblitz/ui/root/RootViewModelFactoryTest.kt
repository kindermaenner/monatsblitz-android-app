package de.kindermaenner.monatsblitz.ui.root

import androidx.lifecycle.ViewModel
import de.kindermaenner.monatsblitz.domain.repository.TournamentRepository
import de.kindermaenner.monatsblitz.domain.usecase.SyncPlayersUseCase
import de.kindermaenner.monatsblitz.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class RootViewModelFactoryTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val tournamentRepository = mockk<TournamentRepository>(relaxed = true)
    private val syncPlayersUseCase = mockk<SyncPlayersUseCase>(relaxed = true)

    @Before
    fun setUp() {
        coEvery { tournamentRepository.getActiveOpenTournamentId() } returns null
    }

    @Test
    fun `create should return RootViewModel when modelClass matches`() {
        val factory = RootViewModelFactory(
            tournamentRepository = tournamentRepository,
            syncPlayersUseCase = syncPlayersUseCase
        )

        val viewModel = factory.create(RootViewModel::class.java)
        assertNotNull(viewModel)
    }

    @Test
    fun `create should throw IllegalArgumentException for unknown ViewModel class`() {
        val factory = RootViewModelFactory(
            tournamentRepository = tournamentRepository,
            syncPlayersUseCase = syncPlayersUseCase
        )

        class UnknownViewModel : ViewModel()

        assertThrows(IllegalArgumentException::class.java) {
            factory.create(UnknownViewModel::class.java)
        }
    }
}

