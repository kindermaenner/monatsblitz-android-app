package de.kindermaenner.monatsblitz.ui.crosstable

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import de.kindermaenner.monatsblitz.domain.repository.PlayerRepository
import de.kindermaenner.monatsblitz.domain.repository.SettingsRepository
import de.kindermaenner.monatsblitz.domain.repository.TournamentRepository
import de.kindermaenner.monatsblitz.domain.sync.TournamentSyncService
import de.kindermaenner.monatsblitz.domain.usecase.AddPlayerUseCase
import de.kindermaenner.monatsblitz.domain.usecase.CalculatePlayerPointsUseCase
import de.kindermaenner.monatsblitz.domain.usecase.CreateTournamentRankingsUseCase
import de.kindermaenner.monatsblitz.domain.usecase.SetGameResultUseCase
import de.kindermaenner.monatsblitz.domain.usecase.SyncPlayersUseCase

class CrosstableViewModelFactory(
    private val tournamentRepository: TournamentRepository,
    private val setGameResultUseCase: SetGameResultUseCase,
    private val createTournamentRankingsUseCase: CreateTournamentRankingsUseCase,
    private val calculatePlayerPointsUseCase: CalculatePlayerPointsUseCase,
    private val tournamentId: Long,
    private val tournamentSyncService: TournamentSyncService? = null,
    private val settingsRepository: SettingsRepository? = null,
    private val playerRepository: PlayerRepository? = null,
    private val addPlayerUseCase: AddPlayerUseCase? = null,
    private val syncPlayersUseCase: SyncPlayersUseCase? = null
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
        null,
        null,
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
                tournamentSyncService,
                settingsRepository,
                playerRepository,
                addPlayerUseCase,
                syncPlayersUseCase
            ) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
