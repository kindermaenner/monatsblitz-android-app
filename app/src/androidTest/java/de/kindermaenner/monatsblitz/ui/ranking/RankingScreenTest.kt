package de.kindermaenner.monatsblitz.ui.ranking

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.PlayerDao
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.dao.TournamentPlayerDao
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.PlayerEntity
import de.kindermaenner.monatsblitz.infrastructure.persistence.room.entity.TournamentPlayerCrossRef
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RankingScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val tournamentPlayerDao = mockk<TournamentPlayerDao>(relaxed = true)
    private val playerDao = mockk<PlayerDao>(relaxed = true)

    @Test
    fun rankingScreen_displaysRowsAndHandlesBack() {
        val tournamentId = 1L
        val rankingRefs = listOf(
            TournamentPlayerCrossRef(tournamentId, 1, 3.0, 1)
        )
        val players = listOf(
            PlayerEntity(id = 1, name = "Mustermann", vorname = "Max", dirty = false)
        )

        coEvery { tournamentPlayerDao.observeRankingForTournament(tournamentId) } returns flowOf(rankingRefs)
        coEvery { playerDao.getAllPlayers() } returns players

        val viewModel = RankingViewModel(tournamentId, tournamentPlayerDao, playerDao)

        var backClicked = false

        composeTestRule.setContent {
            RankingScreen(
                viewModel = viewModel,
                onBack = { backClicked = true }
            )
        }

        composeTestRule.onNodeWithText("Ranking").assertIsDisplayed()
        composeTestRule.onNodeWithText("Max Mustermann").assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription("Zurück").performClick()
        assertTrue("Back button should trigger onBack", backClicked)
    }

    @Test
    fun rankingScreen_finalizeButtonClick_opensConfirmDialog() {
        val tournamentId = 1L
        val rankingRefs = listOf(
            TournamentPlayerCrossRef(tournamentId, 1, 3.0, 1)
        )
        val players = listOf(
            PlayerEntity(id = 1, name = "Mustermann", vorname = "Max", dirty = false)
        )

        coEvery { tournamentPlayerDao.observeRankingForTournament(tournamentId) } returns flowOf(rankingRefs)
        coEvery { playerDao.getAllPlayers() } returns players

        val viewModel = RankingViewModel(tournamentId, tournamentPlayerDao, playerDao)

        composeTestRule.setContent {
            RankingScreen(
                viewModel = viewModel,
                onBack = {}
            )
        }

        composeTestRule.onNodeWithText("Turnier abschließen").performClick()
        composeTestRule.onNodeWithText("Turnier abschließen?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Abbrechen").assertIsDisplayed()
    }
}
