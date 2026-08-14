package de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao

import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.TournamentPlayerCrossRef
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class TournamentPlayerDaoTest : DaoTest() {

    @Test
    fun observeRankingForTournament_respectsOrderAndFilter() = runTest {
        val p1 = playerDao.insert(de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.PlayerEntity(name = "P1", vorname = "V1"))
        val p2 = playerDao.insert(de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.PlayerEntity(name = "P2", vorname = "V2"))
        val p3 = playerDao.insert(de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.PlayerEntity(name = "P3", vorname = "V3"))
        val p4 = playerDao.insert(de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.PlayerEntity(name = "P4", vorname = "V4"))

        val t1 = tournamentDao.insert(de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.TournamentEntity(mode = de.kindermaenner.monatsblitz.domain.model.GameMode.BLITZ_3_2, date = java.time.LocalDate.now(), rounds = 1, remoteId = null))
        val t2 = tournamentDao.insert(de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.TournamentEntity(mode = de.kindermaenner.monatsblitz.domain.model.GameMode.BLITZ_5_0, date = java.time.LocalDate.now(), rounds = 1, remoteId = null))

        tournamentPlayerDao.insertAll(listOf(
            TournamentPlayerCrossRef(t1, p1, points = 2.0, rank = 2),
            TournamentPlayerCrossRef(t1, p2, points = 5.0, rank = 1),
            TournamentPlayerCrossRef(t1, p3, points = 1.0, rank = 3),
            TournamentPlayerCrossRef(t2, p4, points = 10.0, rank = 1)
        ))

        val ranking = tournamentPlayerDao.observeRankingForTournament(t1).first()
        
        assertEquals(3, ranking.size)
        assertEquals(p2, ranking[0].playerId) // Rank 1
        assertEquals(p1, ranking[1].playerId) // Rank 2
        assertEquals(p3, ranking[2].playerId) // Rank 3
    }

    @Test
    fun deleteForTournament_cleansUpData() = runTest {
        val p1 = playerDao.insert(de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.PlayerEntity(name = "P1", vorname = "V1"))
        val p2 = playerDao.insert(de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.PlayerEntity(name = "P2", vorname = "V2"))
        val t1 = tournamentDao.insert(de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.TournamentEntity(mode = de.kindermaenner.monatsblitz.domain.model.GameMode.BLITZ_3_2, date = java.time.LocalDate.now(), rounds = 1, remoteId = null))
        val t2 = tournamentDao.insert(de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.TournamentEntity(mode = de.kindermaenner.monatsblitz.domain.model.GameMode.BLITZ_5_0, date = java.time.LocalDate.now(), rounds = 1, remoteId = null))

        tournamentPlayerDao.insertAll(listOf(
            TournamentPlayerCrossRef(t1, p1, 5.0, 1),
            TournamentPlayerCrossRef(t2, p2, 10.0, 1)
        ))

        tournamentPlayerDao.deleteForTournament(t1)
        
        val t1Ranking = tournamentPlayerDao.observeRankingForTournament(t1).first()
        val t2Ranking = tournamentPlayerDao.observeRankingForTournament(t2).first()

        assertEquals(0, t1Ranking.size)
        assertEquals(1, t2Ranking.size)
    }
}
