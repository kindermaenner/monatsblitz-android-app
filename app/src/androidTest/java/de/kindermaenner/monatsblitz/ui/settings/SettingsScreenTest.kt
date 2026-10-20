package de.kindermaenner.monatsblitz.ui.settings

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import de.kindermaenner.monatsblitz.domain.repository.SettingsRepository
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
        val viewModel = SettingsViewModel(settingsRepository)
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

        val viewModel = SettingsViewModel(settingsRepository)

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

        val viewModel = SettingsViewModel(settingsRepository)

        composeTestRule.setContent {
            SettingsScreen(
                viewModel = viewModel,
                onBack = {}
            )
        }

        composeTestRule.onNodeWithText("Monatsblitz Einrichtung").assertExists()
        composeTestRule.onNodeWithText("Willkommen beim Monatsblitz!").assertExists()
    }
}
