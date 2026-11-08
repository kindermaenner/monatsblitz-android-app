package de.kindermaenner.monatsblitz.ui.settings

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import de.kindermaenner.monatsblitz.domain.model.GameMode
import de.kindermaenner.monatsblitz.domain.repository.SettingsRepository
import de.kindermaenner.monatsblitz.domain.usecase.ConnectionTestResult
import de.kindermaenner.monatsblitz.domain.usecase.TestBackendConnectionUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val settingsRepository = mockk<SettingsRepository>(relaxed = true)
    private val testBackendConnectionUseCase = mockk<TestBackendConnectionUseCase>(relaxed = true)
    private val syncFlow = MutableStateFlow(true)
    private val serverUrlFlow = MutableStateFlow("https://sg-koenigslutter.de")
    private val apiKeyFlow = MutableStateFlow("test-key")
    private val offlineFlow = MutableStateFlow(false)
    private val maintenanceKeyFlow = MutableStateFlow("")

    @Before
    fun setup() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        try {
            com.google.mlkit.common.sdkinternal.MlKitContext.initializeIfNeeded(context)
        } catch (_: Exception) {}

        every { settingsRepository.showDetailedSyncIndicators } returns syncFlow
        every { settingsRepository.serverUrl } returns serverUrlFlow
        every { settingsRepository.apiKey } returns apiKeyFlow
        every { settingsRepository.standaloneOfflineMode } returns offlineFlow
        every { settingsRepository.maintenanceApiKey } returns maintenanceKeyFlow
    }

    @Test
    fun settingsScreen_displaysTitleAndSetting() {
        val viewModel = SettingsViewModel(settingsRepository, testBackendConnectionUseCase)
        var backClicked = false

        composeTestRule.setContent {
            SettingsScreen(
                viewModel = viewModel,
                onBack = { backClicked = true }
            )
        }

        composeTestRule.onNodeWithText("Einstellungen").assertExists()
        composeTestRule.onNodeWithText("Detaillierte Sync-Indikatoren").assertExists()
        composeTestRule.onNodeWithText("Zeigt kleine Statuspunkte an ungespeicherten Zellen und neuen Spielern, bis sie synchronisiert sind.").assertExists()

        composeTestRule.onNodeWithContentDescription("Zurück").performClick()
        assertTrue("Back button should trigger onBack", backClicked)
    }

    @Test
    fun settingsScreen_clickRow_togglesSetting() {
        coEvery { settingsRepository.setShowDetailedSyncIndicators(false) } returns Unit

        val viewModel = SettingsViewModel(settingsRepository, testBackendConnectionUseCase)

        composeTestRule.setContent {
            SettingsScreen(
                viewModel = viewModel,
                onBack = {}
            )
        }

        composeTestRule.onNodeWithText("Detaillierte Sync-Indikatoren")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        coVerify(timeout = 3000) { settingsRepository.setShowDetailedSyncIndicators(false) }
    }

    @Test
    fun settingsScreen_displaysOnboardingBannerWhenNotConfigured() {
        serverUrlFlow.value = ""
        apiKeyFlow.value = ""

        val viewModel = SettingsViewModel(settingsRepository, testBackendConnectionUseCase)

        composeTestRule.setContent {
            SettingsScreen(
                viewModel = viewModel,
                onBack = {}
            )
        }

        composeTestRule.onNodeWithText("Einstellungen").assertExists()
        composeTestRule.onNodeWithText("Willkommen beim Monatsblitz!").assertExists()
        composeTestRule.onNodeWithTag("save_settings_button").assertExists()
    }

    @Test
    fun settingsScreen_buttonsDisabledWhenFieldsAreBlank_enabledWhenFilled() {
        serverUrlFlow.value = ""
        apiKeyFlow.value = ""

        val viewModel = SettingsViewModel(settingsRepository, testBackendConnectionUseCase)

        composeTestRule.setContent {
            SettingsScreen(
                viewModel = viewModel,
                onBack = {}
            )
        }

        composeTestRule.onNodeWithTag("test_connection_button").assertIsNotEnabled()
        composeTestRule.onNodeWithTag("save_settings_button").assertIsNotEnabled()

        composeTestRule.onNodeWithTag("server_url_input").performTextInput("https://sg-koenigslutter.de")
        composeTestRule.onNodeWithTag("api_key_input").performTextInput("geheimes-token")

        composeTestRule.onNodeWithTag("test_connection_button").assertIsEnabled()
        composeTestRule.onNodeWithTag("save_settings_button").assertIsEnabled()
    }

    @Test
    fun settingsScreen_showsSuccessBannerOnSuccessfulConnectionTest() {
        coEvery { testBackendConnectionUseCase(any(), any()) } returns
            ConnectionTestResult.Success(playerCount = 8)

        val viewModel = SettingsViewModel(settingsRepository, testBackendConnectionUseCase)
        viewModel.onServerUrlChanged("https://sg-koenigslutter.de")
        viewModel.onApiKeyChanged("test-key")

        composeTestRule.setContent {
            SettingsScreen(
                viewModel = viewModel,
                onBack = {}
            )
        }

        composeTestRule.onNodeWithTag("test_connection_button")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Verbindung erfolgreich! 8 Spieler gefunden.")
            .performScrollTo()
            .assertExists()
    }

    @Test
    fun settingsScreen_showsErrorBannerOnFailedConnectionTest() {
        coEvery { testBackendConnectionUseCase(any(), any()) } returns
            ConnectionTestResult.Error("API-Schlüssel ungültig (401).")

        val viewModel = SettingsViewModel(settingsRepository, testBackendConnectionUseCase)
        viewModel.onServerUrlChanged("https://sg-koenigslutter.de")
        viewModel.onApiKeyChanged("test-key")

        composeTestRule.setContent {
            SettingsScreen(
                viewModel = viewModel,
                onBack = {}
            )
        }

        composeTestRule.onNodeWithTag("test_connection_button")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("API-Schlüssel ungültig (401).")
            .performScrollTo()
            .assertExists()
    }

    @Test
    fun settingsScreen_displaysQrScanButton() {
        val viewModel = SettingsViewModel(settingsRepository, testBackendConnectionUseCase)

        composeTestRule.setContent {
            SettingsScreen(
                viewModel = viewModel,
                onBack = {}
            )
        }

        composeTestRule.onNodeWithTag("scan_qr_button").assertExists()
        composeTestRule.onNodeWithText("QR-Code scannen").assertExists()
    }

    @Test
    fun settingsScreen_apiKeyVisibilityCanBeToggled() {
        val viewModel = SettingsViewModel(settingsRepository, testBackendConnectionUseCase)

        composeTestRule.setContent {
            SettingsScreen(
                viewModel = viewModel,
                onBack = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("Schlüssel anzeigen")
            .performClick()
        composeTestRule.onNodeWithContentDescription("Schlüssel verbergen")
            .assertExists()

        composeTestRule.onNodeWithContentDescription("Schlüssel verbergen")
            .performClick()
        composeTestRule.onNodeWithContentDescription("Schlüssel anzeigen")
            .assertExists()
    }

    @Test
    fun settingsScreen_offlineMode_hidesServerSettingsAndShowsContinueButton() {
        offlineFlow.value = true
        serverUrlFlow.value = ""
        apiKeyFlow.value = ""

        val viewModel = SettingsViewModel(settingsRepository, testBackendConnectionUseCase)
        var continueClicked = false

        composeTestRule.setContent {
            SettingsScreen(
                viewModel = viewModel,
                onBack = { continueClicked = true }
            )
        }

        // Section 1: Betriebsmodus exists
        composeTestRule.onNodeWithText("Betriebsmodus").assertExists()
        composeTestRule.onNodeWithTag("standalone_offline_switch").assertExists()

        // Section 2: Server & Sync are hidden
        composeTestRule.onNodeWithTag("server_url_input").assertDoesNotExist()
        composeTestRule.onNodeWithTag("api_key_input").assertDoesNotExist()
        composeTestRule.onNodeWithTag("scan_qr_button").assertDoesNotExist()
        composeTestRule.onNodeWithText("Offene Server-Turniere automatisch fortsetzen").assertDoesNotExist()
        composeTestRule.onNodeWithText("Detaillierte Sync-Indikatoren").assertDoesNotExist()

        // Section 3: Darstellung and Section 4: Turnier-Vorauswahl remain visible
        composeTestRule.onNodeWithText("Darstellung").performScrollTo().assertExists()
        composeTestRule.onNodeWithText("Turnier-Vorauswahl").performScrollTo().assertExists()

        // Bottom continue button exists and works
        composeTestRule.onNodeWithTag("continue_to_setup_button")
            .assertExists()
            .performClick()

        assertTrue("Continue button should trigger onBack", continueClicked)
    }

    @Test
    fun settingsScreen_onlineMode_showsServerSettingsAndShowsContinueButton() {
        offlineFlow.value = false
        serverUrlFlow.value = "https://sg-koenigslutter.de"
        apiKeyFlow.value = "valid-key"

        val viewModel = SettingsViewModel(settingsRepository, testBackendConnectionUseCase)

        composeTestRule.setContent {
            SettingsScreen(
                viewModel = viewModel,
                onBack = {}
            )
        }

        composeTestRule.onNodeWithText("WordPress & Synchronisation").assertExists()
        composeTestRule.onNodeWithTag("server_url_input").assertExists()
        composeTestRule.onNodeWithTag("api_key_input").assertExists()
        composeTestRule.onNodeWithTag("scan_qr_button").assertExists()
        composeTestRule.onNodeWithTag("continue_to_setup_button").assertExists()
    }

    @Test
    fun settingsScreen_whenNotConfigured_backClickShowsWarningDialog() {
        offlineFlow.value = false
        serverUrlFlow.value = ""
        apiKeyFlow.value = ""

        val viewModel = SettingsViewModel(settingsRepository, testBackendConnectionUseCase)
        var backClicked = false

        composeTestRule.setContent {
            SettingsScreen(
                viewModel = viewModel,
                onBack = { backClicked = true }
            )
        }

        // Click Back icon
        composeTestRule.onNodeWithContentDescription("Zurück").performClick()
        composeTestRule.waitForIdle()

        // Dialog should be displayed, backClicked should still be false
        composeTestRule.onNodeWithText("Betriebsmodus erforderlich").assertExists()
        org.junit.Assert.assertFalse("Back should not be called immediately", backClicked)

        // Click confirm fallback "Offline-Modus nutzen"
        composeTestRule.onNodeWithText("Offline-Modus nutzen").performClick()
        composeTestRule.waitForIdle()

        assertTrue("After choosing offline mode, onBack should be triggered", backClicked)
    }

    @Test
    fun settingsScreen_whenNotConfigured_dismissButtonClosesWarningDialogWithoutNavigating() {
        offlineFlow.value = false
        serverUrlFlow.value = ""
        apiKeyFlow.value = ""

        val viewModel = SettingsViewModel(settingsRepository, testBackendConnectionUseCase)
        var backClicked = false

        composeTestRule.setContent {
            SettingsScreen(
                viewModel = viewModel,
                onBack = { backClicked = true }
            )
        }

        // Click Back icon
        composeTestRule.onNodeWithContentDescription("Zurück").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Betriebsmodus erforderlich").assertExists()

        // Click dismiss button "Hier bleiben & einrichten"
        composeTestRule.onNodeWithText("Hier bleiben & einrichten").performClick()
        composeTestRule.waitForIdle()

        // Dialog should be gone and back should not have been called
        composeTestRule.onNodeWithText("Betriebsmodus erforderlich").assertDoesNotExist()
        org.junit.Assert.assertFalse("Back should not have been triggered", backClicked)
    }

    @Test
    fun settingsScreen_fixedModePreselection_rendersChipsAndHandlesSelection() {
        val modeTypeFlow = MutableStateFlow(de.kindermaenner.monatsblitz.domain.model.ModePreselectionType.FIXED)
        val fixedModeFlow = MutableStateFlow("3+2")
        val modeRepo = mockk<de.kindermaenner.monatsblitz.domain.repository.ModeRepository>(relaxed = true)

        every { settingsRepository.modePreselectionType } returns modeTypeFlow
        every { settingsRepository.fixedMode } returns fixedModeFlow
        coEvery { modeRepo.observeModes() } returns flowOf(listOf(GameMode.BLITZ_3_2, GameMode.BLITZ_5_0))

        val viewModel = SettingsViewModel(
            settingsRepository = settingsRepository,
            testBackendConnectionUseCase = testBackendConnectionUseCase,
            modeRepository = modeRepo
        )

        composeTestRule.setContent {
            SettingsScreen(viewModel = viewModel, onBack = {})
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Fester Standard-Modus:")
            .performScrollTo()
            .assertExists()
        composeTestRule.onNodeWithText("5+0")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        coVerify { settingsRepository.setFixedMode("5+0") }
    }

    @Test
    fun settingsScreen_rotationModePreselection_rendersCheckboxesAndToggles() {
        val modeTypeFlow = MutableStateFlow(de.kindermaenner.monatsblitz.domain.model.ModePreselectionType.ROTATION)
        val rotationModesFlow = MutableStateFlow(listOf("3+2"))
        val modeRepo = mockk<de.kindermaenner.monatsblitz.domain.repository.ModeRepository>(relaxed = true)

        every { settingsRepository.modePreselectionType } returns modeTypeFlow
        every { settingsRepository.rotationModes } returns rotationModesFlow
        coEvery { modeRepo.observeModes() } returns flowOf(listOf(GameMode.BLITZ_3_2, GameMode.BLITZ_5_0))

        val viewModel = SettingsViewModel(
            settingsRepository = settingsRepository,
            testBackendConnectionUseCase = testBackendConnectionUseCase,
            modeRepository = modeRepo
        )

        composeTestRule.setContent {
            SettingsScreen(viewModel = viewModel, onBack = {})
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Beteiligte Modi für die Rotation:")
            .performScrollTo()
            .assertExists()
        composeTestRule.onNodeWithText("3+2 (Nr. 1 in der Rotation)")
            .performScrollTo()
            .assertExists()
        composeTestRule.onNodeWithText("5+0")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        coVerify { settingsRepository.setRotationModes(any()) }
    }

    @Test
    fun settingsScreen_adminSection_whenLocked_allowsEnteringKeyAndVerifying() {
        val api = mockk<de.kindermaenner.monatsblitz.infrastructure.api.MonatsblitzApi>(relaxed = true)
        coEvery { api.verifyMaintenanceAuth("test-admin-key") } returns
            de.kindermaenner.monatsblitz.infrastructure.api.dto.VerifyAuthResponseDto(valid = true, role = "admin")

        val viewModel = SettingsViewModel(
            settingsRepository = settingsRepository,
            testBackendConnectionUseCase = testBackendConnectionUseCase,
            monatsblitzApi = api
        )

        composeTestRule.setContent {
            SettingsScreen(viewModel = viewModel, onBack = {})
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Admin-Funktionen (Wartung)")
            .performScrollTo()
            .assertExists()

        composeTestRule.onNodeWithTag("maintenance_key_input")
            .performScrollTo()
            .performTextInput("test-admin-key")

        composeTestRule.onNodeWithTag("verify_admin_key_button")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        coVerify { api.verifyMaintenanceAuth("test-admin-key") }
    }

    @Test
    fun settingsScreen_adminSection_whenActive_allowsReopeningTournamentAndRecreatingPostsAndLocking() {
        maintenanceKeyFlow.value = "active_admin_key"
        val api = mockk<de.kindermaenner.monatsblitz.infrastructure.api.MonatsblitzApi>(relaxed = true)
        val storage = mockk<de.kindermaenner.monatsblitz.infrastructure.TournamentStorage>(relaxed = true)

        coEvery { api.reopenTournament("active_admin_key", any()) } returns
            de.kindermaenner.monatsblitz.infrastructure.api.dto.ReopenTournamentResponseDto(success = true, message = "Turnier geöffnet")
        coEvery { api.recreatePostsWithKey("active_admin_key") } returns
            de.kindermaenner.monatsblitz.infrastructure.api.dto.RecreatePostsDto(success = true, processed = 2, succeeded = 2, failed = 0)

        val viewModel = SettingsViewModel(
            settingsRepository = settingsRepository,
            testBackendConnectionUseCase = testBackendConnectionUseCase,
            monatsblitzApi = api,
            tournamentStorage = storage
        )

        composeTestRule.setContent {
            SettingsScreen(viewModel = viewModel, onBack = {})
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Admin-Modus aktiv")
            .performScrollTo()
            .assertExists()

        // Reopen tournament
        composeTestRule.onNodeWithTag("reopen_tournament_id_input")
            .performScrollTo()
            .performTextInput("42")

        composeTestRule.onNodeWithTag("reopen_tournament_button")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        coVerify { api.reopenTournament("active_admin_key", match { it.tournamentId == 42L }) }

        // Recreate posts
        composeTestRule.onNodeWithTag("recreate_posts_button")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        coVerify { api.recreatePostsWithKey("active_admin_key") }

        // Lock button
        composeTestRule.onNodeWithTag("admin_lock_button")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        coVerify { settingsRepository.setMaintenanceApiKey("") }
    }

    @Test
    fun settingsScreen_adminSection_feedbackBanner_displaysMessageAndDismisses() {
        val viewModel = SettingsViewModel(
            settingsRepository = settingsRepository,
            testBackendConnectionUseCase = testBackendConnectionUseCase
        )

        composeTestRule.setContent {
            SettingsScreen(viewModel = viewModel, onBack = {})
        }
        composeTestRule.waitForIdle()

        // Trigger an admin error by verifying with blank input
        composeTestRule.onNodeWithTag("maintenance_key_input")
            .performScrollTo()
            .performTextInput("   ")

        // Call method directly on viewModel to show banner
        viewModel.onMaintenanceKeyChanged("   ")
        viewModel.verifyAndActivateAdminKey()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Bitte gib einen Wartungs-API-Key ein")
            .performScrollTo()
            .assertExists()

        composeTestRule.onNodeWithTag("dismiss_admin_feedback_button")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Bitte gib einen Wartungs-API-Key ein")
            .assertDoesNotExist()
    }

    @Test
    fun settingsScreen_helpAndFeedback_rendersButton() {
        val viewModel = SettingsViewModel(settingsRepository, testBackendConnectionUseCase)

        composeTestRule.setContent {
            SettingsScreen(viewModel = viewModel, onBack = {})
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Fehlerbericht / Feedback senden")
            .performScrollTo()
            .assertExists()
    }

    @Test
    fun settingsScreen_rendersDumpListSection_andAllowsSharing() {
        val dumpRepo = mockk<de.kindermaenner.monatsblitz.domain.repository.TournamentDumpRepository>(relaxed = true)
        val dummyDump = de.kindermaenner.monatsblitz.domain.model.TournamentDumpFileInfo(
            fileName = "tournament_42_20261108.json",
            filePath = "/fake/path/tournament_42_20261108.json",
            tournamentId = 42L,
            timestamp = "20261108",
            sizeBytes = 2048L
        )
        coEvery { dumpRepo.getDumpFiles() } returns listOf(dummyDump)

        val viewModel = SettingsViewModel(
            settingsRepository = settingsRepository,
            testBackendConnectionUseCase = testBackendConnectionUseCase,
            tournamentDumpRepository = dumpRepo
        )
        viewModel.loadDumps()

        composeTestRule.setContent {
            SettingsScreen(viewModel = viewModel, onBack = {})
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Lokale Sicherungen (1)")
            .performScrollTo()
            .assertExists()

        composeTestRule.onNodeWithText("Turnier #42")
            .performScrollTo()
            .assertExists()

        composeTestRule.onNodeWithTag("share_dump_tournament_42_20261108.json")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun settingsScreen_toggleAutoCleanupDumps_triggersRepository() {
        val autoCleanupFlow = MutableStateFlow(true)
        every { settingsRepository.autoCleanupDumps } returns autoCleanupFlow

        val viewModel = SettingsViewModel(
            settingsRepository = settingsRepository,
            testBackendConnectionUseCase = testBackendConnectionUseCase
        )

        composeTestRule.setContent {
            SettingsScreen(viewModel = viewModel, onBack = {})
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("settings_auto_cleanup_dumps_switch")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        coVerify { settingsRepository.setAutoCleanupDumps(false) }
    }
}
