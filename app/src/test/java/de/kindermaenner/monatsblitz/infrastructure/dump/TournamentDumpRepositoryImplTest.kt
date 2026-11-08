package de.kindermaenner.monatsblitz.infrastructure.dump

import de.kindermaenner.monatsblitz.domain.model.GameMode
import de.kindermaenner.monatsblitz.domain.model.GameResult
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.AppDatabase
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.GameDao
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.PlayerDao
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.TournamentDao
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.TournamentPlayerDao
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.GameEntity
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.PlayerEntity
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.TournamentEntity
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.TournamentPlayerCrossRef
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.relation.TournamentWithDetails
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
    private val gameDao = mockk<GameDao>()
    private val tournamentPlayerDao = mockk<TournamentPlayerDao>()

    @org.junit.Before
    fun setUp() {
        coEvery { database.playerDao() } returns playerDao
        coEvery { database.tournamentDao() } returns tournamentDao
        coEvery { database.tournamentPlayerDao() } returns tournamentPlayerDao
        coEvery { database.gameDao() } returns gameDao
    }

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

    @Test
    fun `getDumpFiles and getDumpFile return existing dump files correctly`() = runTest {
        val dumpDir = tempFolder.newFolder("dumps_list")
        val repo = TournamentDumpRepositoryImpl(
            baseDirProvider = { dumpDir },
            database = database
        )

        val file1 = File(dumpDir, "tournament_42_20261108_120000.json").apply { writeText("{}") }
        val file2 = File(dumpDir, "tournament_43_20261108_130000.json").apply { writeText("{}") }
        File(dumpDir, "ignore_me.txt").apply { writeText("not a dump") }

        val dumps = repo.getDumpFiles()
        assertEquals(2, dumps.size)
        assertTrue(dumps.any { it.tournamentId == 42L && it.fileName == file1.name })
        assertTrue(dumps.any { it.tournamentId == 43L && it.fileName == file2.name })

        val foundFile = repo.getDumpFile(file1.name)
        assertNotNull(foundFile)
        assertEquals(file1.absolutePath, foundFile?.absolutePath)

        val notFound = repo.getDumpFile("non_existent.json")
        assertNull(notFound)
    }

    @Test
    fun `restoreDump successfully restores players, tournament, crossRefs and games`() = runTest {
        val dumpDir = tempFolder.newFolder("dumps_restore")
        val repo = TournamentDumpRepositoryImpl(
            baseDirProvider = { dumpDir },
            database = database
        )

        val jsonDump = """
            {
              "dumpedAt": "2026-11-08T15:00:00Z",
              "tournamentId": 42,
              "mode": "BLITZ_3_2",
              "date": "2026-11-08",
              "rounds": 1,
              "remoteId": 100,
              "isLocalOnly": false,
              "countsForRanking": true,
              "label": "Monatsblitz November",
              "players": [
                {
                  "localId": 1,
                  "remoteId": 201,
                  "vorname": "Max",
                  "name": "Mustermann",
                  "rufname": "Maxi",
                  "hasLeft": false,
                  "points": 1.0,
                  "rank": 1
                },
                {
                  "localId": 2,
                  "remoteId": 202,
                  "vorname": "Erika",
                  "name": "Musterfrau",
                  "rufname": null,
                  "hasLeft": false,
                  "points": 0.0,
                  "rank": 2
                }
              ],
              "games": [
                {
                  "localGameId": 501,
                  "player1Id": 1,
                  "player2Id": 2,
                  "leg": 1,
                  "result": "Win",
                  "remoteGameId": 601,
                  "dirty": false
                }
              ]
            }
        """.trimIndent()

        coEvery { database.playerDao() } returns playerDao
        coEvery { database.tournamentDao() } returns tournamentDao
        coEvery { database.tournamentPlayerDao() } returns tournamentPlayerDao
        coEvery { database.gameDao() } returns gameDao

        // Player 1 exists, Player 2 is new
        val existingMax = PlayerEntity(id = 10L, remoteId = null, vorname = "Max", name = "Mustermann", rufname = "Maxi")
        coEvery { playerDao.findPlayersByName("Max", "Mustermann") } returns listOf(existingMax)
        coEvery { playerDao.update(any()) } returns Unit

        coEvery { playerDao.findPlayersByName("Erika", "Musterfrau") } returns emptyList()
        coEvery { playerDao.insert(any()) } returns 20L

        coEvery { tournamentDao.getTournament(42L) } returns null
        coEvery { tournamentDao.insert(any()) } returns 42L
        coEvery { tournamentPlayerDao.upsertAll(any()) } returns Unit
        coEvery { gameDao.upsertAll(any()) } returns listOf(501L)

        val result = repo.restoreDump(jsonDump)

        assertTrue("Restore should succeed", result.isSuccess)
        assertEquals(42L, result.getOrThrow())

        coVerify {
            tournamentDao.insert(match { it.id == 42L && it.dirty })
            tournamentPlayerDao.upsertAll(match { it.size == 2 })
            gameDao.upsertAll(match { games ->
                games.size == 1 && games[0].dirty && games[0].player1Id == 10L && games[0].player2Id == 20L && games[0].result == GameResult.Win
            })
        }
    }

    @Test
    fun `createDump returns failure when tournament is not found`() = runTest {
        val repo = TournamentDumpRepositoryImpl(
            baseDirProvider = { tempFolder.root },
            database = database
        )
        coEvery { database.tournamentDao() } returns tournamentDao
        coEvery { tournamentDao.getTournament(999L) } returns null

        val result = repo.createDump(999L)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `cleanupOldDumps returns 0 when directory does not exist`() = runTest {
        val nonExistentDir = File(tempFolder.root, "does_not_exist")
        val repo = TournamentDumpRepositoryImpl(
            baseDirProvider = { nonExistentDir },
            database = database
        )

        val result = repo.cleanupOldDumps(3)
        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrThrow())
    }

    @Test
    fun `restoreDump updates tournament entity when already present in database`() = runTest {
        val repo = TournamentDumpRepositoryImpl(
            baseDirProvider = { tempFolder.root },
            database = database
        )

        val jsonDump = """
            {
              "dumpedAt": "2026-11-08T15:00:00Z",
              "tournamentId": 42,
              "mode": "BLITZ_3_2",
              "date": "2026-11-08",
              "rounds": 1,
              "remoteId": 100,
              "isLocalOnly": false,
              "countsForRanking": true,
              "label": "Turnier",
              "players": [],
              "games": []
            }
        """.trimIndent()

        val existingTournament = TournamentEntity(
            id = 42L,
            remoteId = 100L,
            mode = GameMode.BLITZ_5_0,
            date = java.time.LocalDate.now(),
            rounds = 1
        )
        val details = TournamentWithDetails(existingTournament, emptyList(), emptyList())

        coEvery { database.tournamentDao() } returns tournamentDao
        coEvery { tournamentDao.getTournament(42L) } returns details
        coEvery { tournamentDao.update(any()) } returns Unit

        val result = repo.restoreDump(jsonDump)
        assertTrue(result.isSuccess)
        coVerify { tournamentDao.update(match { it.id == 42L && it.dirty }) }
    }

    @Test
    fun `restoreDump falls back to GameResult Open for invalid game result string`() = runTest {
        val repo = TournamentDumpRepositoryImpl(
            baseDirProvider = { tempFolder.root },
            database = database
        )

        val jsonDump = """
            {
              "dumpedAt": "2026-11-08T15:00:00Z",
              "tournamentId": 42,
              "mode": "BLITZ_3_2",
              "date": "2026-11-08",
              "rounds": 1,
              "remoteId": 100,
              "isLocalOnly": false,
              "countsForRanking": true,
              "label": "Turnier",
              "players": [
                { "localId": 1, "remoteId": null, "vorname": "A", "name": "B", "rufname": null, "hasLeft": false, "points": null, "rank": null },
                { "localId": 2, "remoteId": null, "vorname": "C", "name": "D", "rufname": null, "hasLeft": false, "points": null, "rank": null }
              ],
              "games": [
                {
                  "localGameId": 10,
                  "player1Id": 1,
                  "player2Id": 2,
                  "leg": 1,
                  "result": "INVALID_RESULT_STRING",
                  "remoteGameId": null,
                  "dirty": false
                }
              ]
            }
        """.trimIndent()

        coEvery { database.playerDao() } returns playerDao
        coEvery { database.tournamentDao() } returns tournamentDao
        coEvery { database.tournamentPlayerDao() } returns tournamentPlayerDao
        coEvery { database.gameDao() } returns gameDao

        coEvery { playerDao.findPlayersByName("A", "B") } returns emptyList()
        coEvery { playerDao.findPlayersByName("C", "D") } returns emptyList()
        coEvery { playerDao.insert(any()) } returnsMany listOf(1L, 2L)
        coEvery { tournamentDao.getTournament(42L) } returns null
        coEvery { tournamentDao.insert(any()) } returns 42L
        coEvery { tournamentPlayerDao.upsertAll(any()) } returns Unit
        coEvery { gameDao.upsertAll(any()) } returns listOf(10L)

        val result = repo.restoreDump(jsonDump)
        assertTrue(result.isSuccess)

        coVerify {
            gameDao.upsertAll(match { games ->
                games.size == 1 && games[0].result == GameResult.Open
            })
        }
    }

    @Test
    fun `restoreDump returns failure for invalid json`() = runTest {
        val repo = TournamentDumpRepositoryImpl(
            baseDirProvider = { tempFolder.root },
            database = database
        )

        val result = repo.restoreDump("not a valid json")
        assertTrue(result.isFailure)
    }

    @Test
    fun `context constructor initializes dump directory in filesDir`() = runTest {
        val mockContext = mockk<android.content.Context>()
        val fakeFilesDir = tempFolder.newFolder("filesDir")
        every { mockContext.filesDir } returns fakeFilesDir

        val repo = TournamentDumpRepositoryImpl(mockContext, database)
        val dumps = repo.getDumpFiles()
        assertNotNull(dumps)
    }

    @Test
    fun `getDumpFiles handles non-matching filenames with fallback tournamentId 0`() = runTest {
        val dumpDir = tempFolder.newFolder("dumps_fallback")
        val repo = TournamentDumpRepositoryImpl(
            baseDirProvider = { dumpDir },
            database = database
        )

        File(dumpDir, "custom_backup.json").apply { writeText("{}") }

        val dumps = repo.getDumpFiles()
        assertEquals(1, dumps.size)
        assertEquals(0L, dumps[0].tournamentId)
        assertEquals("", dumps[0].timestamp)
        assertEquals("custom_backup.json", dumps[0].fileName)
    }

    @Test
    fun `restorePlayers does not update remoteId when existing player already has one`() = runTest {
        val repo = TournamentDumpRepositoryImpl(
            baseDirProvider = { tempFolder.root },
            database = database
        )

        val jsonDump = """
            {
              "dumpedAt": "2026-11-08T15:00:00Z",
              "tournamentId": 42,
              "mode": "BLITZ_3_2",
              "date": "2026-11-08",
              "rounds": 1,
              "remoteId": 100,
              "isLocalOnly": false,
              "countsForRanking": true,
              "label": "Turnier",
              "players": [
                { "localId": 1, "remoteId": 999, "vorname": "Max", "name": "Mustermann", "rufname": null, "hasLeft": false, "points": null, "rank": null }
              ],
              "games": []
            }
        """.trimIndent()

        val existingPlayer = PlayerEntity(id = 10L, remoteId = 201L, vorname = "Max", name = "Mustermann", rufname = "")
        coEvery { playerDao.findPlayersByName("Max", "Mustermann") } returns listOf(existingPlayer)
        coEvery { tournamentDao.getTournament(42L) } returns null
        coEvery { tournamentDao.insert(any()) } returns 42L
        coEvery { tournamentPlayerDao.upsertAll(any()) } returns Unit

        val result = repo.restoreDump(jsonDump)
        assertTrue(result.isSuccess)

        // PlayerDao.update should NOT be called because remoteId was already 201L
        coVerify(exactly = 0) { playerDao.update(any()) }
    }

    @Test
    fun `restoreTournamentPlayers and restoreGames skip orphan player references gracefully`() = runTest {
        val repo = TournamentDumpRepositoryImpl(
            baseDirProvider = { tempFolder.root },
            database = database
        )

        val jsonDump = """
            {
              "dumpedAt": "2026-11-08T15:00:00Z",
              "tournamentId": 42,
              "mode": "BLITZ_3_2",
              "date": "2026-11-08",
              "rounds": 1,
              "remoteId": 100,
              "isLocalOnly": false,
              "countsForRanking": true,
              "label": "Turnier",
              "players": [],
              "games": [
                {
                  "localGameId": 10,
                  "player1Id": 999,
                  "player2Id": 888,
                  "leg": 1,
                  "result": "Win",
                  "remoteGameId": null,
                  "dirty": false
                }
              ]
            }
        """.trimIndent()

        coEvery { tournamentDao.getTournament(42L) } returns null
        coEvery { tournamentDao.insert(any()) } returns 42L

        val result = repo.restoreDump(jsonDump)
        assertTrue(result.isSuccess)

        // Orphan games (where players 999 and 888 are not in localPlayerMap) should be omitted
        coVerify(exactly = 0) { gameDao.upsertAll(any()) }
    }
}
