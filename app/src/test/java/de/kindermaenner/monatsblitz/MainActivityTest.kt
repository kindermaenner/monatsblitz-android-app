package de.kindermaenner.monatsblitz

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MainActivityTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun activity_launchesAndRendersComposeContent() {
        composeTestRule.waitForIdle()
        assertNotNull(composeTestRule.activity)
    }

    @Test
    fun activity_launchesAndPassesLifecycle() {
        val controller = Robolectric.buildActivity(MainActivity::class.java)
        controller.create().start().resume()
        val activity = controller.get()
        assertNotNull(activity)
        controller.pause().stop().destroy()
    }
}
