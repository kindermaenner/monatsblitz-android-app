package de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao

import de.kindermaenner.monatsblitz.domain.model.GameMode
import de.kindermaenner.monatsblitz.domain.model.GameResult
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.GameEntity
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.PlayerEntity
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.TournamentEntity
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.TournamentPlayerCrossRef
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class TournamentDaoTest : DaoTest() {

    @Test
    fun insertAndGetTournamentWithDetails() = runTest {
        val date = LocalDate.of(2026, 8, 14)
        val tournamentId = tournamentDao.insert(TournamentEntity(mode = GameMode.BLITZ_3_2, date = date, rounds = 1, remoteId = null, isLocalOnly = true))
        
        val p1 = playerDao.insert(PlayerEntity(name = "P1", vorname = "V1"))
        val p2 = playerDao.insert(PlayerEntity(name = "P2", vorname = "V2"))
        
        tournamentPlayerDao.insertAll(listOf(
            TournamentPlayerCrossRef(tournamentId, p1),
            TournamentPlayerCrossRef(tournamentId, p2)
        ))

        gameDao.insert(GameEntity(tournamentId = tournamentId, player1Id = p1, player2Id = p2, leg = 1, result = GameResult.Win))

        val result = tournamentDao.getTournament(tournamentId)
        assertNotNull(result)
        assertEquals(GameMode.BLITZ_3_2, result?.tournament?.mode)
        assertTrue(result?.tournament?.isLocalOnly == true)
        assertEquals(2, result?.players?.size)
        assertEquals(1, result?.games?.size)
        assertEquals(p1, result?.games?.get(0)?.player1Id)
    }

    @Test
    fun observeTournamentsEmitsUpdates() = runTest {
        tournamentDao.insert(TournamentEntity(mode = GameMode.BLITZ_5_0, date = LocalDate.now(), rounds = 1, remoteId = null))
        
        val list = tournamentDao.observeTournaments().first()
        assertEquals(1, list.size)
    }

    @Test
    fun updateTournamentRemoteId() = runTest {
        val id = tournamentDao.insert(TournamentEntity(mode = GameMode.BLITZ_3_2, date = LocalDate.now(), rounds = 1, dirty = true, remoteId = null))
        
        tournamentDao.updateTournamentRemoteId(id, 777)
        
        val tournament = tournamentDao.getTournament(id)?.tournament
        assertEquals(777L, tournament?.remoteId)
        assertEquals(false, tournament?.dirty)
    }

    @Test
    fun getDirtyTournaments() = runTest {
        tournamentDao.insert(TournamentEntity(mode = GameMode.BLITZ_3_2, date = LocalDate.now(), rounds = 1, dirty = true, remoteId = null))
        tournamentDao.insert(TournamentEntity(mode = GameMode.BLITZ_5_0, date = LocalDate.now(), rounds = 1, dirty = false, remoteId = 123L))
        
        val dirty = tournamentDao.getDirtyTournaments()
        assertEquals(1, dirty.size)
        assertEquals(GameMode.BLITZ_3_2, dirty[0].tournament.mode)
    }

    @Test
    fun updateTournamentRounds_updatesRoundsAndSetsDirtyTrue() = runTest {
        val id = tournamentDao.insert(TournamentEntity(mode = GameMode.BLITZ_3_2, date = LocalDate.now(), rounds = 1, dirty = false, remoteId = 10L))

        tournamentDao.updateTournamentRounds(id, 2)

        val updated = tournamentDao.getTournament(id)?.tournament
        assertEquals(2, updated?.rounds)
        assertEquals(true, updated?.dirty)
    }

    @Test
    fun markTournamentAsClean_clearsDirtyFlag() = runTest {
        val id = tournamentDao.insert(TournamentEntity(mode = GameMode.BLITZ_3_2, date = LocalDate.now(), rounds = 1, dirty = true, remoteId = null))

        tournamentDao.markTournamentAsClean(id)

        val clean = tournamentDao.getTournament(id)?.tournament
        assertEquals(false, clean?.dirty)
    }

    @Test
    fun deleteById_removesTournamentFromDatabase() = runTest {
        val id = tournamentDao.insert(TournamentEntity(mode = GameMode.BLITZ_3_2, date = LocalDate.now(), rounds = 1, dirty = false, remoteId = null))
        assertNotNull(tournamentDao.getTournament(id))

        tournamentDao.deleteById(id)

        org.junit.Assert.assertNull(tournamentDao.getTournament(id))
    }

    @Test
    fun getTournamentByRemoteId_findsMatchingTournament() = runTest {
        tournamentDao.insert(TournamentEntity(mode = GameMode.BLITZ_3_2, date = LocalDate.now(), rounds = 1, dirty = false, remoteId = 999L))

        val found = tournamentDao.getTournamentByRemoteId(999)
        assertNotNull(found)
        assertEquals(999L, found?.tournament?.remoteId)

        val notFound = tournamentDao.getTournamentByRemoteId(12345)
        org.junit.Assert.assertNull(notFound)
    }
}
