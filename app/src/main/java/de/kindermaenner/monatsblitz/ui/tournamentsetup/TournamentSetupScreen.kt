package de.kindermaenner.monatsblitz.ui.tournamentsetup

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.kindermaenner.monatsblitz.domain.model.GameMode
import de.kindermaenner.monatsblitz.domain.model.Player
import de.kindermaenner.monatsblitz.ui.tournamentsetup.components.GameModeDropdownSelector

internal fun matchesPlayerSearch(player: Player, query: String): Boolean {
    val cleanQuery = query.trim().lowercase()
    if (cleanQuery.isEmpty()) return true

    val targets = listOf(player.fullName, player.vorname, player.name)

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
    onNavigateToSettings: (() -> Unit)? = null,
    onNavigateToCrosstable: (Long) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val canStartTournament = state.selectedPlayerIds.size >= 2

    LaunchedEffect(Unit) {
        viewModel.navigationEffect.collect { tournamentId ->
            onNavigateToCrosstable(tournamentId)
        }
    }

    AddPlayerDialog(
        show = state.showAddPlayerDialog,
        vorname = state.newPlayerVorname,
        name = state.newPlayerName,
        error = state.addPlayerError,
        onNameChanged = viewModel::onNewPlayerNameChanged,
        onConfirm = viewModel::addNewPlayer,
        onDismiss = { viewModel.onShowAddPlayerDialog(false) }
    )

    Scaffold(
        topBar = {
            TournamentSetupTopBar(
                isSyncingPlayers = state.isSyncingPlayers,
                onSyncPlayers = viewModel::syncPlayers,
                onNavigateToSettings = onNavigateToSettings
            )
        },
        bottomBar = {
            TournamentSetupBottomBar(
                selectedCount = state.selectedPlayerIds.size,
                canStartTournament = canStartTournament,
                onStartTournament = viewModel::createTournament
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TournamentSettingsCard(
                selectedMode = state.selectedMode,
                doubleRound = state.doubleRound,
                onModeChanged = viewModel::onModeChanged,
                onDoubleRoundChanged = viewModel::onDoubleRoundChanged
            )

            TournamentPlayerList(
                state = state,
                onShowAddPlayerDialog = { viewModel.onShowAddPlayerDialog(true) },
                onSearchQueryChanged = viewModel::onSearchQueryChanged,
                onShowOnlySelectedChanged = viewModel::onShowOnlySelectedChanged,
                onClearSelectedPlayers = viewModel::clearSelectedPlayers,
                onPlayerChecked = viewModel::onPlayerChecked,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun AddPlayerDialog(
    show: Boolean,
    vorname: String,
    name: String,
    error: String?,
    onNameChanged: (String, String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!show) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Neuen Spieler anlegen") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = vorname,
                    onValueChange = { onNameChanged(it, name) },
                    label = { Text("Vorname") },
                    isError = error != null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        capitalization = KeyboardCapitalization.Words
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { onNameChanged(vorname, it) },
                    label = { Text("Nachname") },
                    isError = error != null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        capitalization = KeyboardCapitalization.Words
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                error?.let { err ->
                    Text(
                        text = err,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = vorname.isNotBlank() && name.isNotBlank()
            ) {
                Text("Anlegen")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TournamentSetupTopBar(
    isSyncingPlayers: Boolean,
    onSyncPlayers: () -> Unit,
    onNavigateToSettings: (() -> Unit)?
) {
    TopAppBar(
        title = { Text("Neues Turnier") },
        actions = {
            if (isSyncingPlayers) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .size(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                IconButton(onClick = onSyncPlayers) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "Spieler synchronisieren"
                    )
                }
            }
            if (onNavigateToSettings != null) {
                IconButton(onClick = onNavigateToSettings) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Einstellungen"
                    )
                }
            }
        }
    )
}

@Composable
private fun TournamentSetupBottomBar(
    selectedCount: Int,
    canStartTournament: Boolean,
    onStartTournament: () -> Unit
) {
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
                onClick = onStartTournament,
                enabled = canStartTournament,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (canStartTournament) "Turnier starten ($selectedCount Spieler)"
                    else "Mindestens 2 Spieler auswählen"
                )
            }
        }
    }
}

@Composable
private fun TournamentSettingsCard(
    selectedMode: GameMode,
    doubleRound: Boolean,
    onModeChanged: (GameMode) -> Unit,
    onDoubleRoundChanged: (Boolean) -> Unit
) {
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
                selectedMode = selectedMode,
                onModeSelected = onModeChanged
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDoubleRoundChanged(!doubleRound) }
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
                    checked = doubleRound,
                    onCheckedChange = onDoubleRoundChanged
                )
            }
        }
    }
}

