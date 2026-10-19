package de.kindermaenner.monatsblitz.infrastructure.sync

import de.kindermaenner.monatsblitz.domain.model.GameMode
import de.kindermaenner.monatsblitz.domain.model.SyncStatus
import de.kindermaenner.monatsblitz.domain.usecase.SyncGameResultsUseCase
import de.kindermaenner.monatsblitz.domain.usecase.SyncPlayersUseCase
import de.kindermaenner.monatsblitz.domain.usecase.SyncTournamentUseCase
import de.kindermaenner.monatsblitz.infrastructure.TournamentState
import de.kindermaenner.monatsblitz.infrastructure.TournamentStorage
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.GameDao
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.TournamentDao
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.GameEntity
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.TournamentEntity
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.relation.TournamentWithDetails
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.time.LocalDate

class TournamentSyncManagerTest {

    private val tournamentStorage = mockk<TournamentStorage>(relaxed = true)
    private val tournamentDao = mockk<TournamentDao>(relaxed = true)
    private val gameDao = mockk<GameDao>(relaxed = true)
    private val syncTournamentUseCase = mockk<SyncTournamentUseCase>(relaxed = true)
    private val syncGameResultsUseCase = mockk<SyncGameResultsUseCase>(relaxed = true)
    private val syncPlayersUseCase = mockk<SyncPlayersUseCase>(relaxed = true)

    private val syncManager = TournamentSyncManager(
        tournamentStorage = tournamentStorage,
        tournamentDao = tournamentDao,
        gameDao = gameDao,
        syncTournamentUseCase = syncTournamentUseCase,
        syncGameResultsUseCase = syncGameResultsUseCase,
        syncPlayersUseCase = syncPlayersUseCase
    )

    @Test
    fun `checkInitialStatus sets FINALIZED when tournamentStorage is finalized`() = runTest {
        coEvery { tournamentStorage.getTournamentState() } returns flowOf(TournamentState(tournamentId = 1L, finalized = true))

        syncManager.checkInitialStatus(1L)

        assertEquals(SyncStatus.FINALIZED, syncManager.syncStatus.value)
    }

    @Test
    fun `checkInitialStatus triggers sync and sets FAILED when dirty games remain unsynced`() = runTest {
        coEvery { tournamentStorage.getTournamentState() } returns flowOf(TournamentState(tournamentId = 1L, finalized = false))
        val syncedTournament = TournamentEntity(id = 1L, mode = GameMode.BLITZ_3_2, date = LocalDate.now(), rounds = 1, remoteId = 100L)
        coEvery { tournamentDao.getTournament(1L) } returns TournamentWithDetails(syncedTournament, emptyList(), emptyList())
        coEvery { gameDao.getDirtyGamesForTournament(1L) } returns listOf(mockk<GameEntity>())

        syncManager.checkInitialStatus(1L)

        coVerify { syncGameResultsUseCase() }
        assertEquals(SyncStatus.FAILED, syncManager.syncStatus.value)
    }

    @Test
    fun `checkInitialStatus triggers initial sync for new unsynced tournament and sets SYNCED on success`() = runTest {
        coEvery { tournamentStorage.getTournamentState() } returns flowOf(TournamentState(tournamentId = 1L, finalized = false))
        coEvery { gameDao.getDirtyGamesForTournament(1L) } returns emptyList()
        val unsyncedEntity = TournamentEntity(id = 1L, mode = GameMode.BLITZ_3_2, date = LocalDate.now(), rounds = 1, remoteId = null)
        val syncedEntity = unsyncedEntity.copy(remoteId = 500L)
        coEvery { tournamentDao.getTournament(1L) } returnsMany listOf(
            TournamentWithDetails(unsyncedEntity, emptyList(), emptyList()),
            TournamentWithDetails(syncedEntity, emptyList(), emptyList())
        )

        syncManager.checkInitialStatus(1L)

        coVerify { syncPlayersUseCase() }
        coVerify { syncTournamentUseCase() }
        assertEquals(SyncStatus.SYNCED, syncManager.syncStatus.value)
    }

    @Test
    fun `checkInitialStatus sets FAILED when initial tournament sync fails to obtain remoteId`() = runTest {
        coEvery { tournamentStorage.getTournamentState() } returns flowOf(TournamentState(tournamentId = 1L, finalized = false))
        coEvery { gameDao.getDirtyGamesForTournament(1L) } returns emptyList()
        val unsyncedEntity = TournamentEntity(id = 1L, mode = GameMode.BLITZ_3_2, date = LocalDate.now(), rounds = 1, remoteId = null)
        coEvery { tournamentDao.getTournament(1L) } returns TournamentWithDetails(unsyncedEntity, emptyList(), emptyList())

        syncManager.checkInitialStatus(1L)

        coVerify { syncTournamentUseCase() }
        assertEquals(SyncStatus.FAILED, syncManager.syncStatus.value)
    }

    @Test
    fun `checkInitialStatus sets SYNCED without triggering network sync when tournament is already synced and no dirty games exist`() = runTest {
        coEvery { tournamentStorage.getTournamentState() } returns flowOf(TournamentState(tournamentId = 1L, finalized = false))
        val syncedEntity = TournamentEntity(id = 1L, mode = GameMode.BLITZ_3_2, date = LocalDate.now(), rounds = 1, remoteId = 100L)
        coEvery { tournamentDao.getTournament(1L) } returns TournamentWithDetails(syncedEntity, emptyList(), emptyList())
        coEvery { gameDao.getDirtyGamesForTournament(1L) } returns emptyList()

        syncManager.checkInitialStatus(1L)

        coVerify(exactly = 0) { syncTournamentUseCase() }
        assertEquals(SyncStatus.SYNCED, syncManager.syncStatus.value)
    }

