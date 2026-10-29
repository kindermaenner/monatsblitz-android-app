package de.kindermaenner.monatsblitz.ui.crosstable

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Surface
import androidx.compose.ui.platform.testTag
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.RadioButton
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import de.kindermaenner.monatsblitz.domain.model.GameResult
import de.kindermaenner.monatsblitz.domain.model.PlayerWithdrawRule
import de.kindermaenner.monatsblitz.domain.model.SyncStatus
import de.kindermaenner.monatsblitz.domain.model.Tournament
import de.kindermaenner.monatsblitz.ui.crosstable.components.CrosstableHeader
import de.kindermaenner.monatsblitz.ui.crosstable.components.CrosstableRow
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrosstableScreen(
    viewModel: CrosstableViewModel,
    onNavigateToRanking: (Long) -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onBackToSetup: () -> Unit
) {
    val horizontalScrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var showBackDialog by remember { mutableStateOf(false) }
    var playerToWithdraw by remember { mutableStateOf<Triple<Long, String, Boolean>?>(null) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.syncMessageEvent.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val handleBackNavigation = {
        if (state.syncStatus == SyncStatus.FINALIZED) {
            onBackToSetup()
        } else {
            showBackDialog = true
        }
    }

    BackHandler {
        handleBackNavigation()
    }

    CrosstableCancelDialog(
        show = showBackDialog,
        onConfirm = {
            showBackDialog = false
            onBackToSetup()
        },
        onDismiss = { showBackDialog = false }
    )

    PlayerWithdrawDialog(
        playerInfo = playerToWithdraw,
        onConfirm = { playerId, rule ->
            viewModel.withdrawPlayer(playerId, rule)
            playerToWithdraw = null
        },
        onDismiss = { playerToWithdraw = null }
    )

    val tournament = state.tournament
    val allGamesPlayed = tournament != null &&
        tournament.games.isNotEmpty() &&
        tournament.games.values.none { it.result == GameResult.Open }
    val isFinalized = state.syncStatus == SyncStatus.FINALIZED

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CrosstableTopBar(
                tournament = tournament,
                syncStatus = state.syncStatus,
                onBackClick = handleBackNavigation,
                onTriggerSync = { viewModel.triggerManualSync() },
                onShowSyncedSnackbar = {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Bereits synchronisiert")
                    }
                },
                onNavigateToSettings = onNavigateToSettings,
                onNavigateToRanking = { tournamentId ->
                    viewModel.prepareRankingsAndNavigate {
                        onNavigateToRanking(tournamentId)
                    }
                }
            )
        },
        bottomBar = {
            if (allGamesPlayed && !isFinalized) {
                Surface(
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Alle Partien gespielt!",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "Turnier jetzt abschließen & Rangliste anzeigen",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.prepareRankingsAndNavigate {
                                    onNavigateToRanking(tournament.id)
                                }
                            },
                            modifier = Modifier.testTag("crosstable_finish_tournament_button")
                        ) {
                            Text("Abschließen")
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (tournament == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            CrosstableTableContent(
                tournament = tournament,
                state = state,
                padding = padding,
                horizontalScrollState = horizontalScrollState,
                onSelectLeg = viewModel::selectLeg,
                onSetResult = viewModel::setResult,
                onPlayerNameClick = { playerId, playerName, isWithdrawn ->
                    playerToWithdraw = Triple(playerId, playerName, isWithdrawn)
                }
            )
        }
    }
}

@Composable
private fun CrosstableCancelDialog(
    show: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!show) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Turnier abbrechen?") },
        text = { Text("Das aktuelle Turnier wird beendet.") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Ja, abbrechen")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Nein, weitermachen")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CrosstableTopBar(
    tournament: Tournament?,
    syncStatus: SyncStatus,
    onBackClick: () -> Unit,
    onTriggerSync: () -> Unit,
    onShowSyncedSnackbar: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToRanking: (Long) -> Unit
) {
    TopAppBar(
        title = {
            if (tournament != null) {
                Text(
                    text = "${tournament.date.format(DateTimeFormatter.ofPattern("dd.MM.yy"))} • ${tournament.mode.displayName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Turnier abbrechen"
                )
            }
        },
        actions = {
            if (tournament?.isLocalOnly != true) {
                SyncStatusAction(
                    syncStatus = syncStatus,
                    onTriggerSync = onTriggerSync,
                    onShowSyncedSnackbar = onShowSyncedSnackbar
                )
            }

            IconButton(onClick = onNavigateToSettings) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Einstellungen"
                )
            }

            if (tournament != null) {
                FilledTonalButton(
                    onClick = { onNavigateToRanking(tournament.id) },
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Icon(
                        Icons.Default.Leaderboard,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text("Rangliste")
                }
            }
        }
    )
}

