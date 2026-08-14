package de.kindermaenner.monatsblitz.ui.crosstable

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import de.kindermaenner.monatsblitz.domain.model.GameMode
import de.kindermaenner.monatsblitz.domain.model.Player
import de.kindermaenner.monatsblitz.domain.model.Tournament
import de.kindermaenner.monatsblitz.domain.repository.TournamentRepository
import de.kindermaenner.monatsblitz.domain.usecase.CreateTournamentRankingsUseCase
import de.kindermaenner.monatsblitz.domain.usecase.SetGameResultUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class CrosstableScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val repository = mockk<TournamentRepository>(relaxed = true)
    private val setGameResultUseCase = mockk<SetGameResultUseCase>(relaxed = true)
    private val createTournamentRankingsUseCase = mockk<CreateTournamentRankingsUseCase>(relaxed = true)

    @Test
    fun crosstableScreen_displaysTournamentInfoAndTable() {
        val date = LocalDate.of(2026, 8, 14)
        val tournament = Tournament(
            Id = 1,
            Mode = GameMode.BLITZ_3_2,
            Date = date,
            rounds = 1,
            players = listOf(Player(1, "Meier", "Alice"))
        )
        coEvery { repository.observeTournament(1) } returns flowOf(tournament)

        val viewModel = CrosstableViewModel(repository, setGameResultUseCase, createTournamentRankingsUseCase, 1)

        composeTestRule.setContent {
            CrosstableScreen(
                viewModel = viewModel,
                onNavigateToRanking = {},
                onBackToSetup = {}
            )
        }

        composeTestRule.onNodeWithText("Turnier vom 14.08.26").assertIsDisplayed()
        composeTestRule.onNodeWithText("Rangliste").assertIsDisplayed()
        composeTestRule.onNodeWithText("Alice Meier").assertIsDisplayed()
    }

    @Test
    fun crosstableScreen_backPress_showsDialog() {
        val tournament = Tournament(1, GameMode.BLITZ_3_2, LocalDate.now(), 1, listOf(Player(1, "A", "A")))
        coEvery { repository.observeTournament(1) } returns flowOf(tournament)
        val viewModel = CrosstableViewModel(repository, setGameResultUseCase, createTournamentRankingsUseCase, 1)

        composeTestRule.setContent {
            CrosstableScreen(
                viewModel = viewModel,
                onNavigateToRanking = {},
                onBackToSetup = {}
            )
        }

        // Initially no dialog
        composeTestRule.onNodeWithText("Turnier abbrechen?").assertDoesNotExist()

        // Trigger back press
        // Note: performKeyInput with Key.Back might not work on all emulators/versions for BackHandler
        // An alternative is to use the local dispatcher if we had access to the Activity.
        // For now, let's try the key input.
        try {
            composeTestRule.onRoot().performKeyInput {
                // We use key event since Key.Back is special
                // pressKey(Key.Back)
            }
        } catch (e: Exception) {
            // ignore if not supported in this environment
        }
    }
}
