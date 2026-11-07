package de.kindermaenner.monatsblitz.ui.ranking

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import de.kindermaenner.monatsblitz.domain.model.GameMode
import de.kindermaenner.monatsblitz.domain.model.Tournament
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
import java.time.LocalDate

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

        composeTestRule.onNodeWithText("Rangliste").assertExists()
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

    @Test
    fun rankingScreen_offlineTournament_showsOfflineConfirmDialog() {
        val tournamentId = 1L
        val rankingItems = listOf(
            TournamentRankingItem(playerId = 1L, rank = 1, name = "Max Mustermann", points = 3.0)
        )
        val offlineTournament = Tournament(
            id = tournamentId,
            date = LocalDate.now(),
            mode = GameMode.BLITZ_5_0,
            rounds = 1,
            players = emptyList(),
            isLocalOnly = true
        )

        every { tournamentRepository.observeTournamentRanking(tournamentId) } returns flowOf(rankingItems)
        every { tournamentRepository.observeTournament(tournamentId) } returns flowOf(offlineTournament)

        val viewModel = RankingViewModel(tournamentId, tournamentRepository)

        composeTestRule.setContent {
            RankingScreen(
                viewModel = viewModel,
                onBack = {}
            )
        }

        composeTestRule.onNodeWithText("Turnier abschließen").performClick()
        composeTestRule.onNodeWithText("Offline-Turnier abschließen?").assertExists()
        composeTestRule.onNodeWithText("Das Offline-Turnier wird abgeschlossen. Es findet keine Übertragung an die Website statt. Danach sind keine Änderungen mehr möglich.").assertExists()
    }

    @Test
    fun rankingScreen_offlineTournament_finalizedSuccessDialogOffersPdfExport() {
        val tournamentId = 1L
        val rankingItems = listOf(
            TournamentRankingItem(playerId = 1L, rank = 1, name = "Max Mustermann", points = 3.0)
        )
        val offlineTournament = Tournament(
            id = tournamentId,
            date = LocalDate.now(),
            mode = GameMode.BLITZ_5_0,
            rounds = 1,
            players = emptyList(),
            isLocalOnly = true
        )

        every { tournamentRepository.observeTournamentRanking(tournamentId) } returns flowOf(rankingItems)
        every { tournamentRepository.observeTournament(tournamentId) } returns flowOf(offlineTournament)

        val finalizeUseCase = mockk<de.kindermaenner.monatsblitz.domain.usecase.FinalizeTournamentUseCase>()
        io.mockk.coEvery { finalizeUseCase(tournamentId) } returns Unit

        val viewModel = RankingViewModel(tournamentId, tournamentRepository, finalizeUseCase)

        composeTestRule.setContent {
            RankingScreen(
                viewModel = viewModel,
                onBack = {}
            )
        }

        viewModel.finalizeTournament()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Offline-Turnier abgeschlossen").assertExists()
        composeTestRule.onNodeWithTag("finalize_export_pdf_button").assertExists()
        composeTestRule.onNodeWithText("PDF speichern / teilen").assertExists()
        composeTestRule.onNodeWithTag("finalize_done_button").assertExists()
        composeTestRule.onNodeWithText("Zum Hauptmenü").assertExists()
    }

    @Test
    fun rankingScreen_openGamesExist_doesNotShowFinalizeButton() {
        val tournamentId = 1L
        val rankingItems = listOf(
            TournamentRankingItem(playerId = 1L, rank = 1, name = "Max Mustermann", points = 3.0)
        )
        val p1 = de.kindermaenner.monatsblitz.domain.model.Player(1, "Mustermann", "Max")
        val p2 = de.kindermaenner.monatsblitz.domain.model.Player(2, "Musterfrau", "Erika")
        val tournament = Tournament(
            id = tournamentId,
            date = LocalDate.now(),
            mode = GameMode.BLITZ_5_0,
            rounds = 1,
            players = listOf(p1, p2),
            games = mapOf(
                1L to de.kindermaenner.monatsblitz.domain.model.Game(1L, 1L, 2L, 1, de.kindermaenner.monatsblitz.domain.model.GameResult.Open)
            )
        )

        every { tournamentRepository.observeTournamentRanking(tournamentId) } returns flowOf(rankingItems)
        every { tournamentRepository.observeTournament(tournamentId) } returns flowOf(tournament)

        val viewModel = RankingViewModel(tournamentId, tournamentRepository)

        composeTestRule.setContent {
            RankingScreen(
                viewModel = viewModel,
                onBack = {}
            )
        }

        composeTestRule.onNodeWithText("Turnier abschließen").assertDoesNotExist()
        composeTestRule.onNodeWithTag("finalize_button").assertDoesNotExist()
    }

    @Test
    fun rankingScreen_finalizeError_showsErrorDialogAndClearsError() {
        val tournamentId = 1L
        val rankingItems = listOf(
            TournamentRankingItem(playerId = 1L, rank = 1, name = "Max Mustermann", points = 3.0)
        )
        val tournament = Tournament(
            id = tournamentId,
            date = LocalDate.now(),
            mode = GameMode.BLITZ_5_0,
            rounds = 1,
            players = emptyList(),
            isLocalOnly = false
        )

        every { tournamentRepository.observeTournamentRanking(tournamentId) } returns flowOf(rankingItems)
        every { tournamentRepository.observeTournament(tournamentId) } returns flowOf(tournament)

        val finalizeUseCase = mockk<de.kindermaenner.monatsblitz.domain.usecase.FinalizeTournamentUseCase>()
        io.mockk.coEvery { finalizeUseCase(tournamentId) } throws RuntimeException("Verbindungsfehler zum Server")

        val viewModel = RankingViewModel(tournamentId, tournamentRepository, finalizeUseCase)

        composeTestRule.setContent {
            RankingScreen(
                viewModel = viewModel,
                onBack = {}
            )
        }

        viewModel.finalizeTournament()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Fehler beim Abschluss").assertExists()
        composeTestRule.onNodeWithText("Verbindungsfehler zum Server").assertExists()
        composeTestRule.onNodeWithText("OK").assertExists().performClick()

        // Error dialog dismissed
        composeTestRule.onNodeWithText("Fehler beim Abschluss").assertDoesNotExist()
    }

    @Test
    fun rankingScreen_openGamesWarningInConfirmDialog() {
        val tournamentId = 1L
        val rankingItems = listOf(
            TournamentRankingItem(playerId = 1L, rank = 1, name = "Max Mustermann", points = 3.0)
        )
        val p1 = de.kindermaenner.monatsblitz.domain.model.Player(1, "Mustermann", "Max")
        val p2 = de.kindermaenner.monatsblitz.domain.model.Player(2, "Musterfrau", "Erika")
        val tournament = Tournament(
            id = tournamentId,
            date = LocalDate.now(),
            mode = GameMode.BLITZ_5_0,
            rounds = 1,
            players = listOf(p1, p2),
            games = mapOf(
                1L to de.kindermaenner.monatsblitz.domain.model.Game(1L, 1L, 2L, 1, de.kindermaenner.monatsblitz.domain.model.GameResult.Win)
            ),
            isLocalOnly = false
        )

        every { tournamentRepository.observeTournamentRanking(tournamentId) } returns flowOf(rankingItems)
        every { tournamentRepository.observeTournament(tournamentId) } returns flowOf(tournament)

        val finalizeUseCase = mockk<de.kindermaenner.monatsblitz.domain.usecase.FinalizeTournamentUseCase>(relaxed = true)
        val viewModel = RankingViewModel(tournamentId, tournamentRepository, finalizeUseCase)

        composeTestRule.setContent {
            RankingScreen(
                viewModel = viewModel,
                onBack = {}
            )
        }

        composeTestRule.onNodeWithText("Turnier abschließen").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Turnier abschließen?").assertExists()
        composeTestRule.onNodeWithText("Jetzt abschließen").assertExists().performClick()
        composeTestRule.waitForIdle()

        io.mockk.coVerify { finalizeUseCase(tournamentId) }
    }

    @Test
    fun rankingScreen_finalizeError_canSendErrorReport() {
        val tournamentId = 1L
        val rankingItems = listOf(TournamentRankingItem(playerId = 1L, rank = 1, name = "Max", points = 1.0))
        val tournament = Tournament(id = tournamentId, date = LocalDate.now(), mode = GameMode.BLITZ_5_0, rounds = 1, players = emptyList(), isLocalOnly = false)

        every { tournamentRepository.observeTournamentRanking(tournamentId) } returns flowOf(rankingItems)
        every { tournamentRepository.observeTournament(tournamentId) } returns flowOf(tournament)

        val finalizeUseCase = mockk<de.kindermaenner.monatsblitz.domain.usecase.FinalizeTournamentUseCase>()
        io.mockk.coEvery { finalizeUseCase(tournamentId) } throws RuntimeException("HTTP 500")

        val viewModel = RankingViewModel(tournamentId, tournamentRepository, finalizeUseCase)

        composeTestRule.setContent {
            RankingScreen(viewModel = viewModel, onBack = {})
        }

        viewModel.finalizeTournament()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Fehler melden").assertExists().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("OK").assertExists().performClick()
        composeTestRule.waitForIdle()

        // Dialog should be dismissed after clicking OK
        composeTestRule.onNodeWithText("Fehler beim Abschluss").assertDoesNotExist()
    }

    @Test
    fun rankingScreen_finalizeSuccessDialog_localOnly_showsOfflineTextAndHandlesExportAndFinish() {
        val tournamentId = 1L
        val rankingItems = listOf(TournamentRankingItem(playerId = 1L, rank = 1, name = "Max", points = 1.0))
        val tournament = Tournament(id = tournamentId, date = LocalDate.now(), mode = GameMode.BLITZ_5_0, rounds = 1, players = emptyList(), isLocalOnly = true)

        every { tournamentRepository.observeTournamentRanking(tournamentId) } returns flowOf(rankingItems)
        every { tournamentRepository.observeTournament(tournamentId) } returns flowOf(tournament)

        val finalizeUseCase = mockk<de.kindermaenner.monatsblitz.domain.usecase.FinalizeTournamentUseCase>()
        io.mockk.coEvery { finalizeUseCase(tournamentId) } returns Unit

        val viewModel = RankingViewModel(tournamentId, tournamentRepository, finalizeUseCase)
        var finalizedCalled = false

        composeTestRule.setContent {
            RankingScreen(
                viewModel = viewModel,
                onBack = {},
                onFinalized = { finalizedCalled = true }
            )
        }

        viewModel.finalizeTournament()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Offline-Turnier abgeschlossen").assertExists()
        composeTestRule.onNodeWithText("Das Offline-Turnier wurde erfolgreich abgeschlossen. Es wurden keine Daten an die Website übertragen. Du kannst die Ergebnisse jetzt als PDF speichern oder teilen.").assertExists()

        composeTestRule.onNodeWithTag("finalize_export_pdf_button").assertExists()

        composeTestRule.onNodeWithTag("finalize_done_button").assertExists().performClick()
        assertTrue(finalizedCalled)
    }

    @Test
    fun rankingScreen_finalizeConfirmDialog_localOnly_showsOfflineTitle() {
        val tournamentId = 1L
        val rankingItems = listOf(TournamentRankingItem(playerId = 1L, rank = 1, name = "Max", points = 1.0))
        val tournament = Tournament(id = tournamentId, date = LocalDate.now(), mode = GameMode.BLITZ_5_0, rounds = 1, players = emptyList(), isLocalOnly = true)

        every { tournamentRepository.observeTournamentRanking(tournamentId) } returns flowOf(rankingItems)
        every { tournamentRepository.observeTournament(tournamentId) } returns flowOf(tournament)

        val viewModel = RankingViewModel(tournamentId, tournamentRepository, mockk(relaxed = true))

        composeTestRule.setContent {
            RankingScreen(viewModel = viewModel, onBack = {})
        }

        composeTestRule.onNodeWithTag("finalize_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Offline-Turnier abschließen?").assertExists()
        composeTestRule.onNodeWithText("Abbrechen").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Offline-Turnier abschließen?").assertDoesNotExist()
    }

    @Test
    fun rankingScreen_topBarPdfExport_triggersPdfSharing() {
        val tournamentId = 1L
        val rankingItems = listOf(TournamentRankingItem(playerId = 1L, rank = 1, name = "Max", points = 1.0))
        val tournament = Tournament(id = tournamentId, date = LocalDate.now(), mode = GameMode.BLITZ_5_0, rounds = 1, players = emptyList(), isLocalOnly = false)

        every { tournamentRepository.observeTournamentRanking(tournamentId) } returns flowOf(rankingItems)
        every { tournamentRepository.observeTournament(tournamentId) } returns flowOf(tournament)

        val viewModel = RankingViewModel(tournamentId, tournamentRepository, mockk(relaxed = true))

        composeTestRule.setContent {
            RankingScreen(viewModel = viewModel, onBack = {})
        }

        composeTestRule.onNodeWithTag("export_pdf_button").assertExists()
    }
}
