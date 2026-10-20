package de.kindermaenner.monatsblitz.app

import android.content.Context
import android.net.ConnectivityManager
import androidx.datastore.preferences.preferencesDataStore
import de.kindermaenner.monatsblitz.domain.repository.PlayerRepository
import de.kindermaenner.monatsblitz.domain.repository.SettingsRepository
import de.kindermaenner.monatsblitz.domain.repository.TournamentRepository
import de.kindermaenner.monatsblitz.domain.sync.TournamentSyncService
import de.kindermaenner.monatsblitz.domain.usecase.AddPlayerUseCase
import de.kindermaenner.monatsblitz.domain.usecase.CalculatePlayerPointsUseCase
import de.kindermaenner.monatsblitz.domain.usecase.CreateNewGamesUseCase
import de.kindermaenner.monatsblitz.domain.usecase.CreateTournamentRankingsUseCase
import de.kindermaenner.monatsblitz.domain.usecase.CreateTournamentUseCase
import de.kindermaenner.monatsblitz.domain.usecase.FinalizeTournamentUseCase
import de.kindermaenner.monatsblitz.domain.usecase.SetGameResultUseCase
import de.kindermaenner.monatsblitz.domain.usecase.SyncGameResultsUseCase
import de.kindermaenner.monatsblitz.domain.usecase.SyncPlayersUseCase
import de.kindermaenner.monatsblitz.domain.usecase.SyncTournamentUseCase
import de.kindermaenner.monatsblitz.domain.usecase.TestBackendConnectionUseCase
import de.kindermaenner.monatsblitz.infrastructure.TournamentStorage
import de.kindermaenner.monatsblitz.infrastructure.api.PlayerRemoteDataSource
import de.kindermaenner.monatsblitz.infrastructure.api.client.RetrofitClient
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.AppDatabase
import de.kindermaenner.monatsblitz.infrastructure.repository.PlayerRepositoryImpl
import de.kindermaenner.monatsblitz.infrastructure.repository.TournamentRepositoryImpl
import de.kindermaenner.monatsblitz.infrastructure.settings.AppSettingsStorage
import de.kindermaenner.monatsblitz.infrastructure.sync.NetworkSyncObserver
import de.kindermaenner.monatsblitz.infrastructure.sync.TournamentSyncManager
import de.kindermaenner.monatsblitz.ui.crosstable.CrosstableViewModelFactory
import de.kindermaenner.monatsblitz.ui.ranking.RankingViewModelFactory
import de.kindermaenner.monatsblitz.ui.root.RootViewModelFactory
import de.kindermaenner.monatsblitz.ui.settings.SettingsViewModelFactory
import de.kindermaenner.monatsblitz.ui.tournamentsetup.TournamentSetupViewModelFactory

private val Context.tournamentDataStore by preferencesDataStore(name = "tournament_state")
private val Context.settingsDataStore by preferencesDataStore(name = "app_settings")

class AppContainer(context: Context) {
    val database = AppDatabase.getInstance(context)

    val appSettingsStorage: SettingsRepository = AppSettingsStorage(context.settingsDataStore)
    val appSettingsInternal: AppSettingsStorage = appSettingsStorage as AppSettingsStorage

    private val api = RetrofitClient.createDynamicApi(
        serverUrlProvider = { appSettingsInternal.getServerUrlSync() },
        apiKeyProvider = { appSettingsInternal.getApiKeySync() }
    )
    val playerRemoteDataSource = PlayerRemoteDataSource(api)

    val playerRepository: PlayerRepository =
        PlayerRepositoryImpl(
            playerDao = database.playerDao(),
            remoteDataSource = playerRemoteDataSource
        )

    val tournamentStorage = TournamentStorage(context.tournamentDataStore)
    val tournamentRepository: TournamentRepository = TournamentRepositoryImpl(
        tournamentDao = database.tournamentDao(),
        tournamentPlayerDao = database.tournamentPlayerDao(),
        gameDao = database.gameDao(),
        database = database,
        tournamentStorage = tournamentStorage,
        playerDao = database.playerDao(),
        monatsblitzApi = api
    )

    val syncPlayersUseCase = SyncPlayersUseCase(
        playerRepository = playerRepository
    )

    val addPlayerUseCase = AddPlayerUseCase(playerRepository)

    val createNewGamesUseCase = CreateNewGamesUseCase()

    val createTournamentUseCase = CreateTournamentUseCase(
        tournamentRepository,
        createNewGamesUseCase
    )
    val setGameResultUseCase = SetGameResultUseCase(tournamentRepository)

    val syncTournamentUseCase = SyncTournamentUseCase(
        tournamentRepository = tournamentRepository
    )

    val syncGameResultsUseCase = SyncGameResultsUseCase(
        tournamentRepository = tournamentRepository
    )

    val calculatePlayerPointsUseCase = CalculatePlayerPointsUseCase()

    val createTournamentRankingUseCase = CreateTournamentRankingsUseCase(
        tournamentRepository = tournamentRepository,
        calculatePlayerPointsUseCase = calculatePlayerPointsUseCase
    )

    val finalizeTournamentUseCase = FinalizeTournamentUseCase(
        tournamentRepository = tournamentRepository,
        syncTournamentUseCase = syncTournamentUseCase,
        syncGameResultsUseCase = syncGameResultsUseCase,
        syncPlayersUseCase = syncPlayersUseCase,
        createTournamentRankingsUseCase = createTournamentRankingUseCase
    )

    val testBackendConnectionUseCase = TestBackendConnectionUseCase()

    val tournamentSyncManager: TournamentSyncService = TournamentSyncManager(
        tournamentStorage = tournamentStorage,
        tournamentDao = database.tournamentDao(),
        gameDao = database.gameDao(),
        syncTournamentUseCase = syncTournamentUseCase,
        syncGameResultsUseCase = syncGameResultsUseCase,
        syncPlayersUseCase = syncPlayersUseCase
    )

    val networkSyncObserver = NetworkSyncObserver(
        connectivityManager = context.getSystemService(ConnectivityManager::class.java),
        tournamentStorage = tournamentStorage,
        tournamentSyncService = tournamentSyncManager
    ).also { it.start() }

    val tournamentSetupViewModelFactory =
        TournamentSetupViewModelFactory(
            playerRepository,
            createTournamentUseCase,
            addPlayerUseCase,
            syncPlayersUseCase,
            appSettingsStorage
        )

    fun crosstableViewModelFactory(tournamentId: Long) =
        CrosstableViewModelFactory(
            tournamentRepository,
            setGameResultUseCase,
            createTournamentRankingUseCase,
            calculatePlayerPointsUseCase,
            tournamentId,
            tournamentSyncManager,
            appSettingsStorage
        )

    val rootViewModelFactory =
        RootViewModelFactory(
            tournamentRepository = tournamentRepository,
            syncPlayersUseCase = syncPlayersUseCase,
            settingsRepository = appSettingsStorage
        )

    fun rankingViewModelFactory(tournamentId: Long) = RankingViewModelFactory(
        tournamentId = tournamentId,
        tournamentRepository = tournamentRepository,
        finalizeTournamentUseCase = finalizeTournamentUseCase,
        tournamentSyncService = tournamentSyncManager
    )

    val settingsViewModelFactory =
        SettingsViewModelFactory(
            settingsRepository = appSettingsStorage,
            testBackendConnectionUseCase = testBackendConnectionUseCase,
            syncPlayersUseCase = syncPlayersUseCase
        )
}
