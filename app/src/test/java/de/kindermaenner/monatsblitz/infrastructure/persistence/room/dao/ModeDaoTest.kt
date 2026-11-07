package de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao

import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.ModeEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ModeDaoTest : DaoTest() {

    @Test
    fun insertAndObserveModes_returnsSortedList() = runTest {
        val m1 = modeDao.insert(ModeEntity(name = "3+2", dirty = false))
        val m2 = modeDao.insert(ModeEntity(name = "5+0", dirty = false))

        val modes = modeDao.observeModes().first()
        assertEquals(2, modes.size)
        assertEquals("3+2", modes[0].name)
        assertEquals("5+0", modes[1].name)

        val allModes = modeDao.getAllModes()
        assertEquals(2, allModes.size)
    }

    @Test
    fun findModeByName_isCaseInsensitive() = runTest {
        modeDao.insert(ModeEntity(name = "Blitz 3+2", dirty = false))

        val matchLower = modeDao.findModeByName("blitz 3+2")
        assertNotNull(matchLower)
        assertEquals("Blitz 3+2", matchLower?.name)

        val matchUpper = modeDao.findModeByName("BLITZ 3+2")
        assertNotNull(matchUpper)
        assertEquals("Blitz 3+2", matchUpper?.name)

        val notFound = modeDao.findModeByName("Rapid")
        assertNull(notFound)
    }

    @Test
    fun getModeByRemoteId_findsMatchingMode() = runTest {
        modeDao.insert(ModeEntity(name = "Handicap", remoteId = 42L, dirty = false))

        val found = modeDao.getModeByRemoteId(42L)
        assertNotNull(found)
        assertEquals("Handicap", found?.name)

        val notFound = modeDao.getModeByRemoteId(999L)
        assertNull(notFound)
    }

    @Test
    fun getDirtyModesAndSetRemoteId_updatesState() = runTest {
        val id = modeDao.insert(ModeEntity(name = "10+0", dirty = true, remoteId = null))

        var dirtyModes = modeDao.getDirtyModes()
        assertEquals(1, dirtyModes.size)
        assertEquals("10+0", dirtyModes[0].name)

        modeDao.setRemoteId(id, 88L)

        dirtyModes = modeDao.getDirtyModes()
        assertEquals(0, dirtyModes.size)

        val updated = modeDao.getModeById(id)
        assertNotNull(updated)
        assertEquals(88L, updated?.remoteId)
        assertEquals(false, updated?.dirty)
    }

    @Test
    fun deleteByIdAndClearAll_removesModes() = runTest {
        val id1 = modeDao.insert(ModeEntity(name = "Mode 1"))
        val id2 = modeDao.insert(ModeEntity(name = "Mode 2"))

        modeDao.deleteById(id1)
        assertEquals(1, modeDao.getAllModes().size)
        assertNull(modeDao.getModeById(id1))
        assertNotNull(modeDao.getModeById(id2))

        modeDao.deleteAll()
        assertEquals(0, modeDao.getAllModes().size)
    }
}
