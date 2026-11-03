package de.kindermaenner.monatsblitz.ui.crosstable

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import de.kindermaenner.monatsblitz.domain.model.Game
import de.kindermaenner.monatsblitz.domain.model.GameMode
import de.kindermaenner.monatsblitz.domain.model.GameResult
import de.kindermaenner.monatsblitz.domain.model.Player
import de.kindermaenner.monatsblitz.domain.model.PlayerWithdrawRule
import de.kindermaenner.monatsblitz.domain.model.Tournament
import de.kindermaenner.monatsblitz.domain.repository.TournamentRepository
import de.kindermaenner.monatsblitz.domain.usecase.CreateTournamentRankingsUseCase
import de.kindermaenner.monatsblitz.domain.usecase.SetGameResultUseCase
import io.mockk.coVerify
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

    @Test
    fun crosstableScreen_completedGames_canNavigateToRankings() {
        val players = listOf(
            Player(1, "Meier", "Alice"),
            Player(2, "Mueller", "Bob")
        )
        val tournament = Tournament(
            id = 42,
            mode = GameMode.BLITZ_3_2,
            date = LocalDate.now(),
            rounds = 1,
            players = players,
            games = mapOf(1L to Game(1L, 1L, 2L, 1, GameResult.Win))
        )
        coEvery { repository.observeTournament(42) } returns flowOf(tournament)
        val viewModel = CrosstableViewModel(
            repository,
            setGameResultUseCase,
            createTournamentRankingsUseCase,
            42
        )
        var navigatedTournamentId: Long? = null

        composeTestRule.setContent {
            CrosstableScreen(
                viewModel = viewModel,
                onNavigateToRanking = { navigatedTournamentId = it }
            ) {}
        }

        composeTestRule.onNodeWithTag("crosstable_finish_tournament_button")
            .assertExists()
            .performClick()
        composeTestRule.waitForIdle()

        coVerify(exactly = 1) { createTournamentRankingsUseCase(tournament) }
        org.junit.Assert.assertEquals(42L, navigatedTournamentId)
    }

    @Test
    fun crosstableScreen_withdrawPlayer_canChooseRegularLossRule() {
        val players = listOf(
            Player(1, "Meier", "Alice"),
            Player(2, "Mueller", "Bob")
        )
        val tournament = Tournament(
            id = 42,
            mode = GameMode.BLITZ_3_2,
            date = LocalDate.now(),
            rounds = 1,
            players = players,
            games = mapOf(1L to Game(1L, 1L, 2L, 1, GameResult.Open))
        )
        coEvery { repository.observeTournament(42) } returns flowOf(tournament)
        val viewModel = CrosstableViewModel(
            repository,
            setGameResultUseCase,
            createTournamentRankingsUseCase,
            42
        )

        composeTestRule.setContent {
            CrosstableScreen(
                viewModel = viewModel,
                onNavigateToRanking = {}
            ) {}
        }

        composeTestRule.onNodeWithText("Alice").performClick()
        composeTestRule.onNodeWithText("Turnier vorzeitig beenden").assertExists()
        composeTestRule.onNodeWithText("Regulär").performClick()
        composeTestRule.onNodeWithText("Turnier beenden").performClick()
        composeTestRule.waitForIdle()

        coVerify(exactly = 1) {
            repository.withdrawPlayer(42L, 1L, PlayerWithdrawRule.REGULAR_LOSS)
        }
    }

    @Test
    fun crosstableScreen_addPlayerButton_opensDialogAndCanAddExistingPlayer() {
        val p1 = Player(1, "Meier", "Alice")
        val p2 = Player(2, "Mueller", "Bob")
        val tournament = Tournament(
            id = 42,
            mode = GameMode.BLITZ_3_2,
            date = LocalDate.now(),
            rounds = 1,
            players = listOf(p1)
        )
        val playerRepo = mockk<de.kindermaenner.monatsblitz.domain.repository.PlayerRepository>()
        coEvery { repository.observeTournament(42) } returns flowOf(tournament)
        coEvery { playerRepo.observePlayers() } returns flowOf(listOf(p1, p2))
        coEvery { repository.addPlayerToTournament(42L, 2L) } returns Unit

        val viewModel = CrosstableViewModel(
            repository = repository,
            setGameResultUseCase = setGameResultUseCase,
            createTournamentRankingsUseCase = createTournamentRankingsUseCase,
            calculatePlayerPointsUseCase = de.kindermaenner.monatsblitz.domain.usecase.CalculatePlayerPointsUseCase(),
            tournamentId = 42L,
            playerRepository = playerRepo
        )

        composeTestRule.setContent {
            CrosstableScreen(
                viewModel = viewModel,
                onNavigateToRanking = {}
            ) {}
        }

        composeTestRule.onNodeWithTag("crosstable_add_player_button").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("Weitere Optionen").performClick()
        composeTestRule.onNodeWithTag("crosstable_add_player_menu_item").assertExists().performClick()
        composeTestRule.onNodeWithText("Spieler zum Turnier hinzufügen").assertExists()
        composeTestRule.onNodeWithTag("add_player_item_2").assertExists().performClick()
        composeTestRule.waitForIdle()

        coVerify(exactly = 1) {
            repository.addPlayerToTournament(42L, 2L)
        }
    }

    @Test
    fun crosstableScreen_addPlayerDialog_canCreateAndAddNewPlayer() {
        val p1 = Player(1, "Meier", "Alice")
        val tournament = Tournament(
            id = 42,
            mode = GameMode.BLITZ_3_2,
            date = LocalDate.now(),
            rounds = 1,
            players = listOf(p1)
        )
        val playerRepo = mockk<de.kindermaenner.monatsblitz.domain.repository.PlayerRepository>()
        val addPlayerUseCase = mockk<de.kindermaenner.monatsblitz.domain.usecase.AddPlayerUseCase>()
        val createdPlayer = Player(99L, "Neu", "Charlie")

        coEvery { repository.observeTournament(42) } returns flowOf(tournament)
        coEvery { playerRepo.observePlayers() } returns flowOf(listOf(p1))
        coEvery { addPlayerUseCase.invoke("Charlie", "Neu", isLocal = false) } returns createdPlayer
        coEvery { repository.addPlayerToTournament(42L, 99L) } returns Unit

        val viewModel = CrosstableViewModel(
            repository = repository,
            setGameResultUseCase = setGameResultUseCase,
            createTournamentRankingsUseCase = createTournamentRankingsUseCase,
            calculatePlayerPointsUseCase = de.kindermaenner.monatsblitz.domain.usecase.CalculatePlayerPointsUseCase(),
            tournamentId = 42L,
            playerRepository = playerRepo,
            addPlayerUseCase = addPlayerUseCase
        )

        composeTestRule.setContent {
            CrosstableScreen(
                viewModel = viewModel,
                onNavigateToRanking = {}
            ) {}
        }

        composeTestRule.onNodeWithContentDescription("Weitere Optionen").performClick()
        composeTestRule.onNodeWithTag("crosstable_add_player_menu_item").performClick()
        composeTestRule.onNodeWithTag("tab_new_player").performClick()

        composeTestRule.onNodeWithTag("new_player_vorname_input")
            .performTextInput("Charlie")
        composeTestRule.onNodeWithTag("new_player_name_input")
            .performTextInput("Neu")

        composeTestRule.onNodeWithTag("submit_new_player_button").performClick()
        composeTestRule.waitForIdle()

        coVerify(exactly = 1) {
            addPlayerUseCase.invoke("Charlie", "Neu", isLocal = false)
            repository.addPlayerToTournament(42L, 99L)
        }
    }
}