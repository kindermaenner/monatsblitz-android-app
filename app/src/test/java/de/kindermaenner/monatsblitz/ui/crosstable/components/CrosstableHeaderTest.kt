package de.kindermaenner.monatsblitz.ui.crosstable.components

import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import de.kindermaenner.monatsblitz.domain.model.GameMode
import de.kindermaenner.monatsblitz.domain.model.Player
import de.kindermaenner.monatsblitz.domain.model.Tournament
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class CrosstableHeaderTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun crosstableHeader_rendersHeadersAndPlayerInitialsCorrectly() {
        val p1 = Player(1L, "Meier", "Alice")
        val p2 = Player(2L, "Mueller", "Bob")
        val p3 = Player(3L, "Carlsen", "")
        val tournament = Tournament(
            id = 1L,
            mode = GameMode.BLITZ_3_2,
            date = LocalDate.now(),
            rounds = 1,
            players = listOf(p1, p2, p3)
        )

        composeTestRule.setContent {
            val scrollState = rememberScrollState()
            CrosstableHeader(
                tournament = tournament,
                horizontalScrollState = scrollState
            )
        }

        // Fixed headers
        composeTestRule.onNodeWithText("Nr").assertExists()
        composeTestRule.onNodeWithText("Spieler").assertExists()
        composeTestRule.onNodeWithText("Pkt.").assertExists()

        // Column indices
        composeTestRule.onNodeWithText("1").assertExists()
        composeTestRule.onNodeWithText("2").assertExists()
        composeTestRule.onNodeWithText("3").assertExists()

        // Initials
        composeTestRule.onNodeWithText("AM").assertExists()
        composeTestRule.onNodeWithText("BM").assertExists()
        composeTestRule.onNodeWithText("C").assertExists()
    }
}
