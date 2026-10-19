package de.kindermaenner.monatsblitz.ui.crosstable

import androidx.lifecycle.ViewModel
import de.kindermaenner.monatsblitz.domain.repository.SettingsRepository
import de.kindermaenner.monatsblitz.domain.repository.TournamentRepository
import de.kindermaenner.monatsblitz.domain.sync.TournamentSyncService
import de.kindermaenner.monatsblitz.domain.usecase.CalculatePlayerPointsUseCase
import de.kindermaenner.monatsblitz.domain.usecase.CreateTournamentRankingsUseCase
import de.kindermaenner.monatsblitz.domain.usecase.SetGameResultUseCase
import de.kindermaenner.monatsblitz.util.MainDispatcherRule
import io.mockk.mockk
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test

class CrosstableViewModelFactoryTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val tournamentRepository = mockk<TournamentRepository>(relaxed = true)
    private val setGameResultUseCase = mockk<SetGameResultUseCase>(relaxed = true)
    private val createTournamentRankingsUseCase = mockk<CreateTournamentRankingsUseCase>(relaxed = true)
    private val calculatePlayerPointsUseCase = mockk<CalculatePlayerPointsUseCase>(relaxed = true)
    private val tournamentSyncService = mockk<TournamentSyncService>(relaxed = true)
    private val settingsRepository = mockk<SettingsRepository>(relaxed = true)

    @Test
    fun `create should return CrosstableViewModel when modelClass matches`() {
        val factory = CrosstableViewModelFactory(
            tournamentRepository = tournamentRepository,
            setGameResultUseCase = setGameResultUseCase,
            createTournamentRankingsUseCase = createTournamentRankingsUseCase,
            calculatePlayerPointsUseCase = calculatePlayerPointsUseCase,
            tournamentId = 1L,
            tournamentSyncService = tournamentSyncService,
            settingsRepository = settingsRepository
        )

        val viewModel = factory.create(CrosstableViewModel::class.java)
        assertNotNull(viewModel)
    }

    @Test
    fun `secondary constructor should initialize and create CrosstableViewModel`() {
        val factory = CrosstableViewModelFactory(
            tournamentRepository = tournamentRepository,
            setGameResultUseCase = setGameResultUseCase,
            createTournamentRankingsUseCase = createTournamentRankingsUseCase,
            tournamentId = 2L
        )

        val viewModel = factory.create(CrosstableViewModel::class.java)
        assertNotNull(viewModel)
    }

    @Test
    fun `create should throw IllegalArgumentException for unknown ViewModel class`() {
        val factory = CrosstableViewModelFactory(
            tournamentRepository = tournamentRepository,
            setGameResultUseCase = setGameResultUseCase,
            createTournamentRankingsUseCase = createTournamentRankingsUseCase,
            tournamentId = 1L
        )

        class UnknownViewModel : ViewModel()

        assertThrows(IllegalArgumentException::class.java) {
            factory.create(UnknownViewModel::class.java)
        }
    }
}
