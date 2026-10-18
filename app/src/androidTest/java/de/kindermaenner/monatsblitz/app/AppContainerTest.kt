package de.kindermaenner.monatsblitz.app

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertNotNull
import org.junit.Test

class AppContainerTest {

    @Test
    fun appContainer_initializesAllPropertiesAndFactories() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val container = AppContainer(context)

        assertNotNull(container.database)
        assertNotNull(container.playerRepository)
        assertNotNull(container.tournamentStorage)
        assertNotNull(container.tournamentRepository)
        assertNotNull(container.playerRemoteDataSource)
        assertNotNull(container.syncPlayersUseCase)
        assertNotNull(container.addPlayerUseCase)
        assertNotNull(container.createNewGamesUseCase)
        assertNotNull(container.createTournamentUseCase)
        assertNotNull(container.setGameResultUseCase)
        assertNotNull(container.syncTournamentUseCase)
        assertNotNull(container.syncGameResultsUseCase)
        assertNotNull(container.calculatePlayerPointsUseCase)
        assertNotNull(container.createTournamentRankingUseCase)
        assertNotNull(container.finalizeTournamentUseCase)
        assertNotNull(container.appSettingsStorage)
        assertNotNull(container.tournamentSyncManager)
        assertNotNull(container.tournamentSetupViewModelFactory)
        assertNotNull(container.rootViewModelFactory)

        val crosstableFactory = container.crosstableViewModelFactory(1L)
        assertNotNull(crosstableFactory)

        val rankingFactory = container.rankingViewModelFactory(1L)
        assertNotNull(rankingFactory)
    }

    @Test
    fun appDatabase_getInstance_returnsSingleton() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db1 = de.kindermaenner.monatsblitz.infrastructure.persistence.room.AppDatabase.getInstance(context)
        val db2 = de.kindermaenner.monatsblitz.infrastructure.persistence.room.AppDatabase.getInstance(context)
        assertNotNull(db1)
        org.junit.Assert.assertSame(db1, db2)
    }
}
