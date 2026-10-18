package de.kindermaenner.monatsblitz.domain.usecase

import de.kindermaenner.monatsblitz.infrastructure.api.PlayerRemoteDataSource
import de.kindermaenner.monatsblitz.infrastructure.api.dto.PlayerDto
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.PlayerDao
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.PlayerEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SyncPlayersUseCaseTest {

    private val remoteDataSource = mockk<PlayerRemoteDataSource>()
    private val playerDao = mockk<PlayerDao>(relaxed = true)
    private val useCase = SyncPlayersUseCase(remoteDataSource, playerDao)

    @Test
    fun `invoke syncs dirty players to remote and remote players to local`() = runTest {
        // 1. Setup dirty players
        val dirtyPlayer = PlayerEntity(id = 1, remoteId = null, name = "N1", vorname = "V1", dirty = true)
        coEvery { playerDao.getDirtyPlayers() } returns listOf(dirtyPlayer)
        coEvery { remoteDataSource.createPlayer(any()) } returns PlayerDto(id = 100, surname = "N1", forename = "V1")

        // 2. Setup remote players
        val remotePlayerDto = PlayerDto(id = 200, surname = "N2", forename = "V2")
        coEvery { remoteDataSource.getPlayers() } returns listOf(remotePlayerDto)
        coEvery { playerDao.getPlayerByRemoteId(200L) } returns null

        useCase()

        // Verify local to remote sync
        coVerify { remoteDataSource.createPlayer(match { it.forename == "V1" && it.surname == "N1" }) }
        coVerify { playerDao.setRemoteId(1, 100L) }

        // Verify remote to local sync
        coVerify { playerDao.insert(match { it.remoteId == 200L && it.vorname == "V2" }) }
    }

    @Test
    fun `invoke with no dirty players should only fetch remote players`() = runTest {
        coEvery { playerDao.getDirtyPlayers() } returns emptyList()
        coEvery { remoteDataSource.getPlayers() } returns emptyList()

        useCase()

        coVerify(exactly = 0) { remoteDataSource.createPlayer(any()) }
        coVerify { remoteDataSource.getPlayers() }
    }

    @Test(expected = Exception::class)
    fun `invoke should rethrow exception if network fails`() = runTest {
        coEvery { playerDao.getDirtyPlayers() } throws Exception("Network error")
        
        useCase()
    }

    @Test
    fun `invoke updates local player when remote player exists by remoteId`() = runTest {
        coEvery { playerDao.getDirtyPlayers() } returns emptyList()
        val remoteDto = PlayerDto(id = 300, surname = "Mustermann", forename = "Max")
        coEvery { remoteDataSource.getPlayers() } returns listOf(remoteDto)
        val existingLocal = PlayerEntity(id = 5, remoteId = 300L, name = "Alt", vorname = "Max", dirty = false)
        coEvery { playerDao.getPlayerByRemoteId(300L) } returns existingLocal

        useCase()

        coVerify { playerDao.update(match { it.id == 5L && it.remoteId == 300L && it.name == "Mustermann" }) }
        coVerify(exactly = 0) { playerDao.insert(any()) }
    }

    @Test
    fun `invoke links remoteId when local player exists by name but has no remoteId`() = runTest {
        coEvery { playerDao.getDirtyPlayers() } returns emptyList()
        val remoteDto = PlayerDto(id = 400, surname = "Schmidt", forename = "Erika")
        coEvery { remoteDataSource.getPlayers() } returns listOf(remoteDto)
        coEvery { playerDao.getPlayerByRemoteId(400L) } returns null
        val existingByName = PlayerEntity(id = 12, remoteId = null, name = "Schmidt", vorname = "Erika", dirty = false)
        coEvery { playerDao.findPlayersByName("Erika", "Schmidt") } returns listOf(existingByName)

        useCase()

        coVerify { playerDao.setRemoteId(playerId = 12L, remoteId = 400L) }
        coVerify(exactly = 0) { playerDao.insert(any()) }
        coVerify(exactly = 0) { playerDao.update(any()) }
    }
}
