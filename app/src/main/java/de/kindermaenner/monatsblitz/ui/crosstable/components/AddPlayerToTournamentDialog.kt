package de.kindermaenner.monatsblitz.ui.crosstable.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import de.kindermaenner.monatsblitz.domain.model.Player

private enum class AddPlayerTab {
    EXISTING,
    NEW
}

@Composable
fun AddPlayerToTournamentDialog(
    show: Boolean,
    availablePlayers: List<Player>,
    isTournamentLocalOnly: Boolean,
    onAddExistingPlayer: (Player) -> Unit,
    onAddNewPlayer: (vorname: String, name: String, rufname: String?, isLocal: Boolean, onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit,
    onDismiss: () -> Unit
) {
    if (!show) return

    var selectedTab by remember {
        mutableStateOf(if (availablePlayers.isNotEmpty()) AddPlayerTab.EXISTING else AddPlayerTab.NEW)
    }
    var searchQuery by remember { mutableStateOf("") }
    var vorname by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var rufname by remember { mutableStateOf("") }
    var isLocalPlayer by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    val filteredPlayers = remember(availablePlayers, searchQuery) {
        if (searchQuery.isBlank()) {
            availablePlayers
        } else {
            val q = searchQuery.trim().lowercase()
            availablePlayers.filter {
                it.fullName.lowercase().contains(q) ||
                    it.name.lowercase().contains(q) ||
                    it.vorname.lowercase().contains(q) ||
                    it.rufname?.lowercase()?.contains(q) == true
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Spieler zum Turnier hinzufügen")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    SegmentedButton(
                        selected = selectedTab == AddPlayerTab.EXISTING,
                        onClick = {
                            selectedTab = AddPlayerTab.EXISTING
                            errorMessage = null
                        },
                        shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tab_existing_player")
                    ) {
                        Text("Vorhanden (${availablePlayers.size})")
                    }
                    SegmentedButton(
                        selected = selectedTab == AddPlayerTab.NEW,
                        onClick = {
                            selectedTab = AddPlayerTab.NEW
                            errorMessage = null
                        },
                        shape = RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tab_new_player")
                    ) {
                        Text("Neu anlegen")
                    }
                }

                when (selectedTab) {
                    AddPlayerTab.EXISTING -> {
                        ExistingPlayerTabContent(
                            searchQuery = searchQuery,
                            onSearchQueryChange = { searchQuery = it },
                            filteredPlayers = filteredPlayers,
                            allAvailablePlayersEmpty = availablePlayers.isEmpty(),
                            onAddPlayer = onAddExistingPlayer,
                            onSwitchToNewPlayer = { selectedTab = AddPlayerTab.NEW }
                        )
                    }

                    AddPlayerTab.NEW -> {
                        NewPlayerTabContent(
                            vorname = vorname,
                            onVornameChange = {
                                vorname = it
                                errorMessage = null
                            },
                            name = name,
                            onNameChange = {
                                name = it
                                errorMessage = null
                            },
                            rufname = rufname,
                            onRufnameChange = {
                                rufname = it
                                errorMessage = null
                            },
                            isLocalPlayer = isLocalPlayer,
                            onIsLocalPlayerChange = { isLocalPlayer = it },
                            isTournamentLocalOnly = isTournamentLocalOnly,
                            errorMessage = errorMessage
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (selectedTab == AddPlayerTab.NEW) {
                Button(
                    onClick = {
                        isSubmitting = true
                        onAddNewPlayer(
                            vorname.trim(),
                            name.trim(),
                            rufname.trim().takeIf { it.isNotBlank() },
                            isLocalPlayer,
                            {
                                isSubmitting = false
                                onDismiss()
                            },
                            { err ->
                                isSubmitting = false
                                errorMessage = err
                            }
                        )
                    },
                    enabled = !isSubmitting && vorname.isNotBlank() && name.isNotBlank(),
                    modifier = Modifier.testTag("submit_new_player_button")
                ) {
                    Text("Hinzufügen")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_add_player_dialog_button")
            ) {
                Text("Abbrechen")
            }
        }
    )
}

@Composable
private fun ExistingPlayerTabContent(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    filteredPlayers: List<Player>,
    allAvailablePlayersEmpty: Boolean,
    onAddPlayer: (Player) -> Unit,
    onSwitchToNewPlayer: () -> Unit
) {
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        label = { Text("Spieler suchen") },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = null)
        },
        trailingIcon = {
            if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { onSearchQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Suche leeren")
                }
            }
        },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("search_player_input")
    )

    Spacer(modifier = Modifier.height(12.dp))

    if (filteredPlayers.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (allAvailablePlayersEmpty) {
                    "Alle vorhandenen Spieler nehmen bereits teil."
                } else {
                    "Kein Spieler gefunden."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = onSwitchToNewPlayer,
                modifier = Modifier.testTag("switch_to_new_player_button")
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Neuen Spieler anlegen")
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 240.dp)
                .testTag("available_players_list")
        ) {
            items(filteredPlayers, key = { it.id }) { player ->
                ExistingPlayerListItem(
                    player = player,
                    onAddPlayer = onAddPlayer
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun ExistingPlayerListItem(
    player: Player,
    onAddPlayer: (Player) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAddPlayer(player) }
            .padding(vertical = 10.dp, horizontal = 4.dp)
            .testTag("add_player_item_${player.id}"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 12.dp)
            )
            Column {
                val displayName = if (!player.rufname.isNullOrBlank()) {
                    "${player.fullName} (${player.rufname})"
                } else {
                    player.fullName
                }
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.bodyLarge
                )
                if (player.isLocal) {
                    Text(
                        text = "Nur lokal",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
        Button(
            onClick = { onAddPlayer(player) },
            modifier = Modifier.testTag("button_add_player_${player.id}")
        ) {
            Text("Hinzufügen")
        }
    }
}

@Composable
private fun NewPlayerTabContent(
    vorname: String,
    onVornameChange: (String) -> Unit,
    name: String,
    onNameChange: (String) -> Unit,
    rufname: String,
    onRufnameChange: (String) -> Unit,
    isLocalPlayer: Boolean,
    onIsLocalPlayerChange: (Boolean) -> Unit,
    isTournamentLocalOnly: Boolean,
    errorMessage: String?
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = vorname,
            onValueChange = onVornameChange,
            label = { Text("Vorname") },
            isError = errorMessage != null,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                capitalization = KeyboardCapitalization.Words
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("new_player_vorname_input")
        )

        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Nachname") },
            isError = errorMessage != null,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                capitalization = KeyboardCapitalization.Words
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("new_player_name_input")
        )

        OutlinedTextField(
            value = rufname,
            onValueChange = onRufnameChange,
            label = { Text("Rufname (optional)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                capitalization = KeyboardCapitalization.Words
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("new_player_rufname_input")
        )

        if (!isTournamentLocalOnly) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onIsLocalPlayerChange(!isLocalPlayer) }
            ) {
                Checkbox(
                    checked = isLocalPlayer,
                    onCheckedChange = onIsLocalPlayerChange
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Nur lokal (nicht mit Server synchronisieren)",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        errorMessage?.let { err ->
            Text(
                text = err,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.testTag("new_player_error_text")
            )
        }
    }
}
