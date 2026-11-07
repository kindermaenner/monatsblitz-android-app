package de.kindermaenner.monatsblitz.ui.crosstable.components

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import de.kindermaenner.monatsblitz.domain.model.Player
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AddPlayerToTournamentDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val player1 = Player(id = 1, name = "Meier", vorname = "Alice", rufname = "Ali", isLocal = false)
    private val player2 = Player(id = 2, name = "Mueller", vorname = "Bob", rufname = null, isLocal = true)

    @Test
    fun dialog_doesNotRenderWhenShowIsFalse() {
        composeTestRule.setContent {
            AddPlayerToTournamentDialog(
                show = false,
                availablePlayers = listOf(player1),
                isTournamentLocalOnly = false,
                onAddExistingPlayer = {},
                onAddNewPlayer = { _, _, _, _, _, _ -> },
                onDismiss = {}
            )
        }

        composeTestRule.onNodeWithText("Spieler zum Turnier hinzufügen").assertDoesNotExist()
    }

    @Test
    fun dialog_showsExistingPlayersAndAllowsAddingExistingPlayer() {
        var addedPlayer: Player? = null
        var dismissed = false

        composeTestRule.setContent {
            AddPlayerToTournamentDialog(
                show = true,
                availablePlayers = listOf(player1, player2),
                isTournamentLocalOnly = false,
                onAddExistingPlayer = { addedPlayer = it },
                onAddNewPlayer = { _, _, _, _, _, _ -> },
                onDismiss = { dismissed = true }
            )
        }

        composeTestRule.onNodeWithText("Spieler zum Turnier hinzufügen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("tab_existing_player").assertIsDisplayed()
        composeTestRule.onNodeWithText("Alice Meier (Ali)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bob Mueller").assertIsDisplayed()
        composeTestRule.onNodeWithText("Nur lokal").assertIsDisplayed()

        // Click button to add player 1
        composeTestRule.onNodeWithTag("button_add_player_1").performClick()
        assertEquals(player1, addedPlayer)

        // Dismiss button
        composeTestRule.onNodeWithTag("cancel_add_player_dialog_button").performClick()
        assertTrue(dismissed)
    }

    @Test
    fun dialog_filterExistingPlayersAndClearSearch() {
        composeTestRule.setContent {
            AddPlayerToTournamentDialog(
                show = true,
                availablePlayers = listOf(player1, player2),
                isTournamentLocalOnly = false,
                onAddExistingPlayer = {},
                onAddNewPlayer = { _, _, _, _, _, _ -> },
                onDismiss = {}
            )
        }

        // Search for "Alice"
        composeTestRule.onNodeWithTag("search_player_input").performTextInput("Alice")
        composeTestRule.onNodeWithText("Alice Meier (Ali)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bob Mueller").assertDoesNotExist()

        // Clear search using icon
        composeTestRule.onNodeWithContentDescription("Suche leeren").performClick()
        composeTestRule.onNodeWithText("Bob Mueller").assertIsDisplayed()
    }

    @Test
    fun dialog_searchNoMatchShowsSwitchToNewPlayer() {
        composeTestRule.setContent {
            AddPlayerToTournamentDialog(
                show = true,
                availablePlayers = listOf(player1),
                isTournamentLocalOnly = false,
                onAddExistingPlayer = {},
                onAddNewPlayer = { _, _, _, _, _, _ -> },
                onDismiss = {}
            )
        }

        composeTestRule.onNodeWithTag("search_player_input").performTextInput("Nonexistent")
        composeTestRule.onNodeWithText("Kein Spieler gefunden.").assertIsDisplayed()

        composeTestRule.onNodeWithTag("switch_to_new_player_button").performClick()
        composeTestRule.onNodeWithTag("new_player_vorname_input").assertIsDisplayed()
    }

    @Test
    fun dialog_emptyAvailablePlayersDefaultsToNewPlayerTab() {
        composeTestRule.setContent {
            AddPlayerToTournamentDialog(
                show = true,
                availablePlayers = emptyList(),
                isTournamentLocalOnly = false,
                onAddExistingPlayer = {},
                onAddNewPlayer = { _, _, _, _, _, _ -> },
                onDismiss = {}
            )
        }

        // Defaults to NEW tab when available players is empty
        composeTestRule.onNodeWithTag("new_player_vorname_input").assertExists()
    }

    @Test
    fun dialog_createNewPlayerSuccess() {
        var submittedVorname = ""
        var submittedName = ""
        var submittedRufname: String? = null
        var submittedLocal = false
        var dismissed = false

        composeTestRule.setContent {
            AddPlayerToTournamentDialog(
                show = true,
                availablePlayers = emptyList(), // Opens directly in NEW tab
                isTournamentLocalOnly = false,
                onAddExistingPlayer = {},
                onAddNewPlayer = { vorname, name, rufname, isLocal, onSuccess, _ ->
                    submittedVorname = vorname
                    submittedName = name
                    submittedRufname = rufname
                    submittedLocal = isLocal
                    onSuccess()
                },
                onDismiss = { dismissed = true }
            )
        }

        val submitButton = composeTestRule.onNodeWithTag("submit_new_player_button")
        submitButton.assertIsNotEnabled()

        composeTestRule.onNodeWithTag("new_player_vorname_input").performTextInput("Charlie")
        submitButton.assertIsNotEnabled()

        composeTestRule.onNodeWithTag("new_player_name_input").performTextInput("Brown")
        submitButton.assertIsEnabled()

        composeTestRule.onNodeWithTag("new_player_rufname_input").performTextInput("Chuck")

        // Toggle local checkbox
        composeTestRule.onNodeWithText("Nur lokal (nicht mit Server synchronisieren)").performClick()

        submitButton.performClick()

        assertEquals("Charlie", submittedVorname)
        assertEquals("Brown", submittedName)
        assertEquals("Chuck", submittedRufname)
        assertTrue(submittedLocal)
        assertTrue(dismissed)
    }

    @Test
    fun dialog_createNewPlayerErrorDisplaysErrorMessage() {
        val errorMessageText = "Spieler existiert bereits"

        composeTestRule.setContent {
            AddPlayerToTournamentDialog(
                show = true,
                availablePlayers = emptyList(),
                isTournamentLocalOnly = true, // isTournamentLocalOnly hides local checkbox
                onAddExistingPlayer = {},
                onAddNewPlayer = { _, _, _, _, _, onError ->
                    onError(errorMessageText)
                },
                onDismiss = {}
            )
        }

        // Local checkbox should be hidden when isTournamentLocalOnly = true
        composeTestRule.onNodeWithText("Nur lokal (nicht mit Server synchronisieren)").assertDoesNotExist()

        composeTestRule.onNodeWithTag("new_player_vorname_input").performTextInput("David")
        composeTestRule.onNodeWithTag("new_player_name_input").performTextInput("Miller")

        composeTestRule.onNodeWithTag("submit_new_player_button").performClick()

        composeTestRule.onNodeWithTag("new_player_error_text").assertIsDisplayed()
        composeTestRule.onNodeWithText(errorMessageText).assertIsDisplayed()
    }
}
