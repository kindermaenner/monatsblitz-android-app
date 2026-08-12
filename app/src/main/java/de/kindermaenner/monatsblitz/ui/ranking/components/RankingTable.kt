package de.kindermaenner.monatsblitz.ui.ranking.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import de.kindermaenner.monatsblitz.ui.ranking.RankingRowData

@Composable
fun RankingTable(rows: List<RankingRowData>) {

    Column {

        Row {
            Text("Platz", modifier = Modifier.weight(1f))
            Text("Name", modifier = Modifier.weight(2f))
            Text("Punkte", modifier = Modifier.weight(1f))
        }

        rows.forEach { row ->
            Row {
                Text("${row.rank}", modifier = Modifier.weight(1f))
                Text(row.name, modifier = Modifier.weight(2f))
                Text("${row.points}", modifier = Modifier.weight(1f))
            }
        }
    }
}