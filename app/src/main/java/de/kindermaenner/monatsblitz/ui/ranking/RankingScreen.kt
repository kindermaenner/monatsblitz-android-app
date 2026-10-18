package de.kindermaenner.monatsblitz.ui.ranking

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import de.kindermaenner.monatsblitz.ui.ranking.components.RankingTable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RankingScreen(
    viewModel: RankingViewModel,
    onBack: () -> Unit,
    onFinalized: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var showFinalizeConfirmDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {

        TopAppBar(
            title = { Text("Ranking") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                }
            },
            actions = {
                val state = uiState
                if (state is RankingUiState.Ready) {
                    FilledTonalButton(
                        onClick = { showFinalizeConfirmDialog = true },
                        enabled = !state.isFinalizing && !state.isFinalized,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        if (state.isFinalizing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Wird abgeschlossen...")
                        } else {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            Text("Turnier abschließen")
                        }
                    }
                }
            }
        )

        when (val state = uiState) {
            RankingUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is RankingUiState.Ready -> {
                RankingTable(state.rows)

                if (showFinalizeConfirmDialog) {
                    AlertDialog(
                        onDismissRequest = { showFinalizeConfirmDialog = false },
                        title = { Text("Turnier abschließen?") },
                        text = {
                            Text("Die Ergebnisse werden an das WordPress-Backend gesendet und auf der Website veröffentlicht. Danach sind keine Änderungen mehr möglich.")
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showFinalizeConfirmDialog = false
                                    viewModel.finalizeTournament()
                                }
                            ) {
                                Text("Jetzt abschließen")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showFinalizeConfirmDialog = false }) {
                                Text("Abbrechen")
                            }
                        }
                    )
                }

                if (state.finalizeError != null) {
                    AlertDialog(
                        onDismissRequest = { viewModel.clearFinalizeError() },
                        title = { Text("Fehler beim Abschluss") },
                        text = { Text(state.finalizeError) },
                        confirmButton = {
                            Button(onClick = { viewModel.clearFinalizeError() }) {
                                Text("OK")
                            }
                        }
                    )
                }

                if (state.isFinalized) {
                    AlertDialog(
                        onDismissRequest = onFinalized,
                        title = { Text("Turnier abgeschlossen") },
                        text = { Text("Das Turnier wurde erfolgreich finalisiert und auf der Vereins-Website veröffentlicht.") },
                        confirmButton = {
                            Button(onClick = onFinalized) {
                                Text("Zum Hauptmenü")
                            }
                        }
                    )
                }
            }
        }
    }
}