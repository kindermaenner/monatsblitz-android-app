package de.kindermaenner.monatsblitz.ui.crosstable.components

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import de.kindermaenner.monatsblitz.domain.model.GameResult
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ResultCellTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `selecting a regular result reports it and closes the picker`() {
        var selectedResult: GameResult? = null

        composeTestRule.setContent {
            ResultCell(
                params = ResultCellParams(
                    value = "",
                    enabled = true,
                    player1Name = "Alice",
                    player2Name = "Bob",
                    round = 2
                ),
                onResultSelected = { selectedResult = it }
            )
        }

        composeTestRule.onNode(hasClickAction()).performClick()
        composeTestRule.onNodeWithText("Ergebnis eintragen (R2)").assertExists()
        composeTestRule.onNodeWithText("Alice  vs.  Bob").assertExists()
        composeTestRule.onNodeWithText("Remis", substring = true).performClick()

        assertEquals(GameResult.Remis, selectedResult)
        composeTestRule.onNodeWithText("Ergebnis eintragen (R2)").assertDoesNotExist()
    }

    @Test
    fun `picker supports forfeit results and resetting an existing result`() {
        val selectedResults = mutableListOf<GameResult>()

        composeTestRule.setContent {
            ResultCell(
                params = ResultCellParams(
                    value = GameResult.Win.displayName,
                    enabled = true,
                    player1Name = "Alice"
                ),
                onResultSelected = selectedResults::add
            )
        }

        composeTestRule.onNode(hasClickAction()).performClick()
        composeTestRule.onNodeWithText("kl. Sieg", substring = true).performClick()
        assertEquals(GameResult.ForfeitWin, selectedResults.last())
        composeTestRule.onNodeWithText("Ergebnis zurücksetzen / löschen").assertDoesNotExist()

        composeTestRule.onNode(hasClickAction()).performClick()
        composeTestRule.onNodeWithText("Ergebnis zurücksetzen / löschen").performClick()

        assertEquals(GameResult.Open, selectedResults.last())
        composeTestRule.onNodeWithText("Ergebnis eintragen (R1)").assertDoesNotExist()
    }

    @Test
    fun `picker distinguishes wins losses and forfeited losses`() {
        val selectedResults = mutableListOf<GameResult>()

        composeTestRule.setContent {
            ResultCell(
                params = ResultCellParams(value = "", enabled = true),
                onResultSelected = selectedResults::add
            )
        }

        composeTestRule.onNode(hasClickAction()).performClick()
        composeTestRule.onNodeWithText("1").performClick()
        assertEquals(GameResult.Win, selectedResults.last())

        composeTestRule.onNode(hasClickAction()).performClick()
        composeTestRule.onNodeWithText("0")
            .performClick()
        assertEquals(GameResult.Loss, selectedResults.last())

        composeTestRule.onNode(hasClickAction()).performClick()
        composeTestRule.onNodeWithText("kl. Niederlage", substring = true)
            .performClick()
        assertEquals(GameResult.ForfeitLoss, selectedResults.last())
    }

    @Test
    fun `empty result cannot be reset and picker can be dismissed`() {
        composeTestRule.setContent {
            ResultCell(
                params = ResultCellParams(value = "", enabled = true),
                onResultSelected = {}
            )
        }

        composeTestRule.onNode(hasClickAction()).performClick()
        composeTestRule.onNodeWithText("Ergebnis zurücksetzen / löschen").assertDoesNotExist()
        composeTestRule.onNodeWithText("Abbrechen").performClick()

        composeTestRule.onNodeWithText("Ergebnis eintragen (R1)").assertDoesNotExist()
    }

    @Test
    fun `read only and diagonal cells cannot open the result picker`() {
        composeTestRule.setContent {
            Column {
                ResultCell(
                    params = ResultCellParams(
                        value = GameResult.Win.displayName,
                        enabled = true,
                        readOnly = true
                    ),
                    onResultSelected = {}
                )
                ResultCell(
                    params = ResultCellParams(value = "x", enabled = true),
                    onResultSelected = {}
                )
            }
        }

        composeTestRule.onNodeWithText(GameResult.Win.displayName).assertExists()
        composeTestRule.onNodeWithText("✕").assertExists()
        assertEquals(0, composeTestRule.onAllNodes(hasClickAction()).fetchSemanticsNodes().size)
        composeTestRule.onNodeWithText("Ergebnis eintragen (R1)").assertDoesNotExist()
    }
}
