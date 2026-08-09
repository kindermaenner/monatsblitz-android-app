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
    fun `invoke syncs dirty tournaments to remote`() = runTest {
        val date = LocalDate.now()
        val tournamentEntity = TournamentEntity(id = 1, mode = GameMode.BLITZ_3_2, date = date, rounds = 1, remoteId = null)
        val dirtyTournament = TournamentWithDetails(tournamentEntity, listOf(), listOf())
        
        coEvery { dao.getDirtyTournaments() } returns listOf(dirtyTournament)
        coEvery { api.createTournament(any()) } returns CreateTournamentResponseDto(success = true, tournament_id = 500)

        useCase()

        coVerify { api.createTournament(match { it.mode == "3+2" }) }
        coVerify { dao.updateTournamentRemoteId(1, 500) }
    }
}
