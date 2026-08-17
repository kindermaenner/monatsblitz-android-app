package de.kindermaenner.monatsblitz.ui.crosstable.components

import android.util.Log
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.kindermaenner.monatsblitz.domain.model.GameResult
import de.kindermaenner.monatsblitz.domain.model.Tournament

@Composable
fun CrosstableRow(
    tournament: Tournament,
    round: Int,
    rowIndex: Int,
    playerName: String,
    totalPoints: Double,
    horizontalScrollState: ScrollState,
    results: List<String>,
    setResult: (rowIndex: Int, columnIndex: Int, round: Int, result: GameResult) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Nummer
        Box(
            modifier = Modifier.width(NumberColumnWidth),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = (rowIndex + 1).toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Spielername
        Box(
            modifier = Modifier
                .width(NameColumnWidth)
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = playerName,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Ergebnisspalten (scrollbar)
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(horizontalScrollState),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tournament.playerIds.forEachIndexed { columnIndex, _ ->
                Box(
                    modifier = Modifier.width(CellColumnWidth),
                    contentAlignment = Alignment.Center
                ) {
                    ResultCell(
                        value = results[columnIndex],
                        enabled = rowIndex != columnIndex,
                        player1Name = playerName,
                        player2Name = tournament.players.getOrNull(columnIndex)?.fullName ?: "",
                        round = round,
                        onResultSelected = { result ->
                            Log.i("ResultCell", "Result selected: rowIndex=$rowIndex, columnIndex=$columnIndex, round=$round, result=$result")
                            setResult(rowIndex, columnIndex, round, result)
                        }
                    )
                }
            }
        }

        // Gesamtpunkte (fixiert rechts)
        val formattedPoints = if (totalPoints % 1.0 == 0.0) {
            totalPoints.toInt().toString()
        } else {
            totalPoints.toString()
        }

        Box(
            modifier = Modifier.width(PointsColumnWidth),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = formattedPoints,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
        }
    }
}
