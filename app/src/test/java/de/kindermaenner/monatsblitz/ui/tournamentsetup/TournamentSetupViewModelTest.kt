package de.kindermaenner.monatsblitz.ui.tournamentsetup

import de.kindermaenner.monatsblitz.domain.model.GameMode
import de.kindermaenner.monatsblitz.domain.model.ModePreselectionType
import de.kindermaenner.monatsblitz.domain.model.PlayerFilterMode
import de.kindermaenner.monatsblitz.domain.repository.ModeRepository
import java.time.LocalDate
import de.kindermaenner.monatsblitz.domain.model.Tournament
import de.kindermaenner.monatsblitz.domain.model.Player
import de.kindermaenner.monatsblitz.domain.model.PlayerSortField
import de.kindermaenner.monatsblitz.domain.model.PlayerSortOrder
import de.kindermaenner.monatsblitz.domain.repository.PlayerRepository
import de.kindermaenner.monatsblitz.domain.repository.RemoteOpenTournamentInfo
import de.kindermaenner.monatsblitz.domain.repository.TournamentRepository
import de.kindermaenner.monatsblitz.domain.usecase.AddPlayerUseCase
import de.kindermaenner.monatsblitz.domain.usecase.CreateTournamentUseCase
import de.kindermaenner.monatsblitz.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import de.kindermaenner.monatsblitz.domain.repository.SettingsRepository
import de.kindermaenner.monatsblitz.domain.usecase.SyncPlayersUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@kotlinx.coroutines.ExperimentalCoroutinesApi
class TournamentSetupViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val playerRepository = mockk<PlayerRepository>()
    private val createTournamentUseCase = mockk<CreateTournamentUseCase>()
    private val addPlayerUseCase = mockk<AddPlayerUseCase>()
    private val syncPlayersUseCase = mockk<SyncPlayersUseCase>(relaxed = true)
    private val settingsRepository = mockk<SettingsRepository>(relaxed = true)

    @org.junit.Before
    fun setUp() {
        coEvery { settingsRepository.isServerConfigured() } returns true
        coEvery { settingsRepository.isServerConfigured } returns flowOf(true)
        coEvery { settingsRepository.isConfigured() } returns true
        coEvery { settingsRepository.isConfigured } returns flowOf(true)
        coEvery { settingsRepository.standaloneOfflineMode } returns flowOf(false)
        coEvery { settingsRepository.showDetailedSyncIndicators } returns flowOf(true)
        coEvery { settingsRepository.playerSortField } returns flowOf(PlayerSortField.LAST_NAME)
        coEvery { settingsRepository.playerSortOrder } returns flowOf(PlayerSortOrder.ASCENDING)
    }

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
        assertEquals(PlayerFilterMode.SELECTED, viewModel.uiState.value.playerFilterMode)

        viewModel.setPlayerFilterMode(PlayerFilterMode.ALL)
        assertEquals(PlayerFilterMode.ALL, viewModel.uiState.value.playerFilterMode)
        assertFalse(viewModel.uiState.value.showOnlySelected)

        viewModel.setPlayerFilterMode(PlayerFilterMode.ACTIVE)
        assertEquals(PlayerFilterMode.ACTIVE, viewModel.uiState.value.playerFilterMode)
        assertFalse(viewModel.uiState.value.showOnlySelected)

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
    fun `onDateChanged updates selectedDate and is passed to createTournament`() = runTest {
        val p1 = Player(1, "A", "A")
        val mockTournament = mockk<Tournament>()
        io.mockk.every { mockTournament.id } returns 456L

        coEvery { playerRepository.observePlayers() } returns flowOf(listOf(p1))
        coEvery { createTournamentUseCase(any(), any(), any(), any()) } returns mockTournament

        val viewModel = TournamentSetupViewModel(playerRepository, createTournamentUseCase, addPlayerUseCase)
        viewModel.togglePlayer(1L)
        viewModel.onModeChanged(GameMode.BLITZ_5_0)

        val customDate = LocalDate.of(2025, 11, 15)
        viewModel.onDateChanged(customDate)

        assertEquals(customDate, viewModel.uiState.value.selectedDate)

        viewModel.createTournament()

        coVerify { createTournamentUseCase(
            players = listOf(p1),
            mode = any(),
            date = customDate,
            rounds = any()
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

    @Test
    fun `aborting setup before starting tournament does not create any entries in DB`() = runTest {
        val p1 = Player(1, "A", "A")
        val p2 = Player(2, "B", "B")
        coEvery { playerRepository.observePlayers() } returns flowOf(listOf(p1, p2))

        val viewModel = TournamentSetupViewModel(playerRepository, createTournamentUseCase, addPlayerUseCase)

        // User selects players, changes mode and rounds, then aborts/clears before pressing Start
        viewModel.onPlayerChecked(1L, true)
        viewModel.onPlayerChecked(2L, true)
        viewModel.onModeChanged(GameMode.BLITZ_5_0)
        viewModel.onDoubleRoundChanged(true)
        viewModel.clearSelectedPlayers()

        assertTrue(viewModel.uiState.value.selectedPlayerIds.isEmpty())
        coVerify(exactly = 0) { createTournamentUseCase(any(), any(), any(), any()) }
    }

    @Test
    fun `syncPlayers executes syncPlayersUseCase and resets syncing state`() = runTest {
        coEvery { playerRepository.observePlayers() } returns flowOf(emptyList())
        val viewModel = TournamentSetupViewModel(
            playerRepository,
            createTournamentUseCase,
            addPlayerUseCase,
            syncPlayersUseCase = syncPlayersUseCase,
            settingsRepository = settingsRepository
        )

        viewModel.syncPlayers()

        coVerify(exactly = 1) { syncPlayersUseCase.invoke() }
        assertFalse(viewModel.uiState.value.isSyncingPlayers)
    }

    @Test
    fun `addNewPlayer triggers background sync on success`() = runTest {
        coEvery { playerRepository.observePlayers() } returns flowOf(emptyList())
        val newPlayer = Player(42L, "Mustermann", "Erika")
        coEvery { addPlayerUseCase("Erika", "Mustermann") } returns newPlayer

        val viewModel = TournamentSetupViewModel(
            playerRepository,
            createTournamentUseCase,
            addPlayerUseCase,
            syncPlayersUseCase = syncPlayersUseCase,
            settingsRepository = settingsRepository
        )
        viewModel.onShowAddPlayerDialog(true)
        viewModel.onNewPlayerNameChanged("Erika", "Mustermann")

        viewModel.addNewPlayer()

        coVerify(exactly = 1) { syncPlayersUseCase.invoke() }
        assertTrue(viewModel.uiState.value.selectedPlayerIds.contains(42L))
    }

    @Test
    fun `settingsRepository observes showDetailedSyncIndicators`() = runTest {
        coEvery { playerRepository.observePlayers() } returns flowOf(emptyList())
        val syncIndicatorsFlow = MutableStateFlow(true)
        coEvery { settingsRepository.showDetailedSyncIndicators } returns syncIndicatorsFlow

        val viewModel = TournamentSetupViewModel(
            playerRepository,
            createTournamentUseCase,
            addPlayerUseCase,
            syncPlayersUseCase = syncPlayersUseCase,
            settingsRepository = settingsRepository
        )

        assertTrue(viewModel.uiState.value.showDetailedSyncIndicators)

        syncIndicatorsFlow.value = false
        assertFalse(viewModel.uiState.value.showDetailedSyncIndicators)
    }

    @Test
    fun `players are sorted by first name ascending by default and can be toggled`() = runTest {
        val p1 = Player(1, "Mueller", "Alice")
        val p2 = Player(2, "Meier", "Bob")
        coEvery { playerRepository.observePlayers() } returns flowOf(listOf(p1, p2))

        val viewModel = TournamentSetupViewModel(playerRepository, createTournamentUseCase, addPlayerUseCase)

        // Default: First name ascending -> Alice (p1), Bob (p2)
        assertEquals(listOf(p1, p2), viewModel.uiState.value.players)
        assertEquals(PlayerSortField.FIRST_NAME, viewModel.uiState.value.sortField)
        assertEquals(PlayerSortOrder.ASCENDING, viewModel.uiState.value.sortOrder)

        // Toggle sort order -> Descending: Bob (p2), Alice (p1)
        viewModel.toggleSortOrder()
        assertEquals(listOf(p2, p1), viewModel.uiState.value.players)
        assertEquals(PlayerSortOrder.DESCENDING, viewModel.uiState.value.sortOrder)

        // Toggle sort field -> Last name descending: Mueller (p1), Meier (p2)
        viewModel.toggleSortField()
        assertEquals(listOf(p1, p2), viewModel.uiState.value.players)
        assertEquals(PlayerSortField.LAST_NAME, viewModel.uiState.value.sortField)

        // Toggle order -> Last name ascending: Meier (p2), Mueller (p1)
        viewModel.toggleSortOrder()
        assertEquals(listOf(p2, p1), viewModel.uiState.value.players)
        assertEquals(PlayerSortOrder.ASCENDING, viewModel.uiState.value.sortOrder)
    }

    @Test
    fun `shows remote open tournament dialog when remote tournament found and autoAdopt disabled`() = runTest {
        val players = listOf(Player(1, "N", "V"))
        coEvery { playerRepository.observePlayers() } returns flowOf(players)
        val tournamentRepo = mockk<TournamentRepository>()
        val remoteInfo = RemoteOpenTournamentInfo(id = 10, date = "2026-10-21", mode = "3+2", roundCount = 1, playerCount = 6)
        coEvery { tournamentRepo.getRemoteOpenTournament() } returns remoteInfo
        coEvery { settingsRepository.autoAdoptOpenTournament } returns flowOf(false)

        val viewModel = TournamentSetupViewModel(
            playerRepository,
            createTournamentUseCase,
            addPlayerUseCase,
            syncPlayersUseCase,
            settingsRepository,
            tournamentRepo
        )
        advanceUntilIdle()

        assertEquals(remoteInfo, viewModel.uiState.value.remoteOpenTournament)

        viewModel.dismissRemoteTournamentDialog()
        assertEquals(null, viewModel.uiState.value.remoteOpenTournament)
    }

    @Test
    fun `adoptRemoteTournament triggers repository and emits navigation`() = runTest {
        val players = listOf(Player(1, "N", "V"))
        coEvery { playerRepository.observePlayers() } returns flowOf(players)
        val tournamentRepo = mockk<TournamentRepository>()
        val remoteInfo = RemoteOpenTournamentInfo(id = 10, date = "2026-10-21", mode = "3+2", roundCount = 1, playerCount = 6)
        coEvery { tournamentRepo.getRemoteOpenTournament() } returns remoteInfo
        coEvery { tournamentRepo.adoptRemoteTournament(10) } returns 42L
        coEvery { settingsRepository.autoAdoptOpenTournament } returns flowOf(false)

        val viewModel = TournamentSetupViewModel(
            playerRepository,
            createTournamentUseCase,
            addPlayerUseCase,
            syncPlayersUseCase,
            settingsRepository,
            tournamentRepo
        )
        advanceUntilIdle()

        var navigatedId: Long? = null
        backgroundScope.launch(kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)) {
            viewModel.navigationEffect.collect { navigatedId = it }
        }

        viewModel.adoptRemoteTournament()
        advanceUntilIdle()
        assertEquals(42L, navigatedId)
    }

    @Test
    fun `preselection fixed mode selects configured mode`() = runTest {
        coEvery { playerRepository.observePlayers() } returns flowOf(emptyList())
        val settings = mockk<SettingsRepository>(relaxed = true)
        coEvery { settings.modePreselectionType } returns flowOf(ModePreselectionType.FIXED)
        coEvery { settings.fixedMode } returns flowOf("5+0")

        val viewModel = TournamentSetupViewModel(
            playerRepository = playerRepository,
            createTournamentUseCase = createTournamentUseCase,
            addPlayerUseCase = addPlayerUseCase,
            settingsRepository = settings
        )
        advanceUntilIdle()

        assertEquals("5+0", viewModel.uiState.value.selectedMode?.name)
    }

    @Test
    fun `preselection rotation mode calculates next mode and skips one-shots`() = runTest {
        coEvery { playerRepository.observePlayers() } returns flowOf(emptyList())
        val settings = mockk<SettingsRepository>(relaxed = true)
        coEvery { settings.modePreselectionType } returns flowOf(ModePreselectionType.ROTATION)
        coEvery { settings.rotationModes } returns flowOf(listOf("3+2", "5+0"))

        val tournamentRepo = mockk<TournamentRepository>(relaxed = true)
        // Last tournament was a one-shot ("Jubilaeumsblitz"), before that was "3+2"
        coEvery { tournamentRepo.getRecentTournamentModes() } returns listOf("Jubilaeumsblitz", "3+2", "5+0")

        val viewModel = TournamentSetupViewModel(
            playerRepository = playerRepository,
            createTournamentUseCase = createTournamentUseCase,
            addPlayerUseCase = addPlayerUseCase,
            settingsRepository = settings,
            tournamentRepository = tournamentRepo
        )
        advanceUntilIdle()

        assertEquals("5+0", viewModel.uiState.value.selectedMode?.name)
    }

    @Test
    fun `add new mode permanent workflow shows confirm dialog and saves to repository`() = runTest {
        coEvery { playerRepository.observePlayers() } returns flowOf(emptyList())
        val modeRepo = mockk<ModeRepository>(relaxed = true)
        val createdMode = GameMode(id = 10L, remoteId = 100L, name = "10+0")
        coEvery { modeRepo.observeModes() } returns flowOf(listOf(GameMode.BLITZ_3_2, GameMode.BLITZ_5_0))
        coEvery { modeRepo.createMode("10+0", permanent = true) } returns createdMode

        val viewModel = TournamentSetupViewModel(
            playerRepository = playerRepository,
            createTournamentUseCase = createTournamentUseCase,
            addPlayerUseCase = addPlayerUseCase,
            modeRepository = modeRepo
        )
        advanceUntilIdle()

        viewModel.onShowAddModeDialog(true)
        viewModel.onNewModeNameChanged("10+0")
        viewModel.onNewModePermanentChanged(true)
        viewModel.onConfirmAddMode()

        // Confirmation dialog is shown
        assertTrue(viewModel.uiState.value.showConfirmAddModeDialog)

        // Confirm
        viewModel.executeAddMode(permanent = true)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showConfirmAddModeDialog)
        assertFalse(viewModel.uiState.value.showAddModeDialog)
        assertEquals("10+0", viewModel.uiState.value.selectedMode?.name)
        coVerify { modeRepo.createMode("10+0", permanent = true) }
    }

    @Test
    fun `add new mode one-shot workflow sets selectedMode directly without confirm dialog`() = runTest {
        coEvery { playerRepository.observePlayers() } returns flowOf(emptyList())
        val modeRepo = mockk<ModeRepository>(relaxed = true)

        val viewModel = TournamentSetupViewModel(
            playerRepository = playerRepository,
            createTournamentUseCase = createTournamentUseCase,
            addPlayerUseCase = addPlayerUseCase,
            modeRepository = modeRepo
        )
        advanceUntilIdle()

        viewModel.onShowAddModeDialog(true)
        viewModel.onNewModeNameChanged("Jubilaeumsblitz")
        viewModel.onNewModePermanentChanged(false)
        viewModel.onConfirmAddMode()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showConfirmAddModeDialog)
        assertFalse(viewModel.uiState.value.showAddModeDialog)
        assertEquals("Jubilaeumsblitz", viewModel.uiState.value.selectedMode?.name)
        coVerify(exactly = 0) { modeRepo.createMode(any(), any()) }
    }

    @Test
    fun `when standaloneOfflineMode is enabled, tournament setup defaults to local only`() = runTest {
        coEvery { playerRepository.observePlayers() } returns flowOf(emptyList())
        coEvery { settingsRepository.standaloneOfflineMode } returns flowOf(true)

        val viewModel = TournamentSetupViewModel(
            playerRepository = playerRepository,
            createTournamentUseCase = createTournamentUseCase,
            addPlayerUseCase = addPlayerUseCase,
            settingsRepository = settingsRepository
        )
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isStandaloneOffline)
        assertTrue(viewModel.uiState.value.isLocalOnly)
    }

    @Test
    fun `when settings change from offline to server configured, viewModel dynamically updates server state`() = runTest {
        coEvery { playerRepository.observePlayers() } returns flowOf(emptyList())
        val offlineFlow = kotlinx.coroutines.flow.MutableStateFlow(true)
        val serverConfiguredFlow = kotlinx.coroutines.flow.MutableStateFlow(false)

        coEvery { settingsRepository.standaloneOfflineMode } returns offlineFlow
        coEvery { settingsRepository.isServerConfigured } returns serverConfiguredFlow

        val viewModel = TournamentSetupViewModel(
            playerRepository = playerRepository,
            createTournamentUseCase = createTournamentUseCase,
            addPlayerUseCase = addPlayerUseCase,
            settingsRepository = settingsRepository
        )
        advanceUntilIdle()

        // Initially in offline mode
        assertTrue(viewModel.uiState.value.isStandaloneOffline)
        assertFalse(viewModel.uiState.value.isServerConfigured)
        assertTrue(viewModel.uiState.value.isLocalOnly)

        // User connects to server in settings
        serverConfiguredFlow.value = true
        offlineFlow.value = false
        advanceUntilIdle()

        // Automatically updated
        assertFalse(viewModel.uiState.value.isStandaloneOffline)
        assertTrue(viewModel.uiState.value.isServerConfigured)
        assertFalse(viewModel.uiState.value.isLocalOnly)
    }

    @Test
    fun `addNewPlayer in local mode does not trigger background sync`() = runTest {
        coEvery { playerRepository.observePlayers() } returns flowOf(emptyList())
        coEvery { settingsRepository.standaloneOfflineMode } returns flowOf(true)
        val localPlayer = Player(77L, "Baum", "Teddy", isLocal = true)
        coEvery { addPlayerUseCase("Teddy", "Baum", isLocal = true) } returns localPlayer

        val viewModel = TournamentSetupViewModel(
            playerRepository,
            createTournamentUseCase,
            addPlayerUseCase,
            syncPlayersUseCase = syncPlayersUseCase,
            settingsRepository = settingsRepository
        )
        advanceUntilIdle()

        viewModel.onShowAddPlayerDialog(true)
        viewModel.onNewPlayerNameChanged("Teddy", "Baum")
        viewModel.addNewPlayer()
        advanceUntilIdle()

        coVerify(exactly = 0) { syncPlayersUseCase.invoke() }
        assertTrue(viewModel.uiState.value.selectedPlayerIds.contains(77L))
    }

    @Test
    fun `createTournament passes isLocalOnly to CreateTournamentUseCase`() = runTest {
        val p1 = Player(1, "A", "A")
        val mockTournament = mockk<Tournament>()
        io.mockk.every { mockTournament.id } returns 555L

        coEvery { playerRepository.observePlayers() } returns flowOf(listOf(p1))
        coEvery { createTournamentUseCase(any(), any(), any(), any(), isLocalOnly = true) } returns mockTournament

        val viewModel = TournamentSetupViewModel(playerRepository, createTournamentUseCase, addPlayerUseCase)
        viewModel.onPlayerChecked(1L, true)
        viewModel.onModeChanged(GameMode.BLITZ_3_2)
        viewModel.onLocalOnlyChanged(true)

        viewModel.createTournament()

        coVerify { createTournamentUseCase(
            players = listOf(p1),
            mode = GameMode.BLITZ_3_2,
            date = any(),
            rounds = 1,
            isLocalOnly = true
        ) }
    }
}
