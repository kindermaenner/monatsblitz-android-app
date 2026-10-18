package de.kindermaenner.monatsblitz.domain.usecase

import de.kindermaenner.monatsblitz.domain.model.GameMode
import de.kindermaenner.monatsblitz.infrastructure.TournamentStorage
import de.kindermaenner.monatsblitz.infrastructure.api.MonatsblitzApi
import de.kindermaenner.monatsblitz.infrastructure.api.dto.BatchResponseDto
import de.kindermaenner.monatsblitz.infrastructure.api.dto.CreateResultsDto
import de.kindermaenner.monatsblitz.infrastructure.api.dto.FinalizeRequestDto
import de.kindermaenner.monatsblitz.infrastructure.api.dto.FinalizeResponseDto
import de.kindermaenner.monatsblitz.infrastructure.api.dto.ResultDto
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.PlayerDao
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.TournamentDao
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.TournamentPlayerDao
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.PlayerEntity
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.TournamentEntity
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.TournamentPlayerCrossRef
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.relation.TournamentWithDetails
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.time.LocalDate

class FinalizeTournamentUseCaseTest {

    private val monatsblitzApi = mockk<MonatsblitzApi>()
    private val tournamentDao = mockk<TournamentDao>(relaxed = true)
    private val tournamentPlayerDao = mockk<TournamentPlayerDao>(relaxed = true)
    private val playerDao = mockk<PlayerDao>(relaxed = true)
    private val tournamentStorage = mockk<TournamentStorage>(relaxed = true)
    private val syncTournamentUseCase = mockk<SyncTournamentUseCase>(relaxed = true)
    private val syncGameResultsUseCase = mockk<SyncGameResultsUseCase>(relaxed = true)
    private val syncPlayersUseCase = mockk<SyncPlayersUseCase>(relaxed = true)
    private val createTournamentRankingsUseCase = mockk<CreateTournamentRankingsUseCase>(relaxed = true)

    private val useCase = FinalizeTournamentUseCase(
        monatsblitzApi = monatsblitzApi,
        tournamentDao = tournamentDao,
        tournamentPlayerDao = tournamentPlayerDao,
        playerDao = playerDao,
        tournamentStorage = tournamentStorage,
        syncTournamentUseCase = syncTournamentUseCase,
        syncGameResultsUseCase = syncGameResultsUseCase,
        syncPlayersUseCase = syncPlayersUseCase,
        createTournamentRankingsUseCase = createTournamentRankingsUseCase
    )

    @Test
    fun `invoke should finalize tournament successfully`() = runTest {
        val tournamentId = 1L
        val remoteTournamentId = 50L
        val date = LocalDate.now()

        val tournamentEntity = TournamentEntity(
            id = tournamentId,
            mode = GameMode.BLITZ_3_2,
            date = date,
            rounds = 1,
            remoteId = remoteTournamentId
        )
        val tournamentWithDetails = TournamentWithDetails(tournamentEntity, listOf(), listOf())

        val rankings = listOf(
            TournamentPlayerCrossRef(tournamentId = tournamentId, playerId = 10L, points = 3.0, rank = 1),
            TournamentPlayerCrossRef(tournamentId = tournamentId, playerId = 20L, points = 1.0, rank = 2)
        )
        val player1 = PlayerEntity(id = 10L, name = "Meier", vorname = "Alice", remoteId = 101L, dirty = false)
        val player2 = PlayerEntity(id = 20L, name = "Mueller", vorname = "Bob", remoteId = 102L, dirty = false)

        coEvery { tournamentDao.getTournament(tournamentId) } returns tournamentWithDetails
        coEvery { tournamentPlayerDao.getRankingForTournament(tournamentId) } returns rankings
        coEvery { playerDao.getPlayersByIds(listOf(10L, 20L)) } returns listOf(player1, player2)
        coEvery { monatsblitzApi.createResults(any()) } returns BatchResponseDto(success = true, count = 2, items = emptyList())
        coEvery { monatsblitzApi.finalize(FinalizeRequestDto(remoteTournamentId.toInt())) } returns FinalizeResponseDto(
            success = true,
            tournamentId = remoteTournamentId.toInt(),
            postId = 200
        )

        val response = useCase(tournamentId)

        assertTrue(response.success)
        assertEquals(200, response.postId)

        coVerify { syncPlayersUseCase() }
        coVerify { syncGameResultsUseCase() }
        coVerify { createTournamentRankingsUseCase(any()) }
        coVerify {
            monatsblitzApi.createResults(
                CreateResultsDto(
                    tournament_id = 50,
                    results = listOf(
                        ResultDto(player_id = 101, points = 3.0, rank = 1),
                        ResultDto(player_id = 102, points = 1.0, rank = 2)
                    )
                )
            )
        }
        coVerify { monatsblitzApi.finalize(FinalizeRequestDto(tournamentId = 50)) }
        coVerify { tournamentStorage.finalizeTournament() }
    }

