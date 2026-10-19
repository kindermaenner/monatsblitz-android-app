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
}
