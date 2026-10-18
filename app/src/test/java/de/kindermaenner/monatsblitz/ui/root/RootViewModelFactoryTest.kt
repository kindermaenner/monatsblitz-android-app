package de.kindermaenner.monatsblitz.ui.root

import androidx.lifecycle.ViewModel
import de.kindermaenner.monatsblitz.domain.usecase.SyncPlayersUseCase
import de.kindermaenner.monatsblitz.infrastructure.TournamentStorage
import de.kindermaenner.monatsblitz.infrastructure.api.MonatsblitzApi
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.TournamentDao
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class RootViewModelFactoryTest {

    private val tournamentStorage = mockk<TournamentStorage>(relaxed = true)
    private val syncPlayersUseCase = mockk<SyncPlayersUseCase>(relaxed = true)
    private val tournamentDao = mockk<TournamentDao>(relaxed = true)
    private val monatsblitzApi = mockk<MonatsblitzApi>(relaxed = true)

    @Before
    fun setUp() {
        every { tournamentStorage.getTournamentState() } returns flowOf(null)
    }

    @Test
    fun `create should return RootViewModel when modelClass matches`() {
        val factory = RootViewModelFactory(
            tournamentPreferences = tournamentStorage,
            syncPlayersUseCase = syncPlayersUseCase,
            tournamentDao = tournamentDao,
            monatsblitzApi = monatsblitzApi
        )

        val viewModel = factory.create(RootViewModel::class.java)
        assertNotNull(viewModel)
    }

    @Test
    fun `create should throw IllegalArgumentException for unknown ViewModel class`() {
        val factory = RootViewModelFactory(
            tournamentPreferences = tournamentStorage,
            syncPlayersUseCase = syncPlayersUseCase
        )

        class UnknownViewModel : ViewModel()

        assertThrows(IllegalArgumentException::class.java) {
            factory.create(UnknownViewModel::class.java)
        }
    }
}
