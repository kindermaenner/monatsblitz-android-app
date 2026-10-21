package de.kindermaenner.monatsblitz.ui.ranking

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import de.kindermaenner.monatsblitz.domain.model.TournamentRankingItem
import de.kindermaenner.monatsblitz.domain.repository.TournamentRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RankingScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val tournamentRepository = mockk<TournamentRepository>(relaxed = true)

    @Before
    fun setup() {
        every { tournamentRepository.observeTournament(any()) } returns flowOf(null)
    }

    @Test
    fun rankingScreen_displaysRowsAndHandlesBack() {
        val tournamentId = 1L
        val rankingItems = listOf(
            TournamentRankingItem(
                playerId = 1L,
                rank = 1,
                name = "Max Mustermann",
                points = 3.0
            )
        )

        every { tournamentRepository.observeTournamentRanking(tournamentId) } returns flowOf(rankingItems)

        val viewModel = RankingViewModel(tournamentId, tournamentRepository)

        var backClicked = false

        composeTestRule.setContent {
            RankingScreen(
                viewModel = viewModel,
                onBack = { backClicked = true }
            )
        }

        composeTestRule.onNodeWithText("Ranking").assertExists()
        composeTestRule.onNodeWithText("Max Mustermann").assertExists()

        composeTestRule.onNodeWithContentDescription("Zurück").performClick()
        assertTrue("Back button should trigger onBack", backClicked)
    }

    @Test
    fun rankingScreen_finalizeButtonClick_opensConfirmDialog() {
        val tournamentId = 1L
        val rankingItems = listOf(
            TournamentRankingItem(
                playerId = 1L,
                rank = 1,
                name = "Max Mustermann",
                points = 3.0
            )
        )

        every { tournamentRepository.observeTournamentRanking(tournamentId) } returns flowOf(rankingItems)

        val viewModel = RankingViewModel(tournamentId, tournamentRepository)

        composeTestRule.setContent {
            RankingScreen(
                viewModel = viewModel,
                onBack = {}
            )
        }

        composeTestRule.onNodeWithText("Turnier abschließen").performClick()
        composeTestRule.onNodeWithText("Turnier abschließen?").assertExists()
        composeTestRule.onNodeWithText("Abbrechen").assertExists()
    }
}
