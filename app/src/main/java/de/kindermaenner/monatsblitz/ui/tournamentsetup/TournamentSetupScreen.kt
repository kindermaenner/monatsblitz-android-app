package de.kindermaenner.monatsblitz.ui.tournamentsetup

import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.kindermaenner.monatsblitz.domain.model.GameMode
import de.kindermaenner.monatsblitz.domain.model.Player
import de.kindermaenner.monatsblitz.domain.model.PlayerFilterMode
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
    val playerListState = rememberLazyListState()

    LaunchedEffect(playerListState) {
        snapshotFlow { playerListState.isScrollInProgress }
            .collect { isScrolling ->
                if (isScrolling) {
                    isSettingsExpanded = false
                }
            }
    }

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
        state = AddModeDialogState(
            show = state.showAddModeDialog,
            modeName = state.newModeName,
            isPermanent = state.newModePermanent,
            error = state.addModeError
        ),
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
        val context = androidx.compose.ui.platform.LocalContext.current
        AlertDialog(
            onDismissRequest = viewModel::dismissRemoteTournamentDialog,
            title = { Text("Fehler") },
            text = { Text(err) },
            confirmButton = {
                Button(onClick = viewModel::dismissRemoteTournamentDialog) {
                    Text("OK")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        de.kindermaenner.monatsblitz.ui.util.ErrorReportSender.sendErrorReport(
                            context = context,
                            actionTitle = "Offenes Turnier fortsetzen",
                            errorMessage = err
                        )
                    }
                ) {
                    Text("Fehler melden")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TournamentSetupTopBar(
                isSyncingPlayers = state.isSyncingPlayers,
                showSyncButton = state.isServerConfigured && !state.isStandaloneOffline,
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
                state = TournamentSettingsState(
                    isExpanded = isSettingsExpanded,
                    selectedDate = state.selectedDate,
                    selectedMode = state.selectedMode,
                    availableModes = state.availableModes,
                    doubleRound = state.doubleRound,
                    isLocalOnly = state.isLocalOnly,
                    isServerConfigured = state.isServerConfigured,
                    isStandaloneOffline = state.isStandaloneOffline
                ),
                actions = TournamentSettingsActions(
                    onToggleExpanded = { isSettingsExpanded = !isSettingsExpanded },
                    onDateChanged = viewModel::onDateChanged,
                    onModeChanged = viewModel::onModeChanged,
                    onAddNewMode = { viewModel.onShowAddModeDialog(true) },
                    onDoubleRoundChanged = viewModel::onDoubleRoundChanged,
                    onLocalOnlyChanged = viewModel::onLocalOnlyChanged
                )
            )

            TournamentPlayerList(
                state = state,
                listState = playerListState,
                actions = TournamentPlayerListActions(
                    onShowAddPlayerDialog = { viewModel.onShowAddPlayerDialog(true) },
                    onSearchQueryChanged = viewModel::onSearchQueryChanged,
                    onSearchFocused = { isSettingsExpanded = false },
                    onFilterModeChanged = viewModel::setPlayerFilterMode,
                    onShowOnlySelectedChanged = viewModel::onShowOnlySelectedChanged,
                    onSelectSortOrder = viewModel::setSortOrder,
                    onClearSelectedPlayers = viewModel::clearSelectedPlayers,
                    onPlayerChecked = { playerId, checked ->
                        if (checked && state.selectedPlayerIds.isEmpty()) {
                            isSettingsExpanded = false
                        }
                        viewModel.onPlayerChecked(playerId, checked)
                    }
                ),
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

data class AddModeDialogState(
    val show: Boolean,
    val modeName: String,
    val isPermanent: Boolean,
    val error: String?
)

@Composable
private fun AddModeDialog(
    state: AddModeDialogState,
    onModeNameChanged: (String) -> Unit,
    onPermanentChanged: (Boolean) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!state.show) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Neuen Modus anlegen") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = state.modeName,
                    onValueChange = onModeNameChanged,
                    label = { Text("Name des Modus") },
                    placeholder = { Text("z. B. 10+0 oder Jubiläumsblitz") },
                    isError = state.error != null,
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
                        .clickable { onPermanentChanged(!state.isPermanent) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = state.isPermanent,
                        onCheckedChange = onPermanentChanged
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Dauerhaft in Auswahlliste speichern",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = if (state.isPermanent) "Wird auf dem Server gesichert und bleibt für zukünftige Turniere erhalten" else "Nur einmalig (One-Shot) für dieses Turnier nutzen",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                state.error?.let { err ->
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
                enabled = state.modeName.isNotBlank()
            ) {
                Text(if (state.isPermanent) "Weiter" else "Anlegen")
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
    showSyncButton: Boolean = true,
    onSyncPlayers: () -> Unit,
    onNavigateToSettings: (() -> Unit)?
) {
    TopAppBar(
        title = { Text("Neues Turnier") },
        actions = {
            if (showSyncButton) {
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
    Surface(
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Button(
                onClick = onStartTournament,
                enabled = canStartTournament,
                modifier = Modifier.fillMaxWidth()
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
}

data class TournamentSettingsState(
    val isExpanded: Boolean,
    val selectedDate: LocalDate,
    val selectedMode: GameMode?,
    val availableModes: List<GameMode> = emptyList(),
    val doubleRound: Boolean = false,
    val isLocalOnly: Boolean = false,
    val isServerConfigured: Boolean = true,
    val isStandaloneOffline: Boolean = false
)

data class TournamentSettingsActions(
    val onToggleExpanded: () -> Unit,
    val onDateChanged: (LocalDate) -> Unit,
    val onModeChanged: (GameMode) -> Unit,
    val onAddNewMode: (() -> Unit)? = null,
    val onDoubleRoundChanged: (Boolean) -> Unit,
    val onLocalOnlyChanged: (Boolean) -> Unit = {}
)

@Composable
internal fun TournamentSettingsCard(
    state: TournamentSettingsState,
    actions: TournamentSettingsActions
) {
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        TournamentDatePickerDialog(
            initialDate = state.selectedDate,
            onDateSelected = actions.onDateChanged,
            onDismiss = { showDatePicker = false }
        )
    }

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        val isToday = state.selectedDate == LocalDate.now()
        val formattedDate = state.selectedDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
        val roundLabel = if (state.doubleRound) "Doppelrunde" else "Einfachrunde"
        val modeLabel = state.selectedMode?.displayName ?: "Kein Modus"

        if (!state.isExpanded) {
            TournamentSettingsCollapsedRow(
                isToday = isToday,
                formattedDate = formattedDate,
                modeLabel = modeLabel,
                roundLabel = roundLabel,
                onToggleExpanded = actions.onToggleExpanded
            )
        } else {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TournamentSettingsExpandedHeader(
                    onToggleExpanded = actions.onToggleExpanded
                )

                TournamentDateRow(
                    isToday = isToday,
                    formattedDate = formattedDate,
                    onOpenDatePicker = { showDatePicker = true }
                )

                GameModeDropdownSelector(
                    selectedMode = state.selectedMode,
                    availableModes = state.availableModes,
                    onModeSelected = actions.onModeChanged,
                    onAddNewMode = actions.onAddNewMode
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { actions.onDoubleRoundChanged(!state.doubleRound) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Doppelrundig", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = state.doubleRound,
                        onCheckedChange = actions.onDoubleRoundChanged
                    )
                }

                TournamentSettingsServerOption(
                    isServerConfigured = state.isServerConfigured,
                    isStandaloneOffline = state.isStandaloneOffline,
                    isLocalOnly = state.isLocalOnly,
                    onLocalOnlyChanged = actions.onLocalOnlyChanged
                )
            }
        }
    }
}

@Composable
private fun TournamentSettingsCollapsedRow(
    isToday: Boolean,
    formattedDate: String,
    modeLabel: String,
    roundLabel: String,
    onToggleExpanded: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleExpanded() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "Turnier einrichten",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${if (isToday) "Heute" else formattedDate} • $modeLabel • $roundLabel",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = "Einstellungen aufklappen",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TournamentSettingsExpandedHeader(
    onToggleExpanded: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleExpanded() }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Turnier einrichten",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Icon(
            imageVector = Icons.Default.KeyboardArrowUp,
            contentDescription = "Einstellungen einklappen",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun TournamentDateRow(
    isToday: Boolean,
    formattedDate: String,
    onOpenDatePicker: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenDatePicker() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = "Turnierdatum",
                tint = if (!isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Turnierdatum",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Surface(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
            color = if (!isToday) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (!isToday) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
        ) {
            Text(
                text = if (isToday) "Heute ($formattedDate)" else formattedDate,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun TournamentSettingsServerOption(
    isServerConfigured: Boolean,
    isStandaloneOffline: Boolean,
    isLocalOnly: Boolean,
    onLocalOnlyChanged: (Boolean) -> Unit
) {
    if (isServerConfigured && !isStandaloneOffline) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onLocalOnlyChanged(!isLocalOnly) },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Nur lokal austragen", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Nicht auf Website veröffentlichen",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = isLocalOnly,
                onCheckedChange = onLocalOnlyChanged,
                modifier = Modifier.testTag("tournament_local_only_switch")
            )
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

data class TournamentPlayerListActions(
    val onShowAddPlayerDialog: () -> Unit,
    val onSearchQueryChanged: (String) -> Unit,
    val onSearchFocused: () -> Unit,
    val onFilterModeChanged: (PlayerFilterMode) -> Unit,
    val onShowOnlySelectedChanged: (Boolean) -> Unit,
    val onSelectSortOrder: (PlayerSortOrder) -> Unit,
    val onClearSelectedPlayers: () -> Unit,
    val onPlayerChecked: (Long, Boolean) -> Unit
)

@Composable
private fun TournamentPlayerList(
    state: TournamentSetupUiState,
    actions: TournamentPlayerListActions,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState()
) {
    val query = state.searchQuery.trim()
    val cutoffDate = remember(state.activeCutoffMonths) {
        LocalDate.now().minusMonths(state.activeCutoffMonths.toLong())
    }
    val activeCount = remember(state.players, cutoffDate) {
        state.players.count { it.isActive(cutoffDate) }
    }

    val matchingPlayers = if (query.isNotEmpty()) {
        state.players.filter { matchesPlayerSearch(it, query) }
    } else {
        when (state.playerFilterMode) {
            PlayerFilterMode.ACTIVE -> state.players.filter { it.isActive(cutoffDate) }
            PlayerFilterMode.SELECTED -> state.players.filter { it.id in state.selectedPlayerIds }
            PlayerFilterMode.ALL -> state.players
        }
    }
    val selectedMatching = matchingPlayers.filter { it.id in state.selectedPlayerIds }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TournamentPlayerHeader(
            selectedCount = state.selectedPlayerIds.size,
            totalCount = state.players.size,
            onShowAddPlayerDialog = actions.onShowAddPlayerDialog
        )

        TournamentPlayerSearchBar(
            query = state.searchQuery,
            onSearchQueryChanged = actions.onSearchQueryChanged,
            onSearchFocused = actions.onSearchFocused
        )

        TournamentPlayerFilterRow(
            filterMode = state.playerFilterMode,
            activeCount = activeCount,
            totalCount = state.players.size,
            selectedCount = state.selectedPlayerIds.size,
            sortOrder = state.sortOrder,
            onFilterModeChanged = actions.onFilterModeChanged,
            onSelectSortOrder = actions.onSelectSortOrder,
            onClearSelectedPlayers = actions.onClearSelectedPlayers
        )

        HorizontalDivider()

        TournamentPlayerListContent(
            state = state,
            matchingPlayers = matchingPlayers,
            selectedMatching = selectedMatching,
            query = query,
            listState = listState,
            onPlayerChecked = actions.onPlayerChecked,
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
    filterMode: PlayerFilterMode,
    activeCount: Int,
    totalCount: Int,
    selectedCount: Int,
    sortOrder: PlayerSortOrder,
    onFilterModeChanged: (PlayerFilterMode) -> Unit,
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
            selected = filterMode == PlayerFilterMode.ACTIVE,
            onClick = { onFilterModeChanged(PlayerFilterMode.ACTIVE) },
            label = { Text("Aktive ($activeCount)") }
        )

        FilterChip(
            selected = filterMode == PlayerFilterMode.SELECTED,
            onClick = {
                onFilterModeChanged(
                    if (filterMode == PlayerFilterMode.SELECTED) PlayerFilterMode.ACTIVE
                    else PlayerFilterMode.SELECTED
                )
            },
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
                    text = { Text("Alle Spieler anzeigen ($totalCount)") },
                    leadingIcon = {
                        if (filterMode == PlayerFilterMode.ALL) {
                            Icon(Icons.Default.Check, contentDescription = null)
                        }
                    },
                    onClick = {
                        onFilterModeChanged(PlayerFilterMode.ALL)
                        menuExpanded = false
                    }
                )
                HorizontalDivider()
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
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState()
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
            state = listState,
            modifier = modifier
                .fillMaxWidth()
                .testTag("tournament_player_list"),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            if (state.playerFilterMode == PlayerFilterMode.SELECTED && query.isEmpty()) {
                renderSelectedOnlyPlayers(
                    selectedMatching = selectedMatching,
                    query = query,
                    showDetailedSyncIndicators = state.showDetailedSyncIndicators,
                    sortField = state.sortField,
                    onPlayerChecked = onPlayerChecked
                )
            } else {
                val emptyMessage = when {
                    query.isNotEmpty() -> "Kein Spieler gefunden für '$query'."
                    state.playerFilterMode == PlayerFilterMode.ACTIVE -> "Keine aktiven Spieler gefunden."
                    else -> "Noch keine Spieler vorhanden."
                }
                renderAllPlayers(
                    players = matchingPlayers,
                    selectedPlayerIds = state.selectedPlayerIds,
                    emptyMessage = emptyMessage,
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
    emptyMessage: String,
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
                    text = emptyMessage,
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
    sortField: PlayerSortField = PlayerSortField.FIRST_NAME,
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
        if (player.isLocal) {
            Spacer(modifier = Modifier.width(6.dp))
            Surface(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                Text(
                    text = "Lokal",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }
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
