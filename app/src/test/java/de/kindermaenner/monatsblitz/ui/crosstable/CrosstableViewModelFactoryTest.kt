package de.kindermaenner.monatsblitz.ui.crosstable

import androidx.lifecycle.ViewModel
import de.kindermaenner.monatsblitz.domain.repository.TournamentRepository
import de.kindermaenner.monatsblitz.domain.usecase.CalculatePlayerPointsUseCase
import de.kindermaenner.monatsblitz.domain.usecase.CreateTournamentRankingsUseCase
import de.kindermaenner.monatsblitz.domain.usecase.SetGameResultUseCase
import de.kindermaenner.monatsblitz.infrastructure.settings.AppSettingsStorage
import de.kindermaenner.monatsblitz.infrastructure.sync.TournamentSyncManager
import io.mockk.mockk
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Test

class CrosstableViewModelFactoryTest {

    private val tournamentRepository = mockk<TournamentRepository>(relaxed = true)
    private val setGameResultUseCase = mockk<SetGameResultUseCase>(relaxed = true)
    private val createTournamentRankingsUseCase = mockk<CreateTournamentRankingsUseCase>(relaxed = true)
    private val calculatePlayerPointsUseCase = mockk<CalculatePlayerPointsUseCase>(relaxed = true)
    private val tournamentSyncManager = mockk<TournamentSyncManager>(relaxed = true)
    private val appSettingsStorage = mockk<AppSettingsStorage>(relaxed = true)

    @Test
    fun `create should return CrosstableViewModel when modelClass matches`() {
        val factory = CrosstableViewModelFactory(
            tournamentRepository = tournamentRepository,
            setGameResultUseCase = setGameResultUseCase,
            createTournamentRankingsUseCase = createTournamentRankingsUseCase,
            calculatePlayerPointsUseCase = calculatePlayerPointsUseCase,
            tournamentId = 1L,
            tournamentSyncManager = tournamentSyncManager,
            appSettingsStorage = appSettingsStorage
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
