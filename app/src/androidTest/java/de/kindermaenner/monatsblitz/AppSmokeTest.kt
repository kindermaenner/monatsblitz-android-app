package de.kindermaenner.monatsblitz

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Finaler Smoke-Test für echte Geräte / Release-Pipelines:
 * Startet die echte MainActivity und stellt sicher, dass der AppContainer,
 * das Theme, die Root-Navigation und der Splash-Screen fehlerfrei initialisieren.
 */
@RunWith(AndroidJUnit4::class)
class AppSmokeTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun app_launchesSuccessfullyAndRenders() {
        composeTestRule.waitUntil(timeoutMillis = 10_000) {
            composeTestRule.onAllNodes(
                hasText("Monatsblitz Einrichtung") or
                hasText("Einstellungen") or
                hasText("Neues Turnier")
            ).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
