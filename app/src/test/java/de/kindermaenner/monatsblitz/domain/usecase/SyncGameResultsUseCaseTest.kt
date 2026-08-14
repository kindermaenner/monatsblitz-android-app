package de.kindermaenner.monatsblitz.domain.usecase

import de.kindermaenner.monatsblitz.domain.model.GameResult
import de.kindermaenner.monatsblitz.infrastructure.api.MonatsblitzApi
import de.kindermaenner.monatsblitz.infrastructure.api.dto.CreateGamesResponseDto
import de.kindermaenner.monatsblitz.infrastructure.api.dto.GameSyncResponseItemDto
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.GameDao
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.projection.GameSyncData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SyncGameResultsUseCaseTest {

    private val api = mockk<MonatsblitzApi>()
    private val dao = mockk<GameDao>(relaxed = true)
    private val useCase = SyncGameResultsUseCase(api, dao)

    @Test
    fun `invoke syncs dirty games to remote`() = runTest {
        val syncData = GameSyncData(
            localGameId = 1,
            tournamentRemoteId = 100,
            player1Id = 10,
            player2Id = 20,
            leg = 1,
            result = GameResult.Win
        )
        
        coEvery { dao.getGamesForSync() } returns listOf(syncData)
        coEvery { api.createGames(any()) } returns CreateGamesResponseDto(
            success = true, 
            count = 1,
            items = listOf(GameSyncResponseItemDto(success = true, gameId = 1000))
        )

        useCase()

        coVerify { api.createGames(match { it.tournamentId == 100 && it.games.size == 1 }) }
        coVerify { dao.markGameAsSynced(1, 1000) }
    }

    @Test
    fun `invoke should not mark games as synced if api returns success false`() = runTest {
        val syncData = GameSyncData(1, 100, 10, 20, 1, GameResult.Win)
        
        coEvery { dao.getGamesForSync() } returns listOf(syncData)
        coEvery { api.createGames(any()) } returns CreateGamesResponseDto(
            success = false, 
            count = 0,
            items = emptyList()
        )

        useCase()

        coVerify { api.createGames(any()) }
        coVerify(exactly = 0) { dao.markGameAsSynced(any(), any()) }
    }
}
