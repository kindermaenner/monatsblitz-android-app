package de.kindermaenner.monatsblitz.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import de.kindermaenner.monatsblitz.infrastructure.settings.AppSettingsStorage
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val appSettingsStorage = mockk<AppSettingsStorage>(relaxed = true)

    @Test
    fun settingsScreen_displaysTitleAndSetting() {
        val flow = MutableStateFlow(true)
        every { appSettingsStorage.showDetailedSyncIndicators } returns flow

        var backClicked = false

        composeTestRule.setContent {
            SettingsScreen(
                appSettingsStorage = appSettingsStorage,
                onBack = { backClicked = true }
            )
        }

        composeTestRule.onNodeWithText("Einstellungen").assertIsDisplayed()
        composeTestRule.onNodeWithText("Detaillierte Sync-Indikatoren").assertIsDisplayed()
        composeTestRule.onNodeWithText("Zeigt kleine Statuspunkte an ungespeicherten Zellen und neuen Spielern, bis sie synchronisiert sind.").assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription("Zurück").performClick()
        assertTrue("Back button should trigger onBack", backClicked)
    }

    @Test
    fun settingsScreen_clickRow_togglesSetting() {
        val flow = MutableStateFlow(true)
        every { appSettingsStorage.showDetailedSyncIndicators } returns flow
        coEvery { appSettingsStorage.setShowDetailedSyncIndicators(false) } returns Unit

        composeTestRule.setContent {
            SettingsScreen(
                appSettingsStorage = appSettingsStorage,
                onBack = {}
            )
        }

        composeTestRule.onNodeWithText("Detaillierte Sync-Indikatoren").performClick()
        composeTestRule.waitForIdle()

        coVerify { appSettingsStorage.setShowDetailedSyncIndicators(false) }
    }
}
