package de.kindermaenner.monatsblitz.ui.tournamentsetup

import de.kindermaenner.monatsblitz.domain.model.GameMode
import de.kindermaenner.monatsblitz.domain.model.Player
import de.kindermaenner.monatsblitz.domain.repository.PlayerRepository
import de.kindermaenner.monatsblitz.domain.usecase.AddPlayerUseCase
import de.kindermaenner.monatsblitz.domain.usecase.CreateTournamentUseCase
import de.kindermaenner.monatsblitz.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TournamentSetupViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val playerRepository = mockk<PlayerRepository>()
    private val createTournamentUseCase = mockk<CreateTournamentUseCase>()
    private val addPlayerUseCase = mockk<AddPlayerUseCase>()

    @Test
    fun `initial state loads players from repository`() = runTest {
        val players = listOf(Player(1, "N", "V"))
        coEvery { playerRepository.observePlayers() } returns flowOf(players)

        val viewModel = TournamentSetupViewModel(playerRepository, createTournamentUseCase, addPlayerUseCase)

        assertEquals(players, viewModel.uiState.value.players)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `togglePlayer updates selected ids`() = runTest {
        coEvery { playerRepository.observePlayers() } returns flowOf(emptyList())
        val viewModel = TournamentSetupViewModel(playerRepository, createTournamentUseCase, addPlayerUseCase)

        viewModel.togglePlayer(1L)
        assertTrue(viewModel.uiState.value.selectedPlayerIds.contains(1L))

        viewModel.togglePlayer(1L)
        assertFalse(viewModel.uiState.value.selectedPlayerIds.contains(1L))
    }

    @Test
    fun `search query and filter updates state`() = runTest {
        coEvery { playerRepository.observePlayers() } returns flowOf(emptyList())
        val viewModel = TournamentSetupViewModel(playerRepository, createTournamentUseCase, addPlayerUseCase)

        viewModel.onSearchQueryChanged("Müller")
        assertEquals("Müller", viewModel.uiState.value.searchQuery)

        viewModel.onShowOnlySelectedChanged(true)
        assertTrue(viewModel.uiState.value.showOnlySelected)

        viewModel.onPlayerChecked(1L, true)
        viewModel.onPlayerChecked(2L, true)
        assertEquals(2, viewModel.uiState.value.selectedPlayerIds.size)

        viewModel.clearSelectedPlayers()
        assertTrue(viewModel.uiState.value.selectedPlayerIds.isEmpty())
    }

    @Test
    fun `matchesPlayerSearch handles umlauts seamlessly`() {
        val player1 = Player(1, "Mühe", "Jörn")
        val player2 = Player(2, "Müller", "Anna")
        val player3 = Player(3, "Weiß", "Stefan")

        // Jörn Mühe matches with or without umlauts
        assertTrue(matchesPlayerSearch(player1, "Jörn"))
        assertTrue(matchesPlayerSearch(player1, "Joern"))
        assertTrue(matchesPlayerSearch(player1, "Jorn"))
        assertTrue(matchesPlayerSearch(player1, "Mühe"))
        assertTrue(matchesPlayerSearch(player1, "Muehe"))
        assertTrue(matchesPlayerSearch(player1, "Muhe"))

        // Müller matches
        assertTrue(matchesPlayerSearch(player2, "Müller"))
        assertTrue(matchesPlayerSearch(player2, "Mueller"))
        assertTrue(matchesPlayerSearch(player2, "Muller"))

        // Weiß matches
        assertTrue(matchesPlayerSearch(player3, "Weiß"))
        assertTrue(matchesPlayerSearch(player3, "Weiss"))
    }

    @Test
    fun `createTournament calls usecase with selected players`() = runTest {
        val p1 = Player(1, "A", "A")
        val p2 = Player(2, "B", "B")
        val mockTournament = mockk<de.kindermaenner.monatsblitz.domain.model.Tournament>()
        io.mockk.every { mockTournament.id } returns 123L
        
        coEvery { playerRepository.observePlayers() } returns flowOf(listOf(p1, p2))
        coEvery { createTournamentUseCase(any(), any(), any(), any()) } returns mockTournament

        val viewModel = TournamentSetupViewModel(playerRepository, createTournamentUseCase, addPlayerUseCase)
        viewModel.togglePlayer(1L)
        viewModel.onModeChanged(GameMode.BLITZ_5_0)
        
        viewModel.createTournament()

        coVerify { createTournamentUseCase(
            players = listOf(p1),
            mode = GameMode.BLITZ_5_0,
            date = any(),
            rounds = 1
        ) }
    }

    @Test
    fun `addNewPlayer success selects new player, closes dialog, and clears fields`() = runTest {
        coEvery { playerRepository.observePlayers() } returns flowOf(emptyList())
        val newPlayer = Player(99L, "Mustermann", "Max")
        coEvery { addPlayerUseCase("Max", "Mustermann") } returns newPlayer

        val viewModel = TournamentSetupViewModel(playerRepository, createTournamentUseCase, addPlayerUseCase)
        viewModel.onShowAddPlayerDialog(true)
        viewModel.onNewPlayerNameChanged("Max", "Mustermann")

        viewModel.addNewPlayer()

        assertTrue(viewModel.uiState.value.selectedPlayerIds.contains(99L))
        assertFalse(viewModel.uiState.value.showAddPlayerDialog)
        assertEquals("", viewModel.uiState.value.newPlayerVorname)
        assertEquals("", viewModel.uiState.value.newPlayerName)
        assertEquals(null, viewModel.uiState.value.addPlayerError)
    }

    @Test
    fun `addNewPlayer duplicate sets addPlayerError and leaves dialog open`() = runTest {
        coEvery { playerRepository.observePlayers() } returns flowOf(emptyList())
        val errorMessage = "Ein Spieler mit dem Namen 'Max Mustermann' existiert bereits."
        coEvery { addPlayerUseCase("Max", "Mustermann") } throws IllegalArgumentException(errorMessage)

        val viewModel = TournamentSetupViewModel(playerRepository, createTournamentUseCase, addPlayerUseCase)
        viewModel.onShowAddPlayerDialog(true)
        viewModel.onNewPlayerNameChanged("Max", "Mustermann")

        viewModel.addNewPlayer()

        assertTrue(viewModel.uiState.value.showAddPlayerDialog)
        assertEquals(errorMessage, viewModel.uiState.value.addPlayerError)
        assertFalse(viewModel.uiState.value.selectedPlayerIds.contains(99L))

        // Typing again clears error
        viewModel.onNewPlayerNameChanged("Maxi", "Mustermann")
        assertEquals(null, viewModel.uiState.value.addPlayerError)
    }

    @Test
    fun `addNewPlayer with blank input does nothing`() = runTest {
        coEvery { playerRepository.observePlayers() } returns flowOf(emptyList())
        val viewModel = TournamentSetupViewModel(playerRepository, createTournamentUseCase, addPlayerUseCase)
        viewModel.onNewPlayerNameChanged("", "   ")

        viewModel.addNewPlayer()

        coVerify(exactly = 0) { addPlayerUseCase(any(), any()) }
    }

    @Test
    fun `createTournament with doubleRound enables 2 rounds`() = runTest {
        val p1 = Player(1, "A", "A")
        val mockTournament = mockk<de.kindermaenner.monatsblitz.domain.model.Tournament>()
        io.mockk.every { mockTournament.id } returns 123L

        coEvery { playerRepository.observePlayers() } returns flowOf(listOf(p1))
        coEvery { createTournamentUseCase(any(), any(), any(), any()) } returns mockTournament

        val viewModel = TournamentSetupViewModel(playerRepository, createTournamentUseCase, addPlayerUseCase)
        viewModel.onPlayerChecked(1L, true)
        viewModel.onModeChanged(GameMode.BLITZ_3_2)
        viewModel.onDoubleRoundChanged(true)

        viewModel.createTournament()

        coVerify { createTournamentUseCase(
            players = listOf(p1),
            mode = GameMode.BLITZ_3_2,
            date = any(),
            rounds = 2
        ) }
    }

    @Test
    fun `onPlayerChecked with false removes player`() = runTest {
        coEvery { playerRepository.observePlayers() } returns flowOf(emptyList())
        val viewModel = TournamentSetupViewModel(playerRepository, createTournamentUseCase, addPlayerUseCase)

        viewModel.onPlayerChecked(1L, true)
        assertTrue(viewModel.uiState.value.selectedPlayerIds.contains(1L))

        viewModel.onPlayerChecked(1L, false)
        assertFalse(viewModel.uiState.value.selectedPlayerIds.contains(1L))
    }
}
