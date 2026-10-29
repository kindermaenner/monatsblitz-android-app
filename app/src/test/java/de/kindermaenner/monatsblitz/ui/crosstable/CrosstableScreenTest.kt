package de.kindermaenner.monatsblitz.ui.crosstable

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
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
            id = 1,
            mode = GameMode.BLITZ_3_2,
            date = date,
            rounds = 1,
            players = listOf(Player(1, "Meier", "Alice")),
        )
        coEvery { repository.observeTournament(1) } returns flowOf(tournament)

        val viewModel = CrosstableViewModel(repository, setGameResultUseCase, createTournamentRankingsUseCase, 1)

        composeTestRule.setContent {
            CrosstableScreen(
                viewModel = viewModel,
                onNavigateToRanking = {}
            ) {
                // onBackToSetup lambda
            }
        }
        composeTestRule.onNodeWithText("14.08.26 • 3+2").assertExists()
        composeTestRule.onNodeWithContentDescription("Rangliste").assertExists()
        composeTestRule.onNodeWithText("Alice").assertExists()
    }

    @Test
    fun crosstableScreen_backClick_showsCancelDialog() {
        val tournament = Tournament(1, GameMode.BLITZ_3_2, LocalDate.now(), 1, listOf(Player(1, "A", "A")))
        coEvery { repository.observeTournament(1) } returns flowOf(tournament)
        val viewModel = CrosstableViewModel(repository, setGameResultUseCase, createTournamentRankingsUseCase, 1)

        composeTestRule.setContent {
            CrosstableScreen(
                viewModel = viewModel,
                onNavigateToRanking = {}
            ) {
                // onBackToSetup lambda
            }
        }

        // Initially no dialog
        composeTestRule.onNodeWithText("Turnier abbrechen?").assertDoesNotExist()

        // Click back button in TopAppBar
        composeTestRule.onNodeWithContentDescription("Turnier abbrechen").performClick()

        // Dialog should now be visible
        composeTestRule.onNodeWithText("Turnier abbrechen?").assertExists()
    }
}
