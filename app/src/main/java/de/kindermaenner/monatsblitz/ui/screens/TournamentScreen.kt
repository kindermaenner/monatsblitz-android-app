package de.kindermaenner.monatsblitz.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.kindermaenner.monatsblitz.domain.model.Tournament
import de.kindermaenner.monatsblitz.ui.viewmodels.TournamentViewModel


@Composable
fun CrosstableHeader(
    tournament: Tournament,
    horizontalScrollState: ScrollState
) {
    val NumberWidth = 40.dp
    val NameWidth = 140.dp
    val CellWidth = 40.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier.width(NumberWidth),
            contentAlignment = Alignment.Center
        ) {
            Text("Nr")
        }

        Box(
            modifier = Modifier.width(NameWidth),
            contentAlignment = Alignment.CenterStart
        ) {
            Text("Spieler")
        }

        Row(
            modifier = Modifier.horizontalScroll(horizontalScrollState)
        ) {
            tournament.playerIds.forEachIndexed { index, _ ->

                Box(
                    modifier = Modifier.width(CellWidth),
                    contentAlignment = Alignment.Center
                ) {
                    Text((index+1).toString())
                }
            }
        }
    }
}

@Composable
fun ResultCell(
    value: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val CellWidth = 40.dp
    val CellHeight = 40.dp
    Box(
        modifier = Modifier
            .width(CellWidth)
            .height(CellHeight)
            .border(
                width = 0.5.dp,
                color = MaterialTheme.colorScheme.outline
            )
            .clickable(
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun CrosstableRow(
    tournament: Tournament,
    rowIndex: Int,
    horizontalScrollState: ScrollState,
    onCellClick: (rowIndex: Int, columnIndex: Int) -> Unit = { _, _ -> }
) {
    val NumberWidth = 40.dp
    val NameWidth = 140.dp
    val rowPlayer = tournament.players[rowIndex]

    Row(
        modifier = Modifier.height(48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        // Nummer
        Box(
            modifier = Modifier.width(NumberWidth),
            contentAlignment = Alignment.Center
        ) {
            Text((rowIndex + 1).toString())
        }

        // Spielername
        Box(
            modifier = Modifier.width(NameWidth),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = "${rowPlayer.Vorname} ${rowPlayer.Name}",
                maxLines = 1
            )
        }

        // Ergebnisspalten
        Row(
            modifier = Modifier.horizontalScroll(horizontalScrollState)
        ) {

            tournament.playerIds.forEachIndexed { columnIndex, _ ->

                val text = if (rowIndex == columnIndex) {
                    "X"
                } else {
                    ""
                }

                ResultCell(
                    value = text,
                    enabled = rowIndex != columnIndex,
                    onClick = {
                        onCellClick(rowIndex, columnIndex)
                    }
                )
            }
        }
    }
}

@Composable
fun TournamentScreen(
    viewModel: TournamentViewModel
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
            text = { Text("Das aktuelle Turnier wird gelöscht.") },
            confirmButton = {
                Button(onClick = {
                    //onBackToHome()
                    showBackDialog = false
                }) {
                    Text("Ja, abbrechen")
                }
            },
            dismissButton = {
                Button(onClick = { showBackDialog = false }) {
                    Text("Nein, weitermachen")
                }
            }
        )
    }
    if (state.tournament == null) {
        return;
    }
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        CrosstableHeader(
            tournament = state.tournament!!,
            horizontalScrollState = horizontalScrollState
        )

        HorizontalDivider()

        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            items(state.tournament!!.playerIds.size) { rowIndex ->

                CrosstableRow(
                    tournament = state.tournament!!,
                    rowIndex = rowIndex,
                    horizontalScrollState = horizontalScrollState,
                    onCellClick = { row, col ->
                        // Ergebnisdialog öffnen
                    }
                )

                HorizontalDivider()
            }
        }
    }
}