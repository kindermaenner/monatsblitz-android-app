package de.kindermaenner.monatsblitz.ui.tournamentsetup

import androidx.lifecycle.ViewModel
import de.kindermaenner.monatsblitz.domain.repository.PlayerRepository
import de.kindermaenner.monatsblitz.domain.usecase.AddPlayerUseCase
import de.kindermaenner.monatsblitz.domain.usecase.CreateTournamentUseCase
import io.mockk.mockk
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Test

class TournamentSetupViewModelFactoryTest {

    private val playerRepository = mockk<PlayerRepository>(relaxed = true)
    private val createTournamentUseCase = mockk<CreateTournamentUseCase>(relaxed = true)
    private val addPlayerUseCase = mockk<AddPlayerUseCase>(relaxed = true)

    @Test
    fun `create should return TournamentSetupViewModel when modelClass matches`() {
        val factory = TournamentSetupViewModelFactory(
            playerRepository = playerRepository,
            createTournamentUseCase = createTournamentUseCase,
            addPlayerUseCase = addPlayerUseCase
        )

        val viewModel = factory.create(TournamentSetupViewModel::class.java)
        assertNotNull(viewModel)
    }

    @Test
    fun `create should throw IllegalArgumentException for unknown ViewModel class`() {
        val factory = TournamentSetupViewModelFactory(
            playerRepository = playerRepository,
            createTournamentUseCase = createTournamentUseCase,
            addPlayerUseCase = addPlayerUseCase
        )

        class UnknownViewModel : ViewModel()

        assertThrows(IllegalArgumentException::class.java) {
            factory.create(UnknownViewModel::class.java)
        }
    }
}
