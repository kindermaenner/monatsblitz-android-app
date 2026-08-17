package de.kindermaenner.monatsblitz.app

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import de.kindermaenner.monatsblitz.BuildConfig.API_KEY
import de.kindermaenner.monatsblitz.domain.repository.PlayerRepository
import de.kindermaenner.monatsblitz.domain.usecase.AddPlayerUseCase
import de.kindermaenner.monatsblitz.domain.usecase.CalculatePlayerPointsUseCase
import de.kindermaenner.monatsblitz.domain.usecase.CreateNewGamesUseCase
import de.kindermaenner.monatsblitz.domain.usecase.CreateTournamentRankingsUseCase
import de.kindermaenner.monatsblitz.domain.usecase.CreateTournamentUseCase
import de.kindermaenner.monatsblitz.domain.usecase.SetGameResultUseCase
import de.kindermaenner.monatsblitz.domain.usecase.SyncGameResultsUseCase
import de.kindermaenner.monatsblitz.domain.usecase.SyncPlayersUseCase
import de.kindermaenner.monatsblitz.domain.usecase.SyncTournamentUseCase
import de.kindermaenner.monatsblitz.infrastructure.TournamentStorage
import de.kindermaenner.monatsblitz.infrastructure.api.PlayerRemoteDataSource
import de.kindermaenner.monatsblitz.infrastructure.api.client.RetrofitClient
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.AppDatabase
import de.kindermaenner.monatsblitz.infrastructure.repository.PlayerRepositoryImpl
import de.kindermaenner.monatsblitz.infrastructure.repository.TournamentRepositoryImpl
import de.kindermaenner.monatsblitz.ui.crosstable.CrosstableViewModelFactory
import de.kindermaenner.monatsblitz.ui.ranking.RankingViewModelFactory
import de.kindermaenner.monatsblitz.ui.root.RootViewModelFactory
import de.kindermaenner.monatsblitz.ui.tournamentsetup.TournamentSetupViewModelFactory

class AppContainer(context: Context) {
    private val Context.dataStore by preferencesDataStore(name = "tournament_state")
    val database = AppDatabase.getInstance(context)

    private val api = RetrofitClient.createApi(API_KEY)

    val playerRepository: PlayerRepository =
        PlayerRepositoryImpl(
            playerDao = database.playerDao()
        )

    val tournamentStorage = TournamentStorage(context.dataStore)
    val tournamentRepository = TournamentRepositoryImpl(
        tournamentDao = database.tournamentDao(),
        tournamentStorage = tournamentStorage,
        gameDao = database.gameDao(),
        database = database,
        tournamentPlayerDao = database.tournamentPlayerDao(),
    )
    val playerRemoteDataSource = PlayerRemoteDataSource(api)

    val syncPlayersUseCase = SyncPlayersUseCase(
        remoteDataSource = playerRemoteDataSource,
        playerDao = database.playerDao())

    val addPlayerUseCase = AddPlayerUseCase(playerRepository)

    val createNewGamesUseCase = CreateNewGamesUseCase()

    val createTournamentUseCase = CreateTournamentUseCase(
        tournamentStorage,
        tournamentRepository,
        createNewGamesUseCase
    )
    val setGameResultUseCase = SetGameResultUseCase(tournamentRepository)

    val syncTournamentUseCase = SyncTournamentUseCase(
        monatsblitzApi = api,
        tournamentDao = database.tournamentDao()
    )

    val syncGameResultsUseCase = SyncGameResultsUseCase(
        monatsblitzApi = api,
        gameDao = database.gameDao()
    )

    val calculatePlayerPointsUseCase = CalculatePlayerPointsUseCase()

    val createTournamentRankingUseCase = CreateTournamentRankingsUseCase(
        tournamentPlayerDao = database.tournamentPlayerDao(),
        calculatePlayerPointsUseCase = calculatePlayerPointsUseCase
    )

    val tournamentSetupViewModelFactory =
        TournamentSetupViewModelFactory(
            playerRepository,
            createTournamentUseCase,
            addPlayerUseCase
        )

    fun crosstableViewModelFactory(tournamentId: Long) =
        CrosstableViewModelFactory(
            tournamentRepository,
            setGameResultUseCase,
            createTournamentRankingUseCase,
            calculatePlayerPointsUseCase,
            tournamentId
        )

    val rootViewModelFactory =
        RootViewModelFactory(
            tournamentStorage,
            syncPlayersUseCase
        )

    fun rankingViewModelFactory(tournamentId: Long) = RankingViewModelFactory(
        tournamentId = tournamentId,
        tournamentPlayerDao = database.tournamentPlayerDao(),
        playerDao = database.playerDao()
    )
}
