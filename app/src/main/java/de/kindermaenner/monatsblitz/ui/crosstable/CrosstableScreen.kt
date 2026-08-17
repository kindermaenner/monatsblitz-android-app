package de.kindermaenner.monatsblitz.ui.crosstable

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.kindermaenner.monatsblitz.ui.crosstable.components.CrosstableHeader
import de.kindermaenner.monatsblitz.ui.crosstable.components.CrosstableRow
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrosstableScreen(
    viewModel: CrosstableViewModel,
    onNavigateToRanking: (Long) -> Unit,
    onBackToSetup: () -> Unit
) {
    val horizontalScrollState = rememberScrollState()
    var showBackDialog by remember { mutableStateOf(false) }
    val state by viewModel.uiState.collectAsState()

    BackHandler {
        showBackDialog = true
    }

    if (showBackDialog) {
        AlertDialog(
            onDismissRequest = { showBackDialog = false },
            title = { Text("Turnier abbrechen?") },
            text = { Text("Das aktuelle Turnier wird beendet.") },
            confirmButton = {
                Button(
                    onClick = {
                        showBackDialog = false
                        onBackToSetup()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Ja, abbrechen")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackDialog = false }) {
                    Text("Nein, weitermachen")
                }
            }
        )
    }

    val tournament = state.tournament

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Kreuztabelle")
                        if (tournament != null) {
                            Text(
                                text = "${tournament.Date.format(DateTimeFormatter.ofPattern("dd.MM.yy"))} • ${tournament.Mode.displayName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { showBackDialog = true }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Turnier abbrechen"
                        )
                    }
                },
                actions = {
                    if (tournament != null) {
                        FilledTonalButton(
                            onClick = {
                                viewModel.prepareRankingsAndNavigate {
                                    onNavigateToRanking(tournament.Id)
                                }
                            },
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
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // --- MULTI-ROUND SELECTOR ---
            if (tournament.rounds > 1) {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    repeat(tournament.rounds) { index ->
                        SegmentedButton(
                            selected = state.leg - 1 == index,
                            onClick = { viewModel.selectLeg(index + 1) },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Runde ${index + 1}")
                        }
                    }
                }
            }

            // --- CROSSTABLE HEADER ---
            CrosstableHeader(
                tournament = tournament,
                horizontalScrollState = horizontalScrollState
            )

            HorizontalDivider()

            // --- CROSSTABLE ROWS ---
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(tournament.playerIds.size) { rowIndex ->
                    val playerId = tournament.playerIds[rowIndex]
                    val points = state.playerPoints[playerId] ?: 0.0

                    val results = mutableListOf<String>()
                    for (columnIndex in tournament.playerIds.indices) {
                        results.add(
                            if (columnIndex == rowIndex) "x"
                            else tournament.findGame(
                                tournament.playerIds[rowIndex],
                                tournament.playerIds[columnIndex],
                                state.leg
                            )?.result?.displayName ?: ""
                        )
                    }

                    CrosstableRow(
                        tournament = tournament,
                        round = state.leg,
                        rowIndex = rowIndex,
                        playerName = tournament.players[rowIndex].fullName,
                        totalPoints = points,
                        horizontalScrollState = horizontalScrollState,
                        results = results,
                        setResult = viewModel::setResult
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}
