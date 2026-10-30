package de.kindermaenner.monatsblitz.infrastructure.repository

import android.util.Log
import androidx.room.withTransaction
import de.kindermaenner.monatsblitz.domain.model.GameMode
import de.kindermaenner.monatsblitz.domain.model.GameResult
import de.kindermaenner.monatsblitz.domain.model.NewGame
import de.kindermaenner.monatsblitz.domain.model.NewTournament
import de.kindermaenner.monatsblitz.domain.model.Player
import de.kindermaenner.monatsblitz.infrastructure.TournamentStorage
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.AppDatabase
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.GameDao
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.TournamentDao
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.TournamentPlayerDao
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.GameEntity
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.TournamentEntity
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.relation.TournamentWithDetails
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class TournamentRepositoryImplTest {

    private val tournamentDao = mockk<TournamentDao>(relaxed = true)
    private val tournamentPlayerDao = mockk<TournamentPlayerDao>(relaxed = true)
    private val gameDao = mockk<GameDao>(relaxed = true)
    private val database = mockk<AppDatabase>(relaxed = true)
    private val tournamentStorage = mockk<TournamentStorage>(relaxed = true)

    private val repository = TournamentRepositoryImpl(
        tournamentDao,
        tournamentPlayerDao,
        gameDao,
        database,
        tournamentStorage
    )

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.i(any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>(), any<Throwable>()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        
        // Mock withTransaction to just execute the block
        mockkStatic("androidx.room.RoomDatabaseKt")
        val transactionBlock = slot<suspend () -> Any>()
        coEvery { database.withTransaction(capture(transactionBlock)) } coAnswers {
            transactionBlock.captured.invoke()
        }
    }

    @Test
    fun `observeTournaments returns mapped domain tournaments`() = runTest {
        val date = LocalDate.now()
        val tournamentEntity = TournamentEntity(id = 1, mode = GameMode.BLITZ_3_2, date = date, rounds = 1, remoteId = null)
        val details = listOf(
            TournamentWithDetails(tournament = tournamentEntity, players = listOf(), games = listOf())
        )
        coEvery { tournamentDao.observeTournaments() } returns flowOf(details)

        val result = repository.observeTournaments().first()

        assertEquals(1, result.size)
        assertEquals(1L, result[0].id)
    }

    @Test
    fun `createTournament inserts all related entities within transaction`() = runTest {
        val date = LocalDate.now()
        val players = listOf(Player(1, "A", "A"))
        val request = NewTournament(
            mode = GameMode.BLITZ_3_2,
            date = date,
            rounds = 1,
            players = players,
            games = listOf(NewGame(1, 2, 1, GameResult.Open))
        )

        coEvery { tournamentDao.insert(any()) } returns 10L
        coEvery { gameDao.insertAll(any()) } returns listOf(100L)
        coEvery { tournamentPlayerDao.insertAll(any()) } returns Unit

        val result = repository.createTournament(request)

        assertEquals(10L, result.id)
        assertEquals(1, result.games.size)
        assertEquals(100L, result.games[100L]?.id)
        coVerify { tournamentDao.insert(match { it.mode == GameMode.BLITZ_3_2 }) }
        coVerify { tournamentPlayerDao.insertAll(match { it.size == 1 && it[0].tournamentId == 10L }) }
        coVerify { tournamentStorage.saveTournamentState(10L, false) }
    }

    @Test
    fun `getTournamentById returns domain model`() = runTest {
        val date = LocalDate.now()
        val tournamentEntity = TournamentEntity(id = 1, mode = GameMode.BLITZ_3_2, date = date, rounds = 1, remoteId = null)
        val details = TournamentWithDetails(tournament = tournamentEntity, players = listOf(), games = listOf())
        
        coEvery { tournamentDao.getTournament(1) } returns details

        val result = repository.getTournamentById(1)

        assertEquals(1L, result?.id)
    }

    @Test
    fun `observeTournament returns domain flow`() = runTest {
        val date = LocalDate.now()
        val tournamentEntity = TournamentEntity(id = 1, mode = GameMode.BLITZ_3_2, date = date, rounds = 1, remoteId = null)
        val details = TournamentWithDetails(tournament = tournamentEntity, players = listOf(), games = listOf())
        
        coEvery { tournamentDao.observeTournament(1) } returns flowOf(details)

        val result = repository.observeTournament(1).first()

        assertEquals(1L, result?.id)
    }

    @Test
    fun `updateGameResult updates existing game if found`() = runTest {
        val gameEntity = GameEntity(id = 100, tournamentId = 1, player1Id = 1, player2Id = 2, leg = 1, result = GameResult.Open, dirty = false)
        coEvery { gameDao.getGameByPlayers(1, 1, 2, 1) } returns gameEntity

        repository.updateGameResult(1, 1, 2, 1, GameResult.Win)

        coVerify { gameDao.update(match { it.id == 100L && it.result == GameResult.Win && it.dirty }) }
    }

    @Test
    fun `updateGameResult resets dirty to false when unsynced game is set back to Open`() = runTest {
        val gameEntity = GameEntity(id = 100, remoteId = null, tournamentId = 1, player1Id = 1, player2Id = 2, leg = 1, result = GameResult.Win, dirty = true)
        coEvery { gameDao.getGameByPlayers(1, 1, 2, 1) } returns gameEntity

        repository.updateGameResult(1, 1, 2, 1, GameResult.Open)

        coVerify { gameDao.update(match { it.id == 100L && it.result == GameResult.Open && !it.dirty }) }
    }

    @Test
    fun `updateGameResult keeps dirty true when synced game with remoteId is set back to Open`() = runTest {
        val gameEntity = GameEntity(id = 100, remoteId = 55L, tournamentId = 1, player1Id = 1, player2Id = 2, leg = 1, result = GameResult.Win, dirty = false)
        coEvery { gameDao.getGameByPlayers(1, 1, 2, 1) } returns gameEntity

        repository.updateGameResult(1, 1, 2, 1, GameResult.Open)

        coVerify { gameDao.update(match { it.id == 100L && it.result == GameResult.Open && it.dirty }) }
    }

    @Test
    fun `updateGameResult inserts new game if not found`() = runTest {
        coEvery { gameDao.getGameByPlayers(1, 1, 2, 1) } returns null

        repository.updateGameResult(1, 1, 2, 1, GameResult.Win)

        coVerify { gameDao.insert(match { it.tournamentId == 1L && it.player1Id == 1L && it.result == GameResult.Win && it.dirty }) }
    }

    @Test
    fun `resetTournament deletes open tournament from db and resets tournamentStorage`() = runTest {
        coEvery { tournamentStorage.getTournamentState() } returns flowOf(de.kindermaenner.monatsblitz.infrastructure.TournamentState(10L, finalized = false))

        repository.resetTournament()

        coVerify { tournamentPlayerDao.deleteForTournament(10L) }
        coVerify { tournamentDao.deleteById(10L) }
        coVerify { tournamentStorage.resetAll() }
    }

    @Test
    fun `resetTournament does not delete finalized tournament from db`() = runTest {
        coEvery { tournamentStorage.getTournamentState() } returns flowOf(de.kindermaenner.monatsblitz.infrastructure.TournamentState(10L, finalized = true))

        repository.resetTournament()

        coVerify(exactly = 0) { tournamentDao.deleteById(any()) }
        coVerify { tournamentStorage.resetAll() }
    }

    @Test
    fun `getTournamentById returns null when not found`() = runTest {
        coEvery { tournamentDao.getTournament(999) } returns null
        val result = repository.getTournamentById(999)
        org.junit.Assert.assertNull(result)
    }

    @Test
    fun `observeTournament returns null flow when not found`() = runTest {
        coEvery { tournamentDao.observeTournament(999) } returns flowOf(null)
        val result = repository.observeTournament(999).first()
        org.junit.Assert.assertNull(result)
    }

    @Test
    fun `saveRankings maps domain entries and upserts to tournamentPlayerDao`() = runTest {
        val rankings = listOf(
            de.kindermaenner.monatsblitz.domain.model.PlayerRankingEntry(playerId = 1, tournamentId = 10, points = 2.5, rank = 1)
        )

        repository.saveRankings(rankings)

        coVerify {
            tournamentPlayerDao.upsertAll(match {
                it.size == 1 && it[0].playerId == 1L && it[0].tournamentId == 10L && it[0].points == 2.5 && it[0].rank == 1
            })
        }
    }

    @Test
    fun `observeTournamentRanking maps joined rows from tournamentPlayerDao`() = runTest {
        val rankingRows = listOf(
            de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.TournamentRankingRowEntity(
                playerId = 1,
                points = 2.5,
                rank = 1,
                vorname = "Alice",
                name = "Meier"
            ),
            de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.TournamentRankingRowEntity(
                playerId = 999,
                points = null,
                rank = 2,
                vorname = null,
                name = null
            )
        )
        coEvery { tournamentPlayerDao.observeRankingWithPlayers(100) } returns flowOf(rankingRows)

        val result = repository.observeTournamentRanking(100).first()

        assertEquals(2, result.size)
        assertEquals("Alice Meier", result[0].name)
        assertEquals(2.5, result[0].points, 0.0)
        assertEquals(1, result[0].rank)
        assertEquals("Unbekannt", result[1].name)
        assertEquals(0.0, result[1].points, 0.0)
        assertEquals(2, result[1].rank)
    }

    @Test
    fun `syncDirtyTournaments uploads dirty tournaments and reuses open remote tournament on HTTP 409`() = runTest {
        val api = mockk<de.kindermaenner.monatsblitz.infrastructure.api.MonatsblitzApi>()
        val repoWithApi = TournamentRepositoryImpl(
            tournamentDao,
            tournamentPlayerDao,
            gameDao,
            database,
            tournamentStorage,
            monatsblitzApi = api
        )
        val date = LocalDate.of(2026, 10, 19)
        val t1 = TournamentEntity(id = 1, mode = GameMode.BLITZ_3_2, date = date, rounds = 1, remoteId = null)
        val t2 = TournamentEntity(id = 2, mode = GameMode.BLITZ_3_2, date = date, rounds = 1, remoteId = null)
        val players = listOf(
            de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.PlayerEntity(id = 1, name = "A", vorname = "A", remoteId = 101L)
        )
        coEvery { tournamentDao.getDirtyTournaments() } returns listOf(
            TournamentWithDetails(t1, emptyList(), players),
            TournamentWithDetails(t2, emptyList(), emptyList())
        )
        val errorResponse = mockk<retrofit2.Response<*>>(relaxed = true) {
            every { code() } returns 409
        }
        coEvery { api.createTournament(match { it.playerIds == listOf(101L) }) } returns
            de.kindermaenner.monatsblitz.infrastructure.api.dto.CreateTournamentResponseDto(success = true, tournamentId = 500)
        coEvery { api.createTournament(match { it.playerIds.isEmpty() }) } throws retrofit2.HttpException(errorResponse)
        coEvery { api.getTournaments() } returns listOf(
            de.kindermaenner.monatsblitz.infrastructure.api.dto.TournamentDto(
                id = 777,
                date = "2026-10-19",
                mode = "3+2",
                roundCount = 1,
                status = "open"
            )
        )

        repoWithApi.syncDirtyTournaments()

        coVerify { tournamentDao.updateTournamentRemoteId(1, 500) }
        coVerify { tournamentDao.updateTournamentRemoteId(2, 777) }
    }

    @Test
    fun `syncDirtyGames uploads dirty games and marks them as synced`() = runTest {
        val api = mockk<de.kindermaenner.monatsblitz.infrastructure.api.MonatsblitzApi>()
        val repoWithApi = TournamentRepositoryImpl(
            tournamentDao,
            tournamentPlayerDao,
            gameDao,
            database,
            tournamentStorage,
            monatsblitzApi = api
        )
        val syncData = de.kindermaenner.monatsblitz.infrastructure.persistence.room.projection.GameSyncData(
            localGameId = 1,
            tournamentRemoteId = 100,
            player1Id = 10,
            player2Id = 20,
            leg = 1,
            result = GameResult.Win,
            remoteGameId = 555L
        )
        coEvery { gameDao.getGamesForSync() } returns listOf(syncData)
        coEvery { api.createGames(any()) } returns de.kindermaenner.monatsblitz.infrastructure.api.dto.CreateGamesResponseDto(
            success = true,
            count = 1,
            items = listOf(de.kindermaenner.monatsblitz.infrastructure.api.dto.GameSyncResponseItemDto(success = true, gameId = 555))
        )

        repoWithApi.syncDirtyGames()

        coVerify { gameDao.markGameAsSynced(1, 555) }
    }

    @Test
    fun `syncDirtyGames keeps a game dirty when the server returns no valid game id`() = runTest {
        val api = mockk<de.kindermaenner.monatsblitz.infrastructure.api.MonatsblitzApi>()
        val repoWithApi = TournamentRepositoryImpl(
            tournamentDao,
            tournamentPlayerDao,
            gameDao,
            database,
            tournamentStorage,
            monatsblitzApi = api
        )
        val syncData = de.kindermaenner.monatsblitz.infrastructure.persistence.room.projection.GameSyncData(
            localGameId = 1,
            tournamentRemoteId = 100,
            player1Id = 10,
            player2Id = 20,
            leg = 1,
            result = GameResult.Win,
            remoteGameId = null
        )
        coEvery { gameDao.getGamesForSync() } returns listOf(syncData)
        coEvery { api.createGames(any()) } returns
            de.kindermaenner.monatsblitz.infrastructure.api.dto.CreateGamesResponseDto(
                success = true,
                count = 1,
                items = listOf(
                    de.kindermaenner.monatsblitz.infrastructure.api.dto.GameSyncResponseItemDto(
                        success = true,
                        gameId = 0
                    )
                )
            )

        repoWithApi.syncDirtyGames()

        coVerify(exactly = 0) { gameDao.markGameAsSynced(any(), any()) }
    }

    @Test
    fun `submitResultsAndFinalize sends results, calls finalize on API, and marks storage finalized`() = runTest {
        val api = mockk<de.kindermaenner.monatsblitz.infrastructure.api.MonatsblitzApi>()
        val playerDao = mockk<de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.PlayerDao>()
        val repoFull = TournamentRepositoryImpl(
            tournamentDao,
            tournamentPlayerDao,
            gameDao,
            database,
            tournamentStorage,
            playerDao = playerDao,
            monatsblitzApi = api
        )
        val tournamentEntity = TournamentEntity(id = 1L, mode = GameMode.BLITZ_3_2, date = LocalDate.now(), rounds = 1, remoteId = 50L)
        coEvery { tournamentDao.getTournament(1L) } returns TournamentWithDetails(tournamentEntity, emptyList(), emptyList())
        coEvery { tournamentPlayerDao.getRankingForTournament(1L) } returns listOf(
            de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.TournamentPlayerCrossRef(1L, 10L, 3.0, 1)
        )
        coEvery { playerDao.getPlayersByIds(listOf(10L)) } returns listOf(
            de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.PlayerEntity(id = 10L, name = "Meier", vorname = "Alice", remoteId = 101L)
        )
        coEvery { api.createResults(any()) } returns de.kindermaenner.monatsblitz.infrastructure.api.dto.BatchResponseDto(success = true, count = 1, items = emptyList())
        coEvery { api.finalize(any()) } returns de.kindermaenner.monatsblitz.infrastructure.api.dto.FinalizeResponseDto(success = true, tournamentId = 50, postId = 200)

        repoFull.submitResultsAndFinalize(1L)

        coVerify { api.createResults(match { it.tournamentId == 50 && it.results.first().playerId == 101 }) }
        coVerify { api.finalize(de.kindermaenner.monatsblitz.infrastructure.api.dto.FinalizeRequestDto(tournamentId = 50)) }
        coVerify { tournamentStorage.finalizeTournament() }
    }

    @Test
    fun `submitResultsAndFinalize does not finalize locally when result upload is rejected`() = runTest {
        val api = mockk<de.kindermaenner.monatsblitz.infrastructure.api.MonatsblitzApi>()
        val playerDao = mockk<de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.PlayerDao>()
        val repoFull = TournamentRepositoryImpl(
            tournamentDao,
            tournamentPlayerDao,
            gameDao,
            database,
            tournamentStorage,
            playerDao = playerDao,
            monatsblitzApi = api
        )
        val tournamentEntity = TournamentEntity(
            id = 1L,
            mode = GameMode.BLITZ_3_2,
            date = LocalDate.now(),
            rounds = 1,
            remoteId = 50L
        )
        coEvery { tournamentDao.getTournament(1L) } returns
            TournamentWithDetails(tournamentEntity, emptyList(), emptyList())
        coEvery { tournamentPlayerDao.getRankingForTournament(1L) } returns listOf(
            de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.TournamentPlayerCrossRef(
                1L,
                10L,
                3.0,
                1
            )
        )
        coEvery { playerDao.getPlayersByIds(listOf(10L)) } returns listOf(
            de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.PlayerEntity(
                id = 10L,
                name = "Meier",
                vorname = "Alice",
                remoteId = 101L
            )
        )
        coEvery { api.createResults(any()) } returns
            de.kindermaenner.monatsblitz.infrastructure.api.dto.BatchResponseDto(
                success = false,
                count = 0,
                items = emptyList(),
                errors = listOf("Ergebnisse konnten nicht gespeichert werden")
            )

        val error = try {
            repoFull.submitResultsAndFinalize(1L)
            null
        } catch (exception: IllegalStateException) {
            exception
        }

        assertEquals("Ergebnisse konnten nicht gespeichert werden", error?.message)
        coVerify(exactly = 0) { api.finalize(any()) }
        coVerify(exactly = 0) { tournamentStorage.finalizeTournament() }
    }

    @Test
    fun `getActiveOpenTournamentId checks local state and remote status`() = runTest {
        val api = mockk<de.kindermaenner.monatsblitz.infrastructure.api.MonatsblitzApi>()
        val repoWithApi = TournamentRepositoryImpl(
            tournamentDao,
            tournamentPlayerDao,
            gameDao,
            database,
            tournamentStorage,
            monatsblitzApi = api
        )
        val tournamentEntity = TournamentEntity(id = 123L, mode = GameMode.BLITZ_3_2, date = LocalDate.now(), rounds = 1, remoteId = 99L)
        coEvery { tournamentStorage.getTournamentState() } returns flowOf(de.kindermaenner.monatsblitz.infrastructure.TournamentState(123L, finalized = false))
        coEvery { tournamentDao.getTournament(123L) } returns TournamentWithDetails(tournamentEntity, emptyList(), emptyList())
        coEvery { api.getTournament(99) } returns de.kindermaenner.monatsblitz.infrastructure.api.dto.TournamentDto(
            id = 99,
            date = "2026-10-19",
            mode = "3+2",
            roundCount = 1,
            status = "finalized"
        )

        val result = repoWithApi.getActiveOpenTournamentId()

        org.junit.Assert.assertNull(result)
        coVerify { tournamentStorage.finalizeTournament() }
    }

    @Test
    fun `getRemoteOpenTournament returns open tournament info when none active locally`() = runTest {
        val api = mockk<de.kindermaenner.monatsblitz.infrastructure.api.MonatsblitzApi>()
        val repo = TournamentRepositoryImpl(
            tournamentDao,
            tournamentPlayerDao,
            gameDao,
            database,
            tournamentStorage,
            monatsblitzApi = api
        )
        coEvery { api.getTournaments() } returns listOf(
            de.kindermaenner.monatsblitz.infrastructure.api.dto.TournamentDto(id = 1, date = "2026-10-10", mode = "3+2", roundCount = 1, status = "finalized"),
            de.kindermaenner.monatsblitz.infrastructure.api.dto.TournamentDto(id = 2, date = "2026-10-21", mode = "3+2", roundCount = 1, status = "open", playerIds = listOf(10L, 20L))
        )
        coEvery { tournamentStorage.getTournamentState() } returns flowOf(null)

        val result = repo.getRemoteOpenTournament()
        org.junit.Assert.assertNotNull(result)
        org.junit.Assert.assertEquals(2, result!!.id)
        org.junit.Assert.assertEquals("2026-10-21", result.date)
        org.junit.Assert.assertEquals(2, result.playerCount)
    }

    @Test
    fun `adoptRemoteTournament downloads players and games and sets local active state`() = runTest {
        val api = mockk<de.kindermaenner.monatsblitz.infrastructure.api.MonatsblitzApi>()
        val playerDaoMock = mockk<de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.PlayerDao>()
        val repo = TournamentRepositoryImpl(
            tournamentDao,
            tournamentPlayerDao,
            gameDao,
            database,
            tournamentStorage,
            playerDao = playerDaoMock,
            monatsblitzApi = api
        )

        coEvery { api.getTournament(50) } returns de.kindermaenner.monatsblitz.infrastructure.api.dto.TournamentDto(
            id = 50, date = "2026-10-21", mode = "3+2", roundCount = 1, status = "open", playerIds = listOf(101L, 102L)
        )
        coEvery { api.getGames(50) } returns listOf(
            de.kindermaenner.monatsblitz.infrastructure.api.dto.GameDto(id = 501, tournamentId = 50, leg = 1, player1Id = 101, player2Id = 102, result = "1:0")
        )

        val p1 = de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.PlayerEntity(id = 1L, name = "Meier", vorname = "Alice", remoteId = 101L)
        val p2 = de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.PlayerEntity(id = 2L, name = "Schulze", vorname = "Bob", remoteId = 102L)
        coEvery { playerDaoMock.getPlayerByRemoteId(101L) } returns p1
        coEvery { playerDaoMock.getPlayerByRemoteId(102L) } returns p2

        coEvery { tournamentDao.getTournamentByRemoteId(50) } returns null
        coEvery { tournamentDao.insert(any()) } returns 77L
        coEvery { tournamentPlayerDao.deleteForTournament(77L) } returns Unit
        coEvery { tournamentPlayerDao.insertAll(any()) } returns Unit
        coEvery { gameDao.deleteForTournament(77L) } returns Unit
        coEvery { gameDao.insertAll(any()) } returns listOf(1L)
        coEvery { tournamentPlayerDao.upsertAll(any()) } returns Unit
        coEvery { tournamentStorage.saveTournamentState(77L, false) } returns Unit

        val localId = repo.adoptRemoteTournament(50)
        org.junit.Assert.assertEquals(77L, localId)

        coVerify { tournamentStorage.saveTournamentState(77L, false) }
        coVerify { gameDao.insertAll(match { it.size == 1 && it.first().result == de.kindermaenner.monatsblitz.domain.model.GameResult.Win }) }
    }

    @Test
    fun `withdrawPlayer with YOUTH_DRAW sets open games to ForfeitDraw and calls remote api`() = runTest {
        val api = mockk<de.kindermaenner.monatsblitz.infrastructure.api.MonatsblitzApi>(relaxed = true)
        val playerDaoMock = mockk<de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.PlayerDao>(relaxed = true)
        val repo = TournamentRepositoryImpl(
            tournamentDao,
            tournamentPlayerDao,
            gameDao,
            database,
            tournamentStorage,
            playerDao = playerDaoMock,
            monatsblitzApi = api
        )

        val g1 = GameEntity(id = 10, tournamentId = 1L, player1Id = 5L, player2Id = 6L, leg = 1, result = GameResult.Open, dirty = false)
        val g2 = GameEntity(id = 11, tournamentId = 1L, player1Id = 7L, player2Id = 5L, leg = 1, result = GameResult.Win, dirty = false)
        val g3 = GameEntity(id = 12, tournamentId = 1L, player1Id = 7L, player2Id = 5L, leg = 2, result = GameResult.Open, dirty = false)
        coEvery { gameDao.getGamesForTournament(1L) } returns listOf(g1, g2, g3)

        val tEntity = TournamentEntity(id = 1L, mode = GameMode.BLITZ_3_2, date = LocalDate.now(), rounds = 1, remoteId = 100L)
        coEvery { tournamentDao.getTournament(1L) } returns TournamentWithDetails(tournament = tEntity, players = emptyList(), games = emptyList())
        val pEntity = de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.PlayerEntity(id = 5L, name = "Meier", vorname = "Max", remoteId = 500L)
        coEvery { playerDaoMock.getPlayerById(5L) } returns pEntity

        repo.withdrawPlayer(1L, 5L, de.kindermaenner.monatsblitz.domain.model.PlayerWithdrawRule.YOUTH_DRAW)

        coVerify { tournamentPlayerDao.setPlayerWithdrawn(1L, 5L, true) }
        coVerify { gameDao.update(match { it.id == 10L && it.result == GameResult.ForfeitDraw && it.dirty }) }
        coVerify { gameDao.update(match { it.id == 12L && it.result == GameResult.ForfeitDraw && it.dirty }) }
        coVerify(exactly = 0) { gameDao.update(match { it.id == 11L }) }
        coVerify {
            api.withdrawPlayer(
                de.kindermaenner.monatsblitz.infrastructure.api.dto.WithdrawPlayerDto(
                    tournamentId = 100,
                    playerId = 500,
                    hasLeft = 1
                )
            )
        }
    }

    @Test
    fun `withdrawPlayer with REGULAR_LOSS sets open games to ForfeitLoss when player1 or ForfeitWin when player2`() = runTest {
        val repo = TournamentRepositoryImpl(
            tournamentDao,
            tournamentPlayerDao,
            gameDao,
            database,
            tournamentStorage
        )

        val g1 = GameEntity(id = 20, tournamentId = 2L, player1Id = 5L, player2Id = 6L, leg = 1, result = GameResult.Open, dirty = false)
        val g2 = GameEntity(id = 21, tournamentId = 2L, player1Id = 6L, player2Id = 5L, leg = 1, result = GameResult.Open, dirty = false)
        coEvery { gameDao.getGamesForTournament(2L) } returns listOf(g1, g2)
        coEvery { tournamentDao.getTournament(2L) } returns null

        repo.withdrawPlayer(2L, 5L, de.kindermaenner.monatsblitz.domain.model.PlayerWithdrawRule.REGULAR_LOSS)

        coVerify { tournamentPlayerDao.setPlayerWithdrawn(2L, 5L, true) }
        coVerify { gameDao.update(match { it.id == 20L && it.result == GameResult.ForfeitLoss && it.dirty }) }
        coVerify { gameDao.update(match { it.id == 21L && it.result == GameResult.ForfeitWin && it.dirty }) }
    }

    @Test
    fun `withdrawPlayer logs warning and does not throw when remote api fails`() = runTest {
        val api = mockk<de.kindermaenner.monatsblitz.infrastructure.api.MonatsblitzApi>()
        val playerDaoMock = mockk<de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.PlayerDao>(relaxed = true)
        val repo = TournamentRepositoryImpl(
            tournamentDao,
            tournamentPlayerDao,
            gameDao,
            database,
            tournamentStorage,
            playerDao = playerDaoMock,
            monatsblitzApi = api
        )

        coEvery { gameDao.getGamesForTournament(3L) } returns emptyList()
        val tEntity = TournamentEntity(id = 3L, mode = GameMode.BLITZ_3_2, date = LocalDate.now(), rounds = 1, remoteId = 100L)
        coEvery { tournamentDao.getTournament(3L) } returns TournamentWithDetails(tournament = tEntity, players = emptyList(), games = emptyList())
        val pEntity = de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.PlayerEntity(id = 5L, name = "Meier", vorname = "Max", remoteId = 500L)
        coEvery { playerDaoMock.getPlayerById(5L) } returns pEntity
        coEvery { api.withdrawPlayer(any()) } throws RuntimeException("Network down")

        repo.withdrawPlayer(3L, 5L, de.kindermaenner.monatsblitz.domain.model.PlayerWithdrawRule.YOUTH_DRAW)

        coVerify { tournamentPlayerDao.setPlayerWithdrawn(3L, 5L, true) }
    }

    @Test
    fun `getRecentTournamentModes returns remote modes when available`() = runTest {
        val api = mockk<de.kindermaenner.monatsblitz.infrastructure.api.MonatsblitzApi>()
        val repo = TournamentRepositoryImpl(
            tournamentDao,
            tournamentPlayerDao,
            gameDao,
            database,
            tournamentStorage,
            monatsblitzApi = api
        )
        coEvery { api.getTournaments() } returns listOf(
            de.kindermaenner.monatsblitz.infrastructure.api.dto.TournamentDto(id = 1, date = "2026-10-10", mode = "Blitz 5+0", roundCount = 1, status = "open"),
            de.kindermaenner.monatsblitz.infrastructure.api.dto.TournamentDto(id = 2, date = "2026-10-11", mode = "Blitz 3+2", roundCount = 1, status = "open")
        )

        val modes = repo.getRecentTournamentModes()
        assertEquals(listOf("Blitz 5+0", "Blitz 3+2"), modes)
    }

    @Test
    fun `getRecentTournamentModes falls back to local tournaments when api is null or fails`() = runTest {
        val api = mockk<de.kindermaenner.monatsblitz.infrastructure.api.MonatsblitzApi>()
        coEvery { api.getTournaments() } throws RuntimeException("Remote error")
        val repo = TournamentRepositoryImpl(
            tournamentDao,
            tournamentPlayerDao,
            gameDao,
            database,
            tournamentStorage,
            monatsblitzApi = api
        )

        val date1 = LocalDate.of(2026, 10, 1)
        val date2 = LocalDate.of(2026, 10, 15)
        val t1 = TournamentEntity(id = 1, mode = GameMode.BLITZ_5_0, date = date1, rounds = 1, remoteId = null)
        val t2 = TournamentEntity(id = 2, mode = GameMode.BLITZ_3_2, date = date2, rounds = 1, remoteId = null)
        coEvery { tournamentDao.observeTournaments() } returns flowOf(
            listOf(
                TournamentWithDetails(tournament = t1, players = emptyList(), games = emptyList()),
                TournamentWithDetails(tournament = t2, players = emptyList(), games = emptyList())
            )
        )

        val modes = repo.getRecentTournamentModes()
        assertEquals(listOf(GameMode.BLITZ_3_2.name, GameMode.BLITZ_5_0.name), modes)
    }

    @Test(expected = IllegalStateException::class)
    fun `syncDirtyTournaments throws IllegalStateException when api is null`() = runTest {
        val repo = TournamentRepositoryImpl(
            tournamentDao,
            tournamentPlayerDao,
            gameDao,
            database,
            tournamentStorage,
            monatsblitzApi = null
        )
        repo.syncDirtyTournaments()
    }

    @Test(expected = IllegalStateException::class)
    fun `submitResultsAndFinalize throws IllegalStateException when playerDao is null`() = runTest {
        val repo = TournamentRepositoryImpl(
            tournamentDao,
            tournamentPlayerDao,
            gameDao,
            database,
            tournamentStorage,
            playerDao = null,
            monatsblitzApi = mockk()
        )
        val newTournament = NewTournament(
            date = LocalDate.now(),
            mode = GameMode.BLITZ_3_2,
            rounds = 1,
            players = listOf(Player(1L, "Meier", "Max")),
            games = emptyList()
        )
        repo.submitResultsAndFinalize(1L)
    }
}
