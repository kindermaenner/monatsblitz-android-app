package de.kindermaenner.monatsblitz.ui.crosstable

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import de.kindermaenner.monatsblitz.domain.repository.TournamentRepository
import de.kindermaenner.monatsblitz.domain.usecase.CalculatePlayerPointsUseCase
import de.kindermaenner.monatsblitz.domain.usecase.CreateTournamentRankingsUseCase
import de.kindermaenner.monatsblitz.domain.usecase.SetGameResultUseCase
import de.kindermaenner.monatsblitz.infrastructure.settings.AppSettingsStorage
import de.kindermaenner.monatsblitz.infrastructure.sync.TournamentSyncManager

class CrosstableViewModelFactory(
    private val tournamentRepository: TournamentRepository,
    private val setGameResultUseCase: SetGameResultUseCase,
    private val createTournamentRankingsUseCase: CreateTournamentRankingsUseCase,
    private val calculatePlayerPointsUseCase: CalculatePlayerPointsUseCase,
    private val tournamentId: Long,
    private val tournamentSyncManager: TournamentSyncManager? = null,
    private val appSettingsStorage: AppSettingsStorage? = null
) : ViewModelProvider.Factory {

    constructor(
        tournamentRepository: TournamentRepository,
        setGameResultUseCase: SetGameResultUseCase,
        createTournamentRankingsUseCase: CreateTournamentRankingsUseCase,
        tournamentId: Long
    ) : this(
        tournamentRepository,
        setGameResultUseCase,
        createTournamentRankingsUseCase,
        CalculatePlayerPointsUseCase(),
        tournamentId,
        null,
        null
    )

    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {
        if (modelClass.isAssignableFrom(CrosstableViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CrosstableViewModel(
                tournamentRepository,
                setGameResultUseCase,
                createTournamentRankingsUseCase,
                calculatePlayerPointsUseCase,
                tournamentId,
                tournamentSyncManager,
                appSettingsStorage
            ) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
