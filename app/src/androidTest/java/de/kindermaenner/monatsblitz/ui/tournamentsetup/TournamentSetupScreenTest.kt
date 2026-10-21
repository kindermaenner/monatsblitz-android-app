package de.kindermaenner.monatsblitz.ui.tournamentsetup

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import de.kindermaenner.monatsblitz.domain.model.Player
import de.kindermaenner.monatsblitz.domain.repository.PlayerRepository
import de.kindermaenner.monatsblitz.domain.usecase.AddPlayerUseCase
import de.kindermaenner.monatsblitz.domain.usecase.CreateTournamentUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test

class TournamentSetupScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val playerRepository = mockk<PlayerRepository>(relaxed = true)
    private val createTournamentUseCase = mockk<CreateTournamentUseCase>(relaxed = true)
    private val addPlayerUseCase = mockk<AddPlayerUseCase>(relaxed = true)

    @org.junit.Before
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
        composeTestRule.onNodeWithText("Mindestens 2 Spieler auswählen").assertExists().assertIsNotEnabled()

        // Select first player
        composeTestRule.onNodeWithText("Meier, Alice").assertExists().performClick()
        composeTestRule.onNodeWithText("Mindestens 2 Spieler auswählen").assertExists().assertIsNotEnabled()

        // Select second player -> Start button becomes active
        composeTestRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Mueller, Bob"))
        composeTestRule.onNodeWithText("Mueller, Bob").assertExists().performClick()
        composeTestRule.onNodeWithText("Turnier starten (2 Spieler)").assertExists().assertIsEnabled()
    }

    @Test
    fun setupScreen_settingsCardCanBeCollapsedAndExpanded() {
        val viewModel = TournamentSetupViewModel(playerRepository, createTournamentUseCase, addPlayerUseCase)

        composeTestRule.setContent {
            TournamentSetupScreen(viewModel = viewModel) {}
        }

        // Initially expanded: "Doppelrundig" switch is visible
        composeTestRule.onNodeWithText("Doppelrundig").assertExists()

        // Collapse settings
        composeTestRule.onNodeWithContentDescription("Einstellungen einklappen").performClick()
        composeTestRule.onNodeWithText("Doppelrundig").assertDoesNotExist()

        // Expand settings
        composeTestRule.onNodeWithContentDescription("Einstellungen aufklappen").performClick()
        composeTestRule.onNodeWithText("Doppelrundig").assertExists()
    }
}
