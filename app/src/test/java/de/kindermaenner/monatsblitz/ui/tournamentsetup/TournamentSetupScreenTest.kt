package de.kindermaenner.monatsblitz.ui.tournamentsetup

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import de.kindermaenner.monatsblitz.domain.model.GameMode
import de.kindermaenner.monatsblitz.domain.model.Player
import de.kindermaenner.monatsblitz.domain.repository.PlayerRepository
import de.kindermaenner.monatsblitz.domain.usecase.AddPlayerUseCase
import de.kindermaenner.monatsblitz.domain.usecase.CreateTournamentUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class TournamentSetupScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val playerRepository = mockk<PlayerRepository>(relaxed = true)
    private val createTournamentUseCase = mockk<CreateTournamentUseCase>(relaxed = true)
    private val addPlayerUseCase = mockk<AddPlayerUseCase>(relaxed = true)

    @Before
    fun setup() {
        coEvery { playerRepository.observePlayers() } returns flowOf(emptyList())
    }

    @Test
    fun setupScreen_displaysTitleAndTogglingPlayersEnablesStartButton() {
        val players = listOf(
            Player(1, "Meier", "Alice"),
            Player(2, "Mueller", "Bob"),
        )
        coEvery { playerRepository.observePlayers() } returns flowOf(players)

        val viewModel = TournamentSetupViewModel(playerRepository, createTournamentUseCase, addPlayerUseCase)

        composeTestRule.setContent {
            TournamentSetupScreen(
                viewModel = viewModel
            ) { _ ->
                // onNavigateToCrosstable lambda
            }
        }

        composeTestRule.onNodeWithText("Neues Turnier").assertExists()
        // No mode selected initially
        composeTestRule.onNodeWithText("Bitte Spielmodus auswählen").assertExists().assertIsNotEnabled()

        // Select mode
        viewModel.onModeChanged(GameMode.BLITZ_5_0)
        composeTestRule.onNodeWithText("Mindestens 2 Spieler auswählen").assertExists().assertIsNotEnabled()

        // Select first player
        composeTestRule.onNodeWithText("Alice Meier").assertExists().performClick()
        composeTestRule.onNodeWithText("Mindestens 2 Spieler auswählen").assertExists().assertIsNotEnabled()

        // Select second player -> Start button becomes active
        composeTestRule.onNodeWithText("Bob Mueller").assertExists().performClick()
        composeTestRule.onNodeWithText("Turnier starten (2 Spieler)").assertExists().assertIsEnabled()
    }

    @Test
    fun setupScreen_addPlayerDialog_requiresBothNamesAndShowsCreationError() {
        val errorMessage = "Ein Spieler mit diesem Namen existiert bereits."
        coEvery {
            addPlayerUseCase("Max", "Mustermann", isLocal = false)
        } throws IllegalArgumentException(errorMessage)
        val viewModel = TournamentSetupViewModel(
            playerRepository,
            createTournamentUseCase,
            addPlayerUseCase
        )

        composeTestRule.setContent {
            TournamentSetupScreen(viewModel = viewModel) {}
        }

        composeTestRule.onNodeWithText("Neuer Spieler").performClick()
        composeTestRule.onNodeWithText("Neuen Spieler anlegen").assertExists()
        composeTestRule.onNodeWithText("Anlegen").assertIsNotEnabled()

        composeTestRule.onNodeWithText("Vorname").performTextInput("Max")
        composeTestRule.onNodeWithText("Nachname").performTextInput("Mustermann")
        composeTestRule.onNodeWithText("Anlegen").assertIsEnabled().performClick()

        composeTestRule.onNodeWithText(errorMessage).assertExists()
        composeTestRule.onNodeWithText("Neuen Spieler anlegen").assertExists()
    }

    @Test
    fun setupScreen_settingsCardCanBeCollapsedAndExpanded() {
        val isExpanded = androidx.compose.runtime.mutableStateOf(true)

        composeTestRule.setContent {
            TournamentSettingsCard(
                state = TournamentSettingsState(
                    isExpanded = isExpanded.value,
                    selectedDate = LocalDate.now(),
                    selectedMode = GameMode.BLITZ_3_2,
                    doubleRound = false
                ),
                actions = TournamentSettingsActions(
                    onToggleExpanded = { isExpanded.value = !isExpanded.value },
                    onDateChanged = {},
                    onModeChanged = {},
                    onDoubleRoundChanged = {}
                )
            )
        }

        // Initially expanded: "Doppelrundig" switch is visible
        composeTestRule.onNodeWithText("Doppelrundig").assertExists()
        composeTestRule.onNodeWithText("Turnierdatum").assertExists()
        composeTestRule.onNodeWithText("Turnier einrichten").assertExists()

        // Collapse settings
        composeTestRule.onNodeWithContentDescription("Einstellungen einklappen").performClick()
        composeTestRule.onNodeWithText("Doppelrundig").assertDoesNotExist()
        composeTestRule.onNodeWithText("Turnier einrichten").assertExists()
        composeTestRule.onNodeWithText("Heute • 3+2 • Einfachrunde").assertExists()

        // Expand settings
        composeTestRule.onNodeWithContentDescription("Einstellungen aufklappen").performClick()
        composeTestRule.onNodeWithText("Doppelrundig").assertExists()
    }

    @Test
    fun setupScreen_serverOptionHiddenInStandaloneOfflineMode() {
        composeTestRule.setContent {
            TournamentSettingsCard(
                state = TournamentSettingsState(
                    isExpanded = true,
                    selectedDate = LocalDate.now(),
                    selectedMode = GameMode.BLITZ_5_0,
                    isServerConfigured = false,
                    isStandaloneOffline = true
                ),
                actions = TournamentSettingsActions(
                    onToggleExpanded = {},
                    onDateChanged = {},
                    onModeChanged = {},
                    onDoubleRoundChanged = {}
                )
            )
        }

        composeTestRule.onNodeWithText("Nur lokal austragen").assertDoesNotExist()
        composeTestRule.onNodeWithTag("tournament_local_only_switch").assertDoesNotExist()
    }

    @Test
    fun setupScreen_serverOptionVisibleWhenServerConfigured() {
        composeTestRule.setContent {
            TournamentSettingsCard(
                state = TournamentSettingsState(
                    isExpanded = true,
                    selectedDate = LocalDate.now(),
                    selectedMode = GameMode.BLITZ_5_0,
                    isServerConfigured = true,
                    isStandaloneOffline = false
                ),
                actions = TournamentSettingsActions(
                    onToggleExpanded = {},
                    onDateChanged = {},
                    onModeChanged = {},
                    onDoubleRoundChanged = {}
                )
            )
        }

        composeTestRule.onNodeWithText("Nur lokal austragen").assertExists()
        composeTestRule.onNodeWithTag("tournament_local_only_switch").assertExists()
    }

    @Test
    fun setupScreen_scrollingPlayerListCollapsesSettings() {
        val players = (1..20).map { i ->
            Player(i.toLong(), "Nachname$i", "Vorname$i")
        }
        coEvery { playerRepository.observePlayers() } returns flowOf(players)

        val viewModel = TournamentSetupViewModel(playerRepository, createTournamentUseCase, addPlayerUseCase)

        composeTestRule.setContent {
            TournamentSetupScreen(viewModel = viewModel) {}
        }

        // Initially expanded
        composeTestRule.onNodeWithText("Doppelrundig").assertExists()

        // Scroll the player list
        composeTestRule.onNodeWithTag("tournament_player_list").performTouchInput {
            swipeUp()
        }

        // Settings should now be collapsed
        composeTestRule.onNodeWithText("Doppelrundig").assertDoesNotExist()
        composeTestRule.onNodeWithText("Turnier einrichten").assertExists()
    }

    @Test
    fun matchesPlayerSearch_matchesNamesAndHandlesUmlauts() {
        val player = Player(1, "Müller", "Jürgen", rufname = "Jürgi")

        // Exact match
        assertTrue(matchesPlayerSearch(player, "Müller"))
        assertTrue(matchesPlayerSearch(player, "Jürgen"))
        assertTrue(matchesPlayerSearch(player, "Jürgi"))

        // Lowercase
        assertTrue(matchesPlayerSearch(player, "müller"))
        assertTrue(matchesPlayerSearch(player, "jürgen"))

        // Expanded umlaut (ue)
        assertTrue(matchesPlayerSearch(player, "mueller"))
        assertTrue(matchesPlayerSearch(player, "juergen"))

        // Simplified umlaut (u)
        assertTrue(matchesPlayerSearch(player, "muller"))

        // Blank query matches all
        assertTrue(matchesPlayerSearch(player, "   "))

        // Non match
        assertFalse(matchesPlayerSearch(player, "Schmidt"))
    }

    @Test
    fun setupScreen_remoteOpenTournamentDialogShownAndCanBeDismissed() {
        val openInfo = de.kindermaenner.monatsblitz.domain.repository.RemoteOpenTournamentInfo(
            id = 42,
            date = "2026-11-07",
            mode = "Blitz 5+0",
            roundCount = 1,
            playerCount = 8,
            dailyNumber = 1
        )
        val viewModel = TournamentSetupViewModel(playerRepository, createTournamentUseCase, addPlayerUseCase)
        val field = TournamentSetupViewModel::class.java.getDeclaredField("_uiState")
        field.isAccessible = true
        val stateFlow = field.get(viewModel) as kotlinx.coroutines.flow.MutableStateFlow<TournamentSetupUiState>
        stateFlow.value = stateFlow.value.copy(remoteOpenTournament = openInfo)

        composeTestRule.setContent {
            TournamentSetupScreen(viewModel = viewModel) {}
        }

        composeTestRule.onNodeWithText("Offenes Turnier gefunden").assertExists()
        composeTestRule.onNodeWithText("Fortsetzen").assertExists()
        composeTestRule.onNodeWithText("Neues Turnier").assertExists().performClick()

        // After clicking "Neues Turnier", dialog is dismissed and setup screen appears
        composeTestRule.onNodeWithText("Offenes Turnier gefunden").assertDoesNotExist()
        composeTestRule.onNodeWithText("Neues Turnier").assertExists()
    }

    @Test
    fun setupScreen_adoptErrorDialogShownAndDismissed() {
        val viewModel = TournamentSetupViewModel(playerRepository, createTournamentUseCase, addPlayerUseCase)
        val field = TournamentSetupViewModel::class.java.getDeclaredField("_uiState")
        field.isAccessible = true
        val stateFlow = field.get(viewModel) as kotlinx.coroutines.flow.MutableStateFlow<TournamentSetupUiState>
        stateFlow.value = stateFlow.value.copy(adoptError = "Verbindungsabbruch zum Server")

        composeTestRule.setContent {
            TournamentSetupScreen(viewModel = viewModel) {}
        }

        composeTestRule.onNodeWithText("Fehler").assertExists()
        composeTestRule.onNodeWithText("Verbindungsabbruch zum Server").assertExists()
        composeTestRule.onNodeWithText("OK").assertExists().performClick()

        composeTestRule.onNodeWithText("Fehler").assertDoesNotExist()
    }

    @Test
    fun settingsCard_datePickerCanBeOpenedAndDismissed() {
        val selectedDate = androidx.compose.runtime.mutableStateOf(LocalDate.now())

        composeTestRule.setContent {
            TournamentSettingsCard(
                state = TournamentSettingsState(
                    isExpanded = true,
                    selectedDate = selectedDate.value,
                    selectedMode = GameMode.BLITZ_3_2,
                    doubleRound = false
                ),
                actions = TournamentSettingsActions(
                    onToggleExpanded = {},
                    onDateChanged = { selectedDate.value = it },
                    onModeChanged = {},
                    onDoubleRoundChanged = {}
                )
            )
        }

        composeTestRule.onNodeWithText("Turnierdatum").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("OK").assertExists()
        composeTestRule.onNodeWithText("Abbrechen").assertExists().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("OK").assertDoesNotExist()
    }

    @Test
    fun setupScreen_searchAndFilterPlayers() {
        val p1 = Player(1, "Meier", "Alice")
        val p2 = Player(2, "Mueller", "Bob")
        coEvery { playerRepository.observePlayers() } returns flowOf(listOf(p1, p2))

        val viewModel = TournamentSetupViewModel(playerRepository, createTournamentUseCase, addPlayerUseCase)
        viewModel.onPlayerChecked(1L, true) // Select Alice

        composeTestRule.setContent {
            TournamentSetupScreen(viewModel = viewModel) {}
        }

        // Search for "Alice"
        composeTestRule.onNodeWithText("Spieler suchen...").performTextInput("Alice")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Alice Meier").assertExists()
        composeTestRule.onNodeWithText("Bob Mueller").assertDoesNotExist()

        // Clear search
        composeTestRule.onNodeWithContentDescription("Suche leeren").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Bob Mueller").assertExists()

        // Filter chip "Ausgewählt (1)"
        composeTestRule.onNodeWithText("Ausgewählt (1)").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Alice Meier").assertExists()
        composeTestRule.onNodeWithText("Bob Mueller").assertDoesNotExist()

        // Filter chip "Aktive (2)"
        composeTestRule.onNodeWithText("Aktive (2)").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Bob Mueller").assertExists()
    }
}
