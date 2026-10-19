package de.kindermaenner.monatsblitz.domain.usecase

import de.kindermaenner.monatsblitz.domain.model.GameMode
import de.kindermaenner.monatsblitz.infrastructure.api.MonatsblitzApi
import de.kindermaenner.monatsblitz.infrastructure.api.dto.CreateTournamentResponseDto
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.TournamentDao
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.TournamentEntity
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.relation.TournamentWithDetails
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.LocalDate

class SyncTournamentUseCaseTest {

    private val api = mockk<MonatsblitzApi>()
    private val dao = mockk<TournamentDao>(relaxed = true)
    private val useCase = SyncTournamentUseCase(api, dao)

    @Test
    fun `invoke syncs dirty tournaments with assigned player remoteIds to remote`() = runTest {
        val date = LocalDate.now()
        val tournamentEntity = TournamentEntity(id = 1, mode = GameMode.BLITZ_3_2, date = date, rounds = 1, remoteId = null)
        val players = listOf(
            de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.PlayerEntity(id = 1, name = "A", vorname = "A", remoteId = 101L),
            de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.PlayerEntity(id = 2, name = "B", vorname = "B", remoteId = 202L)
        )
        val dirtyTournament = TournamentWithDetails(tournamentEntity, listOf(), players)
        
        coEvery { dao.getDirtyTournaments() } returns listOf(dirtyTournament)
        coEvery { api.createTournament(any()) } returns CreateTournamentResponseDto(success = true, tournamentId = 500)

        useCase()

        coVerify { api.createTournament(match { it.mode == "3+2" && it.playerIds == listOf(101L, 202L) }) }
        coVerify { dao.updateTournamentRemoteId(1, 500) }
    }

    @Test
    fun `invoke should not update local db if api returns success false`() = runTest {
        val tournamentEntity = TournamentEntity(id = 1, mode = GameMode.BLITZ_3_2, date = LocalDate.now(), rounds = 1, remoteId = null)
        val dirtyTournament = TournamentWithDetails(tournamentEntity, listOf(), listOf())
        
        coEvery { dao.getDirtyTournaments() } returns listOf(dirtyTournament)
        coEvery { api.createTournament(any()) } returns CreateTournamentResponseDto(success = false, tournamentId = 0)

        useCase()

        coVerify { api.createTournament(any()) }
        coVerify(exactly = 0) { dao.updateTournamentRemoteId(any(), any()) }
    }

    @Test
    fun `invoke reuses existing open remote tournament on same date when HTTP 409 occurs`() = runTest {
        val date = LocalDate.of(2026, 10, 19)
        val tournamentEntity = TournamentEntity(id = 1, mode = GameMode.BLITZ_3_2, date = date, rounds = 1, remoteId = null)
        val dirtyTournament = TournamentWithDetails(tournamentEntity, listOf(), listOf())

        coEvery { dao.getDirtyTournaments() } returns listOf(dirtyTournament)
        val errorResponse = mockk<retrofit2.Response<*>>(relaxed = true) {
            io.mockk.every { code() } returns 409
        }
        coEvery { api.createTournament(any()) } throws retrofit2.HttpException(errorResponse)
        coEvery { api.getTournaments() } returns listOf(
            de.kindermaenner.monatsblitz.infrastructure.api.dto.TournamentDto(
                id = 777,
                date = "2026-10-19",
                mode = "3+2",
                roundCount = 1,
                status = "open"
            )
        )

        useCase()

        coVerify { dao.updateTournamentRemoteId(1, 777) }
    }
}
