package de.kindermaenner.monatsblitz.ui.tournamentsetup

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
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

    @Test
    fun setupScreen_displaysPlayersAndButton() {
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

        composeTestRule.onNodeWithText("Neues Turnier").assertIsDisplayed()
        composeTestRule.onNodeWithText("Alice Meier").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Bob Mueller").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Mindestens 2 Spieler auswählen").assertIsDisplayed().assertIsNotEnabled()
    }
}
