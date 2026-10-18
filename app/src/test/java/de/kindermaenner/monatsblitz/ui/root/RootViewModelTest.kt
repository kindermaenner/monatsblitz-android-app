package de.kindermaenner.monatsblitz.ui.root

import de.kindermaenner.monatsblitz.domain.usecase.SyncPlayersUseCase
import de.kindermaenner.monatsblitz.infrastructure.TournamentState
import de.kindermaenner.monatsblitz.infrastructure.TournamentStorage
import de.kindermaenner.monatsblitz.ui.navigation.AppRoute
import de.kindermaenner.monatsblitz.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class RootViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val tournamentStorage = mockk<TournamentStorage>()
    private val syncPlayersUseCase = mockk<SyncPlayersUseCase>(relaxed = true)

    @Test
    fun `init should determine initial route as TournamentSetup when no tournament is active`() = runTest {
        coEvery { tournamentStorage.getTournamentState() } returns flowOf(null)

        val viewModel = RootViewModel(tournamentStorage, syncPlayersUseCase)

        assertEquals(AppRoute.TournamentSetup, viewModel.initialRoute.value)
        coVerify { syncPlayersUseCase() }
    }

    @Test
    fun `init should determine initial route as Crosstable when tournament is active`() = runTest {
        val activeTournamentId = 123L
        coEvery { tournamentStorage.getTournamentState() } returns flowOf(TournamentState(activeTournamentId, false))

        val viewModel = RootViewModel(tournamentStorage, syncPlayersUseCase)

        val route = viewModel.initialRoute.value as AppRoute.Crosstable
        assertEquals(activeTournamentId, route.id)
        coVerify { syncPlayersUseCase() }
    }

    @Test
    fun `init should handle sync failure gracefully`() = runTest {
        coEvery { syncPlayersUseCase() } throws Exception("Sync failed")
        coEvery { tournamentStorage.getTournamentState() } returns flowOf(null)

        val viewModel = RootViewModel(tournamentStorage, syncPlayersUseCase)

        assertEquals(AppRoute.TournamentSetup, viewModel.initialRoute.value)
    }

    @Test
    fun `init should determine initial route as TournamentSetup when tournament is locally finalized`() = runTest {
        val activeTournamentId = 123L
        coEvery { tournamentStorage.getTournamentState() } returns flowOf(TournamentState(activeTournamentId, finalized = true))

        val viewModel = RootViewModel(tournamentStorage, syncPlayersUseCase)

        assertEquals(AppRoute.TournamentSetup, viewModel.initialRoute.value)
    }

    @Test
    fun `init should finalize local storage and route to TournamentSetup when remote status is finalized`() = runTest {
        val activeTournamentId = 123L
        val remoteId = 99L
        val tournamentDao = mockk<de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.TournamentDao>()
        val monatsblitzApi = mockk<de.kindermaenner.monatsblitz.infrastructure.api.MonatsblitzApi>()
        val tournamentWithDetails = mockk<de.kindermaenner.monatsblitz.infrastructure.persistence.room.relation.TournamentWithDetails>()
        val tournamentEntity = mockk<de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.TournamentEntity>()

        coEvery { tournamentStorage.getTournamentState() } returns flowOf(TournamentState(activeTournamentId, finalized = false))
        coEvery { tournamentStorage.finalizeTournament() } returns Unit
        coEvery { tournamentDao.getTournament(activeTournamentId) } returns tournamentWithDetails
        coEvery { tournamentWithDetails.tournament } returns tournamentEntity
        coEvery { tournamentEntity.remoteId } returns remoteId
        coEvery { monatsblitzApi.getTournament(remoteId.toInt()) } returns de.kindermaenner.monatsblitz.infrastructure.api.dto.TournamentDto(
            id = remoteId.toInt(),
            date = "2026-10-18",
            mode = "3+2",
            round_count = 1,
            status = "finalized"
        )

        val viewModel = RootViewModel(tournamentStorage, syncPlayersUseCase, tournamentDao, monatsblitzApi)

        assertEquals(AppRoute.TournamentSetup, viewModel.initialRoute.value)
        coVerify { tournamentStorage.finalizeTournament() }
    }

    @Test
    fun `init should route to Crosstable when remote status is open`() = runTest {
        val activeTournamentId = 123L
        val remoteId = 99L
        val tournamentDao = mockk<de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.TournamentDao>()
        val monatsblitzApi = mockk<de.kindermaenner.monatsblitz.infrastructure.api.MonatsblitzApi>()
        val tournamentWithDetails = mockk<de.kindermaenner.monatsblitz.infrastructure.persistence.room.relation.TournamentWithDetails>()
        val tournamentEntity = mockk<de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.TournamentEntity>()

        coEvery { tournamentStorage.getTournamentState() } returns flowOf(TournamentState(activeTournamentId, finalized = false))
        coEvery { tournamentDao.getTournament(activeTournamentId) } returns tournamentWithDetails
        coEvery { tournamentWithDetails.tournament } returns tournamentEntity
        coEvery { tournamentEntity.remoteId } returns remoteId
        coEvery { monatsblitzApi.getTournament(remoteId.toInt()) } returns de.kindermaenner.monatsblitz.infrastructure.api.dto.TournamentDto(
            id = remoteId.toInt(),
            date = "2026-10-18",
            mode = "3+2",
            round_count = 1,
            status = "open"
        )

        val viewModel = RootViewModel(tournamentStorage, syncPlayersUseCase, tournamentDao, monatsblitzApi)

        val route = viewModel.initialRoute.value as AppRoute.Crosstable
        assertEquals(activeTournamentId, route.id)
    }

    @Test
    fun `init should fallback to Crosstable when remote check fails due to network error`() = runTest {
        val activeTournamentId = 123L
        val remoteId = 99L
        val tournamentDao = mockk<de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.TournamentDao>()
        val monatsblitzApi = mockk<de.kindermaenner.monatsblitz.infrastructure.api.MonatsblitzApi>()
        val tournamentWithDetails = mockk<de.kindermaenner.monatsblitz.infrastructure.persistence.room.relation.TournamentWithDetails>()
        val tournamentEntity = mockk<de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.TournamentEntity>()

        coEvery { tournamentStorage.getTournamentState() } returns flowOf(TournamentState(activeTournamentId, finalized = false))
        coEvery { tournamentDao.getTournament(activeTournamentId) } returns tournamentWithDetails
        coEvery { tournamentWithDetails.tournament } returns tournamentEntity
        coEvery { tournamentEntity.remoteId } returns remoteId
        coEvery { monatsblitzApi.getTournament(remoteId.toInt()) } throws RuntimeException("Network error")

        val viewModel = RootViewModel(tournamentStorage, syncPlayersUseCase, tournamentDao, monatsblitzApi)

        val route = viewModel.initialRoute.value as AppRoute.Crosstable
        assertEquals(activeTournamentId, route.id)
    }
}
