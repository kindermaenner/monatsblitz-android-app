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
        RankingTableHeader()

        HorizontalDivider()

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            itemsIndexed(rows, key = { index, row -> "${row.rank}_${row.name}_$index" }) { _, row ->
                RankingTableRow(row = row)
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun RankingTableHeader() {
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
}

@Composable
private fun RankingTableRow(row: RankingRowData) {
    val isTopThree = row.rank in 1..3
    val rankColor = if (isTopThree) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    val rankWeight = if (isTopThree) FontWeight.Bold else FontWeight.Normal
    val nameWeight = if (isTopThree) FontWeight.SemiBold else FontWeight.Normal

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
            fontWeight = rankWeight,
            color = rankColor,
            modifier = Modifier.weight(0.8f)
        )
        Text(
            text = row.name,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = nameWeight,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(2.4f)
        )
        Text(
            text = formatPoints(row.points),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.8f)
        )
    }
}

private fun formatPoints(points: Double): String {
    return if (points % 1.0 == 0.0) {
        points.toInt().toString()
    } else {
        points.toString()
    }
}