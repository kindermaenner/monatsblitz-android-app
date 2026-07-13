package de.kindermaenner.monatsblitz.app

import android.content.Context
import de.kindermaenner.monatsblitz.BuildConfig.API_KEY
import de.kindermaenner.monatsblitz.domain.repository.PlayerRepository
import de.kindermaenner.monatsblitz.domain.usecase.CreateNewGamesUseCase
import de.kindermaenner.monatsblitz.domain.usecase.CreateTournamentUseCase
import de.kindermaenner.monatsblitz.domain.usecase.SyncPlayersUseCase
import de.kindermaenner.monatsblitz.infrastructure.TournamentStorage
import de.kindermaenner.monatsblitz.infrastructure.api.PlayerRemoteDataSource
import de.kindermaenner.monatsblitz.infrastructure.api.client.RetrofitClient
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.AppDatabase
import de.kindermaenner.monatsblitz.infrastructure.repository.PlayerRepositoryImpl
import de.kindermaenner.monatsblitz.infrastructure.repository.SyncPlayerRepositoryImpl
import de.kindermaenner.monatsblitz.infrastructure.repository.TournamentRepositoryImpl
import de.kindermaenner.monatsblitz.ui.viewmodels.HomeViewModelFactory
import de.kindermaenner.monatsblitz.ui.viewmodels.RootViewModelFactory
import de.kindermaenner.monatsblitz.ui.viewmodels.TournamentViewModelFactory

class AppContainer(context: Context) {
    private val database = AppDatabase.getInstance(context)

    private val api = RetrofitClient.createApi(API_KEY)

    val playerRepository: PlayerRepository =
        PlayerRepositoryImpl(
            playerDao = database.playerDao()
        )

    val tournamentStorage = TournamentStorage(context)
    val tournamentRepository = TournamentRepositoryImpl(
        tournamentDao = database.tournamentDao(),
        tournamentStorage = tournamentStorage,
        gameDao = database.gameDao(),
        database = database,
        tournamentPlayerDao = database.tournamentPlayerDao(),
        playerDao = database.playerDao()
    )
    val playerRemoteDataSource = PlayerRemoteDataSource(api)

    val syncPlayerRepository = SyncPlayerRepositoryImpl(
        remoteDataSource = playerRemoteDataSource,
        playerDao = database.playerDao()
    )

    val syncPlayersUseCase = SyncPlayersUseCase(syncPlayerRepository)

    val createNewGamesUseCase = CreateNewGamesUseCase()

    val createTournamentUseCase = CreateTournamentUseCase(
        tournamentStorage,
        tournamentRepository,
        createNewGamesUseCase
    )
    val homeViewModelFactory =
        HomeViewModelFactory(
            playerRepository,
            createTournamentUseCase
        )

    fun tournamentViewModelFactory(tournamentId: Long) =
        TournamentViewModelFactory(
            tournamentRepository,
            tournamentId
        )

    val rootViewModelFactory =
        RootViewModelFactory(
            tournamentStorage,
            syncPlayersUseCase
        )

}
