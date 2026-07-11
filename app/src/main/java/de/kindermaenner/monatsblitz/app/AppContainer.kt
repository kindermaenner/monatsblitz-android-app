package de.kindermaenner.monatsblitz.app

import android.content.Context
import de.kindermaenner.monatsblitz.BuildConfig.API_KEY
import de.kindermaenner.monatsblitz.domain.repository.PlayerRepository
import de.kindermaenner.monatsblitz.infrastructure.TournamentStorage
import de.kindermaenner.monatsblitz.infrastructure.api.client.RetrofitClient
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.AppDatabase
import de.kindermaenner.monatsblitz.infrastructure.repository.GameRepositoryImpl
import de.kindermaenner.monatsblitz.infrastructure.repository.PlayerRepositoryImpl
import de.kindermaenner.monatsblitz.infrastructure.repository.TournamentRepositoryImpl
import de.kindermaenner.monatsblitz.ui.viewmodels.HomeViewModel
import de.kindermaenner.monatsblitz.ui.viewmodels.HomeViewModelFactory
import de.kindermaenner.monatsblitz.ui.viewmodels.RootViewModelFactory
import de.kindermaenner.monatsblitz.ui.viewmodels.TournamentViewModelFactory

class AppContainer(context: Context) {
    private val database = AppDatabase.getInstance(context)

    private val api = RetrofitClient.createApi(API_KEY)

    val playerRepository: PlayerRepository =
        PlayerRepositoryImpl(
            api = api,
            playerDao = database.playerDao()
        )

    val tournamentStorage = TournamentStorage(context)
    val gameRepository = GameRepositoryImpl(
        gameDao = database.gameDao()
    )
    val tournamentRepository = TournamentRepositoryImpl(
        tournamentDao = database.tournamentDao(),
        tournamentPlayerDao = database.tournamentPlayerDao(),
        playerDao = database.playerDao(),
        gameDao = database.gameDao(),
        api = api,
    )

    val homeViewModelFactory =
        HomeViewModelFactory(
            playerRepository,
            tournamentRepository,
            tournamentStorage
        )


    fun tournamentViewModelFactory(tournamentId: Int) =
        TournamentViewModelFactory(
            tournamentRepository,
            tournamentId
        )


    val rootViewModelFactory =
        RootViewModelFactory(
            tournamentStorage,
            playerRepository
        )

}
