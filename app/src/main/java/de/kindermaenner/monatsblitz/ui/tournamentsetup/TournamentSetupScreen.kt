package de.kindermaenner.monatsblitz.ui.tournamentsetup

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import de.kindermaenner.monatsblitz.domain.model.Player
import de.kindermaenner.monatsblitz.ui.tournamentsetup.components.GameModeDropdownSelector

internal fun matchesPlayerSearch(player: Player, query: String): Boolean {
    val cleanQuery = query.trim().lowercase()
    if (cleanQuery.isEmpty()) return true

    val targets = listOf(player.fullName, player.Vorname, player.Name)

    return targets.any { target ->
        val cleanTarget = target.lowercase()
        cleanTarget.contains(cleanQuery) ||
                cleanTarget.expandUmlauts().contains(cleanQuery.expandUmlauts()) ||
                cleanTarget.simplifyUmlauts().contains(cleanQuery.simplifyUmlauts())
    }
}

private fun String.expandUmlauts(): String = this
    .replace("ä", "ae")
    .replace("ö", "oe")
    .replace("ü", "ue")
    .replace("ß", "ss")

private fun String.simplifyUmlauts(): String = this
    .replace("ä", "a")
    .replace("ö", "o")
    .replace("ü", "u")
    .replace("ß", "s")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TournamentSetupScreen(
    viewModel: TournamentSetupViewModel,
    onNavigateToCrosstable: (Long) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val canStartTournament = state.selectedPlayerIds.size >= 2

    LaunchedEffect(Unit) {
        viewModel.navigationEffect.collect { tournamentId ->
            onNavigateToCrosstable(tournamentId)
        }
    }

    if (state.showAddPlayerDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onShowAddPlayerDialog(false) },
            title = { Text("Neuen Spieler anlegen") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = state.newPlayerVorname,
                        onValueChange = { viewModel.onNewPlayerNameChanged(it, state.newPlayerName) },
                        label = { Text("Vorname") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            capitalization = KeyboardCapitalization.Words
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.newPlayerName,
                        onValueChange = { viewModel.onNewPlayerNameChanged(state.newPlayerVorname, it) },
                        label = { Text("Nachname") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            capitalization = KeyboardCapitalization.Words
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.addNewPlayer() },
                    enabled = state.newPlayerVorname.isNotBlank() && state.newPlayerName.isNotBlank()
                ) {
                    Text("Anlegen")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onShowAddPlayerDialog(false) }) {
                    Text("Abbrechen")
                }
            }
        )
    }

    // --- FILTERED PLAYERS WITH UMLAUT TOLERANCE ---
    val query = state.searchQuery.trim()
    val matchingPlayers = if (query.isEmpty()) {
        state.players
    } else {
        state.players.filter { matchesPlayerSearch(it, query) }
    }

    val selectedMatching = matchingPlayers.filter { it.id in state.selectedPlayerIds }
    val unselectedMatching = matchingPlayers.filter { it.id !in state.selectedPlayerIds }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Neues Turnier") }
            )
        },
        bottomBar = {
            Surface(
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Button(
                        onClick = { viewModel.createTournament() },
                        enabled = canStartTournament,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (canStartTournament) "Turnier starten (${state.selectedPlayerIds.size} Spieler)"
                            else "Mindestens 2 Spieler auswählen"
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // --- EINSTELLUNGEN CARD ---
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Einstellungen",
                        style = MaterialTheme.typography.titleMedium
                    )

                    GameModeDropdownSelector(
                        selectedMode = state.selectedMode,
                        onModeSelected = { mode -> viewModel.onModeChanged(mode) }
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.onDoubleRoundChanged(!state.doubleRound) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Doppelrundig", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "Hin- und Rückrunde spielen",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = state.doubleRound,
                            onCheckedChange = { viewModel.onDoubleRoundChanged(it) }
                        )
                    }
                }
            }

            // --- SPIELER HEADER MIT AKTION ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Teilnehmer",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${state.selectedPlayerIds.size} von ${state.players.size} ausgewählt",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                TextButton(
                    onClick = { viewModel.onShowAddPlayerDialog(true) }
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text("Neuer Spieler")
                }
            }

            // --- SUCHLEISTE MIT UMLAUT-UNTERSTÜTZUNG ---
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                placeholder = { Text("Spieler suchen...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Suche leeren")
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Search
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // --- FILTER-CHIPS & SCHNELLAKTIONEN ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = !state.showOnlySelected,
                    onClick = { viewModel.onShowOnlySelectedChanged(false) },
                    label = { Text("Alle (${state.players.size})") }
                )

                FilterChip(
                    selected = state.showOnlySelected,
                    onClick = { viewModel.onShowOnlySelectedChanged(true) },
                    label = { Text("Ausgewählt (${state.selectedPlayerIds.size})") }
                )

                if (state.selectedPlayerIds.isNotEmpty()) {
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(
                        onClick = { viewModel.clearSelectedPlayers() },
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Text(
                            text = "Alle abwählen",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            HorizontalDivider()

            // --- SPIELERLISTE ---
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (state.showOnlySelected) {
                    if (selectedMatching.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (query.isEmpty()) "Noch keine Spieler ausgewählt."
                                    else "Kein ausgewählter Spieler für '$query' gefunden.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        items(selectedMatching, key = { it.id }) { player ->
                            PlayerRow(
                                name = player.fullName,
                                selected = true,
                                onCheckedChange = { checked ->
                                    viewModel.onPlayerChecked(player.id, checked)
                                }
                            )
                        }
                    }
                } else {
                    if (matchingPlayers.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Kein Spieler gefunden für '$query'.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        // 1. Ausgewählte Spieler oben pinnen
                        if (selectedMatching.isNotEmpty()) {
                            item {
                                Text(
                                    text = "AUSGEWÄHLT (${selectedMatching.size})",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                                )
                            }
                            items(selectedMatching, key = { it.id }) { player ->
                                PlayerRow(
                                    name = player.fullName,
                                    selected = true,
                                    onCheckedChange = { checked ->
                                        viewModel.onPlayerChecked(player.id, checked)
                                    }
                                )
                            }
                        }

                        // 2. Nicht ausgewählte Spieler
                        if (unselectedMatching.isNotEmpty()) {
                            if (selectedMatching.isNotEmpty()) {
                                item {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "WEITERE SPIELER (${unselectedMatching.size})",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                                    )
                                }
                            }
                            items(unselectedMatching, key = { it.id }) { player ->
                                PlayerRow(
                                    name = player.fullName,
                                    selected = false,
                                    onCheckedChange = { checked ->
                                        viewModel.onPlayerChecked(player.id, checked)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerRow(
    name: String,
    selected: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!selected) }
            .padding(vertical = 4.dp, horizontal = 4.dp)
    ) {
        Checkbox(
            checked = selected,
            onCheckedChange = onCheckedChange
        )
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}
