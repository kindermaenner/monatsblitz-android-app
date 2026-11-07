package de.kindermaenner.monatsblitz.ui

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import com.google.mlkit.common.sdkinternal.MlKitContext
import de.kindermaenner.monatsblitz.app.AppContainer
import de.kindermaenner.monatsblitz.ui.navigation.AppRoute
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MonatsblitzAppTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        try {
            MlKitContext.initializeIfNeeded(context)
        } catch (_: Exception) {}
    }

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
