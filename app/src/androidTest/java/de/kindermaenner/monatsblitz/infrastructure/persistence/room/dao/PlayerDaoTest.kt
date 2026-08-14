package de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao

import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.PlayerEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PlayerDaoTest : DaoTest() {

    @Test
    fun insertAndObservePlayers() = runTest {
        val player = PlayerEntity(name = "Meier", vorname = "Alice")
        playerDao.insert(player)

        val players = playerDao.observePlayers().first()
        assertEquals(1, players.size)
        assertEquals("Meier", players[0].name)
    }

    @Test
    fun getPlayersByIds() = runTest {
        val id1 = playerDao.insert(PlayerEntity(name = "P1", vorname = "V1"))
        val id2 = playerDao.insert(PlayerEntity(name = "P2", vorname = "V2"))
        playerDao.insert(PlayerEntity(name = "P3", vorname = "V3"))

        val result = playerDao.getPlayersByIds(listOf(id1, id2))
        assertEquals(2, result.size)
    }

    @Test
    fun getDirtyPlayersAndMarkClean() = runTest {
        val id = playerDao.insert(PlayerEntity(name = "P1", vorname = "V1", dirty = true))
        
        var dirty = playerDao.getDirtyPlayers()
        assertEquals(1, dirty.size)

        playerDao.markPlayerAsClean(id)
        dirty = playerDao.getDirtyPlayers()
        assertEquals(0, dirty.size)
    }

    @Test
    fun setRemoteIdUpdatesFields() = runTest {
        val id = playerDao.insert(PlayerEntity(name = "P1", vorname = "V1", dirty = true))
        
        playerDao.setRemoteId(id, 500L)
        
        val player = playerDao.getPlayerById(id)
        assertNotNull(player)
        assertEquals(500L, player?.remoteId)
        assertEquals(false, player?.dirty)
    }

    @Test
    fun getPlayerByRemoteId() = runTest {
        playerDao.insert(PlayerEntity(name = "P1", vorname = "V1", remoteId = 100L))
        
        val found = playerDao.getPlayerByRemoteId(100L)
        assertNotNull(found)
        assertEquals("P1", found?.name)

        val notFound = playerDao.getPlayerByRemoteId(999L)
        assertNull(notFound)
    }
}
