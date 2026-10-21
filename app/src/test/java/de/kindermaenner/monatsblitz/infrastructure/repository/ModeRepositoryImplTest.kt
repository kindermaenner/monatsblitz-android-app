package de.kindermaenner.monatsblitz.infrastructure.repository

import de.kindermaenner.monatsblitz.domain.model.GameMode
import de.kindermaenner.monatsblitz.infrastructure.api.ModeRemoteDataSource
import de.kindermaenner.monatsblitz.infrastructure.api.dto.CreateModeResponseDto
import de.kindermaenner.monatsblitz.infrastructure.api.dto.ModeDto
import de.kindermaenner.monatsblitz.infrastructure.api.dto.NewModeDto
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.ModeDao
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.ModeEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ModeRepositoryImplTest {

    private val modeDao = mockk<ModeDao>(relaxed = true)
    private val remoteDataSource = mockk<ModeRemoteDataSource>(relaxed = true)

    private val repository = ModeRepositoryImpl(
        modeDao = modeDao,
        remoteDataSource = remoteDataSource
    )

    @Test
    fun `observeModes returns DEFAULT_MODES when dao has no entities`() = runTest {
        coEvery { modeDao.observeModes() } returns flowOf(emptyList())

        val modes = repository.observeModes().first()
        assertEquals(GameMode.DEFAULT_MODES, modes)
    }

    @Test
    fun `observeModes returns mapped entities from dao`() = runTest {
        val entities = listOf(
            ModeEntity(id = 1L, remoteId = 10L, name = "3+2", dirty = false),
            ModeEntity(id = 2L, remoteId = 11L, name = "10+0", dirty = false)
        )
        coEvery { modeDao.observeModes() } returns flowOf(entities)

        val modes = repository.observeModes().first()
        assertEquals(2, modes.size)
        assertEquals("3+2", modes[0].name)
        assertEquals("10+0", modes[1].name)
    }

    @Test
    fun `createMode with permanent = false does not save to DB or sync to remote`() = runTest {
        val mode = repository.createMode("Jubiläumsblitz", permanent = false)

        assertEquals("Jubiläumsblitz", mode.name)
        assertEquals(0L, mode.id)
        assertNull(mode.remoteId)
        coVerify(exactly = 0) { modeDao.insert(any()) }
        coVerify(exactly = 0) { remoteDataSource.createMode(any()) }
    }

    @Test
    fun `createMode with permanent = true inserts into DB and syncs with server`() = runTest {
        coEvery { modeDao.findModeByName("10+0") } returns null
        coEvery { modeDao.insert(any()) } returns 5L
        coEvery { remoteDataSource.createMode(NewModeDto("10+0")) } returns CreateModeResponseDto(
            success = true,
            id = 42L,
            name = "10+0"
        )

        val mode = repository.createMode("10+0", permanent = true)

        assertEquals("10+0", mode.name)
        assertEquals(5L, mode.id)
        assertEquals(42L, mode.remoteId)
        assertFalse(mode.dirty)

        coVerify { modeDao.insert(ModeEntity(name = "10+0", dirty = true)) }
        coVerify { modeDao.setRemoteId(5L, 42L) }
    }

    @Test
    fun `createMode with permanent = true returns existing mode if already in DB`() = runTest {
        val existing = ModeEntity(id = 3L, remoteId = 15L, name = "3+2", dirty = false)
        coEvery { modeDao.findModeByName("3+2") } returns existing

        val mode = repository.createMode("3+2", permanent = true)

        assertEquals("3+2", mode.name)
        assertEquals(3L, mode.id)
        assertEquals(15L, mode.remoteId)
        coVerify(exactly = 0) { modeDao.insert(any()) }
        coVerify(exactly = 0) { remoteDataSource.createMode(any()) }
    }

    @Test
    fun `syncModes syncs dirty modes and merges remote modes`() = runTest {
        val dirty = listOf(
            ModeEntity(id = 1L, remoteId = null, name = "7+3", dirty = true)
        )
        coEvery { modeDao.getDirtyModes() } returns dirty
        coEvery { remoteDataSource.createMode(NewModeDto("7+3")) } returns CreateModeResponseDto(
            success = true,
            id = 99L,
            name = "7+3"
        )

        val remoteList = listOf(
            ModeDto(id = 10L, name = "3+2"),
            ModeDto(id = 20L, name = "5+0"),
            ModeDto(id = 30L, name = "Blitz")
        )
        coEvery { remoteDataSource.getModes() } returns remoteList
        coEvery { modeDao.getModeByRemoteId(10L) } returns ModeEntity(id = 1L, remoteId = 10L, name = "3+2", dirty = false)
        coEvery { modeDao.getModeByRemoteId(20L) } returns null
        coEvery { modeDao.findModeByName("5+0") } returns ModeEntity(id = 2L, remoteId = null, name = "5+0", dirty = false)
        coEvery { modeDao.getModeByRemoteId(30L) } returns null
        coEvery { modeDao.findModeByName("Blitz") } returns null

        repository.syncModes()

        coVerify { modeDao.setRemoteId(1L, 99L) }
        coVerify { modeDao.update(ModeEntity(id = 1L, remoteId = 10L, name = "3+2", dirty = false)) }
        coVerify { modeDao.setRemoteId(2L, 20L) }
        coVerify { modeDao.insert(ModeEntity(remoteId = 30L, name = "Blitz", dirty = false)) }
    }
}
