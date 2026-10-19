package de.kindermaenner.monatsblitz.ui.ranking.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.kindermaenner.monatsblitz.ui.ranking.RankingRowData

@Composable
fun RankingTable(
    rows: List<RankingRowData>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Platz",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(0.8f)
            )
            Text(
                text = "Name",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(2.4f)
            )
            Text(
                text = "Punkte",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(0.8f)
            )
        }

        HorizontalDivider()

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            itemsIndexed(rows, key = { index, row -> "${row.rank}_${row.name}_$index" }) { _, row ->
                val isTopThree = row.rank in 1..3
                val formattedPoints = if (row.points % 1.0 == 0.0) {
                    row.points.toInt().toString()
                } else {
                    row.points.toString()
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${row.rank}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isTopThree) FontWeight.Bold else FontWeight.Normal,
                        color = if (isTopThree) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(0.8f)
                    )
                    Text(
                        text = row.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isTopThree) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(2.4f)
                    )
                    Text(
                        text = formattedPoints,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(0.8f)
                    )
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}