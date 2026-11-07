package de.kindermaenner.monatsblitz.ui

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import de.kindermaenner.monatsblitz.app.AppContainer
import de.kindermaenner.monatsblitz.ui.navigation.AppRoute
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MonatsblitzAppTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun monatsblitzApp_rendersWithSettingsStartDestination() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val container = AppContainer(context)

        composeTestRule.setContent {
            MonatsblitzApp(
                appContainer = container,
                startDestination = AppRoute.Settings
            )
        }

        composeTestRule.onNodeWithText("Einstellungen").assertExists()
        container.networkSyncObserver.stop()
    }
}