@Composable
private fun SyncStatusAction(
    syncStatus: SyncStatus,
    onTriggerSync: () -> Unit,
    onShowSyncedSnackbar: () -> Unit
) {
    when (syncStatus) {
        SyncStatus.SYNCING -> {
            Box(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .size(32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
            }
        }
        SyncStatus.FAILED -> {
            IconButton(onClick = onTriggerSync) {
                Icon(
                    imageVector = Icons.Default.SyncProblem,
                    contentDescription = "Sync fehlgeschlagen. Tippen zum Synchronisieren.",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
        SyncStatus.SYNCED -> {
            IconButton(onClick = onShowSyncedSnackbar) {
                Icon(
                    imageVector = Icons.Default.CloudDone,
                    contentDescription = "Alles synchronisiert",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        SyncStatus.FINALIZED -> {
            Box(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .size(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Turnier abgeschlossen",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CrosstableTableContent(
    tournament: Tournament,
    state: CrosstableUiState,
    padding: PaddingValues,
    horizontalScrollState: ScrollState,
    onSelectLeg: (Int) -> Unit,
    onSetResult: (rowIndex: Int, columnIndex: Int, round: Int, result: GameResult) -> Unit,
    onPlayerNameClick: (playerId: Long, playerName: String, isWithdrawn: Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        if (tournament.rounds > 1) {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                repeat(tournament.rounds) { index ->
                    SegmentedButton(
                        selected = state.leg - 1 == index,
                        onClick = { onSelectLeg(index + 1) },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Runde ${index + 1}")
                    }
                }
            }
        }

        CrosstableHeader(
            tournament = tournament,
            horizontalScrollState = horizontalScrollState
        )

        HorizontalDivider()

        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            items(state.rows, key = { it.rowIndex }) { rowData ->
                CrosstableRow(
                    tournament = tournament,
                    round = state.leg,
                    rowData = rowData,
                    allPlayerNames = state.rows.map { it.playerName },
                    horizontalScrollState = horizontalScrollState,
                    onPlayerNameClick = onPlayerNameClick,
                    setResult = onSetResult
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun PlayerWithdrawDialog(
    playerInfo: Triple<Long, String, Boolean>?,
    onConfirm: (Long, PlayerWithdrawRule) -> Unit,
    onDismiss: () -> Unit
) {
    if (playerInfo == null) return
    val (playerId, playerName, isWithdrawn) = playerInfo

    if (isWithdrawn) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(playerName) },
            text = { Text("$playerName hat das Turnier bereits vorzeitig beendet (z).") },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("Schließen")
                }
            }
        )
        return
    }

    var selectedRule by remember { mutableStateOf(PlayerWithdrawRule.YOUTH_DRAW) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Turnier vorzeitig beenden") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "$playerName verlässt das Turnier vorzeitig. Wie sollen alle noch ungespielten Partien gewertet werden?",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))

                Column(modifier = Modifier.selectableGroup()) {
                    // Option 1: Jugendregel
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (selectedRule == PlayerWithdrawRule.YOUTH_DRAW),
                                onClick = { selectedRule = PlayerWithdrawRule.YOUTH_DRAW },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (selectedRule == PlayerWithdrawRule.YOUTH_DRAW),
                            onClick = null
                        )
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text(
                                text = "Jugendregel (Kulanz)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Restliche Partien als Remis (=) werten (0.5 Pkt. für beide)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Option 2: Regulär
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (selectedRule == PlayerWithdrawRule.REGULAR_LOSS),
                                onClick = { selectedRule = PlayerWithdrawRule.REGULAR_LOSS },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (selectedRule == PlayerWithdrawRule.REGULAR_LOSS),
                            onClick = null
                        )
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text(
                                text = "Regulär (Erwachsene)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Restliche Partien als verloren (-) werten (Gegner erhalten +)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(playerId, selectedRule) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Turnier beenden")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}
