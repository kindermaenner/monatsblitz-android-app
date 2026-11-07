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
        val p1 = playerDao.insert(de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.PlayerEntity(name = "P1", vorname = "V1", remoteId = 10L))
        val p2 = playerDao.insert(de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.PlayerEntity(name = "P2", vorname = "V2", remoteId = 20L))
        
        val t1 = tournamentDao.insert(TournamentEntity(mode = GameMode.BLITZ_3_2, date = LocalDate.now(), rounds = 1, remoteId = 100L))
        val t2 = tournamentDao.insert(TournamentEntity(mode = GameMode.BLITZ_5_0, date = LocalDate.now(), rounds = 1, remoteId = null)) // No remote ID

        // Game for t1 (should be in sync)
        gameDao.insert(GameEntity(tournamentId = t1, player1Id = p1, player2Id = p2, leg = 1, result = GameResult.Win, dirty = true))
        // Game for t2 (should NOT be in sync because tournament remoteId is null)
        gameDao.insert(GameEntity(tournamentId = t2, player1Id = p1, player2Id = p2, leg = 1, result = GameResult.Loss, dirty = true))
        // Already synced game for t1 (should NOT be in sync because dirty = false)
        gameDao.insert(GameEntity(tournamentId = t1, player1Id = p1, player2Id = p2, leg = 2, result = GameResult.Remis, dirty = false))
        // Open game without remoteId (should NOT be in sync even if dirty = true)
        gameDao.insert(GameEntity(tournamentId = t1, player1Id = p1, player2Id = p2, leg = 3, result = GameResult.Open, dirty = true, remoteId = null))
        // Open game WITH remoteId (reset back to Open -> SHOULD be in sync when dirty = true)
        gameDao.insert(GameEntity(tournamentId = t1, player1Id = p1, player2Id = p2, leg = 4, result = GameResult.Open, dirty = true, remoteId = 999L))

        val syncData = gameDao.getGamesForSync()
        
        assertEquals(2, syncData.size)
        assertEquals(100, syncData[0].tournamentRemoteId)
        assertEquals(10L, syncData[0].player1Id)
        assertEquals(20L, syncData[0].player2Id)
        assertEquals(GameResult.Win, syncData[0].result)
        assertEquals(GameResult.Open, syncData[1].result)
        assertEquals(999L, syncData[1].remoteGameId)
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

    @Test
    fun deleteGamesForLegGreaterThan_deletesOnlyTargetLegsOfSpecifiedTournament() = runTest {
        val p1 = playerDao.insert(de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.PlayerEntity(name = "P1", vorname = "V1"))
        val p2 = playerDao.insert(de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.PlayerEntity(name = "P2", vorname = "V2"))
        val t1 = tournamentDao.insert(TournamentEntity(mode = GameMode.BLITZ_3_2, date = LocalDate.now(), rounds = 2, remoteId = null))
        val t2 = tournamentDao.insert(TournamentEntity(mode = GameMode.BLITZ_5_0, date = LocalDate.now(), rounds = 2, remoteId = null))

        // t1: leg 1 and leg 2
        gameDao.insert(GameEntity(tournamentId = t1, player1Id = p1, player2Id = p2, leg = 1, result = GameResult.Win))
        gameDao.insert(GameEntity(tournamentId = t1, player1Id = p2, player2Id = p1, leg = 2, result = GameResult.Loss))

        // t2: leg 1 and leg 2
        gameDao.insert(GameEntity(tournamentId = t2, player1Id = p1, player2Id = p2, leg = 1, result = GameResult.Remis))
        gameDao.insert(GameEntity(tournamentId = t2, player1Id = p2, player2Id = p1, leg = 2, result = GameResult.Win))

        // Delete leg > 1 for t1
        gameDao.deleteGamesForLegGreaterThan(t1, 1)

        val t1Games = gameDao.getGamesForTournament(t1)
        assertEquals(1, t1Games.size)
        assertEquals(1, t1Games[0].leg)

        val t2Games = gameDao.getGamesForTournament(t2)
        assertEquals(2, t2Games.size)
    }

    @Test
    fun deleteForTournament_removesAllGamesForSpecificTournament() = runTest {
        val p1 = playerDao.insert(de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.PlayerEntity(name = "P1", vorname = "V1"))
        val p2 = playerDao.insert(de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.PlayerEntity(name = "P2", vorname = "V2"))
        val t1 = tournamentDao.insert(TournamentEntity(mode = GameMode.BLITZ_3_2, date = LocalDate.now(), rounds = 1, remoteId = null))
        val t2 = tournamentDao.insert(TournamentEntity(mode = GameMode.BLITZ_5_0, date = LocalDate.now(), rounds = 1, remoteId = null))

        gameDao.insert(GameEntity(tournamentId = t1, player1Id = p1, player2Id = p2, leg = 1, result = GameResult.Win))
        gameDao.insert(GameEntity(tournamentId = t2, player1Id = p1, player2Id = p2, leg = 1, result = GameResult.Loss))

        gameDao.deleteForTournament(t1)

        assertEquals(0, gameDao.getGamesForTournament(t1).size)
        assertEquals(1, gameDao.getGamesForTournament(t2).size)
    }

    @Test
    fun getDirtyGames_returnsAllDirtyGamesAcrossTournaments() = runTest {
        val p1 = playerDao.insert(de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.PlayerEntity(name = "P1", vorname = "V1"))
        val p2 = playerDao.insert(de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.PlayerEntity(name = "P2", vorname = "V2"))
        val t1 = tournamentDao.insert(TournamentEntity(mode = GameMode.BLITZ_3_2, date = LocalDate.now(), rounds = 1, remoteId = null))

        gameDao.insert(GameEntity(tournamentId = t1, player1Id = p1, player2Id = p2, leg = 1, result = GameResult.Win, dirty = true))
        gameDao.insert(GameEntity(tournamentId = t1, player1Id = p2, player2Id = p1, leg = 1, result = GameResult.Remis, dirty = false))

        val dirtyList = gameDao.getDirtyGames()
        assertEquals(1, dirtyList.size)
        assertEquals(GameResult.Win, dirtyList[0].result)
    }
}
