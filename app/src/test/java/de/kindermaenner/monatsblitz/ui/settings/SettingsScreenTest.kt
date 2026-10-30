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
import de.kindermaenner.monatsblitz.domain.repository.SettingsRepository
import de.kindermaenner.monatsblitz.domain.usecase.ConnectionTestResult
import de.kindermaenner.monatsblitz.domain.usecase.TestBackendConnectionUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
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
}
