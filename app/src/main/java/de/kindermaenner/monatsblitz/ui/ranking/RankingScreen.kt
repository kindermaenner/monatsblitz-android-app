package de.kindermaenner.monatsblitz.ui.ranking

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.kindermaenner.monatsblitz.ui.ranking.components.RankingTable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RankingScreen(
    viewModel: RankingViewModel,
    onBack: () -> Unit,
    onFinalized: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showFinalizeConfirmDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            RankingTopBar(
                uiState = uiState,
                onBack = onBack,
                onFinalizeClick = { showFinalizeConfirmDialog = true }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
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

                    FinalizeConfirmDialog(
                        show = showFinalizeConfirmDialog,
                        openGamesCount = state.openGamesCount,
                        onConfirm = {
                            showFinalizeConfirmDialog = false
                            viewModel.finalizeTournament()
                        },
                        onDismiss = { showFinalizeConfirmDialog = false }
                    )

                    FinalizeErrorDialog(
                        error = state.finalizeError,
                        onDismiss = { viewModel.clearFinalizeError() }
                    )

                    FinalizeSuccessDialog(
                        show = state.isFinalized,
                        onConfirm = onFinalized
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RankingTopBar(
    uiState: RankingUiState,
    onBack: () -> Unit,
    onFinalizeClick: () -> Unit
) {
    TopAppBar(
        title = { Text("Ranking") },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
            }
        },
        actions = {
            if (uiState is RankingUiState.Ready) {
                FilledTonalButton(
                    onClick = onFinalizeClick,
                    enabled = !uiState.isFinalizing && !uiState.isFinalized && !uiState.isReadOnly,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    when {
                        uiState.isFinalizing -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Wird abgeschlossen...")
                        }
                        uiState.isReadOnly -> {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            Text("Abgeschlossen")
                        }
                        else -> {
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
        }
    )
}

@Composable
private fun FinalizeConfirmDialog(
    show: Boolean,
    openGamesCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!show) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Turnier abschließen?") },
        text = {
            Column {
                if (openGamesCount > 0) {
                    val gamesLabel = if (openGamesCount == 1) "ist noch 1 Partie" else "sind noch $openGamesCount Partien"
                    Text(
                        text = "Achtung: Es $gamesLabel offen!",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Text("Die Ergebnisse werden an das WordPress-Backend gesendet und auf der Website veröffentlicht. Danach sind keine Änderungen mehr möglich.")
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Jetzt abschließen")
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
private fun FinalizeErrorDialog(
    error: String?,
    onDismiss: () -> Unit
) {
    if (error == null) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Fehler beim Abschluss") },
        text = { Text(error) },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}

@Composable
private fun FinalizeSuccessDialog(
    show: Boolean,
    onConfirm: () -> Unit
) {
    if (!show) return
    AlertDialog(
        onDismissRequest = onConfirm,
        title = { Text("Turnier abgeschlossen") },
        text = { Text("Das Turnier wurde erfolgreich finalisiert und auf der Vereins-Website veröffentlicht.") },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Zum Hauptmenü")
            }
        }
    )
}