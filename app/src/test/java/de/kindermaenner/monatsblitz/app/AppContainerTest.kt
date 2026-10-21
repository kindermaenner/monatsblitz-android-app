package de.kindermaenner.monatsblitz.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.AppDatabase
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppContainerTest {

    @Test
    fun appContainer_initializesAllPropertiesAndFactories() {
        val context = ApplicationProvider.getApplicationContext<Context>()
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
        assertNotNull(container.settingsViewModelFactory)
        assertNotNull(container.networkSyncObserver)

        container.networkSyncObserver.stop()
    }

    @Test
    fun appDatabase_getInstance_returnsSingleton() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db1 = AppDatabase.getInstance(context)
        val db2 = AppDatabase.getInstance(context)
        assertNotNull(db1)
        assertSame(db1, db2)
    }
}
