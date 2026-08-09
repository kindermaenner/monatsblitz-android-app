package de.kindermaenner.monatsblitz.infrastructure.repository

import android.util.Log
import androidx.room.withTransaction
import de.kindermaenner.monatsblitz.domain.model.GameMode
import de.kindermaenner.monatsblitz.domain.model.GameResult
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
        assertEquals(1L, result[0].Id)
    }

    @Test
    fun `createTournament inserts all related entities within transaction`() = runTest {
        val date = LocalDate.now()
        val players = listOf(Player(1, "A", "A"))
        val request = NewTournament(
            Mode = GameMode.BLITZ_3_2,
            Date = date,
            rounds = 1,
            players = players,
            games = listOf()
        )

        coEvery { tournamentDao.insert(any()) } returns 10L
        coEvery { gameDao.insertAll(any()) } returns listOf()
        coEvery { tournamentPlayerDao.insertAll(any()) } returns Unit

        val result = repository.createTournament(request)

        assertEquals(10L, result.Id)
        coVerify { tournamentDao.insert(match { it.mode == GameMode.BLITZ_3_2 }) }
        coVerify { tournamentPlayerDao.insertAll(match { it.size == 1 && it[0].tournamentId == 10L }) }
        coVerify { tournamentStorage.saveTournamentState(10L, false) }
    }

    @Test
    fun `updateGameResult updates existing game if found`() = runTest {
        val gameEntity = GameEntity(id = 100, tournamentId = 1, player1Id = 1, player2Id = 2, leg = 1, result = GameResult.Open)
        coEvery { gameDao.getGameByPlayers(1, 1, 2, 1) } returns gameEntity

        repository.updateGameResult(1, 1, 2, 1, GameResult.Win)

        coVerify { gameDao.update(match { it.id == 100L && it.result == GameResult.Win && it.dirty }) }
    }

    @Test
    fun `updateGameResult inserts new game if not found`() = runTest {
        coEvery { gameDao.getGameByPlayers(1, 1, 2, 1) } returns null

        repository.updateGameResult(1, 1, 2, 1, GameResult.Win)

        coVerify { gameDao.insert(match { it.tournamentId == 1L && it.player1Id == 1L && it.result == GameResult.Win }) }
    }
}
