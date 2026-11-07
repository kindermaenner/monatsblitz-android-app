package de.kindermaenner.monatsblitz.ui.ranking.components

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import de.kindermaenner.monatsblitz.ui.ranking.RankingRowData
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RankingTableTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun rankingTable_rendersHeadersAndRowsCorrectly() {
        val rows = listOf(
            RankingRowData(rank = 1, name = "Alice Meier", points = 3.0),
            RankingRowData(rank = 2, name = "Bob Mueller", points = 2.5),
            RankingRowData(rank = 4, name = "Charlie Brown", points = 0.0)
        )

        composeTestRule.setContent {
            RankingTable(rows = rows)
        }

        // Headers
        composeTestRule.onNodeWithText("Platz").assertExists()
        composeTestRule.onNodeWithText("Name").assertExists()
        composeTestRule.onNodeWithText("Punkte").assertExists()

        // Rows
        composeTestRule.onNodeWithText("Alice Meier").assertExists()
        composeTestRule.onNodeWithText("3").assertExists()

        composeTestRule.onNodeWithText("Bob Mueller").assertExists()
        composeTestRule.onNodeWithText("2.5").assertExists()

        composeTestRule.onNodeWithText("Charlie Brown").assertExists()
        composeTestRule.onNodeWithText("0").assertExists()
        composeTestRule.onNodeWithText("4").assertExists()
    }
}
