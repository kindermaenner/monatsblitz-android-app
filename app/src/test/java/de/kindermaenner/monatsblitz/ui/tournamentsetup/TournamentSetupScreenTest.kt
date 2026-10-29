package de.kindermaenner.monatsblitz.ui.tournamentsetup

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import de.kindermaenner.monatsblitz.domain.model.GameMode
import de.kindermaenner.monatsblitz.domain.model.Player
import de.kindermaenner.monatsblitz.domain.repository.PlayerRepository
import de.kindermaenner.monatsblitz.domain.usecase.AddPlayerUseCase
import de.kindermaenner.monatsblitz.domain.usecase.CreateTournamentUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
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
}
