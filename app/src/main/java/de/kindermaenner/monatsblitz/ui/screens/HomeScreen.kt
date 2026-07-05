package de.kindermaenner.monatsblitz.ui.screens

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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import de.kindermaenner.monatsblitz.data.model.GameMode
import de.kindermaenner.monatsblitz.ui.viewmodels.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModeSelector(
    selectedMode: GameMode,
    onModeSelected: (GameMode) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {
            expanded = !expanded
        }
    ) {

        OutlinedTextField(
            value = selectedMode.displayName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Modus") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(
                    expanded = expanded
                )
            },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            }
        ) {
            GameMode.entries.forEach { mode ->
                DropdownMenuItem(
                    text = { Text(mode.displayName) },
                    onClick = {
                        onModeSelected(mode)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun HomeScreen(
    viewModel: HomeViewModel
) {
    val state by viewModel.uiState.collectAsState()

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
                            viewModel.togglePlayer(player.id)
                        }
                        .padding(8.dp)
                ) {
                    Checkbox(
                        checked = selected,
                        onCheckedChange = { viewModel.togglePlayer(player.id) }
                    )

                    Text(
                        "${player.Vorname} ${player.Name}",
                modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }

        // --- MODE DROPDOWN ---
        ModeSelector(
            selectedMode = state.selectedMode,
            onModeSelected = { mode ->
                viewModel.setMode(mode)
            }
        )

        // --- DOUBLE ROUND ---
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = state.doubleRound,
                onCheckedChange = { viewModel.setDoubleRound(it) }
            )
            Text("Doppelrundig")
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- START BUTTON ---
        Button(
            onClick = {
                viewModel.startTournament()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Turnier starten")
        }
    }
}