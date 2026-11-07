package de.kindermaenner.monatsblitz.ui.tournamentsetup.components

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import de.kindermaenner.monatsblitz.domain.model.GameMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GameModeDropdownSelectorTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun selector_displaysSelectedModeAndSelectsNewMode() {
        var selected: GameMode? = GameMode.BLITZ_3_2
        var addNewModeCalled = false

        composeTestRule.setContent {
            GameModeDropdownSelector(
                selectedMode = selected,
                availableModes = listOf(GameMode.BLITZ_3_2, GameMode.BLITZ_5_0),
                onModeSelected = { selected = it },
                onAddNewMode = { addNewModeCalled = true }
            )
        }

        composeTestRule.onNodeWithText("3+2").assertExists()

        // Click to expand
        composeTestRule.onNodeWithText("3+2").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("5+0").assertExists().performClick()
        assertEquals(GameMode.BLITZ_5_0, selected)

        // Expand again and click Add New Mode
        composeTestRule.onNodeWithText("3+2").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Neuer Modus...").assertExists().performClick()
        assertTrue(addNewModeCalled)
    }

    @Test
    fun selector_emptySelection_showsPlaceholder() {
        composeTestRule.setContent {
            GameModeDropdownSelector(
                selectedMode = null,
                availableModes = listOf(GameMode.BLITZ_5_0),
                onModeSelected = {},
                onAddNewMode = null
            )
        }

        composeTestRule.onNodeWithText("Modus").assertExists()
    }
}
