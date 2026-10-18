package de.kindermaenner.monatsblitz.ui.ranking

import androidx.lifecycle.ViewModel
import de.kindermaenner.monatsblitz.domain.usecase.FinalizeTournamentUseCase
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.PlayerDao
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.TournamentPlayerDao
import io.mockk.mockk
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Test

class RankingViewModelFactoryTest {

    private val tournamentPlayerDao = mockk<TournamentPlayerDao>(relaxed = true)
    private val playerDao = mockk<PlayerDao>(relaxed = true)
    private val finalizeTournamentUseCase = mockk<FinalizeTournamentUseCase>(relaxed = true)

    @Test
    fun `create should return RankingViewModel when modelClass matches`() {
        val factory = RankingViewModelFactory(
            tournamentId = 42L,
            tournamentPlayerDao = tournamentPlayerDao,
            playerDao = playerDao,
            finalizeTournamentUseCase = finalizeTournamentUseCase
        )

        val viewModel = factory.create(RankingViewModel::class.java)
        assertNotNull(viewModel)
    }

    @Test
    fun `create should throw IllegalArgumentException for unknown ViewModel class`() {
        val factory = RankingViewModelFactory(
            tournamentId = 42L,
            tournamentPlayerDao = tournamentPlayerDao,
            playerDao = playerDao
        )

        class UnknownViewModel : ViewModel()

        assertThrows(IllegalArgumentException::class.java) {
            factory.create(UnknownViewModel::class.java)
        }
    }
}
