package de.kindermaenner.monatsblitz.ui.crosstable.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import de.kindermaenner.monatsblitz.domain.model.Tournament

val NumberColumnWidth = 36.dp
val NameColumnWidth = 135.dp
val CellColumnWidth = 44.dp
val PointsColumnWidth = 48.dp

@Composable
fun CrosstableHeader(
    tournament: Tournament,
    horizontalScrollState: ScrollState
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Nr.
        Box(
            modifier = Modifier.width(NumberColumnWidth),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Nr",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Spieler
        Box(
            modifier = Modifier
                .width(NameColumnWidth)
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = "Spieler",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Scrollbare Rundenspalten 1..N
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(horizontalScrollState)
        ) {
            tournament.playerIds.forEachIndexed { index, _ ->
                Box(
                    modifier = Modifier.width(CellColumnWidth),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = (index + 1).toString(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Pkt. Spalte (fixiert rechts)
        Box(
            modifier = Modifier.width(PointsColumnWidth),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Pkt.",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
        }
    }
}
