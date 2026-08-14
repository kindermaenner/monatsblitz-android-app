package de.kindermaenner.monatsblitz.ui.navigation

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import de.kindermaenner.monatsblitz.app.AppContainer
import de.kindermaenner.monatsblitz.domain.model.GameMode
import de.kindermaenner.monatsblitz.domain.model.Player
import de.kindermaenner.monatsblitz.domain.model.Tournament
import de.kindermaenner.monatsblitz.domain.repository.PlayerRepository
import de.kindermaenner.monatsblitz.domain.repository.TournamentRepository
import de.kindermaenner.monatsblitz.ui.crosstable.CrosstableViewModelFactory
import de.kindermaenner.monatsblitz.ui.ranking.RankingViewModelFactory
import de.kindermaenner.monatsblitz.ui.tournamentsetup.TournamentSetupViewModelFactory
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class AppNavHostTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var navController: TestNavHostController
    private val appContainer = mockk<AppContainer>(relaxed = true)
    private val tournamentRepository = mockk<TournamentRepository>(relaxed = true)
    private val playerRepository = mockk<PlayerRepository>(relaxed = true)

    @Before
    fun setup() {
        val tournament = Tournament(1, GameMode.BLITZ_3_2, LocalDate.now(), 1, listOf(Player(1, "A", "A")))
        coEvery { tournamentRepository.observeTournament(any()) } returns flowOf(tournament)
        coEvery { playerRepository.observePlayers() } returns flowOf(emptyList())

        // Setup factories in mock container
        val setupFactory = TournamentSetupViewModelFactory(playerRepository, mockk(relaxed = true))
        every { appContainer.tournamentSetupViewModelFactory } returns setupFactory

        every { appContainer.crosstableViewModelFactory(any()) } answers {
            CrosstableViewModelFactory(tournamentRepository, mockk(relaxed = true), mockk(relaxed = true), it.invocation.args[0] as Long)
        }

        every { appContainer.rankingViewModelFactory(any()) } answers {
            RankingViewModelFactory(it.invocation.args[0] as Long, mockk(relaxed = true), mockk(relaxed = true))
        }
    }

    @Test
    fun appNavHost_startDestinationIsSetup_showsSetup() {
        composeTestRule.setContent {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())
            AppNavHost(
                navController = navController,
                appContainer = appContainer,
                startDestination = AppRoute.TournamentSetup,
            )
        }

        composeTestRule.onNodeWithText("Neues Turnier").assertExists()
    }

    @Test
    fun appNavHost_clickStartTournament_navigatesToCrosstable() {
        composeTestRule.setContent {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())
            AppNavHost(
                navController = navController,
                appContainer = appContainer,
                startDestination = AppRoute.TournamentSetup,
            )
        }

        composeTestRule.onNodeWithText("Turnier starten").performClick()

        // Verify we are on Crosstable screen
        composeTestRule.onNodeWithText("Turnier vom", substring = true).assertExists()
    }

    @Test
    fun appNavHost_clickRanking_navigatesToRanking() {
        composeTestRule.setContent {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())
            AppNavHost(
                navController = navController,
                appContainer = appContainer,
                startDestination = AppRoute.Crosstable(1L),
            )
        }

        composeTestRule.onNodeWithText("Rangliste").performClick()

        // Verify we are on Ranking screen
        composeTestRule.onNodeWithText("Ranking").assertExists()
    }
}
