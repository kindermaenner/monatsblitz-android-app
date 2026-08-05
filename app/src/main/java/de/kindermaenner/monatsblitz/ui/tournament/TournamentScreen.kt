package de.kindermaenner.monatsblitz.ui.tournament

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.kindermaenner.monatsblitz.ui.tournament.components.CrosstableHeader
import de.kindermaenner.monatsblitz.ui.tournament.components.CrosstableRow
import java.time.format.DateTimeFormatter

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
        var text = "Turnier vom ${state.tournament!!.Date.format(
            DateTimeFormatter.ofPattern("dd.MM.yy"))}"
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
        
        if (state.tournament!!.rounds > 1) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                repeat(state.tournament!!.rounds) { index ->
                    SegmentedButton(
                        selected = state.leg - 1 == index,
                        onClick = { viewModel.selectLeg(index + 1) },
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.weight(1f)
                    ) { Text("R${index + 1}") }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(6.dp))
        CrosstableHeader(
            tournament = state.tournament!!,
            horizontalScrollState = horizontalScrollState
        )

        HorizontalDivider()

        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            items(state.tournament!!.playerIds.size) { rowIndex ->
                val results = mutableListOf<String>()
                for (columnIndex in state.tournament!!.playerIds.indices) {
                    results.add(if (columnIndex == rowIndex) "x" else state.tournament!!.findGame(state.tournament!!.playerIds[rowIndex], state.tournament!!.playerIds[columnIndex], state.leg)?.result?.displayName ?: "")
                }
                CrosstableRow(
                    tournament = state.tournament!!,
                    round = state.leg,
                    rowIndex = rowIndex,
                    playerName = state.tournament!!.players[rowIndex].fullName,
                    horizontalScrollState = horizontalScrollState,
                    results = results,
                    setResult = viewModel::setResult
                )
                HorizontalDivider()
            }
        }
    }
}