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
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.TournamentPlayerCrossRef
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
        coVerify { api.finalize(de.kindermaenner.monatsblitz.infrastructure.api.dto.FinalizeRequestDto(tournamentId = 50, roundCount = 1)) }
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
        coEvery { api.getTournament(2) } returns
            de.kindermaenner.monatsblitz.infrastructure.api.dto.TournamentDto(
                id = 2,
                date = "2026-10-21",
                mode = "5+0",
                roundCount = 2,
                status = "open",
                playerIds = listOf(10L, 20L, 30L),
                dailyNumber = 4,
                label = "Finale",
                countsForRanking = false
            )
        coEvery { tournamentStorage.getTournamentState() } returns flowOf(null)

        val result = repo.getRemoteOpenTournament()
        org.junit.Assert.assertNotNull(result)
        org.junit.Assert.assertEquals(2, result!!.id)
        org.junit.Assert.assertEquals("2026-10-21", result.date)
        org.junit.Assert.assertEquals("5+0", result.mode)
        org.junit.Assert.assertEquals(2, result.roundCount)
        org.junit.Assert.assertEquals(3, result.playerCount)
        org.junit.Assert.assertEquals(4, result.dailyNumber)
        org.junit.Assert.assertEquals("Finale", result.label)
        org.junit.Assert.assertEquals(false, result.countsForRanking)
    }

    @Test
    fun `getRemoteOpenTournament returns null when local active tournament matches remote open tournament`() = runTest {
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
            de.kindermaenner.monatsblitz.infrastructure.api.dto.TournamentDto(
                id = 50,
                date = "2026-10-21",
                mode = "3+2",
                roundCount = 1,
                status = "open"
            )
        )
        coEvery { tournamentStorage.getTournamentState() } returns
            flowOf(de.kindermaenner.monatsblitz.infrastructure.TournamentState(5L, finalized = false))
        coEvery { tournamentDao.getTournament(5L) } returns TournamentWithDetails(
            TournamentEntity(
                id = 5L,
                mode = GameMode.BLITZ_3_2,
                date = LocalDate.of(2026, 10, 21),
                rounds = 1,
                remoteId = 50L
            ),
            emptyList(),
            emptyList()
        )

        assertEquals(null, repo.getRemoteOpenTournament())
        coVerify(exactly = 0) { api.getTournament(any()) }
    }

    @Test
    fun `getRemoteOpenTournament returns null when no remote tournament is open`() = runTest {
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
            de.kindermaenner.monatsblitz.infrastructure.api.dto.TournamentDto(
                id = 50,
                date = "2026-10-21",
                mode = "3+2",
                roundCount = 1,
                status = "finalized"
            )
        )

        assertEquals(null, repo.getRemoteOpenTournament())
        coVerify(exactly = 0) { tournamentStorage.getTournamentState() }
    }

    @Test
    fun `getRemoteOpenTournament falls back to summary when details cannot be fetched`() = runTest {
        val api = mockk<de.kindermaenner.monatsblitz.infrastructure.api.MonatsblitzApi>()
        val repo = TournamentRepositoryImpl(
            tournamentDao,
            tournamentPlayerDao,
            gameDao,
            database,
            tournamentStorage,
            monatsblitzApi = api
        )
        val openSummary = de.kindermaenner.monatsblitz.infrastructure.api.dto.TournamentDto(
            id = 50,
            date = "2026-10-21",
            mode = "3+2",
            roundCount = 1,
            status = "open",
            playerIds = listOf(10L)
        )
        coEvery { api.getTournaments() } returns listOf(openSummary)
        coEvery { api.getTournament(50) } throws IllegalStateException("Details unavailable")
        coEvery { tournamentStorage.getTournamentState() } returns flowOf(null)

        val result = repo.getRemoteOpenTournament()

        assertEquals(50, result?.id)
        assertEquals(1, result?.playerCount)
    }

    @Test
    fun `getRemoteOpenTournament returns null when listing remote tournaments fails`() = runTest {
        val api = mockk<de.kindermaenner.monatsblitz.infrastructure.api.MonatsblitzApi>()
        val repo = TournamentRepositoryImpl(
            tournamentDao,
            tournamentPlayerDao,
            gameDao,
            database,
            tournamentStorage,
            monatsblitzApi = api
        )
        coEvery { api.getTournaments() } throws IllegalStateException("Backend unavailable")

        assertEquals(null, repo.getRemoteOpenTournament())
    }

    @Test
    fun `getRemoteOpenTournament returns null when API is not configured`() = runTest {
        val repo = TournamentRepositoryImpl(
            tournamentDao,
            tournamentPlayerDao,
            gameDao,
            database,
            tournamentStorage,
            monatsblitzApi = null
        )

        assertEquals(null, repo.getRemoteOpenTournament())
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
    fun `adoptRemoteTournament updates existing tournament when already present locally`() = runTest {
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

        val existingTournament = TournamentEntity(
            id = 42L,
            mode = GameMode.BLITZ_5_0,
            date = LocalDate.parse("2026-01-01"),
            rounds = 1,
            remoteId = 88L,
            dirty = true
        )
        val existingDetails = TournamentWithDetails(existingTournament, emptyList(), emptyList())
        coEvery { tournamentDao.getTournamentByRemoteId(88) } returns existingDetails
        coEvery { tournamentDao.update(any()) } returns Unit

        coEvery { api.getTournament(88) } returns de.kindermaenner.monatsblitz.infrastructure.api.dto.TournamentDto(
            id = 88,
            date = "2026-11-06",
            mode = "3+2",
            roundCount = 2,
            status = "open",
            playerIds = emptyList()
        )
        coEvery { api.getGames(88) } returns emptyList()
        coEvery { api.getPlayers() } returns emptyList()
        coEvery { tournamentPlayerDao.deleteForTournament(42L) } returns Unit
        coEvery { tournamentPlayerDao.insertAll(any()) } returns Unit
        coEvery { gameDao.deleteForTournament(42L) } returns Unit
        coEvery { tournamentPlayerDao.upsertAll(any()) } returns Unit
        coEvery { tournamentStorage.saveTournamentState(42L, false) } returns Unit

        val localId = repo.adoptRemoteTournament(88)

        assertEquals(42L, localId)
        coVerify {
            tournamentDao.update(match {
                it.id == 42L &&
                    it.mode == GameMode.BLITZ_3_2 &&
                    it.rounds == 2 &&
                    it.date == LocalDate.parse("2026-11-06") &&
                    !it.dirty
            })
        }
        coVerify { tournamentStorage.saveTournamentState(42L, false) }
    }

    @Test
    fun `adoptRemoteTournament reconciles remote players by updating existing, matching by name and inserting new`() = runTest {
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

        coEvery { api.getTournament(60) } returns de.kindermaenner.monatsblitz.infrastructure.api.dto.TournamentDto(
            id = 60,
            date = "2026-11-06",
            mode = "3+2",
            roundCount = 1,
            status = "open",
            playerIds = listOf(101L, 102L, 103L)
        )
        coEvery { api.getGames(60) } returns emptyList()

        val remotePlayers = listOf(
            de.kindermaenner.monatsblitz.infrastructure.api.dto.PlayerDto(id = 101L, surname = "Schmidt", forename = "Hans", lastParticipated = "2026-10-01"),
            de.kindermaenner.monatsblitz.infrastructure.api.dto.PlayerDto(id = 102L, surname = "Mueller", forename = "Anna"),
            de.kindermaenner.monatsblitz.infrastructure.api.dto.PlayerDto(id = 103L, surname = "Neu", forename = "Max")
        )
        coEvery { api.getPlayers() } returns remotePlayers

        // Player 101: exists by remoteId
        val existingP1 = de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.PlayerEntity(id = 1L, name = "Alt", vorname = "Hans", remoteId = 101L)
        coEvery { playerDaoMock.getPlayerByRemoteId(101L) } returns existingP1

        // Player 102: does not exist by remoteId, but exists by name
        coEvery { playerDaoMock.getPlayerByRemoteId(102L) } returns null
        val existingP2 = de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.PlayerEntity(id = 2L, name = "Mueller", vorname = "Anna", remoteId = null)
        coEvery { playerDaoMock.findPlayersByName("Anna", "Mueller") } returns listOf(existingP2)

        // Player 103: does not exist at all
        coEvery { playerDaoMock.getPlayerByRemoteId(103L) } returns null
        coEvery { playerDaoMock.findPlayersByName("Max", "Neu") } returns emptyList()

        coEvery { tournamentDao.getTournamentByRemoteId(60) } returns null
        coEvery { tournamentDao.insert(any()) } returns 99L
        coEvery { tournamentPlayerDao.deleteForTournament(99L) } returns Unit
        coEvery { tournamentPlayerDao.insertAll(any()) } returns Unit
        coEvery { gameDao.deleteForTournament(99L) } returns Unit
        coEvery { tournamentPlayerDao.upsertAll(any()) } returns Unit
        coEvery { tournamentStorage.saveTournamentState(99L, false) } returns Unit

        repo.adoptRemoteTournament(60)

        // 1. Update existing player by remoteId
        coVerify {
            playerDaoMock.update(match {
                it.id == 1L && it.name == "Schmidt" && it.vorname == "Hans" && it.lastParticipated == "2026-10-01" && !it.dirty && !it.isLocal
            })
        }
        // 2. Set remoteId on existing player by name
        coVerify { playerDaoMock.setRemoteId(playerId = 2L, remoteId = 102L) }
        // 3. Insert brand new player entity
        coVerify {
            playerDaoMock.insert(match {
                it.remoteId == 103L && it.name == "Neu" && it.vorname == "Max" && !it.dirty && !it.isLocal
            })
        }
    }

    @Test
    fun `adoptRemoteTournament logs warning and continues when api getPlayers throws`() = runTest {
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

        coEvery { api.getTournament(70) } returns de.kindermaenner.monatsblitz.infrastructure.api.dto.TournamentDto(
            id = 70,
            date = "2026-11-06",
            mode = "3+2",
            roundCount = 1,
            status = "open",
            playerIds = emptyList()
        )
        coEvery { api.getGames(70) } returns emptyList()
        coEvery { api.getPlayers() } throws RuntimeException("Connection failed")
        coEvery { tournamentDao.getTournamentByRemoteId(70) } returns null
        coEvery { tournamentDao.insert(any()) } returns 15L
        coEvery { tournamentPlayerDao.deleteForTournament(15L) } returns Unit
        coEvery { tournamentPlayerDao.insertAll(any()) } returns Unit
        coEvery { gameDao.deleteForTournament(15L) } returns Unit
        coEvery { tournamentPlayerDao.upsertAll(any()) } returns Unit
        coEvery { tournamentStorage.saveTournamentState(15L, false) } returns Unit

        val localId = repo.adoptRemoteTournament(70)

        assertEquals(15L, localId)
        coVerify { tournamentStorage.saveTournamentState(15L, false) }
    }

    @Test
    fun `adoptRemoteTournament calculates rankings with Remis, ForfeitLoss, ForfeitDraw and assigns identical ranks on tie`() = runTest {
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

        coEvery { api.getTournament(80) } returns de.kindermaenner.monatsblitz.infrastructure.api.dto.TournamentDto(
            id = 80,
            date = "2026-11-06",
            mode = "3+2",
            roundCount = 1,
            status = "open",
            playerIds = listOf(101L, 102L, 103L, 104L)
        )
        // Games:
        // P1 vs P2: Remis -> 0.5 each
        // P3 vs P4: 0:1 (Loss for P3, Win for P4) -> P4 +1.0
        // P1 vs P3: 0:0 (ForfeitDraw) -> 0.5 each
        // P2 vs P4: -:+ (ForfeitLoss for P2, ForfeitWin for P4) -> P4 +1.0
        // Total points:
        // P4: 1.0 + 1.0 = 2.0 (Rank 1)
        // P1: 0.5 + 0.5 = 1.0 (Rank 2 - Tie)
        // P3: 0.0 + 0.5 = 0.5 (Rank 4)
        // P2: 0.5 + 0.0 = 0.5 (Rank 4? Wait, let's calculate: P1=1.0, P2=0.5, P3=0.5, P4=2.0)
        // P2 and P3 both have 0.5 points! They should both get Rank 3!
        coEvery { api.getGames(80) } returns listOf(
            de.kindermaenner.monatsblitz.infrastructure.api.dto.GameDto(id = 1, tournamentId = 80, leg = 1, player1Id = 101, player2Id = 102, result = "0.5:0.5"),
            de.kindermaenner.monatsblitz.infrastructure.api.dto.GameDto(id = 2, tournamentId = 80, leg = 1, player1Id = 103, player2Id = 104, result = "0:1"),
            de.kindermaenner.monatsblitz.infrastructure.api.dto.GameDto(id = 3, tournamentId = 80, leg = 1, player1Id = 101, player2Id = 103, result = "=:="),
            de.kindermaenner.monatsblitz.infrastructure.api.dto.GameDto(id = 4, tournamentId = 80, leg = 1, player1Id = 102, player2Id = 104, result = "-:+")
        )
        coEvery { api.getPlayers() } returns emptyList()

        val p1 = de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.PlayerEntity(id = 1L, name = "A", vorname = "A", remoteId = 101L)
        val p2 = de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.PlayerEntity(id = 2L, name = "B", vorname = "B", remoteId = 102L)
        val p3 = de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.PlayerEntity(id = 3L, name = "C", vorname = "C", remoteId = 103L)
        val p4 = de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.PlayerEntity(id = 4L, name = "D", vorname = "D", remoteId = 104L)
        coEvery { playerDaoMock.getPlayerByRemoteId(101L) } returns p1
        coEvery { playerDaoMock.getPlayerByRemoteId(102L) } returns p2
        coEvery { playerDaoMock.getPlayerByRemoteId(103L) } returns p3
        coEvery { playerDaoMock.getPlayerByRemoteId(104L) } returns p4

        coEvery { tournamentDao.getTournamentByRemoteId(80) } returns null
        coEvery { tournamentDao.insert(any()) } returns 25L
        coEvery { tournamentPlayerDao.deleteForTournament(25L) } returns Unit
        coEvery { tournamentPlayerDao.insertAll(any()) } returns Unit
        coEvery { gameDao.deleteForTournament(25L) } returns Unit
        coEvery { gameDao.insertAll(any()) } returns listOf(1L, 2L, 3L, 4L)
        coEvery { tournamentPlayerDao.upsertAll(any()) } returns Unit
        coEvery { tournamentStorage.saveTournamentState(25L, false) } returns Unit

        repo.adoptRemoteTournament(80)

        coVerify {
            tournamentPlayerDao.upsertAll(match { refs ->
                assertEquals(4, refs.size)
                // Sorted descending by points:
                // 1st: P4 with 2.0 points, rank 1
                val r4 = refs.first { it.playerId == 4L }
                assertEquals(2.0, r4.points!!, 0.001)
                assertEquals(1, r4.rank)

                // 2nd: P1 with 1.0 points, rank 2
                val r1 = refs.first { it.playerId == 1L }
                assertEquals(1.0, r1.points!!, 0.001)
                assertEquals(2, r1.rank)

                // 3rd & 4th: P2 and P3 tied with 0.5 points -> both must have rank 3!
                val r2 = refs.first { it.playerId == 2L }
                val r3 = refs.first { it.playerId == 3L }
                assertEquals(0.5, r2.points!!, 0.001)
                assertEquals(0.5, r3.points!!, 0.001)
                assertEquals(3, r2.rank)
                assertEquals(3, r3.rank)
                true
            })
        }
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
    fun `getRecentTournamentModes ignores non-ranking tournaments from remote API`() = runTest {
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
            de.kindermaenner.monatsblitz.infrastructure.api.dto.TournamentDto(id = 1, date = "2026-10-10", mode = "Blitz 5+0", roundCount = 1, status = "finalized", countsForRanking = true),
            de.kindermaenner.monatsblitz.infrastructure.api.dto.TournamentDto(id = 2, date = "2026-10-10", mode = "Blitz 3+0", roundCount = 1, status = "finalized", countsForRanking = false, label = "Nachblitz"),
            de.kindermaenner.monatsblitz.infrastructure.api.dto.TournamentDto(id = 3, date = "2026-10-11", mode = "Blitz 3+2", roundCount = 1, status = "finalized", countsForRanking = true)
        )

        val modes = repo.getRecentTournamentModes()
        assertEquals(listOf("Blitz 5+0", "Blitz 3+2"), modes)
    }

    @Test
    fun `getRecentTournamentModes ignores non-ranking tournaments in local fallback`() = runTest {
        val repo = TournamentRepositoryImpl(
            tournamentDao,
            tournamentPlayerDao,
            gameDao,
            database,
            tournamentStorage,
            monatsblitzApi = null
        )

        val date1 = LocalDate.of(2026, 10, 1)
        val date2 = LocalDate.of(2026, 10, 1)
        val date3 = LocalDate.of(2026, 10, 15)
        val t1 = TournamentEntity(id = 1, mode = GameMode.BLITZ_5_0, date = date1, rounds = 1, remoteId = null, countsForRanking = true)
        val t2 = TournamentEntity(id = 2, mode = GameMode.HANDICAP, date = date2, rounds = 1, remoteId = null, countsForRanking = false, label = "Side")
        val t3 = TournamentEntity(id = 3, mode = GameMode.BLITZ_3_2, date = date3, rounds = 1, remoteId = null, countsForRanking = true)
        coEvery { tournamentDao.observeTournaments() } returns flowOf(
            listOf(
                TournamentWithDetails(tournament = t1, players = emptyList(), games = emptyList()),
                TournamentWithDetails(tournament = t2, players = emptyList(), games = emptyList()),
                TournamentWithDetails(tournament = t3, players = emptyList(), games = emptyList())
            )
        )

        val modes = repo.getRecentTournamentModes()
        assertEquals(listOf(GameMode.BLITZ_3_2.name, GameMode.BLITZ_5_0.name), modes)
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

    @Test
    fun `addPlayerToTournament adds crossref and open games for each existing player and notifies api`() = runTest {
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

        val existingPlayer = de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.PlayerEntity(id = 1L, name = "Meier", vorname = "Max", remoteId = 101L)
        val newPlayer = de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.PlayerEntity(id = 2L, name = "Schulz", vorname = "Anna", remoteId = 102L)
        val tEntity = TournamentEntity(id = 5L, mode = GameMode.BLITZ_3_2, date = LocalDate.now(), rounds = 1, remoteId = 50L)

        coEvery { tournamentDao.getTournament(5L) } returns TournamentWithDetails(
            tournament = tEntity,
            players = listOf(existingPlayer),
            games = emptyList(),
            playerRefs = listOf(TournamentPlayerCrossRef(tournamentId = 5L, playerId = 1L))
        )
        coEvery { playerDaoMock.getPlayerById(2L) } returns newPlayer

        repo.addPlayerToTournament(5L, 2L)

        coVerify {
            tournamentPlayerDao.insertAll(match { refs ->
                refs.size == 1 && refs.first().tournamentId == 5L && refs.first().playerId == 2L && !refs.first().hasLeft
            })
        }
        coVerify {
            gameDao.insertAll(match { games ->
                games.size == 1 && games.first().player1Id == 1L && games.first().player2Id == 2L &&
                    games.first().leg == 1 && games.first().result == GameResult.Open && !games.first().dirty
            })
        }
        coVerify {
            api.addTournamentPlayer(
                de.kindermaenner.monatsblitz.infrastructure.api.dto.AddTournamentPlayerDto(
                    tournamentId = 50,
                    playerId = 102
                )
            )
        }
    }

    @Test
    fun `addPlayerToTournament does nothing when player already participating`() = runTest {
        val playerDaoMock = mockk<de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.PlayerDao>(relaxed = true)
        val repo = TournamentRepositoryImpl(
            tournamentDao,
            tournamentPlayerDao,
            gameDao,
            database,
            tournamentStorage,
            playerDao = playerDaoMock
        )

        val existingPlayer = de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.PlayerEntity(id = 1L, name = "Meier", vorname = "Max")
        val tEntity = TournamentEntity(id = 5L, mode = GameMode.BLITZ_3_2, date = LocalDate.now(), rounds = 1, remoteId = null)

        coEvery { tournamentDao.getTournament(5L) } returns TournamentWithDetails(
            tournament = tEntity,
            players = listOf(existingPlayer),
            games = emptyList()
        )

        repo.addPlayerToTournament(5L, 1L)

        coVerify(exactly = 0) { tournamentPlayerDao.insertAll(any()) }
        coVerify(exactly = 0) { gameDao.insertAll(any()) }
    }

    @Test
    fun `addPlayerToTournament promotes local player when tournament is not local-only`() = runTest {
        val playerDaoMock = mockk<de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.PlayerDao>(relaxed = true)
        val repo = TournamentRepositoryImpl(
            tournamentDao,
            tournamentPlayerDao,
            gameDao,
            database,
            tournamentStorage,
            playerDao = playerDaoMock
        )

        val localPlayer = de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.PlayerEntity(id = 2L, name = "Lokal", vorname = "Spieler", isLocal = true)
        val tEntity = TournamentEntity(id = 5L, mode = GameMode.BLITZ_3_2, date = LocalDate.now(), rounds = 1, remoteId = null, isLocalOnly = false)

        coEvery { tournamentDao.getTournament(5L) } returns TournamentWithDetails(
            tournament = tEntity,
            players = emptyList(),
            games = emptyList()
        )
        coEvery { playerDaoMock.getPlayerById(2L) } returns localPlayer

        repo.addPlayerToTournament(5L, 2L)

        coVerify(exactly = 1) { playerDaoMock.promoteLocalPlayer(2L) }
    }

    @Test
    fun `addPlayerToTournament with withdrawn player assigns appropriate forfeit result`() = runTest {
        val playerDaoMock = mockk<de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.PlayerDao>(relaxed = true)
        val repo = TournamentRepositoryImpl(
            tournamentDao,
            tournamentPlayerDao,
            gameDao,
            database,
            tournamentStorage,
            playerDao = playerDaoMock
        )

        val withdrawnPlayer = de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.PlayerEntity(id = 1L, name = "Alt", vorname = "Spieler")
        val newPlayer = de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.PlayerEntity(id = 2L, name = "Neu", vorname = "Spieler")
        val tEntity = TournamentEntity(id = 5L, mode = GameMode.BLITZ_3_2, date = LocalDate.now(), rounds = 1, remoteId = null)
        val withdrawnGame = de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.GameEntity(id = 11, tournamentId = 5L, player1Id = 1L, player2Id = 9L, leg = 1, result = GameResult.ForfeitDraw)

        coEvery { tournamentDao.getTournament(5L) } returns TournamentWithDetails(
            tournament = tEntity,
            players = listOf(withdrawnPlayer),
            games = listOf(withdrawnGame),
            playerRefs = listOf(TournamentPlayerCrossRef(tournamentId = 5L, playerId = 1L, hasLeft = true))
        )
        coEvery { playerDaoMock.getPlayerById(2L) } returns newPlayer

        repo.addPlayerToTournament(5L, 2L)

        coVerify {
            gameDao.insertAll(match { games ->
                games.size == 1 && games.first().result == GameResult.ForfeitDraw && games.first().dirty
            })
        }
    }

    @Test
    fun `updateTournamentRounds increases rounds to 2 and inserts leg 2 games`() = runTest {
        val p1 = de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.PlayerEntity(id = 1L, name = "A", vorname = "Alice")
        val p2 = de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.PlayerEntity(id = 2L, name = "B", vorname = "Bob")
        val tEntity = TournamentEntity(id = 5L, mode = GameMode.BLITZ_3_2, date = LocalDate.now(), rounds = 1, remoteId = null)
        val leg1Game = de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.GameEntity(id = 10, tournamentId = 5L, player1Id = 1L, player2Id = 2L, leg = 1, result = GameResult.Win)

        coEvery { tournamentDao.getTournament(5L) } returns TournamentWithDetails(
            tournament = tEntity,
            players = listOf(p1, p2),
            games = listOf(leg1Game),
            playerRefs = listOf(
                TournamentPlayerCrossRef(tournamentId = 5L, playerId = 1L),
                TournamentPlayerCrossRef(tournamentId = 5L, playerId = 2L)
            )
        )

        repository.updateTournamentRounds(5L, 2)

        coVerify {
            gameDao.insertAll(match { games ->
                games.size == 1 && games.first().leg == 2 && games.first().result == GameResult.Open
            })
        }
        coVerify { tournamentDao.updateTournamentRounds(5L, 2) }
    }

    @Test
    fun `updateTournamentRounds decreases rounds to 1 and deletes leg 2 games`() = runTest {
        val p1 = de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.PlayerEntity(id = 1L, name = "A", vorname = "Alice")
        val p2 = de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.PlayerEntity(id = 2L, name = "B", vorname = "Bob")
        val tEntity = TournamentEntity(id = 5L, mode = GameMode.BLITZ_3_2, date = LocalDate.now(), rounds = 2, remoteId = null)

        coEvery { tournamentDao.getTournament(5L) } returns TournamentWithDetails(
            tournament = tEntity,
            players = listOf(p1, p2),
            games = emptyList(),
            playerRefs = emptyList()
        )

        repository.updateTournamentRounds(5L, 1)

        coVerify { gameDao.deleteGamesForLegGreaterThan(5L, 1) }
        coVerify { tournamentDao.updateTournamentRounds(5L, 1) }
    }

    @Test
    fun `updateTournamentRounds does not write when requested rounds are unchanged`() = runTest {
        val tEntity = TournamentEntity(id = 5L, mode = GameMode.BLITZ_3_2, date = LocalDate.now(), rounds = 1, remoteId = null)
        coEvery { tournamentDao.getTournament(5L) } returns
            TournamentWithDetails(tEntity, emptyList(), emptyList())

        repository.updateTournamentRounds(5L, 1)

        coVerify(exactly = 0) { gameDao.insertAll(any()) }
        coVerify(exactly = 0) { gameDao.deleteGamesForLegGreaterThan(any(), any()) }
        coVerify(exactly = 0) { tournamentDao.updateTournamentRounds(any(), any()) }
    }

    @Test
    fun `updateTournamentRounds creates missing second legs with correct withdrawn results`() = runTest {
        val players = (1L..4L).map { id ->
            de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.PlayerEntity(
                id = id,
                name = "Player $id",
                vorname = "Player"
            )
        }
        val tEntity = TournamentEntity(id = 5L, mode = GameMode.BLITZ_3_2, date = LocalDate.now(), rounds = 1, remoteId = null)
        val existingLeg2 = GameEntity(
            id = 100L,
            tournamentId = 5L,
            player1Id = 1L,
            player2Id = 2L,
            leg = 2,
            result = GameResult.Open
        )
        val pastDraw = GameEntity(
            id = 101L,
            tournamentId = 5L,
            player1Id = 1L,
            player2Id = 99L,
            leg = 1,
            result = GameResult.ForfeitDraw
        )
        val pastLoss = GameEntity(
            id = 102L,
            tournamentId = 5L,
            player1Id = 2L,
            player2Id = 99L,
            leg = 1,
            result = GameResult.ForfeitLoss
        )
        coEvery { tournamentDao.getTournament(5L) } returns TournamentWithDetails(
            tournament = tEntity,
            players = players,
            games = listOf(existingLeg2, pastDraw, pastLoss),
            playerRefs = players.take(3).map {
                TournamentPlayerCrossRef(tournamentId = 5L, playerId = it.id, hasLeft = true)
            }
        )

        repository.updateTournamentRounds(5L, 2)

        coVerify {
            gameDao.insertAll(match { games ->
                games.size == 5 &&
                    games.all { it.leg == 2 } &&
                    games.none { it.player1Id == 1L && it.player2Id == 2L } &&
                    games.first { it.player1Id == 1L && it.player2Id == 3L }.result == GameResult.ForfeitDraw &&
                    games.first { it.player1Id == 1L && it.player2Id == 4L }.result == GameResult.ForfeitDraw &&
                    games.first { it.player1Id == 2L && it.player2Id == 3L }.result == GameResult.ForfeitDraw &&
                    games.first { it.player1Id == 2L && it.player2Id == 4L }.result == GameResult.ForfeitLoss &&
                    games.first { it.player1Id == 3L && it.player2Id == 4L }.result == GameResult.ForfeitLoss &&
                    games.all { it.dirty }
            })
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `updateTournamentRounds fails when tournament does not exist`() = runTest {
        coEvery { tournamentDao.getTournament(5L) } returns null

        repository.updateTournamentRounds(5L, 2)
    }

    @Test
    fun `updateTournamentRounds throws on unsupported rounds`() = runTest {
        try {
            repository.updateTournamentRounds(5L, 3)
            org.junit.Assert.fail("Should throw IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            org.junit.Assert.assertTrue(e.message!!.contains("Nur 1 oder 2 Runden"))
        }
    }

    @Test(expected = retrofit2.HttpException::class)
    fun `syncDirtyTournaments throws exception when HTTP error is not 409`() = runTest {
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
        val t = TournamentEntity(id = 1, mode = GameMode.BLITZ_3_2, date = date, rounds = 1, remoteId = null)
        coEvery { tournamentDao.getDirtyTournaments() } returns listOf(TournamentWithDetails(t, emptyList(), emptyList()))

        val errorResponse = mockk<retrofit2.Response<*>>(relaxed = true) {
            every { code() } returns 500
        }
        coEvery { api.createTournament(any()) } throws retrofit2.HttpException(errorResponse)

        repoWithApi.syncDirtyTournaments()
    }

    @Test(expected = retrofit2.HttpException::class)
    fun `syncDirtyTournaments HTTP 409 rethrows when no open tournament matches date`() = runTest {
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
        val t = TournamentEntity(id = 1, mode = GameMode.BLITZ_3_2, date = date, rounds = 1, remoteId = null)
        coEvery { tournamentDao.getDirtyTournaments() } returns listOf(TournamentWithDetails(t, emptyList(), emptyList()))

        val errorResponse = mockk<retrofit2.Response<*>>(relaxed = true) {
            every { code() } returns 409
        }
        coEvery { api.createTournament(any()) } throws retrofit2.HttpException(errorResponse)
        coEvery { api.getTournaments() } returns listOf(
            de.kindermaenner.monatsblitz.infrastructure.api.dto.TournamentDto(
                id = 999,
                date = "2026-10-19",
                mode = "3+2",
                roundCount = 1,
                status = "closed"
            )
        )

        repoWithApi.syncDirtyTournaments()
    }

    @Test
    fun `adoptRemoteTournament with malformed date falls back to current date`() = runTest {
        val api = mockk<de.kindermaenner.monatsblitz.infrastructure.api.MonatsblitzApi>()
        val playerDaoMock = mockk<de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.PlayerDao>(relaxed = true)
        val repo = TournamentRepositoryImpl(
            tournamentDao,
            tournamentPlayerDao,
            gameDao,
            database,
            tournamentStorage,
            monatsblitzApi = api,
            playerDao = playerDaoMock
        )
        val remoteDto = de.kindermaenner.monatsblitz.infrastructure.api.dto.TournamentDto(
            id = 95,
            date = "invalid-date",
            mode = "3+2",
            roundCount = 1,
            playerIds = emptyList()
        )
        coEvery { api.getTournament(95) } returns remoteDto
        coEvery { api.getGames(95) } returns emptyList()
        coEvery { api.getPlayers() } returns emptyList()
        coEvery { tournamentDao.getTournamentByRemoteId(95) } returns null
        coEvery { tournamentDao.insert(any()) } returns 123L
        coEvery { tournamentPlayerDao.deleteForTournament(123L) } returns Unit
        coEvery { tournamentPlayerDao.insertAll(any()) } returns Unit
        coEvery { gameDao.deleteForTournament(123L) } returns Unit
        coEvery { tournamentPlayerDao.upsertAll(any()) } returns Unit
        coEvery { tournamentStorage.saveTournamentState(123L, false) } returns Unit

        val localId = repo.adoptRemoteTournament(95)
        org.junit.Assert.assertEquals(123L, localId)

        coVerify {
            tournamentDao.insert(match {
                it.date == LocalDate.now()
            })
        }
    }

    @Test
    fun `addPlayerToTournament logs warning and does not throw when remote api fails`() = runTest {
        val api = mockk<de.kindermaenner.monatsblitz.infrastructure.api.MonatsblitzApi>(relaxed = true)
        val playerDaoMock = mockk<de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.PlayerDao>(relaxed = true)
        val repo = TournamentRepositoryImpl(
            tournamentDao,
            tournamentPlayerDao,
            gameDao,
            database,
            tournamentStorage,
            monatsblitzApi = api,
            playerDao = playerDaoMock
        )
        val tournament = TournamentEntity(id = 1L, mode = GameMode.BLITZ_3_2, date = LocalDate.now(), rounds = 1, remoteId = 50L)
        coEvery { tournamentDao.getTournament(1L) } returns TournamentWithDetails(tournament, emptyList(), emptyList())
        coEvery { playerDaoMock.getPlayerById(10L) } returns de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.PlayerEntity(id = 10L, name = "P", vorname = "P", isLocal = false, remoteId = 100L)
        coEvery { api.addTournamentPlayer(any()) } throws RuntimeException("Remote API down")

        repo.addPlayerToTournament(1L, 10L)

        coVerify { tournamentPlayerDao.insertAll(any()) }
    }

    @Test
    fun `getRecentTournamentModes falls back to local tournaments when remote api fails`() = runTest {
        val api = mockk<de.kindermaenner.monatsblitz.infrastructure.api.MonatsblitzApi>()
        val repo = TournamentRepositoryImpl(
            tournamentDao,
            tournamentPlayerDao,
            gameDao,
            database,
            tournamentStorage,
            monatsblitzApi = api
        )
        coEvery { api.getTournaments() } throws RuntimeException("Network down")
        val localT = TournamentEntity(id = 1, mode = GameMode.BLITZ_5_0, date = LocalDate.now(), rounds = 1, remoteId = null)
        coEvery { tournamentDao.observeTournaments() } returns flowOf(listOf(TournamentWithDetails(localT, emptyList(), emptyList())))

        val modes = repo.getRecentTournamentModes()
        org.junit.Assert.assertEquals(listOf("5+0"), modes)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `addPlayerToTournament throws when tournament does not exist`() = runTest {
        coEvery { tournamentDao.getTournament(999L) } returns null

        repository.addPlayerToTournament(999L, 1L)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `addPlayerToTournament throws when player does not exist`() = runTest {
        val tournament = TournamentEntity(id = 1L, mode = GameMode.BLITZ_3_2, date = LocalDate.now(), rounds = 1, remoteId = null)
        coEvery { tournamentDao.getTournament(1L) } returns TournamentWithDetails(tournament, emptyList(), emptyList())
        val playerDaoMock = mockk<de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.PlayerDao>()
        coEvery { playerDaoMock.getPlayerById(555L) } returns null

        val repo = TournamentRepositoryImpl(
            tournamentDao,
            tournamentPlayerDao,
            gameDao,
            database,
            tournamentStorage,
            playerDao = playerDaoMock
        )

        repo.addPlayerToTournament(1L, 555L)
    }

    @Test
    fun `finalizeLocalTournament calls tournamentStorage finalizeTournament`() = runTest {
        repository.finalizeLocalTournament(1L)

        coVerify { tournamentStorage.finalizeTournament() }
    }

    @Test
    fun `updateTournamentRounds catches and logs exception when syncing dirty tournaments fails`() = runTest {
        val api = mockk<de.kindermaenner.monatsblitz.infrastructure.api.MonatsblitzApi>()
        coEvery { api.createTournament(any()) } throws RuntimeException("Network error")
        val dirtyEntity = TournamentEntity(
            id = 1L,
            mode = GameMode.BLITZ_3_2,
            date = LocalDate.now(),
            rounds = 1,
            remoteId = 100L,
            isLocalOnly = false,
            dirty = true
        )
        val tournamentWithDetails = TournamentWithDetails(dirtyEntity, emptyList(), emptyList())
        coEvery { tournamentDao.getTournament(1L) } returns tournamentWithDetails
        coEvery { tournamentDao.getDirtyTournaments() } returns listOf(tournamentWithDetails)

        val repo = TournamentRepositoryImpl(
            tournamentDao,
            tournamentPlayerDao,
            gameDao,
            database,
            tournamentStorage,
            monatsblitzApi = api
        )

        repo.updateTournamentRounds(1L, 2)

        coVerify { tournamentDao.updateTournamentRounds(1L, 2) }
    }
}
