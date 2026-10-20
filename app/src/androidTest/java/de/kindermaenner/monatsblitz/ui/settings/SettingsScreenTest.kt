package de.kindermaenner.monatsblitz.ui.settings

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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

class SettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val settingsRepository = mockk<SettingsRepository>(relaxed = true)
    private val testBackendConnectionUseCase = mockk<TestBackendConnectionUseCase>(relaxed = true)
    private val syncFlow = MutableStateFlow(true)
    private val serverUrlFlow = MutableStateFlow("https://sg-koenigslutter.de")
    private val apiKeyFlow = MutableStateFlow("test-key")

    @Before
    fun setup() {
        every { settingsRepository.showDetailedSyncIndicators } returns syncFlow
        every { settingsRepository.serverUrl } returns serverUrlFlow
        every { settingsRepository.apiKey } returns apiKeyFlow
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

        composeTestRule.onNodeWithText("Detaillierte Sync-Indikatoren").performClick()
        composeTestRule.waitForIdle()

        coVerify { settingsRepository.setShowDetailedSyncIndicators(false) }
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

        composeTestRule.onNodeWithText("Monatsblitz Einrichtung").assertExists()
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

        composeTestRule.setContent {
            SettingsScreen(
                viewModel = viewModel,
                onBack = {}
            )
        }

        composeTestRule.onNodeWithTag("test_connection_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Verbindung erfolgreich! 8 Spieler gefunden.").assertExists()
    }

    @Test
    fun settingsScreen_showsErrorBannerOnFailedConnectionTest() {
        coEvery { testBackendConnectionUseCase(any(), any()) } returns
            ConnectionTestResult.Error("API-Schlüssel ungültig (401).")

        val viewModel = SettingsViewModel(settingsRepository, testBackendConnectionUseCase)

        composeTestRule.setContent {
            SettingsScreen(
                viewModel = viewModel,
                onBack = {}
            )
        }

        composeTestRule.onNodeWithTag("test_connection_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("API-Schlüssel ungültig (401).").assertExists()
    }
}
