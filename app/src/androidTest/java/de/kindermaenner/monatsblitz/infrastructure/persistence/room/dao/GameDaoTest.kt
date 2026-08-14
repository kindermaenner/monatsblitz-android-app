package de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao

import de.kindermaenner.monatsblitz.domain.model.GameMode
import de.kindermaenner.monatsblitz.domain.model.GameResult
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.GameEntity
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.TournamentEntity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.time.LocalDate

class GameDaoTest : DaoTest() {

    @Test
    fun getGamesForSync_returnsJoinedData() = runTest {
        val p1 = playerDao.insert(de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.PlayerEntity(name = "P1", vorname = "V1"))
        val p2 = playerDao.insert(de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.PlayerEntity(name = "P2", vorname = "V2"))
        
        val t1 = tournamentDao.insert(TournamentEntity(mode = GameMode.BLITZ_3_2, date = LocalDate.now(), rounds = 1, remoteId = 100L))
        val t2 = tournamentDao.insert(TournamentEntity(mode = GameMode.BLITZ_5_0, date = LocalDate.now(), rounds = 1, remoteId = null)) // No remote ID

        // Game for t1 (should be in sync)
        gameDao.insert(GameEntity(tournamentId = t1, player1Id = p1, player2Id = p2, leg = 1, result = GameResult.Win, dirty = true))
        // Game for t2 (should NOT be in sync because tournament remoteId is null)
        gameDao.insert(GameEntity(tournamentId = t2, player1Id = p1, player2Id = p2, leg = 1, result = GameResult.Loss, dirty = true))
        // Already synced game for t1 (should NOT be in sync because dirty = false)
        gameDao.insert(GameEntity(tournamentId = t1, player1Id = p1, player2Id = p2, leg = 2, result = GameResult.Remis, dirty = false))

        val syncData = gameDao.getGamesForSync()
        
        assertEquals(1, syncData.size)
        assertEquals(100, syncData[0].tournamentRemoteId)
        assertEquals(GameResult.Win, syncData[0].result)
    }

    @Test
    fun getGameByPlayers_findsCorrectMatch() = runTest {
        val p1 = playerDao.insert(de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.PlayerEntity(name = "P1", vorname = "V1"))
        val p2 = playerDao.insert(de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.PlayerEntity(name = "P2", vorname = "V2"))
        val tid = tournamentDao.insert(TournamentEntity(mode = GameMode.BLITZ_3_2, date = LocalDate.now(), rounds = 1, remoteId = null))
        
        gameDao.insert(GameEntity(tournamentId = tid, player1Id = p1, player2Id = p2, leg = 1, result = GameResult.Open))
        gameDao.insert(GameEntity(tournamentId = tid, player1Id = p1, player2Id = p2, leg = 2, result = GameResult.Open))

        val found = gameDao.getGameByPlayers(tid, p1, p2, 2)
        assertNotNull(found)
        assertEquals(2, found?.leg)
    }

    @Test
    fun markGameAsSynced_updatesDirtyAndRemoteId() = runTest {
        val p1 = playerDao.insert(de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.PlayerEntity(name = "P1", vorname = "V1"))
        val p2 = playerDao.insert(de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.PlayerEntity(name = "P2", vorname = "V2"))
        val tid = tournamentDao.insert(TournamentEntity(mode = GameMode.BLITZ_3_2, date = LocalDate.now(), rounds = 1, remoteId = null))
        
        val gid = gameDao.insert(GameEntity(tournamentId = tid, player1Id = p1, player2Id = p2, leg = 1, result = GameResult.Win, dirty = true))
        
        gameDao.markGameAsSynced(gid, 555)
        
        val game = gameDao.getGame(gid)
        assertEquals(555L, game?.remoteId)
        assertEquals(false, game?.dirty)
    }
}