@Composable
private fun TournamentPlayerList(
    state: TournamentSetupUiState,
    onShowAddPlayerDialog: () -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onShowOnlySelectedChanged: (Boolean) -> Unit,
    onClearSelectedPlayers: () -> Unit,
    onPlayerChecked: (Long, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val query = state.searchQuery.trim()
    val matchingPlayers = if (query.isEmpty()) {
        state.players
    } else {
        state.players.filter { matchesPlayerSearch(it, query) }
    }
    val selectedMatching = matchingPlayers.filter { it.id in state.selectedPlayerIds }
    val unselectedMatching = matchingPlayers.filter { it.id !in state.selectedPlayerIds }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TournamentPlayerHeader(
            selectedCount = state.selectedPlayerIds.size,
            totalCount = state.players.size,
            onShowAddPlayerDialog = onShowAddPlayerDialog
        )

        TournamentPlayerSearchBar(
            query = state.searchQuery,
            onSearchQueryChanged = onSearchQueryChanged
        )

        TournamentPlayerFilterRow(
            showOnlySelected = state.showOnlySelected,
            totalCount = state.players.size,
            selectedCount = state.selectedPlayerIds.size,
            onShowOnlySelectedChanged = onShowOnlySelectedChanged,
            onClearSelectedPlayers = onClearSelectedPlayers
        )

        HorizontalDivider()

        TournamentPlayerListContent(
            state = state,
            selectedMatching = selectedMatching,
            unselectedMatching = unselectedMatching,
            query = query,
            onPlayerChecked = onPlayerChecked,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TournamentPlayerHeader(
    selectedCount: Int,
    totalCount: Int,
    onShowAddPlayerDialog: () -> Unit
) {
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
                text = "$selectedCount von $totalCount ausgewählt",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        TextButton(onClick = onShowAddPlayerDialog) {
            Icon(
                Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.padding(end = 4.dp)
            )
            Text("Neuer Spieler")
        }
    }
}

@Composable
private fun TournamentPlayerSearchBar(
    query: String,
    onSearchQueryChanged: (String) -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onSearchQueryChanged,
        placeholder = { Text("Spieler suchen...") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onSearchQueryChanged("") }) {
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
}

@Composable
private fun TournamentPlayerFilterRow(
    showOnlySelected: Boolean,
    totalCount: Int,
    selectedCount: Int,
    onShowOnlySelectedChanged: (Boolean) -> Unit,
    onClearSelectedPlayers: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterChip(
            selected = !showOnlySelected,
            onClick = { onShowOnlySelectedChanged(false) },
            label = { Text("Alle ($totalCount)") }
        )

        FilterChip(
            selected = showOnlySelected,
            onClick = { onShowOnlySelectedChanged(true) },
            label = { Text("Ausgewählt ($selectedCount)") }
        )

        if (selectedCount > 0) {
            Spacer(modifier = Modifier.weight(1f))
            TextButton(
                onClick = onClearSelectedPlayers,
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
}

@Composable
private fun TournamentPlayerListContent(
    state: TournamentSetupUiState,
    selectedMatching: List<Player>,
    unselectedMatching: List<Player>,
    query: String,
    onPlayerChecked: (Long, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    if (state.isLoading && state.players.isEmpty()) {
        Box(
            modifier = modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            if (state.showOnlySelected) {
                renderSelectedOnlyPlayers(
                    selectedMatching = selectedMatching,
                    query = query,
                    showDetailedSyncIndicators = state.showDetailedSyncIndicators,
                    onPlayerChecked = onPlayerChecked
                )
            } else {
                renderAllPlayers(
                    selectedMatching = selectedMatching,
                    unselectedMatching = unselectedMatching,
                    query = query,
                    showDetailedSyncIndicators = state.showDetailedSyncIndicators,
                    onPlayerChecked = onPlayerChecked
                )
            }
        }
    }
}


private fun LazyListScope.renderSelectedOnlyPlayers(
    selectedMatching: List<Player>,
    query: String,
    showDetailedSyncIndicators: Boolean,
    onPlayerChecked: (Long, Boolean) -> Unit
) {
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
                player = player,
                selected = true,
                showDirtyIndicator = showDetailedSyncIndicators,
                onCheckedChange = { checked -> onPlayerChecked(player.id, checked) }
            )
        }
    }
}

private fun LazyListScope.renderAllPlayers(
    selectedMatching: List<Player>,
    unselectedMatching: List<Player>,
    query: String,
    showDetailedSyncIndicators: Boolean,
    onPlayerChecked: (Long, Boolean) -> Unit
) {
    if (selectedMatching.isEmpty() && unselectedMatching.isEmpty()) {
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
                    player = player,
                    selected = true,
                    showDirtyIndicator = showDetailedSyncIndicators,
                    onCheckedChange = { checked -> onPlayerChecked(player.id, checked) }
                )
            }
        }

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
                    player = player,
                    selected = false,
                    showDirtyIndicator = showDetailedSyncIndicators,
                    onCheckedChange = { checked -> onPlayerChecked(player.id, checked) }
                )
            }
        }
    }
}

@Composable
private fun PlayerRow(
    player: Player,
    selected: Boolean,
    showDirtyIndicator: Boolean,
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
            text = player.fullName,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.padding(start = 8.dp)
        )
        if (showDirtyIndicator && player.dirty) {
            Spacer(modifier = Modifier.size(6.dp))
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(color = MaterialTheme.colorScheme.tertiary, shape = CircleShape)
            )
        }
    }
}