    @Test
    fun `triggerSync returns early when status is already FINALIZED`() = runTest {
        coEvery { tournamentStorage.getTournamentState() } returns flowOf(TournamentState(tournamentId = 1L, finalized = true))
        syncManager.checkInitialStatus(1L)

        syncManager.triggerSync(1L)

        coVerify(exactly = 0) { syncPlayersUseCase() }
    }

    @Test
    fun `triggerSync transitions to FINALIZED when state is finalized inside lock`() = runTest {
        coEvery { tournamentStorage.getTournamentState() } returns flowOf(TournamentState(tournamentId = 1L, finalized = true))

        syncManager.triggerSync(1L)

        assertEquals(SyncStatus.FINALIZED, syncManager.syncStatus.value)
        coVerify(exactly = 0) { syncPlayersUseCase() }
    }

    @Test
    fun `triggerSync performs full sync and transitions to SYNCED`() = runTest {
        coEvery { tournamentStorage.getTournamentState() } returns flowOf(TournamentState(tournamentId = 1L, finalized = false))
        val syncedEntity = TournamentEntity(id = 1L, mode = GameMode.BLITZ_3_2, date = LocalDate.now(), rounds = 1, remoteId = 100L)
        coEvery { tournamentDao.getTournament(1L) } returns TournamentWithDetails(syncedEntity, emptyList(), emptyList())
        coEvery { gameDao.getDirtyGamesForTournament(1L) } returns emptyList()

        syncManager.triggerSync(1L)

        coVerify { syncPlayersUseCase() }
        coVerify { syncTournamentUseCase() }
        coVerify { syncGameResultsUseCase() }
        assertEquals(SyncStatus.SYNCED, syncManager.syncStatus.value)
    }

    @Test
    fun `triggerSync transitions to FAILED when remaining dirty games exist`() = runTest {
        coEvery { tournamentStorage.getTournamentState() } returns flowOf(TournamentState(tournamentId = 1L, finalized = false))
        coEvery { gameDao.getDirtyGamesForTournament(1L) } returns listOf(mockk<GameEntity>())

        syncManager.triggerSync(1L)

        assertEquals(SyncStatus.FAILED, syncManager.syncStatus.value)
    }

    @Test
    fun `triggerSync transitions to FAILED when an exception is thrown`() = runTest {
        coEvery { tournamentStorage.getTournamentState() } returns flowOf(TournamentState(tournamentId = 1L, finalized = false))
        coEvery { syncGameResultsUseCase() } throws RuntimeException("Network error")

        syncManager.triggerSync(1L)

        assertEquals(SyncStatus.FAILED, syncManager.syncStatus.value)
    }

    @Test
    fun `triggerSync transitions to FINALIZED when HTTP 403 occurs`() = runTest {
        coEvery { tournamentStorage.getTournamentState() } returns flowOf(TournamentState(tournamentId = 1L, finalized = false))
        val errorResponse = Response.error<Any>(
            403,
            "Forbidden".toResponseBody("application/json".toMediaType())
        )
        coEvery { syncGameResultsUseCase() } throws HttpException(errorResponse)

        syncManager.triggerSync(1L)

        coVerify { tournamentStorage.finalizeTournament() }
        assertEquals(SyncStatus.FINALIZED, syncManager.syncStatus.value)
    }

    @Test
    fun `triggerSync transitions to FAILED when HTTP error is 500`() = runTest {
        coEvery { tournamentStorage.getTournamentState() } returns flowOf(TournamentState(tournamentId = 1L, finalized = false))
        val errorResponse = Response.error<Any>(
            500,
            "Internal Server Error".toResponseBody("application/json".toMediaType())
        )
        coEvery { syncGameResultsUseCase() } throws HttpException(errorResponse)

        syncManager.triggerSync(1L)

        coVerify(exactly = 0) { tournamentStorage.finalizeTournament() }
        assertEquals(SyncStatus.FAILED, syncManager.syncStatus.value)
    }

    @Test
    fun `triggerSync completes in syncScope even when caller coroutine is cancelled`() = runTest {
        coEvery { tournamentStorage.getTournamentState() } returns flowOf(TournamentState(tournamentId = 1L, finalized = false))
        val syncedEntity = TournamentEntity(id = 1L, mode = GameMode.BLITZ_3_2, date = LocalDate.now(), rounds = 1, remoteId = 100L)
        coEvery { tournamentDao.getTournament(1L) } returns TournamentWithDetails(syncedEntity, emptyList(), emptyList())
        coEvery { gameDao.getDirtyGamesForTournament(1L) } returns emptyList()

        val startedGate = kotlinx.coroutines.CompletableDeferred<Unit>()
        val finishGate = kotlinx.coroutines.CompletableDeferred<Unit>()
        coEvery { syncPlayersUseCase() } coAnswers {
            startedGate.complete(Unit)
            finishGate.await()
        }

        val callerJob = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
            syncManager.triggerSync(1L)
        }

        startedGate.await()
        callerJob.cancel()
        finishGate.complete(Unit)

        // Wait briefly for syncScope to finish
        val deadline = System.currentTimeMillis() + 2000
        while (syncManager.syncStatus.value == SyncStatus.SYNCING && System.currentTimeMillis() < deadline) {
            kotlinx.coroutines.delay(10)
        }

        coVerify { syncTournamentUseCase() }
        coVerify { syncGameResultsUseCase() }
        assertEquals(SyncStatus.SYNCED, syncManager.syncStatus.value)
    }
}
