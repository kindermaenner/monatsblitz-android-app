package de.kindermaenner.monatsblitz.infrastructure.repository

import de.kindermaenner.monatsblitz.domain.model.NewPlayer
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

    private val playerDao = mockk<PlayerDao>()
    private val repository = PlayerRepositoryImpl(playerDao)

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

        assertEquals("Magnus", result1?.Vorname)
        assertEquals(null, result2)
    }

    @Test
    fun `createPlayer inserts entity and returns domain player`() = runTest {
        val newPlayer = NewPlayer(Name = "Ding", Vorname = "Liren")
        coEvery { playerDao.insert(any()) } returns 3L

        val result = repository.createPlayer(newPlayer)

        assertEquals(3L, result.id)
        assertEquals("Liren Ding", result.fullName)
        coVerify { playerDao.insert(match { it.name == "Ding" && it.vorname == "Liren" }) }
    }
}
