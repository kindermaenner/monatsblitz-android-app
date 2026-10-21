package de.kindermaenner.monatsblitz.ui.navigation

import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import com.google.mlkit.common.sdkinternal.MlKitContext
import de.kindermaenner.monatsblitz.app.AppContainer
import de.kindermaenner.monatsblitz.domain.model.GameMode
import de.kindermaenner.monatsblitz.domain.model.Player
import de.kindermaenner.monatsblitz.domain.model.Tournament
import de.kindermaenner.monatsblitz.domain.repository.PlayerRepository
import de.kindermaenner.monatsblitz.domain.repository.SettingsRepository
import de.kindermaenner.monatsblitz.domain.repository.TournamentRepository
import de.kindermaenner.monatsblitz.ui.crosstable.CrosstableViewModelFactory
import de.kindermaenner.monatsblitz.ui.ranking.RankingViewModelFactory
import de.kindermaenner.monatsblitz.ui.settings.SettingsViewModelFactory
import de.kindermaenner.monatsblitz.ui.tournamentsetup.TournamentSetupViewModelFactory
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class AppNavHostTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var navController: TestNavHostController
    private val appContainer = mockk<AppContainer>(relaxed = true)
    private val tournamentRepository = mockk<TournamentRepository>(relaxed = true)
    private val playerRepository = mockk<PlayerRepository>(relaxed = true)
    private val settingsRepository = mockk<SettingsRepository>(relaxed = true)

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        try {
            MlKitContext.initializeIfNeeded(context)
        } catch (_: Exception) {}

        val tournament = Tournament(1, GameMode.BLITZ_3_2, LocalDate.now(), 1, listOf(Player(1, "A", "A")), emptyMap())
        coEvery { tournamentRepository.observeTournament(any()) } returns flowOf(tournament)
        coEvery { playerRepository.observePlayers() } returns flowOf(emptyList())

        every { settingsRepository.showDetailedSyncIndicators } returns flowOf(true)
        every { settingsRepository.serverUrl } returns flowOf("https://sg-koenigslutter.de")
        every { settingsRepository.apiKey } returns flowOf("test-key")
        every { settingsRepository.standaloneOfflineMode } returns flowOf(false)

        // Setup factories in mock container
        val setupFactory = TournamentSetupViewModelFactory(playerRepository, mockk(relaxed = true), mockk(relaxed = true))
        every { appContainer.tournamentSetupViewModelFactory } returns setupFactory

        every { appContainer.crosstableViewModelFactory(any()) } answers {
            CrosstableViewModelFactory(
                tournamentRepository = tournamentRepository,
                setGameResultUseCase = mockk(relaxed = true),
                createTournamentRankingsUseCase = mockk(relaxed = true),
                tournamentId = it.invocation.args[0] as Long
            )
        }

        every { appContainer.rankingViewModelFactory(any()) } answers {
            RankingViewModelFactory(it.invocation.args[0] as Long, mockk(relaxed = true), mockk(relaxed = true))
        }

        val settingsFactory = SettingsViewModelFactory(settingsRepository, mockk(relaxed = true))
        every { appContainer.settingsViewModelFactory } returns settingsFactory
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

    @Test
    fun appNavHost_startDestinationIsSettings_navigatesToSetupOnBack() {
        composeTestRule.setContent {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())
            AppNavHost(
                navController = navController,
                appContainer = appContainer,
                startDestination = AppRoute.Settings,
            )
        }

        composeTestRule.onNodeWithText("Einstellungen").assertExists()

        // When Settings was the root/start destination, onBack pops Settings and navigates to TournamentSetup
        composeTestRule.onNodeWithContentDescription("Zurück").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Neues Turnier").assertExists()
    }
}
