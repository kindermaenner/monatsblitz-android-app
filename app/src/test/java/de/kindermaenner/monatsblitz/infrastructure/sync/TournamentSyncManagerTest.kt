package de.kindermaenner.monatsblitz.infrastructure.sync

import de.kindermaenner.monatsblitz.infrastructure.TournamentState
import de.kindermaenner.monatsblitz.domain.usecase.SyncGameResultsUseCase
import de.kindermaenner.monatsblitz.domain.usecase.SyncPlayersUseCase
import de.kindermaenner.monatsblitz.domain.usecase.SyncTournamentUseCase
import de.kindermaenner.monatsblitz.infrastructure.TournamentStorage
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.GameDao
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.TournamentDao
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.GameEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

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
    fun `checkInitialStatus sets FAILED when dirty games exist`() = runTest {
        coEvery { tournamentStorage.getTournamentState() } returns flowOf(TournamentState(tournamentId = 1L, finalized = false))
        coEvery { gameDao.getDirtyGamesForTournament(1L) } returns listOf(mockk<GameEntity>())
        coEvery { tournamentDao.getDirtyTournaments() } returns emptyList()

        syncManager.checkInitialStatus(1L)

        assertEquals(SyncStatus.FAILED, syncManager.syncStatus.value)
    }

    @Test
    fun `checkInitialStatus sets SYNCED when no dirty games exist`() = runTest {
        coEvery { tournamentStorage.getTournamentState() } returns flowOf(TournamentState(tournamentId = 1L, finalized = false))
        coEvery { gameDao.getDirtyGamesForTournament(1L) } returns emptyList()
        coEvery { tournamentDao.getDirtyTournaments() } returns emptyList()

        syncManager.checkInitialStatus(1L)

        assertEquals(SyncStatus.SYNCED, syncManager.syncStatus.value)
    }

    @Test
    fun `triggerSync performs full sync and transitions to SYNCED`() = runTest {
        coEvery { tournamentStorage.getTournamentState() } returns flowOf(TournamentState(tournamentId = 1L, finalized = false))
        coEvery { gameDao.getDirtyGamesForTournament(1L) } returns emptyList()

        syncManager.triggerSync(1L)

        coVerify { syncPlayersUseCase() }
        coVerify { syncTournamentUseCase() }
        coVerify { syncGameResultsUseCase() }
        assertEquals(SyncStatus.SYNCED, syncManager.syncStatus.value)
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
}
