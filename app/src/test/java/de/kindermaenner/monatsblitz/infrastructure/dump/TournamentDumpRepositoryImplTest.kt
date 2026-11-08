package de.kindermaenner.monatsblitz.infrastructure.dump

import de.kindermaenner.monatsblitz.domain.model.GameMode
import de.kindermaenner.monatsblitz.domain.model.GameResult
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.AppDatabase
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.PlayerDao
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.TournamentDao
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.GameEntity
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.PlayerEntity
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.TournamentEntity
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.TournamentPlayerCrossRef
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.relation.TournamentWithDetails
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.LocalDate

class TournamentDumpRepositoryImplTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val database = mockk<AppDatabase>()
    private val tournamentDao = mockk<TournamentDao>()
    private val playerDao = mockk<PlayerDao>()

    @Test
    fun `createDump creates JSON file with full tournament data`() = runTest {
        val dumpDir = tempFolder.newFolder("dumps")
        val repo = TournamentDumpRepositoryImpl(
            baseDirProvider = { dumpDir },
            database = database
        )

        val tournamentId = 42L
        val tournamentEntity = TournamentEntity(
            id = tournamentId,
            mode = GameMode.BLITZ_3_2,
            date = LocalDate.of(2026, 11, 8),
            rounds = 1,
            remoteId = 100L,
            isLocalOnly = false,
            countsForRanking = true,
            label = "Monatsblitz November"
        )
        val playerEntity = PlayerEntity(
            id = 1L,
            remoteId = 201L,
            vorname = "Max",
            name = "Mustermann",
            rufname = "Maxi"
        )
        val crossRef = TournamentPlayerCrossRef(
            tournamentId = tournamentId,
            playerId = 1L,
            hasLeft = false,
            points = 3.5,
            rank = 1
        )
        val gameEntity = GameEntity(
            id = 500L,
            tournamentId = tournamentId,
            player1Id = 1L,
            player2Id = 2L,
            leg = 1,
            result = GameResult.Win,
            remoteId = 600L,
            dirty = false
        )

        val tournamentWithDetails = TournamentWithDetails(
            tournament = tournamentEntity,
            players = listOf(playerEntity),
            games = listOf(gameEntity),
            playerRefs = listOf(crossRef)
        )

        coEvery { database.tournamentDao() } returns tournamentDao
        coEvery { database.playerDao() } returns playerDao
        coEvery { tournamentDao.getTournament(tournamentId) } returns tournamentWithDetails
        coEvery { playerDao.getPlayersByIds(listOf(1L)) } returns listOf(playerEntity)

        val result = repo.createDump(tournamentId)

        assertTrue("Dump creation should succeed", result.isSuccess)
        val dumpPath = result.getOrThrow()
        val file = File(dumpPath)
        assertTrue("Dump file must exist", file.exists())
        val content = file.readText()
        assertTrue("Content must contain tournamentId", content.contains("\"tournamentId\": 42"))
        assertTrue("Content must contain player name", content.contains("Mustermann"))
        assertTrue("Content must contain game result", content.contains("Win"))

        assertEquals(1, repo.getDumpCount())
    }

    @Test
    fun `cleanupOldDumps removes files older than retention period`() = runTest {
        val dumpDir = tempFolder.newFolder("dumps_cleanup")
        val repo = TournamentDumpRepositoryImpl(
            baseDirProvider = { dumpDir },
            database = database
        )

        val oldFile = File(dumpDir, "tournament_1_old.json").apply { writeText("{}") }
        // Set last modified to 4 months ago
        val fourMonthsAgo = System.currentTimeMillis() - (4 * 30L * 24L * 60L * 60L * 1000L)
        oldFile.setLastModified(fourMonthsAgo)

        val newFile = File(dumpDir, "tournament_2_new.json").apply { writeText("{}") }
        newFile.setLastModified(System.currentTimeMillis())

        assertEquals(2, repo.getDumpCount())

        val cleanedResult = repo.cleanupOldDumps(retentionMonths = 3)
        assertTrue(cleanedResult.isSuccess)
        assertEquals(1, cleanedResult.getOrThrow())

        assertEquals(1, repo.getDumpCount())
        assertTrue("New file should still exist", newFile.exists())
        assertTrue("Old file should be deleted", !oldFile.exists())
    }
}
