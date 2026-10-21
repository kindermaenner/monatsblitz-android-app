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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.kindermaenner.monatsblitz.domain.model.GameMode
import de.kindermaenner.monatsblitz.domain.model.Player
import de.kindermaenner.monatsblitz.domain.model.PlayerSortField
import de.kindermaenner.monatsblitz.domain.model.PlayerSortOrder
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
    val canStartTournament = state.selectedPlayerIds.size >= 2 && state.selectedMode != null
    var isSettingsExpanded by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        viewModel.navigationEffect.collect { tournamentId ->
            onNavigateToCrosstable(tournamentId)
        }
    }

    LaunchedEffect(state.searchQuery) {
        if (state.searchQuery.isNotBlank()) {
            isSettingsExpanded = false
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

    AddModeDialog(
        show = state.showAddModeDialog,
        modeName = state.newModeName,
        isPermanent = state.newModePermanent,
        error = state.addModeError,
        onModeNameChanged = viewModel::onNewModeNameChanged,
        onPermanentChanged = viewModel::onNewModePermanentChanged,
        onConfirm = viewModel::onConfirmAddMode,
        onDismiss = { viewModel.onShowAddModeDialog(false) }
    )

    if (state.showConfirmAddModeDialog) {
        AlertDialog(
            onDismissRequest = viewModel::onDismissConfirmAddMode,
            title = { Text("Modus anlegen") },
            text = {
                Text("Soll der Modus '${state.newModeName.trim()}' neu angelegt und mit dem Server synchronisiert werden?")
            },
            confirmButton = {
                Button(onClick = { viewModel.executeAddMode(permanent = true) }) {
                    Text("Ja, anlegen")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = viewModel::onDismissConfirmAddMode) {
                    Text("Abbrechen")
                }
            }
        )
    }

    state.remoteOpenTournament?.let { t ->
        val roundsText = if (t.roundCount == 2) "Doppelrunde" else "1 Runde"
        val playerText = if (t.playerCount == 1) "1 Teilnehmer" else "${t.playerCount} Teilnehmer"
        AlertDialog(
            onDismissRequest = viewModel::dismissRemoteTournamentDialog,
            title = { Text("Offenes Turnier gefunden") },
            text = {
                Text(
                    "Auf dem Server wurde ein offenes Turnier gefunden:\n\n" +
                    "• Datum: ${t.date}\n" +
                    "• Modus: ${t.mode} ($roundsText)\n" +
                    "• Teilnehmer: $playerText\n\n" +
                    "Möchtest du dieses Turnier auf diesem Gerät fortsetzen?"
                )
            },
            confirmButton = {
                Button(onClick = viewModel::adoptRemoteTournament) {
                    Text("Fortsetzen")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = viewModel::dismissRemoteTournamentDialog) {
                    Text("Neues Turnier")
                }
            }
        )
    }

    state.adoptError?.let { err ->
        AlertDialog(
            onDismissRequest = viewModel::dismissRemoteTournamentDialog,
            title = { Text("Fehler") },
            text = { Text(err) },
            confirmButton = {
                Button(onClick = viewModel::dismissRemoteTournamentDialog) {
                    Text("OK")
                }
            }
        )
    }

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
                hasSelectedMode = state.selectedMode != null,
                canStartTournament = canStartTournament,
                onStartTournament = viewModel::createTournament
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TournamentSettingsCard(
                isExpanded = isSettingsExpanded,
                onToggleExpanded = { isSettingsExpanded = !isSettingsExpanded },
                selectedDate = state.selectedDate,
                selectedMode = state.selectedMode,
                availableModes = state.availableModes,
                doubleRound = state.doubleRound,
                onDateChanged = viewModel::onDateChanged,
                onModeChanged = viewModel::onModeChanged,
                onAddNewMode = { viewModel.onShowAddModeDialog(true) },
                onDoubleRoundChanged = viewModel::onDoubleRoundChanged
            )

            TournamentPlayerList(
                state = state,
                onShowAddPlayerDialog = { viewModel.onShowAddPlayerDialog(true) },
                onSearchQueryChanged = viewModel::onSearchQueryChanged,
                onSearchFocused = { isSettingsExpanded = false },
                onShowOnlySelectedChanged = viewModel::onShowOnlySelectedChanged,
                onSelectSortOrder = viewModel::setSortOrder,
                onClearSelectedPlayers = viewModel::clearSelectedPlayers,
                onPlayerChecked = { playerId, checked ->
                    if (checked && state.selectedPlayerIds.isEmpty()) {
                        isSettingsExpanded = false
                    }
                    viewModel.onPlayerChecked(playerId, checked)
                },
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

@Composable
private fun AddModeDialog(
    show: Boolean,
    modeName: String,
    isPermanent: Boolean,
    error: String?,
    onModeNameChanged: (String) -> Unit,
    onPermanentChanged: (Boolean) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!show) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Neuen Modus anlegen") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = modeName,
                    onValueChange = onModeNameChanged,
                    label = { Text("Name des Modus") },
                    placeholder = { Text("z. B. 10+0 oder Jubiläumsblitz") },
                    isError = error != null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        capitalization = KeyboardCapitalization.Words
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPermanentChanged(!isPermanent) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isPermanent,
                        onCheckedChange = onPermanentChanged
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Dauerhaft in Auswahlliste speichern",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = if (isPermanent) "Wird auf dem Server gesichert und bleibt für zukünftige Turniere erhalten" else "Nur einmalig (One-Shot) für dieses Turnier nutzen",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

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
                enabled = modeName.isNotBlank()
            ) {
                Text(if (isPermanent) "Weiter" else "Anlegen")
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
    hasSelectedMode: Boolean,
    canStartTournament: Boolean,
    onStartTournament: () -> Unit
) {
    BottomAppBar(
        modifier = Modifier.fillMaxWidth()
    ) {
        Button(
            onClick = onStartTournament,
            enabled = canStartTournament,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            val label = when {
                !hasSelectedMode -> "Bitte Spielmodus auswählen"
                selectedCount < 2 -> "Mindestens 2 Spieler auswählen"
                else -> "Turnier starten ($selectedCount Spieler)"
            }
            Text(label)
        }
    }
}

@Composable
internal fun TournamentSettingsCard(
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    selectedDate: LocalDate,
    selectedMode: GameMode?,
    availableModes: List<GameMode> = GameMode.DEFAULT_MODES,
    doubleRound: Boolean,
    onDateChanged: (LocalDate) -> Unit,
    onModeChanged: (GameMode) -> Unit,
    onAddNewMode: (() -> Unit)? = null,
    onDoubleRoundChanged: (Boolean) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        TournamentDatePickerDialog(
            initialDate = selectedDate,
            onDateSelected = onDateChanged,
            onDismiss = { showDatePicker = false }
        )
    }

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        val isToday = selectedDate == LocalDate.now()
        val formattedDate = selectedDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
        val dateLabel = if (isToday) "Heute ($formattedDate)" else formattedDate
        val roundLabel = if (doubleRound) "Doppelrunde" else "Einfachrunde"
        val modeLabel = selectedMode?.displayName ?: "Kein Modus"

        if (!isExpanded) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpanded() }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Einstellungen",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "(${if (isToday) "Heute" else formattedDate} • $modeLabel • $roundLabel)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Einstellungen aufklappen",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onToggleExpanded() }
                    ) {
                        Text(
                            text = "Einstellungen",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Einstellungen einklappen",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .padding(start = 4.dp)
                                .size(24.dp)
                        )
                    }
                    AssistChip(
                        onClick = { showDatePicker = true },
                        label = {
                            Text(
                                text = dateLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (!isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "Turnierdatum",
                                tint = if (!isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }

                GameModeDropdownSelector(
                    selectedMode = selectedMode,
                    availableModes = availableModes,
                    onModeSelected = onModeChanged,
                    onAddNewMode = onAddNewMode
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDoubleRoundChanged(!doubleRound) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Doppelrundig", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = doubleRound,
                        onCheckedChange = onDoubleRoundChanged
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TournamentDatePickerDialog(
    initialDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        onDateSelected(date)
                    }
                    onDismiss()
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@Composable
private fun TournamentPlayerList(
    state: TournamentSetupUiState,
    onShowAddPlayerDialog: () -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onSearchFocused: () -> Unit,
    onShowOnlySelectedChanged: (Boolean) -> Unit,
    onSelectSortOrder: (PlayerSortOrder) -> Unit,
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

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TournamentPlayerHeader(
            selectedCount = state.selectedPlayerIds.size,
            totalCount = state.players.size,
            onShowAddPlayerDialog = onShowAddPlayerDialog
        )

        TournamentPlayerSearchBar(
            query = state.searchQuery,
            onSearchQueryChanged = onSearchQueryChanged,
            onSearchFocused = onSearchFocused
        )

        TournamentPlayerFilterRow(
            showOnlySelected = state.showOnlySelected,
            totalCount = state.players.size,
            selectedCount = state.selectedPlayerIds.size,
            sortOrder = state.sortOrder,
            onShowOnlySelectedChanged = onShowOnlySelectedChanged,
            onSelectSortOrder = onSelectSortOrder,
            onClearSelectedPlayers = onClearSelectedPlayers
        )

        HorizontalDivider()

        TournamentPlayerListContent(
            state = state,
            matchingPlayers = matchingPlayers,
            selectedMatching = selectedMatching,
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
    onSearchQueryChanged: (String) -> Unit,
    onSearchFocused: () -> Unit
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
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { focusState ->
                if (focusState.isFocused) {
                    onSearchFocused()
                }
            }
    )
}

@Composable
private fun TournamentPlayerFilterRow(
    showOnlySelected: Boolean,
    totalCount: Int,
    selectedCount: Int,
    sortOrder: PlayerSortOrder,
    onShowOnlySelectedChanged: (Boolean) -> Unit,
    onSelectSortOrder: (PlayerSortOrder) -> Unit,
    onClearSelectedPlayers: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
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

        Spacer(modifier = Modifier.weight(1f))

        Box {
            IconButton(
                onClick = { menuExpanded = true }
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Weitere Optionen"
                )
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Sortierung A➔Z") },
                    leadingIcon = {
                        if (sortOrder == PlayerSortOrder.ASCENDING) {
                            Icon(Icons.Default.Check, contentDescription = null)
                        }
                    },
                    onClick = {
                        onSelectSortOrder(PlayerSortOrder.ASCENDING)
                        menuExpanded = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Sortierung Z➔A") },
                    leadingIcon = {
                        if (sortOrder == PlayerSortOrder.DESCENDING) {
                            Icon(Icons.Default.Check, contentDescription = null)
                        }
                    },
                    onClick = {
                        onSelectSortOrder(PlayerSortOrder.DESCENDING)
                        menuExpanded = false
                    }
                )
                if (selectedCount > 0) {
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "Alle abwählen",
                                color = MaterialTheme.colorScheme.error
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = {
                            onClearSelectedPlayers()
                            menuExpanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TournamentPlayerListContent(
    state: TournamentSetupUiState,
    matchingPlayers: List<Player>,
    selectedMatching: List<Player>,
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
                    sortField = state.sortField,
                    onPlayerChecked = onPlayerChecked
                )
            } else {
                renderAllPlayers(
                    players = matchingPlayers,
                    selectedPlayerIds = state.selectedPlayerIds,
                    query = query,
                    showDetailedSyncIndicators = state.showDetailedSyncIndicators,
                    sortField = state.sortField,
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
    sortField: PlayerSortField,
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
                sortField = sortField,
                onCheckedChange = { checked -> onPlayerChecked(player.id, checked) }
            )
        }
    }
}

private fun LazyListScope.renderAllPlayers(
    players: List<Player>,
    selectedPlayerIds: Set<Long>,
    query: String,
    showDetailedSyncIndicators: Boolean,
    sortField: PlayerSortField,
    onPlayerChecked: (Long, Boolean) -> Unit
) {
    if (players.isEmpty()) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (query.isEmpty()) "Noch keine Spieler vorhanden." else "Kein Spieler gefunden für '$query'.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        items(players, key = { it.id }) { player ->
            PlayerRow(
                player = player,
                selected = player.id in selectedPlayerIds,
                showDirtyIndicator = showDetailedSyncIndicators,
                sortField = sortField,
                onCheckedChange = { checked -> onPlayerChecked(player.id, checked) }
            )
        }
    }
}

@Composable
private fun PlayerRow(
    player: Player,
    selected: Boolean,
    showDirtyIndicator: Boolean,
    sortField: PlayerSortField = PlayerSortField.LAST_NAME,
    onCheckedChange: (Boolean) -> Unit
) {
    val displayName = if (sortField == PlayerSortField.LAST_NAME && player.name.isNotBlank() && player.vorname.isNotBlank()) {
        "${player.name}, ${player.vorname}"
    } else {
        player.fullName
    }

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
            text = displayName,
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
