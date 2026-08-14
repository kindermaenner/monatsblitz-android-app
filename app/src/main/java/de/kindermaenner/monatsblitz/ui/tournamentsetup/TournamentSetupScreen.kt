package de.kindermaenner.monatsblitz.ui.tournamentsetup

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.kindermaenner.monatsblitz.ui.tournamentsetup.components.GameModeDropdownSelector


@Composable
fun TournamentSetupScreen(
    viewModel: TournamentSetupViewModel,
    onNavigateToCrosstable: (Long) -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.navigationEffect.collect { tournamentId ->
            onNavigateToCrosstable(tournamentId)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        Text("Neues Turnier", style = MaterialTheme.typography.headlineSmall)

        // --- PLAYER MULTI SELECT ---
        Text("Spieler auswählen")

        LazyColumn(
            modifier = Modifier
                .height(250.dp)
                .fillMaxWidth()
        ) {
            items(state.players) { player ->
                val selected = player.id in state.selectedPlayerIds

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.onPlayerChecked(player.id, !selected)
                        }
                        .padding(8.dp)
                ) {
                    Checkbox(
                        checked = selected,
                        onCheckedChange = { viewModel.onPlayerChecked(player.id, !selected) }
                    )

                    Text(
                        "${player.Vorname} ${player.Name}",
                modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }

        // --- MODE DROPDOWN ---
        GameModeDropdownSelector(
            selectedMode = state.selectedMode,
            onModeSelected = { mode ->
                viewModel.onModeChanged(mode)
            }
        )

        // --- DOUBLE ROUND ---
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = state.doubleRound,
                onCheckedChange = { viewModel.onDoubleRoundChanged(it) }
            )
            Text("Doppelrundig")
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- START BUTTON ---
        Button(
            onClick = {
                viewModel.createTournament()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Turnier starten")
        }
    }
}