    @Test
    fun `invoke should trigger syncTournamentUseCase if remoteId is initially null`() = runTest {
        val tournamentId = 1L
        val date = LocalDate.now()

        val initialEntity = TournamentEntity(id = tournamentId, mode = GameMode.BLITZ_3_2, date = date, rounds = 1, remoteId = null)
        val initialDetails = TournamentWithDetails(initialEntity, listOf(), listOf())

        val syncedEntity = TournamentEntity(id = tournamentId, mode = GameMode.BLITZ_3_2, date = date, rounds = 1, remoteId = 77L)
        val syncedDetails = TournamentWithDetails(syncedEntity, listOf(), listOf())

        coEvery { tournamentDao.getTournament(tournamentId) } returnsMany listOf(initialDetails, syncedDetails)
        coEvery { tournamentPlayerDao.getRankingForTournament(tournamentId) } returns listOf(
            TournamentPlayerCrossRef(tournamentId = tournamentId, playerId = 10L, points = 2.0, rank = 1)
        )
        coEvery { playerDao.getPlayersByIds(listOf(10L)) } returns listOf(
            PlayerEntity(id = 10L, name = "Meier", vorname = "Alice", remoteId = 101L, dirty = false)
        )
        coEvery { monatsblitzApi.createResults(any()) } returns BatchResponseDto(success = true, count = 1, items = emptyList())
        coEvery { monatsblitzApi.finalize(any()) } returns FinalizeResponseDto(success = true, tournamentId = 77)

        val response = useCase(tournamentId)

        assertTrue(response.success)
        coVerify { syncTournamentUseCase() }
        coVerify { monatsblitzApi.finalize(FinalizeRequestDto(tournamentId = 77)) }
    }

    @Test
    fun `invoke should throw exception when tournament is not found`() = runTest {
        coEvery { tournamentDao.getTournament(999L) } returns null

        try {
            useCase(999L)
            fail("Should throw IllegalStateException")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("nicht gefunden") == true)
        }
    }

    @Test
    fun `invoke should throw exception when remote finalize fails`() = runTest {
        val tournamentId = 1L
        val remoteTournamentId = 50L
        val tournamentEntity = TournamentEntity(id = tournamentId, mode = GameMode.BLITZ_3_2, date = LocalDate.now(), rounds = 1, remoteId = remoteTournamentId)
        val tournamentWithDetails = TournamentWithDetails(tournamentEntity, listOf(), listOf())

        coEvery { tournamentDao.getTournament(tournamentId) } returns tournamentWithDetails
        coEvery { tournamentPlayerDao.getRankingForTournament(tournamentId) } returns listOf(
            TournamentPlayerCrossRef(tournamentId = tournamentId, playerId = 10L, points = 1.0, rank = 1)
        )
        coEvery { playerDao.getPlayersByIds(any()) } returns listOf(
            PlayerEntity(id = 10L, name = "Meier", vorname = "Alice", remoteId = 101L, dirty = false)
        )
        coEvery { monatsblitzApi.createResults(any()) } returns BatchResponseDto(success = true, count = 1, items = emptyList())
        coEvery { monatsblitzApi.finalize(any()) } returns FinalizeResponseDto(success = false)

        try {
            useCase(tournamentId)
            fail("Should throw IllegalStateException")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("fehlgeschlagen") == true)
        }
        coVerify(exactly = 0) { tournamentStorage.finalizeTournament() }
    }
}
