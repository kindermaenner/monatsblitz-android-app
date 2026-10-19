package de.kindermaenner.monatsblitz.infrastructure.repository

import de.kindermaenner.monatsblitz.domain.model.NewPlayer
import de.kindermaenner.monatsblitz.infrastructure.api.PlayerRemoteDataSource
import de.kindermaenner.monatsblitz.infrastructure.api.dto.PlayerDto
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.PlayerDao
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.PlayerEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerRepositoryImplTest {

    private val playerDao = mockk<PlayerDao>(relaxed = true)
    private val remoteDataSource = mockk<PlayerRemoteDataSource>()
    private val repository = PlayerRepositoryImpl(playerDao, remoteDataSource)

    @Test
    fun `observePlayers returns mapped domain players`() = runTest {
        val entities = listOf(
            PlayerEntity(id = 1, name = "Carlsen", vorname = "Magnus", dirty = false),
            PlayerEntity(id = 2, name = "Nakamura", vorname = "Hikaru", dirty = false)
        )
        coEvery { playerDao.observePlayers() } returns flowOf(entities)

        val result = repository.observePlayers().first()

        assertEquals(2, result.size)
        assertEquals("Magnus Carlsen", result[0].fullName)
        assertEquals("Hikaru Nakamura", result[1].fullName)
    }

    @Test
    fun `observePlayer returns mapped domain player or null`() = runTest {
        val entity = PlayerEntity(id = 1, name = "Carlsen", vorname = "Magnus", dirty = false)
        coEvery { playerDao.observePlayer(1) } returns flowOf(entity)
        coEvery { playerDao.observePlayer(2) } returns flowOf(null)

        val result1 = repository.observePlayer(1).first()
        val result2 = repository.observePlayer(2).first()

        assertEquals("Magnus", result1?.vorname)
        assertEquals(null, result2)
    }

    @Test
    fun `createPlayer inserts entity and returns domain player`() = runTest {
        val newPlayer = NewPlayer(name = "Ding", vorname = "Liren")
        coEvery { playerDao.insert(any()) } returns 3L

        val result = repository.createPlayer(newPlayer)

        assertEquals(3L, result.id)
        assertEquals("Liren Ding", result.fullName)
        coVerify { playerDao.insert(match { it.name == "Ding" && it.vorname == "Liren" }) }
    }

    @Test
    fun `findPlayersByName returns mapped domain players`() = runTest {
        val entities = listOf(
            PlayerEntity(id = 10, name = "Meier", vorname = "Alice", dirty = false)
        )
        coEvery { playerDao.findPlayersByName("Alice", "Meier") } returns entities

        val result = repository.findPlayersByName("Alice", "Meier")

        assertEquals(1, result.size)
        assertEquals("Alice Meier", result[0].fullName)
    }

    @Test
    fun `syncPlayers syncs dirty players to remote and remote players to local`() = runTest {
        val dirtyPlayer = PlayerEntity(id = 1, remoteId = null, name = "N1", vorname = "V1", dirty = true)
        coEvery { playerDao.getDirtyPlayers() } returns listOf(dirtyPlayer)
        coEvery { remoteDataSource.createPlayer(any()) } returns PlayerDto(id = 100, surname = "N1", forename = "V1")

        val remotePlayerDto = PlayerDto(id = 200, surname = "N2", forename = "V2")
        coEvery { remoteDataSource.getPlayers() } returns listOf(remotePlayerDto)
        coEvery { playerDao.getPlayerByRemoteId(200L) } returns null
        coEvery { playerDao.findPlayersByName("V2", "N2") } returns emptyList()

        repository.syncPlayers()

        coVerify { remoteDataSource.createPlayer(match { it.forename == "V1" && it.surname == "N1" }) }
        coVerify { playerDao.setRemoteId(1, 100L) }
        coVerify { playerDao.insert(match { it.remoteId == 200L && it.vorname == "V2" }) }
    }

    @Test
    fun `syncPlayers updates local player when remote player exists by remoteId`() = runTest {
        coEvery { playerDao.getDirtyPlayers() } returns emptyList()
        val remoteDto = PlayerDto(id = 300, surname = "Mustermann", forename = "Max")
        coEvery { remoteDataSource.getPlayers() } returns listOf(remoteDto)
        val existingLocal = PlayerEntity(id = 5, remoteId = 300L, name = "Alt", vorname = "Max", dirty = false)
        coEvery { playerDao.getPlayerByRemoteId(300L) } returns existingLocal

        repository.syncPlayers()

        coVerify { playerDao.update(match { it.id == 5L && it.remoteId == 300L && it.name == "Mustermann" }) }
        coVerify(exactly = 0) { playerDao.insert(any()) }
    }

    @Test
    fun `syncPlayers links remoteId when local player exists by name but has no remoteId`() = runTest {
        coEvery { playerDao.getDirtyPlayers() } returns emptyList()
        val remoteDto = PlayerDto(id = 400, surname = "Schmidt", forename = "Erika")
        coEvery { remoteDataSource.getPlayers() } returns listOf(remoteDto)
        coEvery { playerDao.getPlayerByRemoteId(400L) } returns null
        val existingByName = PlayerEntity(id = 12, remoteId = null, name = "Schmidt", vorname = "Erika", dirty = false)
        coEvery { playerDao.findPlayersByName("Erika", "Schmidt") } returns listOf(existingByName)

        repository.syncPlayers()

        coVerify { playerDao.setRemoteId(playerId = 12L, remoteId = 400L) }
        coVerify(exactly = 0) { playerDao.insert(any()) }
        coVerify(exactly = 0) { playerDao.update(any()) }
    }
}

