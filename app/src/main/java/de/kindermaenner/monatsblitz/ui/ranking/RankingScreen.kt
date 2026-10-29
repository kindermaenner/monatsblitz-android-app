package de.kindermaenner.monatsblitz.ui.ranking

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.kindermaenner.monatsblitz.infrastructure.export.TournamentPdfGenerator
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
                onBack = onBack
            )
        },
        bottomBar = {
            val readyState = uiState as? RankingUiState.Ready
            if (readyState != null && !readyState.isReadOnly && !readyState.isFinalized) {
                Surface(
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(16.dp)
                    ) {
                        Button(
                            onClick = { showFinalizeConfirmDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("finalize_button"),
                            enabled = !readyState.isFinalizing
                        ) {
                            if (readyState.isFinalizing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Wird abgeschlossen...")
                            } else {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Turnier abschließen")
                            }
                        }
                    }
                }
            }
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

                    val context = LocalContext.current
                    val tournament = state.tournament
                    val isLocalOnly = tournament?.isLocalOnly == true

                    FinalizeConfirmDialog(
                        show = showFinalizeConfirmDialog,
                        openGamesCount = state.openGamesCount,
                        isLocalOnly = isLocalOnly,
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
                        isLocalOnly = isLocalOnly,
                        onExportPdf = if (tournament != null) {
                            {
                                TournamentPdfGenerator.generateAndSharePdf(
                                    context = context,
                                    tournament = tournament,
                                    rankingRows = state.rows
                                )
                            }
                        } else null,
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
    onBack: () -> Unit
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Leaderboard,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text("Rangliste")
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
            }
        },
        actions = {
            if (uiState is RankingUiState.Ready) {
                val context = LocalContext.current
                IconButton(
                    onClick = {
                        val tournament = uiState.tournament
                        if (tournament != null) {
                            TournamentPdfGenerator.generateAndSharePdf(
                                context = context,
                                tournament = tournament,
                                rankingRows = uiState.rows
                            )
                        }
                    },
                    modifier = Modifier.testTag("export_pdf_button")
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = "Als PDF teilen / drucken"
                    )
                }
            }
        }
    )
}

@Composable
private fun FinalizeConfirmDialog(
    show: Boolean,
    openGamesCount: Int,
    isLocalOnly: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!show) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isLocalOnly) "Offline-Turnier abschließen?" else "Turnier abschließen?") },
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
                if (isLocalOnly) {
                    Text("Das Offline-Turnier wird abgeschlossen. Es findet keine Übertragung an die Website statt. Danach sind keine Änderungen mehr möglich.")
                } else {
                    Text("Die Ergebnisse werden an das WordPress-Backend gesendet und auf der Website veröffentlicht. Danach sind keine Änderungen mehr möglich.")
                }
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
    isLocalOnly: Boolean = false,
    onExportPdf: (() -> Unit)? = null,
    onConfirm: () -> Unit
) {
    if (!show) return
    AlertDialog(
        onDismissRequest = onConfirm,
        title = { Text(if (isLocalOnly) "Offline-Turnier abgeschlossen" else "Turnier abgeschlossen") },
        text = {
            Text(
                if (isLocalOnly) {
                    "Das Offline-Turnier wurde erfolgreich abgeschlossen. Es wurden keine Daten an die Website übertragen. Du kannst die Ergebnisse jetzt als PDF speichern oder teilen."
                } else {
                    "Das Turnier wurde erfolgreich finalisiert und auf der Vereins-Website veröffentlicht."
                }
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                modifier = Modifier.testTag("finalize_done_button")
            ) {
                Text("Zum Hauptmenü")
            }
        },
        dismissButton = if (onExportPdf != null) {
            {
                OutlinedButton(
                    onClick = onExportPdf,
                    modifier = Modifier.testTag("finalize_export_pdf_button")
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("PDF speichern / teilen")
                }
            }
        } else null
    )
}